package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.FdEvent
import com.nagabhushansn95.financetracker.data.db.entity.FdEventKind
import com.nagabhushansn95.financetracker.data.db.entity.FdStatus
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentValuation
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate

/**
 * One fixed deposit as the screens show it: the instrument, its terms (may be missing for deposits created from
 * the transaction editor), the deposit row's amount and date, and everything computed from them.
 */
data class FdView(
    val instrument: Instrument,
    val terms: FixedDeposit?,
    val position: Position,
    /** Amount deposited (net of partial withdrawals, as the ledger says). */
    val principal: Double,
    val depositDate: LocalDate?,
    val maturityDate: LocalDate?,
    /** Value today: a manual valuation if one exists, else the formula, else the principal. */
    val estimated: Double,
    val maturityAmount: Double?,
    /** Estimated by [FdTax] from the holder's interest with the bank, not from a stored rate. */
    val tdsAtMaturity: Double,
    val daysLeft: Long?,
    val rows: List<TransactionEntity>,
    /** Interest credited into the deposit and tax deducted from it, as recorded by hand. */
    val events: List<FdEvent> = emptyList(),
) {
    val status: FdStatus
        get() = terms?.status
            ?: if (position.costHeld > Position.UNIT_EPSILON) FdStatus.ACTIVE else FdStatus.CLOSED_AT_MATURITY
    val isOpen: Boolean get() = status == FdStatus.ACTIVE
    val interestSoFar: Double get() = estimated - principal

    /**
     * What a closed deposit earned after tax: the payouts over the principal, plus interest credited to a bank
     * account as its own rows (sweep-in deposits), less TDS rows charged to the deposit.
     */
    val interestEarned: Double get() = position.realised + position.income
    val maturityAfterTds: Double? get() = maturityAmount?.let { it - tdsAtMaturity }
    val bank: String? get() = terms?.bankName
    val rate: Double? get() = terms?.rate
    val matured: Boolean get() = isOpen && maturityDate != null && daysLeft != null && daysLeft <= 0
    val periodLabel: String?
        get() = terms?.let {
            FdMath.periodLabel(
                it.periodMonths,
                it.periodDays
            )
        }
}

/** One line of a deposit's (or a chain's) ledger as the detail screen shows it. */
/** Interest a deposit paid out inside its payout rows, dated on the last payout (see [FixedDeposits.depositInterest]). */
data class DepositInterest(
    val instrumentId: Long,
    val date: LocalDate,
    val amount: Double,
    val toAccountId: Long?
)

sealed class FdLedgerEntry {
    abstract val date: LocalDate
    abstract val deposit: FdView

    /** A plain transaction row; press and hold edits it. */
    data class Row(val row: TransactionEntity, override val deposit: FdView) : FdLedgerEntry() {
        override val date: LocalDate get() = row.date
    }

    /** The two rows of a renewal shown as one: the redemption of [deposit] and the deposit into [into]. */
    data class Renewal(
        val redeem: TransactionEntity,
        val invest: TransactionEntity,
        override val deposit: FdView,
        val into: FdView,
    ) : FdLedgerEntry() {
        override val date: LocalDate get() = redeem.date
        val amount: Double get() = invest.amount
    }

    /** The day a sweep-in deposit was linked to the account (from the deposit's terms, no amount). */
    data class SweepIn(override val date: LocalDate, override val deposit: FdView) : FdLedgerEntry()

    /** A recorded interest credit or TDS deduction; press and hold edits or deletes it. */
    data class Event(val event: FdEvent, override val deposit: FdView) : FdLedgerEntry() {
        override val date: LocalDate get() = event.date
        val tax: Boolean get() = event.kind == FdEventKind.TDS
    }

    /** Derived from the payout, not stored: the gross interest the bank credited, or the tax it deducted. */
    data class Derived(
        override val date: LocalDate,
        override val deposit: FdView,
        val label: String,
        val amount: Double,
        val tax: Boolean,
    ) : FdLedgerEntry()
}

object FixedDeposits {
    /**
     * The ledger of [deposits] (one deposit, or a whole chain), oldest first. A renewal's REDEEM on one deposit and
     * INVEST into the next (same date, same amount, no account on either side) collapse into one [FdLedgerEntry.Renewal].
     * Recorded interest and TDS events appear on their dates. Before the payout of a closed deposit come "Interest
     * credited" (the realised profit plus every TDS, less the interest already recorded) and "Tax deducted" (the TDS
     * reported at closing), so the ledger reads like the bank's statement.
     */
    fun ledger(deposits: List<FdView>): List<FdLedgerEntry> {
        val byId = deposits.associateBy { it.instrument.id }
        val consumed = HashSet<Long>()
        val entries = ArrayList<FdLedgerEntry>()
        for (d in deposits) {
            val next = d.terms?.nextFdId?.let { byId[it] }
            for (row in d.rows) {
                if (row.id in consumed) continue
                val invest =
                    if (row.kind == TransactionKind.REDEEM && row.toAccountId == null && next != null)
                        next.rows.firstOrNull {
                            it.id !in consumed && it.kind == TransactionKind.INVEST && it.fromAccountId == null &&
                                    it.date == row.date && Math.abs(it.amount - row.amount) < 0.005
                        } else null
                if (invest != null) {
                    consumed += invest.id
                    entries += FdLedgerEntry.Renewal(row, invest, d, next!!)
                } else entries += FdLedgerEntry.Row(row, d)
            }
            d.events.forEach { entries += FdLedgerEntry.Event(it, d) }
            d.terms?.sweepInDate?.let { entries += FdLedgerEntry.SweepIn(it, d) }
            if (!d.isOpen) {
                val closedOn =
                    d.rows.filter { it.kind == TransactionKind.REDEEM }.maxOfOrNull { it.date }
                val tdsReported = d.terms?.tdsReported?.takeIf { it > 0 } ?: 0.0
                val tdsRecorded = d.events.filter { it.kind == FdEventKind.TDS }.sumOf { it.amount }
                val interestRecorded =
                    d.events.filter { it.kind == FdEventKind.INTEREST }.sumOf { it.amount }
                val remaining = d.position.realised + tdsReported + tdsRecorded - interestRecorded
                if (closedOn != null) {
                    if (remaining > 0.005) entries += FdLedgerEntry.Derived(
                        closedOn,
                        d,
                        "Interest credited",
                        remaining,
                        tax = false
                    )
                    if (tdsReported > 0) entries += FdLedgerEntry.Derived(
                        closedOn,
                        d,
                        "Tax deducted (TDS)",
                        tdsReported,
                        tax = true
                    )
                }
            }
        }
        // Same day: the deposit row, then derived or recorded interest and tax, then the ledger's own rows in the
        // order they were entered (a sweep-in's interest credit sits next to its liquidation, as on the statement).
        fun rank(e: FdLedgerEntry) = when (e) {
            is FdLedgerEntry.Derived -> if (e.tax) 1 else 0
            is FdLedgerEntry.Event -> if (e.tax) 1 else 0
            is FdLedgerEntry.Row -> if (e.row.kind == TransactionKind.INVEST) -1 else 2
            is FdLedgerEntry.Renewal -> 2
            is FdLedgerEntry.SweepIn -> -2
        }
        return entries.sortedWith(compareBy<FdLedgerEntry> { it.date }.thenBy { rank(it) })
    }

    /**
     * Interest of deposits that was paid out inside the payout rows, for Statistics: per deposit, what came back over
     * what went in, dated on the last payout. Deposits whose interest is already ledger rows of its own (INCOME
     * credits, as sweep-in deposits have) are left out, as are rows kept out of statistics.
     */
    fun depositInterest(
        rows: List<TransactionEntity>,
        instruments: List<Instrument>
    ): List<DepositInterest> {
        val deposits = instruments.filter { it.instrumentType == InstrumentType.FIXED_DEPOSIT }
            .associateBy { it.id }
        return rows.filter { it.includeInStatistics && it.instrumentId in deposits }
            .groupBy { it.instrumentId!! }
            .mapNotNull { (id, own) ->
                if (own.any { it.kind == TransactionKind.INCOME }) return@mapNotNull null
                val payouts = own.filter { it.kind == TransactionKind.REDEEM }
                val last = payouts.maxWithOrNull(compareBy({ it.date }, { it.id }))
                    ?: return@mapNotNull null
                val interest =
                    payouts.sumOf { it.amountInr } - own.filter { it.kind == TransactionKind.INVEST }
                        .sumOf { it.amountInr }
                if (interest > 0.005) DepositInterest(
                    id,
                    last.date,
                    interest,
                    last.toAccountId
                ) else null
            }
    }

    private val NUMBERED = Regex("""FD #(\d+)""")

    /** "FD #n" with the next free number among the existing deposits. */
    fun nextName(existing: List<Instrument>): String {
        val max = existing.mapNotNull { NUMBERED.find(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 0
        return "FD #${max + 1}"
    }

    /**
     * The renewal chain [id] belongs to, oldest first: back along previous_fd_id to the first deposit, then forward
     * along next_fd_id. Links to deposits that no longer exist are skipped; a loop stops where it started.
     */
    fun chain(id: Long, byId: Map<Long, FdView>): List<FdView> {
        val start = byId[id] ?: return emptyList()
        val seen = linkedSetOf(id)
        var first = start
        while (true) {
            val prev = first.terms?.previousFdId?.let { byId[it] } ?: break
            if (!seen.add(prev.instrument.id)) break
            first = prev
        }
        val chain = arrayListOf(first)
        seen.clear(); seen.add(first.instrument.id)
        var cur = first
        while (true) {
            val next = cur.terms?.nextFdId?.let { byId[it] } ?: break
            if (!seen.add(next.instrument.id)) break
            chain += next
            cur = next
        }
        return chain
    }

    /** Builds the views for every FIXED_DEPOSIT instrument, including closed ones (they keep their history). */
    fun build(
        instruments: List<Instrument>,
        terms: List<FixedDeposit>,
        ledgerWithInstrument: List<TransactionEntity>,
        valuations: List<InstrumentValuation>,
        asOf: LocalDate,
        events: List<FdEvent> = emptyList(),
    ): List<FdView> {
        val termsById = terms.associateBy { it.instrumentId }
        val eventsById = events.groupBy { it.instrumentId }
        val rowsById = ledgerWithInstrument.groupBy { it.instrumentId }
        val latest = valuations.groupBy { it.instrumentId }
            .mapValues { (_, v) -> v.maxWith(compareBy({ it.date }, { it.id })) }
        val views = instruments.filter { it.instrumentType == InstrumentType.FIXED_DEPOSIT }
            .map { instrument ->
                val rows =
                    rowsById[instrument.id].orEmpty().sortedWith(compareBy({ it.date }, { it.id }))
                val position = Holdings.position(instrument.id, rows, unitBased = false)
                val fd = termsById[instrument.id]
                val depositDate = rows.firstOrNull { it.kind == TransactionKind.INVEST }?.date
                    ?: position.firstDate
                val principal = position.costHeld
                val maturity = fd?.maturityDate ?: depositDate?.let { d ->
                    fd?.let {
                        FdMath.maturityDate(
                            d,
                            it.periodMonths,
                            it.periodDays
                        )
                    }
                }
                val valuation = latest[instrument.id]
                val formula =
                    if (fd != null && depositDate != null && principal > 0) FdMath.estimate(
                        fd,
                        principal,
                        depositDate,
                        asOf
                    ) else null
                val estimated = when {
                    principal <= Position.UNIT_EPSILON -> 0.0
                    valuation?.totalValue != null && (formula == null || !valuation.date.isBefore(
                        asOf.minusDays(7)
                    )) -> valuation.totalValue

                    formula != null -> formula
                    else -> principal
                }
                val maturityAmount = if (fd != null && depositDate != null) FdMath.maturityAmount(
                    fd,
                    principal.takeIf { it > 0 } ?: position.totalInvested,
                    depositDate) else null
                FdView(
                    instrument = instrument,
                    terms = fd,
                    position = position,
                    principal = principal,
                    depositDate = depositDate,
                    maturityDate = maturity,
                    estimated = estimated,
                    maturityAmount = maturityAmount,
                    tdsAtMaturity = 0.0,
                    daysLeft = FdMath.daysLeft(maturity, asOf),
                    rows = rows,
                    events = eventsById[instrument.id].orEmpty()
                        .sortedWith(compareBy({ it.date }, { it.id })),
                )
            }
        // TDS depends on the holder's other deposits with the bank, so it is estimated once every view exists.
        return views.map { v ->
            if (v.maturityAmount != null) v.copy(
                tdsAtMaturity = FdTax.estimate(
                    v,
                    views
                )
            ) else v
        }
    }
}
