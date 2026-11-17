package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.dao.CategoryTotal
import com.nagabhushansn95.financetracker.data.db.dao.PeriodTotals
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate

/**
 * The Statistics totals: the rupee accounts summed in SQL ([com.nagabhushansn95.financetracker.data.db.dao.StatisticsDao])
 * plus the foreign-currency accounts valued row by row by [ForeignCurrency] and added into the same buckets.
 */
class Statistics(private val db: AppDatabase) {

    private class Foreign(
        val accounts: List<Account>,
        val rows: List<TransactionEntity>,
        val values: Map<Long, Double>
    )

    /** Every row with a foreign side, valued in rupees; empty when no account is in a foreign currency. */
    private suspend fun foreign(): Foreign? {
        val accounts = db.accountDao().getAll()
        if (accounts.none { it.currency != "INR" }) return null
        val currency = accounts.associate { it.id to it.currency }
        val rows = db.transactionDao().getAll().filter {
            (currency[it.fromAccountId] ?: "INR") != "INR" || (currency[it.toAccountId]
                ?: "INR") != "INR"
        }
        return Foreign(
            accounts,
            rows,
            ForeignCurrency.rupeeValues(accounts, rows, db.instrumentDao().getAllFxRates())
        )
    }

    private fun TransactionEntity.inFilter(
        from: LocalDate, to: LocalDate, accountIds: List<Long>, categoryIds: List<Long>,
    ) = includeInStatistics && date >= from && date <= to &&
            (accountIds.isEmpty() || fromAccountId in accountIds || toAccountId in accountIds) &&
            (categoryIds.isEmpty() || categoryId in categoryIds)

    suspend fun periodTotals(
        from: LocalDate,
        to: LocalDate,
        periodLength: Int,
        accountIds: List<Long>,
        categoryIds: List<Long>,
    ): List<PeriodTotals> {
        val sql = db.statisticsDao().periodTotals(
            from.toString(), to.toString(), periodLength,
            accountIds.isNotEmpty(), accountIds, categoryIds.isNotEmpty(), categoryIds,
        )
        val f = foreign() ?: return sql
        val byId = f.accounts.associateBy { it.id }
        fun foreignSide(id: Long?) = id != null && (byId[id]?.currency ?: "INR") != "INR"
        val extra = HashMap<String, DoubleArray>() // expense, income, savings, withdrawals
        for (t in f.rows) {
            if (!t.inFilter(from, to, accountIds, categoryIds)) continue
            val value = f.values[t.id] ?: continue
            val cell = extra.getOrPut(t.date.toString().take(periodLength)) { DoubleArray(4) }
            when (t.kind) {
                TransactionKind.EXPENSE -> if (foreignSide(t.fromAccountId)) cell[0] += value
                TransactionKind.INCOME -> if (foreignSide(t.toAccountId) && byId[t.toAccountId]?.accountType != AccountType.PROVIDENT_FUND) cell[1] += value
                TransactionKind.INVEST -> if (foreignSide(t.fromAccountId)) cell[2] += value
                TransactionKind.REDEEM -> if (foreignSide(t.toAccountId)) cell[3] += value
                TransactionKind.TRANSFER -> Unit
            }
        }
        if (extra.isEmpty()) return sql
        val merged = sql.associateBy { it.period }.toMutableMap()
        for ((period, cell) in extra) {
            val base = merged[period] ?: PeriodTotals(period, 0.0, 0.0, 0.0, 0.0)
            merged[period] = base.copy(
                expense = base.expense + cell[0], income = base.income + cell[1],
                savings = base.savings + cell[2], withdrawals = base.withdrawals + cell[3],
            )
        }
        return merged.values.sortedBy { it.period }
    }

    suspend fun categoryTotals(
        kind: TransactionKind, from: LocalDate, to: LocalDate, accountIds: List<Long>,
    ): List<CategoryTotal> {
        val sql = db.statisticsDao().categoryTotals(
            kind.name,
            from.toString(),
            to.toString(),
            accountIds.isNotEmpty(),
            accountIds
        )
        val f = foreign() ?: return sql
        val byId = f.accounts.associateBy { it.id }
        fun foreignSide(id: Long?) = id != null && (byId[id]?.currency ?: "INR") != "INR"
        val extra = HashMap<Long?, Pair<Double, Int>>()
        for (t in f.rows) {
            if (t.kind != kind || !t.inFilter(from, to, accountIds, emptyList())) continue
            val side = if (kind == TransactionKind.EXPENSE) t.fromAccountId else t.toAccountId
            if (!foreignSide(side)) continue
            val value = f.values[t.id] ?: continue
            val (sum, n) = extra[t.categoryId] ?: (0.0 to 0)
            extra[t.categoryId] = (sum + value) to (n + 1)
        }
        if (extra.isEmpty()) return sql
        val merged = sql.associateBy { it.categoryId }.toMutableMap()
        for ((categoryId, pair) in extra) {
            val base = merged[categoryId] ?: CategoryTotal(categoryId, 0.0, 0)
            merged[categoryId] =
                base.copy(total = base.total + pair.first, count = base.count + pair.second)
        }
        return merged.values.sortedByDescending { it.total }
    }

    suspend fun earliestDate(): LocalDate? =
        db.statisticsDao().earliestDate()?.let { LocalDate.parse(it) }
}
