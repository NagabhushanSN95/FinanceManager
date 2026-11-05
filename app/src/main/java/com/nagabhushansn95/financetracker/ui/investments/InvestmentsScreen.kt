package com.nagabhushansn95.financetracker.ui.investments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummary
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import java.time.format.DateTimeFormatter

private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Routes of the screens under Investments. */
object InvestmentRoutes {
    const val HOLDINGS = "investments/holdings"
    const val PROVIDENT_FUNDS = "investments/pf"
    const val TARGETS = "investments/targets"
    const val TYPE = "investments/type/{type}"
    const val FIXED_DEPOSITS = "investments/fd"
    const val FIXED_DEPOSIT = "investments/fd/{id}"
    const val INSURANCE = "investments/insurance"
    const val POLICY = "investments/insurance/{id}"
    const val PF_ACCOUNT = "investments/pf/{id}"
    const val SAVINGS = "investments/savings"
    fun fixedDeposit(id: Long) = "investments/fd/$id"
    fun policy(id: Long) = "investments/insurance/$id"
    fun pfAccount(id: Long) = "investments/pf/$id"
    fun type(type: InstrumentType) = when (type) {
        InstrumentType.FIXED_DEPOSIT -> FIXED_DEPOSITS
        InstrumentType.INSURANCE -> INSURANCE
        else -> "investments/type/${type.name}"
    }
    fun isInvestmentRoute(route: String?) = route?.startsWith("investments/") == true
}

/**
 * Investments hub: the totals, tiles that open the per-type screens, and the Investment Summary sheet live
 * (categories, risk levels, Zerodha, liquid) in a narrow table with three switchable metric columns.
 */
@Composable
fun InvestmentsScreen(onNavigate: (String) -> Unit, viewModel: InvestmentsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val summary = state.summary
    if (summary == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { Text(if (state.loaded) "Nothing invested yet." else "Loading…") }
        return
    }
    var view by rememberSaveable { mutableStateOf(MetricView.VALUE) }
    var expanded by rememberSaveable { mutableStateOf(setOf<String>()) }
    val prices by viewModel.prices.collectAsStateWithLifecycle()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TotalsCard(summary, prices, viewModel::refreshPrices) }
        item { Tiles(summary, onNavigate) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Investment summary", style = MaterialTheme.typography.titleMedium)
                    MetricViewChips(
                        view,
                        { view = it },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    SummaryTable(
                        lines = summary.categories,
                        summary = summary,
                        view = view,
                        expanded = expanded,
                        onToggle = { key ->
                            expanded = if (key in expanded) expanded - key else expanded + key
                        },
                        footer = summary.total,
                    )
                    if (view == MetricView.ALLOCATION) Text(
                        if (summary.goalAmount == null) "No goal amount set: targets follow the wealth shares. Set it under Categories & targets."
                        else "Targets: goal ${MoneyFormat.compact(summary.goalAmount)} by the goal shares, the rest by the wealth shares. \"!\" marks a drift beyond the tolerance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "By risk and liquidity",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    SummaryTable(
                        lines = summary.riskLevels + listOf(summary.zerodha, summary.liquid),
                        summary = summary,
                        view = view,
                        expanded = expanded,
                        onToggle = { key ->
                            expanded = if (key in expanded) expanded - key else expanded + key
                        },
                    )
                }
            }
        }
        item { Box(modifier = Modifier.padding(bottom = 48.dp)) }
    }
}

@Composable
private fun TotalsCard(s: InvestmentSummary, prices: PriceStatus, onRefresh: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Total investments", style = MaterialTheme.typography.labelLarge)
            Text(
                MoneyFormat.amount(s.total.current),
                style = MaterialTheme.typography.headlineSmall
            )
            Row {
                Stat("Invested", MoneyFormat.compact(s.total.invested)); Stat(
                "Held P/L",
                MoneyFormat.compact(s.total.held),
                s.total.held
            ); Stat("Realised", MoneyFormat.compact(s.total.realised), s.total.realised)
            }
            Row {
                Stat("XIRR", pct(s.total.xirr)); Stat(
                "Income",
                MoneyFormat.compact(s.total.income)
            ); Stat("Prices as of", s.latestPriceDate?.format(DATE) ?: "no prices yet")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (prices.refreshing) "Updating prices…" else prices.message
                        ?: "Stock prices from Yahoo Finance, fund NAVs from AMFI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (prices.refreshing) CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                else TextButton(onClick = onRefresh) { Text("Update prices") }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Stat(
    label: String,
    value: String,
    signed: Double? = null
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
            color = when {
                signed == null -> MaterialTheme.colorScheme.onSurface; signed < 0 -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.tertiary
            },
        )
    }
}

/** Two tiles per row, each opening a screen: deposits, PF, insurance, stocks & funds, others, categories, savings. */
@Composable
private fun Tiles(s: InvestmentSummary, onNavigate: (String) -> Unit) {
    val stocksAndFunds = (s.byType[InstrumentType.STOCK]?.current
        ?: 0.0) + (s.byType[InstrumentType.MUTUAL_FUND]?.current ?: 0.0)
    val byGroup = mapOf(
        com.nagabhushansn95.financetracker.data.investments.InvestmentGroup.FIXED_DEPOSITS to Triple(
            "Fixed deposits",
            MoneyFormat.compact(s.byType[InstrumentType.FIXED_DEPOSIT]?.current ?: 0.0),
            InvestmentRoutes.type(InstrumentType.FIXED_DEPOSIT)
        ),
        com.nagabhushansn95.financetracker.data.investments.InvestmentGroup.PROVIDENT_FUNDS to Triple(
            "Provident funds",
            MoneyFormat.compact(s.providentFunds?.current ?: 0.0),
            InvestmentRoutes.PROVIDENT_FUNDS
        ),
        com.nagabhushansn95.financetracker.data.investments.InvestmentGroup.INSURANCE to Triple(
            "Insurance",
            MoneyFormat.compact(s.byType[InstrumentType.INSURANCE]?.current ?: 0.0),
            InvestmentRoutes.type(InstrumentType.INSURANCE)
        ),
        com.nagabhushansn95.financetracker.data.investments.InvestmentGroup.STOCKS_AND_FUNDS to Triple(
            "Stocks & funds",
            MoneyFormat.compact(stocksAndFunds),
            InvestmentRoutes.HOLDINGS
        ),
        com.nagabhushansn95.financetracker.data.investments.InvestmentGroup.OTHERS to Triple(
            "Others",
            MoneyFormat.compact(s.byType[InstrumentType.OTHER]?.current ?: 0.0),
            InvestmentRoutes.type(InstrumentType.OTHER)
        ),
    )
    // Same order as the categories under Manage › Investment categories (the summary lists them in that order).
    val tiles =
        com.nagabhushansn95.financetracker.data.investments.InvestmentOrder.groups(s.categories.map { it.name })
            .map { byGroup.getValue(it) } + listOf(
        Triple(
            "Categories & targets",
            s.goalAmount?.let { "goal " + MoneyFormat.compact(it) } ?: "goal not set",
            InvestmentRoutes.TARGETS),
        Triple("Savings", "monthly · yearly", InvestmentRoutes.SAVINGS),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (title, value, route) ->
                    Card(modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(route) }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(title, style = MaterialTheme.typography.labelLarge)
                            Text(
                                value,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
