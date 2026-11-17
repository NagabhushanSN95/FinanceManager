package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.FxRate
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate

/**
 * What a row on a foreign-currency account is worth in rupees, at the cost of the money it moved. Each account keeps
 * a pool: the foreign units held and the rupees they cost. A load (a transfer from a rupee account, with the rupees
 * on the from side and the foreign amount on the to side) adds to the pool at the rate it was bought; a spend takes
 * its share at the pool's running average; a credit with no load behind it (cash back, money paid back) joins at the
 * row's own rate, or the day's market rate, or the average. A row nothing can price counts as zero rather than at
 * face value (a dollar is not a rupee). Nothing is stored: the values are computed from the account's whole history
 * each time statistics are drawn.
 */
object ForeignCurrency {
    private const val EPSILON = 1e-6

    /** Rupees per unit of [currency] on [date], from the nearest rate on or before it, else the nearest after. */
    fun rateOn(rates: List<FxRate>, currency: String, date: LocalDate): Double? {
        val ofCurrency = rates.filter { it.base == currency && it.quote == "INR" }
        return ofCurrency.filter { !it.date.isAfter(date) }.maxByOrNull { it.date }?.rate
            ?: ofCurrency.filter { it.date.isAfter(date) }.minByOrNull { it.date }?.rate
    }

    private class Pool(var units: Double = 0.0, var cost: Double = 0.0) {
        val average: Double? get() = if (units > EPSILON) cost / units else null
        fun add(u: Double, rupees: Double) {
            units += u; cost += rupees
        }

        /** Takes [u] units out at the average; the rupees they cost. */
        fun take(u: Double): Double {
            val avg = average ?: return 0.0
            val rupees = minOf(u, units) * avg
            units -= u; cost -= rupees
            if (units < EPSILON) {
                units = 0.0; cost = 0.0
            }
            return rupees
        }
    }

    /**
     * The rupee value of every row that touches a foreign-currency account, by row id. [rows] may be the whole
     * ledger; only rows with a foreign side are valued. [rates] fill in for credits the rows cannot price themselves.
     */
    fun rupeeValues(
        accounts: List<Account>,
        rows: List<TransactionEntity>,
        rates: List<FxRate> = emptyList(),
    ): Map<Long, Double> {
        val currency = accounts.associate { it.id to it.currency }
        fun foreign(id: Long?) = id != null && (currency[id] ?: "INR") != "INR"
        val pools = HashMap<Long, Pool>()
        fun pool(id: Long) = pools.getOrPut(id) { Pool() }
        val values = HashMap<Long, Double>()
        for (t in rows.sortedWith(compareBy({ it.date }, { it.id }))) {
            val from = t.fromAccountId?.takeIf { foreign(it) }
            val to = t.toAccountId?.takeIf { foreign(it) }
            if (from == null && to == null) continue
            when {
                // A load: rupees out of a rupee account, foreign money into the card.
                from == null && to != null && t.kind == TransactionKind.TRANSFER && t.fromAccountId != null -> {
                    val units = t.arriving
                    val rupees = t.amount
                    pool(to).add(units, rupees)
                    values[t.id] = rupees
                }
                // Money coming back: foreign out of the card, rupees into a rupee account.
                from != null && to == null && t.kind == TransactionKind.TRANSFER && t.toAccountId != null -> {
                    pool(from).take(t.amount)
                    values[t.id] = t.arriving
                }
                // Between two foreign accounts: the rupees leave one pool and enter the other.
                from != null && to != null -> {
                    val rupees = pool(from).take(t.amount).takeIf { it > 0 }
                        ?: t.fxRate?.let { it * t.amount }
                        ?: rateOn(rates, currency.getValue(from), t.date)?.let { it * t.amount }
                        ?: 0.0
                    pool(to).add(t.arriving, rupees)
                    values[t.id] = rupees
                }
                // A credit into a foreign account (cash back, money paid back): its own rate, the day's, or the average.
                to != null -> {
                    val rate =
                        t.fxRate ?: rateOn(rates, currency.getValue(to), t.date) ?: pool(to).average
                        ?: 0.0
                    val rupees = t.arriving * rate
                    pool(to).add(t.arriving, rupees)
                    values[t.id] = rupees
                }
                // A spend (or an investment paid) from a foreign account: the average cost, unless the row pins a rate.
                else -> {
                    val p = pool(from!!)
                    val pinned = t.fxRate
                    val rupees = when {
                        pinned != null -> t.amount * pinned
                        p.average != null -> t.amount * p.average!!
                        else -> t.amount * (rateOn(rates, currency.getValue(from), t.date) ?: 0.0)
                    }
                    p.take(t.amount)
                    values[t.id] = rupees
                }
            }
        }
        return values
    }
}
