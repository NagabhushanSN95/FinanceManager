package com.nagabhushansn95.financetracker.ui.statistics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.dao.PeriodTotals
import com.nagabhushansn95.financetracker.ui.common.FilterSheet
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.PeriodMenu
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MONTH = DateTimeFormatter.ofPattern("MMM yyyy")

/** Per-period Expenses / Income / Savings / Withdrawals plus category breakdowns, all computed from the ledger. */
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodMenu(state.filter, onChange = viewModel::setFilter)
            Grouping.entries.forEach { g ->
                FilterChip(
                    selected = state.grouping == g,
                    onClick = { viewModel.setGrouping(g) },
                    label = { Text(g.label) })
            }
            Spacer(Modifier.weight(1f))
            BadgedBox(badge = { if (state.filter.activeCount > 0) Badge { Text("${state.filter.activeCount}") } }) {
                TextButton(onClick = { showFilters = true }) { Text("Filters") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)) {
                TotalsRow("Period", "Expenses", "Income", "Savings", "Withdrawals", header = true)
                HorizontalDivider()
                if (state.loaded && state.rows.isEmpty()) {
                    Text(
                        "Nothing in this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                state.rows.forEach { r -> TotalsRow(periodLabel(r.period), r) }
                if (state.rows.size > 1) {
                    HorizontalDivider(); TotalsRow("Total", state.total, bold = true)
                }
            }
        }

        Breakdown("Expenses by category", state.expenseByCategory)
        Breakdown("Income by category", state.incomeByCategory)
        Spacer(Modifier.height(64.dp))
    }

    if (showFilters) {
        FilterSheet(
            filter = state.filter,
            accounts = state.accounts,
            categories = state.categories,
            showKinds = false,
            onApply = { viewModel.setFilter(it); showFilters = false },
            onDismiss = { showFilters = false },
        )
    }
}

private fun periodLabel(period: String) =
    if (period.length == 7) YearMonth.parse(period).format(MONTH) else period

@Composable
private fun TotalsRow(label: String, r: PeriodTotals, bold: Boolean = false) =
    TotalsRow(
        label,
        MoneyFormat.amount(r.expense),
        MoneyFormat.amount(r.income),
        MoneyFormat.amount(r.savings),
        MoneyFormat.amount(r.withdrawals),
        bold = bold
    )

@Composable
private fun TotalsRow(
    label: String,
    c1: String,
    c2: String,
    c3: String,
    c4: String,
    header: Boolean = false,
    bold: Boolean = false
) {
    val style =
        if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val weight = if (bold || header) FontWeight.SemiBold else FontWeight.Normal
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, style = style, fontWeight = weight, modifier = Modifier.width(84.dp))
        listOf(c1, c2, c3, c4).forEach {
            Text(
                it,
                style = style,
                fontWeight = weight,
                fontFamily = if (header) null else FontFamily.Monospace,
                textAlign = TextAlign.End,
                modifier = Modifier.width(118.dp)
            )
        }
    }
}

@Composable
private fun Breakdown(title: String, lines: List<CategoryLine>) {
    if (lines.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            lines.forEach { line ->
                CategoryRow(line, indent = 0.dp)
                line.children.forEach { CategoryRow(it, indent = 16.dp) }
            }
        }
    }
}

@Composable
private fun CategoryRow(line: CategoryLine, indent: androidx.compose.ui.unit.Dp) {
    Column(modifier = Modifier.padding(start = indent)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                line.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(line.share * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.End
            )
            Text(
                MoneyFormat.amount(line.total),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(118.dp),
                textAlign = TextAlign.End
            )
        }
        LinearProgressIndicator(
            progress = { line.share.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )
    }
}
