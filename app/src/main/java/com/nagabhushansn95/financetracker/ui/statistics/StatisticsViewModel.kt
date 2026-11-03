package com.nagabhushansn95.financetracker.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.dao.PeriodTotals
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.ui.common.LedgerFilter
import com.nagabhushansn95.financetracker.ui.common.Period
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class Grouping(val label: String, val periodLength: Int) {
    MONTHS(
        "By month",
        7
    ),
    YEARS("By year", 4)
}

/** One line of the category breakdown: a parent with its total (children rolled up) and the children beneath. */
data class CategoryLine(
    val name: String,
    val total: Double,
    val count: Int,
    val share: Double,
    val children: List<CategoryLine> = emptyList()
)

data class StatisticsUiState(
    val filter: LedgerFilter = LedgerFilter(period = Period.THIS_YEAR),
    val grouping: Grouping = Grouping.MONTHS,
    val rows: List<PeriodTotals> = emptyList(),
    val total: PeriodTotals = PeriodTotals("Total", 0.0, 0.0, 0.0, 0.0),
    val expenseByCategory: List<CategoryLine> = emptyList(),
    val incomeByCategory: List<CategoryLine> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val loaded: Boolean = false,
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val _state = MutableStateFlow(StatisticsUiState())
    val state: StateFlow<StatisticsUiState> = _state

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                accounts = db.accountDao().getVisible(),
                categories = db.categoryDao().getVisible()
            )
            // Recompute whenever the ledger changes.
            db.transactionDao().observeAll().collect { recompute() }
        }
    }

    fun setFilter(filter: LedgerFilter) {
        _state.value = _state.value.copy(filter = filter); viewModelScope.launch { recompute() }
    }

    fun setGrouping(grouping: Grouping) {
        _state.value = _state.value.copy(grouping = grouping); viewModelScope.launch { recompute() }
    }

    private suspend fun recompute() {
        val s = _state.value
        val earliest = db.statisticsDao().earliestDate()?.let { LocalDate.parse(it) }
        val (from, to) = s.filter.range(earliest = earliest)
        val dao = db.statisticsDao()
        val accountIds = s.filter.accountIds.toList()
        val categoryIds = s.filter.categoryIds.toList()
        val rows = dao.periodTotals(
            from.toString(),
            to.toString(),
            s.grouping.periodLength,
            accountIds.isNotEmpty(),
            accountIds,
            categoryIds.isNotEmpty(),
            categoryIds
        )
        val total = PeriodTotals(
            "Total",
            rows.sumOf { it.expense },
            rows.sumOf { it.income },
            rows.sumOf { it.savings },
            rows.sumOf { it.withdrawals })
        val categories = db.categoryDao().getAll().associateBy { it.id }
        suspend fun breakdown(kind: String): List<CategoryLine> {
            val totals = if (rows.isEmpty()) emptyList() else dao.categoryTotals(
                kind,
                from.toString(),
                to.toString(),
                accountIds.isNotEmpty(),
                accountIds
            )
            val filtered =
                if (categoryIds.isEmpty()) totals else totals.filter { it.categoryId in categoryIds }
            val grand = filtered.sumOf { it.total }.takeIf { it > 0 } ?: 1.0
            // Roll children up into their parent; uncategorised rows become their own line.
            val byParent =
                filtered.groupBy { ct -> ct.categoryId?.let { categories[it]?.parentId ?: it } }
            return byParent.map { (parentId, members) ->
                val parent = parentId?.let { categories[it] }
                val sum = members.sumOf { it.total }
                val children = members.filter { it.categoryId != parentId }.map { ct ->
                    CategoryLine(ct.categoryId?.let { categories[it]?.name } ?: "Uncategorised",
                        ct.total,
                        ct.count,
                        ct.total / grand)
                }.sortedByDescending { it.total }
                CategoryLine(
                    parent?.name ?: "Uncategorised",
                    sum,
                    members.sumOf { it.count },
                    sum / grand,
                    children
                )
            }.sortedByDescending { it.total }
        }
        _state.value = s.copy(
            rows = rows, total = total,
            expenseByCategory = breakdown("EXPENSE"), incomeByCategory = breakdown("INCOME"),
            loaded = true,
        )
    }
}
