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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
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
    var filter by rememberSaveable { mutableStateOf(BillFilter.ALL) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    val searching = searchOpen || searchText.isNotBlank()
    val searchFocus = remember { FocusRequester() }
    var creating by remember { mutableStateOf(false) }
    // Reorder mode (the Custom sort): the ids in the order being arranged, one of them possibly under the finger.
    var reordering by remember { mutableStateOf(false) }
    val order = remember { mutableStateListOf<Long>() }
    val listState = rememberLazyListState()
    var dragging by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val byId = remember(state.views) { state.views.associateBy { it.bill.id } }
    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.messageShown() }
    }
    val shown = state.views.filter { filter.accepts(it, state) && it.matches(searchText, state) }
    val active = viewModel.sorted(shown.filter { !it.bill.ended && !it.bill.income }, sort)
    val incomes = viewModel.sorted(shown.filter { !it.bill.ended && it.bill.income }, sort)
    // Ended bills stay folded away unless asked for, searched for, or the filter is about them.
    val ended = if (filter == BillFilter.ENDED || searchText.isNotBlank()) viewModel.sorted(
        shown.filter { it.bill.ended },
        sort
    ) else emptyList()
    val narrowed = filter != BillFilter.ALL || searchText.isNotBlank()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
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
                    if (reordering) {
                        Text(
                            "Drag the handles into order",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { reordering = false }) { Text("Cancel") }
                        Button(onClick = {
                            reordering = false; viewModel.saveOrder(order.toList())
                        }) { Text("Save") }
                    } else {
                        SortChip(
                            options = BillSort.entries,
                            selected = sort.first,
                            descending = sort.second,
                            titleOf = { it.title },
                            onPick = viewModel::pickSort,
                            onFlip = viewModel::flipSort,
                        )
                        Spacer(Modifier.weight(1f))
                        if (sort.first == BillSort.CUSTOM) TextButton(onClick = {
                            if (sort.second) viewModel.flipSort() // arranged top to bottom as listed
                            order.clear()
                            order += viewModel.sorted(state.views, sort).map { it.bill.id }
                            dragging = null
                            reordering = true
                        }) { Text("Reorder") }
                        Box {
                            var open by remember { mutableStateOf(false) }
                            FilterChip(
                                selected = filter != BillFilter.ALL,
                                onClick = { open = true },
                                label = { Text(filter.title) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = null
                                    )
                                },
                            )
                            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                BillFilter.entries.forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(if (f == filter) "✓ ${f.title}" else f.title) },
                                        onClick = { filter = f; open = false },
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { searchOpen = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search bills")
                        }
                    }
                }
                if (searching && !reordering) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search bills") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { searchText = ""; searchOpen = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close search")
                            }
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
                if (narrowed && !reordering && state.loaded) Text(
                    "${active.size + incomes.size + ended.size} of ${state.views.size} bills",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
                HorizontalDivider()
            }
            if (reordering) {
                items(order, key = { it }) { id ->
                    val v = byId[id] ?: return@items
                    val held = dragging == id
                    ReorderRow(
                        v = v,
                        held = held,
                        modifier = Modifier
                            .then(if (held) Modifier else Modifier.animateItem())
                            .zIndex(if (held) 1f else 0f)
                            .graphicsLayer { translationY = if (held) dragOffset else 0f },
                        handle = Modifier.pointerInput(id) {
                            detectDragGestures(
                                onDragStart = { dragging = id; dragOffset = 0f },
                                onDragEnd = { dragging = null; dragOffset = 0f },
                                onDragCancel = { dragging = null; dragOffset = 0f },
                            ) { change, delta ->
                                change.consume()
                                dragOffset += delta.y
                                val rows = listState.layoutInfo.visibleItemsInfo
                                val me =
                                    rows.firstOrNull { it.key == id } ?: return@detectDragGestures
                                val centre = me.offset + me.size / 2 + dragOffset
                                val over =
                                    rows.firstOrNull { it.key is Long && it.key != id && centre >= it.offset && centre < it.offset + it.size }
                                        ?: return@detectDragGestures
                                val from = order.indexOf(id)
                                val to = order.indexOf(over.key as Long)
                                if (from >= 0 && to >= 0) {
                                    order.add(to, order.removeAt(from))
                                    // The row is now laid out where the other one was; keep it under the finger.
                                    dragOffset -= (over.offset - me.offset)
                                }
                            }
                        },
                    )
                }
                return@LazyColumn
            }
            if (state.loaded && state.views.isEmpty()) item {
                Text(
                    "Nothing here yet. Use + to add a bill: rent, a card, a subscription, a recharge, anything that comes round again. An income that comes round, a salary say, goes in the same way.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else if (state.loaded && narrowed && active.isEmpty() && incomes.isEmpty() && ended.isEmpty()) item {
                Text(
                    if (searchText.isNotBlank()) "No bill matches \"${searchText.trim()}\"." else "No bills under ${filter.title}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(active, key = { it.bill.id }) { v ->
                BillRow(
                    v,
                    state,
                    onClick = { onOpenBill(v.bill.id) })
            }
            if (incomes.isNotEmpty()) {
                item {
                    Text(
                        "Income",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    HorizontalDivider()
                }
                items(incomes, key = { "i${it.bill.id}" }) { v ->
                    BillRow(
                        v,
                        state,
                        onClick = { onOpenBill(v.bill.id) })
                }
            }
            if (ended.isNotEmpty()) {
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

/** A row in reorder mode: the name and status, with a handle to drag it by where the amount and date usually are. */
@Composable
private fun ReorderRow(v: BillView, held: Boolean, modifier: Modifier, handle: Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (held) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)
                .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(v.bill.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (v.bill.ended) "Ended" else Bills.scheduleText(v.bill, v.dueDay),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Filled.Menu,
                contentDescription = "Drag to move",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = handle.padding(12.dp),
            )
        }
        HorizontalDivider()
    }
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
    val t =
        v.lastPaid?.transaction ?: return if (v.bill.income) "Not received yet" else "Not paid yet"
    val account = t.fromAccountId ?: t.toAccountId
    return "${if (v.bill.income) "Last received" else "Last paid"} ${t.date.format(SHORT_DATE)} · ${
        MoneyFormat.amount(
            t.amount
        )
    } · ${
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
