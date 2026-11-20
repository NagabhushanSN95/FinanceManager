package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.HoldingEvent
import com.nagabhushansn95.financetracker.data.db.entity.HoldingEventKind
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.Listed
import com.nagabhushansn95.financetracker.data.db.entity.Security
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/**
 * What can happen to a stock, ETF or mutual fund, as the + menu on its screen offers it. Each maps onto one ledger
 * kind: buys and sells carry units and a rate, a dividend is income tagged with the holding, bonus units and splits
 * are income rows of zero amount that carry only units, charges are an expense from the demat account.
 */
enum class HoldingEntryKind(val transactionKind: TransactionKind) {
    BUY(TransactionKind.INVEST),
    SELL(TransactionKind.REDEEM),
    DIVIDEND(TransactionKind.INCOME),
    BONUS(TransactionKind.INCOME),
    SPLIT(TransactionKind.INCOME),
    CHARGES(TransactionKind.EXPENSE);

    /** Funds are bought and redeemed; stocks are bought and sold. */
    fun title(type: InstrumentType): String = when (this) {
        BUY -> if (type == InstrumentType.MUTUAL_FUND) "Purchase" else "Buy"
        SELL -> if (type == InstrumentType.MUTUAL_FUND) "Redeem" else "Sell"
        DIVIDEND -> "Dividend"
        BONUS -> "Bonus shares"
        SPLIT -> "Stock split"
        CHARGES -> "Charges"
    }

    /** The label of the cash account field; null for entries that move no cash. */
    val cashLabel: String?
        get() = when (this) {
            BUY, CHARGES -> "Paid from"
            SELL, DIVIDEND -> "Credited to"
            BONUS, SPLIT -> null
        }

    val movesCash: Boolean get() = cashLabel != null
    val hasUnits: Boolean get() = this == BUY || this == SELL
    val isUnitsOnly: Boolean get() = this == BONUS || this == SPLIT

    /** Cash arriving (sale proceeds, dividends) rather than leaving. */
    val incoming: Boolean get() = this == SELL || this == DIVIDEND
}

/** One line of a holding's ledger as its screen shows it: a cash entry, or units that arrived without cash. */
sealed class HoldingLine {
    abstract val date: LocalDate

    data class Cash(val row: TransactionEntity) : HoldingLine() {
        override val date: LocalDate get() = row.date
    }

    data class Units(val event: HoldingEvent) : HoldingLine() {
        override val date: LocalDate get() = event.date
    }
}

/** One stock or fund with its ledger (oldest first), its unit events, and the position they add up to. */
data class HoldingView(
    val instrument: Instrument,
    val security: Security,
    val rows: List<TransactionEntity>,
    val position: Position,
    /** Bonus and split units, oldest first. */
    val events: List<HoldingEvent> = emptyList(),
) {
    val listed: Listed get() = Listed(instrument, security)
    val type: InstrumentType get() = instrument.instrumentType
    val own: Boolean get() = instrument.owner == Instrument.OWNER_SELF
    val displayName: String get() = listed.displayName
    fun rowsOf(kind: HoldingEntryKind) = rows.filter { HoldingEntries.kindOf(it) == kind }

    /** Rows and unit events together, oldest first (rows before events on the same day). */
    val ledger: List<HoldingLine>
        get() = (rows.map { HoldingLine.Cash(it) } + events.map { HoldingLine.Units(it) }).sortedBy { it.date }

    /** The account the last buy, sell or charge went through (the demat account, usually). */
    val lastTradeAccount: Long?
        get() = rows.lastOrNull {
            val k = HoldingEntries.kindOf(it)
            k == HoldingEntryKind.BUY || k == HoldingEntryKind.SELL || k == HoldingEntryKind.CHARGES
        }?.let { it.fromAccountId ?: it.toAccountId }

    /** The bank the last dividend was credited to. */
    val lastDividendAccount: Long?
        get() = rowsOf(HoldingEntryKind.DIVIDEND).lastOrNull()?.toAccountId
}

object HoldingEntries {
    const val DIVIDEND_CATEGORY = "Interest & Dividends"
    const val CHARGES_CATEGORY = "Trading"
    private val EX_DATE_SUFFIX = Regex("\\s*\\(Ex-Dividend Date: [^)]*\\)")
    private val RATIO = Regex("(\\d+(?:\\.\\d+)?)\\s*:\\s*(\\d+(?:\\.\\d+)?)")

    fun kindsFor(type: InstrumentType): List<HoldingEntryKind> =
        if (type == InstrumentType.MUTUAL_FUND) listOf(
            HoldingEntryKind.BUY,
            HoldingEntryKind.SELL,
            HoldingEntryKind.DIVIDEND,
            HoldingEntryKind.CHARGES
        ) else HoldingEntryKind.entries

    /** Which entry a ledger row is; null for a row the sheet cannot describe (a transfer tagged with the holding). */
    fun kindOf(t: TransactionEntity): HoldingEntryKind? = when (t.kind) {
        TransactionKind.INVEST -> HoldingEntryKind.BUY
        TransactionKind.REDEEM -> HoldingEntryKind.SELL
        TransactionKind.INCOME -> HoldingEntryKind.DIVIDEND
        TransactionKind.EXPENSE -> HoldingEntryKind.CHARGES
        TransactionKind.TRANSFER -> null
    }

    /** Which entry a unit event is; null for a deposit's events, which never sit on a stock. */
    fun kindOf(e: HoldingEvent): HoldingEntryKind? = when (e.kind) {
        HoldingEventKind.BONUS -> HoldingEntryKind.BONUS
        HoldingEventKind.SPLIT -> HoldingEntryKind.SPLIT
        else -> null
    }

    /** The view of every stock and fund in [listed], from the ledger rows and events tagged with them. */
    fun build(
        listed: List<Listed>,
        ledger: List<TransactionEntity>,
        events: List<HoldingEvent> = emptyList(),
    ): List<HoldingView> {
        val byInstrument = ledger.filter { it.instrumentId != null }.groupBy { it.instrumentId!! }
        val eventsById = events.filter { it.kind.addsUnits }.groupBy { it.instrumentId }
        return listed
            .filter { it.instrument.instrumentType == InstrumentType.STOCK || it.instrument.instrumentType == InstrumentType.MUTUAL_FUND }
            .map { l ->
                val rows =
                    byInstrument[l.id].orEmpty().sortedWith(compareBy({ it.date }, { it.id }))
                val ev = eventsById[l.id].orEmpty().sortedWith(compareBy({ it.date }, { it.id }))
                HoldingView(
                    l.instrument,
                    l.security,
                    rows,
                    Holdings.position(l.id, rows, unitBased = true, events = ev),
                    ev
                )
            }
    }

    /** How the particulars name the holding: a stock by its symbol, a fund by its name (the 4.x wording). */
    fun label(listed: Listed): String =
        if (listed.instrument.instrumentType == InstrumentType.STOCK) listed.security.symbol?.takeIf { it.isNotBlank() }
            ?: listed.name
        else listed.name

    /** The house-style particulars for an entry; [ratio] is "a:b" for bonus and split rows. */
    fun defaultParticulars(
        kind: HoldingEntryKind,
        listed: Listed,
        ratio: String? = null
    ): String {
        val name = label(listed)
        val fund = listed.instrument.instrumentType == InstrumentType.MUTUAL_FUND
        return when (kind) {
            HoldingEntryKind.BUY -> if (fund) "Invested In Mutual Funds - $name" else "Bought Stocks - $name"
            HoldingEntryKind.SELL -> if (fund) "Redeemed Mutual Funds - $name" else "Sold Stocks - $name"
            HoldingEntryKind.DIVIDEND -> if (fund) "Dividend From Mutual Funds - $name" else "Dividend From Stocks - $name"
            HoldingEntryKind.BONUS -> "Bonus Shares${ratio?.let { " $it" } ?: ""} - $name"
            HoldingEntryKind.SPLIT -> "${Holdings.SPLIT_PREFIX}${ratio?.let { " $it" } ?: ""} - $name"
            HoldingEntryKind.CHARGES -> if (fund) "Charges - $name" else "Depository Participant Charges For Selling Stocks - $name"
        }
    }

    /**
     * Continues the last entry of the kind (a SIP keeps reading as one), else the default. Bonus and split
     * particulars carry their ratio, so they are always built afresh. An old dividend's ex-date suffix is dropped:
     * the ex-date has a field of its own now.
     */
    fun suggestedParticulars(
        kind: HoldingEntryKind,
        view: HoldingView,
        ratio: String? = null
    ): String {
        if (kind.isUnitsOnly) return defaultParticulars(kind, view.listed, ratio)
        val last = view.rowsOf(kind).lastOrNull()?.particulars?.let(::stripExDate)
        return last?.takeIf { it.isNotBlank() } ?: defaultParticulars(kind, view.listed)
    }

    fun stripExDate(particulars: String): String = particulars.replace(EX_DATE_SUFFIX, "").trim()

    /** The "a:b" in a bonus or split row's particulars, if it has one. */
    fun ratioOf(particulars: String): Pair<String, String>? =
        RATIO.find(particulars)?.let { it.groupValues[1] to it.groupValues[2] }

    /** Bonus a:b (a new for every b held): the extra units. */
    fun bonusUnits(held: Double, a: Double, b: Double): Double = held * a / b

    /** Split old:new (every [old] becomes [new]): the extra units. */
    fun splitUnits(held: Double, old: Double, new: Double): Double = held * new / old - held

    /** The last charge on any holding, the usual DP charge, as the amount a new Charges entry starts with. */
    fun usualCharge(views: List<HoldingView>): Double? =
        views.flatMap { it.rowsOf(HoldingEntryKind.CHARGES) }
            .maxWithOrNull(compareBy({ it.date }, { it.id }))?.amount

    /** The event for bonus shares or a split: the units added and the ratio, on the record date. */
    fun toEvent(
        view: HoldingView,
        kind: HoldingEntryKind,
        date: LocalDate,
        units: Double,
        ratio: String?,
        existing: HoldingEvent? = null,
    ): HoldingEvent {
        require(kind.isUnitsOnly) { "${kind.name} moves cash" }
        return HoldingEvent(
            id = existing?.id ?: 0,
            instrumentId = view.instrument.id,
            date = date,
            kind = if (kind == HoldingEntryKind.SPLIT) HoldingEventKind.SPLIT else HoldingEventKind.BONUS,
            units = units,
            ratio = ratio?.trim()?.ifBlank { null } ?: existing?.ratio,
            notes = existing?.notes,
        )
    }

    /**
     * The ledger row for a cash entry. Buys and sells carry units and the rate and move cash through [accountId]; a
     * dividend lands in [accountId] with the ex-date and its category; charges leave [accountId] under their
     * category. Editing [existing] keeps its identity and broker reference. Bonus and split units are events, see
     * [toEvent].
     */
    fun toTransaction(
        view: HoldingView,
        kind: HoldingEntryKind,
        date: LocalDate,
        units: Double?,
        rate: Double?,
        amount: Double,
        accountId: Long?,
        particulars: String,
        exDate: LocalDate?,
        categoryId: Long?,
        existing: TransactionEntity? = null,
        now: LocalDateTime = LocalDateTime.now(),
        /** The statement reference for a dividend recorded from the pending list. */
        brokerRef: String? = null,
    ): TransactionEntity {
        require(!kind.isUnitsOnly) { "${kind.name} is an event, not a ledger row" }
        val text = particulars.trim().ifBlank { defaultParticulars(kind, view.listed) }
        val out = kind == HoldingEntryKind.BUY || kind == HoldingEntryKind.CHARGES
        val into = kind == HoldingEntryKind.SELL || kind == HoldingEntryKind.DIVIDEND
        return TransactionEntity(
            id = existing?.id ?: 0,
            createdTime = existing?.createdTime ?: now,
            modifiedTime = now,
            date = date,
            kind = kind.transactionKind,
            fromAccountId = accountId.takeIf { out },
            toAccountId = accountId.takeIf { into },
            categoryId = categoryId.takeIf { kind == HoldingEntryKind.DIVIDEND || kind == HoldingEntryKind.CHARGES },
            instrumentId = view.instrument.id,
            particulars = text,
            quantity = units,
            rate = rate,
            amount = amount,
            exDate = exDate.takeIf { kind == HoldingEntryKind.DIVIDEND },
            brokerRef = existing?.brokerRef ?: brokerRef,
            hidden = existing?.hidden ?: false,
            includeInStatistics = existing?.includeInStatistics ?: true,
        )
    }

    /** Null when the entry can be saved, else what is wrong with it. */
    fun problem(
        view: HoldingView,
        kind: HoldingEntryKind,
        units: Double?,
        amount: Double,
        accountId: Long?,
        existing: TransactionEntity?,
    ): String? {
        if (kind.movesCash && accountId == null) return "Choose the account"
        if (kind.hasUnits && (units == null || units <= 0)) return "Units must be more than zero"
        if (kind.isUnitsOnly && (units == null || units <= 0)) return "Units must be more than zero"
        if (!kind.isUnitsOnly && amount <= 0) return "The amount must be more than zero"
        if (kind == HoldingEntryKind.SELL) {
            // The units already sold by the row being edited come back before the check.
            val held =
                view.position.units + (existing?.takeIf { it.kind == TransactionKind.REDEEM }?.quantity
                    ?: 0.0)
            if (units!! > held + Position.UNIT_EPSILON) return "Only ${FdMath.plain(held)} units are held"
        }
        if (kind == HoldingEntryKind.SPLIT && abs(view.position.units) < Position.UNIT_EPSILON && existing == null) return "Nothing is held to split"
        return null
    }
}
