package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow

/** A dated cash flow from the investor's point of view: negative = money out (buy), positive = money back. */
data class CashFlow(val date: LocalDate, val amount: Double)

/**
 * What the ledger says about one instrument, in INR.
 *
 * Unit-based instruments (rows carry quantities) match every sale against the oldest units still held, first in
 * first out, as Zerodha's P&L and the income-tax rules do: a sale realises `amount − cost of those lots`. Bonus units
 * form a lot of their own at zero cost; a split spreads the existing lots over the new unit count. [averageCost] is
 * the average of the lots still held.
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
    /** Units bought together and the cost still attached to them. */
    private class Lot(var units: Double, var cost: Double)

    /** Prefix the bonus-or-split dialog writes for a split (see InstrumentDetailScreen). */
    const val SPLIT_PREFIX = "Stock Split"

    /** Zero-cost units from a split rather than a bonus: the dialog writes "Stock split old:new - name". */
    fun isSplit(particulars: String): Boolean =
        particulars.trimStart().startsWith(SPLIT_PREFIX, ignoreCase = true)

    /**
     * Positions for every instrument that appears in [transactions] (which may be the whole ledger). [unitBased] says
     * which instruments are tracked in units (stocks, funds); every other instrument uses the amount-only method even
     * when its rows carry a quantity, because deposits and policies imported from 4.x all carry a quantity of 1.
     */
    fun positions(
        transactions: List<TransactionEntity>,
        unitBased: (Long) -> Boolean = { true }
    ): Map<Long, Position> =
        transactions.filter { it.instrumentId != null }
            .groupBy { it.instrumentId!! }
            .mapValues { (id, rows) ->
                position(
                    id,
                    rows.sortedWith(compareBy({ it.date }, { it.id })),
                    unitBased(id)
                )
            }

    fun position(
        instrumentId: Long,
        rows: List<TransactionEntity>,
        unitBased: Boolean = true
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

        for (t in rows) {
            val amount = t.amountInr
            when (t.kind) {
                TransactionKind.INVEST -> {
                    invested += amount
                    cost += amount
                    if (hasUnits) {
                        val q = t.quantity ?: 0.0
                        units += q
                        // A purchase recorded without units keeps its cost in the holding; nothing can sell it.
                        if (q > Position.UNIT_EPSILON) lots.addLast(Lot(q, amount))
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
                            val lot = lots.first()
                            val take = minOf(lot.units, need)
                            val share = lot.cost * take / lot.units
                            costOut += share
                            lot.units -= take
                            lot.cost -= share
                            need -= take
                            if (lot.units <= Position.UNIT_EPSILON) lots.removeFirst()
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
                    // Bonus units carry a quantity and no cash; dividends and interest carry cash.
                    if (hasUnits && t.quantity != null && amount == 0.0) {
                        if (isSplit(t.particulars) && units > Position.UNIT_EPSILON) {
                            // A split: the same cost now covers more units in every open lot.
                            val factor = (units + t.quantity) / units
                            lots.forEach { it.units *= factor }
                        } else lots.addLast(Lot(t.quantity, 0.0))
                        units += t.quantity
                    } else {
                        income += amount
                        if (amount != 0.0) flows += CashFlow(t.date, amount)
                    }
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
        )
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
