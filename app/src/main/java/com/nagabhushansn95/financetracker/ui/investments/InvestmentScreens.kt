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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.investments.HoldingRow
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummary
import com.nagabhushansn95.financetracker.data.investments.SummaryLine
import com.nagabhushansn95.financetracker.ui.common.LoadingOrMessage
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.StatCell
import com.nagabhushansn95.financetracker.ui.edit.AccountSheet
import com.nagabhushansn95.financetracker.ui.edit.EditDataViewModel
import com.nagabhushansn95.financetracker.ui.theme.appTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvestmentScaffold(
    title: String,
    onBack: () -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    empty: String = "Nothing invested yet.",
    content: @Composable (InvestmentSummary) -> Unit
) {
    val viewModel: InvestmentSummaryViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
                title = { Text(title) },
                navigationIcon = { com.nagabhushansn95.financetracker.ui.common.BackAndMenu(onBack) })
        },
        floatingActionButton = floatingActionButton,
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            val summary = state.summary
            if (summary == null) LoadingOrMessage(state.loaded, empty, Modifier.fillMaxSize())
            else content(summary)
        }
    }
}

/**
 * EPF and PPF: contributions, interest, balance and XIRR per account. The + adds a provident-fund account; like
 * every other investment, these accounts are added, edited and deleted only here, never under Manage.
 */
@Composable
fun ProvidentFundsScreen(
    onBack: () -> Unit,
    onOpenAccount: (Long) -> Unit = {}
) {
    val editData: EditDataViewModel = viewModel()
    var adding by remember { mutableStateOf(false) }
    if (adding) AccountSheet(
        existing = null,
        initialType = AccountType.PROVIDENT_FUND,
        transactionCount = null,
        onSave = { editData.saveAccount(it); adding = false },
        onDelete = null,
        onDismiss = { adding = false },
        types = listOf(AccountType.PROVIDENT_FUND),
    )
    InvestmentScaffold(
        "Provident funds", onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add a provident-fund account"
                )
            }
        },
        empty = "No provident-fund accounts yet. Add one with +.",
    ) { s ->
        val pf = s.providentFunds
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (pf == null) item { Text("No provident-fund accounts yet. Add one with +.") }
            else {
                item { TypeHeader(listOf(pf)) }
                items(pf.children, key = { it.name }) { line ->
                    ProvidentFundCard(line, onOpenAccount)
                }
                item {
                    Text(
                        "Open an account to record contributions, interest credits, transfers in and withdrawals from its + button. " +
                                "They show under Transactions like any other row but are edited only there.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvidentFundCard(line: SummaryLine, onOpen: (Long) -> Unit) {
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
            if (line.accountId != null) {
                Text(
                    "Tap for the contributor breakdown, financial years, the ledger and the + button.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun TypeHeader(lines: List<SummaryLine>) {
    if (lines.isEmpty()) return
    val invested = lines.sumOf { it.invested }
    val current = lines.sumOf { it.current }
    val realised = lines.sumOf { it.realised }
    val xirr = lines.singleOrNull()?.xirr
        ?: com.nagabhushansn95.financetracker.data.investments.Holdings.xirr(
            lines.flatMap { it.cashFlows }, current, java.time.LocalDate.now()
        )
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    MoneyFormat.amount(current),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "XIRR ${pct(xirr)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
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
internal fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    HorizontalDivider()
}

@Composable
internal fun ProfitLine(h: HoldingRow) {
    Row {
        val held = h.heldProfit
        Text(
            "Held ${MoneyFormat.amount(held)}" + (h.heldProfitPct?.let { " (${pct(it)})" } ?: ""),
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
