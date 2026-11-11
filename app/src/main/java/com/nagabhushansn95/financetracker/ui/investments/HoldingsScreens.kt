package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentValuation
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.investments.FdMath
import com.nagabhushansn95.financetracker.data.investments.HoldingEntries
import com.nagabhushansn95.financetracker.data.investments.HoldingEntryKind
import com.nagabhushansn95.financetracker.data.investments.HoldingRow
import com.nagabhushansn95.financetracker.data.investments.HoldingView
import com.nagabhushansn95.financetracker.data.investments.Holdings
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummary
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummaryBuilder
import com.nagabhushansn95.financetracker.data.investments.Position
import com.nagabhushansn95.financetracker.data.prices.AmfiNav
import com.nagabhushansn95.financetracker.data.prices.AmfiScheme
import com.nagabhushansn95.financetracker.data.repository.HoldingsRepository
import com.nagabhushansn95.financetracker.ui.common.AccountField
import com.nagabhushansn95.financetracker.ui.common.BackAndMenu
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.NumberField
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.common.StatCell
import com.nagabhushansn95.financetracker.ui.transaction.InstrumentPickerSheet
import com.nagabhushansn95.financetracker.ui.transaction.typeTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HoldingsState(
    val views: List<HoldingView> = emptyList(),
    val summary: InvestmentSummary? = null,
    val accounts: Map<Long, Account> = emptyMap(),
    /** Accounts an entry can move cash through: everything but provident funds. */
    val cashAccounts: List<Account> = emptyList(),
    val categories: List<InvestmentCategory> = emptyList(),
    /** The latest valuation per instrument. */
    val valuations: Map<Long, InstrumentValuation> = emptyMap(),
    val loaded: Boolean = false,
) {
    fun accountName(id: Long?): String = id?.let { accounts[it]?.name } ?: "—"
    fun view(id: Long): HoldingView? = views.firstOrNull { it.instrument.id == id }

    /** The summary's row for a holding (price, value, XIRR), or a bare one for a holding without entries yet. */
    fun rowFor(v: HoldingView): HoldingRow =
        summary?.rows?.firstOrNull { it.instrument.id == v.instrument.id } ?: HoldingRow(
            instrument = v.instrument,
            position = v.position,
            categoryName = categoryPath(v.instrument) ?: "",
            unitPrice = valuations[v.instrument.id]?.unitPrice,
            priceDate = valuations[v.instrument.id]?.date,
            currentValue = v.position.costHeld,
            xirr = null,
        )

    fun categoryPath(instrument: Instrument): String? {
        val byId = categories.associateBy { it.id }
        val c = instrument.categoryId?.let { byId[it] } ?: return null
        return c.parentId?.let { byId[it]?.name }?.let { "$it › ${c.name}" } ?: c.name
    }

    /** The demat (or bank) account a new buy of [v] is likely paid from. */
    fun defaultTradeAccount(v: HoldingView): Long? =
        v.lastTradeAccount
            ?: views.filter { it.type == v.type }.mapNotNull { it.lastTradeAccount }.lastOrNull()
            ?: if (v.type == InstrumentType.STOCK) cashAccounts.firstOrNull { it.accountType == AccountType.DEMAT && it.currency == v.instrument.currency }?.id
            else cashAccounts.firstOrNull { it.accountType == AccountType.BANK }?.id

    /** The bank a dividend on [v] is likely credited to. */
    fun defaultDividendAccount(v: HoldingView): Long? =
        v.lastDividendAccount
            ?: views.mapNotNull { it.lastDividendAccount }.lastOrNull()
            ?: cashAccounts.firstOrNull { it.accountType == AccountType.BANK }?.id
}

class HoldingsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = HoldingsRepository(db)

    /** One-off outcome of a save or delete, shown in a snackbar. */
    val message = MutableStateFlow<String?>(null)
    fun messageShown() {
        message.value = null
    }

    private val base = combine(
        db.instrumentDao().observeAll(),
        db.transactionDao().observeWithInstrument(),
        db.accountDao().observeAll(),
        db.instrumentDao().observeCategories(),
        db.instrumentDao().observeValuations(),
    ) { instruments, ledger, accounts, categories, valuations ->
        HoldingsState(
            views = HoldingEntries.build(instruments, ledger),
            accounts = accounts.associateBy { it.id },
            cashAccounts = accounts.filter { it.accountType != AccountType.PROVIDENT_FUND && !it.deleted },
            categories = categories,
            valuations = valuations.groupBy { it.instrumentId }
                .mapValues { (_, v) -> v.maxWith(compareBy({ it.date }, { it.id })) },
            loaded = true,
        )
    }

    val state: StateFlow<HoldingsState> =
        combine(
            base,
            InvestmentSummaryBuilder.observe(db)
        ) { s, summary -> s.copy(summary = summary) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HoldingsState())

    fun create(
        name: String, type: InstrumentType, symbol: String?, exchange: String?, isin: String?,
        currency: String, categoryId: Long?, owner: String, notes: String?, onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                repository.add(
                    name,
                    type,
                    symbol,
                    exchange,
                    isin,
                    currency,
                    categoryId,
                    owner,
                    notes
                )
            }
                .onSuccess { onDone(it) }
                .onFailure { message.value = it.message ?: "Could not add it" }
        }
    }

    fun update(instrument: Instrument) {
        viewModelScope.launch {
            runCatching { repository.update(instrument) }
                .onFailure { message.value = it.message ?: "Could not save" }
        }
    }

    fun saveEntry(
        view: HoldingView,
        kind: HoldingEntryKind,
        date: LocalDate,
        units: Double?,
        rate: Double?,
        amount: Double,
        accountId: Long?,
        particulars: String,
        exDate: LocalDate?,
        existing: TransactionEntity?,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.saveEntry(
                    view,
                    kind,
                    date,
                    units,
                    rate,
                    amount,
                    accountId,
                    particulars,
                    exDate,
                    existing
                )
            }.onFailure { message.value = it.message ?: "Could not save the entry" }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteEntry(id) }
                .onSuccess { message.value = "Entry deleted" }
                .onFailure { message.value = it.message ?: "Could not delete the entry" }
        }
    }

    fun delete(instrumentId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.delete(instrumentId) }
                .onSuccess { onDone() }
                .onFailure { message.value = it.message ?: "Could not delete it" }
        }
    }

    fun mergeInto(fromId: Long, intoId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.mergeInto(fromId, intoId) }
                .onSuccess { onDone() }
                .onFailure { message.value = it.message ?: "Could not merge" }
        }
    }

    fun setManualPrice(instrumentId: Long, price: Double) {
        viewModelScope.launch {
            runCatching { repository.setManualPrice(instrumentId, price, LocalDate.now()) }
                .onFailure { message.value = it.message ?: "Could not save the price" }
        }
    }

    fun linkScheme(instrumentId: Long, scheme: AmfiScheme) {
        viewModelScope.launch {
            runCatching { repository.linkScheme(instrumentId, scheme) }
                .onFailure { message.value = it.message ?: "Could not link the scheme" }
        }
    }
}

/**
 * Stocks & ETFs, or Mutual funds: what is held, grouped by category, with sold-out ones folded away behind a toggle
 * in the top bar. The + buys: pick the holding (or add a new one) and the buy sheet opens for it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingsListScreen(
    type: InstrumentType,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: HoldingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showSoldOut by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()
    // Collapsed groups, as their keys joined by newlines (a saveable form of a set).
    var collapsedKeys by rememberSaveable { mutableStateOf("") }
    val collapsed = collapsedKeys.split('\n').filter { it.isNotEmpty() }.toSet()
    fun toggle(key: String) {
        collapsedKeys =
            (if (key in collapsed) collapsed - key else collapsed + key).joinToString("\n")
    }

    var picking by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var buying by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }
    val ofType = state.views.filter { it.type == type }
    val own = ofType.filter { it.own }

    // A search looks through everything of the kind, sold out and family included, by name, symbol or ISIN.
    fun matches(v: HoldingView) = q.isEmpty() || listOfNotNull(
        v.instrument.name,
        v.instrument.shortName,
        v.instrument.symbol,
        v.instrument.isin
    )
        .any { it.contains(q, ignoreCase = true) }

    val held =
        own.filter { (it.position.units > Position.UNIT_EPSILON || it.rows.isEmpty()) && matches(it) }
    val soldOut = own.filter {
        it.position.units <= Position.UNIT_EPSILON && it.rows.isNotEmpty() && matches(it)
    }
    val family = ofType.filter { !it.own && matches(it) }
    val summary = state.summary
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(typeTitle(type)) },
                navigationIcon = { BackAndMenu(onBack) },
                actions = {
                    if (!searching && soldOut.isNotEmpty()) TextButton(onClick = {
                        showSoldOut = !showSoldOut
                    }) {
                        Text(if (showSoldOut) "Hide sold out" else "Show sold out")
                    }
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                        Icon(
                            if (searching) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (searching) "Close search" else "Search"
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { picking = true }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = if (type == InstrumentType.MUTUAL_FUND) "Purchase" else "Buy"
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            if (searching) item {
                val focus = remember { androidx.compose.ui.focus.FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search by name or symbol") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(focus),
                )
            } else item { TypeHeader(listOfNotNull(summary?.byType?.get(type))) }
            if (q.isNotEmpty() && held.isEmpty() && soldOut.isEmpty() && family.isEmpty()) item {
                Text("Nothing matches \"$q\".", modifier = Modifier.padding(16.dp))
            }
            if (state.loaded && ofType.isEmpty()) item {
                Text(
                    "Nothing here yet. Use + to record the first ${if (type == InstrumentType.MUTUAL_FUND) "purchase" else "buy"}.",
                    modifier = Modifier.padding(16.dp)
                )
            }
            // Every group collapses from its header; sold-out holdings keep their categories under their own title.
            fun group(prefix: String, category: String, rows: List<HoldingRow>) {
                val key = "$prefix:$category"
                item(key = key) {
                    GroupHeader(
                        category,
                        rows.size,
                        key in collapsed
                    ) { toggle(key) }
                }
                if (key !in collapsed) items(
                    rows,
                    key = { it.instrument.id }) { h -> HoldingListRow(h) { onOpen(h.instrument.id) } }
            }

            val heldRows = held.map(state::rowFor).sortedBy { it.instrument.name.lowercase() }
            heldRows.groupBy { it.categoryName }.toSortedMap()
                .forEach { (category, rows) -> group("held", category, rows) }
            if ((showSoldOut || q.isNotEmpty()) && soldOut.isNotEmpty()) {
                item(key = "s:sold") { SectionTitle("Sold out") }
                soldOut.map(state::rowFor).sortedByDescending { it.position.lastDate }
                    .groupBy { it.categoryName }.toSortedMap()
                    .forEach { (category, rows) -> group("sold", category, rows) }
            }
            if (family.isNotEmpty()) {
                item(key = "s:family") { SectionTitle("Family (not in the totals)") }
                family.map(state::rowFor).groupBy { it.categoryName }.toSortedMap()
                    .forEach { (category, rows) -> group("family", category, rows) }
            }
        }
    }
    // Sold-out holdings retire after 90 idle days; the picker lists them under "Inactive", and a buy brings them back.
    if (picking) InstrumentPickerSheet(
        instruments = own.map { it.instrument },
        recent = emptyList(),
        optional = false,
        onPick = { id -> picking = false; buying = id },
        onNew = { picking = false; creating = true },
        onDismiss = { picking = false },
        typeOrder = listOf(type),
    )
    if (creating) HoldingSheet(
        existing = null,
        type = type,
        categories = state.categories,
        onSave = { name, symbol, exchange, isin, currency, categoryId, owner, _, notes ->
            creating = false
            viewModel.create(
                name,
                type,
                symbol,
                exchange,
                isin,
                currency,
                categoryId,
                owner,
                notes
            ) { buying = it }
        },
        onDismiss = { creating = false },
    )
    buying?.let { id ->
        val v = state.view(id)
        if (v != null) HoldingEntrySheet(
            view = v,
            accounts = state.cashAccounts,
            defaultTradeAccount = state.defaultTradeAccount(v),
            defaultDividendAccount = state.defaultDividendAccount(v),
            usualCharge = HoldingEntries.usualCharge(state.views),
            initialKind = HoldingEntryKind.BUY,
            existing = null,
            onSave = { kind, date, units, rate, amount, account, particulars, exDate ->
                viewModel.saveEntry(
                    v,
                    kind,
                    date,
                    units,
                    rate,
                    amount,
                    account,
                    particulars,
                    exDate,
                    null
                )
                buying = null
                onOpen(id)
            },
            onDelete = null,
            onDismiss = { buying = null },
        )
    }
}

/** A category's header: name, how many holdings, and a chevron; tapping folds the group away or opens it. */
@Composable
private fun GroupHeader(title: String, count: Int, collapsed: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            if (collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
            contentDescription = if (collapsed) "Expand $title" else "Collapse $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}

/** Name with the value on the right; below, the symbol, units, average cost, price and cost; then the profit line. */
@Composable
private fun HoldingListRow(h: HoldingRow, onClick: () -> Unit) {
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
            h.instrument.symbol?.takeIf { it.isNotBlank() && !it.all(Char::isDigit) },
            units?.let { "${FdMath.plain(it)} units" },
            h.position.averageCost?.let { "avg %.2f".format(it) },
            h.unitPrice?.let { "price %.2f".format(it) }
                ?: if (units != null && units > Position.UNIT_EPSILON) "no price" else null,
            if (h.position.units > Position.UNIT_EPSILON) "invested ${MoneyFormat.amount(h.position.costHeld)}" else null,
            h.instrument.owner.takeIf { it != Instrument.OWNER_SELF }?.let { "owner $it" },
        ).joinToString(" · ").ifEmpty { "No entries yet" }
        Text(
            line,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (h.position.firstDate != null) ProfitLine(h)
    }
    HorizontalDivider()
}

/**
 * One stock or fund: position, price and the ledger. The + records a buy, sell, dividend, bonus, split or charge on
 * its own sheet; press and hold a ledger row to change or delete it. The pencil edits the name, symbol and the rest;
 * the menu has the manual price, the AMFI link, merging and deleting.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HoldingScreen(
    instrumentId: Long,
    onBack: () -> Unit,
    viewModel: HoldingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val v = state.view(instrumentId)
    var adding by remember { mutableStateOf<HoldingEntryKind?>(null) }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    var fabMenu by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var editingHolding by remember { mutableStateOf(false) }
    var pricing by remember { mutableStateOf(false) }
    var linking by remember { mutableStateOf(false) }
    var merging by remember { mutableStateOf(false) }
    var mergeInto by remember { mutableStateOf<Instrument?>(null) }
    var deleting by remember { mutableStateOf(false) }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(v?.instrument?.name ?: "Holding") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (v != null) {
                        IconButton(onClick = { editingHolding = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Set the price by hand…") },
                                onClick = { menu = false; pricing = true })
                            if (v.type == InstrumentType.MUTUAL_FUND) DropdownMenuItem(
                                text = { Text(if (v.instrument.exchange == AmfiNav.EXCHANGE) "Change the AMFI scheme…" else "Link the AMFI scheme…") },
                                onClick = { menu = false; linking = true })
                            DropdownMenuItem(
                                text = { Text("Merge into another…") },
                                onClick = { menu = false; merging = true })
                            DropdownMenuItem(
                                text = { Text("Delete…", color = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; deleting = true })
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (v != null) Box {
                FloatingActionButton(onClick = { fabMenu = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Record an entry")
                }
                DropdownMenu(expanded = fabMenu, onDismissRequest = { fabMenu = false }) {
                    HoldingEntries.kindsFor(v.type).forEach { kind ->
                        DropdownMenuItem(
                            text = { Text(kind.title(v.type)) },
                            onClick = { fabMenu = false; adding = kind })
                    }
                }
            }
        },
    ) { padding ->
        if (v == null) {
            Text(
                if (state.loaded) "Not found." else "Loading…",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
            return@Scaffold
        }
        val position = v.position
        val valuation = state.valuations[v.instrument.id]
        val current = position.currentValue(valuation?.unitPrice, valuation?.totalValue)
        val xirr = Holdings.xirr(position.cashFlows, current, LocalDate.now())
        // Trades through more than one account (yours and a family member's demat, say) name the account per row.
        val tradeAccounts = v.rows.filter {
            val k = HoldingEntries.kindOf(it)
            k == HoldingEntryKind.BUY || k == HoldingEntryKind.SELL
        }.mapNotNull { it.fromAccountId ?: it.toAccountId }.toSet()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            listOfNotNull(
                                v.instrument.symbol?.takeIf { it.isNotBlank() }
                                    ?.let { if (v.type == InstrumentType.MUTUAL_FUND) "AMFI $it" else it },
                                v.instrument.isin,
                                state.categoryPath(v.instrument),
                                v.instrument.owner.takeIf { it != Instrument.OWNER_SELF }
                                    ?.let { "owner $it" },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            MoneyFormat.amount(current),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Row {
                            StatCell("Units", FdMath.plain(position.units))
                            StatCell(
                                "Avg cost",
                                position.averageCost?.let { "%.2f".format(it) } ?: "—")
                            StatCell(
                                "Price",
                                valuation?.unitPrice?.let { "%.2f".format(it) } ?: "none")
                        }
                        Row {
                            StatCell("Invested", MoneyFormat.amount(position.costHeld))
                            StatCell(
                                "Held P/L",
                                MoneyFormat.amount(current - position.costHeld),
                                current - position.costHeld
                            )
                            StatCell(
                                "Realised",
                                MoneyFormat.amount(position.realised),
                                position.realised
                            )
                        }
                        Row {
                            StatCell(
                                if (v.type == InstrumentType.STOCK) "Dividends" else "Income",
                                MoneyFormat.amount(position.income)
                            )
                            StatCell("XIRR", pct(xirr))
                            StatCell(
                                "Price as of",
                                valuation?.let { "${it.date.format(SHORT_DATE)} · ${it.source.name.lowercase()}" }
                                    ?: "—")
                        }
                        if (position.unitsByAccount.size > 1) Text(
                            position.unitsByAccount.entries.sortedByDescending { it.value }
                                .joinToString(" · ") { (account, units) ->
                                    "${FdMath.plain(units)} in ${
                                        state.accountName(
                                            account
                                        )
                                    }"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Total put in ${MoneyFormat.amount(position.totalInvested)}, taken out ${
                                MoneyFormat.amount(
                                    position.totalRedeemed
                                )
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        v.instrument.notes?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    "Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                HorizontalDivider()
            }
            if (v.rows.isEmpty()) item {
                Text(
                    "No entries yet. Use + to record the first ${if (v.type == InstrumentType.MUTUAL_FUND) "purchase" else "buy"}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(v.rows.asReversed(), key = { it.id }) { t ->
                val kind = HoldingEntries.kindOf(t)
                val detail = listOfNotNull(
                    t.date.format(SHORT_DATE),
                    when (kind) {
                        HoldingEntryKind.BUY, HoldingEntryKind.SELL -> t.quantity?.let { q ->
                            t.rate?.let { "${FdMath.plain(q)} @ %.2f".format(it) }
                                ?: "${FdMath.plain(q)} units"
                        }

                        HoldingEntryKind.DIVIDEND -> listOfNotNull(
                            t.exDate?.let { "ex ${it.format(SHORT_DATE)}" },
                            t.rate?.takeIf { it > 0 }?.let { r ->
                                t.quantity?.let { q ->
                                    "${FdMath.plain(q)} × %.2f".format(r)
                                } ?: "%.2f per unit".format(r)
                            },
                        ).joinToString(" · ").ifEmpty { null }

                        HoldingEntryKind.BONUS, HoldingEntryKind.SPLIT -> t.quantity?.let {
                            "+${
                                FdMath.plain(
                                    it
                                )
                            } units"
                        }

                        HoldingEntryKind.CHARGES, null -> null
                    },
                    (t.fromAccountId
                        ?: t.toAccountId)?.takeIf { tradeAccounts.size > 1 && kind != HoldingEntryKind.DIVIDEND }
                        ?.let { state.accountName(it) },
                ).joinToString(" · ")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = {}, onLongClick = { editing = t })
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            HoldingEntries.stripExDate(t.particulars),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (kind?.isUnitsOnly == true) Text(
                        "+${FdMath.plain(t.quantity ?: 0.0)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary,
                    ) else Text(
                        MoneyFormat.amount(t.amount, t.currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (kind?.incoming == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                HorizontalDivider()
            }
        }
    }

    val sheetKind = adding ?: editing?.let { HoldingEntries.kindOf(it) }
    if (v != null && sheetKind != null) HoldingEntrySheet(
        view = v,
        accounts = state.cashAccounts,
        defaultTradeAccount = state.defaultTradeAccount(v),
        defaultDividendAccount = state.defaultDividendAccount(v),
        usualCharge = HoldingEntries.usualCharge(state.views),
        initialKind = sheetKind,
        existing = editing,
        onSave = { kind, date, units, rate, amount, account, particulars, exDate ->
            viewModel.saveEntry(
                v,
                kind,
                date,
                units,
                rate,
                amount,
                account,
                particulars,
                exDate,
                editing
            )
            adding = null; editing = null
        },
        onDelete = editing?.let { row -> { viewModel.deleteEntry(row.id); editing = null } },
        onDismiss = { adding = null; editing = null },
    )
    // A row the sheet cannot describe (a transfer tagged with the holding) is left alone rather than edited blind.
    LaunchedEffect(editing) {
        val row = editing ?: return@LaunchedEffect
        if (HoldingEntries.kindOf(row) == null) {
            editing =
                null; snackbar.showSnackbar("This row is not an entry the sheet can edit; it stays as it is.")
        }
    }
    if (editingHolding && v != null) HoldingSheet(
        existing = v.instrument,
        type = v.type,
        categories = state.categories,
        onSave = { name, symbol, exchange, isin, currency, categoryId, owner, active, notes ->
            editingHolding = false
            viewModel.update(
                v.instrument.copy(
                    name = name,
                    symbol = symbol,
                    exchange = exchange,
                    isin = isin,
                    currency = currency,
                    categoryId = categoryId,
                    owner = owner,
                    active = active,
                    notes = notes,
                )
            )
        },
        onDismiss = { editingHolding = false },
    )
    if (pricing && v != null) {
        var text by remember { mutableStateOf("") }
        val value = text.trim().toDoubleOrNull()
        AlertDialog(
            onDismissRequest = { pricing = false },
            title = { Text("Price per unit today") },
            text = {
                Column {
                    NumberField(text, "Price", { text = it })
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Recorded as a manual valuation for today; the next price update replaces it when it finds a quote.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = value != null && value >= 0, onClick = {
                    viewModel.setManualPrice(v.instrument.id, value!!); pricing = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { pricing = false }) { Text("Cancel") } },
        )
    }
    if (linking && v != null) AmfiSchemeSheet(
        initialQuery = v.instrument.name,
        onPick = { viewModel.linkScheme(v.instrument.id, it); linking = false },
        onDismiss = { linking = false })
    if (merging && v != null) InstrumentPickerSheet(
        instruments = state.views.filter { it.type == v.type && it.instrument.id != v.instrument.id }
            .map { it.instrument },
        recent = emptyList(),
        optional = false,
        onPick = { id -> mergeInto = state.view(id ?: -1)?.instrument; merging = false },
        onNew = { merging = false },
        onDismiss = { merging = false },
        typeOrder = listOf(v.type),
    )
    mergeInto?.let { target ->
        if (v != null) AlertDialog(
            onDismissRequest = { mergeInto = null },
            title = { Text("Merge ${v.instrument.name} into ${target.name}?") },
            text = {
                Text(
                    "All ${v.rows.size} entries of ${v.instrument.name} move to ${target.name}, and ${v.instrument.name} is deleted. " +
                            "For when the same holding ended up twice (an import under a different symbol, say). This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val from = v.instrument.id
                    mergeInto = null
                    viewModel.mergeInto(from, target.id, onBack)
                }) { Text("Merge") }
            },
            dismissButton = { TextButton(onClick = { mergeInto = null }) { Text("Cancel") } },
        )
    }
    if (deleting && v != null) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text("Delete ${v.instrument.name}?") },
        text = {
            Text(
                if (v.rows.isEmpty()) "It has no entries."
                else if (v.rows.size == 1) "Its one entry goes too, with the account balance put back. This cannot be undone."
                else "Its ${v.rows.size} entries go too, with the account balances put back. This cannot be undone."
            )
        },
        confirmButton = {
            TextButton(onClick = { deleting = false; viewModel.delete(v.instrument.id, onBack) }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } },
    )
}

/**
 * Name, symbol and the rest of a stock or fund. A stock names its exchange symbol (NSE unless said otherwise); a
 * fund's AMFI scheme code is best set by "Link the AMFI scheme" on its screen. Retyping a holding as another kind is
 * only under Manage › Instruments.
 */
@Composable
fun HoldingSheet(
    existing: Instrument?,
    type: InstrumentType,
    categories: List<InvestmentCategory>,
    onSave: (
        name: String, symbol: String?, exchange: String?, isin: String?, currency: String,
        categoryId: Long?, owner: String, active: Boolean, notes: String?
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val fund = type == InstrumentType.MUTUAL_FUND
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var symbol by remember { mutableStateOf(existing?.symbol ?: "") }
    var exchange by remember { mutableStateOf(existing?.exchange ?: if (fund) "" else "NSE") }
    var isin by remember { mutableStateOf(existing?.isin ?: "") }
    var currency by remember { mutableStateOf(existing?.currency ?: "INR") }
    var owner by remember { mutableStateOf(existing?.owner ?: Instrument.OWNER_SELF) }
    var active by remember { mutableStateOf(existing?.active ?: true) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var categoryMenu by remember { mutableStateOf(false) }
    val byId = categories.associateBy { it.id }
    val top = categories.firstOrNull {
        it.parentId == null && it.name.equals(
            com.nagabhushansn95.financetracker.data.investments.InvestmentCategories.defaultCategoryName(
                type
            ), ignoreCase = true
        )
    }

    fun categoryLabel(id: Long?) = id?.let { byId[it] }?.let { c ->
        c.parentId?.let { byId[it]?.name }?.let { "$it › ${c.name}" } ?: c.name
    } ?: "Choose a category"
    FormSheet(
        title = if (existing == null) (if (fund) "New mutual fund" else "New stock or ETF") else "Edit",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            placeholder = { Text(if (fund) "Parag Parikh Flexi Cap Fund" else "Nippon India ETF Nifty 50 BeES") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        if (fund) OutlinedTextField(
            value = symbol,
            onValueChange = { symbol = it.trim() },
            label = { Text("AMFI scheme code (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        ) else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it.uppercase().trim() },
                label = { Text("Symbol") },
                placeholder = { Text("NIFTYBEES") },
                singleLine = true,
                modifier = Modifier.weight(2f),
            )
            OutlinedTextField(
                value = exchange,
                onValueChange = { exchange = it.uppercase().trim() },
                label = { Text("Exchange") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = isin,
            onValueChange = { isin = it.uppercase().trim() },
            label = { Text("ISIN (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(80.dp)
            )
            TextButton(onClick = { categoryMenu = true }) { Text(categoryLabel(categoryId)) }
            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                // Only the kind's own group (Stocks › Equity Index ETF, Mutual Funds › Flexi Cap): a fund is never a stock type.
                top?.let { parent ->
                    DropdownMenuItem(
                        text = { Text(parent.name) },
                        onClick = { categoryId = parent.id; categoryMenu = false })
                    categories.filter { it.parentId == parent.id }.forEach { child ->
                        DropdownMenuItem(
                            text = { Text("    ${child.name}") },
                            onClick = { categoryId = child.id; categoryMenu = false })
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it.uppercase().take(3) },
                label = { Text("Currency") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = owner,
                onValueChange = { owner = it },
                label = { Text("Owner") },
                singleLine = true,
                modifier = Modifier.weight(2f)
            )
        }
        if (owner.trim() != Instrument.OWNER_SELF) Text(
            "Owned by someone else: excluded from your totals and statistics. Your own money in a family member's demat account stays owner Self; the account tells them apart.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (existing != null) Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Active",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = active, onCheckedChange = { active = it })
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (fund) "Prices come from AMFI once the scheme is linked (the menu on the fund's screen does it by name). Retyping it as another kind of instrument is under Manage › Instruments."
            else "Prices come from Yahoo Finance by the symbol (NSE, then its RR series, then BSE). A renamed stock keeps its ledger: change the symbol and name here. Retyping it as another kind of instrument is under Manage › Instruments.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            enabled = name.isNotBlank() && (fund || symbol.isNotBlank()),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    name.trim(),
                    symbol.trim().ifBlank { null },
                    exchange.trim().ifBlank { null }
                        ?: if (fund) (if (symbol.isNotBlank()) AmfiNav.EXCHANGE else null) else "NSE",
                    isin.trim().ifBlank { null },
                    currency.trim().ifBlank { "INR" },
                    categoryId,
                    owner.trim().ifBlank { Instrument.OWNER_SELF },
                    active,
                    notes.trim().ifBlank { null },
                )
            },
        ) { Text(if (existing == null) "Add" else "Save") }
    }
}

/**
 * One entry on a stock or fund. A buy or sell takes units and the rate (the amount follows unless typed, for
 * brokerage), the date and the demat or bank account; a sell shows what it realises against the oldest lots of that
 * account. A dividend takes the amount, the bank it landed in and the ex-date. Bonus shares and a split take the
 * ratio and compute the units from what is held. Charges take the amount (the usual DP charge to begin with) and
 * the account. The particulars continue the last entry of the kind unless typed.
 */
@Composable
fun HoldingEntrySheet(
    view: HoldingView,
    accounts: List<Account>,
    defaultTradeAccount: Long?,
    defaultDividendAccount: Long?,
    usualCharge: Double?,
    initialKind: HoldingEntryKind,
    existing: TransactionEntity?,
    onSave: (
        kind: HoldingEntryKind, date: LocalDate, units: Double?, rate: Double?, amount: Double,
        accountId: Long?, particulars: String, exDate: LocalDate?
    ) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val type = view.type
    var kind by remember { mutableStateOf(initialKind) }
    var date by remember { mutableStateOf<LocalDate?>(existing?.date ?: LocalDate.now()) }
    var units by remember { mutableStateOf(existing?.quantity?.let(FdMath::plain) ?: "") }
    var rate by remember { mutableStateOf(existing?.rate?.let(FdMath::plain) ?: "") }
    var amount by remember { mutableStateOf(existing?.amount?.let(FdMath::plain) ?: "") }
    var amountTyped by remember { mutableStateOf(existing != null) }
    var exDate by remember { mutableStateOf(existing?.exDate) }
    var particulars by remember {
        mutableStateOf(existing?.let { HoldingEntries.stripExDate(it.particulars) } ?: "")
    }
    var typed by remember { mutableStateOf(existing != null) }
    val existingRatio = existing?.let { HoldingEntries.ratioOf(it.particulars) }
    var a by remember { mutableStateOf(existingRatio?.first ?: "1") }
    var b by remember { mutableStateOf(existingRatio?.second ?: "1") }
    var accountId by remember(kind) {
        mutableStateOf(
            existing?.let { it.fromAccountId ?: it.toAccountId }
                ?: if (kind == HoldingEntryKind.DIVIDEND) defaultDividendAccount else defaultTradeAccount
        )
    }
    val unitsValue = units.trim().toDoubleOrNull()
    val rateValue = rate.trim().toDoubleOrNull()
    // The amount follows units × rate until typed; charges start with the usual DP charge; a dividend's per-unit
    // rate is optional and gives the unit count when present.
    val amountShown = when {
        amountTyped -> amount
        kind.hasUnits && unitsValue != null && rateValue != null -> FdMath.plain(unitsValue * rateValue)
        kind == HoldingEntryKind.CHARGES && existing == null -> usualCharge?.let(FdMath::plain)
            ?: ""

        else -> amount
    }
    val amountValue = amountShown.trim().toDoubleOrNull()
    // Bonus and split units come from the ratio and what was held on the record date (a bonus recorded late must
    // not count shares bought since); an existing row shows its units as they are.
    val heldNow = view.position.units
    val held = if (kind.isUnitsOnly && date != null && date != LocalDate.now())
        Holdings.position(
            view.instrument.id,
            view.rows.filter { it.date <= date!! && it.id != existing?.id }).units
    else heldNow
    val av = a.trim().toDoubleOrNull()
    val bv = b.trim().toDoubleOrNull()
    val ratioUnits: Double? = when {
        existing != null || av == null || bv == null || av <= 0 || bv <= 0 -> null
        kind == HoldingEntryKind.SPLIT -> HoldingEntries.splitUnits(held, av, bv)
        kind == HoldingEntryKind.BONUS -> HoldingEntries.bonusUnits(held, av, bv)
        else -> null
    }
    val ratio =
        if (kind.isUnitsOnly && av != null && bv != null) "${a.trim()}:${b.trim()}" else null
    val suggested = HoldingEntries.suggestedParticulars(kind, view, ratio)
    val effectiveUnits: Double? = when {
        kind.hasUnits -> unitsValue
        kind.isUnitsOnly -> if (existing != null) unitsValue else ratioUnits
        kind == HoldingEntryKind.DIVIDEND -> rateValue?.takeIf { it > 0 }
            ?.let { amountValue?.div(it) }

        else -> null
    }
    val effectiveRate = if (kind.hasUnits || kind == HoldingEntryKind.DIVIDEND) rateValue else null
    val problem = if (date == null) "Choose the date"
    else HoldingEntries.problem(
        view,
        kind,
        effectiveUnits,
        amountValue ?: 0.0,
        accountId.takeIf { kind.movesCash },
        existing
    )
    val sellPreview =
        if (kind == HoldingEntryKind.SELL && unitsValue != null && amountValue != null && problem == null && date != null)
            Holdings.realisedBySelling(
                view.instrument.id,
                view.rows,
                unitsValue,
                amountValue,
                date!!,
                accountId,
                existing?.id
            ) else null
    FormSheet(
        title = if (existing == null) kind.title(type) else "Edit ${kind.title(type).lowercase()}",
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HoldingEntries.kindsFor(type).forEach { k ->
                FilterChip(
                    selected = kind == k,
                    onClick = { kind = k },
                    label = { Text(k.title(type)) })
            }
        }
        when {
            kind.hasUnits -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(units, "Units", { units = it }, Modifier.weight(1f))
                    NumberField(
                        rate,
                        if (type == InstrumentType.MUTUAL_FUND) "NAV" else "Rate",
                        { rate = it },
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField("Date", date, { date = it }, Modifier.weight(1f))
                    NumberField(
                        amountShown,
                        "Amount",
                        { amount = it; amountTyped = true },
                        Modifier.weight(1f)
                    )
                }
                AccountField(
                    kind.cashLabel!!,
                    accounts,
                    accountId,
                    { accountId = it },
                    Modifier.fillMaxWidth()
                )
            }

            kind == HoldingEntryKind.DIVIDEND -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField("Credited on", date, { date = it }, Modifier.weight(1f))
                    DateField(
                        "Ex-date",
                        exDate,
                        { exDate = it },
                        Modifier.weight(1f),
                        clearable = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        amountShown,
                        "Amount",
                        { amount = it; amountTyped = true },
                        Modifier.weight(1f)
                    )
                    NumberField(rate, "Per unit (optional)", { rate = it }, Modifier.weight(1f))
                }
                AccountField(
                    kind.cashLabel!!,
                    accounts,
                    accountId,
                    { accountId = it },
                    Modifier.fillMaxWidth()
                )
            }

            kind.isUnitsOnly -> {
                if (existing == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        a,
                        if (kind == HoldingEntryKind.SPLIT) "Old" else "Bonus",
                        { a = it },
                        Modifier.weight(1f)
                    )
                    NumberField(
                        b,
                        if (kind == HoldingEntryKind.SPLIT) "New" else "For every",
                        { b = it },
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField("Record date", date, { date = it }, Modifier.weight(1f))
                    if (existing != null) NumberField(
                        units,
                        "Units",
                        { units = it },
                        Modifier.weight(1f)
                    )
                }
            }

            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField("Date", date, { date = it }, Modifier.weight(1f))
                    NumberField(
                        amountShown,
                        "Amount",
                        { amount = it; amountTyped = true },
                        Modifier.weight(1f)
                    )
                }
                AccountField(
                    kind.cashLabel!!,
                    accounts,
                    accountId,
                    { accountId = it },
                    Modifier.fillMaxWidth()
                )
            }
        }
        OutlinedTextField(
            value = if (typed) particulars else suggested,
            onValueChange = { particulars = it; typed = true },
            label = { Text("Particulars") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            when (kind) {
                HoldingEntryKind.BUY -> "Held ${FdMath.plain(heldNow)} units. The amount follows units × ${if (type == InstrumentType.MUTUAL_FUND) "NAV" else "rate"} unless typed (brokerage, stamp duty)."
                HoldingEntryKind.SELL -> sellPreview?.let {
                    "Held ${FdMath.plain(heldNow)} units. Realises ${MoneyFormat.amount(it)} against the oldest lots" +
                            (accountId?.let { id -> " bought through ${accounts.firstOrNull { it.id == id }?.name ?: "this account"}" }
                                ?: "") + "."
                }
                    ?: "Held ${FdMath.plain(heldNow)} units. The oldest lots go first (FIFO), as in Zerodha's P&L and the tax rules."

                HoldingEntryKind.DIVIDEND -> "Counts as income on this holding, not as a sale. The ex-date is for the record."
                HoldingEntryKind.BONUS -> "Held ${FdMath.plain(held)} units on the record date" + (ratioUnits?.let {
                    " → +${
                        FdMath.plain(
                            it
                        )
                    } at no cost; the average cost falls, the invested amount stays."
                } ?: ".")

                HoldingEntryKind.SPLIT -> "Held ${FdMath.plain(held)} units on the record date" + (ratioUnits?.let {
                    " → +${
                        FdMath.plain(
                            it
                        )
                    }; the same cost now covers ${FdMath.plain(held + it)} units."
                } ?: ".")

                HoldingEntryKind.CHARGES -> "An expense under ${HoldingEntries.CHARGES_CATEGORY}, counted against this holding's realised result."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            enabled = problem == null,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    kind, date!!, effectiveUnits, effectiveRate, amountValue ?: 0.0,
                    accountId.takeIf { kind.movesCash }, if (typed) particulars else suggested,
                    exDate.takeIf { kind == HoldingEntryKind.DIVIDEND },
                )
            },
        ) { Text(if (existing == null) "Record" else "Save") }
        if (onDelete != null) OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete entry", color = MaterialTheme.colorScheme.error)
        }
    }
}
