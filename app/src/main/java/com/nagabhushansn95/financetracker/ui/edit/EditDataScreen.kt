package com.nagabhushansn95.financetracker.ui.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nagabhushansn95.financetracker.data.db.entity.FdTemplate
import com.nagabhushansn95.financetracker.data.investments.InvestmentOrder
import com.nagabhushansn95.financetracker.ui.investments.FdTemplateSheet
import com.nagabhushansn95.financetracker.ui.investments.summary
import com.nagabhushansn95.financetracker.ui.transaction.typeTitle
import kotlinx.coroutines.launch

/** Tab titles, in order; the drawer's Manage sub-entries use the same list. */
val MANAGE_TABS =
    listOf("Accounts", "Categories", "Instruments", "Investment categories", "Templates")
private val TABS = MANAGE_TABS

/**
 * Manage: accounts, categories, instruments, investment categories and templates with add / edit / delete /
 * restore and drag-handle reorder. The tabs are swiped as well as tapped.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditDataScreen(
    snackbarHostState: SnackbarHostState,
    initialTab: Int = 0,
    viewModel: EditDataViewModel = viewModel()
) {
    val pager =
        rememberPagerState(initialPage = initialTab.coerceIn(0, TABS.size - 1)) { TABS.size }
    val scope = rememberCoroutineScope()
    // The drawer's sub-entries land on a tab; a repeat visit with another tab scrolls there.
    LaunchedEffect(initialTab) { pager.scrollToPage(initialTab.coerceIn(0, TABS.size - 1)) }
    val message by viewModel.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) { message?.let { snackbarHostState.showSnackbar(it); viewModel.messageShown() } }

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.ScrollableTabRow(
            selectedTabIndex = pager.currentPage,
            edgePadding = 0.dp
        ) {
            TABS.forEachIndexed { i, title ->
                Tab(
                    selected = pager.currentPage == i,
                    onClick = { scope.launch { pager.animateScrollToPage(i) } },
                    text = { Text(title) })
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> AccountsTab(viewModel)
                1 -> CategoriesTab(viewModel)
                2 -> InstrumentsTab(viewModel)
                3 -> InvestmentCategoriesTab(viewModel)
                else -> TemplatesTab(viewModel)
            }
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
    val investmentCategories by viewModel.investmentCategories.collectAsStateWithLifecycle()
    var showInactive by rememberSaveable { mutableStateOf(false) }
    var collapsed by rememberSaveable { mutableStateOf(setOf<String>()) }
    var editing by remember { mutableStateOf<Instrument?>(null) }
    var adding by remember { mutableStateOf(false) }
    var txCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(editing) {
        txCount = editing?.let { viewModel.instrumentTransactionCount(it.id) }
    }

    // Fixed deposits are managed entirely from Investments › Fixed deposits; nothing about them lives here.
    val managed = all.filter { it.instrumentType != InstrumentType.FIXED_DEPOSIT }
    val active = managed.filter { it.active }
    val inactive = managed.filter { !it.active }
    val sections =
        InvestmentOrder.types(InvestmentOrder.names(investmentCategories)).mapNotNull { type ->
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
                collapsed = collapsed,
                onToggleSection = { key ->
                    collapsed = if (key in collapsed) collapsed - key else collapsed + key
                },
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
            categories = investmentCategories,
            onSave = { viewModel.saveInstrument(it); editing = null; adding = false },
            onDismiss = { editing = null; adding = false },
        )
    }
}

// ---------------------------------------------------------------------------------------------------- templates

/**
 * Saved templates grouped by kind, then the fixed deposit templates. Press and hold for Edit / Hide / Delete; + adds
 * one of either kind, as do the editor's tick box and the deposit form's "Save these details as a template".
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TemplatesTab(viewModel: EditDataViewModel) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val fdTemplates by viewModel.fdTemplates.collectAsStateWithLifecycle()
    val fdSuggestions by viewModel.fdSuggestions.collectAsStateWithLifecycle()
    var fdMenuFor by remember { mutableStateOf<FdTemplate?>(null) }
    var fdEditing by remember { mutableStateOf<FdTemplate?>(null) }
    var fdAdding by remember { mutableStateOf(false) }
    var fdConfirmDelete by remember { mutableStateOf<FdTemplate?>(null) }
    var addMenu by remember { mutableStateOf(false) }
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val instruments by viewModel.instruments.collectAsStateWithLifecycle()
    val lookups = remember(
        accounts,
        categories,
        instruments
    ) {
        com.nagabhushansn95.financetracker.ui.transaction.LedgerLookups(
            accounts,
            categories,
            instruments
        )
    }
    var menuFor by remember {
        mutableStateOf<com.nagabhushansn95.financetracker.data.db.entity.Template?>(
            null
        )
    }
    var confirmDelete by remember {
        mutableStateOf<com.nagabhushansn95.financetracker.data.db.entity.Template?>(
            null
        )
    }
    var editing by remember {
        mutableStateOf<com.nagabhushansn95.financetracker.data.db.entity.Template?>(null)
    }
    var adding by remember { mutableStateOf(false) }
    // Kinds folded away (there are many expense templates) and the particulars search.
    var collapsed by rememberSaveable { mutableStateOf(setOf<String>()) }
    var query by rememberSaveable { mutableStateOf("") }

    fun describe(t: com.nagabhushansn95.financetracker.data.db.entity.Template): String =
        listOfNotNull(
            t.fromAccountId?.let { "from " + lookups.accountName(it) },
            t.toAccountId?.let { "to " + lookups.accountName(it) },
            lookups.categoryPath(t.categoryId),
            lookups.instrumentName(t.instrumentId),
            t.rate?.let { r ->
                t.quantity?.let { q ->
                    "${MoneyFormat.amount(r)} × ${
                        com.nagabhushansn95.financetracker.data.investments.FdMath.plain(
                            q
                        )
                    }"
                }
                    ?: MoneyFormat.amount(r)
            },
        ).joinToString(" · ")

    val matching =
        if (query.isBlank()) templates else templates.filter {
            it.particulars.contains(
                query.trim(),
                ignoreCase = true
            )
        }
    val matchingFd =
        if (query.isBlank()) fdTemplates else fdTemplates.filter {
            it.name.contains(
                query.trim(),
                ignoreCase = true
            )
        }
    // One list holds both kinds; deposit templates carry negative ids so the keys never collide.
    val sections: List<Section<Any>> =
        com.nagabhushansn95.financetracker.data.db.entity.TransactionKind.entries.mapNotNull { kind ->
            val ofKind = matching.filter { it.kind == kind }
            if (ofKind.isEmpty()) null else Section<Any>(
                kind.name,
                kind.name.lowercase().replaceFirstChar { c -> c.uppercase() },
                ofKind,
                reorderable = false
            )
        } + if (matchingFd.isEmpty()) emptyList() else listOf(
            Section<Any>(
                "FD",
                "Fixed deposits",
                matchingFd,
                reorderable = false
            )
        )
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search particulars") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (query.isEmpty()) null else {
                    {
                        androidx.compose.material3.IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (templates.isEmpty() && fdTemplates.isEmpty()) Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No templates. Tap + here, tick 'Save as template' when adding a transaction, or save a deposit's details from New fixed deposit.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(32.dp)
                )
            }
            else ReorderableSections(
                sections = sections,
                id = { if (it is FdTemplate) -it.id else (it as com.nagabhushansn95.financetracker.data.db.entity.Template).id },
                onReordered = { _, _ -> },
                // While searching every section stays open, so a match is never hidden.
                collapsed = if (query.isBlank()) collapsed else emptySet(),
                onToggleSection = { key ->
                    collapsed = if (key in collapsed) collapsed - key else collapsed + key
                },
                row = { item, _ ->
                    if (item is FdTemplate) EditRow(
                        item.name,
                        subtitle = item.summary().ifEmpty { null },
                        onLongPress = { fdMenuFor = item },
                    ) {}
                    else (item as com.nagabhushansn95.financetracker.data.db.entity.Template).let { template ->
                        EditRow(
                            template.particulars + if (template.hidden) "  (hidden)" else "",
                            subtitle = describe(template).ifEmpty { null },
                            muted = template.hidden,
                            onLongPress = { menuFor = template },
                        ) {}
                    }
                },
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(onClick = { addMenu = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add template")
            }
            DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Transaction template") },
                    onClick = { addMenu = false; adding = true })
                DropdownMenuItem(
                    text = { Text("Fixed deposit template") },
                    onClick = { addMenu = false; fdAdding = true })
            }
        }
    }

    if (fdEditing != null || fdAdding) {
        FdTemplateSheet(
            existing = fdEditing,
            suggestions = fdSuggestions,
            accounts = accounts.filter { !it.deleted && it.isCashAccount },
            onSave = { viewModel.saveFdTemplate(it); fdEditing = null; fdAdding = false },
            onDelete = fdEditing?.let { t -> { fdEditing = null; fdConfirmDelete = t } },
            onDismiss = { fdEditing = null; fdAdding = false },
            otherNames = fdTemplates.filter { it.id != fdEditing?.id }.map { it.name },
        )
    }
    fdMenuFor?.let { t ->
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { fdMenuFor = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    t.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                androidx.compose.material3.ListItem(
                    headlineContent = { Text("Edit") },
                    modifier = Modifier.combinedClickable(onClick = {
                        fdEditing = t; fdMenuFor = null
                    }),
                )
                androidx.compose.material3.ListItem(
                    headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.combinedClickable(onClick = {
                        fdMenuFor = null; fdConfirmDelete = t
                    }),
                )
            }
        }
    }
    fdConfirmDelete?.let { t ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { fdConfirmDelete = null },
            title = { Text("Delete this deposit template?") },
            text = { Text(t.name) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFdTemplate(t.id); fdConfirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { fdConfirmDelete = null }) { Text("Cancel") } },
        )
    }

    if (editing != null || adding) {
        TemplateSheet(
            existing = editing,
            accounts = accounts.filter { !it.deleted },
            categories = categories.filter { !it.deleted },
            instruments = instruments.filter { it.active },
            onSave = { viewModel.saveTemplate(it); editing = null; adding = false },
            onDelete = editing?.let { t -> { editing = null; confirmDelete = t } },
            onDismiss = { editing = null; adding = false },
        )
    }

    menuFor?.let { t ->
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { menuFor = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    t.particulars,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                androidx.compose.material3.ListItem(
                    headlineContent = { Text("Edit") },
                    modifier = Modifier.combinedClickable(onClick = {
                        editing = t; menuFor = null
                    }),
                )
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(if (t.hidden) "Unhide" else "Hide from suggestions") },
                    modifier = Modifier.combinedClickable(onClick = {
                        viewModel.setTemplateHidden(
                            t,
                            !t.hidden
                        ); menuFor = null
                    }),
                )
                androidx.compose.material3.ListItem(
                    headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.combinedClickable(onClick = {
                        menuFor = null; confirmDelete = t
                    }),
                )
            }
        }
    }
    confirmDelete?.let { t ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this template?") },
            text = { Text(t.particulars) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTemplate(t); confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

// ---------------------------------------------------------------------------------------------------- investment categories

/** The Investment Summary's category tree (groups and their members) and the risk levels, all reorderable. */
@Composable
private fun InvestmentCategoriesTab(viewModel: EditDataViewModel) {
    val all by viewModel.investmentCategories.collectAsStateWithLifecycle()
    val riskLevels by viewModel.riskLevels.collectAsStateWithLifecycle()
    var editing by remember {
        mutableStateOf<com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory?>(
            null
        )
    }
    var editingRisk by remember {
        mutableStateOf<com.nagabhushansn95.financetracker.data.db.entity.RiskLevel?>(
            null
        )
    }
    var adding by remember { mutableStateOf<String?>(null) }   // "category" or "risk"
    var addMenu by remember { mutableStateOf(false) }
    val parents = all.filter { it.parentId == null }
    val riskName = riskLevels.associate { it.id to it.name }
    val sections: List<Section<Any>> = listOf(
        Section<Any>("risk", "Risk levels (least to most risky)", riskLevels),
        Section<Any>("top", "Groups", parents)
    ) +
            parents.map { p ->
                Section<Any>(
                    "ic${p.id}",
                    p.name,
                    all.filter { it.parentId == p.id })
            }
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Text(
                "The rows of the Investment Summary. Every instrument belongs to one of these (chosen in the instrument's sheet) " +
                        "and the summary adds its value up by group and by risk level. Here you add, rename, group and order the " +
                        "categories and name the risk levels. Which risk level a category has, and its allocation targets, " +
                        "are set under Investments › Categories & targets. Press and hold a row to edit it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ReorderableSections(
                sections = sections,
                id = {
                    (it as? com.nagabhushansn95.financetracker.data.db.entity.RiskLevel)?.id
                        ?: (it as com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory).id
                },
                onReordered = { key, ids ->
                    if (key == "risk") viewModel.reorderRiskLevels(ids) else viewModel.reorderInvestmentCategories(
                        ids
                    )
                },
                row = { item, handle ->
                    when (item) {
                        is com.nagabhushansn95.financetracker.data.db.entity.RiskLevel -> EditRow(
                            item.name,
                            subtitle = null,
                            onLongPress = { editingRisk = item },
                            handle = handle
                        )

                        is com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory -> EditRow(
                            item.name,
                            subtitle = listOfNotNull(
                                item.riskLevelId?.let { riskName[it] } ?: "no risk level",
                                item.targetGoalPct?.let { "goal %.0f%%".format(it * 100) },
                                item.targetWealthPct?.let { "wealth %.0f%%".format(it * 100) },
                                item.tolerancePct?.let { "±%.0f%%".format(it * 100) },
                            ).joinToString(" · "),
                            onLongPress = { editing = item },
                            handle = handle,
                        )
                    }
                },
            )
        }
        Box(modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)) {
            FloatingActionButton(onClick = { addMenu = true }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add"
                )
            }
            androidx.compose.material3.DropdownMenu(
                expanded = addMenu,
                onDismissRequest = { addMenu = false }) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Investment category") },
                    onClick = { adding = "category"; addMenu = false })
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Risk level") },
                    onClick = { adding = "risk"; addMenu = false })
            }
        }
    }
    if (editing != null || adding == "category") {
        InvestmentCategorySheet(
            existing = editing, parents = parents, riskLevels = riskLevels,
            onSave = { viewModel.saveInvestmentCategory(it); editing = null; adding = null },
            onDelete = editing?.let { c ->
                {
                    viewModel.deleteInvestmentCategory(c.id); editing = null
                }
            },
            onDismiss = { editing = null; adding = null },
        )
    }
    if (editingRisk != null || adding == "risk") {
        val level = editingRisk
        RiskLevelSheet(
            existing = level,
            onSave = { name ->
                if (level == null) viewModel.addRiskLevel(name) else viewModel.renameRiskLevel(
                    level,
                    name
                ); editingRisk = null; adding = null
            },
            onDelete = level?.let { r -> { viewModel.deleteRiskLevel(r.id); editingRisk = null } },
            onDismiss = { editingRisk = null; adding = null },
        )
    }
}
