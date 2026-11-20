package com.nagabhushansn95.financetracker.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.ui.summary.SummaryViewModel
import com.nagabhushansn95.financetracker.ui.transaction.label
import java.time.Instant
import java.time.ZoneOffset

/** One button showing the current period; it opens a menu with the periods, "Custom" a date-range dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodMenu(
    filter: LedgerFilter,
    onChange: (LedgerFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRange by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        androidx.compose.material3.AssistChip(
            onClick = { open = true },
            label = { Text(filter.rangeLabel()) },
            trailingIcon = {
                androidx.compose.material3.Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Change period"
                )
            },
        )
        androidx.compose.material3.DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false }) {
            Period.entries.forEach { p ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (p == filter.period) "✓ ${p.label}" else p.label) },
                    onClick = {
                        open = false
                        if (p == Period.CUSTOM) showRange =
                            true else onChange(filter.copy(period = p))
                    },
                )
            }
        }
    }
    if (showRange) {
        val (from, to) = filter.range()
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = from.atStartOfDay().toInstant(ZoneOffset.UTC)
                .toEpochMilli(),
            initialSelectedEndDateMillis = to.atStartOfDay().toInstant(ZoneOffset.UTC)
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showRange = false },
            confirmButton = {
                TextButton(onClick = {
                    val a = state.selectedStartDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    val b = state.selectedEndDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    if (a != null) onChange(
                        filter.copy(
                            period = Period.CUSTOM,
                            customFrom = a,
                            customTo = b ?: a
                        )
                    )
                    showRange = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showRange = false }) { Text("Cancel") } },
        ) { DateRangePicker(state = state, modifier = Modifier.height(480.dp)) }
    }
}

/**
 * Kinds, accounts and categories. Changes apply on "Apply". The sheet does not drag: pulling the chip list down
 * used to close it by accident, so it closes with the X, the back button or a tap outside.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun FilterSheet(
    filter: LedgerFilter,
    accounts: List<Account>,
    categories: List<Category>,
    showKinds: Boolean,
    onApply: (LedgerFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(filter) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = false,
        dragHandle = null,
    ) {
        // Header and Apply stay put; only the chips scroll, so Apply is always reachable however many accounts there are.
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 16.dp, end = 4.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    "Filters",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    draft = LedgerFilter(
                        period = draft.period,
                        customFrom = draft.customFrom,
                        customTo = draft.customTo,
                        query = draft.query,
                    )
                }) { Text("Clear") }
                androidx.compose.material3.IconButton(onClick = onDismiss) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close"
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showKinds) {
                    Text("Kind", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionKind.entries.forEach { k ->
                            FilterChip(
                                selected = k in draft.kinds,
                                onClick = { draft = draft.copy(kinds = draft.kinds.toggle(k)) },
                                label = { Text(k.label) })
                        }
                    }
                }
                Text("Accounts", style = MaterialTheme.typography.labelLarge)
                SummaryViewModel.SECTION_ORDER.forEach { type ->
                    val ofType = accounts.filter { it.accountType == type }
                    if (ofType.isNotEmpty()) {
                        Text(
                            SummaryViewModel.titleOf(type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ofType.forEach { a ->
                                FilterChip(
                                    selected = a.id in draft.accountIds,
                                    onClick = {
                                        draft =
                                            draft.copy(accountIds = draft.accountIds.toggle(a.id))
                                    },
                                    label = { Text(a.name) })
                            }
                        }
                    }
                }
                Text("Categories", style = MaterialTheme.typography.labelLarge)
                val byParent = categories.groupBy { it.parentId }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.filter { it.parentId == null }.forEach { parent ->
                        val children = byParent[parent.id].orEmpty()
                        FilterChip(
                            selected = parent.id in draft.categoryIds,
                            // Selecting a parent selects its children too, so statistics include the whole group.
                            onClick = {
                                val group = setOf(parent.id) + children.map { it.id }
                                draft =
                                    draft.copy(categoryIds = if (parent.id in draft.categoryIds) draft.categoryIds - group else draft.categoryIds + group)
                            },
                            label = { Text(parent.name) },
                        )
                        children.forEach { c ->
                            FilterChip(
                                selected = c.id in draft.categoryIds,
                                onClick = {
                                    draft = draft.copy(categoryIds = draft.categoryIds.toggle(c.id))
                                },
                                label = { Text("${parent.name} › ${c.name}") })
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onApply(draft) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Apply") }
        }
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
