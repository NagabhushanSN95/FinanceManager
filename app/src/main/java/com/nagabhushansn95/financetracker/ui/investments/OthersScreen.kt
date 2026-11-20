package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.FdMath
import com.nagabhushansn95.financetracker.data.investments.OtherEntryKind
import com.nagabhushansn95.financetracker.data.investments.OtherView
import com.nagabhushansn95.financetracker.data.investments.Others
import com.nagabhushansn95.financetracker.data.repository.OthersRepository
import com.nagabhushansn95.financetracker.ui.common.AccountField
import com.nagabhushansn95.financetracker.ui.common.BackAndMenu
import com.nagabhushansn95.financetracker.ui.common.ConfirmDeleteEntryDialog
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.NumberField
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.common.StatCell
import com.nagabhushansn95.financetracker.ui.theme.appTopBarColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class OthersState(
    val views: List<OtherView> = emptyList(),
    val accounts: Map<Long, Account> = emptyMap(),
    val banks: List<Account> = emptyList(),
    val loaded: Boolean = false,
) {
    fun accountName(id: Long?): String = id?.let { accounts[it]?.name } ?: "—"
}

class OthersViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = OthersRepository(db)

    /** One-off outcome of a save or delete, shown in a snackbar. */
    val message = MutableStateFlow<String?>(null)
    fun messageShown() {
        message.value = null
    }

    val state: StateFlow<OthersState> = combine(
        db.instrumentDao().observeAll(),
        db.transactionDao().observeWithInstrument(),
        db.accountDao().observeAll(),
    ) { instruments, ledger, accounts ->
        OthersState(
            views = Others.build(instruments, ledger),
            accounts = accounts.associateBy { it.id },
            banks = accounts.filter { it.accountType != AccountType.PROVIDENT_FUND && !it.deleted },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OthersState())

    fun create(name: String, notes: String?, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.add(name, notes) }
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
        view: OtherView,
        kind: OtherEntryKind,
        date: LocalDate,
        amount: Double,
        particulars: String,
        bankAccountId: Long,
        existing: TransactionEntity?,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.saveEntry(
                    view,
                    kind,
                    date,
                    amount,
                    particulars,
                    bankAccountId,
                    existing
                )
            }
                .onFailure { message.value = it.message ?: "Could not save the entry" }
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
}

/**
 * Loans and money parked elsewhere: one row each with what is still out, settled ones folded away behind a toggle in
 * the top bar, and a + to start a new one (which then opens so its first entry can be recorded).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OthersScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: OthersViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showSettled by rememberSaveable { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }
    val own = state.views.filter { it.own }
    val open = own.filter { !it.settled }
    val settled = own.filter { it.settled }
    val family = state.views.filter { !it.own }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
                title = { Text("Others") },
                navigationIcon = { BackAndMenu(onBack) },
                actions = {
                    if (settled.isNotEmpty()) TextButton(onClick = { showSettled = !showSettled }) {
                        Text(if (showSettled) "Hide settled" else "Show settled")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New loan or parked account")
            }
        },
    ) { padding ->
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
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            MoneyFormat.amount(open.sumOf { it.outstanding }),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Row {
                            StatCell("Put in", MoneyFormat.amount(own.sumOf { it.putIn }))
                            StatCell("Taken back", MoneyFormat.amount(own.sumOf { it.takenBack }))
                            StatCell("Settled", settled.size.toString())
                        }
                    }
                }
                Text(
                    "Loans and money parked elsewhere: savings rather than investments. They count in the Savings tables but not under Total investments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (state.loaded && state.views.isEmpty()) item {
                Text(
                    "Nothing here yet. Use + to add a loan or an account you keep money in.",
                    modifier = Modifier.padding(16.dp)
                )
            }
            val sections = listOf(
                "Open" to open,
                "Settled" to if (showSettled) settled else emptyList(),
                "Family (not in the totals)" to family,
            ).filter { it.second.isNotEmpty() }
            sections.forEach { (title, list) ->
                item(key = "s:$title") { OthersSectionTitle(title) }
                items(
                    list,
                    key = { it.instrument.id }) { v -> OtherRow(v) { onOpen(v.instrument.id) } }
            }
            if (!showSettled && settled.isNotEmpty() && open.isEmpty()) item {
                Text(
                    "Everything is settled. Show settled from the top bar to see it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
    if (creating) OtherSheet(
        existing = null,
        onSave = { name, notes -> creating = false; viewModel.create(name, notes) { onOpen(it) } },
        onDismiss = { creating = false },
    )
}

@Composable
private fun OthersSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    HorizontalDivider()
}

/** Name and what is still out; below, what went and came back and since when; a settled one says when. */
@Composable
private fun OtherRow(v: OtherView, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                v.instrument.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (v.settled) Text(
                "Settled",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            ) else Text(
                MoneyFormat.amount(v.outstanding),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            otherSummaryLine(v),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}

private fun otherSummaryLine(v: OtherView): String {
    if (v.rows.isEmpty()) return "No entries yet"
    val parts = ArrayList<String>()
    parts += "${v.outLabel} ${MoneyFormat.amount(v.putIn)}"
    if (v.takenBack > 0) parts += "${v.inLabel.lowercase()} ${MoneyFormat.amount(v.takenBack)}"
    parts += if (v.settled) "${v.firstDate?.format(SHORT_DATE)} to ${v.lastDate?.format(SHORT_DATE)}"
    else "since ${v.firstDate?.format(SHORT_DATE)}"
    if (!v.own) parts += "owner ${v.instrument.owner}"
    return parts.joinToString(" · ")
}

/**
 * One loan or parked account: what is out, what went and came back, and the ledger (date and bank account under each
 * entry's particulars). The + records money out or in; press and hold an entry to change or delete it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OtherScreen(instrumentId: Long, onBack: () -> Unit, viewModel: OthersViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val v = state.views.firstOrNull { it.instrument.id == instrumentId }
    var adding by remember { mutableStateOf<OtherEntryKind?>(null) }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    var confirmDeleteEntry by remember { mutableStateOf<TransactionEntity?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
                title = { Text(v?.instrument?.name ?: "Others") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (v != null) {
                        IconButton(onClick = { renaming = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { deleting = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (v != null) FloatingActionButton(onClick = { adding = Others.likelyNext(v) }) {
                Icon(Icons.Filled.Add, contentDescription = "Record an entry")
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
                            if (v.settled && v.rows.isNotEmpty()) "Settled" else MoneyFormat.amount(
                                v.outstanding
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (v.settled && v.rows.isNotEmpty()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                        )
                        Row {
                            StatCell(v.outLabel, MoneyFormat.amount(v.putIn))
                            StatCell(v.inLabel, MoneyFormat.amount(v.takenBack))
                            StatCell("Since", v.firstDate?.format(SHORT_DATE) ?: "—")
                        }
                        if (v.settled && v.rows.isNotEmpty()) Text(
                            "Everything came back by ${v.lastDate?.format(SHORT_DATE)}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        listOfNotNull(
                            v.instrument.owner.takeIf { it != Instrument.OWNER_SELF }
                                ?.let { "Owner $it" },
                            v.instrument.notes?.takeIf { it.isNotBlank() },
                        ).forEach {
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
                    "No entries yet. Use + to record the money going out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(v.rows.asReversed(), key = { it.id }) { t ->
                val incoming = t.kind == TransactionKind.REDEEM
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = {}, onLongClick = { editing = t })
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.particulars, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${t.date.format(SHORT_DATE)} · ${state.accountName(t.fromAccountId ?: t.toAccountId)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        MoneyFormat.amount(t.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (incoming) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                HorizontalDivider()
            }
        }
    }

    val sheetKind = adding ?: editing?.let { Others.entryKindOf(it) }
    if (v != null && sheetKind != null) OtherEntrySheet(
        view = v,
        banks = state.banks,
        defaultBankId = (editing ?: v.rows.lastOrNull())?.let { it.fromAccountId ?: it.toAccountId }
            ?: state.views.flatMap { it.rows }.maxWithOrNull(compareBy({ it.date }, { it.id }))
                ?.let { it.fromAccountId ?: it.toAccountId },
        initialKind = sheetKind,
        existing = editing,
        onSave = { kind, date, amount, particulars, bank ->
            viewModel.saveEntry(v, kind, date, amount, particulars, bank, editing); adding =
            null; editing = null
        },
        onDelete = editing?.let { row -> { confirmDeleteEntry = row } },
        onDismiss = { adding = null; editing = null },
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
    // A row the sheet cannot describe (an income tagged with it, say) is left alone rather than edited blind.
    LaunchedEffect(editing) {
        val row = editing ?: return@LaunchedEffect
        if (Others.entryKindOf(row) == null) {
            editing =
                null; snackbar.showSnackbar("This row is not a money-out or money-in entry; it stays as it is.")
        }
    }
    if (renaming && v != null) OtherSheet(
        existing = v.instrument,
        onSave = { name, notes ->
            renaming = false; viewModel.update(v.instrument.copy(name = name, notes = notes))
        },
        onDismiss = { renaming = false },
    )
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

/** Name and notes of a loan or parked account; "Loan To <person>" makes the entries read as loaned and paid back. */
@Composable
fun OtherSheet(existing: Instrument?, onSave: (String, String?) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    FormSheet(
        title = if (existing == null) "New loan or parked account" else "Edit",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            placeholder = { Text("Loan To Mahadeva Anna") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Name a loan \"Loan To <person>\": its entries then read as loaned and paid back. Anything else (an account kept for someone, money parked) reads as put in and taken back. " +
                    "The list is in name order.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSave(name.trim(), notes.trim().ifBlank { null }) },
        ) { Text(if (existing == null) "Add" else "Save") }
    }
}

/**
 * One entry: money out (lent, parked) or money in (paid back, withdrawn), the date, the amount, the bank account it
 * moves against, and the particulars (the last entry of the kind continued unless typed).
 */
@Composable
fun OtherEntrySheet(
    view: OtherView,
    banks: List<Account>,
    defaultBankId: Long?,
    initialKind: OtherEntryKind,
    existing: TransactionEntity?,
    onSave: (OtherEntryKind, LocalDate, Double, String, Long) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(initialKind) }
    var date by remember { mutableStateOf<LocalDate?>(existing?.date ?: LocalDate.now()) }
    var amount by remember { mutableStateOf(existing?.amount?.let(FdMath::plain) ?: "") }
    var amountTyped by remember { mutableStateOf(existing != null) }
    var bankId by remember { mutableStateOf(defaultBankId ?: banks.firstOrNull()?.id) }
    var particulars by remember { mutableStateOf(existing?.particulars ?: "") }
    var typed by remember { mutableStateOf(existing != null) }
    // Money coming back on an open loan starts with the last repayment (instalments repeat), else the whole balance.
    val defaultAmount: Double? = when {
        existing != null -> existing.amount
        kind == OtherEntryKind.IN -> view.rowsOf(OtherEntryKind.IN).lastOrNull()?.amount
            ?: view.outstanding.takeIf { it > Others.EPSILON }

        else -> null
    }
    val amountShown = if (amountTyped) amount else defaultAmount?.let(FdMath::plain) ?: ""
    val suggested = Others.suggestedParticulars(kind, view)
    val value = amountShown.toDoubleOrNull()
    val valid = date != null && value != null && value > 0 && bankId != null
    FormSheet(
        title = if (existing == null) kind.title else "Edit ${kind.title.lowercase()}",
        onDismiss = onDismiss
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            OtherEntryKind.entries.forEachIndexed { i, k ->
                SegmentedButton(
                    selected = kind == k,
                    onClick = { kind = k },
                    shape = SegmentedButtonDefaults.itemShape(i, OtherEntryKind.entries.size),
                    label = { Text(if (k == OtherEntryKind.OUT) view.outLabel else view.inLabel) },
                )
            }
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
        AccountField(kind.bankLabel, banks, bankId, { bankId = it }, Modifier.fillMaxWidth())
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
                OtherEntryKind.OUT -> "Leaves the account above and adds to what is out. Not an expense: it stays yours until it comes back."
                OtherEntryKind.IN -> "Comes into the account above and reduces what is out. Not income."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    kind,
                    date!!,
                    value!!,
                    if (typed) particulars else suggested,
                    bankId!!
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
