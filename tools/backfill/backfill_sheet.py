#!/usr/bin/env python3
"""
One-off backfill of the "My Savings" Google Sheet into a Finance Tracker 5.0.0 database copy.

    python3 backfill_sheet.py --db "Finance Tracker Database - <date>.db" --sheet "My Savings_<date>.xlsx"
                              [--out backfilled.db] [--report report.md]

The input database is never modified: it is copied to --out first. Needs Python 3.9+ and openpyxl
(`pip install openpyxl`). The sheet export must contain the values (not just formulas): File > Download
> Microsoft Excel from Google Sheets does.

What it does (details in README.md):
  0. Adds the income categories (Salary, Cashback, Refunds, Reimbursements), Loans & Lends for both kinds, and lets
     Family, Friends and Relatives and Others take income too; the 4.x import creates "Interest & Dividends" only.
     With --assign-income the income rows without a category are filed by keyword rules (preview in the report).
  1. Seeds the investment category tree if the app has not yet (same tree as the app), adds sub-categories
     for fund types the sheet uses that the tree lacks, and writes the targets, tolerance bands and rebalance
     notes from the Investment Summary sheet plus the goal amount.
  2. Assigns every stock and fund to its sheet type's sub-category; deposits, policies and others to theirs.
  3. Splits the single "Fixed Deposits (unmapped)" instrument into one instrument per sheet deposit, with the
     terms from the Fixed Deposits sheet, renewal chains (deposit and maturity rows that never touched a bank
     account), the sweep-in deposits merged into the existing sweep-in instrument, and the 4.x "interest
     credited" rows (which duplicate the interest already inside the matured amount) removed.
  4. Fills in policy numbers, premium amounts, frequencies and due dates for the life insurance policies; turns the
     sheet's health insurance rows into a health policy (an expense policy, out of the investment totals) with one
     cover period per premium; with --insurance, makes a vehicle of each one listed there and links its insurance,
     service and emission-test expenses to it.
  4b. --merge OLD=NEW (repeatable) merges a renamed instrument into its new name the way the app's
     "Merge into another instrument" does (rows and templates move, the old one is deleted), e.g. --merge ZOMATO=ETERNAL.
  5. Writes a report of what was done and of the totals compared with the sheet, and aborts without writing
     when a check fails (use --force to write anyway).
"""
import argparse
import collections
import datetime as dt
import json
import re
import shutil
import sqlite3
import sys

try:
    import openpyxl
except ImportError:  # pragma: no cover
    sys.exit("openpyxl is required: pip install openpyxl")

SCHEMA_VERSION = 8
# Room's identity hashes (room_master_table), written after this script applies the app's own migrations.
SCHEMA_V2_IDENTITY_HASH = "cfac96437eb4bd5056257688ca832fea"
SCHEMA_V3_IDENTITY_HASH = "860e0b3e05274df33b4d89e1d1380939"
SCHEMA_V4_IDENTITY_HASH = "0711ddf2bc34586547a6551698ce0b80"
SCHEMA_V5_IDENTITY_HASH = "50066662eb25daa0fd2869d49fed834d"
SCHEMA_V6_IDENTITY_HASH = "bff783913534b7a34ea6dc5cedde0ffa"
SCHEMA_V7_IDENTITY_HASH = "afd4d13b5813c801722c693f0e319fac"
SCHEMA_V8_IDENTITY_HASH = "f549d763e40f6240e309b39296845270"
GOAL_AMOUNT_KEY = "investments.goal_amount"
BACKFILL_KEY = "backfill.sheet"
UNMAPPED_FD_NAME = "Fixed Deposits (unmapped)"

# The app's category tree (InvestmentCategories.kt), name -> risk level, children in order.
RISK_LEVELS = ["No risk", "Low risk", "Medium risk", "High risk"]
TREE = [
    ("Fixed Deposits", "No risk", []),
    ("Mutual Funds", "Medium risk", [
        ("Arbitrage", "No risk"), ("Large Cap", "Low risk"), ("Flexi Cap", "Low risk"), ("Focused", "Medium risk"),
        ("Value Discovery", "Medium risk"), ("Mid Cap", "Medium risk"), ("Foreign Equity", "Medium risk"),
        ("Small Cap", "High risk"),
    ]),
    ("Stocks", "Medium risk", [
        ("Debt ETF", "No risk"), ("Gold ETF", "Low risk"), ("ETF", "Medium risk"), ("Foreign ETF", "Medium risk"),
        ("REIT", "Medium risk"), ("Silver ETF", "Medium risk"), ("Equity", "High risk"),
    ]),
    ("Provident Funds", "No risk", []),
    ("Life Insurance", "No risk", []),
    ("Others", "No risk", []),
]
SUMMARY_PARENTS = {"Mutual Funds - All": "Mutual Funds", "Stocks - All": "Stocks"}
# Sheet bank labels -> a distinctive part of the account name in the app.
BANK_ACCOUNT_HINTS = {"SBI": "State Bank", "HDFC": "HDFC Bank", "ICICI": "ICICI Bank", "UBI": "Union Bank", "KVGB": "KaGB"}


def now_iso():
    return dt.datetime.now().isoformat(timespec="milliseconds")


def as_date(v):
    if v in (None, ""):
        return None
    if isinstance(v, dt.datetime):
        return v.date()
    if isinstance(v, dt.date):
        return v
    return dt.date.fromisoformat(str(v)[:10])


def as_float(v):
    if v in (None, ""):
        return None
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def tds_fraction(v):
    """'0.1', 0.1, '0,10,10%' (per financial year) -> 0.1; the text is kept in the notes by the caller."""
    if v in (None, ""):
        return None
    if isinstance(v, (int, float)):
        return float(v) / 100 if v > 1 else float(v)
    nums = [float(x) for x in re.findall(r"[\d.]+", str(v))]
    if not nums:
        return None
    last = max(nums)
    return last / 100 if last > 1 else last


def period_label(months, days):
    if months:
        m = int(months)
        return f"{m // 12} year{'s' if m // 12 != 1 else ''}" if m % 12 == 0 and m >= 12 else f"{m} months"
    if days:
        return f"{int(days)} days"
    return "?"


def plain(x):
    s = f"{x:.4f}".rstrip("0").rstrip(".")
    return s if s else "0"


class Report:
    def __init__(self):
        self.extra_sections = []
        self.lines = []
        self.problems = []

    def add(self, text=""):
        self.lines.append(text)

    def problem(self, text):
        self.problems.append(text)
        self.lines.append(f"**PROBLEM:** {text}")

    def text(self):
        return "\n".join(self.lines + self.extra_sections) + "\n"


# --------------------------------------------------------------------------------------------------- sheet

class Sheet:
    def __init__(self, path):
        self.wb = openpyxl.load_workbook(path, data_only=True, read_only=True)

    def rows(self, name):
        ws = self.wb[name]
        it = ws.iter_rows(values_only=True)
        header = list(next(it))
        n = max(i for i, c in enumerate(header) if c not in (None, "")) + 1
        header = header[:n]
        out = []
        for r in it:
            r = list(r[:n]) + [None] * (n - len(r))
            if all(c in (None, "") for c in r):
                continue
            out.append(dict(zip(header, r)))
        return out

    def fixed_deposits(self):
        out = []
        for r in self.rows("Fixed Deposits"):
            if as_float(r.get("ID")) is None:
                continue
            out.append({
                "id": int(r["ID"]),
                "certificate": r.get("Certificate No"),
                "holder": r.get("Name"),
                "amount": as_float(r.get("Deposit Amount")) or 0.0,
                "date": as_date(r.get("Deposit Date")),
                "months": as_float(r.get("Period (Months)")),
                "days": as_float(r.get("Period (Days)")),
                "rate": as_float(r.get("Rate")),
                "compounding": as_float(r.get("Compounding Frequency (Months)")),
                "maturity": as_date(r.get("Maturity Date")),
                "printed": as_float(r.get("Maturity Amount (Printed)")),
                "tds_rate_text": r.get("TDS Rate"),
                "tds_rate": tds_fraction(r.get("TDS Rate")),
                "tds_reported": as_float(r.get("TDS Reported by Bank")),
                "credited": as_float(r.get("Amount Credited post TDS")),
                "interest": as_float(r.get("Interest Earned post TDS")),
                "action": (r.get("Action") or "").strip(),
                "account_no": r.get("Account No"),
                "bank": (r.get("Bank") or "").strip() or None,
                "branch": r.get("Branch"),
                "cust_id": r.get("Cust ID"),
                "notes": r.get("Notes"),
                "next": int(as_float(r.get("Next"))) if as_float(r.get("Next")) is not None else None,
                "previous": int(as_float(r.get("Previous"))) if as_float(r.get("Previous")) is not None else None,
                "extra": False,
            })
        return out

    def investment_summary(self):
        return [r for r in self.rows("Investment Summary") if r.get("Name")]

    def insurances(self):
        return [r for r in self.rows("Insurances") if r.get("Date")]

    def stock_types(self):
        """Stock name -> type, from the Stock Credits sheet (the summary only lists what is still held)."""
        votes = collections.defaultdict(collections.Counter)
        for r in self.rows("Stock Credits"):
            t = r.get("Stock Type Prefilled") or r.get("Stock Type")
            if r.get("Stock Name") and t:
                votes[r["Stock Name"]][t] += 1
        return {k: v.most_common(1)[0][0] for k, v in votes.items()}

    def fund_types(self):
        votes = collections.defaultdict(collections.Counter)
        for r in self.rows("Mutual Fund Credits"):
            t = r.get("Mutual Fund Type Prefilled") or r.get("Mutual Fund Type")
            if r.get("Fund Name") and t:
                votes[r["Fund Name"]][t] += 1
        return {k: v.most_common(1)[0][0] for k, v in votes.items()}


def extra_fixed_deposits(path):
    """
    Deposits the sheet never had (found in the ledger, or renewals of such a deposit), from a JSON file kept next to the
    sheet export, never in this repository: a list of objects with the sheet's columns in snake_case — id (any string,
    only used by next / previous), holder, amount, date, months, days, rate, compounding, maturity, printed,
    tds_reported, credited, interest, action ("", "Closed at Maturity", "Renewed", "Closed Prematurely"), closed_on
    (the actual date of a premature closure), account_no, bank, branch, cust_id, certificate, notes, next, previous.
    Two more for chains the bank ran as "renew principal, repay interest": renewed (the amount that went into the
    next deposit when it is less than credited; the rest is booked as paid to the bank account on that date) and
    booked_as (the amount of the ledger's closure row when it lumped earlier interest payouts in: the row is matched
    by that amount and cut down to credited, the payouts having been booked on their own dates).
    """
    with open(path) as fh:
        rows = json.load(fh)
    out = []
    for r in rows:
        out.append({
            "id": str(r["id"]),
            "certificate": r.get("certificate"),
            "holder": r.get("holder"),
            "amount": as_float(r.get("amount")) or 0.0,
            "date": as_date(r.get("date")),
            "months": as_float(r.get("months")),
            "days": as_float(r.get("days")),
            "rate": as_float(r.get("rate")),
            "compounding": as_float(r.get("compounding")),
            "maturity": as_date(r.get("maturity")),
            "closed_on": as_date(r.get("closed_on")),
            "printed": as_float(r.get("printed")),
            "tds_rate_text": None,
            "tds_rate": None,
            "tds_reported": as_float(r.get("tds_reported")),
            "credited": as_float(r.get("credited")),
            "renewed": as_float(r.get("renewed")),
            "booked_as": as_float(r.get("booked_as")),
            "interest": as_float(r.get("interest")),
            "action": (r.get("action") or "").strip(),
            "account_no": r.get("account_no"),
            "bank": (r.get("bank") or "").strip() or None,
            "branch": r.get("branch"),
            "cust_id": r.get("cust_id"),
            "notes": r.get("notes"),
            "next": str(r["next"]) if r.get("next") not in (None, "") else None,
            "previous": str(r["previous"]) if r.get("previous") not in (None, "") else None,
            "extra": True,
        })
    return out


def sweep_in_plan(path):
    """
    Which partial liquidations belong to which sweep-in deposit, from a JSON file kept next to the sheet export: a
    list of objects with sheet_id (the sheet's ID column), converted_on (the day the deposit was linked to the
    account, shown in the ledger), liquidations (the ids of the ledger's "liquidated partially" rows, in any order)
    and optionally amount (the deposit, when it is not the only deposit row near the sheet's date). The bank's
    statement says which deposit each liquidation came from; the interest row beside each liquidation confirms it,
    and the script checks that every deposit ends at exactly zero.
    """
    with open(path) as fh:
        rows = json.load(fh)
    out = {}
    for r in rows:
        out[int(r["sheet_id"])] = {
            "converted_on": as_date(r.get("converted_on")),
            "liquidations": [int(x) for x in r.get("liquidations", [])],
            "amount": as_float(r.get("amount")),
        }
    return out


def pair_interest(liquidations, interests):
    """
    Pairs each "liquidated partially" row of one day with the "interest credited" row booked beside it: an
    order-preserving matching that uses every interest row and leaves a whole-rupee principal (the bank breaks
    deposits in whole rupees; the paise are the interest). Returns {liquidation id: interest row or None}.
    """
    L = sorted(liquidations, key=lambda r: r["id"])
    I = sorted(interests, key=lambda r: r["id"])

    def whole(l, i):
        p = l["amount"] - i["amount"]
        return p > 0 and abs(p - round(p)) < 0.005

    def solve(li, ii):
        if ii == len(I):
            return {}
        if li == len(L):
            return None
        if whole(L[li], I[ii]):
            rest = solve(li + 1, ii + 1)
            if rest is not None:
                return {L[li]["id"]: I[ii], **rest}
        return solve(li + 1, ii)

    found = solve(0, 0)
    if found is None:
        return None
    return {l["id"]: found.get(l["id"]) for l in L}


def sheet_order(f):
    """Sheet rows by ID, then the extra file's rows in their own order."""
    return (1 if f["extra"] else 0, f["id"] if not f["extra"] else 0, str(f["id"]))


def name_deposits(fds, chronological, report):
    """
    Gives every deposit its app name and sets f["name"]. By default the sheet keeps its order and each deposit from the
    extra file is slotted in before the first sheet row dated after it, so the sheet's later IDs move up by the number
    of deposits inserted before them (an extra 2020 deposit becomes FD #2 and the sheet's FD #2 becomes FD #3).
    With chronological=False the sheet's IDs stay and the extra deposits continue after the last one.
    """
    sheet_rows = sorted([f for f in fds if not f["extra"]], key=lambda f: f["id"])
    extras = sorted([f for f in fds if f["extra"]], key=lambda f: (f["date"] or dt.date.min, str(f["id"])))
    if chronological:
        sequence = []
        for row in sheet_rows:
            while extras and (extras[0]["date"] or dt.date.min) < (row["date"] or dt.date.max):
                sequence.append(extras.pop(0))
            sequence.append(row)
        sequence.extend(extras)
        for n, f in enumerate(sequence, 1):
            f["name"] = f"FD #{n}"
        moved = sorted((f["id"], int(f["name"][4:])) for f in sheet_rows if int(f["name"][4:]) != f["id"])
        if moved:
            runs = []
            for old, new in moved:
                if runs and runs[-1][1] == old - 1 and runs[-1][3] == new - 1:
                    runs[-1][1], runs[-1][3] = old, new
                else:
                    runs.append([old, old, new, new])
            text = "; ".join(f"sheet FD #{a}–#{b} → FD #{c}–#{d}" if a != b else f"sheet FD #{a} → FD #{c}" for a, b, c, d in runs)
            report.add(f"- Deposits from the extra file were slotted in by date, so {len(moved)} sheet IDs moved: {text}.")
        elif extras or any(f["extra"] for f in fds):
            report.add("- Deposits from the extra file were slotted in by date after the last sheet row; every sheet ID kept its number.")
    else:
        top = max([f["id"] for f in sheet_rows] or [0])
        for f in sheet_rows:
            f["name"] = f"FD #{f['id']}"
        for f in extras:
            top += 1
            f["name"] = f"FD #{top}"
        if extras:
            report.add("- Deposits keep the sheet's ID numbers; deposits from the extra file continue after the last one.")


# ------------------------------------------------------------------------------------------------ database

class Db:
    def __init__(self, path):
        self.c = sqlite3.connect(path)
        self.c.row_factory = sqlite3.Row
        self.c.execute("PRAGMA foreign_keys = OFF")

    def q(self, sql, *args):
        return self.c.execute(sql, args).fetchall()

    def one(self, sql, *args):
        return self.c.execute(sql, args).fetchone()

    def x(self, sql, *args):
        return self.c.execute(sql, args)

    def version(self):
        return self.one("PRAGMA user_version")[0]

    def migrate_1_to_2(self, report):
        """The app's Migrations.MIGRATION_1_2, for snapshots taken before the app itself was upgraded."""
        report.add("- Input was schema version 1: applied the app's 1 -> 2 migration (risk levels, settings, category risk ids).")
        self.x("CREATE TABLE IF NOT EXISTS `risk_levels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `display_order` INTEGER NOT NULL)")
        self.x("CREATE INDEX IF NOT EXISTS `index_risk_levels_display_order` ON `risk_levels` (`display_order`)")
        self.x("INSERT INTO `risk_levels` (`id`, `name`, `display_order`) VALUES (1, 'No risk', 1), (2, 'Low risk', 2), (3, 'Medium risk', 3), (4, 'High risk', 4)")
        self.x("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
        self.x("CREATE TABLE `investment_categories_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `parent_id` INTEGER, "
               "`risk_level_id` INTEGER, `tolerance_pct` REAL, `target_goal_pct` REAL, `target_wealth_pct` REAL, `rebalance_notes` TEXT, `display_order` INTEGER NOT NULL, "
               "FOREIGN KEY(`parent_id`) REFERENCES `investment_categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
               "FOREIGN KEY(`risk_level_id`) REFERENCES `risk_levels`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
        self.x("INSERT INTO `investment_categories_new` (`id`, `name`, `parent_id`, `risk_level_id`, `tolerance_pct`, `target_goal_pct`, `target_wealth_pct`, `rebalance_notes`, `display_order`) "
               "SELECT `id`, `name`, `parent_id`, CASE `risk_bucket` WHEN 'NONE' THEN 1 WHEN 'LOW' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'HIGH' THEN 4 ELSE NULL END, "
               "`tolerance_pct`, `target_goal_pct`, `target_wealth_pct`, `rebalance_notes`, `display_order` FROM `investment_categories`")
        self.x("DROP TABLE `investment_categories`")
        self.x("ALTER TABLE `investment_categories_new` RENAME TO `investment_categories`")
        self.x("CREATE INDEX IF NOT EXISTS `index_investment_categories_parent_id` ON `investment_categories` (`parent_id`)")
        self.x("CREATE INDEX IF NOT EXISTS `index_investment_categories_risk_level_id` ON `investment_categories` (`risk_level_id`)")
        self.x("CREATE INDEX IF NOT EXISTS `index_investment_categories_display_order` ON `investment_categories` (`display_order`)")
        self.x("INSERT INTO `investment_categories` (`name`, `risk_level_id`, `display_order`) "
               "SELECT 'Provident Funds', 1, (SELECT COALESCE(MAX(`display_order`), 0) + 1 FROM `investment_categories`) "
               "WHERE EXISTS (SELECT 1 FROM `investment_categories`) AND NOT EXISTS (SELECT 1 FROM `investment_categories` WHERE `name` = 'Provident Funds')")
        self.x("UPDATE room_master_table SET identity_hash = ?", SCHEMA_V2_IDENTITY_HASH)
        self.x("PRAGMA user_version = 2")

    def migrate_2_to_3(self, report):
        """The app's Migrations.MIGRATION_2_3: templates carry a rate and a quantity instead of an amount."""
        report.add("- Input was schema version 2: applied the app's 2 -> 3 migration (template amount becomes the rate).")
        self.x("CREATE TABLE `templates_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `particulars` TEXT NOT NULL, `kind` TEXT NOT NULL, "
               "`from_account_id` INTEGER, `to_account_id` INTEGER, `category_id` INTEGER, `instrument_id` INTEGER, `rate` REAL, `quantity` REAL, `hidden` INTEGER NOT NULL, "
               "FOREIGN KEY(`from_account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
               "FOREIGN KEY(`to_account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
               "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
               "FOREIGN KEY(`instrument_id`) REFERENCES `instruments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        self.x("INSERT INTO `templates_new` (`id`, `particulars`, `kind`, `from_account_id`, `to_account_id`, `category_id`, `instrument_id`, `rate`, `quantity`, `hidden`) "
               "SELECT `id`, `particulars`, `kind`, `from_account_id`, `to_account_id`, `category_id`, `instrument_id`, `amount`, NULL, `hidden` FROM `templates`")
        self.x("DROP TABLE `templates`")
        self.x("ALTER TABLE `templates_new` RENAME TO `templates`")
        for col in ("from_account_id", "to_account_id", "category_id", "instrument_id"):
            self.x(f"CREATE INDEX IF NOT EXISTS `index_templates_{col}` ON `templates` (`{col}`)")
        self.x("UPDATE room_master_table SET identity_hash = ?", SCHEMA_V3_IDENTITY_HASH)
        self.x("PRAGMA user_version = 3")

    def migrate_3_to_4(self, report):
        """The app's Migrations.MIGRATION_3_4: the fixed deposit templates table."""
        report.add("- Input was schema version 3: applied the app's 3 -> 4 migration (fixed deposit templates table).")
        self.x("CREATE TABLE IF NOT EXISTS `fd_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, "
               "`bank_name` TEXT, `branch` TEXT, `holder_name` TEXT, `cust_id` TEXT, `period_months` INTEGER, `period_days` INTEGER, "
               "`rate` REAL, `compounding_months` INTEGER, `tds_rate` REAL, `owner` TEXT NOT NULL, `display_order` INTEGER NOT NULL)")
        self.x("UPDATE room_master_table SET identity_hash = ?", SCHEMA_V4_IDENTITY_HASH)
        self.x("PRAGMA user_version = 4")

    def migrate_4_to_5(self, report):
        """The app's Migrations.MIGRATION_4_5: tds_yearly on deposits, wider templates, the fd_events table."""
        report.add("- Input was schema version 4: applied the app's 4 -> 5 migration (TDS timing, template amount and account, deposit events).")
        self.x("ALTER TABLE `fixed_deposits` ADD COLUMN `tds_yearly` INTEGER")
        self.x("CREATE TABLE IF NOT EXISTS `fd_templates_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, "
               "`bank_name` TEXT, `branch` TEXT, `holder_name` TEXT, `cust_id` TEXT, `amount` REAL, `from_account_id` INTEGER, "
               "`period_months` INTEGER, `period_days` INTEGER, `rate` REAL, `compounding_months` INTEGER, `owner` TEXT, `notes` TEXT, "
               "`display_order` INTEGER NOT NULL)")
        self.x("INSERT INTO `fd_templates_new` (`id`, `name`, `bank_name`, `branch`, `holder_name`, `cust_id`, `period_months`, `period_days`, "
               "`rate`, `compounding_months`, `owner`, `display_order`) SELECT `id`, `name`, `bank_name`, `branch`, `holder_name`, `cust_id`, "
               "`period_months`, `period_days`, `rate`, `compounding_months`, `owner`, `display_order` FROM `fd_templates`")
        self.x("DROP TABLE `fd_templates`")
        self.x("ALTER TABLE `fd_templates_new` RENAME TO `fd_templates`")
        self.x("CREATE TABLE IF NOT EXISTS `fd_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `instrument_id` INTEGER NOT NULL, "
               "`date` TEXT NOT NULL, `kind` TEXT NOT NULL, `amount` REAL NOT NULL, `notes` TEXT, "
               "FOREIGN KEY(`instrument_id`) REFERENCES `instruments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        self.x("CREATE INDEX IF NOT EXISTS `index_fd_events_instrument_id_date` ON `fd_events` (`instrument_id`, `date`)")
        self.x("UPDATE room_master_table SET identity_hash = ?", SCHEMA_V5_IDENTITY_HASH)
        self.x("PRAGMA user_version = 5")

    def migrate_5_to_6(self, report):
        """The app's Migrations.MIGRATION_5_6: deposit type (regular or sweep-in) and the sweep-in date."""
        report.add("- Input was schema version 5: applied the app's 5 -> 6 migration (deposit type and sweep-in date).")
        self.x("ALTER TABLE `fixed_deposits` ADD COLUMN `deposit_type` TEXT NOT NULL DEFAULT 'REGULAR'")
        self.x("ALTER TABLE `fixed_deposits` ADD COLUMN `sweep_in_date` TEXT")
        self.x("UPDATE room_master_table SET identity_hash = ?", SCHEMA_V6_IDENTITY_HASH)
        self.x("PRAGMA user_version = 6")

    def migrate_6_to_7(self, report):
        """The app's Migrations.MIGRATION_6_7: insurance types by what they cover, due day and month, validity periods."""
        report.add("- Input was schema version 6: applied the app's 6 -> 7 migration (insurance due day and month, vehicle number, validity periods).")
        self.x("CREATE TABLE IF NOT EXISTS `insurance_policies_new` (`instrument_id` INTEGER NOT NULL, `insurance_type` TEXT NOT NULL, "
               "`provider` TEXT, `policy_name` TEXT, `policy_number` TEXT, `vehicle_number` TEXT, `sum_assured` REAL, `premium_amount` REAL, "
               "`premium_frequency_months` INTEGER, `due_day` INTEGER, `due_month` INTEGER, `start_date` TEXT, `maturity_date` TEXT, "
               "PRIMARY KEY(`instrument_id`), FOREIGN KEY(`instrument_id`) REFERENCES `instruments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        self.x("INSERT INTO `insurance_policies_new` (`instrument_id`, `insurance_type`, `provider`, `policy_name`, `policy_number`, `vehicle_number`, "
               "`sum_assured`, `premium_amount`, `premium_frequency_months`, `due_day`, `due_month`, `start_date`, `maturity_date`) "
               "SELECT `instrument_id`, CASE `insurance_type` WHEN 'OTHER' THEN 'TERM' ELSE `insurance_type` END, `provider`, `policy_name`, "
               "`policy_number`, NULL, `sum_assured`, `premium_amount`, `premium_frequency_months`, "
               "CAST(substr(`next_due_date`, 9, 2) AS INTEGER), CAST(substr(`next_due_date`, 6, 2) AS INTEGER), `start_date`, `maturity_date` "
               "FROM `insurance_policies`")
        self.x("DROP TABLE `insurance_policies`")
        self.x("ALTER TABLE `insurance_policies_new` RENAME TO `insurance_policies`")
        self.x("CREATE TABLE IF NOT EXISTS `validity_periods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `instrument_id` INTEGER NOT NULL, "
               "`kind` TEXT NOT NULL, `start_date` TEXT, `end_date` TEXT NOT NULL, `transaction_id` INTEGER, `provider` TEXT, `reference` TEXT, "
               "FOREIGN KEY(`instrument_id`) REFERENCES `instruments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
               "FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        self.x("CREATE INDEX IF NOT EXISTS `index_validity_periods_instrument_id_kind` ON `validity_periods` (`instrument_id`, `kind`)")
        self.x("CREATE INDEX IF NOT EXISTS `index_validity_periods_transaction_id` ON `validity_periods` (`transaction_id`)")
        self.x("UPDATE room_master_table SET identity_hash = ?", SCHEMA_V7_IDENTITY_HASH)
        self.x("PRAGMA user_version = 7")

    def migrate_7_to_8(self, report):
        """The app's Migrations.MIGRATION_7_8: a validity period carries the sum insured a health premium bought."""
        report.add("- Input was schema version 7: applied the app's 7 -> 8 migration (sum insured on validity periods).")
        self.x("ALTER TABLE `validity_periods` ADD COLUMN `sum_insured` REAL")
        self.x("UPDATE room_master_table SET identity_hash = ?", SCHEMA_V8_IDENTITY_HASH)
        self.x("PRAGMA user_version = 8")

    # --- balance fingerprint: the backfill must not move money in or out of any account
    def account_fingerprint(self):
        rows = self.q("""
            SELECT a.id, a.name,
              ROUND(COALESCE(SUM(CASE WHEN t.to_account_id = a.id THEN t.amount ELSE 0 END), 0)
                  - COALESCE(SUM(CASE WHEN t.from_account_id = a.id THEN t.amount ELSE 0 END), 0), 2) AS net
            FROM accounts a LEFT JOIN transactions t ON t.from_account_id = a.id OR t.to_account_id = a.id
            GROUP BY a.id""")
        return {r["id"]: (r["name"], r["net"]) for r in rows}

    def insert_transaction(self, date, kind, amount, particulars, instrument_id, from_account=None, to_account=None,
                           contributor=None, now=None):
        now = now or now_iso()
        cur = self.x("""INSERT INTO transactions (created_time, modified_time, date, kind, from_account_id, to_account_id,
                            category_id, instrument_id, particulars, quantity, rate, amount, currency, fx_rate, ex_date,
                            contributor, broker_ref, hidden, include_in_statistics)
                        VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, NULL, NULL, ?, 'INR', NULL, NULL, ?, NULL, 0, 1)""",
                     now, now, date.isoformat(), kind, from_account, to_account, instrument_id, particulars, amount, contributor)
        return cur.lastrowid

    def insert_instrument(self, name, itype, category_id, active=True, notes=None, owner="Self", now=None):
        now = now or now_iso()
        order = (self.one("SELECT COALESCE(MAX(display_order), 0) FROM instruments")[0] or 0) + 1
        cur = self.x("""INSERT INTO instruments (name, short_name, instrument_type, isin, symbol, exchange, currency, category_id,
                            owner, active, display_order, notes, created_time, modified_time)
                        VALUES (?, NULL, ?, NULL, NULL, NULL, 'INR', ?, ?, ?, ?, ?, ?, ?)""",
                     name, itype, category_id, owner, 1 if active else 0, order, notes, now, now)
        return cur.lastrowid


# ---------------------------------------------------------------------------------------------- categories

# Income categories the 4.x import does not create (it has only "Interest & Dividends"): added top-level, scope INCOME.
INCOME_CATEGORIES = ("Salary", "Cashback", "Refunds", "Reimbursements", "Others")
# Categories for both kinds: existing expense ones that take income too (money paid back by the people they cover)
# are widened to BOTH, the rest are added with scope BOTH.
SHARED_CATEGORIES = ("Family", "Friends and Relatives", "Loans & Lends")


def seed_income_categories(db, report):
    """Adds the income categories and widens the shared ones; idempotent, names matched ignoring case."""
    existing = {r["name"].strip().lower(): r for r in db.q("SELECT id, name, applies_to, deleted FROM categories")}
    order = (db.one("SELECT COALESCE(MAX(display_order), 0) FROM categories")[0] or 0) + 1
    added, widened = [], []
    for name in INCOME_CATEGORIES:
        row = existing.get(name.lower())
        if row is None:
            db.x("INSERT INTO categories (name, parent_id, applies_to, icon, color, display_order, deleted) VALUES (?, NULL, 'INCOME', NULL, NULL, ?, 0)",
                 name, order)
            order += 1
            added.append(name)
        elif row["applies_to"] == "EXPENSE":
            db.x("UPDATE categories SET applies_to = 'BOTH', deleted = 0 WHERE id = ?", row["id"])
            widened.append(row["name"])
    for name in SHARED_CATEGORIES:
        row = existing.get(name.lower())
        if row is None:
            db.x("INSERT INTO categories (name, parent_id, applies_to, icon, color, display_order, deleted) VALUES (?, NULL, 'BOTH', NULL, NULL, ?, 0)",
                 name, order)
            order += 1
            added.append(name)
        elif row["applies_to"] != "BOTH":
            db.x("UPDATE categories SET applies_to = 'BOTH' WHERE id = ?", row["id"])
            widened.append(row["name"])
    if added:
        report.add(f"- Categories added: {', '.join(added)}.")
    if widened:
        report.add(f"- Categories now applying to income and expenses alike: {', '.join(widened)}.")
    if not added and not widened:
        report.add("- Income categories already present; kept.")


# Keyword rules filing the 4.x income rows that have no category (first match wins; particulars matched ignoring case).
# Money lent to us and money paid back to us both go to Loans & Lends; Family is the parents only (Appa, Amma), the
# wider family (Doddamma, Atya, Mama, ...) counts as Friends and Relatives. Whatever no rule matches goes to Others
# (EPF contributions among them, to be sorted out later), so no income row is left without a category.
INCOME_RULES = (
    ("Reimbursements", r"reimburs|iisc gave|cisco paid"),
    ("Refunds", r"refund|returned"),
    ("Cashback", r"cash ?back|reward points|promo|offer"),
    ("Salary", r"salary|stipend|scholarship|bonus|arrears"),
    ("Loans & Lends", r"lent( to)? me|\(lent\)|have to return|loan|borrow|paid( me)? back|(gave|sent|have) back|contributed|\bpaid\b"),
    ("Family", r"\b(appa|amma)\b"),
    ("Friends and Relatives", r"\b(doddamma|atya|mama|kakka|ajji|dada)\b|gave|to me|sent|gift"),
    ("Others", r""),
)


def assign_income_categories(db, report, apply):
    """Previews (and with [apply] writes) the INCOME_RULES on the uncategorised income rows; the report gets the table."""
    import re
    cats = {r["name"].lower(): r["id"] for r in db.q("SELECT id, name FROM categories WHERE deleted = 0")}
    rows = db.q("SELECT id, particulars, amount FROM transactions WHERE kind = 'INCOME' AND category_id IS NULL")
    rules = [(name, re.compile(pat, re.IGNORECASE)) for name, pat in INCOME_RULES]
    by_rule = {name: [] for name, _ in INCOME_RULES}
    unmatched = []
    for r in rows:
        text = r["particulars"] or ""
        for name, rx in rules:
            if rx.search(text):
                by_rule[name].append(r)
                break
        else:
            unmatched.append(r)
    lines = ["", f"### Income categories by keyword ({'applied' if apply else 'preview only'})", "",
             "| Category | Rule | Rows | Total | Most frequent particulars |", "|---|---|---:|---:|---|"]
    for name, pat in INCOME_RULES:
        hits = by_rule[name]
        if not hits:
            continue
        freq = {}
        for r in hits:
            freq[r["particulars"]] = freq.get(r["particulars"], 0) + 1
        top = ", ".join(f"{k} ({v})" for k, v in sorted(freq.items(), key=lambda kv: -kv[1])[:6])
        lines.append(f"| {name} | {'(everything else)' if not pat else '`' + pat + '`'} | {len(hits)} | {sum(r['amount'] for r in hits):,.0f} | {top} |")
    lines.append(f"| (no rule) | | {len(unmatched)} | {sum(r['amount'] for r in unmatched):,.0f} | |")
    if unmatched:
        freq = {}
        for r in unmatched:
            k = r["particulars"]
            n, total = freq.get(k, (0, 0.0))
            freq[k] = (n + 1, total + r["amount"])
        lines += ["", "Left without a category:", ""]
        lines += [f"- {k} × {n}, {total:,.0f}" for k, (n, total) in sorted(freq.items(), key=lambda kv: -kv[1][0])]
    report.extra_sections.append("\n".join(lines))
    if apply:
        missing = [name for name, _ in INCOME_RULES if name.lower() not in cats and by_rule[name]]
        if missing:
            report.problem(f"Income rules name categories that do not exist: {', '.join(missing)}")
            return
        for name, hits in by_rule.items():
            for r in hits:
                db.x("UPDATE transactions SET category_id = ? WHERE id = ?", cats[name.lower()], r["id"])
        report.add(f"- Income rows filed by keyword: {sum(len(h) for h in by_rule.values())} of {len(rows)} ({len(unmatched)} left without a category).")
    else:
        report.add(f"- Income rows the keyword rules would file: {sum(len(h) for h in by_rule.values())} of {len(rows)} (preview; pass --assign-income to apply).")


def seed_categories(db, report):
    if db.one("SELECT COUNT(*) FROM risk_levels")[0] == 0:
        for i, name in enumerate(RISK_LEVELS, 1):
            db.x("INSERT INTO risk_levels (name, display_order) VALUES (?, ?)", name, i)
    risk = {r["name"]: r["id"] for r in db.q("SELECT id, name FROM risk_levels")}
    if db.one("SELECT COUNT(*) FROM investment_categories")[0] == 0:
        order = 1
        for name, r, children in TREE:
            cur = db.x("INSERT INTO investment_categories (name, parent_id, risk_level_id, display_order) VALUES (?, NULL, ?, ?)",
                       name, risk.get(r), order)
            order += 1
            parent = cur.lastrowid
            for cname, cr in children:
                db.x("INSERT INTO investment_categories (name, parent_id, risk_level_id, display_order) VALUES (?, ?, ?, ?)",
                     cname, parent, risk.get(cr), order)
                order += 1
        report.add("- Investment categories were empty: seeded the app's tree.")
    else:
        report.add("- Investment categories already seeded by the app; kept.")
    return risk


def category_index(db):
    """(parent name or None, name) -> id."""
    cats = db.q("SELECT c.id, c.name, p.name AS parent FROM investment_categories c LEFT JOIN investment_categories p ON p.id = c.parent_id")
    return {(r["parent"], r["name"]): r["id"] for r in cats}


def ensure_child(db, parent_name, name, risk_level_id, report, reason):
    idx = category_index(db)
    if (parent_name, name) in idx:
        return idx[(parent_name, name)]
    parent_id = idx[(None, parent_name)]
    order = (db.one("SELECT COALESCE(MAX(display_order), 0) FROM investment_categories")[0] or 0) + 1
    cur = db.x("INSERT INTO investment_categories (name, parent_id, risk_level_id, display_order) VALUES (?, ?, ?, ?)",
               name, parent_id, risk_level_id, order)
    report.add(f"- Added category '{parent_name} > {name}' ({reason}).")
    return cur.lastrowid


def apply_targets(db, sheet, report):
    rows = sheet.investment_summary()
    idx = category_index(db)
    parents = [n for n, _, _ in TREE]
    parent_of_child = {c: p for p, _, ch in TREE for c, _ in ch}
    goal = None
    applied = 0
    for r in rows:
        name = str(r["Name"]).strip()
        if name == "Amount needed for Goal":
            goal = as_float(r.get("Amount Invested"))
            continue
        target = None
        if name in SUMMARY_PARENTS:
            target = idx.get((None, SUMMARY_PARENTS[name]))
            leaf = False
        elif name in parents:
            target = idx.get((None, name))
            leaf = True
        elif name in parent_of_child:
            target = idx.get((parent_of_child[name], name))
            leaf = True
        if target is None:
            continue
        tol = as_float(r.get("Tolerance Band"))
        notes = (r.get("Rebalance Notes") or None)
        if leaf:
            db.x("UPDATE investment_categories SET tolerance_pct = ?, target_goal_pct = ?, target_wealth_pct = ?, rebalance_notes = ? WHERE id = ?",
                 tol, as_float(r.get("Target (Goal)")), as_float(r.get("Target (Wealth)")), notes, target)
        else:  # the app derives a parent's goal and wealth shares from its children
            db.x("UPDATE investment_categories SET tolerance_pct = ?, rebalance_notes = ? WHERE id = ?", tol, notes, target)
        applied += 1
    if goal is not None:
        db.x("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)", GOAL_AMOUNT_KEY, repr(goal))
    report.add(f"- Targets, tolerance bands and notes written for {applied} categories; goal amount {'set to ' + plain(goal) if goal else 'not found in the sheet'}.")


def assign_instrument_categories(db, sheet, risk, report):
    idx = category_index(db)
    stock_types = sheet.stock_types()
    fund_types = sheet.fund_types()
    unassigned = []
    for inst in db.q("SELECT id, name, instrument_type FROM instruments"):
        t = inst["instrument_type"]
        cat = None
        if t == "STOCK":
            st = stock_types.get(inst["name"])
            if st:
                cat = idx.get(("Stocks", st)) or ensure_child(db, "Stocks", st, risk["Medium risk"], report, "stock type used in the sheet")
                idx = category_index(db)
        elif t == "MUTUAL_FUND":
            ft = fund_types.get(inst["name"])
            if ft:
                cat = idx.get(("Mutual Funds", ft)) or ensure_child(db, "Mutual Funds", ft, risk["Medium risk"], report,
                                                                     "fund type used in the sheet; risk level set to the parent's, adjust under Manage")
                idx = category_index(db)
        elif t == "FIXED_DEPOSIT":
            cat = idx[(None, "Fixed Deposits")]
        elif t == "INSURANCE":
            cat = idx[(None, "Life Insurance")]
        elif t == "OTHER":
            cat = idx[(None, "Others")]
        if cat is None:
            unassigned.append(f"{inst['name']} ({t})")
            continue
        db.x("UPDATE instruments SET category_id = ? WHERE id = ?", cat, inst["id"])
    # "Others" lines of the summary that are not the generic one become sub-categories, matched to instruments by a shared word
    for r in others_lines(sheet, include_generic=False):
        line = str(r["Name"]).strip()
        words = {w.lower() for w in re.findall(r"[A-Za-z]{4,}", line)} - {"saved", "account"}
        targets = [i for i in db.q("SELECT id, name FROM instruments WHERE instrument_type = 'OTHER'")
                   if words & {w.lower() for w in re.findall(r"[A-Za-z]{4,}", i["name"])}]
        if not targets:
            report.add(f"- Summary line '{line}' matched no 'Other' instrument; skipped.")
            continue
        cid = ensure_child(db, "Others", line, risk["No risk"], report, "line of the Investment Summary")
        db.x("UPDATE investment_categories SET target_goal_pct = ?, target_wealth_pct = ?, tolerance_pct = ? WHERE id = ?",
             as_float(r.get("Target (Goal)")), as_float(r.get("Target (Wealth)")), as_float(r.get("Tolerance Band")), cid)
        for i in targets:
            db.x("UPDATE instruments SET category_id = ? WHERE id = ?", cid, i["id"])
    report.add(f"- Instruments assigned to categories; {len(unassigned)} left without a sheet type: {', '.join(unassigned) or 'none'}.")


# ------------------------------------------------------------------------------------------ fixed deposits

def others_lines(sheet, include_generic):
    """The Investment Summary lines between 'Life Insurance' and the risk totals: 'Others' and its named siblings."""
    out = []
    seen_life = False
    for r in sheet.investment_summary():
        name = str(r["Name"]).strip()
        if name == "Life Insurance":
            seen_life = True
            continue
        if seen_life and name.startswith("No Risk"):
            break
        if seen_life and (include_generic or name != "Others"):
            out.append(r)
    return out


def bank_accounts(db):
    out = {}
    accounts = db.q("SELECT id, name FROM accounts WHERE account_type = 'BANK' AND deleted = 0")
    for bank, hint in BANK_ACCOUNT_HINTS.items():
        match = [a for a in accounts if hint.lower() in a["name"].lower() and not a["name"].lower().startswith("amma")]
        if match:
            out[bank] = match[0]["id"]
    return out


def backfill_fixed_deposits(db, sheet, report, extras=(), chronological=True, sweep_plan=None):
    fds = sheet.fixed_deposits() + list(extras)
    name_deposits(fds, chronological, report)
    cat_fd = category_index(db)[(None, "Fixed Deposits")]
    unmapped = db.one("SELECT id FROM instruments WHERE instrument_type = 'FIXED_DEPOSIT' AND name = ?", UNMAPPED_FD_NAME)
    if unmapped is None:
        report.problem(f"No instrument named '{UNMAPPED_FD_NAME}': the deposits were already split, nothing to do.")
        return
    unmapped = unmapped["id"]
    sweep = db.one("SELECT id FROM instruments WHERE instrument_type = 'FIXED_DEPOSIT' AND name LIKE '%Sweep%'")
    sweep = sweep["id"] if sweep else None
    banks = bank_accounts(db)
    now = now_iso()

    def ledger(kind, inst):
        return [dict(r) for r in db.q("SELECT id, date, kind, from_account_id, to_account_id, amount, particulars FROM transactions "
                                      "WHERE instrument_id = ? AND kind = ? ORDER BY date, id", inst, kind)]

    invests = ledger("INVEST", unmapped)
    redeems = ledger("REDEEM", unmapped)
    incomes = ledger("INCOME", unmapped)
    used = set()

    def take(rows, date, amount, tolerance_days, account=None):
        best = None
        for r in rows:
            if r["id"] in used or abs(r["amount"] - amount) > 1.0:
                continue
            delta = abs((dt.date.fromisoformat(r["date"]) - date).days)
            if delta > tolerance_days:
                continue
            key = (0 if account is None or r.get("from_account_id") == account or r.get("to_account_id") == account else 1, delta)
            if best is None or key < best[0]:
                best = (key, r)
        if best:
            used.add(best[1]["id"])
            return best[1]
        return None

    by_id = {f["id"]: f for f in fds}
    # 1. deposits that went through a bank account
    for f in fds:
        f["invest_row"] = None
        f["instrument"] = None
        if f["action"] == "Closed with Sweep-In":
            continue
        acc = banks.get(f["bank"])
        f["invest_row"] = take(invests, f["date"], f["amount"], 3, acc) if f["amount"] > 0 else None

    # 2. renewal chains: a renewed deposit's credited amount reappears as a deposit without a bank row on the maturity date
    successors = {}
    for f in sorted(fds, key=sheet_order):
        if f["action"] != "Renewed":
            continue
        cands = [g for g in sorted(fds, key=sheet_order)
                 if g["invest_row"] is None and g["id"] not in successors.values() and g["action"] != "Closed with Sweep-In"
                 and g["date"] and f["maturity"] and abs((g["date"] - f["maturity"]).days) <= 3
                 and f["credited"] is not None and abs(g["amount"] - (f.get("renewed") or f["credited"])) <= 1.0]
        if f["next"] in by_id and by_id[f["next"]] in cands:
            cands = [by_id[f["next"]]]
        if not cands:
            report.problem(f"{f['name']} is marked Renewed but no deposit of {plain(f['credited'] or 0)} on {f['maturity']} was found to renew into.")
            continue
        successors[f["id"]] = cands[0]["id"]
    predecessors = {v: k for k, v in successors.items()}

    # 3. create the instruments and terms
    status_of = {"": "ACTIVE", "Closed at Maturity": "CLOSED_AT_MATURITY", "Renewed": "RENEWED", "Closed Prematurely": "CLOSED_PREMATURELY"}
    created = 0
    for f in fds:
        if f["action"] == "Closed with Sweep-In":
            continue
        status = status_of.get(f["action"])
        if status is None:
            report.problem(f"{f['name']}: unknown action '{f['action']}'.")
            continue
        notes = []
        if f["notes"]:
            notes.append(str(f["notes"]))
        if f["tds_rate_text"] not in (None, "") and not isinstance(f["tds_rate_text"], (int, float)):
            notes.append(f"TDS rate in the sheet: {f['tds_rate_text']}")
        if f["next"] and f["id"] not in successors:
            notes.append(f"Sheet says next deposit: {by_id[f['next']]['name'] if f['next'] in by_id else 'FD #' + str(f['next'])}")
        inst = db.insert_instrument(f["name"], "FIXED_DEPOSIT", cat_fd, active=(status == "ACTIVE"),
                                    notes="\n".join(notes) or None, now=now)
        f["instrument"] = inst
        created += 1
    for f in fds:
        if f["instrument"] is None:
            continue
        prev = predecessors.get(f["id"])
        nxt = successors.get(f["id"])
        db.x("""INSERT INTO fixed_deposits (instrument_id, certificate_no, holder_name, bank_name, branch, bank_account_no, cust_id,
                    period_months, period_days, rate, compounding_months, maturity_date, maturity_amount_printed, tds_rate,
                    tds_reported, status, previous_fd_id, next_fd_id, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)""",
             f["instrument"], str(f["certificate"]) if f["certificate"] else None, f["holder"], f["bank"], f["branch"],
             str(f["account_no"]) if f["account_no"] else None, str(f["cust_id"]) if f["cust_id"] else None,
             int(f["months"]) if f["months"] else None, int(f["days"]) if f["days"] else None, f["rate"],
             int(f["compounding"]) if f["compounding"] else 3, f["maturity"].isoformat() if f["maturity"] else None,
             f["printed"], f["tds_rate"], f["tds_reported"], status_of[f["action"]],
             by_id[prev]["instrument"] if prev else None, by_id[nxt]["instrument"] if nxt else None)

    # 4. move or create the ledger rows
    moved = inserted = 0
    for f in fds:
        if f["instrument"] is None:
            continue
        inst = f["instrument"]
        terms = f"{f['bank'] or 'FD'} {period_label(f['months'], f['days'])} @ {plain(f['rate'])}%" if f["rate"] else (f["bank"] or "FD")
        if f["invest_row"]:
            db.x("UPDATE transactions SET instrument_id = ?, modified_time = ? WHERE id = ?", inst, now, f["invest_row"]["id"])
            moved += 1
        elif f["id"] in predecessors:
            prev = by_id[predecessors[f["id"]]]
            db.insert_transaction(f["date"], "INVEST", f["amount"], f"Renewed from {prev['name']} · {terms}", inst, now=now)
            inserted += 1
        else:
            db.insert_transaction(f["date"], "INVEST", f["amount"], f"Fixed deposit opened · {terms} (before the ledger tracked deposits)", inst, now=now)
            inserted += 1
            report.add(f"- {f['name']}: no deposit row in the ledger for {plain(f['amount'])} on {f['date']}; added one without a bank account.")
        if f["action"] in ("Closed at Maturity", "Closed Prematurely"):
            early = f["action"] == "Closed Prematurely"
            closed_on = (f.get("closed_on") if early else None) or f["maturity"]
            cred = f["credited"] if f["credited"] is not None else f["printed"]
            booked = f.get("booked_as")
            row = take(redeems, closed_on, booked or cred, 7) if cred else None
            if row:
                db.x("UPDATE transactions SET instrument_id = ?, modified_time = ? WHERE id = ?", inst, now, row["id"])
                moved += 1
                if booked and abs(booked - cred) > 0.005:
                    db.x("UPDATE transactions SET amount = ?, modified_time = ? WHERE id = ?", cred, now, row["id"])
                    report.add(f"- {f['name']}: the ledger's closure row of {plain(booked)} is cut to {plain(cred)}; the {plain(booked - cred)} of interest it lumped in is booked on the renewal dates instead.")
            else:
                # the ledger may have booked the maturity as plain income into a bank (deposits made before it tracked them)
                alt = db.one("SELECT id, to_account_id, date FROM transactions WHERE instrument_id IS NULL AND kind = 'INCOME' AND to_account_id IS NOT NULL "
                             "AND ABS(amount - ?) < 1 AND date BETWEEN ? AND ? ORDER BY date LIMIT 1",
                             cred, closed_on.isoformat(), (closed_on + dt.timedelta(days=60)).isoformat()) if cred else None
                if alt:
                    db.x("UPDATE transactions SET kind = 'REDEEM', instrument_id = ?, particulars = ?, modified_time = ? WHERE id = ?",
                         inst, "Fixed deposit closed before maturity" if early else "Fixed deposit matured", now, alt["id"])
                    moved += 1
                    report.add(f"- {f['name']}: {'closure' if early else 'maturity'} found as an income row of {plain(cred)} on {alt['date']}; turned into the deposit's {'closure' if early else 'maturity'} (same account, same balance).")
                else:
                    report.problem(f"{f['name']}: no {'closure' if early else 'maturity'} row of {plain(cred or 0)} around {closed_on} in the ledger.")
        elif f["action"] == "Renewed":
            nxt = by_id[successors[f["id"]]]
            cred = f["credited"] if f["credited"] is not None else f["printed"]
            db.insert_transaction(nxt["date"], "REDEEM", nxt["amount"], f"Renewed into {nxt['name']}", inst, now=now)
            inserted += 1
            remainder = (cred or 0) - nxt["amount"]
            if remainder > 0.5:
                principal_only = f.get("renewed") is not None
                db.insert_transaction(nxt["date"], "REDEEM", remainder,
                                      f"Interest paid out, principal renewed into {nxt['name']}" if principal_only else "Fixed deposit matured, balance paid out",
                                      inst, to_account=banks.get(f["bank"]), now=now)
                inserted += 1
                report.add(f"- {f['name']}: {plain(remainder)} of the matured amount was not renewed; booked as paid out to the {f['bank']} account.")

    # 5. sweep-in deposits: the sheet carries them at zero; the ledger has every partial liquidation under one
    #    instrument. The --sweep-in file says which liquidation belongs to which deposit; each becomes a deposit of
    #    its own, closed before maturity on its last liquidation, with the interest row beside each liquidation
    #    telling the principal it took out.
    sweep_rows = [f for f in fds if f["action"] == "Closed with Sweep-In"]
    sweep_source = [unmapped] + ([sweep] if sweep else [])
    if sweep_rows and not sweep_plan:
        report.problem(f"{len(sweep_rows)} sheet rows are 'Closed with Sweep-In' but no --sweep-in file was given.")
    elif sweep_rows:
        marks = ",".join("?" * len(sweep_source))
        rows_by_id = {r["id"]: dict(r) for r in db.q(
            f"SELECT id, date, kind, from_account_id, to_account_id, instrument_id, amount, particulars FROM transactions WHERE instrument_id IN ({marks})", *sweep_source)}
        owner_of = {}
        for f in sorted(sweep_rows, key=sheet_order):
            plan = sweep_plan.get(f["id"]) if isinstance(f["id"], int) else None
            if plan is None:
                report.problem(f"{f['name']} (sheet FD #{f['id']}) is 'Closed with Sweep-In' but the --sweep-in file has no entry for it.")
                continue
            pool = [r for r in rows_by_id.values() if r["kind"] == "INVEST" and r["id"] not in used]
            row = take(pool, f["date"], plan["amount"], 3, banks.get(f["bank"])) if plan["amount"] else next(
                (r for r in sorted(pool, key=lambda r: r["id"]) if abs((dt.date.fromisoformat(r["date"]) - f["date"]).days) <= 3), None)
            if row is None:
                report.problem(f"{f['name']}: no deposit row near {f['date']} for the sweep-in deposit.")
                continue
            used.add(row["id"])
            f["amount"] = row["amount"]
            f["invest_row"] = row
            notes = [str(f["notes"])] if f["notes"] else []
            inst = db.insert_instrument(f["name"], "FIXED_DEPOSIT", cat_fd, active=False, notes="\n".join(notes) or None, now=now)
            f["instrument"] = inst
            created += 1
            db.x("""INSERT INTO fixed_deposits (instrument_id, certificate_no, holder_name, bank_name, branch, bank_account_no, cust_id,
                        period_months, period_days, rate, compounding_months, maturity_date, tds_rate, status, deposit_type, sweep_in_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CLOSED_PREMATURELY', 'SWEEP_IN', ?)""",
                 inst, str(f["certificate"]) if f["certificate"] else None, f["holder"], f["bank"], f["branch"],
                 str(f["account_no"]) if f["account_no"] else None, str(f["cust_id"]) if f["cust_id"] else None,
                 int(f["months"]) if f["months"] else None, int(f["days"]) if f["days"] else None, f["rate"],
                 int(f["compounding"]) if f["compounding"] else 3, f["maturity"].isoformat() if f["maturity"] else None,
                 f["tds_rate"], plan["converted_on"].isoformat() if plan["converted_on"] else None)
            db.x("UPDATE transactions SET instrument_id = ?, modified_time = ? WHERE id = ?", inst, now, row["id"])
            moved += 1
            for lid in plan["liquidations"]:
                r = rows_by_id.get(lid)
                if r is None or r["kind"] != "REDEEM":
                    report.problem(f"{f['name']}: liquidation row {lid} is not a payout row under the deposit instruments.")
                    continue
                if lid in owner_of:
                    report.problem(f"{f['name']}: liquidation row {lid} is also listed under {owner_of[lid]['name']}.")
                    continue
                owner_of[lid] = f
                db.x("UPDATE transactions SET instrument_id = ?, modified_time = ? WHERE id = ?", inst, now, lid)
                moved += 1
        # the walk: pair every liquidation with its interest row, then run the principal of each deposit down to zero
        liquidations = sorted((rows_by_id[i] for i in owner_of), key=lambda r: (r["date"], r["id"]))
        interests = [r for r in rows_by_id.values() if r["kind"] == "INCOME" and r["from_account_id"] is None and r["to_account_id"] is None]
        sweep_interests = [r for r in interests if "sweep" in (r["particulars"] or "").lower()]
        if sweep_interests:
            interests = sweep_interests
        interest_of = {}
        for day in sorted({r["date"] for r in liquidations}):
            pairs = pair_interest([r for r in liquidations if r["date"] == day], [r for r in interests if r["date"] == day])
            if pairs is None:
                report.problem(f"Sweep-in: the interest rows of {day} cannot be paired with that day's liquidations.")
                continue
            interest_of.update(pairs)
        remaining = {f["name"]: f["amount"] for f in sweep_rows if f["instrument"]}
        table = ["", "### Sweep-in liquidations", "", "| Date | Row | Paid out | Interest | Principal | Deposit | Left in it |", "|---|---|---:|---:|---:|---|---:|"]
        split = 0
        for r in liquidations:
            f = owner_of[r["id"]]
            interest = interest_of.get(r["id"])
            principal = round(r["amount"] - (interest["amount"] if interest else 0.0), 2)
            remaining[f["name"]] = round(remaining[f["name"]] - principal, 2)
            table.append(f"| {r['date']} | {r['id']} | {plain(r['amount'])} | {plain(interest['amount']) if interest else '—'} | {plain(principal)} | {f['name']} | {plain(remaining[f['name']])} |")
            if remaining[f["name"]] < -0.5:
                report.problem(f"{f['name']}: the liquidations listed for it take out more than it held (row {r['id']}).")
            if interest:
                # Two rows, as on the bank statement: the principal comes back on the payout row, the interest row
                # beside it becomes a credit to the same account under the deposit (the 4.x row had no account).
                db.x("UPDATE transactions SET amount = ?, rate = ?, modified_time = ? WHERE id = ?", principal, principal, now, r["id"])
                db.x("UPDATE transactions SET to_account_id = ?, instrument_id = ?, modified_time = ? WHERE id = ?",
                     r["to_account_id"], f["instrument"], now, interest["id"])
                split += 1
        # TDS the bank charged on that interest: an expense from the same account on the day of a liquidation
        tds_linked = 0
        for t in db.q("SELECT id, date, from_account_id, amount FROM transactions WHERE kind = 'EXPENSE' AND instrument_id IS NULL "
                      "AND particulars LIKE '%TDS%' AND particulars LIKE '%Sweep%' ORDER BY date, id"):
            same_day = [r for r in liquidations if r["date"] == t["date"] and r["to_account_id"] == t["from_account_id"]]
            if not same_day:
                continue
            by_deposit = {}
            for r in same_day:
                i = interest_of.get(r["id"])
                by_deposit[owner_of[r["id"]]["instrument"]] = by_deposit.get(owner_of[r["id"]]["instrument"], 0.0) + (i["amount"] if i else 0.0)
            inst = min(by_deposit, key=lambda k: abs(by_deposit[k] * 0.1 - t["amount"]))
            db.x("UPDATE transactions SET instrument_id = ?, modified_time = ? WHERE id = ?", inst, now, t["id"])
            tds_linked += 1
        report.add(f"- Sweep-in liquidations split as on the bank statement: {split} interest rows kept as credits to the account under their deposit "
                   f"(the payout rows keep the principal); {tds_linked} TDS rows linked to their deposit.")
        for name, left in remaining.items():
            if abs(left) > 0.5:
                report.problem(f"{name}: {plain(left)} of principal is left after its last liquidation; the --sweep-in assignment is incomplete.")
        unassigned = [r for r in rows_by_id.values() if r["kind"] == "REDEEM" and r["id"] not in owner_of and "sweep" in (r["particulars"] or "").lower()]
        for r in unassigned:
            report.problem(f"Sweep-in liquidation row {r['id']} ({plain(r['amount'])} on {r['date']}) is not assigned to any deposit.")
        report.add(f"- {len(sweep_rows)} sweep-in deposits recorded as deposits of their own, closed on their last liquidation; "
                   f"{len(liquidations)} liquidations assigned from the --sweep-in file (walk below).")
        report.extra_sections.extend(table)

    # 6. the 4.x "interest credited" rows duplicate interest that the matured amount already carries; drop them
    fd_ids = [unmapped] + ([sweep] if sweep else []) + [f["instrument"] for f in fds if f["instrument"]]
    marks = ",".join("?" * len(fd_ids))
    dup = db.one(f"SELECT COUNT(*), COALESCE(SUM(amount), 0) FROM transactions WHERE kind = 'INCOME' AND to_account_id IS NULL AND from_account_id IS NULL AND instrument_id IN ({marks})", *fd_ids)
    db.x(f"DELETE FROM transactions WHERE kind = 'INCOME' AND to_account_id IS NULL AND from_account_id IS NULL AND instrument_id IN ({marks})", *fd_ids)
    report.add(f"- Removed {dup[0]} 'interest credited' rows totalling {plain(dup[1])} (no account; the interest stays as each deposit's realised profit).")

    if sweep:
        left = db.one("SELECT COUNT(*) FROM transactions WHERE instrument_id = ?", sweep)[0]
        if left:
            report.problem(f"{left} rows are still under the ledger's sweep-in instrument.")
        else:
            db.x("DELETE FROM fixed_deposits WHERE instrument_id = ?", sweep)
            db.x("DELETE FROM instruments WHERE id = ?", sweep)

    # 7. whatever is left under the unmapped instrument becomes deposits of its own
    leftovers = [dict(r) for r in db.q("SELECT id, date, kind, amount, particulars FROM transactions WHERE instrument_id = ? ORDER BY date, id", unmapped)]
    extra = 0
    current = None
    for r in leftovers:
        if r["kind"] == "INVEST":
            m = re.search(r"(\d+)\s*(Day|Month)", r["particulars"] or "", re.I)
            months = int(m.group(1)) if m and m.group(2).lower().startswith("month") else None
            days = int(m.group(1)) if m and m.group(2).lower().startswith("day") else None
            current = db.insert_instrument(f"FD (not in the sheet) {r['date']}", "FIXED_DEPOSIT", cat_fd, active=True,
                                           notes="Deposit found in the ledger but not in the Fixed Deposits sheet.", now=now)
            db.x("INSERT INTO fixed_deposits (instrument_id, period_months, period_days, compounding_months, status) VALUES (?, ?, ?, 3, 'ACTIVE')",
                 current, months, days)
            extra += 1
        if current is None:
            report.problem(f"Ledger row {r['id']} ({r['kind']} {plain(r['amount'])} on {r['date']}) under '{UNMAPPED_FD_NAME}' has no deposit to belong to.")
            continue
        db.x("UPDATE transactions SET instrument_id = ?, modified_time = ? WHERE id = ?", current, now, r["id"])
    for inst in db.q("SELECT i.id FROM instruments i WHERE i.name LIKE 'FD (not in the sheet)%'"):
        net = db.one("SELECT COALESCE(SUM(CASE kind WHEN 'INVEST' THEN amount WHEN 'REDEEM' THEN -amount ELSE 0 END), 0) FROM transactions WHERE instrument_id = ?", inst["id"])[0]
        if net <= 0.5:
            db.x("UPDATE fixed_deposits SET status = 'CLOSED_PREMATURELY' WHERE instrument_id = ?", inst["id"])
            db.x("UPDATE instruments SET active = 0 WHERE id = ?", inst["id"])
    if extra:
        report.add(f"- {extra} deposits found in the ledger but not in the sheet; kept as 'FD (not in the sheet) <date>'.")

    remaining = db.one("SELECT COUNT(*) FROM transactions WHERE instrument_id = ?", unmapped)[0]
    if remaining:
        report.problem(f"{remaining} rows still under '{UNMAPPED_FD_NAME}'.")
    else:
        db.x("UPDATE templates SET instrument_id = NULL WHERE instrument_id = ?", unmapped)
        db.x("DELETE FROM fixed_deposits WHERE instrument_id = ?", unmapped)
        db.x("DELETE FROM instruments WHERE id = ?", unmapped)
    n_extra = sum(1 for f in fds if f["extra"] and f["instrument"])
    report.add(f"- Fixed deposits: {created} instruments created ({n_extra} of them from the extra file), {moved} ledger rows moved, {inserted} rows added for renewals and untracked deposits.")

    # 8. checks against the sheet
    sheet_active = sum(f["amount"] for f in fds if f["action"] == "")
    db_active = db.one("SELECT COALESCE(SUM(CASE kind WHEN 'INVEST' THEN amount WHEN 'REDEEM' THEN -amount ELSE 0 END), 0) "
                       "FROM transactions WHERE instrument_id IN (SELECT instrument_id FROM fixed_deposits WHERE status = 'ACTIVE')")[0]
    report.add(f"- Open deposits: sheet {plain(sheet_active)} vs database {plain(db_active)}" + (" ✓" if abs(sheet_active - db_active) < 1 else " ✗"))
    if abs(sheet_active - db_active) >= 1:
        report.problem("Open deposit principal differs from the sheet.")
    sheet_interest = sum((f["interest"] or 0) for f in fds if f["action"] in ("Closed at Maturity", "Renewed") and not f["extra"])
    closed = [f["instrument"] for f in fds if f["instrument"] and f["action"] in ("Closed at Maturity", "Renewed") and not f["extra"]]
    db_interest = db.one("SELECT COALESCE(SUM(CASE kind WHEN 'REDEEM' THEN amount WHEN 'INVEST' THEN -amount ELSE 0 END), 0) "
                         f"FROM transactions WHERE instrument_id IN ({','.join('?' * len(closed))})", *closed)[0] if closed else 0.0
    report.add(f"- Interest realised on the sheet's closed and renewed deposits: sheet {plain(sheet_interest)} vs database {plain(db_interest)}"
               + (" ✓" if abs(sheet_interest - db_interest) < 1 else " ✗ (the sheet sums each deposit's interest after TDS, the ledger what the bank credited)"))
    if abs(sheet_interest - db_interest) >= 1:
        report.problem("Realised deposit interest differs from the sheet.")
    other_interest = db.one("SELECT COALESCE(SUM(CASE kind WHEN 'REDEEM' THEN amount WHEN 'INVEST' THEN -amount ELSE 0 END), 0) FROM transactions "
                            "WHERE instrument_id IN (SELECT instrument_id FROM fixed_deposits WHERE status != 'ACTIVE') "
                            f"AND instrument_id NOT IN ({','.join('?' * len(closed))})", *closed)[0] if closed else 0.0
    if abs(other_interest) >= 0.5:
        report.add(f"- Interest on the sweep-in, extra and unlisted deposits (not in the sheet's totals): {plain(other_interest)}.")
    missing_invest = db.q("SELECT i.name FROM instruments i WHERE i.instrument_type = 'FIXED_DEPOSIT' AND NOT EXISTS (SELECT 1 FROM transactions t WHERE t.instrument_id = i.id AND t.kind = 'INVEST')")
    for m in missing_invest:
        report.problem(f"Deposit '{m['name']}' has no deposit row.")


# ----------------------------------------------------------------------------------------------- insurance

def backfill_insurance(db, sheet, report):
    rows = sheet.insurances()
    by_policy = collections.defaultdict(list)
    for r in rows:
        key = f"{str(r.get('Insurance Provider') or '').strip()} - {str(r.get('Insurance Name') or '').strip()}"
        by_policy[key].append(r)
    updated = 0
    for inst in db.q("SELECT i.id, i.name, p.insurance_type FROM instruments i JOIN insurance_policies p ON p.instrument_id = i.id WHERE i.instrument_type = 'INSURANCE'"):
        rs = sorted(by_policy.get(inst["name"], []), key=lambda r: as_date(r["Date"]))
        if not rs:
            report.add(f"- Policy '{inst['name']}' not found in the Insurances sheet; left as is.")
            continue
        premiums = [r for r in rs if str(r.get("Transaction Type") or "").lower().startswith("premium")]
        number = next((str(r["Policy Number"]) for r in reversed(rs) if r.get("Policy Number")), None)
        start = as_date(rs[0]["Date"])
        last = premiums[-1] if premiums else None
        freq = None
        if len(premiums) >= 2:
            gap = (as_date(premiums[-1]["Date"]) - as_date(premiums[-2]["Date"])).days
            freq = min([1, 3, 6, 12], key=lambda m: abs(m * 30.4 - gap))
        next_due = None
        if last and freq:
            d = as_date(last["Date"])
            month = d.month - 1 + freq
            next_due = dt.date(d.year + month // 12, month % 12 + 1, min(d.day, 28))
        # The app keeps the due day (and, unless monthly, the anchor month) and works the next due date out from the last premium.
        db.x("""UPDATE insurance_policies SET policy_number = COALESCE(policy_number, ?), premium_amount = ?, premium_frequency_months = ?,
                    start_date = COALESCE(start_date, ?), due_day = ?, due_month = ? WHERE instrument_id = ?""",
             number, as_float(last["Amount"]) if last else None, freq, start.isoformat() if start else None,
             next_due.day if next_due else None, next_due.month if next_due and freq and freq > 1 else None, inst["id"])
        updated += 1
    report.add(f"- Insurance: policy number, premium, frequency, start and due day written for {updated} life policies.")


# ------------------------------------------------------------------------------------------ health and vehicles

# The app's expense categories for the policies that are protection, not investment (Insurance.kt).
INSURANCE_CATEGORY = "Insurance"   # term, health and motor premiums
VEHICLE_CATEGORY = "Vehicle"       # a vehicle's services and emission tests
# Insurers that may be named in a motor premium's particulars; the extras file can always say instead.
INSURERS = ("ICICI Lombard", "HDFC Ergo", "Bajaj Allianz", "New India", "Tata AIG", "Acko", "Go Digit", "Digit", "United India",
            "National Insurance", "Oriental", "Reliance General", "SBI General", "Royal Sundaram", "Chola MS", "Kotak General",
            "Zuno", "Liberty", "Magma", "IFFCO Tokio", "Future Generali", "Shriram", "Universal Sompo", "Navi")
PERIOD_KINDS = {"cover": "COVER", "third_party": "THIRD_PARTY", "own_damage": "OWN_DAMAGE", "service": "SERVICE", "emission": "EMISSION_TEST"}


def insurance_extras(path):
    """
    What the sheet does not say about the protection policies, from a JSON file kept next to the sheet export, never in
    this repository: {"health": [...], "vehicles": [...]}.

    A health entry is matched to a sheet policy by "key" ("Provider - Name", as the sheet spells them) and may give
    insurer (the company on the policy and its cover periods, when the sheet's provider names the broker too),
    sum_insured, policy_number, owner, notes, cover_months (default 12), ids (ledger rows of premiums the sheet never
    listed; with provider and name instead of key it describes a policy known only from the ledger) and premiums, the
    policy schedules' own terms for the premiums: {paid, start, end, policy_number, sum_insured}, matched to a premium by
    the date it was paid (within five days) and taking the place of the computed cover period and the sheet's policy
    number; the sum insured goes on that premium's cover period, since a renewal may change it, and the policy row
    shows the latest one.

    A vehicle has a name (the instrument's name), vehicle_number, owner (default Self), active (default true), notes,
    purchased (date), match (words of the particulars that name it; default the name), skip (ledger ids to leave alone),
    service_months (default 12) and emission_months (default 6: a new vehicle's first certificate lasts a year, the
    later ones six months), periods (validity set by hand, for cover no ledger row paid for: kind third_party /
    own_damage / service / emission, start, end, provider, reference) and entries ({ledger id: {...}} overriding what the
    particulars say about one row: kind premium / service / emission / skip, months, third_party_months,
    own_damage_months, third_party_until, own_damage_until, provider, reference).
    """
    with open(path) as f:
        data = json.load(f)
    health = list(data.get("health") or [])
    vehicles = list(data.get("vehicles") or [])
    for v in vehicles:
        if not str(v.get("name") or "").strip():
            sys.exit(f"{path}: every vehicle needs a name")
    for h in health:
        if not h.get("key") and not (h.get("provider") and h.get("name")):
            sys.exit(f"{path}: every health entry needs a key (\"Provider - Name\") or a provider and a name")
    return health, vehicles


def add_months(d, months):
    import calendar
    m = d.month - 1 + months
    y, m = d.year + m // 12, m % 12 + 1
    return dt.date(y, m, min(d.day, calendar.monthrange(y, m)[1]))


def ensure_expense_category(db, name, report):
    r = db.one("SELECT id FROM categories WHERE deleted = 0 AND applies_to != 'INCOME' AND lower(trim(name)) = lower(?)", name)
    if r:
        return r["id"]
    order = (db.one("SELECT COALESCE(MAX(display_order), 0) FROM categories")[0] or 0) + 1
    cur = db.x("INSERT INTO categories (name, parent_id, applies_to, icon, color, display_order, deleted) VALUES (?, NULL, 'EXPENSE', NULL, NULL, ?, 0)",
               name, order)
    report.add(f"- Added the '{name}' expense category.")
    return cur.lastrowid


def insert_policy(db, name, policy, owner="Self", active=True, notes=None, now=None):
    """An INSURANCE instrument with its policy row; no investment category, so it stays out of the Investment Summary."""
    iid = db.insert_instrument(name, "INSURANCE", None, active=active, notes=notes, owner=owner or "Self", now=now)
    cols = ["insurance_type", "provider", "policy_name", "policy_number", "vehicle_number", "sum_assured", "premium_amount",
            "premium_frequency_months", "due_day", "due_month", "start_date", "maturity_date"]
    db.x(f"INSERT INTO insurance_policies (instrument_id, {', '.join(cols)}) VALUES (?, {', '.join('?' * len(cols))})",
         iid, *[policy.get(c) for c in cols])
    return iid


def insert_period(db, iid, kind, start, end, transaction_id=None, provider=None, reference=None, sum_insured=None):
    db.x("INSERT INTO validity_periods (instrument_id, kind, start_date, end_date, transaction_id, provider, reference, sum_insured) "
         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
         iid, kind, start.isoformat() if start else None, end.isoformat(), transaction_id, provider or None, reference or None, sum_insured)


def link_row(db, row_id, iid, category_id, now):
    """Puts a ledger row on an instrument and under a category; the accounts and the amount stay as they are."""
    db.x("UPDATE transactions SET instrument_id = ?, category_id = ?, modified_time = ? WHERE id = ?", iid, category_id, now, row_id)


def usable_policy_number(v):
    """The sheet export turns a long policy number into a rounded float; such a value is not a policy number any more."""
    if v is None or v == "":
        return None
    if isinstance(v, float) and v != int(v):
        return None
    if isinstance(v, (int, float)) and abs(v) >= 1e15:
        return None
    return str(int(v)) if isinstance(v, float) else str(v).strip()


def backfill_health(db, sheet, extras, report):
    """
    The sheet's Health Insurance rows become one HEALTH policy per provider and plan: an expense policy, whose premiums
    the app files under the Insurance category and leaves out of the Investment Summary. Each premium is found in the
    ledger by amount and date (within five days) and gets a cover period: to the policy's anniversary when it was paid
    within 45 days of one (the anniversary is the first premium's date), else twelve months from the payment, unless the
    extras file gives the schedule's own dates for that premium. The extras file adds what the sheet lacks (sum insured,
    policy number, owner, the insurer behind a broker); the sheet's own policy numbers are used only when the export
    kept their digits, as the number of the policy and as the reference of the premium's cover period.
    """
    now = now_iso()
    by_key = collections.OrderedDict()
    for r in sheet.insurances():
        if not str(r.get("Insurance Type") or "").lower().startswith("health"):
            continue
        key = f"{str(r.get('Insurance Provider') or '').strip()} - {str(r.get('Insurance Name') or '').strip()}"
        by_key.setdefault(key, []).append(r)
    extra_by_key = {}
    for e in extras:
        key = e.get("key") or f"{e['provider']} - {e['name']}"
        extra_by_key[key] = e
        by_key.setdefault(key, [])
    if not by_key:
        report.add("- Health insurance: nothing in the sheet or the extras file.")
        return
    insurance_cat = ensure_expense_category(db, INSURANCE_CATEGORY, report)
    for key, rows in by_key.items():
        e = extra_by_key.get(key, {})
        provider, _, plan = key.partition(" - ")
        existing = db.one("SELECT id FROM instruments WHERE name = ? AND instrument_type = 'INSURANCE'", key)
        if existing:
            update_health_terms(db, existing["id"], key, e, report)
            continue
        premiums = []   # (date, amount, ledger row or None, sheet policy number)
        unmatched = []
        for r in sorted(rows, key=lambda r: as_date(r["Date"])):
            if not str(r.get("Transaction Type") or "premium").lower().startswith("premium"):
                continue
            d, amount = as_date(r["Date"]), as_float(r["Amount"])
            hit = db.one("""SELECT id, date, amount, particulars FROM transactions WHERE kind = 'EXPENSE' AND instrument_id IS NULL
                            AND ABS(amount - ?) < 0.01 AND date BETWEEN ? AND ? ORDER BY ABS(julianday(date) - julianday(?)) LIMIT 1""",
                         amount, (d - dt.timedelta(days=5)).isoformat(), (d + dt.timedelta(days=5)).isoformat(), d.isoformat())
            if hit is None:
                unmatched.append(f"{d} {plain(amount)}")
            premiums.append((d, amount, hit, usable_policy_number(r.get("Policy Number"))))
        for tid in e.get("ids") or []:
            hit = db.one("SELECT id, date, amount, particulars FROM transactions WHERE id = ? AND kind = 'EXPENSE' AND instrument_id IS NULL", tid)
            if hit is None:
                report.problem(f"Health policy '{key}': ledger row {tid} is not an unlinked expense.")
                continue
            premiums.append((as_date(hit["date"]), hit["amount"], hit, None))
        premiums.sort(key=lambda x: x[0])
        if not premiums:
            report.add(f"- Health policy '{key}': no premiums; skipped.")
            continue
        anchor = premiums[0][0]
        last_paid = max((x for x in premiums if x[2] is not None), key=lambda x: x[0], default=premiums[-1])
        schedules = sorted((dict(o, paid=as_date(o["paid"])) for o in e.get("premiums") or []), key=lambda o: o["paid"])

        def schedule_for(d):
            return next((o for o in schedules if abs((o["paid"] - d).days) <= 5), None)

        def reference_of(x):
            o = schedule_for(x[0])
            return (o and o.get("policy_number")) or x[3]

        insurer = e.get("insurer") or provider or None
        number = e.get("policy_number") or next((reference_of(x) for x in reversed(premiums) if reference_of(x)), None)
        # The policy row shows the sum insured in force: the extras' own figure, else the latest schedule's.
        sum_insured = e.get("sum_insured") or next((o["sum_insured"] for o in reversed(schedules) if o.get("sum_insured")), None)
        iid = insert_policy(db, key, {
            "insurance_type": "HEALTH", "provider": insurer, "policy_name": plan or None, "policy_number": number,
            "sum_assured": sum_insured, "premium_amount": last_paid[1], "premium_frequency_months": 12,
            "due_day": anchor.day, "due_month": anchor.month, "start_date": anchor.isoformat(),
        }, owner=e.get("owner", "Self"), active=e.get("active", True), notes=e.get("notes"), now=now)
        months = int(e.get("cover_months") or 12)
        cover_end = None
        linked = 0
        from_schedule = 0
        for k, (d, amount, hit, sheet_number) in enumerate(premiums):
            if hit is None:
                continue
            anniversary = add_months(anchor, 12 * k)
            start, end = d, add_months(anniversary if abs((d - anniversary).days) <= 45 else d, months)
            o = schedule_for(d)
            if o is not None:
                start = as_date(o["start"]) if o.get("start") else start
                end = as_date(o["end"]) if o.get("end") else end
                from_schedule += 1
            link_row(db, hit["id"], iid, insurance_cat, now)
            insert_period(db, iid, "COVER", start, end, hit["id"], provider=insurer, reference=reference_of((d, amount, hit, sheet_number)),
                          sum_insured=(o or {}).get("sum_insured"))
            cover_end = max(cover_end or end, end)
            linked += 1
        text = f"- Health policy '{key}': {linked} premiums linked (last {plain(last_paid[1])} on {last_paid[0]}), cover to {cover_end}"
        if from_schedule:
            text += f"; {from_schedule} cover periods dated from the policy schedules"
            sums = [o["sum_insured"] for o in schedules if o.get("sum_insured")]
            if sums:
                text += f" (sum insured {' → '.join(plain(v) for v in dict.fromkeys(sums))})"
        unused = [o for o in schedules if not any(abs((o["paid"] - x[0]).days) <= 5 for x in premiums if x[2] is not None)]
        for o in unused:
            report.problem(f"Health policy '{key}': the extras file dates a premium paid {o['paid']} that is not among the linked premiums.")
        if unmatched:
            text += f"; not in the ledger: {', '.join(unmatched)}"
        if not number and any(r.get("Policy Number") for r in rows):
            text += "; the sheet's policy number lost its digits in the export, enter it in the app"
        if sum_insured is None:
            text += "; sum insured not known, enter it in the app"
        report.add(text + ".")


def update_health_terms(db, iid, key, extras, report):
    """
    A health policy the database already has (an earlier run made it) takes the policy schedules' terms from the extras
    file: each premium's cover period gets the schedule's dates, policy number and sum insured, and the policy row the
    latest ones. Nothing else about it changes.
    """
    schedules = [dict(o, paid=as_date(o["paid"])) for o in extras.get("premiums") or []]
    insurer = extras.get("insurer")
    periods = db.q("""SELECT v.id, v.start_date, v.end_date, v.provider, v.reference, v.sum_insured, t.date
                      FROM validity_periods v JOIN transactions t ON t.id = v.transaction_id
                      WHERE v.instrument_id = ? AND v.kind = 'COVER' ORDER BY t.date""", iid)
    updated = 0
    for o in schedules:
        v = next((v for v in periods if abs((as_date(v["date"]) - o["paid"]).days) <= 5), None)
        if v is None:
            report.problem(f"Health policy '{key}': the extras file dates a premium paid {o['paid']} that has no cover period.")
            continue
        new = {
            "start_date": o.get("start") or v["start_date"], "end_date": o.get("end") or v["end_date"],
            "provider": insurer or v["provider"], "reference": o.get("policy_number") or v["reference"],
            "sum_insured": o.get("sum_insured") if o.get("sum_insured") is not None else v["sum_insured"],
        }
        if any(str(new[c]) != str(v[c]) for c in new):
            db.x("UPDATE validity_periods SET start_date = ?, end_date = ?, provider = ?, reference = ?, sum_insured = ? WHERE id = ?",
                 new["start_date"], new["end_date"], new["provider"], new["reference"], new["sum_insured"], v["id"])
            updated += 1
    latest = db.one("SELECT reference, sum_insured, provider FROM validity_periods WHERE instrument_id = ? AND kind = 'COVER' ORDER BY end_date DESC, id DESC LIMIT 1", iid)
    if latest:
        db.x("""UPDATE insurance_policies SET policy_number = COALESCE(?, policy_number), sum_assured = COALESCE(?, sum_assured),
                provider = COALESCE(?, provider) WHERE instrument_id = ?""",
             extras.get("policy_number") or latest["reference"], extras.get("sum_insured") or latest["sum_insured"], insurer or latest["provider"], iid)
    report.add(f"- Health policy '{key}' already exists; {updated} cover periods took the schedules' terms" + (", the policy row the latest." if updated else "."))


def classify_vehicle_row(particulars):
    p = particulars.lower()
    if any(w in p for w in ("duplicate", "reversed", "reversal", "refund")):
        return "skip"
    if "insur" in p:
        return "premium"
    if any(w in p for w in ("emission", "pollution", "puc")):
        return "emission"
    if "servic" in p:
        return "service"
    return None


def premium_terms(particulars):
    """(third party?, own damage?, months, named?) from a motor premium's particulars; unnamed cover is taken as both."""
    p = particulars.lower()
    tp = any(w in p for w in ("third party", "third-party", "liability", "comprehensive"))
    od = any(w in p for w in ("first party", "first-party", "own damage", "own-damage", "comprehensive", "standalone od", " od "))
    named = tp or od
    if not named:
        tp = od = True
    m = re.search(r"(\d+)\s*(?:year|yr)", p)
    months = 12 * int(m.group(1)) if m else None
    if months is None:
        m = re.search(r"(\d+)\s*month", p)
        months = int(m.group(1)) if m else 12
    return tp, od, months, named


def vehicle_particulars(name, kind):
    """The app's default wording for a vehicle row: the insurer, cover and place are recorded beside it, not in it."""
    return {"premium": f"Motor Insurance Premium - {name}", "service": f"Vehicle Service - {name}",
            "emission": f"Vehicle Emission Test - {name}"}[kind]


def reword_vehicle_row(db, row_id, name, kind, overrides, now, report_lines):
    """
    A linked row reads in the house style: the file's `particulars` for the row when given, else the default unless
    the row already starts with it (anything added after it, an AMC bought say, stays).
    """
    old = db.one("SELECT particulars FROM transactions WHERE id = ?", row_id)[0]
    wanted = (overrides.get("particulars") or "").strip() or None
    if wanted is None:
        default = vehicle_particulars(name, kind)
        wanted = None if old.startswith(default) else default
    if wanted and wanted != old:
        db.x("UPDATE transactions SET particulars = ?, modified_time = ? WHERE id = ?", wanted, now, row_id)
        report_lines.append(f"row {row_id}: '{old}' → '{wanted}'")


def touch_existing_vehicle(db, iid, name, v, places, report):
    """
    A vehicle already in the database (an earlier run made it, the app is in use): its service and emission-test
    rows that say nowhere get the file's `service_at` / `emission_test_at` (a place typed in the app stays), and its
    rows are reworded as reword_vehicle_row says, by the validity each one bought.
    """
    done = []
    for kind_name, place in places.items():
        if not place:
            continue
        n = db.x("UPDATE validity_periods SET provider = ? WHERE instrument_id = ? AND kind = ? AND transaction_id IS NOT NULL AND provider IS NULL",
                 place, iid, kind_name).rowcount
        if n:
            done.append(f"{n} {'service' if kind_name == 'SERVICE' else 'emission-test'} row(s) now at {place}")
    overrides = {int(k): o for k, o in (v.get("entries") or {}).items()}
    now = now_iso()
    reworded = []
    kind_of = {"THIRD_PARTY": "premium", "OWN_DAMAGE": "premium", "SERVICE": "service", "EMISSION_TEST": "emission"}
    for r in db.q("SELECT DISTINCT transaction_id AS id, kind FROM validity_periods WHERE instrument_id = ? AND transaction_id IS NOT NULL ORDER BY transaction_id", iid):
        reword_vehicle_row(db, r["id"], name, kind_of[r["kind"]], overrides.get(r["id"], {}), now, reworded)
    done.extend(reworded)
    report.add(f"- Vehicle '{name}' already exists; " + ("; ".join(done) if done else "left as is") + ".")


def backfill_vehicles(db, vehicles, report):
    """
    Each vehicle in the extras file becomes a MOTOR policy (a vehicle, in the app's words). The ledger's expense rows
    that name it are read by their particulars: an insurance premium (third-party, own-damage or comprehensive cover,
    for the years it says, else one), an emission test, a service; everything else that mentions the vehicle (petrol,
    the purchase) is left alone and counted. Premiums go under the Insurance category, services and tests under
    Vehicle, each with the validity it bought, and the file's hand-set periods cover what no ledger row paid for (the
    five-year third-party cover that came with a new vehicle).
    """
    if not vehicles:
        return
    now = now_iso()
    insurance_cat = ensure_expense_category(db, INSURANCE_CATEGORY, report)
    vehicle_cat = ensure_expense_category(db, VEHICLE_CATEGORY, report)
    for v in vehicles:
        name = v["name"].strip()
        places = {"SERVICE": (v.get("service_at") or "").strip() or None, "EMISSION_TEST": (v.get("emission_test_at") or "").strip() or None}
        existing = db.one("SELECT id FROM instruments WHERE name = ? AND instrument_type = 'INSURANCE'", name)
        if existing:
            touch_existing_vehicle(db, existing[0], name, v, places, report)
            continue
        words = [w.lower() for w in (v.get("match") or [name])]
        skip = {int(x) for x in v.get("skip") or []}
        overrides = {int(k): o for k, o in (v.get("entries") or {}).items()}
        rows = [r for r in db.q("SELECT id, date, amount, particulars FROM transactions WHERE kind = 'EXPENSE' AND instrument_id IS NULL ORDER BY date, id")
                if any(w in r["particulars"].lower() for w in words)]
        purchased = as_date(v["purchased"]) if v.get("purchased") else None
        iid = insert_policy(db, name, {"insurance_type": "MOTOR", "vehicle_number": v.get("vehicle_number"),
                                       "start_date": purchased.isoformat() if purchased else None},
                            owner=v.get("owner", "Self"), active=v.get("active", True), notes=v.get("notes"), now=now)
        lines, left, skipped, guessed, reworded = [], [], [], [], []
        service_months, emission_months = int(v.get("service_months") or 12), int(v.get("emission_months") or 6)
        for r in rows:
            o = overrides.get(r["id"], {})
            kind = o.get("kind") or classify_vehicle_row(r["particulars"])
            if r["id"] in skip or kind == "skip":
                skipped.append(f"{r['date']} {plain(r['amount'])} {r['particulars']}")
                continue
            if kind is None:
                left.append(r)
                continue
            d = as_date(r["date"])
            if kind == "premium":
                tp, od, months, named = premium_terms(r["particulars"])
                provider = o.get("provider") or next((i for i in INSURERS if i.lower() in r["particulars"].lower()), None)
                ends = {}
                for key, kind_name, on in (("third_party", "THIRD_PARTY", tp), ("own_damage", "OWN_DAMAGE", od)):
                    until = o.get(f"{key}_until")
                    own_months = o.get(f"{key}_months")
                    if until:
                        ends[kind_name] = as_date(until)
                    elif own_months:
                        ends[kind_name] = add_months(d, int(own_months))
                    elif on:
                        ends[kind_name] = add_months(d, months)
                if not ends:
                    report.problem(f"Vehicle '{name}': premium row {r['id']} buys no cover; give it third_party_months or own_damage_months.")
                    continue
                link_row(db, r["id"], iid, insurance_cat, now)
                reword_vehicle_row(db, r["id"], name, "premium", o, now, reworded)
                for kind_name, end in ends.items():
                    insert_period(db, iid, kind_name, d, end, r["id"], provider, o.get("reference"))
                what = " and ".join(("third-party" if k == "THIRD_PARTY" else "own-damage") + f" cover to {e}" for k, e in ends.items())
                lines.append(f"{d} {plain(r['amount'])} premium" + (f" ({provider})" if provider else "") + f": {what}")
                if not named and not (o.get("third_party_until") or o.get("own_damage_until") or o.get("third_party_months") or o.get("own_damage_months")):
                    guessed.append(str(r["id"]))
            else:
                months = int(o.get("months") or (service_months if kind == "service" else emission_months))
                end = add_months(d, months)
                link_row(db, r["id"], iid, vehicle_cat, now)
                reword_vehicle_row(db, r["id"], name, kind, o, now, reworded)
                kind_name = "SERVICE" if kind == "service" else "EMISSION_TEST"
                insert_period(db, iid, kind_name, d, end, r["id"], o.get("provider") or places[kind_name])
                lines.append(f"{d} {plain(r['amount'])} {'service' if kind == 'service' else 'emission test'}, next by {end}"
                             + (f" ({o.get('provider') or places[kind_name]})" if o.get("provider") or places[kind_name] else ""))
        for pr in v.get("periods") or []:
            kind_name = PERIOD_KINDS.get(str(pr.get("kind", "")).lower())
            if kind_name is None or not pr.get("end"):
                report.problem(f"Vehicle '{name}': hand-set period needs a kind ({', '.join(PERIOD_KINDS)}) and an end date.")
                continue
            insert_period(db, iid, kind_name, as_date(pr["start"]) if pr.get("start") else None, as_date(pr["end"]), None,
                          pr.get("provider"), pr.get("reference"))
            lines.append(f"set by hand: {kind_name.lower().replace('_', ' ')} to {pr['end']}" + (f" ({pr['provider']})" if pr.get("provider") else ""))
        report.add(f"- Vehicle '{name}'" + (f" ({v['vehicle_number']})" if v.get("vehicle_number") else " (no registration number given)") + ":")
        for line in lines:
            report.add(f"    - {line}")
        if reworded:
            report.add(f"    - reworded in the house style: {'; '.join(reworded)}")
        if skipped:
            report.add(f"    - skipped: {'; '.join(skipped)}")
        if guessed:
            report.add(f"    - premium rows {', '.join(guessed)} named neither third-party nor own-damage cover; both were assumed.")
        if left:
            sample = "; ".join(r["particulars"][:50] for r in left[:3])
            report.add(f"    - {len(left)} other rows mention it and were left alone (petrol and the like): {sample}{'; …' if len(left) > 3 else ''}")
        if not lines:
            report.add("    - no insurance, service or emission-test rows found in the ledger.")


# --------------------------------------------------------------------------------------------------- merges

def merge_instruments(db, merges, report):
    """EditDataRepository.mergeInstruments: rows, templates and deposit links move to the target, the old one goes."""
    for spec in merges:
        if "=" not in spec:
            report.problem(f"--merge expects OLD=NEW, got '{spec}'.")
            continue
        old_name, new_name = (x.strip() for x in spec.split("=", 1))
        find = lambda n: db.one("SELECT id, name, instrument_type, isin, symbol, category_id, notes FROM instruments WHERE name = ? OR symbol = ? ORDER BY id LIMIT 1", n, n)
        old, new = find(old_name), find(new_name)
        if old is None:
            report.add(f"- Merge {old_name} -> {new_name}: '{old_name}' not found (already merged?); skipped.")
            continue
        if new is None:
            report.problem(f"Merge {old_name} -> {new_name}: '{new_name}' not found.")
            continue
        if old["id"] == new["id"] or old["instrument_type"] != new["instrument_type"]:
            report.problem(f"Merge {old_name} -> {new_name}: same instrument or different types.")
            continue
        now = now_iso()
        n = db.x("UPDATE transactions SET instrument_id = ?, modified_time = ? WHERE instrument_id = ?", new["id"], now, old["id"]).rowcount
        db.x("UPDATE templates SET instrument_id = ? WHERE instrument_id = ?", new["id"], old["id"])
        db.x("UPDATE fixed_deposits SET previous_fd_id = ? WHERE previous_fd_id = ?", new["id"], old["id"])
        db.x("UPDATE fixed_deposits SET next_fd_id = ? WHERE next_fd_id = ?", new["id"], old["id"])
        db.x("DELETE FROM instrument_valuations WHERE instrument_id = ?", old["id"])
        db.x("DELETE FROM fixed_deposits WHERE instrument_id = ?", old["id"])
        db.x("DELETE FROM insurance_policies WHERE instrument_id = ?", old["id"])
        db.x("DELETE FROM instruments WHERE id = ?", old["id"])
        note = f"Merged from {old['name']}" + (f" ({old['symbol']})" if old["symbol"] else "")
        db.x("UPDATE instruments SET isin = COALESCE(isin, ?), category_id = COALESCE(category_id, ?), active = 1, notes = ?, modified_time = ? WHERE id = ?",
             old["isin"], old["category_id"], "\n".join(filter(None, [new["notes"], note])), now, new["id"])
        report.add(f"- Merged '{old['name']}' into '{new['name']}': {n} rows moved, old instrument deleted.")


# ----------------------------------------------------------------------------------------------- carry over

def carry_over(db, src_path, report):
    """
    Copies what was set by hand in a database the app is using now (so it survives a re-import of the 4.5.4 data):
    deposit templates, the order of the investment categories, and for every instrument except the deposits
    (those are renumbered by the backfill) its order, its active flag and its price link. Everything is matched
    by name, never by id; the prefs rows of the settings table come along too.
    """
    src = sqlite3.connect(f"file:{src_path}?mode=ro", uri=True)
    src.row_factory = sqlite3.Row
    version = src.execute("PRAGMA user_version").fetchone()[0]
    if not 5 <= version <= SCHEMA_VERSION:
        report.problem(f"--carry-from database is schema version {version}; expected 5 to {SCHEMA_VERSION}.")
        src.close()
        return
    now = now_iso()
    # deposit templates: the paying account is matched by the account's name
    accounts = {r["name"]: r["id"] for r in db.q("SELECT id, name FROM accounts WHERE deleted = 0")}
    src_accounts = {r["id"]: r["name"] for r in src.execute("SELECT id, name FROM accounts")}
    cols = ["name", "bank_name", "branch", "holder_name", "cust_id", "amount", "from_account_id", "period_months", "period_days",
            "rate", "compounding_months", "owner", "notes", "display_order"]
    added = updated = 0
    for t in src.execute(f"SELECT {', '.join(cols)} FROM fd_templates ORDER BY display_order, name"):
        values = dict(t)
        if values["from_account_id"] is not None:
            name = src_accounts.get(values["from_account_id"])
            values["from_account_id"] = accounts.get(name)
            if values["from_account_id"] is None:
                report.add(f"- Template '{t['name']}': paying account '{name}' not found here; left unset.")
        existing = db.one("SELECT id FROM fd_templates WHERE lower(trim(name)) = lower(trim(?))", t["name"])
        if existing:
            db.x("UPDATE fd_templates SET " + ", ".join(f"{c} = ?" for c in cols) + " WHERE id = ?", *[values[c] for c in cols], existing["id"])
            updated += 1
        else:
            db.x(f"INSERT INTO fd_templates ({', '.join(cols)}) VALUES ({', '.join('?' * len(cols))})", *[values[c] for c in cols])
            added += 1
    report.add(f"- Carried over {added + updated} deposit templates ({added} added, {updated} updated).")
    # investment categories: order only (targets and risk levels come from the sheet)
    def path(conn, cid, byid):
        parts = []
        while cid is not None:
            parts.append(byid[cid]["name"])
            cid = byid[cid]["parent_id"]
        return " > ".join(reversed(parts))
    src_cats = {r["id"]: r for r in src.execute("SELECT id, name, parent_id, display_order FROM investment_categories")}
    dst_cats = {r["id"]: r for r in db.q("SELECT id, name, parent_id, display_order FROM investment_categories")}
    dst_by_path = {path(db, i, dst_cats): r for i, r in dst_cats.items()}
    moved = 0
    for i, r in src_cats.items():
        target = dst_by_path.get(path(src, i, src_cats))
        if target is not None and target["display_order"] != r["display_order"]:
            db.x("UPDATE investment_categories SET display_order = ? WHERE id = ?", r["display_order"], target["id"])
            moved += 1
    report.add(f"- Investment categories: order carried over ({moved} moved).")
    # instruments other than deposits: order, active flag, price link
    dst_inst = {(r["name"], r["instrument_type"]): r for r in db.q(
        "SELECT id, name, instrument_type, display_order, active, isin, symbol, exchange FROM instruments WHERE instrument_type != 'FIXED_DEPOSIT'")}
    ordered = retired = linked = 0
    for r in src.execute("SELECT name, instrument_type, display_order, active, isin, symbol, exchange FROM instruments WHERE instrument_type != 'FIXED_DEPOSIT'"):
        d = dst_inst.get((r["name"], r["instrument_type"]))
        if d is None:
            continue
        if d["display_order"] != r["display_order"]:
            db.x("UPDATE instruments SET display_order = ? WHERE id = ?", r["display_order"], d["id"])
            ordered += 1
        if d["active"] != r["active"]:
            db.x("UPDATE instruments SET active = ?, modified_time = ? WHERE id = ?", r["active"], now, d["id"])
            retired += 1
        if r["symbol"] and (d["isin"], d["symbol"], d["exchange"]) != (r["isin"], r["symbol"], r["exchange"]):
            db.x("UPDATE instruments SET isin = COALESCE(isin, ?), symbol = ?, exchange = ?, modified_time = ? WHERE id = ?",
                 r["isin"], r["symbol"], r["exchange"], now, d["id"])
            linked += 1
    report.add(f"- Stocks, funds and other instruments: {ordered} reordered, {retired} active flags changed, {linked} price links copied.")
    # app preferences kept in the settings table
    n = 0
    for r in src.execute("SELECT key, value FROM settings WHERE key LIKE 'prefs.%'"):
        db.x("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)", r["key"], r["value"])
        n += 1
    report.add(f"- {n} preference rows copied into the settings table.")
    src.close()


# ----------------------------------------------------------------------------------------------- reconcile

def reconcile(db, sheet, report):
    report.add("")
    report.add("## Totals compared with the Investment Summary sheet")
    report.add("")
    report.add("| Line | Sheet invested | Database invested |")
    report.add("|---|---:|---:|")
    summary = {str(r["Name"]).strip(): r for r in sheet.investment_summary()}

    def line(name, db_value):
        s = as_float(summary.get(name, {}).get("Amount Invested"))
        mark = "" if s is None else (" ✓" if abs(s - db_value) < 1 else " ✗")
        report.add(f"| {name} | {plain(s) if s is not None else '-'} | {plain(db_value)}{mark} |")

    line("Fixed Deposits", db.one("SELECT COALESCE(SUM(CASE kind WHEN 'INVEST' THEN amount WHEN 'REDEEM' THEN -amount ELSE 0 END), 0) "
                                 "FROM transactions WHERE instrument_id IN (SELECT id FROM instruments WHERE instrument_type = 'FIXED_DEPOSIT' AND active = 1)")[0])
    for acc in db.q("SELECT id, name FROM accounts WHERE account_type = 'PROVIDENT_FUND' AND deleted = 0"):
        contributed = db.one("SELECT COALESCE(SUM(CASE WHEN to_account_id = ? AND COALESCE(contributor, '') != 'Interest' THEN amount "
                             "WHEN from_account_id = ? THEN -amount ELSE 0 END), 0) FROM transactions WHERE kind IN ('INCOME', 'TRANSFER')",
                             acc["id"], acc["id"])[0]
        line(acc["name"], contributed)
    line("Life Insurance", db.one("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE kind = 'INVEST' AND instrument_id IN (SELECT id FROM instruments WHERE instrument_type = 'INSURANCE')")[0])
    others = db.one("SELECT COALESCE(SUM(CASE kind WHEN 'INVEST' THEN amount WHEN 'REDEEM' THEN -amount ELSE 0 END), 0) "
                    "FROM transactions WHERE instrument_id IN (SELECT id FROM instruments WHERE instrument_type = 'OTHER')")[0]
    others_sheet = sum(as_float(r.get("Amount Invested")) or 0 for r in others_lines(sheet, include_generic=True))
    report.add(f"| Others (all lines) | {plain(others_sheet)} | {plain(others)}{' ✓' if abs(others_sheet - others) < 1 else ' ✗'} |")
    report.add("")
    last = db.one("SELECT MAX(date) FROM transactions")[0]
    report.add(f"The ledger's last entry is dated {last}; sheet rows after that date (for example a provident-fund contribution "
               "entered in the sheet but not yet in the app) show up as differences here.")
    report.add("")
    report.add("Stocks and mutual funds are not re-summed here: their rows were already complete in the ledger (same row counts as the "
               "sheet), and the app computes their invested amounts by average cost. Compare the Investment Summary screen or the "
               "workbook export with the sheet after installing this database.")


# ---------------------------------------------------------------------------------------------------- main

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--db", required=True, help="Finance Tracker database file (a backup copy; not modified)")
    ap.add_argument("--sheet", required=True, help="My Savings xlsx export")
    ap.add_argument("--out", help="where to write the backfilled database (default: <db>.backfilled.db)")
    ap.add_argument("--report", help="where to write the report (default: <out>.md)")
    ap.add_argument("--merge", action="append", default=[], metavar="OLD=NEW",
                    help="merge a renamed instrument into its new name (by name or symbol), e.g. ZOMATO=ETERNAL; repeatable")
    ap.add_argument("--extra", help="JSON file of deposits the sheet does not have (kept next to the sheet, never in the repository)")
    ap.add_argument("--sweep-in", help="JSON file assigning the ledger's partial liquidations to the sheet's sweep-in deposits (kept next to the sheet)")
    ap.add_argument("--insurance", metavar="JSON",
                    help="JSON file with the vehicles and what the sheet lacks about the health policies (kept next to the sheet)")
    ap.add_argument("--add-insurance", action="store_true",
                    help="on a database the app is using now (already backfilled): only add the health policy and the vehicles, "
                         "leaving everything else as it is")
    ap.add_argument("--keep-sheet-ids", action="store_true",
                    help="name deposits after the sheet's ID column instead of numbering every deposit by its date")
    ap.add_argument("--carry-from", metavar="DB",
                    help="a 5.0.0 database in use now: its deposit templates, category and instrument order, active flags, "
                         "price links and preferences are carried over (matched by name)")
    ap.add_argument("--assign-income", action="store_true",
                    help="file the uncategorised income rows by the keyword rules (without it the report only previews them)")
    ap.add_argument("--force", action="store_true", help="write the output even when a check fails")
    args = ap.parse_args()
    out = args.out or re.sub(r"\.db$", "", args.db) + ".backfilled.db"
    report_path = args.report or re.sub(r"\.db$", "", out) + ".md"

    shutil.copyfile(args.db, out)
    for suffix in ("-wal", "-shm"):
        try:
            shutil.copyfile(args.db + suffix, out + suffix)
        except FileNotFoundError:
            pass
    db = Db(out)
    report = Report()
    report.add(f"# Backfill of {args.sheet.rsplit('/', 1)[-1]} into {args.db.rsplit('/', 1)[-1]}")
    report.add("")
    report.add(f"Run on {dt.datetime.now():%Y-%m-%d %H:%M}.")
    report.add("")
    report.add("## Steps")
    report.add("")
    try:
        backfilled = db.one("SELECT value FROM settings WHERE key = ?", BACKFILL_KEY) if db.version() >= 2 else None
        if backfilled and not args.add_insurance:
            sys.exit("This database was already backfilled (settings.backfill.sheet is set). Start from a fresh backup, "
                     "or pass --add-insurance to add only the health policy and the vehicles to it.")
        if db.version() == 1:
            db.migrate_1_to_2(report)
        if db.version() == 2:
            db.migrate_2_to_3(report)
        if db.version() == 3:
            db.migrate_3_to_4(report)
        if db.version() == 4:
            db.migrate_4_to_5(report)
        if db.version() == 5:
            db.migrate_5_to_6(report)
        if db.version() == 6:
            db.migrate_6_to_7(report)
        if db.version() == 7:
            db.migrate_7_to_8(report)
        if db.version() != SCHEMA_VERSION:
            sys.exit(f"Unexpected schema version {db.version()} (expected {SCHEMA_VERSION}); update this script first.")
        before = db.account_fingerprint()
        sheet = Sheet(args.sheet)
        health_extras, vehicles = insurance_extras(args.insurance) if args.insurance else ([], [])
        if args.add_insurance:
            # The database is in use: nothing is renumbered or re-filed, only the protection policies are added.
            report.add("- --add-insurance: only the health policy and the vehicles are added; deposits, categories and income rows are left as they are.")
            backfill_health(db, sheet, health_extras, report)
            backfill_vehicles(db, vehicles, report)
            key = BACKFILL_KEY + ".insurance"
        else:
            risk = seed_categories(db, report)
            seed_income_categories(db, report)
            apply_targets(db, sheet, report)
            extras = extra_fixed_deposits(args.extra) if args.extra else []
            sweep_plan = sweep_in_plan(args.sweep_in) if args.sweep_in else None
            backfill_fixed_deposits(db, sheet, report, extras, chronological=not args.keep_sheet_ids, sweep_plan=sweep_plan)
            merge_instruments(db, args.merge, report)
            assign_instrument_categories(db, sheet, risk, report)
            backfill_insurance(db, sheet, report)
            backfill_health(db, sheet, health_extras, report)
            backfill_vehicles(db, vehicles, report)
            # After the deposits: a 4.x maturity booked as income has become a redemption by now.
            assign_income_categories(db, report, apply=args.assign_income)
            if args.carry_from:
                carry_over(db, args.carry_from, report)
            key = BACKFILL_KEY
        after = db.account_fingerprint()
        changed = [(before[k][0], before[k][1], after[k][1]) for k in before if before[k][1] != after.get(k, (None, None))[1]]
        if changed:
            for name, b, a in changed:
                report.problem(f"Account '{name}' net cash flow changed from {plain(b)} to {plain(a)}.")
        else:
            report.add("- Every account's net cash flow is unchanged (no money moved in or out of any account).")
        if not args.add_insurance:
            reconcile(db, sheet, report)
        db.x("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)", key, args.sheet.rsplit("/", 1)[-1])
        if report.problems and not args.force:
            db.c.rollback()
            report.add("")
            report.add(f"**Not written: {len(report.problems)} problem(s) above. Re-run with --force to write anyway.**")
            status = 1
        else:
            db.c.commit()
            db.x("VACUUM")
            status = 0
    finally:
        db.c.close()
    with open(report_path, "w") as f:
        f.write(report.text())
    print(report.text())
    print(f"Report: {report_path}")
    if status:
        import os
        os.remove(out)
        print("Output database removed because of the problems above.")
    else:
        print(f"Backfilled database: {out}")
    sys.exit(status)


if __name__ == "__main__":
    main()
