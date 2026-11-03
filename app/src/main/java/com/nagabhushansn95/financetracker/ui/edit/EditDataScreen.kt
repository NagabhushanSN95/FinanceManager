package com.nagabhushansn95.financetracker.ui.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.summary.SummaryViewModel
import com.nagabhushansn95.financetracker.ui.transaction.typeTitle

private val TABS = listOf("Accounts", "Categories", "Instruments")

/** Manage: accounts, categories and instruments with add / edit / delete / restore and drag-handle reorder. */
@Composable
fun EditDataScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: EditDataViewModel = viewModel()
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val message by viewModel.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) { message?.let { snackbarHostState.showSnackbar(it); viewModel.messageShown() } }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            TABS.forEachIndexed { i, title ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text(title) })
            }
        }
        when (tab) {
            0 -> AccountsTab(viewModel)
            1 -> CategoriesTab(viewModel)
            else -> InstrumentsTab(viewModel)
        }
    }
}

@Composable
private fun ShowDeletedRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onChange)
            .padding(horizontal = 16.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = null)
    }
}

// ---------------------------------------------------------------------------------------------------- accounts

@Composable
private fun AccountsTab(viewModel: EditDataViewModel) {
    val all by viewModel.accounts.collectAsStateWithLifecycle()
    var showDeleted by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Account?>(null) }
    var adding by remember { mutableStateOf<AccountType?>(null) }
    var txCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(editing) { txCount = editing?.let { viewModel.accountTransactionCount(it.id) } }

    val visible = all.filter { !it.deleted }
    val deleted = all.filter { it.deleted }
    val sections = SummaryViewModel.SECTION_ORDER.mapNotNull { type ->
        val ofType = visible.filter { it.accountType == type }
        if (ofType.isEmpty()) null else Section(type.name, SummaryViewModel.titleOf(type), ofType)
    } + if (showDeleted && deleted.isNotEmpty()) listOf(
        Section(
            "deleted",
            "Deleted",
            deleted,
            reorderable = false
        )
    ) else emptyList()

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            ShowDeletedRow("Show deleted accounts (${deleted.size})", showDeleted) {
                showDeleted = it
            }
            ReorderableSections(
                sections = sections,
                id = { it.id },
                onReordered = { _, ids -> viewModel.reorderAccounts(ids) },
                row = { account, handle ->
                    if (account.deleted) {
                        EditRow(
                            account.name,
                            subtitle = SummaryViewModel.titleOf(account.accountType),
                            muted = true,
                            onLongPress = {}) {
                            TextButton(onClick = { viewModel.restoreAccount(account.id) }) { Text("Restore") }
                        }
                    } else {
                        EditRow(
                            account.name,
                            subtitle = account.accountNumber?.let { "A/c $it" },
                            trailing = MoneyFormat.amount(account.balance, account.currency),
                            onLongPress = { editing = account },
                            handle = handle,
                        )
                    }
                },
            )
        }
        FloatingActionButton(
            onClick = { adding = AccountType.WALLET },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add account")
        }
    }

    if (editing != null || adding != null) {
        AccountSheet(
            existing = editing,
            initialType = adding ?: AccountType.WALLET,
            transactionCount = txCount,
            onSave = { viewModel.saveAccount(it); editing = null; adding = null },
            onDelete = editing?.let { a -> { viewModel.deleteAccount(a.id); editing = null } },
            onDismiss = { editing = null; adding = null },
        )
    }
}

// ---------------------------------------------------------------------------------------------------- categories

@Composable
private fun CategoriesTab(viewModel: EditDataViewModel) {
    val all by viewModel.categories.collectAsStateWithLifecycle()
    var showDeleted by rememberSaveable { mutableStateOf(false) }
    var openParent by rememberSaveable { mutableStateOf<Long?>(null) }   // null = top level, else that parent's children
    var editing by remember { mutableStateOf<Category?>(null) }
    var adding by remember { mutableStateOf(false) }
    var txCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(editing) { txCount = editing?.let { viewModel.categoryTransactionCount(it.id) } }

    val visible = all.filter { !it.deleted }
    val deleted = all.filter { it.deleted }
    val parent = openParent?.let { id -> all.firstOrNull { it.id == id } }
    val childCount = visible.groupingBy { it.parentId }.eachCount()

    val sections: List<Section<Category>> = if (parent == null) {
        CategoryScope.entries.mapNotNull { scope ->
            val ofScope = visible.filter { it.parentId == null && it.appliesTo == scope }
            if (ofScope.isEmpty()) null else Section(scope.name, scopeTitle(scope), ofScope)
        } + if (showDeleted && deleted.isNotEmpty()) listOf(
            Section(
                "deleted",
                "Deleted",
                deleted,
                reorderable = false
            )
        ) else emptyList()
    } else {
        listOf(
            Section(
                "children",
                "Sub-categories of ${parent.name}",
                visible.filter { it.parentId == parent.id })
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            if (parent == null) ShowDeletedRow(
                "Show deleted categories (${deleted.size})",
                showDeleted
            ) { showDeleted = it }
            else TextButton(onClick = { openParent = null }) { Text("← All categories") }
            ReorderableSections(
                sections = sections,
                id = { it.id },
                onReordered = { _, ids -> viewModel.reorderCategories(ids) },
                row = { category, handle ->
                    if (category.deleted) {
                        EditRow(
                            category.name,
                            subtitle = scopeTitle(category.appliesTo),
                            muted = true,
                            onLongPress = {}) {
                            TextButton(onClick = { viewModel.restoreCategory(category.id) }) {
                                Text(
                                    "Restore"
                                )
                            }
                        }
                    } else {
                        val children = childCount[category.id] ?: 0
                        EditRow(
                            category.name,
                            subtitle = if (parent == null && children > 0) "$children sub-categor${if (children == 1) "y" else "ies"}" else null,
                            onLongPress = { editing = category },
                        ) {
                            if (parent == null) TextButton(onClick = {
                                openParent = category.id
                            }) { Text("Sub") }
                            handle()
                        }
                    }
                },
            )
        }
        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add category")
        }
    }

    if (editing != null || adding) {
        CategorySheet(
            existing = editing,
            parents = visible.filter { it.parentId == null },
            initialScope = parent?.appliesTo ?: CategoryScope.EXPENSE,
            initialParentId = parent?.id,
            transactionCount = txCount,
            onSave = { viewModel.saveCategory(it); editing = null; adding = false },
            onDelete = editing?.let { c -> { viewModel.deleteCategory(c.id); editing = null } },
            onDismiss = { editing = null; adding = false },
        )
    }
}

// ---------------------------------------------------------------------------------------------------- instruments

@Composable
private fun InstrumentsTab(viewModel: EditDataViewModel) {
    val all by viewModel.instruments.collectAsStateWithLifecycle()
    var showInactive by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Instrument?>(null) }
    var adding by remember { mutableStateOf(false) }
    var txCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(editing) {
        txCount = editing?.let { viewModel.instrumentTransactionCount(it.id) }
    }

    val active = all.filter { it.active }
    val inactive = all.filter { !it.active }
    val sections = InstrumentType.entries.mapNotNull { type ->
        val ofType = active.filter { it.instrumentType == type }
        if (ofType.isEmpty()) null else Section(type.name, typeTitle(type), ofType)
    } + if (showInactive && inactive.isNotEmpty()) listOf(
        Section(
            "inactive",
            "Inactive",
            inactive,
            reorderable = false
        )
    ) else emptyList()

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            ShowDeletedRow(
                "Show inactive instruments (${inactive.size})",
                showInactive
            ) { showInactive = it }
            ReorderableSections(
                sections = sections,
                id = { it.id },
                onReordered = { _, ids -> viewModel.reorderInstruments(ids) },
                row = { instrument, handle ->
                    EditRow(
                        instrument.name,
                        subtitle = listOfNotNull(
                            instrument.symbol,
                            instrument.isin,
                            instrument.owner.takeIf { it != Instrument.OWNER_SELF }).joinToString(" · ")
                            .ifEmpty { null },
                        muted = !instrument.active,
                        onLongPress = { editing = instrument },
                    ) {
                        if (!instrument.active) TextButton(onClick = {
                            viewModel.setInstrumentActive(
                                instrument.id,
                                true
                            )
                        }) { Text("Reactivate") }
                        else handle()
                    }
                },
            )
        }
        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add instrument")
        }
    }

    if (editing != null || adding) {
        InstrumentSheet(
            existing = editing,
            initialType = InstrumentType.STOCK,
            transactionCount = txCount,
            onSave = { viewModel.saveInstrument(it); editing = null; adding = false },
            onDismiss = { editing = null; adding = false },
        )
    }
}
