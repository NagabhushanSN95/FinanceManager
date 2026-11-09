package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.FdEvent
import com.nagabhushansn95.financetracker.data.db.entity.FdStatus
import com.nagabhushansn95.financetracker.data.db.entity.FdTemplate
import com.nagabhushansn95.financetracker.data.db.entity.FdType
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.investments.FdMath
import com.nagabhushansn95.financetracker.data.investments.FdView
import com.nagabhushansn95.financetracker.data.investments.FixedDeposits
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import com.nagabhushansn95.financetracker.data.prefs.FdListPrefs
import com.nagabhushansn95.financetracker.data.repository.FixedDepositRepository
import com.nagabhushansn95.financetracker.data.repository.NewFixedDeposit
import com.nagabhushansn95.financetracker.ui.common.AccountField
import com.nagabhushansn95.financetracker.ui.common.AutocompleteField
import com.nagabhushansn95.financetracker.ui.common.ChoiceDropdown
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.LabelRow
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.NaturalOrder
import com.nagabhushansn95.financetracker.ui.common.NumberField
import com.nagabhushansn95.financetracker.ui.common.PickDateDialog
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.common.StatCell
import com.nagabhushansn95.financetracker.ui.transaction.LedgerLookups
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FixedDepositsState(
    val deposits: List<FdView> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<InvestmentCategory> = emptyList(),
    val templates: List<FdTemplate> = emptyList(),
    val lookups: LedgerLookups? = null,
    val loaded: Boolean = false,
)

/** Every fixed deposit with its computed figures, plus the actions of the FD screens. */
class FixedDepositsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repo = FixedDepositRepository(db)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    val state: StateFlow<FixedDepositsState> = combine(
        db.instrumentDao().observeAll(),
        db.instrumentDao().observeFixedDeposits(),
        db.transactionDao().observeWithInstrument(),
        db.instrumentDao().observeValuations(),
        combine(
            db.accountDao().observeVisible(),
            db.instrumentDao().observeCategories(),
            db.instrumentDao().observeFdTemplates(),
            db.instrumentDao().observeFdEvents(),
        ) { a, c, t, e -> Extras(a, c, t, e) },
    ) { instruments, terms, ledger, valuations, (accounts, categories, templates, events) ->
        FixedDepositsState(
            deposits = FixedDeposits.build(
                instruments,
                terms,
                ledger,
                valuations,
                LocalDate.now(),
                events
            ),
            accounts = accounts.filter { it.isCashAccount },
            categories = categories,
            templates = templates,
            lookups = LedgerLookups(accounts, emptyList(), instruments),
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FixedDepositsState())

    private data class Extras(
        val accounts: List<Account>,
        val categories: List<InvestmentCategory>,
        val templates: List<FdTemplate>,
        val events: List<FdEvent>,
    )

    /** Filter and sort of the list, kept across visits and restarts. */
    private val prefs = AppPreferences(application)
    private val _listPrefs = MutableStateFlow(prefs.fdList)
    val listPrefs: StateFlow<FdListPrefs> = _listPrefs
    fun updateList(change: (FdListPrefs) -> FdListPrefs) {
        _listPrefs.value = change(_listPrefs.value).also { prefs.fdList = it }
    }

    fun nextName() = FixedDeposits.nextName(state.value.deposits.map { it.instrument })

    fun saveEvent(event: FdEvent) = run {
        repo.saveEvent(event)
        if (event.kind == com.nagabhushansn95.financetracker.data.db.entity.FdEventKind.TDS) "Tax deduction recorded" else "Interest credit recorded"
    }

    fun deleteEvent(id: Long) = run { repo.deleteEvent(id); "Entry deleted" }

    fun create(new: NewFixedDeposit) = run { repo.create(new); "Deposit opened" }
    fun saveTemplate(template: FdTemplate) = run {
        repo.saveTemplate(template); "Template \"${template.name.trim()}\" saved"
    }

    fun delete(instrumentId: Long) = run { repo.delete(instrumentId); "Deposit deleted" }
    fun saveTerms(instrument: Instrument, terms: FixedDeposit, depositDate: LocalDate?) =
        run { repo.saveTerms(instrument, terms, depositDate); "Saved" }

    fun close(
        id: Long,
        date: LocalDate,
        amount: Double,
        toAccountId: Long?,
        status: FdStatus,
        tdsReported: Double?
    ) =
        run {
            repo.close(
                id,
                date,
                amount,
                toAccountId,
                status,
                tdsReported
            ); if (status == FdStatus.CLOSED_AT_MATURITY) "Deposit matured" else "Deposit closed"
        }

    fun renew(
        oldId: Long,
        date: LocalDate,
        matured: Double,
        renewed: Double,
        remainderTo: Long?,
        newName: String,
        terms: FixedDeposit,
        tdsReported: Double?,
    ) =
        run {
            repo.renew(
                oldId,
                date,
                matured,
                renewed,
                remainderTo,
                newName,
                terms,
                tdsReported,
            ); "Renewed into $newName"
        }

    private fun run(block: suspend () -> String) {
        viewModelScope.launch {
            _message.value =
                runCatching { block() }.getOrElse { it.message ?: "Something went wrong" }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}

/** Closed covers deposits that matured, were closed early or were renewed into another. */
enum class FdFilter(val title: String) { ACTIVE("Active"), CLOSED("Closed"), ALL("All") }

/** Distinct values already used by the deposits (latest first), offered as suggestions in the form. */
data class FdSuggestions(
    val banks: List<String> = emptyList(),
    val holders: List<String> = emptyList(),
    val branches: List<String> = emptyList(),
    val custIds: List<String> = emptyList(),
    val owners: List<String> = emptyList(),
) {
    companion object {
        fun of(deposits: List<FdView>): FdSuggestions {
            val latestFirst = deposits.sortedByDescending { it.instrument.id }
            return of(latestFirst.mapNotNull { it.terms }, latestFirst.map { it.instrument.owner })
        }

        /** [terms] and [owners] latest first. */
        fun of(terms: List<FixedDeposit>, owners: List<String>): FdSuggestions {
            fun values(pick: (FixedDeposit) -> String?) =
                terms.mapNotNull { pick(it)?.trim()?.ifBlank { null } }.distinct()
            return FdSuggestions(
                banks = values { it.bankName },
                holders = values { it.holderName },
                branches = values { it.branch },
                custIds = values { it.custId },
                owners = owners.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            )
        }
    }
}

/** One line for a template: "₹1,00,000 · SBI, Jayanagar · 1 year @ 7% · holder". */
fun FdTemplate.summary(): String = listOfNotNull(
    amount?.let { MoneyFormat.amount(it) },
    listOfNotNull(bankName, branch).joinToString(", ").ifEmpty { null },
    listOfNotNull(
        FdMath.periodLabel(periodMonths, periodDays),
        rate?.let { "@ ${FdMath.plain(it)}%" })
        .joinToString(" ").ifEmpty { null },
    holderName,
    owner?.takeIf { it != Instrument.OWNER_SELF }?.let { "owner $it" },
).joinToString(" · ")

/** [defaultDescending]: the direction that makes sense first (largest amounts first, latest deposits first). */
enum class FdSort(val title: String, val defaultDescending: Boolean = false) {
    MATURITY("Maturity date"), DEPOSIT("Deposit date", true), AMOUNT("Amount", true), VALUE(
        "Value",
        true
    ),
    BANK("Bank"), NAME("Name"), DAYS_LEFT("Days left");

    /** [MATURITY] reads as the date it sorts by under the current filter: closed deposits sort by their closing date. */
    fun titleFor(filter: FdFilter): String = if (this != MATURITY) title else when (filter) {
        FdFilter.ACTIVE -> "Maturity date"
        FdFilter.CLOSED -> "Closing date"
        FdFilter.ALL -> "Maturity / closing date"
    }

    /**
     * Ascending order; the screen reverses it when asked. Names and banks compare naturally (FD #9 before FD #10).
     * [MATURITY] orders open deposits by when they mature and closed ones by when they were paid out.
     */
    fun comparator(): Comparator<FdView> = when (this) {
        MATURITY -> compareBy<FdView, LocalDate?>(nullsLast()) { if (it.isOpen) it.maturityDate else it.closedOn }.thenBy { it.instrument.id }
        DEPOSIT -> compareBy<FdView, LocalDate?>(nullsLast()) { it.depositDate }.thenBy { it.instrument.id }
        AMOUNT -> compareBy<FdView> {
            it.principal.takeIf { p -> p > 0 } ?: it.position.totalInvested
        }.thenBy { it.instrument.id }

        VALUE -> compareBy<FdView> { if (it.isOpen) it.estimated else it.position.totalRedeemed }.thenBy { it.instrument.id }
        BANK -> compareBy<FdView, String?>(nullsLast(NaturalOrder)) { it.bank }.thenBy(nullsLast()) { it.maturityDate }
        NAME -> compareBy(NaturalOrder) { it.instrument.name }
        DAYS_LEFT -> compareBy<FdView, Long?>(nullsLast()) { it.daysLeft }.thenBy { it.instrument.id }
    }
}

/**
 * Fixed deposits: active by default, with status, bank, holder, branch and owner filters on one line (each of the
 * value filters takes any number of values), sorting, and "New fixed deposit".
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun FixedDepositsScreen(
    onBack: () -> Unit,
    onOpenDeposit: (Long) -> Unit,
    viewModel: FixedDepositsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    // Filter and sort live in the preferences, so the list comes back the way it was left, even weeks later.
    val listPrefs by viewModel.listPrefs.collectAsStateWithLifecycle()
    val filter = FdFilter.entries.firstOrNull { it.name == listPrefs.filter } ?: FdFilter.ACTIVE
    val sort = FdSort.entries.firstOrNull { it.name == listPrefs.sort } ?: FdSort.MATURITY
    val descending = if (listPrefs.sort.isEmpty()) sort.defaultDescending else listPrefs.descending
    val banks = listPrefs.banks
    val holders = listPrefs.holders
    val branches = listPrefs.branches
    val owners = listPrefs.owners
    var creating by remember { mutableStateOf(false) }

    val all = state.deposits
    val suggestions = remember(all) { FdSuggestions.of(all) }
    fun FdView.holder() = terms?.holderName?.trim()?.ifBlank { null }
    fun FdView.branch() = terms?.branch?.trim()?.ifBlank { null }
    val shown = all.filter { d ->
        when (filter) {
            FdFilter.ACTIVE -> d.isOpen
            FdFilter.CLOSED -> !d.isOpen
            FdFilter.ALL -> true
        } && (banks.isEmpty() || d.bank in banks) && (holders.isEmpty() || d.holder() in holders) &&
                (branches.isEmpty() || d.branch() in branches) && (owners.isEmpty() || d.instrument.owner in owners)
    }.sortedWith(sort.comparator().let { if (descending) it.reversed() else it })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fixed deposits") },
                navigationIcon = { com.nagabhushansn95.financetracker.ui.common.BackAndMenu(onBack) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                creating = true
            }) { Icon(Icons.Filled.Add, contentDescription = "New fixed deposit") }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                item { FdTotals(all.filter { it.isOpen && it.instrument.owner == Instrument.OWNER_SELF }) }
                item {
                    // Dropdown chips that wrap onto a second line when they must; nothing scrolls sideways.
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PickChip(
                            filter.title,
                            FdFilter.entries.map { it.title },
                            filter.title
                        ) { picked ->
                            viewModel.updateList { it.copy(filter = FdFilter.entries.first { f -> f.title == picked }.name) }
                        }
                        val bankOptions = suggestions.banks.sortedWith(NaturalOrder)
                        val holderOptions = suggestions.holders.sortedWith(NaturalOrder)
                        val branchOptions = suggestions.branches.sortedWith(NaturalOrder)
                        val ownerOptions = suggestions.owners.sortedWith(NaturalOrder)
                        if (bankOptions.size > 1) MultiPickChip(
                            "Bank",
                            "banks",
                            bankOptions,
                            banks
                        ) { v -> viewModel.updateList { it.copy(banks = v) } }
                        if (holderOptions.size > 1) MultiPickChip(
                            "Holder",
                            "holders",
                            holderOptions,
                            holders
                        ) { v -> viewModel.updateList { it.copy(holders = v) } }
                        if (branchOptions.size > 1) MultiPickChip(
                            "Branch",
                            "branches",
                            branchOptions,
                            branches
                        ) { v -> viewModel.updateList { it.copy(branches = v) } }
                        if (ownerOptions.size > 1) MultiPickChip(
                            "Owner",
                            "owners",
                            ownerOptions,
                            owners
                        ) { v -> viewModel.updateList { it.copy(owners = v) } }
                        SortChip(
                            sort,
                            descending,
                            filter,
                            onPick = { s ->
                                if (s != sort) viewModel.updateList {
                                    it.copy(
                                        sort = s.name,
                                        descending = s.defaultDescending
                                    )
                                }
                            },
                            onFlip = {
                                viewModel.updateList {
                                    it.copy(
                                        sort = sort.name,
                                        descending = !descending
                                    )
                                }
                            },
                        )
                    }
                }
                if (shown.isEmpty()) item {
                    Text(
                        if (state.loaded) "No deposits here. Tap + to open one." else "Loading…",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(
                    shown,
                    key = { it.instrument.id }) { d -> FdRow(d) { onOpenDeposit(d.instrument.id) } }
            }
        }
    }

    if (creating) FixedDepositSheet(
        existing = null,
        defaultName = viewModel.nextName(),
        accounts = state.accounts,
        suggestions = suggestions,
        templates = state.templates,
        onCreate = { viewModel.create(it); creating = false },
        onSaveTemplate = { viewModel.saveTemplate(it) },
        onSaveTerms = { _, _ -> },
        onDismiss = { creating = false },
    )
}

/** A chip that opens a list of [options]; the picked one is ticked. */
@Composable
private fun PickChip(
    label: String,
    options: List<String>,
    selected: String,
    onPick: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = true,
            onClick = { open = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(if (o == selected) "✓ $o" else o) },
                    onClick = { onPick(o); open = false },
                )
            }
        }
    }
}

/**
 * "Sort: Maturity date" with the keys in a menu, and beside it an arrow button that flips the direction (up:
 * ascending, down: descending). A new key starts in its own natural direction.
 */
@Composable
private fun SortChip(
    sort: FdSort,
    descending: Boolean,
    filter: FdFilter,
    onPick: (FdSort) -> Unit,
    onFlip: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            FilterChip(
                selected = false,
                onClick = { open = true },
                label = { Text("Sort: ${sort.titleFor(filter)}") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                FdSort.entries.forEach { s ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (s == sort) "✓ ${s.titleFor(filter)}" else s.titleFor(
                                    filter
                                )
                            )
                        },
                        onClick = { onPick(s); open = false },
                    )
                }
            }
        }
        IconButton(onClick = onFlip) {
            // A real arrow (not a chevron, which reads as fold/unfold): the back arrow turned up or down.
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (descending) "Descending; tap for ascending" else "Ascending; tap for descending",
                modifier = Modifier.rotate(if (descending) -90f else 90f),
            )
        }
    }
}

/**
 * A chip that opens a checklist: any number of [options] can be ticked. Reads as the label when nothing is picked,
 * the value when one is, and "2 banks" when more.
 */
@Composable
private fun MultiPickChip(
    label: String,
    plural: String,
    options: List<String>,
    selected: Set<String>,
    onChange: (Set<String>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected.isNotEmpty(),
            onClick = { open = true },
            label = {
                Text(
                    when (selected.size) {
                        0 -> label
                        1 -> selected.first()
                        else -> "${selected.size} $plural"
                    }
                )
            },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Any ${label.lowercase()}") },
                leadingIcon = { Checkbox(checked = selected.isEmpty(), onCheckedChange = null) },
                onClick = { onChange(emptySet()); open = false },
            )
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o) },
                    leadingIcon = { Checkbox(checked = o in selected, onCheckedChange = null) },
                    onClick = { onChange(if (o in selected) selected - o else selected + o) },
                )
            }
        }
    }
}

@Composable
private fun FdTotals(open: List<FdView>) {
    val principal = open.sumOf { it.principal }
    val estimated = open.sumOf { it.estimated }
    val next = open.mapNotNull { it.maturityDate }.minOrNull()
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(MoneyFormat.amount(estimated), style = MaterialTheme.typography.headlineSmall)
            Row {
                StatCell("Deposited", MoneyFormat.amount(principal))
                StatCell(
                    "Accrued",
                    MoneyFormat.amount(estimated - principal),
                    estimated - principal
                )
                StatCell("Open", "${open.size}")
            }
            next?.let {
                Text(
                    "Next maturity ${it.format(SHORT_DATE)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FdRow(d: FdView, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                listOfNotNull(d.instrument.name, d.bank).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)
            )
            Text(
                MoneyFormat.amount(if (d.isOpen) d.estimated else d.position.totalRedeemed),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            listOfNotNull(
                MoneyFormat.amount(d.principal.takeIf { it > 0 } ?: d.position.totalInvested),
                d.rate?.let { "@ ${FdMath.plain(it)}%" },
                d.periodLabel,
                d.terms?.depositType?.takeIf { it == FdType.SWEEP_IN }?.title,
                d.instrument.owner.takeIf { it != Instrument.OWNER_SELF }?.let { "owner $it" },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                listOfNotNull(
                    d.depositDate?.format(SHORT_DATE),
                    d.maturityDate?.format(SHORT_DATE)
                ).joinToString(" → ").ifEmpty { "no dates" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                fdStatusText(d),
                style = MaterialTheme.typography.bodySmall,
                color = fdStatusColor(d)
            )
        }
    }
    HorizontalDivider()
}

@Composable
fun fdStatusText(d: FdView): String = when {
    d.status == FdStatus.RENEWED -> "Renewed"
    d.status == FdStatus.CLOSED_AT_MATURITY -> "Closed at maturity"
    d.status == FdStatus.CLOSED_PREMATURELY -> "Closed early"
    d.matured -> "Matured, not closed"
    d.daysLeft != null -> "${d.daysLeft} days left"
    else -> "Open"
}

@Composable
fun fdStatusColor(d: FdView) = when {
    d.matured -> MaterialTheme.colorScheme.error
    d.isOpen && (d.daysLeft ?: Long.MAX_VALUE) <= 30 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}


/**
 * New deposit (instrument, terms and the deposit row) or the terms of an existing one. Fields run in the order the
 * advice is read: template, name, paid from, holder, amount and date, period, rate, compounding, the computed maturity,
 * then the bank details, owner and notes. No TDS rate is asked for; TDS is estimated from the holder's interest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedDepositSheet(
    existing: FdView?,
    defaultName: String,
    accounts: List<Account>,
    suggestions: FdSuggestions,
    onCreate: (NewFixedDeposit) -> Unit,
    onSaveTerms: (Instrument, FixedDeposit) -> Unit,
    onDismiss: () -> Unit,
    templates: List<FdTemplate> = emptyList(),
    onSaveTemplate: ((FdTemplate) -> Unit)? = null,
) {
    val t = existing?.terms
    var name by remember { mutableStateOf(existing?.instrument?.name ?: defaultName) }
    var editingName by remember { mutableStateOf(false) }
    var fromAccount by remember {
        mutableStateOf((accounts.firstOrNull { it.accountType == com.nagabhushansn95.financetracker.data.db.entity.AccountType.BANK }
            ?: accounts.firstOrNull())?.id)
    }
    var holder by remember { mutableStateOf(t?.holderName ?: "") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var months by remember { mutableStateOf(t?.periodMonths?.toString() ?: "12") }
    var days by remember { mutableStateOf(t?.periodDays?.toString() ?: "") }
    var rate by remember { mutableStateOf(t?.rate?.let(FdMath::plain) ?: "") }
    var compounding by remember {
        mutableStateOf((t?.compoundingMonths ?: FdMath.DEFAULT_COMPOUNDING_MONTHS).toString())
    }
    var maturity by remember { mutableStateOf(t?.maturityDate) }
    var maturityTouched by remember { mutableStateOf(t?.maturityDate != null) }
    var pickingMaturity by remember { mutableStateOf(false) }
    var printed by remember { mutableStateOf(t?.maturityAmountPrinted?.let(FdMath::plain) ?: "") }
    var depositType by remember { mutableStateOf(t?.depositType ?: FdType.REGULAR) }
    var sweepInDate by remember { mutableStateOf(t?.sweepInDate) }
    var bank by remember { mutableStateOf(t?.bankName ?: "") }
    var branch by remember { mutableStateOf(t?.branch ?: "") }
    var certificate by remember { mutableStateOf(t?.certificateNo ?: "") }
    var accountNo by remember { mutableStateOf(t?.bankAccountNo ?: "") }
    var custId by remember { mutableStateOf(t?.custId ?: "") }
    var owner by remember { mutableStateOf(existing?.instrument?.owner ?: Instrument.OWNER_SELF) }
    var notes by remember { mutableStateOf(existing?.instrument?.notes ?: t?.notes ?: "") }
    var template by remember { mutableStateOf<FdTemplate?>(null) }
    var namingTemplate by remember { mutableStateOf(false) }

    /** Fills only what the template has; everything it leaves empty stays as typed. */
    fun applyTemplate(tpl: FdTemplate) {
        template = tpl
        tpl.bankName?.let { bank = it }
        tpl.branch?.let { branch = it }
        tpl.holderName?.let { holder = it }
        tpl.custId?.let { custId = it }
        tpl.amount?.let { amount = FdMath.plain(it) }
        tpl.fromAccountId?.let { id -> if (accounts.any { it.id == id }) fromAccount = id }
        if (tpl.periodMonths != null || tpl.periodDays != null) {
            months = tpl.periodMonths?.toString() ?: ""
            days = tpl.periodDays?.toString() ?: ""
        }
        tpl.rate?.let { rate = FdMath.plain(it) }
        tpl.compoundingMonths?.let { compounding = it.toString() }
        tpl.owner?.let { owner = it }
        tpl.notes?.let { notes = it }
    }

    val amountValue = amount.trim().toDoubleOrNull()

    fun asTemplate(name: String) = FdTemplate(
        name = name.trim(),
        bankName = bank.trim().ifBlank { null },
        branch = branch.trim().ifBlank { null },
        holderName = holder.trim().ifBlank { null },
        custId = custId.trim().ifBlank { null },
        amount = if (existing == null) amountValue?.takeIf { it > 0 }
        else (existing.principal.takeIf { it > 0 }
            ?: existing.position.totalInvested).takeIf { it > 0 },
        fromAccountId = if (existing == null) fromAccount
        else existing.rows.firstOrNull { it.kind == com.nagabhushansn95.financetracker.data.db.entity.TransactionKind.INVEST }?.fromAccountId,
        periodMonths = months.toIntOrNull()?.takeIf { it > 0 },
        periodDays = days.toIntOrNull()?.takeIf { it > 0 },
        rate = rate.toDoubleOrNull(),
        compoundingMonths = compounding.toIntOrNull()?.takeIf { it > 0 },
        owner = owner.trim().ifBlank { null },
        notes = notes.trim().ifBlank { null },
    )

    /** What differs from the picked template, in the template's own fields (the amount is handled apart). */
    fun templateChanges(tpl: FdTemplate): List<String> {
        fun str(a: String?) = a?.trim().orEmpty()
        val out = ArrayList<String>()
        tpl.bankName?.let {
            if (str(it) != str(bank)) out += "Bank: $it → ${
                bank.trim().ifBlank { "—" }
            }"
        }
        tpl.branch?.let {
            if (str(it) != str(branch)) out += "Branch: $it → ${
                branch.trim().ifBlank { "—" }
            }"
        }
        tpl.holderName?.let {
            if (str(it) != str(holder)) out += "Holder: $it → ${
                holder.trim().ifBlank { "—" }
            }"
        }
        tpl.custId?.let {
            if (str(it) != str(custId)) out += "Customer ID: $it → ${
                custId.trim().ifBlank { "—" }
            }"
        }
        if (tpl.periodMonths != null || tpl.periodDays != null) {
            val was = FdMath.periodLabel(tpl.periodMonths, tpl.periodDays) ?: "—"
            val now = FdMath.periodLabel(months.toIntOrNull(), days.toIntOrNull()) ?: "—"
            if (was != now) out += "Period: $was → $now"
        }
        tpl.rate?.let {
            if (it != rate.toDoubleOrNull()) out += "Rate: ${FdMath.plain(it)}% → ${
                rate.trim().ifBlank { "—" }
            }%"
        }
        tpl.compoundingMonths?.let {
            if (it != compounding.toIntOrNull()) out += "Compounds every: $it → ${
                compounding.trim().ifBlank { "—" }
            } months"
        }
        tpl.fromAccountId?.let { id ->
            if (existing == null && id != fromAccount) out += "Paid from: ${accounts.firstOrNull { it.id == id }?.name ?: "?"} → ${accounts.firstOrNull { it.id == fromAccount }?.name ?: "no account"}"
        }
        tpl.owner?.let {
            if (str(it) != str(owner)) out += "Owner: $it → ${
                owner.trim().ifBlank { "—" }
            }"
        }
        tpl.notes?.let { if (str(it) != str(notes)) out += "Notes changed" }
        return out
    }

    fun amountDiffers(tpl: FdTemplate) =
        existing == null && tpl.amount != null && tpl.amount != amountValue

    /** The template with only the fields it already holds brought up to date (plus the amount when asked). */
    fun updatedTemplate(tpl: FdTemplate, alsoAmount: Boolean): FdTemplate {
        val fresh = asTemplate(tpl.name)
        return tpl.copy(
            bankName = if (tpl.bankName != null) fresh.bankName else null,
            branch = if (tpl.branch != null) fresh.branch else null,
            holderName = if (tpl.holderName != null) fresh.holderName else null,
            custId = if (tpl.custId != null) fresh.custId else null,
            periodMonths = if (tpl.periodMonths != null || tpl.periodDays != null) fresh.periodMonths else null,
            periodDays = if (tpl.periodMonths != null || tpl.periodDays != null) fresh.periodDays else null,
            rate = if (tpl.rate != null) fresh.rate else null,
            compoundingMonths = if (tpl.compoundingMonths != null) fresh.compoundingMonths else null,
            fromAccountId = if (tpl.fromAccountId != null && existing == null) fresh.fromAccountId else tpl.fromAccountId,
            owner = if (tpl.owner != null) fresh.owner else null,
            notes = if (tpl.notes != null) fresh.notes else null,
            amount = if (alsoAmount) fresh.amount else tpl.amount,
        )
    }

    var confirmingChanges by remember { mutableStateOf<List<String>?>(null) }

    val depositDate = existing?.depositDate ?: date
    val computedMaturity =
        depositDate?.let { FdMath.maturityDate(it, months.toIntOrNull(), days.toIntOrNull()) }
    val shownMaturity = if (maturityTouched) maturity else computedMaturity
    val canSave =
        name.isNotBlank() && (existing != null || (amountValue != null && amountValue > 0 && date != null))

    fun terms() = FixedDeposit(
        instrumentId = existing?.instrument?.id ?: 0,
        certificateNo = certificate.trim().ifBlank { null },
        holderName = holder.trim().ifBlank { null },
        bankName = bank.trim().ifBlank { null },
        branch = branch.trim().ifBlank { null },
        bankAccountNo = accountNo.trim().ifBlank { null },
        custId = custId.trim().ifBlank { null },
        periodMonths = months.toIntOrNull()?.takeIf { it > 0 },
        periodDays = days.toIntOrNull()?.takeIf { it > 0 },
        rate = rate.toDoubleOrNull(),
        compoundingMonths = compounding.toIntOrNull()?.takeIf { it > 0 }
            ?: FdMath.DEFAULT_COMPOUNDING_MONTHS,
        maturityDate = shownMaturity,
        maturityAmountPrinted = printed.toDoubleOrNull(),
        tdsRate = t?.tdsRate,
        tdsReported = t?.tdsReported,
        tdsYearly = t?.tdsYearly,
        status = t?.status ?: FdStatus.ACTIVE,
        depositType = depositType,
        sweepInDate = if (depositType == FdType.SWEEP_IN) sweepInDate else null,
        previousFdId = t?.previousFdId,
        nextFdId = t?.nextFdId,
        notes = t?.notes,
    )

    fun save() {
        if (existing == null) onCreate(
            NewFixedDeposit(
                name = name,
                amount = amountValue!!,
                date = date!!,
                fromAccountId = fromAccount,
                terms = terms(),
                owner = owner.trim().ifBlank { Instrument.OWNER_SELF },
                categoryId = null,
                notes = notes.ifBlank { null },
            )
        ) else onSaveTerms(
            existing.instrument.copy(
                name = name.trim(),
                owner = owner.trim().ifBlank { Instrument.OWNER_SELF },
                notes = notes.ifBlank { null }),
            terms(),
        )
    }

    FormSheet(
        title = if (existing == null) "New fixed deposit" else "Deposit terms",
        onDismiss = onDismiss,
    ) {
        if (existing == null && templates.isNotEmpty()) ChoiceDropdown(
            "Template",
            listOf<FdTemplate?>(null) + templates,
            template,
            { it?.name ?: "None" },
        ) { picked -> if (picked == null) template = null else applyTemplate(picked) }
        if (editingName) OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            trailingIcon = { TextButton(onClick = { editingName = false }) { Text("Done") } },
            modifier = Modifier.fillMaxWidth(),
        ) else LabelRow("Name", name.ifBlank { "—" }) { editingName = true }
        if (existing == null) AccountField(
            "Paid from",
            accounts,
            fromAccount,
            { fromAccount = it },
            Modifier.fillMaxWidth(),
            none = "No account of ours",
        )
        AutocompleteField(
            holder,
            "Holder",
            suggestions.holders,
            { holder = it },
            Modifier.fillMaxWidth()
        )
        if (existing == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(amount, "Deposit amount", { amount = it }, Modifier.weight(1f))
            DateField("Deposit date", date, { date = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(months, "Months", { months = it }, Modifier.weight(1f))
            NumberField(days, "Days", { days = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(rate, "Rate %", { rate = it }, Modifier.weight(1f))
            NumberField(
                compounding,
                "Compounds every (months)",
                { compounding = it },
                Modifier.weight(1f)
            )
        }
        ChoiceDropdown("Type", FdType.entries, depositType, { it.title }) { depositType = it }
        if (depositType == FdType.SWEEP_IN) DateField(
            "Sweep-in from (optional)",
            sweepInDate,
            { sweepInDate = it },
            Modifier.fillMaxWidth()
        )
        LabelRow(
            if (maturityTouched) "Maturity date (set by hand)" else "Maturity date",
            shownMaturity?.format(SHORT_DATE) ?: "Enter the period above",
        ) { pickingMaturity = true }
        if (maturityTouched && computedMaturity != null) TextButton(onClick = {
            maturity = null; maturityTouched = false
        }) {
            Text("Use the computed date, ${computedMaturity.format(SHORT_DATE)}")
        }
        NumberField(
            printed,
            "Printed maturity amount (optional)",
            { printed = it },
            Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutocompleteField(bank, "Bank", suggestions.banks, { bank = it }, Modifier.weight(1f))
            AutocompleteField(
                branch,
                "Branch",
                suggestions.branches,
                { branch = it },
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = certificate,
                onValueChange = { certificate = it },
                label = { Text("Certificate no") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = accountNo,
                onValueChange = { accountNo = it },
                label = { Text("FD account no") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutocompleteField(
                custId,
                "Customer ID",
                suggestions.custIds,
                { custId = it },
                Modifier.weight(1f)
            )
            AutocompleteField(
                owner,
                "Owner",
                suggestions.owners,
                { owner = it },
                Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(enabled = canSave, modifier = Modifier.fillMaxWidth(), onClick = {
            // Opened from a template that no longer matches: offer to bring the template up to date first.
            val tpl = template
            val changes =
                if (tpl != null && onSaveTemplate != null) templateChanges(tpl) else emptyList()
            if (changes.isNotEmpty()) confirmingChanges = changes else save()
        }) { Text(if (existing == null) "Open deposit" else "Save") }
        if (onSaveTemplate != null) TextButton(
            onClick = { namingTemplate = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save these details as a template…") }
    }

    if (pickingMaturity) PickDateDialog(
        initial = shownMaturity ?: depositDate ?: LocalDate.now(),
        onDismiss = { pickingMaturity = false },
    ) { maturity = it; maturityTouched = true; pickingMaturity = false }

    confirmingChanges?.let { changes ->
        val tpl = template!!
        var alsoAmount by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { confirmingChanges = null; save() },
            title = { Text("Update the template \"${tpl.name}\"?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "These details differ from the template:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    changes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    if (amountDiffers(tpl)) Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { alsoAmount = !alsoAmount },
                    ) {
                        Checkbox(checked = alsoAmount, onCheckedChange = { alsoAmount = it })
                        Text(
                            "Also the amount: ${tpl.amount?.let(MoneyFormat::amount)} → ${
                                amountValue?.let(
                                    MoneyFormat::amount
                                ) ?: "—"
                            }"
                        )
                    }
                    Text(
                        "The deposit is opened either way.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveTemplate?.invoke(updatedTemplate(tpl, alsoAmount)); confirmingChanges =
                    null; save()
                }) { Text("Update template") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmingChanges = null; save()
                }) { Text("Keep template") }
            },
        )
    }

    if (namingTemplate) {
        val picked = template
        var replace by remember { mutableStateOf(picked != null) }
        var templateName by remember { mutableStateOf(picked?.name ?: bank.trim()) }
        val nameTaken =
            templates.any { it.name.trim().equals(templateName.trim(), ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { namingTemplate = false },
            title = { Text(if (picked != null) "Save as a template" else "Template name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Keeps everything typed here except the name, the dates, the certificate and the FD account number. Edit or delete it under Manage › Templates.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (picked != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { replace = true }) {
                            androidx.compose.material3.RadioButton(
                                selected = replace,
                                onClick = { replace = true })
                            Text("Replace \"${picked.name}\"")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { replace = false }) {
                            androidx.compose.material3.RadioButton(
                                selected = !replace,
                                onClick = { replace = false })
                            Text("New template")
                        }
                    }
                    if (!replace) OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        isError = nameTaken,
                        supportingText = if (nameTaken) ({ Text("A template with this name exists") }) else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = replace || (templateName.isNotBlank() && !nameTaken),
                    onClick = {
                        onSaveTemplate?.invoke(
                            if (replace && picked != null) asTemplate(picked.name).copy(
                                id = picked.id,
                                displayOrder = picked.displayOrder
                            )
                            else asTemplate(templateName)
                        )
                        namingTemplate = false
                    }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { namingTemplate = false }) { Text("Cancel") } },
        )
    }
}

/** Add or edit a fixed deposit template (Manage › Templates › Fixed deposits). Every field is optional. */
@Composable
fun FdTemplateSheet(
    existing: FdTemplate?,
    suggestions: FdSuggestions,
    accounts: List<Account>,
    onSave: (FdTemplate) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    /** Names of the other templates; a name among them (ignoring case) cannot be saved. */
    otherNames: Collection<String> = emptyList(),
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    val nameTaken = otherNames.any { it.trim().equals(name.trim(), ignoreCase = true) }
    var bank by remember { mutableStateOf(existing?.bankName ?: "") }
    var branch by remember { mutableStateOf(existing?.branch ?: "") }
    var holder by remember { mutableStateOf(existing?.holderName ?: "") }
    var custId by remember { mutableStateOf(existing?.custId ?: "") }
    var amount by remember { mutableStateOf(existing?.amount?.let(FdMath::plain) ?: "") }
    var fromAccount by remember { mutableStateOf(existing?.fromAccountId) }
    var months by remember { mutableStateOf(existing?.periodMonths?.toString() ?: "") }
    var days by remember { mutableStateOf(existing?.periodDays?.toString() ?: "") }
    var rate by remember { mutableStateOf(existing?.rate?.let(FdMath::plain) ?: "") }
    var compounding by remember { mutableStateOf(existing?.compoundingMonths?.toString() ?: "") }
    var owner by remember { mutableStateOf(existing?.owner ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    FormSheet(
        title = if (existing == null) "New deposit template" else "Edit deposit template",
        onDismiss = onDismiss
    ) {
        Text(
            "What \"New fixed deposit\" fills in when this template is picked. Leave blank whatever changes from one deposit to the next.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Template name") },
            singleLine = true,
            isError = nameTaken,
            supportingText = if (nameTaken) ({ Text("A template with this name exists") }) else null,
            modifier = Modifier.fillMaxWidth()
        )
        AccountField(
            "Paid from",
            accounts,
            fromAccount,
            { fromAccount = it },
            Modifier.fillMaxWidth(),
            none = "Not set"
        )
        AutocompleteField(
            holder,
            "Holder",
            suggestions.holders,
            { holder = it },
            Modifier.fillMaxWidth()
        )
        NumberField(amount, "Deposit amount", { amount = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(months, "Months", { months = it }, Modifier.weight(1f))
            NumberField(days, "Days", { days = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(rate, "Rate %", { rate = it }, Modifier.weight(1f))
            NumberField(
                compounding,
                "Compounds every (months)",
                { compounding = it },
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutocompleteField(bank, "Bank", suggestions.banks, { bank = it }, Modifier.weight(1f))
            AutocompleteField(
                branch,
                "Branch",
                suggestions.branches,
                { branch = it },
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutocompleteField(
                custId,
                "Customer ID",
                suggestions.custIds,
                { custId = it },
                Modifier.weight(1f)
            )
            AutocompleteField(
                owner,
                "Owner",
                suggestions.owners,
                { owner = it },
                Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = name.isNotBlank() && !nameTaken,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    (existing ?: FdTemplate(name = "")).copy(
                        name = name.trim(),
                        bankName = bank.trim().ifBlank { null },
                        branch = branch.trim().ifBlank { null },
                        holderName = holder.trim().ifBlank { null },
                        custId = custId.trim().ifBlank { null },
                        amount = amount.toDoubleOrNull()?.takeIf { it > 0 },
                        fromAccountId = fromAccount,
                        periodMonths = months.toIntOrNull()?.takeIf { it > 0 },
                        periodDays = days.toIntOrNull()?.takeIf { it > 0 },
                        rate = rate.toDoubleOrNull(),
                        compoundingMonths = compounding.toIntOrNull()?.takeIf { it > 0 },
                        owner = owner.trim().ifBlank { null },
                        notes = notes.trim().ifBlank { null },
                    )
                )
            }) { Text("Save") }
        if (onDelete != null) OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete template", color = MaterialTheme.colorScheme.error)
        }
    }
}
