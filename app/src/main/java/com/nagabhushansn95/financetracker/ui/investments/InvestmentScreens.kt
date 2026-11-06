package com.nagabhushansn95.financetracker.ui.investments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.investments.HoldingRow
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummary
import com.nagabhushansn95.financetracker.data.investments.SummaryLine
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.StatCell
import com.nagabhushansn95.financetracker.ui.transaction.typeTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvestmentScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (InvestmentSummary) -> Unit
) {
    val viewModel: InvestmentSummaryViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = { com.nagabhushansn95.financetracker.ui.common.BackAndMenu(onBack) })
    }) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            val summary = state.summary
            if (summary == null) Text(
                if (state.loaded) "Nothing invested yet." else "Loading…",
                modifier = Modifier.padding(16.dp)
            )
            else content(summary)
        }
    }
}

/** Stocks and funds: units, average cost, price, value, held and realised profit, XIRR, grouped by category. */
@Composable
fun HoldingsScreen(onBack: () -> Unit, onOpenInstrument: (Long) -> Unit) {
    InvestmentScaffold("Stocks & funds", onBack) { s ->
        val groups = s.holdings.groupBy { it.categoryName }
        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
            item {
                TypeHeader(
                    listOfNotNull(
                        s.byType[InstrumentType.STOCK],
                        s.byType[InstrumentType.MUTUAL_FUND]
                    )
                )
            }
            groups.forEach { (category, rows) ->
                item(key = "h:$category") { SectionTitle(category) }
                items(rows, key = { it.instrument.id }) { h ->
                    HoldingRowView(h) {
                        onOpenInstrument(
                            h.instrument.id
                        )
                    }
                }
            }
        }
    }
}

/** Deposits, policies or others: one row per instrument, own first, then closed, then family-owned. */
@Composable
fun InstrumentListScreen(
    type: InstrumentType,
    onBack: () -> Unit,
    onOpenInstrument: (Long) -> Unit
) {
    InvestmentScaffold(typeTitle(type), onBack) { s ->
        val rows = s.rows.filter { it.instrument.instrumentType == type }
            .sortedWith(compareBy({ it.instrument.displayOrder }, { it.instrument.id }))
        val own = rows.filter { it.instrument.owner == Instrument.OWNER_SELF }
        val sections = listOf(
            "Active" to own.filter { it.instrument.active },
            "Closed or retired" to own.filter { !it.instrument.active },
            "Family (not in the totals)" to rows.filter { it.instrument.owner != Instrument.OWNER_SELF },
        ).filter { it.second.isNotEmpty() }
        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
            item { TypeHeader(listOfNotNull(s.byType[type])) }
            if (type == InstrumentType.OTHER) item {
                Text(
                    "Money parked elsewhere and loans: savings rather than investments. These count in the Savings tables but not under Total investments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (sections.isEmpty()) item {
                Text(
                    "Nothing here yet.",
                    modifier = Modifier.padding(16.dp)
                )
            }
            sections.forEach { (title, list) ->
                item(key = "s:$title") { SectionTitle(title) }
                items(
                    list,
                    key = { it.instrument.id }) { h -> AmountRowView(h) { onOpenInstrument(h.instrument.id) } }
            }
        }
    }
}

/** EPF and PPF: contributions, interest, balance and XIRR per account. */
@Composable
fun ProvidentFundsScreen(
    onBack: () -> Unit,
    onAddToAccount: (Long) -> Unit,
    onOpenAccount: (Long) -> Unit = {}
) {
    InvestmentScaffold("Provident funds", onBack) { s ->
        val pf = s.providentFunds
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (pf == null) item { Text("No provident-fund accounts. Add one under Manage › Accounts.") }
            else {
                item { TypeHeader(listOf(pf)) }
                items(pf.children, key = { it.name }) { line ->
                    ProvidentFundCard(
                        line,
                        onAddToAccount,
                        onOpenAccount,
                    )
                }
                item {
                    Text(
                        "Record employer and employee contributions as Income into the account with the contributor chip; " +
                                "PPF deposits as a Transfer from the bank; interest as Income with contributor Interest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvidentFundCard(line: SummaryLine, onAdd: (Long) -> Unit, onOpen: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .let { m -> line.accountId?.let { id -> m.clickable { onOpen(id) } } ?: m }) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    line.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    MoneyFormat.amount(line.current),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row {
                Stat("Contributions", MoneyFormat.amount(line.invested)); Stat(
                "Interest",
                MoneyFormat.amount(line.income)
            ); Stat("XIRR", pct(line.xirr))
            }
            line.accountId?.let { id ->
                OutlinedButton(
                    onClick = { onAdd(id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add a contribution") }
                Text(
                    "Tap for the contributor breakdown, financial years and the ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TypeHeader(lines: List<SummaryLine>) {
    if (lines.isEmpty()) return
    val invested = lines.sumOf { it.invested }
    val current = lines.sumOf { it.current }
    val realised = lines.sumOf { it.realised }
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(MoneyFormat.amount(current), style = MaterialTheme.typography.headlineSmall)
            Row {
                Stat("Invested", MoneyFormat.amount(invested)); Stat(
                "Held P/L",
                MoneyFormat.amount(current - invested),
                current - invested
            ); Stat("Realised", MoneyFormat.amount(realised), realised)
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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    HorizontalDivider()
}

@Composable
private fun HoldingRowView(h: HoldingRow, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                h.instrument.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                MoneyFormat.amount(h.currentValue),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        val units = h.position.units.takeIf { h.position.hasUnits }
        val line = listOfNotNull(
            units?.let {
                "%s units".format(
                    if (it == it.toLong().toDouble()) it.toLong().toString() else "%.3f".format(it)
                )
            },
            h.position.averageCost?.let { "avg %.2f".format(it) },
            h.unitPrice?.let { "price %.2f".format(it) } ?: if (units != null) "no price" else null,
            "invested ${MoneyFormat.amount(h.position.costHeld)}",
        ).joinToString(" · ")
        Text(
            line,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ProfitLine(h)
    }
    HorizontalDivider()
}

@Composable
private fun AmountRowView(h: HoldingRow, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                h.instrument.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                MoneyFormat.amount(h.currentValue),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            listOfNotNull(
                "invested ${MoneyFormat.amount(h.position.costHeld)}",
                h.position.firstDate?.let { "since $it" },
                h.instrument.owner.takeIf { it != Instrument.OWNER_SELF }
                    ?.let { "owner $it" }).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProfitLine(h)
    }
    HorizontalDivider()
}

@Composable
private fun ProfitLine(h: HoldingRow) {
    Row {
        val held = h.heldProfit
        Text(
            "Held ${MoneyFormat.amount(held)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (held < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "Realised ${MoneyFormat.amount(h.position.realised)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (h.position.realised < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "XIRR ${pct(h.xirr)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
