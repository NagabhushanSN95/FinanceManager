# Schema v11 — the instrument side, redesigned

Status: BUILT (2026-11-13, same day): schema 11 with `Migration10To11`, 133 unit tests, migration
rehearsed on the emulator with the 13 Nov phone backup. Decisions below are as taken. Previously:
DECIDED (2026-11-13) — every decision D1…D14 taken as recommended, with D6 (keep owner), D13 (keep
fx_rate as the row's own rate) and D14 (convert on demand) settled in discussion. Nothing is built
yet. Sections 5 and 6 are build notes; the user reads the Rollout steps and the forex pairs in the
migration report.

## 1. Why

The v10 schema grew by accretion: `instruments` carries the stock and fund columns for every kind,
`insurance_policies` doubles as the vehicle table, `validity_periods` mixes vehicle records with
health covers, bonus and split rows sit in the cash ledger with a zero amount, and `settings` mixes
one-shot flags, script guards and a mirror of the phone's preferences. The ledger itself
(`transactions`, `accounts`, `categories`) is right and stays.

## 2. Principles

1. **One ledger for anything that moves cash.** Buys, sells, dividends, premiums, deposits,
   redemptions, loans out and back, charges: all are `transactions` rows. Account balances, the
   Transactions screen, statistics, savings, FIFO and the backup read one table.
2. **Parent + detail for instruments.** `instruments` is the slim row everything points at
   (transactions, pending dividends, summary groups, categories). Each kind has one detail table
   keyed by the instrument id. A kind with nothing extra has no detail table.
3. **Events that move no cash live beside the ledger, not in it.** Bonus units, split units, deposit
   interest credited, TDS deducted: `holding_events`.
4. **Settings hold data, not device state.** Three prefixes, nothing else.
5. **Porting is one Room migration in SQL.** No Python on the switch-over path; a restore of any v10
   backup migrates on first open.

## 3. Tables

Legend: *kept* = as in v10; *new*; *changed*; *dropped*.

### Cash side — kept

`accounts`, `categories`, `transactions` (one change below), `templates` (one change), `fx_rates`,
`fd_templates`.

- `transactions`: *changed* — bonus and split rows (kind INCOME, amount 0, quantity = units) move to
  `holding_events`. Every remaining row moves money. `contributor` stays for provident funds,
  `ex_date` for dividends, `broker_ref` for imports. **D13**: `to_amount` added — a transfer between
  accounts of different currencies is one row: `amount` leaves the from-account in its currency,
  `to_amount` arrives in the to-account's currency (16953 INR out, 300 AUD in). `fx_rate` stays,
  redefined (decided 2026-11-13): rupees per unit of the row's foreign currency, filled when the row
  itself knows it — computed on a load from `amount`/`to_amount`, typed (or prefilled from
  `fx_rates`) on a foreign credit with no load behind it, null on a spend meaning "the pool's
  average". `currency` goes: a row's currency is its account's. Today the same event is an EXPENSE
  plus an INCOME, which also wrongly counts in statistics as spending and earning.
- `templates`: *changed* — `instrument_id` dropped (no row uses it; the editor is cash-only).

### Investment categories — kept

`investment_categories`, `risk_levels`, `summary_groups` (members `type:`, `category:`,
`instrument:` as today; `type:VEHICLE` becomes possible).

### `instruments` — changed (slimmed)

| column                      | note                                                                                                                                                                                                                                      |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| id                          |                                                                                                                                                                                                                                           |
| kind                        | STOCK, MUTUAL_FUND, FIXED_DEPOSIT, INSURANCE, VEHICLE, OTHER (was `instrument_type`; VEHICLE is new, see D4)                                                                                                                              |
| name                        |                                                                                                                                                                                                                                           |
| category_id                 | FK investment_categories                                                                                                                                                                                                                  |
| active                      |                                                                                                                                                                                                                                           |
| owner                       | "Self" by default; Appa's or Amma's deposits later carry their name and stay out of own totals (the form, list filter, totals and hub already key on it). Distinct from a deposit's `holder_name`, which is whose name the bank has on it |
| notes                       |                                                                                                                                                                                                                                           |
| created_time, modified_time |                                                                                                                                                                                                                                           |

Dropped from the parent: `short_name, isin, symbol, exchange, series, currency` (→ `securities`),
`display_order` (nothing is reordered any more; lists sort by name) **D6**.

### `securities` — new (stocks, ETFs, REITs, mutual funds)

| column                | note                                    |
|-----------------------|-----------------------------------------|
| instrument_id         | PK, FK instruments CASCADE              |
| short_name            | shown on list rows and ledger lines     |
| isin                  |                                         |
| symbol                | ticker or AMFI scheme code              |
| exchange              | NSE, BSE, NASDAQ, NYSE, AMFI            |
| series                | RR for REITs/InvITs                     |
| folio                 | funds, from Coin                        |
| currency              | INR / USD                               |
| last_price            | **D2** replaces `instrument_valuations` |
| last_price_date       | the quote's own date                    |
| last_price_source     | YAHOO, AMFI, MANUAL                     |
| last_price_fetched_at |                                         |

One table for stocks and funds (**D1**): the shape is identical, only the price source differs, and
every screen that lists holdings treats them alike.

### `fixed_deposits` — changed

Kept:
`certificate_no, holder_name, bank_name, branch, bank_account_no, cust_id, period_months, period_days, rate, compounding_months, maturity_date, maturity_amount_printed, tds_reported, status, deposit_type, sweep_in_date, previous_fd_id, next_fd_id, notes`.

- `tds_rate`: *dropped* (the app estimates TDS; nothing reads it).
- `tds_yearly` → `is_tds_yearly` (**D3**, kept, renamed; no screen asks for it until you confirm the
  SBI/HDFC behaviour).
- TDS staging: the "so much TDS deducted from FD #n" SMS goes into `holding_events` kind
  TDS_DEDUCTED on that deposit (it is what `fd_events` is today, just unified). It touches no
  account and no total; later TDS work reads these rows.

### `insurance_policies` — changed

Life, term and health only. `vehicle_number` *dropped* (vehicles get their own table). Health's
per-renewal cover rows move to `health_covers`.

### `vehicles` — new (**D4**)

| column          | note                                      |
|-----------------|-------------------------------------------|
| instrument_id   | PK, FK instruments CASCADE (kind VEHICLE) |
| registration_no |                                           |
| make_model      | optional, new                             |
| bought_on       | optional, new                             |

### `vehicle_records` — new (replaces the vehicle half of `validity_periods`)

| column               | note                                                                    |
|----------------------|-------------------------------------------------------------------------|
| id                   |                                                                         |
| instrument_id        | FK vehicles CASCADE                                                     |
| kind                 | OWN_DAMAGE, THIRD_PARTY, SERVICE, EMISSION_TEST                         |
| start_date, end_date |                                                                         |
| provider             | insurer / garage                                                        |
| reference            | policy number / invoice                                                 |
| transaction_id       | the premium or bill that paid for it, FK transactions CASCADE, nullable |

### `health_covers` — new (replaces the COVER half of `validity_periods`) (**D5**)

| column               | note                          |
|----------------------|-------------------------------|
| id                   |                               |
| instrument_id        | FK insurance_policies CASCADE |
| start_date, end_date |                               |
| provider             | insurer for that year         |
| reference            | policy number for that year   |
| sum_insured          |                               |
| transaction_id       | the premium, nullable         |

### `holding_events` — new (replaces `fd_events` and the zero-amount ledger rows)

| column        | note                                          |
|---------------|-----------------------------------------------|
| id            |                                               |
| instrument_id | FK instruments CASCADE                        |
| date          |                                               |
| kind          | BONUS, SPLIT, INTEREST_CREDITED, TDS_DEDUCTED |
| units         | bonus/split: units added                      |
| amount        | interest/TDS: rupees                          |
| ratio         | "1:2" for bonus/split, display only           |
| notes         |                                               |

### `pending_dividends` — kept

### `settings` — changed (**D8**)

| key                                                                                                                                                                                                                                                        | note                                                                  |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| `data.goal_amount`                                                                                                                                                                                                                                         | the only real datum (was `investments.goal_amount`)                   |
| `prefs.alert_days_monthly`, `prefs.alert_days_within_year`, `prefs.alert_days_beyond_year`, `prefs.theme_mode`, `prefs.auto_prices`, `prefs.price_refresh_interval`, `prefs.daily_backup_enabled`, `prefs.drive_upload_enabled`, `prefs.drive_folder_path` | the preferences worth restoring; mirrored before each backup as today |

Dropped: `backfill.*` (script guards), `categories.renamed.equity_index_etf`,
`summary_groups.seeded`, `templates.investment_kinds_removed` (one-shot flags the migration makes
moot; the seed checks the data instead), and from the mirror every device/session value: last backup
path, restore URI, Zerodha statement URI, last import summary, last price summary and time, last
Drive upload, last export, last auto-backup date, FD list filter/sort, collapsed summary sections,
recent-value lists. Those stay in SharedPreferences on the phone only.

### Dropped tables

`instrument_valuations` (**D2**), `validity_periods`, `fd_events`.

## 4. Decisions for you

- **D1** One `securities` table for stocks and funds — *recommended* — or two tables with identical
  columns.
- **D2** Latest price on the security row, history dropped — *recommended*; the app only ever reads
  the latest row and the 46 rows on the phone are a week of fetches. Alternative: keep a
  `price_history` table too, for charts later. It can be added later without touching anything else.
- **D3** `tds_yearly` kept and renamed `is_tds_yearly`; `tds_rate` dropped; `tds_reported` kept. TDS
  staging = `holding_events` TDS_DEDUCTED.
- **D4** VEHICLE as its own instrument kind with `vehicles` + `vehicle_records` — *recommended*. New
  optional fields `make_model`, `bought_on`. Alternative: keep vehicles as MOTOR policies (nothing
  changes for them).
- **D5** `health_covers` separate from `vehicle_records` — *recommended*; they share only dates and
  a reference.
- **D6** Keep `owner` (decided 2026-11-13: family deposits will use it, and the form, filter, totals
  and hub are already wired to it); drop `display_order` — the one place it still matters is a
  tiebreak in the summary that name order replaces.
- **D7** Bonus/split out of the ledger into `holding_events` — *recommended*. Cost: the FIFO code
  reads two tables for a stock. Benefit: every ledger row moves money, so no amount-zero rows in
  Transactions, statistics or exports.
- **D8** Settings keys and the smaller mirror as above.
- **D9** Instrument kind name: `kind` with values as listed, or keep `instrument_type`. Cosmetic;
  *recommended* `kind` since it is the word the code uses.
- **D10** Others (loans, parked money): no detail table; the "Loan To …" name convention stays.
  Alternative: an `others` table with `counterparty` and `is_loan`. Only worth it if you want more
  fields there.
- **D11** The backfill script: freeze at v10 output and retire it after v11 — *recommended*. The
  port is done; from v11 on, any fix-up is a Room migration, and the script's touch-ups have all
  run.
- **D13** Cross-currency transfers as one row (`to_amount`), `fx_rate` kept as the row's own rate,
  `currency` dropped — *decided*. Loading a forex card or the Zerodha USD account becomes one
  Transfer; the editor shows a second amount field only when the two accounts' currencies differ;
  the markup and GST stay a separate expense as now. Migration: pair each "Bought … Forex" EXPENSE
  with its same-day INCOME into the card by hand-checkable rule (same date, one INR expense from a
  bank, one foreign income into a foreign account) and merge them, listing the pairs in the
  migration report for you to confirm; unmatched rows stay as they are. Statistics then treat them
  as transfers. Every row on the phone today is INR-labelled, so dropping `currency` loses nothing.
- **D14** Foreign-currency expenses in statistics — *decided 2026-11-13: option A, computed on
  demand*. Today: ported rows are INR-labelled and count at face value (a $88 dinner counts as ₹88);
  rows typed in 5.0 carry the account's currency but no rate and count as zero. In v11 nothing is
  stored per row: a function runs each foreign account's rows forward in date order (loads from D13
  transfers add dollars and the rupees they cost, spends take their share at the running average;
  FIFO is a one-line alternative) and returns the rupee value of every row. Statistics merge those
  into the period and category buckets beside the SQL sum of the INR rows; the sheet export uses the
  same function for its INR column; the ledger keeps showing foreign amounts with their symbol. A
  credit without a load behind it (Uber Cash, money paid back) joins the pool at the row's own
  `fx_rate`, typed or prefilled from `fx_rates`; a spend normally has no rate and takes the average,
  and one typed on a spend (the first from an empty pool) pins it. The function needs only the
  account's rows; `fx_rates` keeps valuing USD holdings and prefilling that field. Editing an
  earlier row changes nothing stored. Migration: relabel nothing (rows carry no currency any more);
  merge the load pairs per D13 and list in the report the foreign-account rows with no load before
  them.
- **D12** Timing: build v11 now, or after you have used 5.0 a while longer. Each later schema change
  costs a migration either way; v11 is the one that touches the most tables, so sooner is cheaper.

## 5. Porting (Room migration 10 → 11, plain SQL)

Order matters for the foreign keys. All inside one transaction; Room's `MIGRATION_10_11`.

1. `CREATE TABLE securities`;
   `INSERT … SELECT id, short_name, isin, symbol, exchange, series, NULL, currency, … FROM instruments WHERE instrument_type IN ('STOCK','MUTUAL_FUND')`.
   Latest price per instrument from `instrument_valuations` (max `date`, then max `fetched_at`).
   Folio: parse "Folio N" out of `notes` where present (the Coin import wrote it there).
2. `CREATE TABLE vehicles`;
   `INSERT … SELECT instrument_id, vehicle_number FROM insurance_policies WHERE insurance_type = 'MOTOR'`;
   `UPDATE instruments SET kind = 'VEHICLE'` for them;
   `DELETE FROM insurance_policies WHERE insurance_type = 'MOTOR'`.
3. `CREATE TABLE vehicle_records` ← `validity_periods WHERE kind != 'COVER'`;
   `CREATE TABLE health_covers` ← `validity_periods WHERE kind = 'COVER'`.
4. `CREATE TABLE holding_events` ← `fd_events` (INTEREST → INTEREST_CREDITED, TDS → TDS_DEDUCTED) ∪
   ledger rows with
   `amount = 0 AND quantity IS NOT NULL AND kind = 'INCOME' AND instrument_id IN (stocks)` (BONUS vs
   SPLIT from the particulars, ratio parsed from them), then `DELETE` those ledger rows.
5. Rebuild `instruments` with the slim columns (SQLite cannot drop columns with indexes cleanly:
   create `instruments_new`, copy, drop, rename, recreate indexes). Same for `fixed_deposits` (drop
   `tds_rate`, rename `tds_yearly`), `insurance_policies` (drop `vehicle_number`), `templates` (drop
   `instrument_id`).
6. `DROP TABLE instrument_valuations, validity_periods, fd_events`.
7. Settings: rename `investments.goal_amount` → `data.goal_amount`; delete `backfill.%`, the three
   one-shot flags, and the `prefs.%` keys not in the keep list.
8. Bump `AppDatabase` to 11, export `11.json`, migration test with a v10 fixture that has one row of
   every kind (stock with a bonus row, fund with a folio note, deposit with an fd_event, health
   policy with a COVER row, vehicle with OD/TP/service rows, Others).

### App code touched

Entities and DAOs for the new tables; `Holdings` (FIFO reads `holding_events` for bonus/split
units); `FixedDeposits.ledger` (reads `holding_events` instead of `fd_events`); `Insurance` /
vehicle screens (own entity, kind VEHICLE in `InvestmentOrder`, hub tile, drawer, summary groups);
price fetch writes `securities.last_*` instead of inserting valuations; `InvestmentSummary`, XIRR
and the hub read `last_price`; `HoldingsRepository.add`/`HoldingSheet` write `securities`; Zerodha
importer writes folio to the column; `V4Importer` writes the new shape; `DatabaseBackup` mirrors the
smaller prefs list; export (CSV/sheet) columns; Help text. Tests: migration, holdings (bonus from
events), FD ledger, insurance/vehicle, importers.

### Rollout

1. Extras › Backup on the phone (keeps a v10 file to fall back to).
2. Install the v11 build; the app migrates on first open. A restore of any older backup also
   migrates.
3. Rehearse first on the emulator with the latest phone backup; compare the hub totals, each
   holding's units and average cost, the deposit ledgers and the vehicle/health cards before and
   after.

## 6. Out of scope

Provident funds stay account-shaped (decided). The ledger's shape, categories, templates and the
Transactions screen do not change. No repo rename, no release.
