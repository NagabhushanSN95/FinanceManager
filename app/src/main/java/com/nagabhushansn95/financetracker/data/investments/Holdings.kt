package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.HoldingEvent
import com.nagabhushansn95.financetracker.data.db.entity.HoldingEventKind
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow

/** A dated cash flow from the investor's point of view: negative = money out (buy), positive = money back. */
data class CashFlow(val date: LocalDate, val amount: Double)

/**
 * What the ledger says about one instrument, in the instrument's currency.
 *
 * Unit-based instruments (rows carry quantities) match every sale against the oldest units still held, first in
 * first out, as Zerodha's P&L and the income-tax rules do: a sale realises `amount − cost of those lots`. Lots are
 * kept per cash account (the demat account a purchase was paid from), and a sale takes the oldest lots of the
 * account its proceeds land in before any other, since capital gains are taxed per account holder; a holding bought
 * through one account and sold through another still matches. Bonus units form a lot of their own at zero cost,
 * spread over the accounts in proportion to what each holds; a split spreads the existing lots over the new unit
 * count; both come from the holding's events, never from the ledger. [averageCost] is the average of the lots still
 * held.
 * Amount-only instruments (deposits, policies, SIPs recorded without units) treat redemptions as returning cost
 * first; anything beyond the cost ever put in is realised profit.
 */
data class Position(
    val instrumentId: Long,
    val units: Double,
    /** Cost basis of what is still held. */
    val costHeld: Double,
    /** Everything ever paid in. */
    val totalInvested: Double,
    /** Everything ever taken out (sales, maturities). */
    val totalRedeemed: Double,
    val realised: Double,
    /** Dividends, interest and other INCOME rows tagged with the instrument. */
    val income: Double,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
    val hasUnits: Boolean,
    val cashFlows: List<CashFlow>,
    /** Units still held through each cash account (null: bought without an account), when more than one is involved. */
    val unitsByAccount: Map<Long?, Double> = emptyMap(),
) {
    val averageCost: Double? get() = if (hasUnits && units > UNIT_EPSILON) costHeld / units else null

    /** Market value from a unit price, or the given total; falls back to cost when nothing is known. */
    fun currentValue(unitPrice: Double?, totalValue: Double?): Double = when {
        totalValue != null -> totalValue
        unitPrice != null && hasUnits -> units * unitPrice
        else -> costHeld
    }

    companion object {
        const val UNIT_EPSILON = 0.0005
    }
}

object Holdings {
    /** Units bought together, the cost still attached to them, and the cash account they were bought through. */
    private class Lot(var units: Double, var cost: Double, val account: Long?)

    /** Prefix the bonus-or-split dialog writes for a split (see InstrumentDetailScreen). */
    const val SPLIT_PREFIX = "Stock Split"

    /** Zero-cost units from a split rather than a bonus: the dialog writes "Stock split old:new - name". */
    fun isSplit(particulars: String): Boolean =
        particulars.trimStart().startsWith(SPLIT_PREFIX, ignoreCase = true)

    /**
     * Positions for every instrument that appears in [transactions] (which may be the whole ledger). [unitBased] says
     * which instruments are tracked in units (stocks, funds); every other instrument uses the amount-only method even
     * when its rows carry a quantity, because deposits and policies imported from 4.x all carry a quantity of 1.
     * [events] carry the bonus and split units.
     */
    fun positions(
        transactions: List<TransactionEntity>,
        unitBased: (Long) -> Boolean = { true },
        events: List<HoldingEvent> = emptyList(),
    ): Map<Long, Position> {
        val eventsById = events.groupBy { it.instrumentId }
        return transactions.filter { it.instrumentId != null }
            .groupBy { it.instrumentId!! }
            .mapValues { (id, rows) ->
                position(
                    id,
                    rows.sortedWith(compareBy({ it.date }, { it.id })),
                    unitBased(id),
                    eventsById[id].orEmpty(),
                )
            }
    }

    /** One step of a holding's history: a ledger row, or an event that added units. */
    private sealed class Step(val date: LocalDate) {
        class Cash(val row: TransactionEntity) : Step(row.date)
        class Units(val event: HoldingEvent) : Step(event.date)
    }

    fun position(
        instrumentId: Long,
        rows: List<TransactionEntity>,
        unitBased: Boolean = true,
        events: List<HoldingEvent> = emptyList(),
    ): Position {
        var units = 0.0
        var cost = 0.0
        var invested = 0.0
        var redeemed = 0.0
        var realised = 0.0
        var income = 0.0
        val flows = ArrayList<CashFlow>()
        val hasUnits =
            unitBased && rows.any { it.quantity != null && (it.kind == TransactionKind.INVEST || it.kind == TransactionKind.REDEEM) }
        // Open lots, oldest first; a sale consumes them in that order (FIFO).
        val lots = ArrayDeque<Lot>()
        // Rows and unit events in date order; on one day the rows come first, then the events in their order.
        val steps = (rows.map { Step.Cash(it) } + events.filter { it.kind.addsUnits }
            .sortedWith(compareBy({ it.date }, { it.id })).map { Step.Units(it) })
            .sortedBy { it.date }

        for (step in steps) {
            if (step is Step.Units) {
                val e = step.event
                val q = e.units ?: 0.0
                if (!hasUnits || q <= Position.UNIT_EPSILON) continue
                if (e.kind == HoldingEventKind.SPLIT && units > Position.UNIT_EPSILON) {
                    // A split: the same cost now covers more units in every open lot.
                    val factor = (units + q) / units
                    lots.forEach { it.units *= factor }
                } else {
                    // Bonus units land in each account in proportion to what it holds.
                    val held = lots.sumOf { it.units }
                    val byAccount =
                        lots.groupBy { it.account }.mapValues { (_, l) -> l.sumOf { it.units } }
                    if (held > Position.UNIT_EPSILON) byAccount.forEach { (account, u) ->
                        lots.addLast(Lot(q * u / held, 0.0, account))
                    } else lots.addLast(Lot(q, 0.0, null))
                }
                units += q
                continue
            }
            val t = (step as Step.Cash).row
            val amount = t.amount
            when (t.kind) {
                TransactionKind.INVEST -> {
                    invested += amount
                    cost += amount
                    if (hasUnits) {
                        val q = t.quantity ?: 0.0
                        units += q
                        // A purchase recorded without units keeps its cost in the holding; nothing can sell it.
                        if (q > Position.UNIT_EPSILON) lots.addLast(Lot(q, amount, t.fromAccountId))
                    }
                    if (amount != 0.0) flows += CashFlow(t.date, -amount)
                }

                TransactionKind.REDEEM -> {
                    redeemed += amount
                    if (hasUnits) {
                        val q = t.quantity ?: 0.0
                        var need = q
                        var costOut = 0.0
                        while (need > Position.UNIT_EPSILON && lots.isNotEmpty()) {
                            // The oldest lot of the account the money comes back to, else the oldest of any.
                            val lot =
                                lots.firstOrNull { it.account == t.toAccountId } ?: lots.first()
                            val take = minOf(lot.units, need)
                            val share = lot.cost * take / lot.units
                            costOut += share
                            lot.units -= take
                            lot.cost -= share
                            need -= take
                            if (lot.units <= Position.UNIT_EPSILON) lots.remove(lot)
                        }
                        costOut = costOut.coerceAtMost(cost)
                        realised += amount - costOut
                        cost -= costOut
                        units -= q
                    } else {
                        val costOut = amount.coerceAtMost(cost)
                        realised += amount - costOut
                        cost -= costOut
                    }
                    if (amount != 0.0) flows += CashFlow(t.date, amount)
                }

                TransactionKind.INCOME -> {
                    // Dividends and interest; bonus and split units are events, not rows.
                    income += amount
                    if (amount != 0.0) flows += CashFlow(t.date, amount)
                }

                TransactionKind.EXPENSE -> {
                    // Charges attributed to the holding reduce the realised result.
                    realised -= amount
                    if (amount != 0.0) flows += CashFlow(t.date, -amount)
                }

                TransactionKind.TRANSFER -> Unit
            }
        }
        if (abs(units) < Position.UNIT_EPSILON) units = 0.0
        if (abs(cost) < 0.005) cost = 0.0
        return Position(
            instrumentId = instrumentId,
            units = units,
            costHeld = cost,
            totalInvested = invested,
            totalRedeemed = redeemed,
            realised = realised,
            income = income,
            firstDate = rows.firstOrNull()?.date,
            lastDate = rows.lastOrNull()?.date,
            hasUnits = hasUnits,
            cashFlows = flows,
            unitsByAccount = lots.groupBy { it.account }
                .mapValues { (_, l) -> l.sumOf { it.units } }
                .filterValues { it > Position.UNIT_EPSILON },
        )
    }

    /**
     * What a sale of [units] for [amount] into [toAccountId] would realise on top of what [rows] already have: the
     * amount less the FIFO cost of the lots it would consume. For the Sell sheet's preview.
     */
    fun realisedBySelling(
        instrumentId: Long,
        rows: List<TransactionEntity>,
        units: Double,
        amount: Double,
        date: LocalDate,
        toAccountId: Long?,
        excludingId: Long? = null,
        events: List<HoldingEvent> = emptyList(),
    ): Double {
        val base = rows.filter { it.id != excludingId }
        val sale = TransactionEntity(
            id = Long.MAX_VALUE,
            createdTime = java.time.LocalDateTime.MIN,
            modifiedTime = java.time.LocalDateTime.MIN,
            date = date,
            kind = TransactionKind.REDEEM,
            toAccountId = toAccountId,
            instrumentId = instrumentId,
            particulars = "",
            quantity = units,
            amount = amount,
        )
        val ordered = (base + sale).sortedWith(compareBy({ it.date }, { it.id }))
        return position(instrumentId, ordered, events = events).realised - position(
            instrumentId,
            base,
            events = events
        ).realised
    }

    /**
     * Annualised internal rate of return of [flows] plus a final inflow of [currentValue] on [asOf].
     * Null when there is nothing to solve (no outflow, or no inflow and no value) or the solver does not converge.
     */
    fun xirr(flows: List<CashFlow>, currentValue: Double, asOf: LocalDate): Double? {
        val all = flows + CashFlow(asOf, currentValue)
        if (all.none { it.amount < 0 } || all.none { it.amount > 0 }) return null
        val t0 = all.minOf { it.date }
        val years = all.map { ChronoUnit.DAYS.between(t0, it.date) / 365.0 }
        fun npv(r: Double) = all.indices.sumOf { all[it].amount / (1 + r).pow(years[it]) }
        // Bisection on a wide bracket: robust for the noisy flows a personal ledger produces.
        var lo = -0.9999
        var hi = 10.0
        var fLo = npv(lo)
        if (fLo * npv(hi) > 0) return null
        repeat(200) {
            val mid = (lo + hi) / 2
            val fMid = npv(mid)
            if (abs(fMid) < 1e-7 || hi - lo < 1e-9) return mid
            if (fLo * fMid < 0) hi = mid else {
                lo = mid; fLo = fMid
            }
        }
        return (lo + hi) / 2
    }
}
