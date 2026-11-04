package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentValuation
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.ValuationSource
import com.nagabhushansn95.financetracker.data.investments.Holdings
import com.nagabhushansn95.financetracker.data.investments.Position
import com.nagabhushansn95.financetracker.data.repository.EditDataRepository
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.edit.InstrumentSheet
import com.nagabhushansn95.financetracker.ui.transaction.LedgerLookups
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class InstrumentDetailState(
    val instrument: Instrument? = null,
    val position: Position? = null,
    val valuation: InstrumentValuation? = null,
    val rows: List<TransactionEntity> = emptyList(),
    val lookups: LedgerLookups? = null,
    val categories: List<InvestmentCategory> = emptyList(),
    val allInstruments: List<Instrument> = emptyList(),
    val categoryPath: String? = null,
    val xirr: Double? = null,
    val currentValue: Double = 0.0,
    val loaded: Boolean = false,
)

class InstrumentDetailViewModel(application: Application, private val instrumentId: Long) :
    AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)

    val state: StateFlow<InstrumentDetailState> = combine(
        db.instrumentDao().observeAll(),
        db.instrumentDao().observeCategories(),
        db.transactionDao().observeWithInstrument(),
        db.instrumentDao().observeValuations(),
        db.accountDao().observeAll(),
    ) { instruments, categories, ledger, valuations, accounts ->
        val instrument = instruments.firstOrNull { it.id == instrumentId }
            ?: return@combine InstrumentDetailState(loaded = true)
        val rows = ledger.filter { it.instrumentId == instrumentId }
        val position = Holdings.position(
            instrumentId,
            rows,
            unitBased = instrument.instrumentType == com.nagabhushansn95.financetracker.data.db.entity.InstrumentType.STOCK || instrument.instrumentType == com.nagabhushansn95.financetracker.data.db.entity.InstrumentType.MUTUAL_FUND
        )
        val valuation = valuations.filter { it.instrumentId == instrumentId }
            .maxWithOrNull(compareBy({ it.date }, { it.id }))
        val current = position.currentValue(valuation?.unitPrice, valuation?.totalValue)
        val byId = categories.associateBy { it.id }
        InstrumentDetailState(
            instrument = instrument,
            position = position,
            valuation = valuation,
            rows = rows.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id }),
            lookups = LedgerLookups(accounts, db.categoryDao().getAll(), instruments),
            categories = categories,
            allInstruments = instruments,
            categoryPath = instrument.categoryId?.let { byId[it] }?.let { c ->
                c.parentId?.let { byId[it]?.name }?.let { "$it › ${c.name}" } ?: c.name
            },
            xirr = Holdings.xirr(position.cashFlows, current, LocalDate.now()),
            currentValue = current,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstrumentDetailState())

    fun setManualPrice(unitPrice: Double?, totalValue: Double?, date: LocalDate) {
        viewModelScope.launch {
            db.instrumentDao().insertValuation(
                InstrumentValuation(
                    instrumentId = instrumentId,
                    date = date,
                    unitPrice = unitPrice,
                    totalValue = totalValue,
                    source = ValuationSource.MANUAL,
                    fetchedAt = LocalDateTime.now()
                )
            )
        }
    }

    fun save(instrument: Instrument) {
        viewModelScope.launch { EditDataRepository(db).updateInstrument(instrument) }
    }

    fun mergeInto(targetId: Long) {
        viewModelScope.launch {
            runCatching {
                EditDataRepository(db).mergeInstruments(
                    instrumentId,
                    targetId
                )
            }
        }
    }

    fun addBonusUnits(extraUnits: Double, date: LocalDate, particulars: String) {
        viewModelScope.launch {
            runCatching {
                EditDataRepository(db).addBonusUnits(
                    instrumentId,
                    extraUnits,
                    date,
                    particulars
                )
            }
        }
    }

    /** Stores the scheme code and ISIN on the fund and today's NAV from the list. */
    fun linkScheme(scheme: com.nagabhushansn95.financetracker.data.prices.AmfiScheme) {
        viewModelScope.launch {
            val instrument = db.instrumentDao().getById(instrumentId) ?: return@launch
            EditDataRepository(db).updateInstrument(
                instrument.copy(
                    symbol = scheme.code,
                    exchange = com.nagabhushansn95.financetracker.data.prices.AmfiNav.EXCHANGE,
                    isin = scheme.isinGrowth ?: scheme.isinReinvestment ?: instrument.isin,
                )
            )
            db.instrumentDao().insertValuation(
                InstrumentValuation(
                    instrumentId = instrumentId,
                    date = scheme.date,
                    unitPrice = scheme.nav,
                    source = ValuationSource.AMFI,
                    fetchedAt = LocalDateTime.now()
                )
            )
        }
    }
}

private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy")

/** One instrument: position, price, and its ledger. Press and hold a row to edit the transaction. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InstrumentDetailScreen(
    instrumentId: Long,
    onBack: () -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val viewModel: InstrumentDetailViewModel = viewModel(
        key = "instrument$instrumentId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                InstrumentDetailViewModel(app, instrumentId) as T
        })
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var pricing by remember { mutableStateOf(false) }
    var linking by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var merging by remember { mutableStateOf(false) }
    var mergeInto by remember { mutableStateOf<Instrument?>(null) }
    var bonus by remember { mutableStateOf(false) }
    val instrument = state.instrument
    val position = state.position

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(instrument?.name ?: "Instrument") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (instrument != null) IconButton(onClick = { editing = true }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit instrument"
                        )
                    }
                    if (instrument != null) IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = menu,
                        onDismissRequest = { menu = false }) {
                        if (position?.hasUnits == true) androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Record bonus shares or split…") },
                            onClick = { menu = false; bonus = true })
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Merge into another instrument…") },
                            onClick = { menu = false; merging = true })
                    }
                },
            )
        },
    ) { padding ->
        if (instrument == null || position == null) {
            Text(
                if (state.loaded) "Instrument not found." else "Loading…",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            item {
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            listOfNotNull(
                                instrument.symbol,
                                instrument.isin,
                                state.categoryPath,
                                instrument.owner.takeIf { it != Instrument.OWNER_SELF }
                                    ?.let { "owner $it" }).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            MoneyFormat.amount(state.currentValue),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (position.hasUnits) Row {
                            Stat(
                                "Units",
                                if (position.units == position.units.toLong()
                                        .toDouble()
                                ) position.units.toLong()
                                    .toString() else "%.3f".format(position.units)
                            )
                            Stat("Avg cost", position.averageCost?.let { "%.2f".format(it) } ?: "—")
                            Stat(
                                "Price",
                                state.valuation?.unitPrice?.let { "%.2f".format(it) } ?: "none")
                        }
                        Row {
                            Stat(
                                "Invested",
                                MoneyFormat.amount(position.costHeld)
                            ); Stat(
                            "Held P/L",
                            MoneyFormat.amount(state.currentValue - position.costHeld),
                            state.currentValue - position.costHeld
                        ); Stat(
                            "Realised",
                            MoneyFormat.amount(position.realised),
                            position.realised
                        )
                        }
                        Row {
                            Stat("Income", MoneyFormat.amount(position.income)); Stat(
                            "XIRR",
                            pct(state.xirr)
                        ); Stat(
                            "Price as of",
                            state.valuation?.let { "${it.date.format(DATE)} · ${it.source.name.lowercase()}" }
                                ?: "—")
                        }
                        Text(
                            "Total put in ${MoneyFormat.amount(position.totalInvested)}, taken out ${
                                MoneyFormat.amount(
                                    position.totalRedeemed
                                )
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { pricing = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (position.hasUnits) "Update current price" else "Update current value") }
                        if (instrument.instrumentType == com.nagabhushansn95.financetracker.data.db.entity.InstrumentType.MUTUAL_FUND) {
                            OutlinedButton(
                                onClick = { linking = true },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (instrument.exchange == com.nagabhushansn95.financetracker.data.prices.AmfiNav.EXCHANGE) "Change AMFI scheme (${instrument.symbol})" else "Link AMFI scheme for NAVs") }
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
            items(state.rows, key = { it.id }) { t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = {}, onLongClick = { onEditTransaction(t.id) })
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.particulars, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${t.date.format(DATE)} · ${state.lookups?.describe(t) ?: t.kind.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        MoneyFormat.amount(t.amount, t.currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                HorizontalDivider()
            }
        }
    }

    if (editing && instrument != null) {
        InstrumentSheet(
            existing = instrument,
            initialType = instrument.instrumentType,
            transactionCount = state.rows.size,
            categories = state.categories,
            onSave = { viewModel.save(it); editing = false },
            onDismiss = { editing = false })
    }
    if (merging && instrument != null) {
        com.nagabhushansn95.financetracker.ui.transaction.InstrumentPickerSheet(
            instruments = state.allInstruments.filter { it.instrumentType == instrument.instrumentType && it.id != instrument.id },
            recent = emptyList(),
            optional = false,
            onPick = { id ->
                mergeInto = state.allInstruments.firstOrNull { it.id == id }; merging = false
            },
            onNew = { merging = false },
            onDismiss = { merging = false },
        )
    }
    mergeInto?.let { target ->
        if (instrument != null) AlertDialog(
            onDismissRequest = { mergeInto = null },
            title = { Text("Merge ${instrument.name} into ${target.name}?") },
            text = {
                Text(
                    "All ${state.rows.size} transactions of ${instrument.name} move to ${target.name}, and ${instrument.name} is deleted. " +
                            "Use this when a stock was renamed (ZOMATO → ETERNAL): the purchases under the old name then belong to the new one. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.mergeInto(target.id); mergeInto = null; onBack()
                }) { Text("Merge") }
            },
            dismissButton = { TextButton(onClick = { mergeInto = null }) { Text("Cancel") } },
        )
    }
    if (bonus && instrument != null && position != null) {
        BonusOrSplitDialog(
            instrument = instrument,
            unitsHeld = position.units,
            onRecord = { extra, date, particulars ->
                viewModel.addBonusUnits(
                    extra,
                    date,
                    particulars
                ); bonus = false
            },
            onDismiss = { bonus = false },
        )
    }
    if (linking && instrument != null) {
        AmfiSchemeSheet(
            initialQuery = instrument.name,
            onPick = { viewModel.linkScheme(it); linking = false },
            onDismiss = { linking = false })
    }
    if (pricing && position != null) {
        var text by remember { mutableStateOf("") }
        val value = text.trim().toDoubleOrNull()
        AlertDialog(
            onDismissRequest = { pricing = false },
            title = { Text(if (position.hasUnits) "Current price per unit" else "Current value") },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(if (position.hasUnits) "Price" else "Value") })
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "The market price today, used for current value and held profit. Recorded as a manual valuation; live prices arrive with a later milestone.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = value != null && value >= 0, onClick = {
                    if (position.hasUnits) viewModel.setManualPrice(
                        value,
                        null,
                        LocalDate.now()
                    ) else viewModel.setManualPrice(null, value, LocalDate.now())
                    pricing = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { pricing = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Stat(label: String, value: String, signed: Double? = null) {
    Column(modifier = Modifier.width(110.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value, style = MaterialTheme.typography.bodyMedium,
            color = when {
                signed == null -> MaterialTheme.colorScheme.onSurface; signed < 0 -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.tertiary
            }
        )
    }
}


/** Bonus a:b (a new for every b held) or split old:new; the extra units are an income row at zero cost. */
@Composable
private fun BonusOrSplitDialog(
    instrument: Instrument,
    unitsHeld: Double,
    onRecord: (Double, LocalDate, String) -> Unit,
    onDismiss: () -> Unit
) {
    var split by remember { mutableStateOf(false) }
    var a by remember { mutableStateOf("1") }
    var b by remember { mutableStateOf("1") }
    var date by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    val av = a.toDoubleOrNull()
    val bv = b.toDoubleOrNull()
    val extra = when {
        av == null || bv == null || av <= 0 || bv <= 0 -> null
        split -> unitsHeld * bv / av - unitsHeld                       // old:new, e.g. 1:5 turns 100 into 500
        else -> unitsHeld * av / bv                                     // bonus a:b, e.g. 1:1 doubles
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (split) "Stock split" else "Bonus shares") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = !split,
                        onClick = { split = false },
                        label = { Text("Bonus") })
                    androidx.compose.material3.FilterChip(
                        selected = split,
                        onClick = { split = true },
                        label = { Text("Split") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = a,
                        onValueChange = { a = it },
                        label = { Text(if (split) "Old" else "Bonus") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = b,
                        onValueChange = { b = it },
                        label = { Text(if (split) "New" else "For every") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                com.nagabhushansn95.financetracker.ui.common.DateField(
                    "Record date",
                    date,
                    { date = it },
                    Modifier.fillMaxWidth()
                )
                Text(
                    "Held ${
                        com.nagabhushansn95.financetracker.data.investments.FdMath.plain(
                            unitsHeld
                        )
                    } units" +
                            (extra?.let {
                                " → +${
                                    com.nagabhushansn95.financetracker.data.investments.FdMath.plain(
                                        it
                                    )
                                } units at no cost; the average cost falls, the invested amount stays."
                            } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = extra != null && extra > 0 && date != null, onClick = {
                onRecord(
                    extra!!,
                    date!!,
                    if (split) "Stock split ${a.trim()}:${b.trim()} - ${instrument.name}" else "Bonus Shares ${a.trim()}:${b.trim()} - ${instrument.name}"
                )
            }) { Text("Record") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
