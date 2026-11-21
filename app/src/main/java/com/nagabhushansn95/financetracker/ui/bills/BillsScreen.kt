package com.nagabhushansn95.financetracker.ui.bills

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.bills.BillView
import com.nagabhushansn95.financetracker.data.bills.Bills
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.common.SortChip
import java.time.LocalDate

/**
 * Bills: the credit scores on top, then every bill with what falls due next, sorted by the chips (remembered), ended
 * ones folded away. A tap opens the bill; + starts a new one, which then opens so its first payment can be recorded.
 */
@Composable
fun BillsScreen(
    onOpenBill: (Long) -> Unit,
    onOpenCredit: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: BillsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showEnded by rememberSaveable { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.messageShown() }
    }
    val active = viewModel.sorted(state.views.filter { !it.bill.ended }, sort)
    val ended = viewModel.sorted(state.views.filter { it.bill.ended }, sort)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item { CreditScoreCard(state, onOpenCredit) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SortChip(
                        options = BillSort.entries,
                        selected = sort.first,
                        descending = sort.second,
                        titleOf = { it.title },
                        onPick = viewModel::pickSort,
                        onFlip = viewModel::flipSort,
                    )
                    Spacer(Modifier.weight(1f))
                    if (ended.isNotEmpty()) TextButton(onClick = { showEnded = !showEnded }) {
                        Text(if (showEnded) "Hide ended" else "Show ended")
                    }
                }
                HorizontalDivider()
            }
            if (state.loaded && state.views.isEmpty()) item {
                Text(
                    "Nothing here yet. Use + to add a bill: rent, a card, a subscription, a recharge, anything that comes round again.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(active, key = { it.bill.id }) { v ->
                BillRow(
                    v,
                    state,
                    onClick = { onOpenBill(v.bill.id) })
            }
            if (showEnded && ended.isNotEmpty()) {
                item {
                    Text(
                        "Ended",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    HorizontalDivider()
                }
                items(ended, key = { "e${it.bill.id}" }) { v ->
                    BillRow(
                        v,
                        state,
                        onClick = { onOpenBill(v.bill.id) })
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "New bill") }
    }
    if (creating) BillSheet(
        existing = null,
        accounts = state.cashAccounts,
        categories = state.categories,
        onSave = { bill -> creating = false; viewModel.save(bill) { onOpenBill(it) } },
        onDismiss = { creating = false },
    )
}

/** Name and the expected amount; what falls due next; the last payment. Overdue reads in the error colour. */
@Composable
private fun BillRow(v: BillView, state: BillsState, onClick: () -> Unit) {
    val today = state.today
    val days = v.daysLeft(today)
    val late = days != null && days < 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                v.bill.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                v.expected?.let { MoneyFormat.amount(it) } ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Bills.status(v, today),
                style = MaterialTheme.typography.bodySmall,
                color = if (late) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            v.nextDue?.let {
                Text(
                    it.format(SHORT_DATE),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (late) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            lastPaidLine(v, state),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}

fun lastPaidLine(v: BillView, state: BillsState): String {
    val t = v.lastPaid?.transaction ?: return "Not paid yet"
    val account = t.fromAccountId ?: t.toAccountId
    return "Last paid ${t.date.format(SHORT_DATE)} · ${MoneyFormat.amount(t.amount)} · ${
        state.accountName(
            account
        )
    }"
}

/** The latest score of each agency; a tap opens the history. */
@Composable
private fun CreditScoreCard(state: BillsState, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onOpen)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Credit score", style = MaterialTheme.typography.titleMedium)
            if (state.agencies.isEmpty()) Text(
                "No agency yet. Tap to add one and record a score.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.agencies.forEach { agency ->
                val latest = state.latestScore(agency.id)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        agency.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        latest?.let { "${it.score} · ${it.date.format(SHORT_DATE)}" }
                            ?: "No score yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (latest == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (latest == null) FontWeight.Normal else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

internal fun LocalDate.short(): String = format(SHORT_DATE)
