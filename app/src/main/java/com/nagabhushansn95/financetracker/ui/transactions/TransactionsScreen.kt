package com.nagabhushansn95.financetracker.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.ui.common.FilterSheet
import com.nagabhushansn95.financetracker.ui.common.PeriodMenu
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")

/** The amount column is this wide whatever the amount, so the particulars always get the same room. */
private val AMOUNT_WIDTH = 112.dp

/**
 * Ledger grouped by day with sticky date headers, a period menu, a filter sheet and a search field. A single tap
 * does nothing (stray touches while keeping the screen awake must not open anything); press and hold a row for
 * Edit / Delete, or, for an investment or redemption, to open the instrument's screen where it is edited.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    onEdit: (Long) -> Unit,
    onOpenInstrument: (InstrumentType, Long) -> Unit,
    onOpenPfAccount: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: TransactionsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var menuFor by remember { mutableStateOf<LedgerRow?>(null) }
    var confirmDelete by remember { mutableStateOf<LedgerRow?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    // The search field opens from the magnifier and stays while it holds text (the filter sheet sets the same text).
    var searchOpen by remember { mutableStateOf(false) }
    val searching = searchOpen || state.filter.query.isNotBlank()
    val searchFocus = remember { FocusRequester() }
    // Typed text lives here so keystrokes are not lost while the filtered list is being recomputed.
    var searchText by remember { mutableStateOf(state.filter.query) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            PeriodMenu(state.filter, onChange = viewModel::setFilter)
            Spacer(Modifier.weight(1f))
            BadgedBox(badge = { if (state.filter.activeCount > 0) Badge { Text("${state.filter.activeCount}") } }) {
                TextButton(onClick = { showFilters = true }) { Text("Filters") }
            }
            IconButton(onClick = { searchOpen = true }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        }
        if (searching) {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it; viewModel.setFilter(state.filter.copy(query = it))
                },
                placeholder = { Text("Search particulars") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = {
                        searchText =
                            ""; viewModel.setFilter(state.filter.copy(query = "")); searchOpen =
                        false
                    }) { Icon(Icons.Filled.Close, contentDescription = "Close search") }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .focusRequester(searchFocus),
            )
            LaunchedEffect(searchOpen) { if (searchOpen) searchFocus.requestFocus() }
        }
        if (state.loaded && state.shown != state.total) {
            Text(
                "%,d of %,d transactions".format(state.shown, state.total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (state.loaded && state.days.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (state.total == 0) "No transactions yet. Use + to add one." else "Nothing matches these filters.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LedgerList(state, listState, onLongPress = { menuFor = it })
        }
    }

    if (showFilters) {
        FilterSheet(
            filter = state.filter,
            accounts = state.accounts,
            categories = state.categories,
            showKinds = true,
            onApply = { searchText = it.query; viewModel.setFilter(it); showFilters = false },
            onDismiss = { showFilters = false },
        )
    }

    menuFor?.let { row ->
        ModalBottomSheet(onDismissRequest = { menuFor = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    row.particulars.ifBlank { row.description },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                if (row.particulars.isNotBlank()) Text(
                    row.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                if (row.editedUnderInvestments) {
                    ListItem(
                        headlineContent = { Text("Open in Investments") },
                        supportingContent = {
                            Text(
                                when {
                                    row.pfAccountId != null -> "Provident-fund entries are recorded and edited on the account's screen."
                                    row.isInsurance -> "Insurance entries are recorded and edited on the policy's screen."
                                    row.instrumentType == InstrumentType.OTHER -> "Loans and money parked elsewhere are recorded and edited on their own screen under Others."
                                    row.isHolding -> "Buys, sells, dividends, bonus units and charges on a stock or fund are recorded and edited on its own screen."
                                    else -> "Investments and redemptions are edited on the instrument's screen."
                                }
                            )
                        },
                        modifier = Modifier.combinedClickable(onClick = {
                            menuFor = null
                            val pf = row.pfAccountId
                            if (pf != null) onOpenPfAccount(pf)
                            else onOpenInstrument(row.instrumentType!!, row.instrumentId!!)
                        })
                    )
                } else {
                    ListItem(
                        headlineContent = { Text("Edit") },
                        modifier = Modifier.combinedClickable(onClick = {
                            menuFor = null; onEdit(row.id)
                        })
                    )
                    ListItem(
                        headlineContent = {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.combinedClickable(onClick = {
                            menuFor = null; confirmDelete = row
                        })
                    )
                }
            }
        }
    }

    confirmDelete?.let { row ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this transaction?") },
            text = { Text("${row.particulars}\n${row.amountText}\nThe account balances are adjusted back.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    viewModel.delete(row.id) { message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message
                            )
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LedgerList(
    state: TransactionsUiState,
    listState: LazyListState,
    onLongPress: (LedgerRow) -> Unit
) {
    val scope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 8 } }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            state.days.forEach { day ->
                stickyHeader(key = "h${day.date}") {
                    Text(
                        day.date.format(DAY_FORMAT),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(day.rows, key = { it.id }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = {}, onLongClick = { onLongPress(row) })
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)) {
                            // Without particulars the description takes the main line, so the row looks complete.
                            Text(
                                row.particulars.ifBlank { row.description },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (row.hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            if (row.particulars.isNotBlank()) Text(
                                row.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            (if (row.outflow) "−" else "+") + row.amountText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (row.outflow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.width(AMOUNT_WIDTH),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            // Long amounts shrink a little instead of eating into the particulars.
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 11.sp,
                                maxFontSize = 16.sp,
                                stepSize = 0.5.sp
                            ),
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
        AnimatedVisibility(
            visible = showScrollToTop, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            SmallFloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Scroll to top")
            }
        }
    }
}
