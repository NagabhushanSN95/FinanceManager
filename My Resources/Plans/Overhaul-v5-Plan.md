# Finance Tracker (formerly FinanceManager) v5.0.0 Overhaul Plan

Drafted 2026-11-03 against app v4.5.3 (143), DB snapshot
`~/Downloads/FinanceManager/expenditureManager`
(16,555 transactions) and the sheet `My Savings_VALUES_ONLY_20260706.xlsx`. Rev 3, 2026-11-03.
Living document: add requirements under "Backlog", record decisions in the "Decision log".

---

## 0. Step zero, before any code change — DONE 2026-11-03

Status: 4.5.4 (144) committed, merged to master, tagged `v4.5.4` (pushed). Fallback archive at
`~/Downloads/FinanceManager/Fallback-v4.5.4/FinanceManager-v4.5.4-fallback-20261103.zip`
(debug-signed APK, debug keystore copy, source snapshot, README). Branch `dev-v5.0.0` created. App
renamed **Finance Tracker**, package `com.nagabhushansn95.financetracker`, new navy adaptive
launcher icon (mipmap set from ~/Downloads/FinanceTracker_Navy). Root folder and GitHub repo keep
the old name until the end. (The three old packages were merged under the new one;
`com.chaturvedi.datastructures` → `…financemanager.datastructures`,
`com.chaturvedi.customviews` → `…financemanager.customviews`). Backup/export folders moved to
`Documents/Finance Tracker/…` so the two apps share nothing; hand-over files are copied manually on
the phone.

1. Add a small "Backup database file" action to the current Java app (byte copy of
   `expenditureManager` to the backup folder), then commit the working tree (ChangeLog, Coin
   importer, summary scroll fix, this) as **v4.5.4 (144)** and tag it. This is the hand-over format
   from the old app to the new one, so 5.0.0 never needs an `.snb` importer.
2. Build a release APK of 4.5.4 and zip it with a source snapshot into a dated archive outside the
   repo. This is the fallback app for daily use while 5.0.0 is built.
3. Branch `dev-v5` from the tag. All overhaul work happens there.
4. Package rename to `com.nagabhushansn95.financemanager` (applicationId + namespace + source tree)
   is the first commit on `dev-v5`, so 5.0.0 installs **side by side** with 4.5.4 on the phone.
5. Switch-over: 4.5.4 "Backup database file" → 5.0.0 "Restore database file" → migration runs →
   verify → one-off xlsx backfill → uninstall 4.5.4. The migration is rehearsed on DB copies before
   that day.

## 0b. Items to close before the 5.0.0 commit

- **"App was built for an older version of Android" install warning.** Cause: `targetSdk 33` on a
  phone running a newer Android. Fix in milestone E/F: raise `compileSdk` and `targetSdk` to 35 (36
  if the SDK is installed), then walk the behaviour changes that apply to this app: scoped storage
  (replace `WRITE_EXTERNAL_STORAGE` + raw
  `File` paths in Documents with `MediaStore`/SAF for backups and exports), foreground-service type
  for the daily backup service (or move it to WorkManager, already planned), notification permission
  if reminders are added, edge-to-edge enforcement on 35 (Compose handles it; XML screens need
  insets), and `PendingIntent` flags. Run the Android Studio "Android API Upgrade Assistant" as a
  checklist.
- Rename the root folder and GitHub repo to FinanceTracker (user will do; needs reopening the
  project).
- Database file: still named `expenditureManager` inside the new app's private storage; Room will
  create `finance_tracker.db` in milestone A. The new app never touches the old app's private data
  or its `Documents/Chaturvedi/…` folder (done: backups/exports now go to
  `Documents/Finance Tracker/{Backups,Auto Backups,Daily Backups,Exports}`; any hand-over file is
  copied manually on the phone).
- About screen: credit the new logo; the old "Aaditya Ravindran" credit now says "original Finance
  Manager logo".

## 1. What the code looks like today

- 100% Java, `android.app.Activity`, no Kotlin, no networking, no RecyclerView/Material. Deps:
  `appcompat`,
  `itextg` (PDF), `poi-ooxml 5.2.3`. minSdk 26, compileSdk 33.
- `DatabaseAdapter` (`SQLiteOpenHelper` v1) has a destructive `onUpgrade`; real migrations are
  versionCode-keyed
  `updates/UpdateXToY` classes.
- `transactions.type` is a positional string (`Credit Bank03`, `Debit Wallet01 Exp02`,
  `Transfer Bank17 Bank16`)
  parsed with fixed substring offsets and filtered with SQL `LIKE`.
- Credit cards are bank rows with negative balances (9 of 24). Investments are the pseudo-bank
  `Bank16` holding cost basis (2,146 rows) plus `Bank17 Zerodha Demat`. Realized profit is a
  separate `Credit Bank16` row.
- `counters` duplicates per-day sums of `transactions` with one column per expenditure type.
- Zerodha importers receive ISIN and symbol in the CSVs and discard them.
- Bank SMS reading exists but has been disabled by the user for a long time.
- Backup is a line-based `.snb` text file.

## 2. The Google Sheet, mapped

| Sheet                                        | Rows                | Becomes in app                                                                                     |
|----------------------------------------------|---------------------|----------------------------------------------------------------------------------------------------|
| Savings Yearly / Monthly                     | 19 / 121            | Query over INVEST/REDEEM rows and PF contributions, grouped by period and category                 |
| Investment Summary                           | 40                  | Investments dashboard; `investment_categories` holds targets, tolerance, risk, notes               |
| Fixed Deposits                               | 116                 | `instruments` (type FIXED_DEPOSIT) + `fixed_deposits` detail, owner Self                           |
| Appa Amma FDs / Doddamma FDs                 | 223 / 101           | Supported (owner column) but **not imported for now**                                              |
| Provident Funds                              | 82                  | EPF and PPF are `accounts` (type PROVIDENT_FUND); contributions are INCOME/TRANSFER rows into them |
| Insurances                                   | 20                  | `instruments` (type INSURANCE) + `insurance_policies`; premiums are INVEST rows                    |
| MF Credits / Debits / Summary                | 254 / 12 / 15       | `instruments` (type MUTUAL_FUND) + INVEST/REDEEM rows; summary computed                            |
| Stock Credits / Dividends / Debits / Summary | 995 / 63 / 154 / 15 | `instruments` (type STOCK) + INVEST/REDEEM/INCOME rows; summary computed                           |
| Others                                       | 26                  | `instruments` (type OTHER) + INVEST/REDEEM rows                                                    |

---

## 3. Core model

### 3.1 Accounts: the things you hold cash in

```
accounts
  id, name
  account_type     TEXT NOT NULL  -- WALLET | BANK | CREDIT_CARD | DEMAT | PROVIDENT_FUND
  currency         TEXT NOT NULL DEFAULT 'INR'
  balance          REAL NOT NULL
  account_number   TEXT            -- bank / card / demat client id / UAN
  credit_limit, statement_day, due_day   -- card only
  display_order    INTEGER NOT NULL
  deleted          INTEGER NOT NULL DEFAULT 0
  notes, created_time, modified_time
```

Rows after migration: 33 wallets, 15 banks, 9 credit cards, `Zerodha Demat INR` (today's Bank17) and
a new
`Zerodha Demat USD` (currency USD), and two PROVIDENT_FUND accounts `EPF` and `PPF`. PF accounts
behave like banks whose balance is the passbook value: contributions and interest are INCOME rows
into them, PPF deposits are TRANSFERs from a bank. About 60 rows in total, stable.

### 3.2 Instruments: the things you invest in

```
instruments
  id, name, short_name
  instrument_type   TEXT NOT NULL  -- STOCK | MUTUAL_FUND | FIXED_DEPOSIT | INSURANCE | OTHER
  isin, symbol, exchange (NSE|BSE|NASDAQ|NYSE|AMFI)
  currency          TEXT NOT NULL DEFAULT 'INR'
  category_id       FK investment_categories   -- Equity, ETF, Gold ETF, REIT, Arbitrage, Flexi Cap, Life Insurance, ...
  owner             TEXT NOT NULL DEFAULT 'Self'   -- Appa Amma | Doddamma for family FDs (not imported yet)
  active            INTEGER NOT NULL DEFAULT 1
  display_order, notes, created_time, modified_time

fixed_deposits (1:1, FIXED_DEPOSIT)
  instrument_id PK, certificate_no, holder_name, bank_name, branch, bank_account_no, cust_id,
  period_months, period_days, rate, compounding_months, maturity_date, maturity_amount_printed,
  tds_rate, tds_reported, status (ACTIVE|CLOSED_AT_MATURITY|RENEWED|CLOSED_PREMATURELY),
  previous_fd_id FK, next_fd_id FK, notes
  -- deposit amount and date are NOT stored here: they are the INVEST row

insurance_policies (1:1, INSURANCE)
  instrument_id PK, insurance_type (LIFE|HEALTH|MOTOR|OTHER), provider, policy_name, policy_number,
  sum_assured, premium_amount, premium_frequency_months, start_date, maturity_date, next_due_date,
  counts_as_investment

investment_categories
  id, name, parent_id, risk_bucket (NONE|LOW|MEDIUM|HIGH), target_pct, tolerance_pct,
  target_goal_pct, target_wealth_pct, rebalance_notes, display_order

instrument_valuations     -- prices / values over time; latest row is "current"
  id, instrument_id FK, date, unit_price, total_value, source (YAHOO|AMFI|MANUAL|FD_FORMULA), fetched_at

fx_rates
  id, date, base, quote, rate, source
```

Instruments never have a stored balance: units held, cost basis, realized and held P/L, dividends
and XIRR are computed from the ledger plus the latest valuation. FD current value is computed from
the INVEST row (amount, date)
and the detail row (rate, compounding) or taken from a MANUAL valuation.

### 3.3 Transactions: one ledger, five kinds

```
transactions
  id, created_time, modified_time, date
  kind                 TEXT NOT NULL  -- INCOME | EXPENSE | TRANSFER | INVEST | REDEEM
  from_account_id      FK accounts    -- EXPENSE, TRANSFER, INVEST (NULL = money came from outside)
  to_account_id        FK accounts    -- INCOME, TRANSFER, REDEEM  (NULL = money went outside)
  category_id          FK categories  -- EXPENSE always; INCOME optional (Salary, Interest & Dividends, Refund…); BOTH-scoped categories (Friends & Family, Loans & Repayments) usable either way
  instrument_id        FK instruments -- INVEST/REDEEM required; INCOME/EXPENSE optional tag (dividend source,
                                      --   charges attributed to a holding)
  particulars          TEXT
  quantity, rate       REAL           -- when an instrument is involved
  amount               REAL NOT NULL  -- in `currency`
  currency             TEXT NOT NULL DEFAULT 'INR'
  fx_rate              REAL           -- native → INR when currency ≠ INR (also used for INR↔USD transfers)
  ex_date              TEXT           -- dividends
  contributor          TEXT           -- PF: Employer | Employee | Self | Interest
  broker_ref           TEXT UNIQUE    -- Zerodha trade_id / order_id for idempotent re-import
  hidden, include_in_counters INTEGER
indexes: (date), (from_account_id,date), (to_account_id,date), (category_id,date), (instrument_id,date)
```

Every event in the sheet maps onto these five kinds:

| Event                                                | kind                                      | from             | to           | instrument          | notes                                                 |
|------------------------------------------------------|-------------------------------------------|------------------|--------------|---------------------|-------------------------------------------------------|
| Salary                                               | INCOME                                    | —                | bank         | —                   | category Salary                                       |
| Dividend / FD interest paid out                      | INCOME                                    | —                | bank         | stock / FD          | category Interest & Dividends                         |
| **Employer / employee PF contribution, PF interest** | INCOME                                    | —                | EPF          | —                   | contributor set                                       |
| Bonus shares                                         | INCOME                                    | —                | —            | stock               | amount 0, quantity n                                  |
| Expense                                              | EXPENSE                                   | wallet/bank/card | —            | —                   | category                                              |
| DP / Zerodha charges                                 | EXPENSE                                   | Demat            | —            | stock (optional)    | category Trading                                      |
| Bank → bank, card payment                            | TRANSFER                                  | bank             | bank / card  | —                   |                                                       |
| Fund Demat USD                                       | TRANSFER                                  | bank (INR)       | Demat USD    | —                   | amount USD, fx_rate                                   |
| PPF deposit                                          | TRANSFER                                  | bank             | PPF          | —                   |                                                       |
| Buy stock / MF / SIP                                 | INVEST                                    | Demat / bank     | —            | stock / MF          | qty @ rate                                            |
| Buy US stock                                         | INVEST                                    | Demat USD        | —            | US stock            | amount USD, fx_rate                                   |
| Create FD, pay premium, "Others"                     | INVEST                                    | bank             | —            | FD / policy / other |                                                       |
| Sell stock / redeem MF                               | REDEEM                                    | —                | Demat / bank | stock / MF          | qty @ rate; profit computed                           |
| FD matures                                           | REDEEM                                    | —                | bank         | FD                  | amount credited                                       |
| **FD renewed**                                       | REDEEM (to = NULL) + INVEST (from = NULL) |                  |              | old FD, new FD      | created together by "Renew"; linked via previous/next |
| TDS on FD                                            | EXPENSE                                   | —                | —            | FD                  | category Tax; or just `tds_reported` in detail        |
| Family FD (later)                                    | INVEST                                    | — (NULL)         | —            | FD owner ≠ Self     | **never income**, excluded from own totals            |

Balance effects: INCOME adds to `to`; EXPENSE subtracts from `from`; TRANSFER both; INVEST subtracts
from `from`; REDEEM adds to `to`. A NULL account side means no balance change.

**Counters, derived:** income = INCOME into WALLET/BANK/CREDIT_CARD/DEMAT accounts only, so PF
contributions do not inflate income; spent = EXPENSE from those accounts; savings = INVEST from
cash + TRANSFER into PROVIDENT_FUND

+ INCOME into PROVIDENT_FUND; withdrawals = REDEEM into cash. Family FD rows have NULL accounts and
  owner ≠ Self, so they touch none of these.

Display labels derive from kind + endpoints: "Bought GOLDBEES 10 @ 180.05", "Sold MON100 50 @
52.10",
"Dividend · EMBASSY", "Employer PF", "FD #35 created", "Renewed FD #8 → #35", "Expense · Food",
"Income · Salary".

### 3.4 Categories (replaces `expenditure_types`)

```
categories
  id, name, parent_id (NULL = top level), applies_to (EXPENSE | INCOME | BOTH),
  icon, color, display_order, deleted
```

- All string parsing and splitting disappears: a transaction holds `category_id`, statistics
  `GROUP BY` it, the type string and the per-type `counters` columns are gone.
- Two levels: e.g. Food → Groceries / Restaurants / Snacks; Travels → Flights / Trains / Local.
  Statistics show the parent and expand to children. Existing 21 expenditure types migrate as
  top-level EXPENSE categories; you can split them later and reassign transactions in bulk from the
  filter screen.
- Income categories (Salary, Interest & Dividends, Refund, Others; plus Friends & Family and Loans &
  Repayments scoped to both) give the Income statistics
  the breakdown they never had. Optional per transaction.
- Spending categories and `investment_categories` (Equity, Mid Cap, REIT, …) stay separate
  taxonomies.

### 3.5 Credit cards

`account_type = CREDIT_CARD`, signed balance kept (negative = outstanding) so the ledger has no card
special case; display shows "Outstanding ₹X" in red, net worth subtracts it, `credit_limit` gives
available credit.

### 3.6 Remove `counters`

Measured on the real DB (Mac, incl. process start-up): monthly spent/income over all years 17 ms,
per-type totals for a year 6 ms, per-day totals for one type 9 ms. Statistics become indexed
queries. A per-month cache can be added later behind the same interface if ever needed.

### 3.7 Remove Bank SMS

Drop `sms_name`, the SMS receiver, SMS permissions, the setting and `ACTION_BANK_SMS`.

### 3.8 Display order

`display_order` on `accounts`, `instruments`, `categories`, seeded from id.
`ORDER BY display_order, id`. Deleted rows keep their value; restore appends at the end. Drag-handle
reorder within each group. Pickers carry ids.

### 3.9 Migration from v4 (in-app, first launch of 5.0.0, one SQLite transaction)

1. `accounts` ← wallets (WALLET) + banks (BANK / CREDIT_CARD by hard-coded list of the 9 card names;
   `Zerodha Demat` → DEMAT, renamed `Zerodha Demat INR`). Ids remapped; a mapping table drives steps
   2–3.
2. `categories` ← expenditure_types (top level, EXPENSE).
3. `transactions.type` → `kind` + FK columns; `templates` likewise. (Implemented as a one-way
   importer from the v4 file into the empty v5 database, not an in-place Room migration: the v4 file
   is never modified.)
4. Bank16 rows → instruments and INVEST/REDEEM/INCOME rows by particulars pattern
   (`Bought/Sold Stocks - X`,
   `Invested In Mutual Funds - X`, `SIP Installment - X`,
   `Dividend From Stocks - X (Ex-Dividend Date …)`,
   `Created A Fixed Deposit …`, `Fixed Deposit Matured …`, `… Interest Credited`,
   `Life/Health Insurance Premium …`,
   `Provident Fund …` → INCOME/TRANSFER into the new EPF/PPF accounts). `Profit/Loss In Selling`
   rows are dropped after the computed realized P/L is checked against their sum. The 25 unmatched
   rows (transfers to Amma's KVGB account, the loan to Mahadeva Anna) become one OTHER instrument
   "Family transfers" like the sheet's "Others".
5. Delete Bank16 when empty. Drop `counters`, `wallets`, `banks`, `expenditure_types`.
6. Abort and list offending rows if anything fails to map. Rehearsed on DB copies with `sqlite3` and
   an instrumented test.

Then on the phone: DB-file backup → desktop xlsx backfill script (pre-2024 history, own FDs, PF
ledger, insurances, categories, targets/tolerances/notes, ISINs from tradebook CSVs; family FDs
skipped) → reconcile totals against the sheet → restore.

---

## 4. UI

### 4.1 Summary

Collapsible Wallets / Banks / Credit Cards sections with subtotals (Demat INR/USD listed under
Banks, USD shown with INR equivalent), collapsed state persisted. Then an Investments line (invested
vs current, incl. PF balances), Amount Spent / Income for the interval, Net worth. Navigation order:
Summary, Transactions, **Investments**, Edit Data, Statistics, Extras.

### 4.2 Add / edit transaction

Kind selector: Income · Expense · Transfer · Invest · Redeem. Account field opens a bottom sheet
(search, Recent row, Wallets / Banks / Credit Cards / PF sections with balances, pre-filled with
last used). Instrument field (Invest / Redeem, optional tag on Income/Expense) opens a second bottom
sheet with search and type sections, with a
"+ New instrument" entry. Quantity and rate appear for stock/MF instruments. Transfer hides the
chosen source.

### 4.3 Investments

- **Dashboard**: Investment Summary sheet live; category rows expandable to sub-types; invested,
  current, held and realized P/L, XIRR, % vs target with drift; risk buckets; In Zerodha / Liquid /
  Total; last refresh; refresh.
- **Savings**: monthly and yearly tables as in the sheet plus a bar chart.
- **Fixed deposits**: ACTIVE by default; sort by name, bank, amount, deposit date, maturity date,
  est. value, days left; filter by bank, status, owner. Detail with all sheet columns, computed
  maturity/TDS, renewal chain (previous → this → next) and actions Renew / Mark matured / Close
  prematurely.
- **PF / Insurance**: EPF and PPF ledgers (contributor breakdown, interest), policies with premium
  ledger and next due; `counts_as_investment`.
- **Stocks & MFs**: holdings by category (units, avg cost, LTP, value, P/L, dividends, XIRR), INR +
  USD for US holdings; instrument detail with ledger, dividends with TDS, corporate actions.
- **Zerodha import** on the new model: instruments created/updated by ISIN, dedupe on `broker_ref`;
  US-stock importer once the first statement format is known.

### 4.4 Prices and FX

`PriceSource` interface: AMFI for MF NAVs, Yahoo Finance chart endpoint for Indian/US stocks and
`USDINR=X`
(unofficial; stored, UI shows "as of"); manual override. Fetch on dashboard open, hourly throttle,
manual refresh.

### 4.5 Excel export

Values-only workbook (POI `SXSSFWorkbook`): Savings Yearly, Savings Monthly, Investment Summary,
Fixed Deposits, Provident Funds, Insurances, Mutual Funds Summary, Stocks Summary, Others. No
MF/stock ledger sheets.

### 4.6 Backup

- **DB-file backup** (byte copy of `expenditureManager`) replaces `.snb`. No `.snb` support in
  5.0.0; 4.5.4 produces DB-file backups.
- **Save to any location** via the system file picker (`ACTION_CREATE_DOCUMENT`): with the Google
  Drive app installed, "Save to Drive" appears in that dialog. Zero setup, manual, available in
  milestone A.
- **Automatic upload to Google Drive** (Drive REST API, scope `drive.file`, visible folder
  `FinanceManager Backups`, so files can be downloaded from drive.google.com and restored with the
  normal restore): runs after the daily backup via WorkManager when online, plus a manual "Upload
  now". Needs a Google Cloud project with an OAuth client for the app's package + signing SHA-1
  (one-time, steps will be written down). `drive.file` is a non-sensitive scope, so the consent
  screen can be set to Production without Google verification; keep it out of "Testing"
  status, where refresh tokens expire every 7 days. Restore from Drive is deferred as agreed.

---

## 5. Technology

- **Kotlin, whole app.** Every file that touches the DB or the type string is rewritten in Kotlin
  against Room:
  that is `DatabaseAdapter`, `DatabaseManager`, all transaction layouts, filters, statistics,
  summary, edit screens, importers, backup/restore, parser, templates, which is most of the app. The
  few files left (Settings, Splash, custom views, PDF export) are converted with the IDE's
  Java→Kotlin converter near the end so 5.0.0 ships as a single-language codebase. No mixed codebase
  to maintain afterwards.
- **Room** for the schema, DAOs, `@Transaction` atomicity and versioned migrations; the v4→v5
  migration is a Room
  `Migration(1, 2)` with hand-written SQL.
- **Jetpack Compose** for every screen that is rebuilt (Summary, Add/Edit transaction with bottom
  sheets, Edit data with reorder, Transactions list, Statistics, all Investments screens). XML
  remains only on screens not touched.
- Coroutines for imports, price fetching and Drive upload; WorkManager for the daily backup/upload.
- compileSdk/targetSdk 35; deps: room, compose BOM, material3, kotlinx-coroutines, work-runtime, poi
  (existing), play-services-auth / google-api-services-drive (milestone E).

---

## 6. Milestones (all ship together as v5.0.0)

| # | Milestone                 | Content                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|---|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A | Foundations               | Step zero DONE; Kotlin/Room/Compose set up DONE (AGP 9.5, Gradle 9.8.2, compileSdk 37); Room schema v1 DONE (`app/schemas/.../1.json`); v4 importer DONE (`data/importer/v4`, tested on the real DB: 16,442 tx, 59 accounts, 95 instruments, 113 realised-P/L rows dropped, 246 FD rows on a placeholder instrument); Compose shell DONE (`ui/MainActivity` is the launcher: first-run Welcome screen offers the same choices as Finance Manager had, in order: Set up (5-step wizard: wallets, banks, credit cards, expense categories, income categories), Restore a backup (a `Finance Tracker Database - ….db` file, validated then swapped in via `DatabaseRestore`), Skip set-up (zero-balance Cash wallet + default categories), and last the one-time Import of a 4.5.4 `Database Backup - ….db`; drawer with Summary/Transactions/Investments/Edit Data/Statistics/Extras/Settings/Help/About; Summary shows collapsible account sections from Room; Extras has 'Back up database file' (Documents/Finance Tracker/Backups via MediaStore) and 'Save database file to…' (ACTION_CREATE_DOCUMENT) using `data/backup/DatabaseBackup` with WAL checkpoint); own prefs file `finance_tracker_prefs`. Legacy Java screens still compiled but unreachable until B deletes them |
| B | Accounts & categories UI  | Summary with collapsible sections, net worth, USD display; credit cards; reorder; bottom-sheet pickers; Add/Edit transaction on five kinds; categories with sub-categories and income categories; filters, statistics, templates on FK columns                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| C | Investments core          | Instruments, investment categories, holdings math (units, avg cost, realized/held P/L, XIRR), Zerodha importers, Stocks/MF screens, dashboard with manual prices                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| D | FD / PF / Insurance       | FD list/detail/renewal chain/actions; PF and insurance screens; Savings monthly/yearly                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| E | Prices, FX, export, Drive | AMFI + Yahoo, FX, US stocks, targets/drift/risk, Excel export, Drive auto-upload                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| F | Backfill & switch-over    | xlsx backfill script, reconciliation, migration dry-runs, release 5.0.0                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |

Later fixes to 5.0.x, new features to 5.x.

---

## 7. Decision log

| Date       | Decision                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-11-03 | `accounts` = wallets, banks, credit cards, Zerodha Demat INR/USD, EPF, PPF only; column `account_type`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| 2026-11-03 | Instruments (stocks, MFs, FDs, insurance, others) in a separate `instruments` table; no account per instrument                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| 2026-11-03 | Transactions: five kinds INCOME/EXPENSE/TRANSFER/INVEST/REDEEM with FK endpoints and `instrument_id`; type string removed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| 2026-11-03 | Investments pseudo-bank removed; realized P/L computed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| 2026-11-03 | `counters` removed; statistics by query. Bank SMS removed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| 2026-11-03 | Credit cards keep signed balance (negative = outstanding)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| 2026-11-03 | Family FDs supported via `owner` but not imported now; they are INVEST rows with no account, never income                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| 2026-11-03 | PF contributions are INCOME into PF accounts; the income counter counts cash accounts only                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| 2026-11-03 | Expenditure types → hierarchical `categories` with income categories                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| 2026-11-03 | Yahoo Finance (unofficial) acceptable behind an interface                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| 2026-11-03 | Excel export: summary sheets only                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| 2026-11-03 | Investments top-level after Transactions, before Edit Data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| 2026-11-03 | One v5.0.0 release; milestones internal                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 2026-11-03 | Kotlin for the whole app by 5.0.0; Room; Compose for rebuilt screens                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| 2026-11-03 | Google Drive: save-to-location now, automatic upload in milestone E, restore deferred                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| 2026-11-03 | Package renamed to `com.nagabhushansn95.financemanager`; 5.0.0 coexists with 4.5.4 on the phone                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| 2026-11-03 | 4.5.4 gets a DB-file backup action; `.snb` dropped from 5.0.0                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| 2026-11-03 | OAuth: one Cloud project, two Android clients (debug and release SHA-1), consent screen in Production                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| 2026-11-03 | Room runs on the bundled SQLite driver (`androidx.sqlite:sqlite-bundled`), not framework SQLite: measured 8x faster bulk inserts on the emulator (the framework compatibility path re-prepares statements per row), one SQLite version on every Android release, `VACUUM INTO` available. Consequence: transactions use `useWriterConnection { immediateTransaction { } }`; room-ktx `withTransaction` throws in driver mode. Unit tests use the same configuration via `AppDatabase.inMemory/create`; the host native library comes from `sqlite-bundled-jvm` through the Gradle task `extractSqliteHostNatives`. |
| 2026-11-03 | Database-file backup = `VACUUM INTO` snapshot streamed out with header bytes 18/19 set to 1 (rollback journal), so the file opens read-only on a desktop without -wal/-shm files. Written to Documents/Finance Tracker/Backups via MediaStore, or anywhere via ACTION_CREATE_DOCUMENT.                                                                                                                                                                                                                                                                                                                             |
| 2026-11-03 | `ui.MainActivity` (Compose) is the launcher; the first run keeps Finance Manager's three options (Set up wizard, Restore, Skip) and adds the 4.x import as the last option, since it is a one-time case. Restore is first-run only for now (nothing to lose); restore over existing data comes later with a confirmation. Legacy Java activities stay compiled but unreachable until milestone B deletes them.                                                                                                                                                                                                     |

## 8. Backlog / later ideas

- Import family FDs (Appa Amma, Doddamma) from the sheet.
- Restore from Google Drive inside the app.
- FD maturity and insurance premium notifications.
- Capital-gains report per financial year (STCG/LTCG).
- Goal tracking; rebalance suggestions ("Exit in this sequence").
- `opening_balance` on cash accounts and a "recompute balance from ledger" check.
- Multi-currency wallets shown in native currency with INR equivalent.
