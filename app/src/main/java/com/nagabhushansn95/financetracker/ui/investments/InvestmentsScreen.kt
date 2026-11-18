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
import com.nagabhushansn95.financetracker.ui.common.StatCell
import java.time.format.DateTimeFormatter

private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Routes of the screens under Investments. */
object InvestmentRoutes {
    const val STOCKS = "investments/stocks"
    const val FUNDS = "investments/funds"
    const val HOLDING = "investments/holding/{id}"
    const val PROVIDENT_FUNDS = "investments/pf"
    const val TARGETS = "investments/targets"
    const val FIXED_DEPOSITS = "investments/fd"
    const val FIXED_DEPOSIT = "investments/fd/{id}"
    const val INSURANCE = "investments/insurance"
    const val POLICY = "investments/insurance/{id}"
    const val INSURANCE_TYPE = "investments/insurance/type/{type}"
    const val PF_ACCOUNT = "investments/pf/{id}"
    const val SAVINGS = "investments/savings"
    const val OTHERS = "investments/others"
    const val OTHER = "investments/others/{id}"
    fun fixedDeposit(id: Long) = "investments/fd/$id"
    fun holding(id: Long) = "investments/holding/$id"
    fun other(id: Long) = "investments/others/$id"
    fun policy(id: Long) = "investments/insurance/$id"
    fun insuranceType(type: com.nagabhushansn95.financetracker.data.db.entity.InsuranceType) =
        "investments/insurance/type/${type.name}"
    fun pfAccount(id: Long) = "investments/pf/$id"

    /** The list screen for a type. */
    fun type(type: InstrumentType) = when (type) {
        InstrumentType.STOCK -> STOCKS
        InstrumentType.MUTUAL_FUND -> FUNDS
        InstrumentType.FIXED_DEPOSIT -> FIXED_DEPOSITS
        InstrumentType.INSURANCE, InstrumentType.VEHICLE -> INSURANCE
        InstrumentType.OTHER -> OTHERS
    }
    fun isInvestmentRoute(route: String?) = route?.startsWith("investments/") == true
}

/**
 * Investments hub: the totals, tiles that open the per-type screens, and the Investment Summary sheet live
 * (categories, risk levels, the user's own groups) in narrow tables with three switchable metric columns.
 */
@Composable
fun InvestmentsScreen(onNavigate: (String) -> Unit, viewModel: InvestmentsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savings by viewModel.savings.collectAsStateWithLifecycle()
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
        item { Tiles(summary, savings, onNavigate) }
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
                        if (summary.goalAmount == null) "No goal amount set: targets follow the wealth shares. Set it under Targets."
                        else "Targets: goal ${MoneyFormat.compact(summary.goalAmount)} by the goal shares, the rest by the wealth shares. Bar: current share; tick: target; box: tolerance. \"!\" marks a drift beyond it.",
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
                        "By risk",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    SummaryTable(
                        lines = summary.riskLevels,
                        summary = summary,
                        view = view,
                        expanded = expanded,
                        onToggle = { key ->
                            expanded = if (key in expanded) expanded - key else expanded + key
                        },
                        footer = summary.total,
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Groups",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (summary.groups.isEmpty()) Text(
                        "No groups yet. Add them under Manage › Summary groups: any mix of kinds and categories, such as what is liquid or what sits in one broker.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) else SummaryTable(
                        lines = summary.groups,
                        summary = summary,
                        view = view,
                        expanded = expanded,
                        onToggle = { key ->
                            expanded = if (key in expanded) expanded - key else expanded + key
                        },
                    )
                    Text(
                        "Groups overlap by design; each is set up under Manage › Summary groups. Targets are the members' targets added up.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
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
                Stat("Invested", MoneyFormat.amount(s.total.invested)); Stat(
                "Held P/L",
                MoneyFormat.amount(s.total.held),
                s.total.held
            ); Stat("Realised", MoneyFormat.amount(s.total.realised), s.total.realised)
            }
            Row {
                Stat("XIRR", pct(s.total.xirr)); Stat(
                "Income",
                MoneyFormat.amount(s.total.income)
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
    StatCell(label, value, signed)
}

/** Under a tile's value, one short line each: the invested amount, the held P/L with its percentage in colour, the XIRR. */
@Composable
private fun TileFigures(line: com.nagabhushansn95.financetracker.data.investments.SummaryLine) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val small = MaterialTheme.typography.bodySmall
    Text(
        "Invested ${MoneyFormat.compact(line.invested)}",
        style = small,
        color = muted,
        maxLines = 1
    )
    val held = line.held
    val heldPct = if (line.invested > 0) held / line.invested else null
    Text(
        (if (held >= 0) "+" else "") + MoneyFormat.compact(held) + (heldPct?.let { " (${pct(it)})" }
            ?: ""),
        style = small,
        color = if (held < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
        maxLines = 1,
    )
    line.xirr?.let { Text("XIRR ${pct(it)}", style = small, color = muted, maxLines = 1) }
}

private data class Tile(
    val title: String,
    val value: String,
    val route: String,
    val note: String? = null,
    /** The group's line, for the invested amount, held P/L and XIRR under the value. */
    val line: com.nagabhushansn95.financetracker.data.investments.SummaryLine? = null,
)

/**
 * Two tiles per row, each opening a screen: the groups in the order of their categories (deposits, PF, insurance,
 * stocks, funds, and the "Others", money parked elsewhere and loans, which are savings rather than investments and
 * sit outside the total), then Savings and Categories & targets.
 */
@Composable
private fun Tiles(s: InvestmentSummary, savings: SavingsGlance?, onNavigate: (String) -> Unit) {
    // A group's tile: the current value, with the invested amount, held P/L and XIRR under it.
    fun tile(
        title: String,
        line: com.nagabhushansn95.financetracker.data.investments.SummaryLine?,
        route: String
    ) =
        Tile(title, MoneyFormat.compact(line?.current ?: 0.0), route, line = line)
    val byGroup = mapOf(
        com.nagabhushansn95.financetracker.data.investments.InvestmentGroup.FIXED_DEPOSITS to tile(
            "Fixed deposits",
            s.byType[InstrumentType.FIXED_DEPOSIT],
            InvestmentRoutes.type(InstrumentType.FIXED_DEPOSIT)
        ),
        com.nagabhushansn95.financetracker.data.investments.InvestmentGroup.PROVIDENT_FUNDS to tile(
            "Provident funds",
            s.providentFunds,
            InvestmentRoutes.PROVIDENT_FUNDS
        ),
        com.nagabhushansn95.financetracker.data.investments.InvestmentGroup.INSURANCE to tile(
            "Insurance",
            s.byType[InstrumentType.INSURANCE],
            InvestmentRoutes.type(InstrumentType.INSURANCE)
        ),
        com.nagabhushansn95.financetracker.data.investments.InvestmentGroup.STOCKS to tile(
            "Stocks & ETFs",
            s.byType[InstrumentType.STOCK],
            InvestmentRoutes.STOCKS
        ),
        com.nagabhushansn95.financetracker.data.investments.InvestmentGroup.MUTUAL_FUNDS to tile(
            "Mutual funds",
            s.byType[InstrumentType.MUTUAL_FUND],
            InvestmentRoutes.FUNDS
        ),
        com.nagabhushansn95.financetracker.data.investments.InvestmentGroup.OTHERS to Tile(
            "Others",
            MoneyFormat.compact(s.byType[InstrumentType.OTHER]?.current ?: 0.0),
            InvestmentRoutes.OTHERS,
            note = "Savings, not investments: outside the total above",
        ),
    )
    // Same order as the categories under Manage › Investment categories (the summary lists them in that order).
    val tiles =
        com.nagabhushansn95.financetracker.data.investments.InvestmentOrder.groups(s.categories.map { it.name })
            .mapNotNull { byGroup[it] } + listOf(
            Tile(
                "Savings",
                savings?.let { MoneyFormat.compact(it.thisMonth) + " this month" }
                    ?: "monthly · yearly",
                InvestmentRoutes.SAVINGS,
                note = savings?.let { MoneyFormat.compact(it.thisYear) + " this year" },
            ),
            Tile(
                "Targets",
                s.goalAmount?.let { "goal " + MoneyFormat.compact(it) } ?: "goal not set",
                InvestmentRoutes.TARGETS
            ),
        )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { tile ->
                    Card(modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(tile.route) }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(tile.title, style = MaterialTheme.typography.labelLarge)
                            Text(
                                tile.value,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            tile.line?.let { line -> TileFigures(line) }
                            tile.note?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
