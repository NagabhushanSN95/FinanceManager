package com.nagabhushansn95.financetracker.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.Listed
import com.nagabhushansn95.financetracker.ui.common.FormSheet
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
 * Generic bottom sheet: search box, "Recent" section, then named sections. Rows are tapped to pick. A [FormSheet],
 * so a scroll up the list does not close it.
 */
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

    FormSheet(title = null, onDismiss = onDismiss, scrollable = false) {
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
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
    instruments: List<Listed>,
    recent: List<Long>,
    optional: Boolean,
    onPick: (Long?) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
    typeOrder: List<InstrumentType> = com.nagabhushansn95.financetracker.data.investments.InvestmentOrder.DEFAULT_TYPES,
) {
    val byId = instruments.associateBy { it.id }
    fun row(i: Listed) = PickRow(
        i.id,
        i.name,
        subtitle = listOfNotNull(i.security.symbol, i.security.isin).joinToString(" · ")
            .ifEmpty { null })

    val active = instruments.filter { it.instrument.active }
    val inactive = instruments.filter { !it.instrument.active }
    PickerSheet(
        title = "Instrument",
        sections = typeOrder.mapNotNull { type ->
            val ofType = active.filter { it.instrument.instrumentType == type }
            if (ofType.isEmpty()) null else typeTitle(type) to ofType.map(::row)
        } + if (inactive.isEmpty()) emptyList() else listOf(
            "Inactive (sold out or retired)" to inactive.map(
                ::row
            )
        ),
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
    InstrumentType.VEHICLE -> "Vehicles"
    InstrumentType.OTHER -> "Others"
}

