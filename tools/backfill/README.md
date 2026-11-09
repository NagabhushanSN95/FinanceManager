# Backfilling the "My Savings" sheet into Finance Tracker 5.0.0

`backfill_sheet.py` takes a backup of the app's database and the Google Sheet export and writes a
new
database file with everything the sheet tracked that the 4.x ledger did not: one instrument per
fixed
deposit with its terms and renewal chain, the investment categories with their targets, every stock
and
fund in its sheet category, the life policies' details, the sheet's health policy with its cover
periods and, from a
sidecar file, the vehicles with their insurance, services and emission tests. The input files are
never modified.

It is meant to be run twice: once to rehearse, and once more, on a fresh backup, on the day you
switch
from Finance Manager 4.5.4 to Finance Tracker 5.0.0. It refuses to run on a database it has already
backfilled, so always start from a fresh backup.

## Requirements

- Python 3.9 or newer and `openpyxl` (`python3 -m pip install openpyxl`).
- The sheet exported with values: in Google Sheets, File › Download › Microsoft Excel (.xlsx).
- A database backup from the app: Extras › Back up data, then copy
  `Documents/Finance Tracker/Backups/Finance Tracker Database - <date>.db` to the Mac.

## Running it

```
python3 tools/backfill/backfill_sheet.py \
    --db "Finance Tracker Database - 2026-11-04 09-00-00.db" \
    --sheet "My Savings_20261104.xlsx" \
    --merge ZOMATO=ETERNAL
```

`--merge OLD=NEW` (repeatable) merges a renamed instrument into its new name exactly as the app's
"Merge into another instrument" does, so it need not be redone in the app after the restore. It is
skipped quietly when the old name no longer exists.

`--extra extra_fixed_deposits.json` adds deposits the sheet never had: the ones the ledger shows
under
"Fixed Deposit" but no sheet row explains (a short e-TDR and its auto-renewals, for example). The
file
lives next to the sheet export, never in this repository, and holds a list of objects with the
sheet's
columns in snake_case (`id`, `holder`, `amount`, `date`, `months`, `days`, `rate`, `compounding`,
`maturity`, `printed`, `credited`, `interest`, `action`, `account_no`, `bank`, `branch`, `cust_id`,
`certificate`, `notes`, `next`, `previous`). `id`, `next` and `previous` are any strings and only
link
the rows to each other. `action` is `""`, `Closed at Maturity`, `Renewed` or `Closed Prematurely`; a
premature closure also gives `closed_on`, the date the money came back. The rows go through the same
matching as the sheet's: the deposit row is found in the ledger by amount and date, renewals by the
credited amount reappearing as the next deposit, the closure by the amount credited back.

Deposits from the extra file are slotted into the numbering by date, so the sheet's later IDs move
up
by the number inserted before them; the report lists the moves. `--keep-sheet-ids` keeps the sheet's
numbers and names the extra deposits after the last one instead.

`--sweep-in sweep_in_deposits.json` splits the sweep-in deposits (sheet status "Closed with
Sweep-In",
carried at zero there) into deposits of their own. The file lives next to the sheet export and
lists,
per deposit, `sheet_id`, `converted_on` (the day it was linked to the account; the ledger shows
"Converted to sweep-in" on that day) and `liquidations` (the ids of the ledger's "liquidated
partially"
rows that came out of it, as the bank's statement says), plus `amount` when the deposit row is not
the
only one near the sheet's date. Each liquidation is paired with the "interest credited" row booked
beside it (whole-rupee principal, paise are the interest) and the report prints the walk: every
liquidation, its deposit and what was left in it. The payout row keeps the principal and the
interest
row becomes a credit to the same account under the deposit, as the bank statement shows them, and
the sweep-in TDS expenses of that day are linked to the deposit. A deposit that does not end at
exactly
zero, or a liquidation listed twice or not at all, is a problem that stops the run.

`--insurance insurance_extras.json` adds what the sheet does not say about the protection policies.
The file
lives next to the sheet export, never in this repository, and holds
`{"health": [...], "vehicles": [...]}`.

- The sheet's Health Insurance rows need no entry: each provider and plan becomes a health policy on
  its own,
  its premiums found in the ledger by amount and date. A `health` entry keyed by `"Provider - Name"`
  adds the
  `insurer` (the company, when the sheet's provider names the broker too), `sum_insured`,
  `policy_number`,
  `owner`, `notes` or `cover_months` (default 12); `premiums` gives the policy schedules' own terms,
  one
  `{paid, start, end, policy_number, sum_insured}` per premium matched by the day it was paid
  (within five
  days), which replace the computed cover period and put that year's policy number and sum insured
  on it (a
  renewal may change both; the policy row shows the latest, or `sum_insured` when given); with
  `provider`,
  `name` and `ids` (ledger rows of its premiums) it describes a policy the sheet never had.
- Each `vehicles` entry becomes a vehicle (a motor policy): `name`, `vehicle_number`, `owner`,
  `active`,
  `purchased`, `notes`, and `match`, the words of the particulars that name it (default the name).
  The ledger's
  expense rows that match are read by their wording: a row with "insurance" is a premium
  (third-party,
  own-damage or comprehensive cover, for the years it says, else one; the insurer when it is named),
  a row with
  "emission", "PUC" or "pollution" an emission test, a row with "service" a service; a row saying
  duplicate,
  reversed or refund is skipped, and anything else that mentions the vehicle (petrol, the purchase)
  is left
  alone and counted. `service_months` (default 12) and `emission_months` (default 6) say how long a
  service or
  a certificate lasts; `periods` sets validity no ledger row paid for (the five-year third-party
  cover that
  came with a new two-wheeler): `kind` third_party / own_damage / service / emission, `start`,
  `end`,
  `provider`, `reference`; `entries` (`{ledger id: {...}}`) overrides one row: `kind` (premium /
  service /
  emission / skip), `months`, `third_party_months`, `own_damage_months`, `third_party_until`,
  `own_damage_until`, `provider`, `reference`, `particulars`; `skip` lists ids to leave alone. A
  linked row is
  reworded in the app's house style ("Motor Insurance Premium - <vehicle>", "Vehicle
  Service - <vehicle>",
  "Vehicle Emission Test - <vehicle>") unless it already starts that way or its entry gives
  `particulars`; the
  insurer, cover dates and place are recorded beside the row, not in it. `service_at` and
  `emission_test_at` name where the vehicle is serviced and tested: each service or test row carries
  it (an
  entry's `provider` overrides), the app shows it with the row, and on a vehicle that already exists
  the rows
  that say nowhere get it.

The report lists every policy and vehicle with the rows linked to it, what each bought and until
when, the
rows skipped, and how many other rows mention the vehicle.

`--add-insurance` runs these two steps alone on a database the app is using now (one the script
already
backfilled, which it otherwise refuses): take a fresh backup in the app, run

```
python3 tools/backfill/backfill_sheet.py --db "<fresh backup>.db" --sheet "My Savings_<date>.xlsx" \
    --insurance insurance_extras.json --add-insurance
```

and restore `<fresh backup>.backfilled.db` in the app. Deposits, categories and every other row are
left
exactly as they are; a policy or vehicle that already exists is skipped, so the run is safe to
repeat.

`--carry-from <db>` takes a 5.0.0 database the app is using now (pulled from the phone or a backup)
and carries over what was set by hand in it, so a re-import of the 4.5.4 data does not lose it: the
deposit templates (the paying account matched by the account's name), the order of the investment
categories, and for every instrument except the deposits its order, active flag and price link (ISIN
and scheme code or symbol); the `prefs.*` rows of the settings table come along too. Everything is
matched by name. Deposits are left out because the backfill numbers them itself.

This writes `<db>.backfilled.db` and `<db>.backfilled.md` next to the input. The report lists every
step,
every row it could not place, and a table of totals compared with the Investment Summary sheet. When
a
check fails the output database is removed and the script exits with status 1; `--force` writes it
anyway.

## Switch-over day

1. In the app: Extras › Back up data. Copy the backup to the Mac.
2. Export the sheet (values) the same day.
3. Run the script. Read the report; the totals table should show ✓ on every line except entries
   dated
   after the ledger's last row.
4. Copy the backfilled file back to the phone (into `Documents/Finance Tracker/Backups`). In the app
   choose Extras › Restore data and pick it: the current data is backed up first, then the app
   restarts
   with the backfilled database. The settings are kept (the script's output carries none, so nothing
   is overwritten).
5. Open Investments. Prices refresh, the categories and targets are in place, and the fixed deposits
   list
   shows one deposit per sheet row, named `FD #<sheet id>`. Insurance lists the life policies, the
   health
   policy and the vehicles; fill in what the sheet did not have (sum insured, registration numbers)
   from the
   policy's pencil.
6. Instruments with no sheet type are listed in the report; a renamed stock whose old symbol is
   still in
   the ledger is handled by `--merge OLD=NEW` on the command line, anything else via the
   instrument's menu
   in the app.

## What the script decides for you

- **Income categories.** The 4.x import creates one income category, "Interest & Dividends". The
  script adds Salary, Cashback, Refunds and Reimbursements (scope Income), Loans & Lends (scope
  Both)
  and lets the existing Family, Friends and Relatives and Others categories take income as well as
  expenses (scope Both), so money paid back by them can be filed under the same name. Keyword rules
  (`INCOME_RULES`, first match wins; money lent to us and paid back to us both under Loans & Lends,
  Family = Appa and Amma only, the wider family under Friends and Relatives, everything else under
  Others) file the income rows that have no category: the report always shows the preview table;
  `--assign-income` writes the assignments. The step runs after the deposits, so a 4.x maturity
  booked as income is already a redemption by then.
- **Deposits are named `FD #1` … in the sheet's order**, with any deposits from the extra file
  slotted
  in by date (see `--extra`); the app's next deposit continues the numbering.
- **Renewals** become a redemption with no account on the old deposit and a deposit with no account
  on
  the new one, linked through the renewal chain, exactly as the app's own Renew action records them.
  The successor is found by the credited amount reappearing as a deposit on the maturity date; the
  sheet's Next/Previous columns are used only to break ties.
- **The 4.x "Fixed Deposit Interest Credited" rows are removed.** They carried no account and the
  matured amount already includes the interest, so they double-counted it in XIRR and income. Each
  deposit's interest is now its realised profit.
- **Sweep-in deposits** (zero in the sheet) stay as the one `HDFC Sweep-In Fixed Deposits`
  instrument
  holding every partial liquidation; it is closed, with the sheet's three terms in its notes.
- **Deposits older than the ledger** (the 2018 tax-saving deposit) get a deposit row without an
  account;
  their maturity, which the 4.x ledger booked as plain income into the bank, becomes the deposit's
  redemption into the same account, so no balance changes.
- **Fund types the app's tree lacks** (Hybrid, Large and Mid Cap, Healthcare, Multi Asset) are added
  under Mutual Funds with the parent's risk level; adjust them under Manage › Investment categories.
- **The "Saved In …" line of the summary** becomes a sub-category of Others and the matching
  instrument
  moves into it; the loan stays directly under Others.
- **Health insurance** is a policy of its own, not an investment: its premiums stay expenses, moved
  from
  wherever the 4.x ledger filed them into a new "Insurance" category and linked to the policy, each
  with a
  cover period running to the policy's anniversary (the first premium's date) when it was paid
  within 45 days
  of one, else twelve months from the payment. The premium falls due on the anniversary. The sheet's
  Life
  Insurance total is matched exactly without the health premiums.
- **Vehicles** come only from the `--insurance` file. Their premiums go under "Insurance", their
  services and
  emission tests under a new "Vehicle" category; each row keeps its account and amount and gains the
  validity
  it bought, so the app's alerts know when the cover, the next service or the next test is due. The
  vehicle
  has no investment category and stays out of the Investment Summary.
- **Nothing moves money.** Every account's net flow is compared before and after and the script
  aborts
  if any differs.

## Known differences from the sheet

- **Open-deposit estimates** are a little higher in the app (about 0.6 % in total): the app
  compounds
  quarterly from the deposit date and reproduces the banks' printed maturity amounts; the sheet's
  running
  estimate uses a more conservative interpolation.
- **Stock cost basis**: the app uses average cost; the sheet's summary uses first-in-first-out for
  sold
  units, so a stock with partial sales (Silver ETF) shows a slightly different invested amount.
- **Savings tables**: the sheet counts health insurance premiums as savings and ignores the sweep-in
  deposits; the app does the opposite. Provident-fund interest is excluded on both sides.
- Sheet rows entered after the backup was taken show up as differences in the report's totals table.
