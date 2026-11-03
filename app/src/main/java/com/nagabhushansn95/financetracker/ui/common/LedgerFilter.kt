package com.nagabhushansn95.financetracker.ui.common

import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate
import java.time.YearMonth

enum class Period(val label: String) {
    THIS_MONTH("This month"), LAST_MONTH("Last month"), THIS_YEAR(
        "This year"
    ),
    ALL("All time"), CUSTOM("Custom")
}

/** What the ledger list and Statistics are narrowed to. Shared between both screens. */
data class LedgerFilter(
    val period: Period = Period.THIS_MONTH,
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,
    val kinds: Set<TransactionKind> = emptySet(),        // empty = all
    val accountIds: Set<Long> = emptySet(),               // empty = all
    val categoryIds: Set<Long> = emptySet(),              // empty = all
    val query: String = "",
) {
    fun range(
        today: LocalDate = LocalDate.now(),
        earliest: LocalDate? = null
    ): Pair<LocalDate, LocalDate> = when (period) {
        Period.THIS_MONTH -> YearMonth.from(today).let { it.atDay(1) to it.atEndOfMonth() }
        Period.LAST_MONTH -> YearMonth.from(today).minusMonths(1)
            .let { it.atDay(1) to it.atEndOfMonth() }

        Period.THIS_YEAR -> today.withDayOfYear(1) to today.withMonth(12).withDayOfMonth(31)
        Period.ALL -> (earliest ?: LocalDate.of(2000, 1, 1)) to today.plusYears(10)
        Period.CUSTOM -> (customFrom ?: today.withDayOfMonth(1)) to (customTo ?: today)
    }

    val activeCount: Int
        get() = listOf(
            kinds.isNotEmpty(),
            accountIds.isNotEmpty(),
            categoryIds.isNotEmpty(),
            query.isNotBlank()
        ).count { it }

    fun matches(t: TransactionEntity, range: Pair<LocalDate, LocalDate>): Boolean =
        t.date >= range.first && t.date <= range.second &&
                (kinds.isEmpty() || t.kind in kinds) &&
                (accountIds.isEmpty() || t.fromAccountId in accountIds || t.toAccountId in accountIds) &&
                (categoryIds.isEmpty() || t.categoryId in categoryIds) &&
                (query.isBlank() || t.particulars.contains(query.trim(), ignoreCase = true))

    fun rangeLabel(today: LocalDate = LocalDate.now()): String = when (period) {
        Period.CUSTOM -> range(today).let { (a, b) -> "${a.format(DATE)} – ${b.format(DATE)}" }
        else -> period.label
    }

    companion object {
        private val DATE = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")
    }
}
