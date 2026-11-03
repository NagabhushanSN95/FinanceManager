package com.nagabhushansn95.financetracker.data.importer.v4

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.Converters
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.FdStatus
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InsuranceType
import com.nagabhushansn95.financetracker.data.db.entity.Template
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate
import java.time.LocalDateTime

/** A v4 row that was intentionally not imported. */
data class DroppedRow(
    val v4Id: Int,
    val type: String,
    val particulars: String,
    val amount: Double,
    val reason: String
)

data class V4ImportReport(
    val accounts: Int,
    val categories: Int,
    val instruments: Int,
    val transactionsByKind: Map<TransactionKind, Int>,
    val templates: Int,
    val dropped: List<DroppedRow>,
    /** Realised profit (+) / loss (-) per instrument name, summed from the dropped bookkeeping rows, for later verification. */
    val realisedPnlByInstrument: Map<String, Double>,
    val notes: List<String>,
) {
    val transactions: Int get() = transactionsByKind.values.sum()

    fun summary(): String = buildString {
        appendLine("Accounts: $accounts, categories: $categories, instruments: $instruments, templates: $templates")
        appendLine(
            "Transactions: $transactions " + transactionsByKind.entries.joinToString(
                ", ",
                "(",
                ")"
            ) { "${it.key}=${it.value}" })
        appendLine("Dropped rows: ${dropped.size}")
        notes.forEach { appendLine("- $it") }
    }
}

/**
 * One-way import of a Finance Manager 4.x database into the (empty) v5 database.
 *
 * Mapping (see plan section 3.9): wallets and banks become `accounts` (the "Investments" pseudo-bank is dissolved into
 * instruments), expenditure types become `categories`, and every transaction's positional type string becomes
 * `kind` + foreign keys. Rows that only existed to keep the pseudo-bank at cost basis (realised profit / loss) are
 * dropped and reported, since v5 computes realised P/L from the ledger.
 */
class V4Importer(private val db: AppDatabase) {
    companion object {
        const val INVESTMENT_INCOME_CATEGORY = "Interest & Dividends"
    }


    suspend fun import(source: V4Source, now: LocalDateTime = LocalDateTime.now()): V4ImportReport =
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                require(db.transactionDao().count() == 0 && db.accountDao().getAll().isEmpty()) {
                    "The v5 database must be empty before importing v4 data"
                }
                Session(source, now).run()
            }
        }

    private inner class Session(private val source: V4Source, private val now: LocalDateTime) {
        private val accountIds = HashMap<String, Long>()          // "W1" / "B3" -> accounts.id
        private var pseudoBankKey: String? = null                 // "B16"
        private val instrumentIds = HashMap<InstrumentKey, Long>()
        private var nextAccountOrder = 0
        private var nextInstrumentOrder = 0
        private var epfId = 0L
        private var ppfId = 0L
        private var investmentIncomeCategoryId = 0L
        private val categoryIds = HashSet<Long>()
        private val dropped = ArrayList<DroppedRow>()
        private val realised = LinkedHashMap<String, Double>()
        private val notes = ArrayList<String>()
        private val kindCounts = LinkedHashMap<TransactionKind, Int>()
        private val pfBalance = HashMap<Long, Double>()
        private var fdPlaceholderRows = 0

        suspend fun run(): V4ImportReport {
            importAccounts()
            importCategories()
            val txCount = importTransactions()
            val templateCount = importTemplates()
            finalisePfBalances()
            if (fdPlaceholderRows > 0) {
                notes += "$fdPlaceholderRows fixed-deposit rows were attached to the placeholder instrument " +
                        "'${InstrumentKey.FD_UNMAPPED.name}'; split them into real FDs with the sheet backfill."
            }
            return V4ImportReport(
                accounts = accountIds.size + 3,
                categories = categoryIds.size,
                instruments = instrumentIds.size,
                transactionsByKind = kindCounts,
                templates = templateCount,
                dropped = dropped,
                realisedPnlByInstrument = realised,
                notes = notes,
            ).also { check(it.transactions == txCount) }
        }

        // ---------------------------------------------------------------- accounts

        private suspend fun importAccounts() {
            val dao = db.accountDao()
            for (w in source.wallets()) {
                val id = dao.insert(
                    Account(
                        name = w.name,
                        accountType = AccountType.WALLET,
                        currency = currencyFromName(w.name),
                        balance = w.amount,
                        displayOrder = ++nextAccountOrder,
                        deleted = w.deleted,
                        createdTime = now,
                        modifiedTime = now,
                    )
                )
                accountIds["W${w.id}"] = id
            }
            for (b in source.banks()) {
                if (b.name.equals("Investments", ignoreCase = true)) {
                    pseudoBankKey = "B${b.id}"
                    notes += "Pseudo-bank '${b.name}' (Bank${b.id}, balance ${"%.2f".format(b.balance)}) dissolved into instruments."
                    continue
                }
                val isDemat = b.name.contains("Demat", ignoreCase = true)
                val type = when {
                    b.name.contains("Credit Card", ignoreCase = true) -> AccountType.CREDIT_CARD
                    isDemat -> AccountType.DEMAT
                    else -> AccountType.BANK
                }
                val name =
                    if (isDemat && !b.name.contains("INR") && !b.name.contains("USD")) "${b.name} INR" else b.name
                val id = dao.insert(
                    Account(
                        name = name,
                        accountType = type,
                        currency = currencyFromName(b.name),
                        balance = b.balance,
                        accountNumber = b.accountNumber?.takeIf { it.isNotBlank() && it != "0000" && it != "0" },
                        displayOrder = ++nextAccountOrder,
                        deleted = b.deleted,
                        createdTime = now,
                        modifiedTime = now,
                    )
                )
                accountIds["B${b.id}"] = id
            }
            dao.insert(
                Account(
                    name = "Zerodha Demat USD",
                    accountType = AccountType.DEMAT,
                    currency = "USD",
                    balance = 0.0,
                    displayOrder = ++nextAccountOrder,
                    createdTime = now,
                    modifiedTime = now,
                )
            )
            epfId = dao.insert(
                Account(
                    name = "EPF",
                    accountType = AccountType.PROVIDENT_FUND,
                    displayOrder = ++nextAccountOrder,
                    createdTime = now,
                    modifiedTime = now
                )
            )
            ppfId = dao.insert(
                Account(
                    name = "PPF",
                    accountType = AccountType.PROVIDENT_FUND,
                    displayOrder = ++nextAccountOrder,
                    createdTime = now,
                    modifiedTime = now
                )
            )
            pfBalance[epfId] = 0.0
            pfBalance[ppfId] = 0.0
        }

        private fun currencyFromName(name: String): String {
            val last = name.trim().substringAfterLast(' ')
            return if (last in setOf("USD", "SGD", "AUD", "EUR", "GBP")) last else "INR"
        }

        // ---------------------------------------------------------------- categories

        private suspend fun importCategories() {
            val dao = db.categoryDao()
            var maxId = 0L
            for (e in source.expenditureTypes()) {
                val id = dao.insert(
                    Category(
                        id = e.id.toLong(),
                        name = e.name,
                        displayOrder = e.id,
                        deleted = e.deleted
                    )
                )
                categoryIds += id
                if (id > maxId) maxId = id
            }
            // One income category covers both; the instrument on the row tells dividends from interest.
            investmentIncomeCategoryId = dao.insert(
                Category(
                    name = INVESTMENT_INCOME_CATEGORY,
                    appliesTo = CategoryScope.INCOME,
                    displayOrder = (maxId + 1).toInt()
                )
            )
            categoryIds += investmentIncomeCategoryId
        }

        private fun categoryId(expTypeId: Int, v4Id: Int): Long {
            val id = expTypeId.toLong()
            if (id !in categoryIds) throw V4ImportException("Transaction $v4Id references unknown expenditure type $expTypeId")
            return id
        }

        // ---------------------------------------------------------------- instruments

        private suspend fun instrumentId(key: InstrumentKey): Long = instrumentIds[key] ?: run {
            val dao = db.instrumentDao()
            val id = dao.insert(
                Instrument(
                    name = key.name,
                    instrumentType = key.type,
                    symbol = key.symbol,
                    exchange = key.exchange,
                    displayOrder = ++nextInstrumentOrder,
                    createdTime = now,
                    modifiedTime = now,
                    notes = if (key == InstrumentKey.FD_UNMAPPED) "Placeholder created by the v4 import. Split into real FDs." else null,
                )
            )
            when (key.type) {
                InstrumentType.FIXED_DEPOSIT -> dao.upsertFixedDeposit(
                    FixedDeposit(
                        instrumentId = id,
                        status = FdStatus.ACTIVE
                    )
                )

                InstrumentType.INSURANCE -> {
                    val provider = key.name.substringBefore(" - ", missingDelimiterValue = "")
                        .takeIf { it.isNotBlank() }
                    val policyName =
                        key.name.substringAfter(" - ", missingDelimiterValue = key.name)
                    dao.upsertInsurancePolicy(
                        InsurancePolicy(
                            instrumentId = id,
                            insuranceType = InsuranceType.LIFE,
                            provider = provider,
                            policyName = policyName,
                            countsAsInvestment = true
                        )
                    )
                }

                else -> Unit
            }
            instrumentIds[key] = id
            id
        }

        // ---------------------------------------------------------------- transactions

        private suspend fun importTransactions(): Int {
            val rows = source.transactions()
            val out = ArrayList<TransactionEntity>(rows.size)
            for (t in rows) {
                val entity = mapTransaction(t)
                if (entity != null) {
                    out += entity
                    kindCounts[entity.kind] = (kindCounts[entity.kind] ?: 0) + 1
                    applyPfBalance(entity)
                }
            }
            out.chunked(1000).forEach { db.transactionDao().insertAll(it) }
            return out.size
        }

        private fun accountId(ep: V4Endpoint, v4Id: Int): Long =
            accountIds[ep.key]
                ?: throw V4ImportException("Transaction $v4Id references unknown account $ep")

        private fun isPseudo(ep: V4Endpoint) = ep.key == pseudoBankKey

        private suspend fun mapTransaction(t: V4Transaction): TransactionEntity? {
            val base = TransactionEntity(
                id = t.id.toLong(),
                createdTime = parseTimestamp(t.createdTime, t.id),
                modifiedTime = parseTimestamp(t.modifiedTime, t.id),
                date = parseDate(t.date, t.id),
                kind = TransactionKind.INCOME,
                particulars = t.particulars,
                quantity = t.quantity.takeIf { it != 0.0 && it != 1.0 }
                    ?: t.quantity.takeIf { it != 0.0 },
                rate = t.rate.takeIf { it != 0.0 },
                amount = t.amount,
                hidden = t.hidden,
                includeInStatistics = t.includeInCounters,
            )
            return when (val type = V4TypeParser.parse(t.type)) {
                is V4Type.Credit -> if (isPseudo(type.to)) mapPseudoCredit(
                    t,
                    base
                ) else mapIncome(t, base, accountId(type.to, t.id))

                is V4Type.Debit -> if (isPseudo(type.from)) mapPseudoDebit(t, base) else mapExpense(
                    t,
                    base,
                    accountId(type.from, t.id),
                    type.expTypeId
                )

                is V4Type.Transfer -> when {
                    isPseudo(type.from) && isPseudo(type.to) -> throw V4ImportException("Transaction ${t.id}: transfer within the pseudo-bank")
                    isPseudo(type.to) -> mapInto(t, base, accountId(type.from, t.id))
                    isPseudo(type.from) -> mapOutOf(t, base, accountId(type.to, t.id))
                    else -> base.copy(
                        kind = TransactionKind.TRANSFER,
                        fromAccountId = accountId(type.from, t.id),
                        toAccountId = accountId(type.to, t.id),
                        quantity = null,
                        rate = null
                    )
                }
            }
        }

        private suspend fun mapIncome(
            t: V4Transaction,
            base: TransactionEntity,
            toId: Long
        ): TransactionEntity {
            V4InvestmentClassifier.dividend(t.particulars)?.let { d ->
                return base.copy(
                    kind = TransactionKind.INCOME,
                    toAccountId = toId,
                    instrumentId = instrumentId(InstrumentKey.stock(d.symbol)),
                    exDate = d.exDate,
                    categoryId = investmentIncomeCategoryId,
                )
            }
            val category =
                if (V4InvestmentClassifier.isInterest(t.particulars)) investmentIncomeCategoryId else null
            return base.copy(
                kind = TransactionKind.INCOME,
                toAccountId = toId,
                categoryId = category,
                quantity = null,
                rate = null
            )
        }

        private suspend fun mapExpense(
            t: V4Transaction,
            base: TransactionEntity,
            fromId: Long,
            expTypeId: Int
        ): TransactionEntity {
            val stock = V4InvestmentClassifier.chargedStock(t.particulars)
            return base.copy(
                kind = TransactionKind.EXPENSE,
                fromAccountId = fromId,
                categoryId = categoryId(expTypeId, t.id),
                instrumentId = stock?.let { instrumentId(InstrumentKey.stock(it)) },
            )
        }

        private suspend fun mapInto(
            t: V4Transaction,
            base: TransactionEntity,
            fromId: Long
        ): TransactionEntity =
            when (val ev = V4InvestmentClassifier.classifyInto(t.particulars)) {
                is InvestmentEvent.Invest -> {
                    if (ev.instrument == InstrumentKey.FD_UNMAPPED) fdPlaceholderRows++
                    base.copy(
                        kind = TransactionKind.INVEST,
                        fromAccountId = fromId,
                        instrumentId = instrumentId(ev.instrument)
                    )
                }

                is InvestmentEvent.ProvidentFundIn ->
                    base.copy(
                        kind = TransactionKind.TRANSFER,
                        fromAccountId = fromId,
                        toAccountId = pfAccount(ev.fund),
                        contributor = ev.contributor,
                        quantity = null,
                        rate = null
                    )

                null -> throw V4ImportException("Transaction ${t.id}: cannot classify investment '${t.particulars}' (${t.type})")
                else -> throw V4ImportException("Transaction ${t.id}: unexpected event $ev for '${t.particulars}' (${t.type})")
            }

        private suspend fun mapOutOf(
            t: V4Transaction,
            base: TransactionEntity,
            toId: Long
        ): TransactionEntity =
            when (val ev = V4InvestmentClassifier.classifyOutOf(t.particulars)) {
                is InvestmentEvent.Redeem -> {
                    if (ev.instrument == InstrumentKey.FD_UNMAPPED) fdPlaceholderRows++
                    base.copy(
                        kind = TransactionKind.REDEEM,
                        toAccountId = toId,
                        instrumentId = instrumentId(ev.instrument)
                    )
                }

                is InvestmentEvent.ProvidentFundOut ->
                    base.copy(
                        kind = TransactionKind.TRANSFER,
                        fromAccountId = pfAccount(ev.fund),
                        toAccountId = toId,
                        contributor = "Self",
                        quantity = null,
                        rate = null
                    )

                null -> throw V4ImportException("Transaction ${t.id}: cannot classify redemption '${t.particulars}' (${t.type})")
                else -> throw V4ImportException("Transaction ${t.id}: unexpected event $ev for '${t.particulars}' (${t.type})")
            }

        private suspend fun mapPseudoCredit(
            t: V4Transaction,
            base: TransactionEntity
        ): TransactionEntity? =
            when (val ev = V4InvestmentClassifier.classifyCredit(t.particulars)) {
                is InvestmentEvent.RealisedPnl -> drop(t, ev, +t.amount)
                is InvestmentEvent.ReinvestedIncome -> {
                    if (ev.instrument == InstrumentKey.FD_UNMAPPED) fdPlaceholderRows++
                    base.copy(
                        kind = TransactionKind.INCOME,
                        instrumentId = instrumentId(ev.instrument),
                        categoryId = investmentIncomeCategoryId,
                        quantity = null,
                        rate = null
                    )
                }

                is InvestmentEvent.ProvidentFundIn ->
                    base.copy(
                        kind = TransactionKind.INCOME,
                        toAccountId = pfAccount(ev.fund),
                        contributor = ev.contributor,
                        categoryId = if (ev.contributor == "Interest") investmentIncomeCategoryId else null,
                        quantity = null,
                        rate = null,
                    )

                null -> throw V4ImportException("Transaction ${t.id}: cannot classify pseudo-bank credit '${t.particulars}'")
                else -> throw V4ImportException("Transaction ${t.id}: unexpected event $ev for '${t.particulars}' (${t.type})")
            }

        private fun mapPseudoDebit(t: V4Transaction, base: TransactionEntity): TransactionEntity? =
            when (val ev = V4InvestmentClassifier.classifyDebit(t.particulars)) {
                is InvestmentEvent.RealisedPnl -> drop(t, ev, -t.amount)
                null -> throw V4ImportException("Transaction ${t.id}: cannot classify pseudo-bank debit '${t.particulars}'")
                else -> throw V4ImportException("Transaction ${t.id}: unexpected event $ev for '${t.particulars}' (${t.type})")
            }

        private fun drop(
            t: V4Transaction,
            ev: InvestmentEvent.RealisedPnl,
            signedAmount: Double
        ): TransactionEntity? {
            dropped += DroppedRow(
                t.id,
                t.type,
                t.particulars,
                t.amount,
                "Realised ${if (ev.isProfit) "profit" else "loss"} bookkeeping row; computed in v5"
            )
            realised[ev.instrument.name] = (realised[ev.instrument.name] ?: 0.0) + signedAmount
            return null
        }

        private fun pfAccount(fund: ProvidentFund): Long =
            if (fund == ProvidentFund.EPF) epfId else ppfId

        private fun applyPfBalance(e: TransactionEntity) {
            e.toAccountId?.let {
                if (it in pfBalance) pfBalance[it] = pfBalance.getValue(it) + e.amount
            }
            e.fromAccountId?.let {
                if (it in pfBalance) pfBalance[it] = pfBalance.getValue(it) - e.amount
            }
        }

        private suspend fun finalisePfBalances() {
            val stamp = Converters().localDateTimeToString(now)!!
            for ((id, balance) in pfBalance) db.accountDao().adjustBalance(id, balance, stamp)
        }

        // ---------------------------------------------------------------- templates

        private suspend fun importTemplates(): Int {
            var count = 0
            for (tpl in source.templates()) {
                val entity = mapTemplate(tpl) ?: continue
                db.transactionDao().insertTemplate(entity)
                count++
            }
            return count
        }

        private suspend fun mapTemplate(tpl: V4Template): Template? {
            val base = Template(
                particulars = tpl.particulars,
                kind = TransactionKind.INCOME,
                amount = tpl.amount.takeIf { it != 0.0 && it != 1.0 },
                hidden = tpl.hidden
            )

            fun instr(key: InstrumentKey?): Long? =
                null.also { /* templates do not pin an instrument when the v4 particulars had none */ }
            return when (val type = V4TypeParser.parse(tpl.type)) {
                is V4Type.Credit -> if (isPseudo(type.to)) {
                    when (val ev = V4InvestmentClassifier.classifyCredit(tpl.particulars + " x")) {
                        is InvestmentEvent.ProvidentFundIn -> base.copy(
                            kind = TransactionKind.INCOME,
                            toAccountId = pfAccount(ev.fund)
                        )

                        is InvestmentEvent.ReinvestedIncome -> base.copy(
                            kind = TransactionKind.INCOME,
                            instrumentId = instr(ev.instrument),
                            categoryId = investmentIncomeCategoryId
                        )

                        else -> null
                    }
                } else base.copy(
                    kind = TransactionKind.INCOME,
                    toAccountId = accountIds[type.to.key] ?: return null
                )

                is V4Type.Debit -> if (isPseudo(type.from)) null
                else base.copy(
                    kind = TransactionKind.EXPENSE,
                    fromAccountId = accountIds[type.from.key] ?: return null,
                    categoryId = type.expTypeId.toLong().takeIf { it in categoryIds })

                is V4Type.Transfer -> when {
                    isPseudo(type.to) -> when (val ev =
                        V4InvestmentClassifier.classifyInto(tpl.particulars + " x")) {
                        is InvestmentEvent.Invest -> base.copy(
                            kind = TransactionKind.INVEST,
                            fromAccountId = accountIds[type.from.key] ?: return null,
                            instrumentId = instrumentIds[ev.instrument]
                        )

                        is InvestmentEvent.ProvidentFundIn -> base.copy(
                            kind = TransactionKind.TRANSFER,
                            fromAccountId = accountIds[type.from.key] ?: return null,
                            toAccountId = pfAccount(ev.fund)
                        )

                        else -> null
                    }

                    isPseudo(type.from) -> when (val ev =
                        V4InvestmentClassifier.classifyOutOf(tpl.particulars + " x")) {
                        is InvestmentEvent.Redeem -> base.copy(
                            kind = TransactionKind.REDEEM,
                            toAccountId = accountIds[type.to.key] ?: return null,
                            instrumentId = instrumentIds[ev.instrument]
                        )

                        is InvestmentEvent.ProvidentFundOut -> base.copy(
                            kind = TransactionKind.TRANSFER,
                            fromAccountId = pfAccount(ev.fund),
                            toAccountId = accountIds[type.to.key] ?: return null
                        )

                        else -> null
                    }

                    else -> base.copy(
                        kind = TransactionKind.TRANSFER,
                        fromAccountId = accountIds[type.from.key] ?: return null,
                        toAccountId = accountIds[type.to.key] ?: return null
                    )
                }
            }
        }

        // ---------------------------------------------------------------- parsing helpers

        /** v4 dates are `yyyy/MM/dd`. */
        private fun parseDate(s: String, v4Id: Int): LocalDate {
            val p = s.trim().split('/')
            if (p.size != 3) throw V4ImportException("Transaction $v4Id: bad date '$s'")
            return LocalDate.of(p[0].toInt(), p[1].toInt(), p[2].toInt())
        }

        /** v4 timestamps are `yyyy/MM/dd/HH/mm/ss/S` with 1-3 millisecond digits. */
        private fun parseTimestamp(s: String, v4Id: Int): LocalDateTime {
            val p = s.trim().split('/')
            if (p.size < 6) throw V4ImportException("Transaction $v4Id: bad timestamp '$s'")
            val millis = p.getOrNull(6)?.toIntOrNull() ?: 0
            return LocalDateTime.of(
                p[0].toInt(),
                p[1].toInt(),
                p[2].toInt(),
                p[3].toInt(),
                p[4].toInt(),
                p[5].toInt(),
                millis.coerceIn(0, 999) * 1_000_000
            )
        }
    }
}
