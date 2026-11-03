package com.nagabhushansn95.financetracker.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.summary.SummaryViewModel

/** One row of a picker sheet. */
private data class PickRow(
    val id: Long,
    val title: String,
    val trailing: String? = null,
    val subtitle: String? = null
)

/**
 * Generic bottom sheet: search box, "Recent" section, then named sections. Rows are tapped to pick.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(
    title: String,
    sections: List<Pair<String, List<PickRow>>>,
    recent: List<PickRow>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
    clearable: Boolean = false,
    extraAction: Pair<String, () -> Unit>? = null,
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val filtered = if (q.isEmpty()) sections
    else sections.map { (name, rows) ->
        name to rows.filter {
            it.title.contains(
                q,
                ignoreCase = true
            ) || it.subtitle?.contains(q, ignoreCase = true) == true
        }
    }
        .filter { it.second.isNotEmpty() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (clearable) TextButton(onClick = { onPick(-1) }) { Text("None") }
                extraAction?.let { (label, action) -> TextButton(onClick = action) { Text(label) } }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)) {
                if (q.isEmpty() && recent.isNotEmpty()) {
                    item { SectionHeader("Recent") }
                    items(
                        recent,
                        key = { "r${it.id}" }) { row -> PickRowView(row) { onPick(row.id) } }
                }
                filtered.forEach { (name, rows) ->
                    item { SectionHeader(name) }
                    items(rows, key = { it.id }) { row -> PickRowView(row) { onPick(row.id) } }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
    HorizontalDivider()
}

@Composable
private fun PickRowView(row: PickRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.bodyLarge)
            row.subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        row.trailing?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

/** Accounts grouped by type with balances; [exclude] hides the other side of a transfer. */
@Composable
fun AccountPickerSheet(
    title: String,
    accounts: List<Account>,
    recent: List<Long>,
    exclude: Long? = null,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val visible = accounts.filter { it.id != exclude }
    val byId = visible.associateBy { it.id }
    fun row(a: Account) = PickRow(a.id, a.name, MoneyFormat.amount(a.balance, a.currency))
    PickerSheet(
        title = title,
        sections = SummaryViewModel.SECTION_ORDER.mapNotNull { type ->
            val ofType = visible.filter { it.accountType == type }
            if (ofType.isEmpty()) null else SummaryViewModel.titleOf(type) to ofType.map(::row)
        },
        recent = recent.mapNotNull { byId[it] }.map(::row),
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

/** Categories for one scope, children shown under their parent as "Parent › Child". */
@Composable
fun CategoryPickerSheet(
    scope: CategoryScope,
    categories: List<Category>,
    recent: List<Long>,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val applicable =
        categories.filter { it.appliesTo == scope || it.appliesTo == CategoryScope.BOTH }
    val byId = applicable.associateBy { it.id }
    val parents = applicable.filter { it.parentId == null }
    val rows = parents.flatMap { p ->
        listOf(PickRow(p.id, p.name)) + applicable.filter { it.parentId == p.id }
            .map { PickRow(it.id, "${p.name} › ${it.name}") }
    }
    PickerSheet(
        title = if (scope == CategoryScope.EXPENSE) "Expense category" else "Income category",
        sections = listOf("All" to rows),
        recent = recent.mapNotNull { byId[it] }.map { c ->
            PickRow(c.id, c.parentId?.let { byId[it]?.name }?.let { "$it › ${c.name}" } ?: c.name)
        },
        onPick = { if (it == -1L) onPick(null) else onPick(it) },
        onDismiss = onDismiss,
        clearable = scope == CategoryScope.INCOME,
    )
}

/** Instruments grouped by type, with a "+ New" entry. */
@Composable
fun InstrumentPickerSheet(
    instruments: List<Instrument>,
    recent: List<Long>,
    optional: Boolean,
    onPick: (Long?) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val byId = instruments.associateBy { it.id }
    fun row(i: Instrument) = PickRow(
        i.id,
        i.name,
        subtitle = listOfNotNull(i.symbol, i.isin).joinToString(" · ").ifEmpty { null })
    PickerSheet(
        title = "Instrument",
        sections = InstrumentType.entries.mapNotNull { type ->
            val ofType = instruments.filter { it.instrumentType == type }
            if (ofType.isEmpty()) null else typeTitle(type) to ofType.map(::row)
        },
        recent = recent.mapNotNull { byId[it] }.map(::row),
        onPick = { if (it == -1L) onPick(null) else onPick(it) },
        onDismiss = onDismiss,
        clearable = optional,
        extraAction = "+ New" to onNew,
    )
}

fun typeTitle(type: InstrumentType): String = when (type) {
    InstrumentType.STOCK -> "Stocks & ETFs"
    InstrumentType.MUTUAL_FUND -> "Mutual funds"
    InstrumentType.FIXED_DEPOSIT -> "Fixed deposits"
    InstrumentType.INSURANCE -> "Insurance"
    InstrumentType.OTHER -> "Others"
}

/** Minimal new-instrument sheet: name, type, optional symbol. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInstrumentSheet(
    onCreate: (name: String, type: InstrumentType, symbol: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(InstrumentType.STOCK) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("New instrument", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InstrumentType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(typeTitle(t)) })
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (type == InstrumentType.STOCK || type == InstrumentType.MUTUAL_FUND) {
                OutlinedTextField(
                    value = symbol, onValueChange = { symbol = it },
                    label = { Text(if (type == InstrumentType.STOCK) "NSE symbol" else "Scheme code (optional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                "ISIN, category and the rest can be filled in under Investments.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                enabled = name.isNotBlank(),
                onClick = { onCreate(name, type, symbol) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create") }
        }
    }
}
