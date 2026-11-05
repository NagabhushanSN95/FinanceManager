package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query

/** One period (month "2026-11" or year "2026") with the four classic Finance Manager counters, in INR. */
data class PeriodTotals(
    val period: String,
    val expense: Double,
    val income: Double,
    val savings: Double,
    val withdrawals: Double,
)

data class CategoryTotal(val categoryId: Long?, val total: Double, val count: Int)

/**
 * Statistics straight from the ledger (plan 3.6: no counters table). Rules from plan 3.3:
 * income = INCOME into cash accounts only (PF contributions never inflate income);
 * expense = EXPENSE from any account;
 * savings = INVEST paid from an account + TRANSFER into a provident fund + INCOME into a provident fund other than interest;
 * withdrawals = REDEEM credited to an account + TRANSFER out of a provident fund + deposit interest credited to an
 * account as its own INCOME row (a sweep-in deposit's), which a regular deposit pays out inside its REDEEM.
 * Amounts are converted with fx_rate; foreign-currency rows without a rate count as zero rather than at face value.
 */
@Dao
interface StatisticsDao {
    @Query(
        """
        SELECT substr(t.date, 1, :periodLength) AS period,
          SUM(CASE WHEN t.kind = 'EXPENSE' THEN $AMOUNT_INR ELSE 0 END) AS expense,
          SUM(CASE WHEN t.kind = 'INCOME' AND ta.account_type IS NOT NULL AND ta.account_type != 'PROVIDENT_FUND' THEN $AMOUNT_INR ELSE 0 END) AS income,
          SUM(CASE WHEN (t.kind = 'INVEST' AND t.from_account_id IS NOT NULL)
                     OR (t.kind = 'TRANSFER' AND ta.account_type = 'PROVIDENT_FUND')
                     OR (t.kind = 'INCOME' AND ta.account_type = 'PROVIDENT_FUND' AND COALESCE(t.contributor, '') != 'Interest') THEN $AMOUNT_INR ELSE 0 END) AS savings,
          SUM(CASE WHEN (t.kind = 'REDEEM' AND t.to_account_id IS NOT NULL)
                     OR (t.kind = 'TRANSFER' AND fa.account_type = 'PROVIDENT_FUND')
                     OR (t.kind = 'INCOME' AND ta.account_type IS NOT NULL AND ta.account_type != 'PROVIDENT_FUND'
                         AND fi.instrument_type = 'FIXED_DEPOSIT') THEN $AMOUNT_INR ELSE 0 END) AS withdrawals
        FROM transactions t
          LEFT JOIN accounts fa ON fa.id = t.from_account_id
          LEFT JOIN accounts ta ON ta.id = t.to_account_id
          LEFT JOIN instruments fi ON fi.id = t.instrument_id
        WHERE t.include_in_statistics = 1 AND t.date BETWEEN :from AND :to
          AND (:filterAccounts = 0 OR t.from_account_id IN (:accountIds) OR t.to_account_id IN (:accountIds))
          AND (:filterCategories = 0 OR t.category_id IN (:categoryIds))
        GROUP BY period ORDER BY period
        """
    )
    suspend fun periodTotals(
        from: String, to: String, periodLength: Int,
        filterAccounts: Boolean, accountIds: List<Long>,
        filterCategories: Boolean, categoryIds: List<Long>,
    ): List<PeriodTotals>

    /** Totals per category for one kind (EXPENSE or INCOME); rows without a category come back with a NULL id. */
    @Query(
        """
        SELECT t.category_id AS categoryId, SUM($AMOUNT_INR) AS total, COUNT(*) AS count
        FROM transactions t LEFT JOIN accounts ta ON ta.id = t.to_account_id
        WHERE t.include_in_statistics = 1 AND t.kind = :kind AND t.date BETWEEN :from AND :to
          AND (t.kind != 'INCOME' OR (ta.account_type IS NOT NULL AND ta.account_type != 'PROVIDENT_FUND'))
          AND (:filterAccounts = 0 OR t.from_account_id IN (:accountIds) OR t.to_account_id IN (:accountIds))
        GROUP BY t.category_id ORDER BY total DESC
        """
    )
    suspend fun categoryTotals(
        kind: String, from: String, to: String,
        filterAccounts: Boolean, accountIds: List<Long>,
    ): List<CategoryTotal>

    @Query("SELECT MIN(date) FROM transactions")
    suspend fun earliestDate(): String?

    companion object {
        const val AMOUNT_INR =
            "(CASE WHEN t.currency = 'INR' THEN t.amount ELSE t.amount * COALESCE(t.fx_rate, 0) END)"
    }
}
