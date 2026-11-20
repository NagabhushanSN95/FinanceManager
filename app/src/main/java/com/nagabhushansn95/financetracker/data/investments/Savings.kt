package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind

/** One column of the sheet's Savings tables for one period: money put in, taken out, and the difference. */
data class SavingsLine(val name: String, val credits: Double, val debits: Double) {
    val net: Double get() = credits - debits
}

/** One row of the Savings Monthly ("2026-07") or Savings Yearly ("2026") table. */
data class SavingsPeriod(val key: String, val lines: List<SavingsLine>) {
    val total: Double get() = lines.sumOf { it.net }
    val credits: Double get() = lines.sumOf { it.credits }
    val debits: Double get() = lines.sumOf { it.debits }
}

/**
 * The sheet's Savings Monthly / Yearly tables from the ledger: every INVEST row is a credit to its instrument's
 * top-level category and every REDEEM row a debit (renewals count on both sides, as in the sheet); provident-fund
 * contributions and transfers in are credits to "Provident Funds", transfers out are debits. Interest credited to a
 * provident fund is growth, not saving, and is left out (as in the sheet). Family-owned instruments are left out.
 * Rows on a foreign-currency account count at the rupee cost of the money they moved ([ForeignCurrency]).
 */
object Savings {
    /** Contributor tag of provident-fund interest rows (see the PF account screen). */
    const val PF_INTEREST = "Interest"

    fun build(
        transactions: List<TransactionEntity>,
        instruments: List<Instrument>,
        categories: List<InvestmentCategory>,
        accounts: List<Account>,
        monthly: Boolean,
    ): List<SavingsPeriod> {
        val instrumentById = instruments.associateBy { it.id }
        val categoryById = categories.associateBy { it.id }
        val pfIds =
            accounts.filter { it.accountType == AccountType.PROVIDENT_FUND }.map { it.id }.toSet()
        val keyLength = if (monthly) 7 else 4
        // Rows on a foreign-currency account count at the rupee cost of the money they moved.
        val rupees = ForeignCurrency.rupeeValues(accounts, transactions)
        fun TransactionEntity.inr() = rupees[id] ?: amount
        val order = LinkedHashMap<String, Int>()
        categories.filter { it.parentId == null }.forEachIndexed { i, c -> order[c.name] = i }
        fun columnOf(instrument: Instrument): String {
            val c = instrument.categoryId?.let { categoryById[it] }
            val top = c?.let { it.parentId?.let { p -> categoryById[p] } ?: it }
            return top?.name ?: InvestmentCategories.defaultCategoryName(instrument.instrumentType)
        }

        val credits = HashMap<Pair<String, String>, Double>()
        val debits = HashMap<Pair<String, String>, Double>()
        for (t in transactions) {
            val key = t.date.toString().take(keyLength)
            when (t.kind) {
                TransactionKind.INVEST, TransactionKind.REDEEM -> {
                    val instrument = t.instrumentId?.let { instrumentById[it] } ?: continue
                    if (instrument.owner != Instrument.OWNER_SELF) continue
                    val cell = key to columnOf(instrument)
                    if (t.kind == TransactionKind.INVEST) credits.merge(
                        cell,
                        t.inr(),
                        Double::plus
                    )
                    else debits.merge(cell, t.amount, Double::plus)
                }

                TransactionKind.INCOME -> {
                    if (t.toAccountId in pfIds && t.contributor != PF_INTEREST) credits.merge(
                        key to InvestmentCategories.PROVIDENT_FUNDS,
                        t.inr(),
                        Double::plus
                    )
                    // Deposit interest credited to a cash account as its own row (sweep-in deposits) is money
                    // leaving savings, like the interest inside a regular deposit's payout.
                    val deposit = t.instrumentId?.let { instrumentById[it] }
                        ?.takeIf { it.instrumentType == InstrumentType.FIXED_DEPOSIT && it.owner == Instrument.OWNER_SELF }
                    if (deposit != null && t.toAccountId != null && t.toAccountId !in pfIds)
                        debits.merge(key to columnOf(deposit), t.amount, Double::plus)
                }

                TransactionKind.TRANSFER -> {
                    if (t.toAccountId in pfIds) credits.merge(
                        key to InvestmentCategories.PROVIDENT_FUNDS,
                        t.inr(),
                        Double::plus
                    )
                    if (t.fromAccountId in pfIds) debits.merge(
                        key to InvestmentCategories.PROVIDENT_FUNDS,
                        t.inr(),
                        Double::plus
                    )
                }

                TransactionKind.EXPENSE -> {
                    // TDS charged to a deposit from a cash account: a regular payout is net of it, so it comes off
                    // the debit here too.
                    val deposit = t.instrumentId?.let { instrumentById[it] }
                        ?.takeIf { it.instrumentType == InstrumentType.FIXED_DEPOSIT && it.owner == Instrument.OWNER_SELF }
                    if (deposit != null && t.fromAccountId != null)
                        debits.merge(key to columnOf(deposit), -t.inr(), Double::plus)
                }
            }
        }
        // Plain list sorting: SortedSet.reversed() resolves to the Java 21 method, which Android lacks.
        val keys = (credits.keys + debits.keys).map { it.first }.distinct().sortedDescending()
        return keys.map { key ->
            val names =
                (credits.keys + debits.keys).filter { it.first == key }.map { it.second }.distinct()
                    .sortedWith(compareBy({ order[it] ?: Int.MAX_VALUE }, { it }))
            SavingsPeriod(
                key,
                names.map { SavingsLine(it, credits[key to it] ?: 0.0, debits[key to it] ?: 0.0) })
        }
    }
}
