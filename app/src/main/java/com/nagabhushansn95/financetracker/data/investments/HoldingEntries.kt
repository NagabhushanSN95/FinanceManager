package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
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

/** One stock or fund with its ledger (oldest first) and the position the ledger adds up to. */
data class HoldingView(
    val instrument: Instrument,
    val rows: List<TransactionEntity>,
    val position: Position,
) {
    val type: InstrumentType get() = instrument.instrumentType
    val own: Boolean get() = instrument.owner == Instrument.OWNER_SELF
    fun rowsOf(kind: HoldingEntryKind) = rows.filter { HoldingEntries.kindOf(it) == kind }

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
        TransactionKind.INCOME ->
            if (t.quantity != null && t.amount == 0.0) {
                if (Holdings.isSplit(t.particulars)) HoldingEntryKind.SPLIT else HoldingEntryKind.BONUS
            } else HoldingEntryKind.DIVIDEND

        TransactionKind.EXPENSE -> HoldingEntryKind.CHARGES
        TransactionKind.TRANSFER -> null
    }

    /** The view of every stock and fund in [instruments], from the ledger rows tagged with them. */
    fun build(instruments: List<Instrument>, ledger: List<TransactionEntity>): List<HoldingView> {
        val byInstrument = ledger.filter { it.instrumentId != null }.groupBy { it.instrumentId!! }
        return instruments
            .filter { it.instrumentType == InstrumentType.STOCK || it.instrumentType == InstrumentType.MUTUAL_FUND }
            .map { i ->
                val rows =
                    byInstrument[i.id].orEmpty().sortedWith(compareBy({ it.date }, { it.id }))
                HoldingView(i, rows, Holdings.position(i.id, rows, unitBased = true))
            }
    }

    /** How the particulars name the holding: a stock by its symbol, a fund by its name (the 4.x wording). */
    fun label(instrument: Instrument): String =
        if (instrument.instrumentType == InstrumentType.STOCK) instrument.symbol?.takeIf { it.isNotBlank() }
            ?: instrument.name
        else instrument.name

    /** The house-style particulars for an entry; [ratio] is "a:b" for bonus and split rows. */
    fun defaultParticulars(
        kind: HoldingEntryKind,
        instrument: Instrument,
        ratio: String? = null
    ): String {
        val name = label(instrument)
        val fund = instrument.instrumentType == InstrumentType.MUTUAL_FUND
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
        if (kind.isUnitsOnly) return defaultParticulars(kind, view.instrument, ratio)
        val last = view.rowsOf(kind).lastOrNull()?.particulars?.let(::stripExDate)
        return last?.takeIf { it.isNotBlank() } ?: defaultParticulars(kind, view.instrument)
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

    /**
     * The ledger row for an entry. Buys and sells carry units and the rate and move cash through [accountId]; a
     * dividend lands in [accountId] with the ex-date and its category; bonus and split rows carry only units; charges
     * leave [accountId] under their category. Editing [existing] keeps its identity and broker reference.
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
    ): TransactionEntity {
        val text = particulars.trim().ifBlank {
            defaultParticulars(
                kind,
                view.instrument,
                units?.let { ratioOf(existing?.particulars ?: "") }
                    ?.let { "${it.first}:${it.second}" })
        }
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
            amount = if (kind.isUnitsOnly) 0.0 else amount,
            currency = view.instrument.currency,
            fxRate = existing?.fxRate,
            exDate = exDate.takeIf { kind == HoldingEntryKind.DIVIDEND },
            brokerRef = existing?.brokerRef,
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
