package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import com.nagabhushansn95.financetracker.data.db.entity.HoldingEvent
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.Listed
import com.nagabhushansn95.financetracker.data.db.entity.PendingDividend
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.FdMath
import com.nagabhushansn95.financetracker.data.investments.HoldingEntries
import com.nagabhushansn95.financetracker.data.investments.HoldingEntryKind
import com.nagabhushansn95.financetracker.data.investments.HoldingLine
import com.nagabhushansn95.financetracker.data.investments.HoldingRow
import com.nagabhushansn95.financetracker.data.investments.HoldingView
import com.nagabhushansn95.financetracker.data.investments.Holdings
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummary
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummaryBuilder
import com.nagabhushansn95.financetracker.data.investments.Position
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import com.nagabhushansn95.financetracker.data.prefs.HoldingListPrefs
import com.nagabhushansn95.financetracker.data.prices.AmfiNav
import com.nagabhushansn95.financetracker.data.prices.AmfiScheme
import com.nagabhushansn95.financetracker.data.repository.HoldingsRepository
import com.nagabhushansn95.financetracker.ui.common.AccountField
import com.nagabhushansn95.financetracker.ui.common.BackAndMenu
import com.nagabhushansn95.financetracker.ui.common.ConfirmDeleteDialog
import com.nagabhushansn95.financetracker.ui.common.ConfirmDeleteEntryDialog
import com.nagabhushansn95.financetracker.ui.common.DatabaseFilePickers
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.NaturalOrder
import com.nagabhushansn95.financetracker.ui.common.NumberField
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.common.SortChip
import com.nagabhushansn95.financetracker.ui.common.StatCell
import com.nagabhushansn95.financetracker.ui.theme.appTopBarColors
import com.nagabhushansn95.financetracker.ui.transaction.InstrumentPickerSheet
import com.nagabhushansn95.financetracker.ui.transaction.typeTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Sort keys of the Stocks and Mutual funds lists. [defaultDescending]: the direction a key starts in (largest
 * amounts first). Ties fall back to the shown name.
 */
enum class HoldingSort(val title: String, val defaultDescending: Boolean = false) {
    NAME("Name"), DATE("Date"), INVESTED("Invested", true), VALUE("Current value", true),
    PROFIT("Profit", true), PROFIT_PCT("Profit %", true), XIRR("XIRR", true);

    /**
     * The order for the direction. Rows without the figure (no units held, no XIRR) come last either way. [DATE]
     * reads differently by direction: oldest first goes by the first entry, newest first by the last.
     */
    fun comparator(descending: Boolean): Comparator<HoldingRow> {
        fun <K : Comparable<K>> by(key: (HoldingRow) -> K?): Comparator<HoldingRow> =
            compareBy(nullsLast(if (descending) reverseOrder<K>() else naturalOrder()), key)

        val main: Comparator<HoldingRow> = when (this) {
            NAME -> compareBy(if (descending) NaturalOrder.reversed() else NaturalOrder) { it.shownName }
            DATE -> by { dateOf(it, descending) }
            INVESTED -> by { it.position.costHeld.takeIf { c -> c > 0 } }
            VALUE -> by { it.currentValue.takeIf { v -> v > 0 } }
            PROFIT -> by { it.heldProfit.takeIf { _ -> it.position.units > Position.UNIT_EPSILON } }
            PROFIT_PCT -> by { it.heldProfitPct }
            XIRR -> by { it.xirr?.takeIf { x -> x.isFinite() } }
        }
        return main.thenBy(NaturalOrder) { it.shownName }
    }

    companion object {
        /** The date the [DATE] key sorts a row by: its first entry when ascending, its last when descending. */
        fun dateOf(row: HoldingRow, descending: Boolean): LocalDate? =
            if (descending) row.position.lastDate else row.position.firstDate
    }
}

data class HoldingsState(
    val views: List<HoldingView> = emptyList(),
    val summary: InvestmentSummary? = null,
    val accounts: Map<Long, Account> = emptyMap(),
    /** Accounts an entry can move cash through: everything but provident funds. */
    val cashAccounts: List<Account> = emptyList(),
    val categories: List<InvestmentCategory> = emptyList(),
    /** Dividends announced by a statement that the bank has not credited yet. */
    val pending: List<PendingDividend> = emptyList(),
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
            unitPrice = v.security.lastPrice,
            priceDate = v.security.lastPriceDate,
            currentValue = v.position.costHeld,
            xirr = null,
            security = v.security,
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
            ?: if (v.type == InstrumentType.STOCK) cashAccounts.firstOrNull { it.accountType == AccountType.DEMAT && it.currency == v.security.currency }?.id
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
    private val prefs = AppPreferences(application)
    private val listPrefs = mutableMapOf<InstrumentType, MutableStateFlow<HoldingListPrefs>>()

    /** Grouping and sort of the list of [type], remembered across openings. */
    fun listPrefs(type: InstrumentType): StateFlow<HoldingListPrefs> = listPrefsOf(type)

    fun updateList(type: InstrumentType, change: (HoldingListPrefs) -> HoldingListPrefs) {
        val flow = listPrefsOf(type)
        flow.value = change(flow.value).also { prefs.setHoldingList(type.name, it) }
    }

    private fun listPrefsOf(type: InstrumentType) =
        listPrefs.getOrPut(type) { MutableStateFlow(prefs.holdingList(type.name)) }

    /** One-off outcome of a save or delete, shown in a snackbar. */
    val message = MutableStateFlow<String?>(null)
    fun messageShown() {
        message.value = null
    }

    private val base = combine(
        db.instrumentDao().observeListed(),
        db.transactionDao().observeWithInstrument(),
        db.accountDao().observeAll(),
        db.instrumentDao().observeCategories(),
        db.holdingEventDao().observeAll(),
    ) { listed, ledger, accounts, categories, events ->
        HoldingsState(
            views = HoldingEntries.build(listed, ledger, events),
            accounts = accounts.associateBy { it.id },
            cashAccounts = accounts.filter { it.accountType != AccountType.PROVIDENT_FUND && !it.deleted },
            categories = categories,
            loaded = true,
        )
    }

    val state: StateFlow<HoldingsState> =
        combine(
            base,
            InvestmentSummaryBuilder.observe(db),
            db.pendingDividendDao().observeAll(),
        ) { s, summary, pending -> s.copy(summary = summary, pending = pending) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HoldingsState())

    fun create(
        name: String, type: InstrumentType, symbol: String?, exchange: String?, isin: String?,
        currency: String, categoryId: Long?, owner: String, notes: String?, shortName: String?,
        series: String?, onDone: (Long) -> Unit
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
                    notes,
                    shortName = shortName,
                    series = series,
                )
            }
                .onSuccess { onDone(it) }
                .onFailure { message.value = it.message ?: "Could not add it" }
        }
    }

    fun update(listed: Listed) {
        viewModelScope.launch {
            runCatching { repository.update(listed) }
                .onFailure { message.value = it.message ?: "Could not save" }
        }
    }

    /** Bonus shares or a split: units that arrived without cash, recorded as an event. */
    fun saveEvent(
        view: HoldingView, kind: HoldingEntryKind, date: LocalDate, units: Double?, ratio: String?,
        existing: HoldingEvent?,
    ) {
        viewModelScope.launch {
            runCatching { repository.saveEvent(view, kind, date, units, ratio, existing) }
                .onFailure { message.value = it.message ?: "Could not save the entry" }
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteEvent(id) }
                .onSuccess { message.value = "Entry deleted" }
                .onFailure { message.value = it.message ?: "Could not delete the entry" }
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

    /** The bank credited a dividend from the awaiting list: it becomes the ledger entry and leaves the list. */
    fun recordPending(
        pending: PendingDividend,
        view: HoldingView,
        date: LocalDate,
        units: Double?,
        rate: Double?,
        amount: Double,
        accountId: Long?,
        particulars: String,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.recordPendingDividend(
                    pending,
                    view,
                    date,
                    units,
                    rate,
                    amount,
                    accountId,
                    particulars
                )
            }
                .onFailure { message.value = it.message ?: "Could not record the dividend" }
        }
    }

    fun dismissPending(id: Long) {
        viewModelScope.launch {
            runCatching { repository.dismissPendingDividend(id) }
                .onSuccess { message.value = "Dividend dropped" }
                .onFailure { message.value = it.message ?: "Could not drop it" }
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
 * Stocks & ETFs, or Mutual funds: what is held, grouped by category, with sold-out ones folded away behind an entry
 * in the ⋮ menu, which also holds the Zerodha imports. The + buys: pick the holding (or add a new one) and the buy
 * sheet opens for it.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
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
    val listPrefs by viewModel.listPrefs(type).collectAsStateWithLifecycle()
    val sort = HoldingSort.entries.firstOrNull { it.name == listPrefs.sort } ?: HoldingSort.NAME
    val descending = if (listPrefs.sort.isEmpty()) sort.defaultDescending else listPrefs.descending
    val grouped = listPrefs.grouped
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

    // Zerodha statements come in from the menu: three kinds for stocks, the Coin pair for funds.
    val importVm: ZerodhaImportViewModel = viewModel()
    val importState by importVm.state.collectAsStateWithLifecycle()
    LaunchedEffect(importState.message) {
        importState.message?.let { snackbar.showSnackbar(it); importVm.messageShown() }
    }
    var menu by remember { mutableStateOf(false) }
    var importKind by remember { mutableStateOf<ZerodhaImportKind?>(null) }
    val pickStatement = rememberLauncherForActivityResult(
        DatabaseFilePickers.OpenAny("Download", importVm::lastStatementUri)
    ) { uri ->
        val kind = importKind
        importKind = null
        if (uri != null && kind != null) importVm.preview(kind, uri)
    }
    var coinDialog by remember { mutableStateOf(false) }
    var ledgerDialog by remember { mutableStateOf(false) }
    var coinUri by remember { mutableStateOf<Uri?>(null) }
    var ledgerUri by remember { mutableStateOf<Uri?>(null) }
    var tradebookUri by remember { mutableStateOf<Uri?>(null) }
    val pickCoin = rememberLauncherForActivityResult(
        DatabaseFilePickers.OpenAny("Download", importVm::lastStatementUri)
    ) { if (it != null) coinUri = it }
    val pickTradebook = rememberLauncherForActivityResult(
        DatabaseFilePickers.OpenAny("Download", importVm::lastStatementUri)
    ) { if (it != null) tradebookUri = it }
    val pickLedger = rememberLauncherForActivityResult(
        DatabaseFilePickers.OpenAny("Download", importVm::lastStatementUri)
    ) { if (it != null) ledgerUri = it }
    var pendingSheet by remember { mutableStateOf(false) }
    val pendingHere = state.pending.filter { p -> own.any { it.instrument.id == p.instrumentId } }

    // A search looks through everything of the kind, sold out and family included, by name, symbol or ISIN.
    fun matches(v: HoldingView) = q.isEmpty() || listOfNotNull(
        v.instrument.name,
        v.security.shortName,
        v.security.symbol,
        v.security.isin
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
                colors = appTopBarColors(),
                title = { Text(typeTitle(type)) },
                navigationIcon = { BackAndMenu(onBack) },
                actions = {
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                        Icon(
                            if (searching) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (searching) "Close search" else "Search"
                        )
                    }
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (soldOut.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text(if (showSoldOut) "Hide sold out" else "Show sold out") },
                                onClick = { menu = false; showSoldOut = !showSoldOut })
                            HorizontalDivider()
                        }
                        if (type == InstrumentType.STOCK) {
                            DropdownMenuItem(
                                text = { Text("Import Kite tradebook and ledger…") },
                                onClick = { menu = false; ledgerDialog = true })
                            listOf(ZerodhaImportKind.DIVIDENDS).forEach { kind ->
                                DropdownMenuItem(
                                    text = { Text("Import ${kind.title}…") },
                                    onClick = {
                                        menu = false; importKind = kind; pickStatement.launch(
                                        arrayOf("*/*")
                                    )
                                    })
                            }
                        }
                        if (type == InstrumentType.MUTUAL_FUND) DropdownMenuItem(
                            text = { Text("Import Coin orders…") },
                            onClick = { menu = false; coinDialog = true })
                        importState.lastImport?.let { last ->
                            HorizontalDivider()
                            Text(
                                "Last import: $last",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .width(260.dp)
                            )
                        }
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
            item {
                // Sort key with its direction, and whether the rows sit under their category headers.
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SortChip(
                        options = HoldingSort.entries,
                        selected = sort,
                        descending = descending,
                        titleOf = { it.title },
                        onPick = { s ->
                            if (s != sort) viewModel.updateList(type) {
                                it.copy(
                                    sort = s.name,
                                    descending = s.defaultDescending
                                )
                            }
                        },
                        onFlip = {
                            viewModel.updateList(type) {
                                it.copy(
                                    sort = sort.name,
                                    descending = !descending
                                )
                            }
                        },
                    )
                    FilterChip(
                        selected = grouped,
                        onClick = { viewModel.updateList(type) { it.copy(grouped = !grouped) } },
                        label = { Text("Group by category") },
                        leadingIcon = if (grouped) {
                            {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                    )
                }
            }
            if (!searching && pendingHere.isNotEmpty()) item {
                Text(
                    "${pendingHere.size} dividend${if (pendingHere.size == 1) "" else "s"} awaiting credit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pendingSheet = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (q.isNotEmpty() && held.isEmpty() && soldOut.isEmpty() && family.isEmpty()) item {
                Text("Nothing matches \"$q\".", modifier = Modifier.padding(16.dp))
            }
            if (state.loaded && ofType.isEmpty()) item {
                Text(
                    "Nothing here yet. Use + to record the first ${if (type == InstrumentType.MUTUAL_FUND) "purchase" else "buy"}.",
                    modifier = Modifier.padding(16.dp)
                )
            }
            // When sorted by date, each row says which date placed it ("since" the first entry, "last" the last).
            val dateNote: (HoldingRow) -> String? = { h ->
                if (sort != HoldingSort.DATE) null
                else HoldingSort.dateOf(h, descending)
                    ?.let { d -> "${if (descending) "last" else "since"} ${d.format(SHORT_DATE)}" }
            }

            fun rows(prefix: String, rows: List<HoldingRow>) = items(
                rows,
                key = { "$prefix:${it.instrument.id}" }) { h ->
                HoldingListRow(
                    h,
                    dateNote(h)
                ) { onOpen(h.instrument.id) }
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
                if (key !in collapsed) rows(prefix, rows)
            }

            // A section's rows in the chosen order: under category headers, or as one flat list.
            val order = sort.comparator(descending)
            fun section(prefix: String, views: List<HoldingView>) {
                val sorted = views.map(state::rowFor).sortedWith(order)
                if (grouped) sorted.groupBy { it.categoryName }.toSortedMap()
                    .forEach { (category, rows) -> group(prefix, category, rows) }
                else rows(prefix, sorted)
            }

            section("held", held)
            if ((showSoldOut || q.isNotEmpty()) && soldOut.isNotEmpty()) {
                item(key = "s:sold") { SectionTitle("Sold out") }
                section("sold", soldOut)
            }
            if (family.isNotEmpty()) {
                item(key = "s:family") { SectionTitle("Family (not in the totals)") }
                section("family", family)
            }
        }
    }
    importState.preview?.let { preview ->
        ZerodhaImportSheet(
            preview = preview,
            busy = importState.busy,
            onSettings = importVm::updateSettings,
            onImport = importVm::commit,
            onDismiss = importVm::dismiss,
        )
    }
    if (ledgerDialog) TradebookLedgerImportDialog(
        tradebookName = tradebookUri?.let(importVm::fileName),
        ledgerName = ledgerUri?.let(importVm::fileName),
        onPickTradebook = { pickTradebook.launch(arrayOf("*/*")) },
        onPickLedger = { pickLedger.launch(arrayOf("*/*")) },
        onImport = {
            ledgerDialog = false; importVm.previewTradebookAndLedger(
            tradebookUri!!,
            ledgerUri!!
        )
        },
        onDismiss = { ledgerDialog = false },
    )
    if (coinDialog) CoinImportDialog(
        coinName = coinUri?.let(importVm::fileName),
        tradebookName = tradebookUri?.let(importVm::fileName),
        onPickCoin = { pickCoin.launch(arrayOf("*/*")) },
        onPickTradebook = { pickTradebook.launch(arrayOf("*/*")) },
        onImport = { coinDialog = false; importVm.previewCoin(coinUri!!, tradebookUri!!) },
        onDismiss = { coinDialog = false },
    )
    if (pendingSheet) FormSheet(
        title = "Dividends awaiting credit",
        onDismiss = { pendingSheet = false }) {
        pendingHere.sortedBy { it.exDate }.forEach { p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pendingSheet = false; onOpen(p.instrumentId) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.view(p.instrumentId)?.instrument?.name ?: "?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "ex ${p.exDate.format(SHORT_DATE)} · ${FdMath.plain(p.quantity)} × ${
                            FdMath.plain(
                                p.perShare
                            )
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    MoneyFormat.amount(p.gross),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            HorizontalDivider()
        }
        Text(
            "Open the stock and tap the dividend when the bank credits it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // Sold-out holdings retire after 90 idle days; the picker lists them under "Inactive", and a buy brings them back.
    if (picking) InstrumentPickerSheet(
        instruments = own.map { it.listed },
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
        onSave = { name, shortName, symbol, exchange, series, isin, currency, categoryId, owner, _, notes ->
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
                notes,
                shortName,
                series,
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
            onSave = { kind, date, units, rate, amount, account, particulars, exDate, _ ->
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

/**
 * Name with the value on the right; below, the symbol, units, average cost, price and cost (and [dateNote] when
 * the list is sorted by date); then the profit line.
 */
@Composable
private fun HoldingListRow(h: HoldingRow, dateNote: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                // The short name when there is one; the holding's own screen shows the full name.
                h.shownName,
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
            h.security?.tickerOrNull,
            units?.let { "${FdMath.plain(it)} units" },
            h.position.averageCost?.let { "avg %.2f".format(it) },
            h.unitPrice?.let { "price %.2f".format(it) }
                ?: if (units != null && units > Position.UNIT_EPSILON) "no price" else null,
            if (h.position.units > Position.UNIT_EPSILON) "invested ${MoneyFormat.amount(h.position.costHeld)}" else null,
            h.instrument.owner.takeIf { it != Instrument.OWNER_SELF }?.let { "owner $it" },
            dateNote,
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
    var editingEvent by remember { mutableStateOf<HoldingEvent?>(null) }
    var confirmDeleteEntry by remember { mutableStateOf<TransactionEntity?>(null) }
    var confirmDeleteEvent by remember { mutableStateOf<HoldingEvent?>(null) }
    var fabMenu by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var editingHolding by remember { mutableStateOf(false) }
    var pricing by remember { mutableStateOf(false) }
    var linking by remember { mutableStateOf(false) }
    var merging by remember { mutableStateOf(false) }
    var mergeInto by remember { mutableStateOf<Instrument?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<PendingDividend?>(null) }
    var dismissing by remember { mutableStateOf<PendingDividend?>(null) }
    val pendingHere =
        state.pending.filter { it.instrumentId == instrumentId }.sortedBy { it.exDate }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
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
                                text = { Text(if (v.security.exchange == AmfiNav.EXCHANGE) "Change the AMFI scheme…" else "Link the AMFI scheme…") },
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
        val security = v.security
        val current = position.currentValue(security.lastPrice, null)
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
                                v.security.symbol?.takeIf { it.isNotBlank() }
                                    ?.let { if (v.type == InstrumentType.MUTUAL_FUND) "AMFI $it" else it },
                                v.security.isin,
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
                                security.lastPrice?.let { "%.2f".format(it) } ?: "none")
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
                                security.lastPriceDate?.let { "${it.format(SHORT_DATE)} · ${security.lastPriceSource?.name?.lowercase() ?: ""}" }
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
                if (pendingHere.isNotEmpty()) {
                    Text(
                        "Awaiting credit",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    HorizontalDivider()
                    pendingHere.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { recording = p },
                                    onLongClick = { dismissing = p })
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Dividend announced",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "ex ${p.exDate.format(SHORT_DATE)} · ${FdMath.plain(p.quantity)} × ${
                                        FdMath.plain(
                                            p.perShare
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                MoneyFormat.amount(p.gross),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                    }
                    Text(
                        "Tap one when the bank credits it to enter the date and the amount received; press and hold to drop it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
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
            items(
                v.ledger.asReversed(),
                key = { if (it is HoldingLine.Cash) "t:${it.row.id}" else "e:${(it as HoldingLine.Units).event.id}" },
            ) { line ->
                if (line is HoldingLine.Units) {
                    val e = line.event
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = {}, onLongClick = { editingEvent = e })
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                e.kind.title + (e.ratio?.let { " $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${e.date.format(SHORT_DATE)} · +${FdMath.plain(e.units ?: 0.0)} units",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "+${FdMath.plain(e.units ?: 0.0)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    HorizontalDivider()
                    return@items
                }
                val t = (line as HoldingLine.Cash).row
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

                        HoldingEntryKind.BONUS, HoldingEntryKind.SPLIT, HoldingEntryKind.CHARGES, null -> null
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
                    Text(
                        MoneyFormat.amount(t.amount, v.security.currency),
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
    ?: editingEvent?.let { HoldingEntries.kindOf(it) }
    if (v != null && sheetKind != null) HoldingEntrySheet(
        view = v,
        accounts = state.cashAccounts,
        defaultTradeAccount = state.defaultTradeAccount(v),
        defaultDividendAccount = state.defaultDividendAccount(v),
        usualCharge = HoldingEntries.usualCharge(state.views),
        initialKind = sheetKind,
        existing = editing,
        existingEvent = editingEvent,
        onSave = { kind, date, units, rate, amount, account, particulars, exDate, ratio ->
            if (kind.isUnitsOnly) viewModel.saveEvent(v, kind, date, units, ratio, editingEvent)
            else viewModel.saveEntry(
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
            adding = null; editing = null; editingEvent = null
        },
        onDelete = editing?.let { row -> { confirmDeleteEntry = row } }
            ?: editingEvent?.let { ev -> { confirmDeleteEvent = ev } },
        onDismiss = { adding = null; editing = null; editingEvent = null },
    )
    confirmDeleteEntry?.let { row ->
        ConfirmDeleteEntryDialog(
            row,
            onConfirm = {
                viewModel.deleteEntry(row.id); editing = null; confirmDeleteEntry = null
            },
            onDismiss = { confirmDeleteEntry = null },
        )
    }
    confirmDeleteEvent?.let { ev ->
        ConfirmDeleteDialog(
            title = "Delete this ${ev.kind.name.lowercase()}?",
            text = "${ev.date.format(SHORT_DATE)}\nThe holding's units are recomputed without it.",
            onConfirm = {
                viewModel.deleteEvent(ev.id); editingEvent = null; confirmDeleteEvent = null
            },
            onDismiss = { confirmDeleteEvent = null },
        )
    }
    // A dividend from the awaiting list: the sheet opens filled in, with today's date and the gross amount to correct.
    recording?.let { p ->
        if (v != null) HoldingEntrySheet(
            view = v,
            accounts = state.cashAccounts,
            defaultTradeAccount = state.defaultTradeAccount(v),
            defaultDividendAccount = state.defaultDividendAccount(v),
            usualCharge = HoldingEntries.usualCharge(state.views),
            initialKind = HoldingEntryKind.DIVIDEND,
            existing = null,
            prefill = TransactionEntity(
                createdTime = LocalDateTime.now(),
                modifiedTime = LocalDateTime.now(),
                date = LocalDate.now(),
                kind = TransactionKind.INCOME,
                instrumentId = v.instrument.id,
                particulars = HoldingEntries.defaultParticulars(
                    HoldingEntryKind.DIVIDEND,
                    v.listed,
                    null
                ),
                quantity = p.quantity,
                rate = p.perShare,
                amount = p.gross,
                exDate = p.exDate,
            ),
            onSave = { kind, date, units, rate, amount, account, particulars, exDate, _ ->
                if (kind == HoldingEntryKind.DIVIDEND) viewModel.recordPending(
                    p,
                    v,
                    date,
                    units,
                    rate,
                    amount,
                    account,
                    particulars
                )
                else viewModel.saveEntry(
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
                recording = null
            },
            onDelete = null,
            onDismiss = { recording = null },
        )
    }
    dismissing?.let { p ->
        AlertDialog(
            onDismissRequest = { dismissing = null },
            title = { Text("Drop this dividend?") },
            text = { Text("It leaves the awaiting list without a ledger entry. Importing the statement again brings it back unless a dividend with the same ex-date has been recorded.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissPending(p.id); dismissing = null }) {
                    Text("Drop", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { dismissing = null }) { Text("Cancel") } },
        )
    }
    // A row the sheet cannot describe (a transfer tagged with the holding) is left alone rather than edited blind.
    LaunchedEffect(editing) {
        val row = editing ?: return@LaunchedEffect
        if (HoldingEntries.kindOf(row) == null) {
            editing =
                null; snackbar.showSnackbar("This row is not an entry the sheet can edit; it stays as it is.")
        }
    }
    if (editingHolding && v != null) HoldingSheet(
        existing = v.listed,
        type = v.type,
        categories = state.categories,
        onSave = { name, shortName, symbol, exchange, series, isin, currency, categoryId, owner, active, notes ->
            editingHolding = false
            viewModel.update(
                Listed(
                    v.instrument.copy(
                        name = name,
                        categoryId = categoryId,
                        owner = owner,
                        active = active,
                        notes = notes,
                    ),
                    v.security.copy(
                        shortName = shortName,
                        symbol = symbol,
                        exchange = exchange,
                        series = series,
                        isin = isin,
                        currency = currency,
                    ),
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
            .map { it.listed },
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
 * fund's AMFI scheme code is best set by "Link the AMFI scheme" on its screen. A holding never changes kind: a fund
 * stays a fund.
 */
@Composable
fun HoldingSheet(
    existing: Listed?,
    type: InstrumentType,
    categories: List<InvestmentCategory>,
    onSave: (
        name: String, shortName: String?, symbol: String?, exchange: String?, series: String?, isin: String?,
        currency: String, categoryId: Long?, owner: String, active: Boolean, notes: String?
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val fund = type == InstrumentType.MUTUAL_FUND
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var shortName by remember { mutableStateOf(existing?.security?.shortName ?: "") }
    var symbol by remember { mutableStateOf(existing?.security?.symbol ?: "") }
    var exchange by remember {
        mutableStateOf(
            existing?.security?.exchange ?: if (fund) "" else "NSE"
        )
    }
    var series by remember { mutableStateOf(existing?.security?.series ?: "") }
    var isin by remember { mutableStateOf(existing?.security?.isin ?: "") }
    var currency by remember { mutableStateOf(existing?.security?.currency ?: "INR") }
    var owner by remember { mutableStateOf(existing?.instrument?.owner ?: Instrument.OWNER_SELF) }
    var active by remember { mutableStateOf(existing?.instrument?.active ?: true) }
    var notes by remember { mutableStateOf(existing?.instrument?.notes ?: "") }
    var categoryId by remember { mutableStateOf(existing?.instrument?.categoryId) }
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
        OutlinedTextField(
            value = shortName,
            onValueChange = { shortName = it },
            label = { Text("Short name (optional)") },
            placeholder = { Text(if (fund) "PPFAS Flexi Cap" else "Nifty BeES") },
            supportingText = { Text("Shown on this list and on ledger lines instead of the ${if (fund) "name" else "symbol"}.") },
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
            OutlinedTextField(
                value = series,
                onValueChange = { series = it.uppercase().trim() },
                label = { Text("Series") },
                placeholder = { Text("RR") },
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
            if (fund) "Prices come from AMFI once the scheme is linked (the menu on the fund's screen does it by name)."
            else "Prices come from Yahoo Finance by the symbol (NSE, then its RR series, then BSE). A renamed stock keeps its ledger: change the symbol and name here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            enabled = name.isNotBlank() && (fund || symbol.isNotBlank()),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    name.trim(),
                    shortName.trim().ifBlank { null },
                    symbol.trim().ifBlank { null },
                    exchange.trim().ifBlank { null }
                        ?: if (fund) (if (symbol.isNotBlank()) AmfiNav.EXCHANGE else null) else "NSE",
                    series.trim().ifBlank { null }?.takeIf { !fund },
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
        accountId: Long?, particulars: String, exDate: LocalDate?, ratio: String?
    ) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    /** Starting values for a new entry (a dividend from the awaiting list); nothing is updated through it. */
    prefill: TransactionEntity? = null,
    /** The bonus or split event being edited. */
    existingEvent: HoldingEvent? = null,
) {
    val type = view.type
    val seed = existing ?: prefill
    val editingAny = existing != null || existingEvent != null
    // The kind is fixed for the sheet's life: a buy never turns into a dividend by a stray tap.
    val kind = initialKind
    var date by remember {
        mutableStateOf<LocalDate?>(
            existingEvent?.date ?: seed?.date ?: LocalDate.now()
        )
    }
    var units by remember {
        mutableStateOf(
            existingEvent?.units?.let(FdMath::plain) ?: seed?.quantity?.let(FdMath::plain) ?: ""
        )
    }
    var unitsTyped by remember { mutableStateOf(seed?.quantity != null || existingEvent != null) }
    var rate by remember { mutableStateOf(seed?.rate?.let(FdMath::plain) ?: "") }
    var amount by remember { mutableStateOf(seed?.amount?.let(FdMath::plain) ?: "") }
    var amountTyped by remember { mutableStateOf(seed != null) }
    var exDate by remember { mutableStateOf(seed?.exDate) }
    var particulars by remember {
        mutableStateOf(seed?.let { HoldingEntries.stripExDate(it.particulars) } ?: "")
    }
    var typed by remember { mutableStateOf(seed != null) }
    val existingRatio = existingEvent?.ratio?.let { HoldingEntries.ratioOf(it) }
    var a by remember { mutableStateOf(existingRatio?.first ?: "1") }
    var b by remember { mutableStateOf(existingRatio?.second ?: "1") }
    var accountId by remember {
        mutableStateOf(
            existing?.let { it.fromAccountId ?: it.toAccountId }
                ?: if (kind == HoldingEntryKind.DIVIDEND) defaultDividendAccount else defaultTradeAccount
        )
    }
    // A dividend typed by hand starts with the units held on the ex-date (or the credit date) until the count is typed;
    // it is never derived from the amount, which is net of TDS.
    val unitsShown = when {
        unitsTyped || kind != HoldingEntryKind.DIVIDEND -> units
        else -> (exDate ?: date)?.let { d ->
            Holdings.position(view.instrument.id, view.rows.filter { it.date <= d }).units
        }?.takeIf { it > 0 }?.let(FdMath::plain) ?: ""
    }
    val unitsValue = unitsShown.trim().toDoubleOrNull()
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
            view.rows.filter { it.date <= date!! && it.id != existing?.id },
            events = view.events.filter { it.date <= date!! && it.id != existingEvent?.id }).units
    else heldNow
    val av = a.trim().toDoubleOrNull()
    val bv = b.trim().toDoubleOrNull()
    val ratioUnits: Double? = when {
        existingEvent != null || av == null || bv == null || av <= 0 || bv <= 0 -> null
        kind == HoldingEntryKind.SPLIT -> HoldingEntries.splitUnits(held, av, bv)
        kind == HoldingEntryKind.BONUS -> HoldingEntries.bonusUnits(held, av, bv)
        else -> null
    }
    val ratio =
        if (kind.isUnitsOnly && av != null && bv != null) "${a.trim()}:${b.trim()}" else null
    val suggested = HoldingEntries.suggestedParticulars(kind, view, ratio)
    val effectiveUnits: Double? = when {
        kind.hasUnits -> unitsValue
        kind.isUnitsOnly -> if (existingEvent != null) unitsValue else ratioUnits
        kind == HoldingEntryKind.DIVIDEND -> unitsValue?.takeIf { it > 0 }
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
                existing?.id,
                view.events,
            ) else null
    FormSheet(
        title = if (!editingAny) kind.title(type) else "Edit ${kind.title(type).lowercase()}",
        onDismiss = onDismiss
    ) {
        when {
            kind.hasUnits -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        units,
                        "Units",
                        { units = it; unitsTyped = true },
                        Modifier.weight(1f)
                    )
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
                        unitsShown,
                        "Units",
                        { units = it; unitsTyped = true },
                        Modifier.weight(1f)
                    )
                    NumberField(rate, "Per unit", { rate = it }, Modifier.weight(1f))
                }
                NumberField(
                    amountShown,
                    "Amount credited",
                    { amount = it; amountTyped = true },
                    Modifier.fillMaxWidth()
                )
                // The gross (the statement's from the awaiting list, else units × per unit) against what the bank credited.
                val gross = if (prefill != null && existing == null) prefill.amount
                else if (unitsValue != null && rateValue != null && unitsValue > 0 && rateValue > 0) unitsValue * rateValue
                else null
                if (gross != null) {
                    val tds = amountValue?.let { gross - it }
                    Text(
                        "Gross ${MoneyFormat.amount(gross)}" +
                                (tds?.takeIf { it > 0.005 }
                                    ?.let { " · TDS ${MoneyFormat.amount(it)}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            kind.isUnitsOnly -> {
                if (existingEvent == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    if (existingEvent != null) NumberField(
                        units,
                        "Units",
                        { units = it; unitsTyped = true },
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

                HoldingEntryKind.DIVIDEND -> "Counts as income on this holding, not as a sale. Units and per-unit rate are the gross; the amount is what the bank credited (TDS is the difference)."
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
                    exDate.takeIf { kind == HoldingEntryKind.DIVIDEND }, ratio,
                )
            },
        ) { Text(if (!editingAny) "Record" else "Save") }
        if (onDelete != null) OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete entry", color = MaterialTheme.colorScheme.error)
        }
    }
}
