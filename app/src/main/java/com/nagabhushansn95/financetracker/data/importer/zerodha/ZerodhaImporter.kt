package com.nagabhushansn95.financetracker.data.importer.zerodha

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.PendingDividend
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.repository.HoldingsRepository
import com.nagabhushansn95.financetracker.data.repository.TransactionRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/** Which accounts the statement's money moves through, and whether to add the DP charge on each sale. */
data class ZerodhaImportSettings(
    /** Zerodha Demat INR: stocks are bought from and sold into it; charges come out of it. */
    val dematAccountId: Long?,
    /** The bank behind Coin mandates and dividend credits. */
    val bankAccountId: Long?,
    val addDpCharges: Boolean = true,
    val dpCharge: Double = DP_CHARGE,
) {
    companion object {
        const val DP_CHARGE = 15.34
    }
}

/** An instrument the import will create (id 0 until committed). */
data class NewInstrument(
    val name: String,
    val type: InstrumentType,
    val symbol: String?,
    val isin: String?,
    val exchange: String? = null,
    val series: String? = null,
    val folio: String? = null,
)

/** A dividend the statement announced, to wait for its bank credit. [instrument] is set when the stock is new. */
data class PlannedDividend(
    val dividend: PendingDividend,
    val instrument: NewInstrument? = null,
    val label: String,
)

/** One row the import will write. [instrument] is set when the row needs an instrument that does not exist yet. */
data class PlannedTransaction(
    val transaction: TransactionEntity,
    val instrument: NewInstrument? = null,
    val label: String
)

data class ImportPlan(
    val type: StatementType,
    val toInsert: List<PlannedTransaction>,
    /** Dividends that go to the pending list rather than the ledger (a dividends statement only). */
    val toHold: List<PlannedDividend> = emptyList(),
    val newInstruments: List<NewInstrument>,
    /** Existing instruments that gain an ISIN from the statement. */
    val isinUpdates: Map<Long, String>,
    /** Existing stocks that gain their series (and exchange) from a tradebook. */
    val detailUpdates: Map<Long, Pair<String?, String?>> = emptyMap(),
    /** Rows already present by broker reference. */
    val duplicates: Int,
    /** Rows present without a reference (recorded by hand or imported by 4.x); they receive the reference on commit. */
    val legacyMatches: Map<Long, String>,
    val skipped: List<String>,
    val from: LocalDate?,
    val to: LocalDate?,
) {
    val needsDemat get() = toInsert.any { it.transaction.fromAccountId == DEMAT_PLACEHOLDER || it.transaction.toAccountId == DEMAT_PLACEHOLDER }
    val needsBank get() = toInsert.any { it.transaction.fromAccountId == BANK_PLACEHOLDER || it.transaction.toAccountId == BANK_PLACEHOLDER }

    companion object {
        /** Account ids in a plan made without a chosen account; the plan cannot be committed until they are replaced. */
        const val DEMAT_PLACEHOLDER = -1L
        const val BANK_PLACEHOLDER = -2L
    }
}

data class ImportResult(
    val inserted: Int,
    val instrumentsCreated: Int,
    val referencesStamped: Int,
    val duplicates: Int,
    val held: Int = 0,
) {
    fun summary(type: StatementType) = buildString {
        if (type == StatementType.DIVIDENDS) append("${type.title}: $held awaiting credit")
        else append("${type.title}: $inserted new row${if (inserted == 1) "" else "s"}")
        if (instrumentsCreated > 0) append(", $instrumentsCreated new instrument${if (instrumentsCreated == 1) "" else "s"}")
        if (duplicates > 0) append(", $duplicates already present")
        if (referencesStamped > 0) append(" ($referencesStamped matched rows recorded earlier)")
    }
}

/**
 * Zerodha statements on the new ledger (plan 3.3): stock trades move money between the Demat account and the
 * instrument, fund orders between the bank and the fund, dividends credit the bank tagged with the stock, DP and
 * transaction charges are Trading expenses from the Demat account. Rows carry `broker_ref`, so a statement can be
 * imported twice without doubling anything; rows recorded before references existed are matched by instrument,
 * date, quantity and amount and stamped with the reference.
 */
class ZerodhaImporter(private val db: AppDatabase) {

    /** The Demat and bank accounts the existing ledger already uses for these flows, else the first of each type. */
    suspend fun defaultSettings(): ZerodhaImportSettings {
        val accounts = db.accountDao().getVisible()
        val instruments = db.instrumentDao().getAll().associateBy { it.id }
        val ledger = db.transactionDao().getAllWithInstrument()
        fun mostCommon(ids: List<Long?>): Long? =
            ids.filterNotNull().groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

        val demat =
            mostCommon(ledger.filter { it.kind == TransactionKind.INVEST && instruments[it.instrumentId]?.instrumentType == InstrumentType.STOCK }
                .map { it.fromAccountId })
                ?.takeIf { id -> accounts.any { it.id == id } }
                ?: accounts.firstOrNull { it.accountType == AccountType.DEMAT && it.currency == "INR" }?.id
        val bank =
            mostCommon(ledger.filter { it.kind == TransactionKind.INVEST && instruments[it.instrumentId]?.instrumentType == InstrumentType.MUTUAL_FUND }
                .map { it.fromAccountId })
                ?.takeIf { id -> accounts.any { it.id == id } }
                ?: mostCommon(ledger.filter { it.kind == TransactionKind.INCOME && instruments[it.instrumentId]?.instrumentType == InstrumentType.STOCK }
                    .map { it.toAccountId })
                    ?.takeIf { id -> accounts.any { it.id == id } }
                ?: accounts.firstOrNull { it.accountType == AccountType.BANK }?.id
        return ZerodhaImportSettings(dematAccountId = demat, bankAccountId = bank)
    }

    suspend fun plan(
        statement: ParsedStatement,
        settings: ZerodhaImportSettings,
        now: LocalDateTime = LocalDateTime.now()
    ): ImportPlan {
        val demat = settings.dematAccountId ?: ImportPlan.DEMAT_PLACEHOLDER
        val bank = settings.bankAccountId ?: ImportPlan.BANK_PLACEHOLDER
        val instruments = db.instrumentDao().getListed()
        val byIsin =
            instruments.filter { it.security.isin != null }.associateBy { it.security.isin!! }
        val bySymbol =
            instruments.filter { it.instrument.instrumentType == InstrumentType.STOCK && it.security.symbol != null }
                .associateBy { it.security.symbol!!.uppercase() }
        val byName = instruments.groupBy { it.instrument.instrumentType }
            .mapValues { (_, list) -> list.associateBy { it.name.lowercase() } }
        val trading = categoryId("Trading", CategoryScope.EXPENSE)
        val dividends = categoryId(DIVIDEND_CATEGORY, CategoryScope.INCOME)

        val newInstruments = LinkedHashMap<String, NewInstrument>()
        val isinUpdates = LinkedHashMap<Long, String>()

        /** Existing instrument id, or 0 with a [NewInstrument] queued. */
        fun resolve(
            type: InstrumentType,
            name: String,
            symbol: String?,
            isin: String?
        ): Pair<Long, NewInstrument?> {
            val existing = isin?.let { byIsin[it] }
                ?: symbol?.let { bySymbol[it.uppercase()] }
                ?: byName[type]?.get(name.lowercase())
                ?: symbol?.let { byName[type]?.get(it.lowercase()) }
            if (existing != null) {
                if (existing.security.isin == null && isin != null) isinUpdates[existing.id] = isin
                return existing.id to null
            }
            val key = isin ?: "${type}:${(symbol ?: name).lowercase()}"
            val created = newInstruments.getOrPut(key) { NewInstrument(name, type, symbol, isin) }
            return 0L to created
        }

        /** Series and exchange reach the stock when it is created, or a known stock that lacks them. */
        val detailUpdates = LinkedHashMap<Long, Pair<String?, String?>>()

        val planned = ArrayList<PlannedTransaction>()
        val dpSeen = HashSet<String>()
        val sorted = statement.rows.sortedBy { it.date }
        for (row in sorted) when (row) {
            is TradeRow -> {
                val type = if (row.mutualFund) InstrumentType.MUTUAL_FUND else InstrumentType.STOCK
                var (id, created) = resolve(type, row.name, row.symbol, row.isin)
                if (created != null) {
                    val richer = created.copy(
                        exchange = created.exchange ?: row.exchange,
                        series = created.series ?: row.series,
                        folio = created.folio ?: row.folio,
                    )
                    if (richer != created) {
                        newInstruments.entries.first { it.value == created }
                            .setValue(richer); created = richer
                    }
                } else if (!row.mutualFund && row.series != null) {
                    val known = instruments.first { it.id == id }
                    if (known.security.series == null && row.series != "EQ") detailUpdates[id] =
                        row.series to row.exchange
                }
                val buy = row.side == Side.BUY
                val particulars = when {
                    !row.mutualFund && buy -> "Bought Stocks - ${row.name}"
                    !row.mutualFund -> "Sold Stocks - ${row.name}"
                    buy && row.sip -> "Mutual Funds SIP Installment - ${row.name}"
                    buy -> "Invested In Mutual Funds - ${row.name}"
                    else -> "Redeemed Mutual Funds - ${row.name}"
                }
                val cash = if (row.mutualFund) bank else demat
                planned += PlannedTransaction(
                    TransactionEntity(
                        createdTime = now,
                        modifiedTime = now,
                        date = row.cashDate,
                        kind = if (buy) TransactionKind.INVEST else TransactionKind.REDEEM,
                        fromAccountId = if (buy) cash else null,
                        toAccountId = if (buy) null else cash,
                        instrumentId = id,
                        particulars = particulars,
                        quantity = row.quantity,
                        rate = row.price,
                        amount = row.amount,
                        brokerRef = row.ref,
                    ),
                    created, particulars,
                )
                if (!buy && !row.mutualFund && settings.addDpCharges && dpSeen.add("${row.date}:${row.name}")) {
                    planned += PlannedTransaction(
                        TransactionEntity(
                            createdTime = now,
                            modifiedTime = now,
                            date = row.date,
                            kind = TransactionKind.EXPENSE,
                            fromAccountId = demat,
                            categoryId = trading,
                            instrumentId = id,
                            particulars = "Depository Participant Charges For Selling Stocks - ${row.name}",
                            amount = settings.dpCharge,
                            brokerRef = "kite:dp:${row.date}:${row.name}",
                        ),
                        created, "DP charge · ${row.name}",
                    )
                }
            }

            is DividendRow -> {
                val (id, created) = resolve(InstrumentType.STOCK, row.symbol, row.symbol, row.isin)
                val ex = row.exDate ?: row.date
                planned += PlannedTransaction(
                    TransactionEntity(
                        createdTime = now,
                        modifiedTime = now,
                        date = row.date,
                        kind = TransactionKind.INCOME,
                        toAccountId = bank,
                        categoryId = dividends,
                        instrumentId = id,
                        particulars = "Dividend From Stocks - ${row.symbol}",
                        quantity = row.quantity,
                        rate = row.perShare,
                        amount = row.total,
                        exDate = ex,
                        brokerRef = "div:${row.symbol}:$ex",
                    ),
                    created, "Dividend · ${row.symbol}",
                )
            }

            is ChargeRow -> planned += PlannedTransaction(
                TransactionEntity(
                    createdTime = now,
                    modifiedTime = now,
                    date = row.date,
                    kind = TransactionKind.EXPENSE,
                    fromAccountId = demat,
                    categoryId = trading,
                    particulars = "Zerodha Transaction Charges",
                    amount = row.amount,
                    brokerRef = "charges:${row.date}:${row.amount}",
                ),
                null, "Charges",
            )
        }

        // De-duplicate: by reference first, then against reference-less rows of the same instrument, date, quantity and amount.
        val refs = planned.map { it.transaction.brokerRef!! }
        val existingRefs = refs.filter { db.transactionDao().existsByBrokerRef(it) }.toSet()
        val from = sorted.firstOrNull()?.date
        val to = sorted.lastOrNull()?.date
        // Dividends are recorded on the day the bank credits them, weeks after the ex-date, so the window reaches past the statement.
        val candidates: MutableList<TransactionEntity> =
            if (from == null || to == null) ArrayList()
            else db.transactionDao().getBetween(from.toString(), to.plusDays(90).toString())
                .filter { it.brokerRef == null }.toMutableList()
        val legacy = LinkedHashMap<Long, String>()
        var duplicates = 0
        val toInsert = ArrayList<PlannedTransaction>()
        val seenRefs = HashSet<String>()
        for (p in planned) {
            val t = p.transaction
            val ref = t.brokerRef!!
            if (ref in existingRefs || !seenRefs.add(ref)) {
                duplicates++; continue
            }
            val match = candidates.firstOrNull { e ->
                // A dividend is identified by the stock and its ex-date: the credited amount may be net of tax and dated later.
                if (t.exDate != null) return@firstOrNull e.kind == t.kind && e.exDate == t.exDate && e.instrumentId == t.instrumentId && t.instrumentId != 0L
                e.kind == t.kind && e.date == t.date && closeTo(e.amount, t.amount) &&
                        (t.instrumentId == 0L && p.instrument != null && e.instrumentId == null || e.instrumentId == t.instrumentId) &&
                        (t.quantity == null || e.quantity == null || abs(e.quantity - t.quantity) < 1e-3) &&
                        (t.instrumentId != null || e.particulars == t.particulars)
            }
            if (match != null) {
                candidates.remove(match); legacy[match.id] = ref; duplicates++; continue
            }
            toInsert += p
        }
        // A dividends statement feeds the pending list, not the ledger: the credit date and the net amount come later.
        val toHold = ArrayList<PlannedDividend>()
        val inserts = ArrayList<PlannedTransaction>()
        for (p in toInsert) {
            val t = p.transaction
            if (statement.type != StatementType.DIVIDENDS || t.exDate == null) {
                inserts += p; continue
            }
            if (db.pendingDividendDao().existsByBrokerRef(t.brokerRef!!)) {
                duplicates++; continue
            }
            toHold += PlannedDividend(
                PendingDividend(
                    instrumentId = t.instrumentId ?: 0L,
                    exDate = t.exDate,
                    quantity = t.quantity ?: 0.0,
                    perShare = t.rate ?: 0.0,
                    gross = t.amount,
                    brokerRef = t.brokerRef,
                    importedAt = now,
                ),
                p.instrument, p.label,
            )
        }
        val usedNew =
            (inserts.mapNotNull { it.instrument } + toHold.mapNotNull { it.instrument }).distinct()
        return ImportPlan(
            type = statement.type,
            toInsert = inserts,
            toHold = toHold,
            newInstruments = usedNew,
            isinUpdates = isinUpdates,
            detailUpdates = detailUpdates,
            duplicates = duplicates,
            legacyMatches = legacy,
            skipped = statement.skipped,
            from = from,
            to = to,
        )
    }

    /** Writes the plan in one database transaction: instruments first, then ISINs and references, then the rows with their balance effects. */
    suspend fun commit(plan: ImportPlan, now: LocalDateTime = LocalDateTime.now()): ImportResult {
        require(!plan.needsDemat) { "Choose the Demat account first" }
        require(!plan.needsBank) { "Choose the bank account first" }
        val repository = TransactionRepository(db)
        return db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val dao = db.instrumentDao()
                val holdings = HoldingsRepository(db)
                val ids = HashMap<NewInstrument, Long>()
                // The same path as the + on the Stocks & ETFs and Mutual funds screens: default category, NSE for a stock.
                for (n in plan.newInstruments) {
                    ids[n] = holdings.add(
                        name = n.name,
                        type = n.type,
                        symbol = n.symbol,
                        exchange = n.exchange,
                        isin = n.isin,
                        currency = "INR",
                        categoryId = null,
                        owner = Instrument.OWNER_SELF,
                        notes = null,
                        now = now,
                        series = n.series?.takeIf { it != "EQ" },
                    )
                    n.folio?.let { folio ->
                        dao.getSecurity(ids.getValue(n))
                            ?.let { dao.upsertSecurity(it.copy(folio = folio)) }
                    }
                }
                for ((id, isin) in plan.isinUpdates) dao.getSecurity(id)
                    ?.let { dao.upsertSecurity(it.copy(isin = isin)) }
                for ((id, detail) in plan.detailUpdates) dao.getSecurity(id)?.let {
                    dao.upsertSecurity(
                        it.copy(
                            series = detail.first,
                            exchange = it.exchange ?: detail.second
                        )
                    )
                }
                val held = plan.toHold.map { p ->
                    if (p.instrument != null) p.dividend.copy(instrumentId = ids.getValue(p.instrument)) else p.dividend
                }
                if (held.isNotEmpty()) db.pendingDividendDao().insertAll(held)
                for ((id, ref) in plan.legacyMatches) db.transactionDao().setBrokerRef(id, ref)
                val rows = plan.toInsert.map { p ->
                    val t = p.transaction
                    if (p.instrument != null) t.copy(instrumentId = ids.getValue(p.instrument)) else t
                }
                repository.addAllInTransaction(rows, now)
                ImportResult(
                    inserted = rows.size,
                    instrumentsCreated = ids.size,
                    referencesStamped = plan.legacyMatches.size,
                    duplicates = plan.duplicates,
                    held = held.size,
                )
            }
        }
    }

    private suspend fun categoryId(name: String, scope: CategoryScope): Long {
        val all = db.categoryDao().getAll()
        all.firstOrNull {
            it.name.equals(
                name,
                ignoreCase = true
            ) && !it.deleted && (it.appliesTo == scope || it.appliesTo == CategoryScope.BOTH)
        }?.let { return it.id }
        all.firstOrNull {
            it.name.equals(
                name,
                ignoreCase = true
            ) && (it.appliesTo == scope || it.appliesTo == CategoryScope.BOTH)
        }?.let { return it.id }
        return db.categoryDao().insert(
            Category(
                name = name,
                appliesTo = scope,
                displayOrder = db.categoryDao().maxDisplayOrder() + 1
            )
        )
    }

    private fun closeTo(a: Double, b: Double) = abs(a - b) <= maxOf(0.01, abs(b) * 0.005)

    companion object {
        const val DIVIDEND_CATEGORY = "Interest & Dividends"
    }
}
