package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.FdStatus
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.investments.FdMath
import com.nagabhushansn95.financetracker.data.investments.FdView
import com.nagabhushansn95.financetracker.data.investments.FixedDeposits
import com.nagabhushansn95.financetracker.data.repository.FixedDepositRepository
import com.nagabhushansn95.financetracker.data.repository.NewFixedDeposit
import com.nagabhushansn95.financetracker.ui.common.AccountDropdown
import com.nagabhushansn95.financetracker.ui.common.ChoiceDropdown
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.NumberField
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
            db.instrumentDao().observeCategories()
        ) { a, c -> a to c },
    ) { instruments, terms, ledger, valuations, (accounts, categories) ->
        FixedDepositsState(
            deposits = FixedDeposits.build(instruments, terms, ledger, valuations, LocalDate.now()),
            accounts = accounts.filter { it.isCashAccount },
            categories = categories,
            lookups = LedgerLookups(accounts, emptyList(), instruments),
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FixedDepositsState())

    fun nextName() = FixedDeposits.nextName(state.value.deposits.map { it.instrument })

    fun create(new: NewFixedDeposit) = run { repo.create(new); "Deposit opened" }
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
        terms: FixedDeposit
    ) =
        run {
            repo.renew(
                oldId,
                date,
                matured,
                renewed,
                remainderTo,
                newName,
                terms
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

enum class FdFilter(val title: String) {
    ACTIVE("Active"), DUE("Matured, not closed"), RENEWED("Renewed"), CLOSED(
        "Closed"
    ),
    ALL("All")
}

enum class FdSort(val title: String) {
    MATURITY("Maturity date"), DEPOSIT("Deposit date"), AMOUNT("Amount"), VALUE(
        "Est. value"
    ),
    BANK("Bank"), NAME("Name"), DAYS_LEFT("Days left")
}

/** Fixed deposits: active by default, with status / bank / owner filters, sorting, and "New fixed deposit". */
@OptIn(ExperimentalMaterial3Api::class)
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

    var filter by rememberSaveable { mutableStateOf(FdFilter.ACTIVE) }
    var sort by rememberSaveable { mutableStateOf(FdSort.MATURITY) }
    var bank by rememberSaveable { mutableStateOf<String?>(null) }
    var owner by rememberSaveable { mutableStateOf<String?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    var bankMenu by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }

    val all = state.deposits
    val banks = all.mapNotNull { it.bank }.distinct().sorted()
    val owners = all.map { it.instrument.owner }.distinct().sorted()
    val shown = all.filter { d ->
        when (filter) {
            FdFilter.ACTIVE -> d.isOpen
            FdFilter.DUE -> d.matured
            FdFilter.RENEWED -> d.status == FdStatus.RENEWED
            FdFilter.CLOSED -> d.status == FdStatus.CLOSED_AT_MATURITY || d.status == FdStatus.CLOSED_PREMATURELY
            FdFilter.ALL -> true
        } && (bank == null || d.bank == bank) && (owner == null || d.instrument.owner == owner)
    }.sortedWith(
        when (sort) {
            FdSort.MATURITY -> compareBy<FdView, LocalDate?>(nullsLast()) { it.maturityDate }.thenBy { it.instrument.id }
            FdSort.DEPOSIT -> compareByDescending<FdView, LocalDate?>(nullsLast()) { it.depositDate }.thenByDescending { it.instrument.id }
            FdSort.AMOUNT -> compareByDescending { it.principal }
            FdSort.VALUE -> compareByDescending { it.estimated }
            FdSort.BANK -> compareBy<FdView, String?>(nullsLast()) { it.bank }.thenBy { it.maturityDate }
            FdSort.NAME -> compareBy { it.instrument.name }
            FdSort.DAYS_LEFT -> compareBy<FdView, Long?>(nullsLast()) { it.daysLeft }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fixed deposits") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Sort"
                        )
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        FdSort.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(if (s == sort) "✓ ${s.title}" else s.title) },
                                onClick = { sort = s; sortMenu = false })
                        }
                    }
                },
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
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FdFilter.entries.forEach { f ->
                            if (f != FdFilter.DUE || all.any { it.matured }) FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                label = { Text(f.title) })
                        }
                    }
                    if (banks.size > 1 || owners.size > 1) Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (banks.size > 1) {
                            AssistChip(
                                onClick = { bankMenu = true },
                                label = { Text(bank ?: "All banks") })
                            DropdownMenu(
                                expanded = bankMenu,
                                onDismissRequest = { bankMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("All banks") },
                                    onClick = { bank = null; bankMenu = false })
                                banks.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b) },
                                        onClick = { bank = b; bankMenu = false })
                                }
                            }
                        }
                        if (owners.size > 1) owners.forEach { o ->
                            FilterChip(
                                selected = owner == o,
                                onClick = { owner = if (owner == o) null else o },
                                label = { Text(o) })
                        }
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
        categories = state.categories,
        onCreate = { viewModel.create(it); creating = false },
        onSaveTerms = { _, _ -> },
        onDismiss = { creating = false },
    )
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
                StatCell("Deposited", MoneyFormat.compact(principal))
                StatCell(
                    "Accrued",
                    MoneyFormat.compact(estimated - principal),
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
    d.status == FdStatus.CLOSED_AT_MATURITY -> "Closed"
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

/** New deposit (instrument, terms and the deposit row) or the terms of an existing one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedDepositSheet(
    existing: FdView?,
    defaultName: String,
    accounts: List<Account>,
    categories: List<InvestmentCategory>,
    onCreate: (NewFixedDeposit) -> Unit,
    onSaveTerms: (Instrument, FixedDeposit) -> Unit,
    onDismiss: () -> Unit,
) {
    val t = existing?.terms
    var name by remember { mutableStateOf(existing?.instrument?.name ?: defaultName) }
    var bank by remember { mutableStateOf(t?.bankName ?: "") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var fromAccount by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var months by remember { mutableStateOf(t?.periodMonths?.toString() ?: "12") }
    var days by remember { mutableStateOf(t?.periodDays?.toString() ?: "") }
    var rate by remember { mutableStateOf(t?.rate?.let(FdMath::plain) ?: "") }
    var compounding by remember {
        mutableStateOf(
            (t?.compoundingMonths ?: FdMath.DEFAULT_COMPOUNDING_MONTHS).toString()
        )
    }
    var maturity by remember { mutableStateOf(t?.maturityDate) }
    var maturityTouched by remember { mutableStateOf(t?.maturityDate != null) }
    var printed by remember { mutableStateOf(t?.maturityAmountPrinted?.let(FdMath::plain) ?: "") }
    var tdsRate by remember {
        mutableStateOf(t?.tdsRate?.let { FdMath.plain(if (it <= 1.0) it * 100 else it) } ?: "")
    }
    var certificate by remember { mutableStateOf(t?.certificateNo ?: "") }
    var holder by remember { mutableStateOf(t?.holderName ?: "") }
    var accountNo by remember { mutableStateOf(t?.bankAccountNo ?: "") }
    var custId by remember { mutableStateOf(t?.custId ?: "") }
    var branch by remember { mutableStateOf(t?.branch ?: "") }
    var owner by remember { mutableStateOf(existing?.instrument?.owner ?: Instrument.OWNER_SELF) }
    var categoryId by remember { mutableStateOf(existing?.instrument?.categoryId) }
    var notes by remember { mutableStateOf(existing?.instrument?.notes ?: t?.notes ?: "") }

    val depositDate = existing?.depositDate ?: date
    val computedMaturity =
        depositDate?.let { FdMath.maturityDate(it, months.toIntOrNull(), days.toIntOrNull()) }
    val shownMaturity = if (maturityTouched) maturity else computedMaturity
    val amountValue = amount.trim().toDoubleOrNull()
    val canSave =
        name.isNotBlank() && (existing != null || (amountValue != null && amountValue > 0 && date != null))

    fun terms() = FixedDeposit(
        instrumentId = existing?.instrument?.id ?: 0,
        certificateNo = certificate.ifBlank { null },
        holderName = holder.ifBlank { null },
        bankName = bank.trim().ifBlank { null },
        branch = branch.ifBlank { null },
        bankAccountNo = accountNo.ifBlank { null },
        custId = custId.ifBlank { null },
        periodMonths = months.toIntOrNull()?.takeIf { it > 0 },
        periodDays = days.toIntOrNull()?.takeIf { it > 0 },
        rate = rate.toDoubleOrNull(),
        compoundingMonths = compounding.toIntOrNull()?.takeIf { it > 0 }
            ?: FdMath.DEFAULT_COMPOUNDING_MONTHS,
        maturityDate = shownMaturity,
        maturityAmountPrinted = printed.toDoubleOrNull(),
        tdsRate = tdsRate.toDoubleOrNull()?.let { if (it > 1.0) it / 100 else it },
        tdsReported = t?.tdsReported,
        status = t?.status ?: FdStatus.ACTIVE,
        previousFdId = t?.previousFdId,
        nextFdId = t?.nextFdId,
        notes = t?.notes,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (existing == null) "New fixed deposit" else "Deposit terms",
                style = MaterialTheme.typography.titleLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("Bank") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            if (existing == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(amount, "Amount", { amount = it }, Modifier.weight(1f))
                    DateField("Deposit date", date, { date = it }, Modifier.weight(1f))
                }
                AccountDropdown(
                    "Paid from",
                    accounts,
                    fromAccount,
                    { fromAccount = it },
                    none = "No account of ours"
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(months, "Months", { months = it }, Modifier.weight(1f))
                NumberField(days, "Days", { days = it }, Modifier.weight(1f))
                NumberField(rate, "Rate %", { rate = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(
                    "Maturity date",
                    shownMaturity,
                    { maturity = it; maturityTouched = it != null },
                    Modifier.weight(1f),
                    clearable = maturityTouched
                )
                NumberField(
                    compounding,
                    "Compounds every (months)",
                    { compounding = it },
                    Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    printed,
                    "Maturity amount (printed)",
                    { printed = it },
                    Modifier.weight(1f)
                )
                NumberField(tdsRate, "TDS %", { tdsRate = it }, Modifier.weight(1f))
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
                OutlinedTextField(
                    value = holder,
                    onValueChange = { holder = it },
                    label = { Text("Holder") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = custId,
                    onValueChange = { custId = it },
                    label = { Text("Customer ID") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Branch") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Owner") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            if (categories.isNotEmpty()) {
                val choices =
                    listOf<InvestmentCategory?>(null) + categories.filter { it.parentId == null }
                        .flatMap { p -> listOf(p) + categories.filter { it.parentId == p.id } }
                val byId = categories.associateBy { it.id }
                ChoiceDropdown(
                    "Category",
                    choices,
                    choices.firstOrNull { it?.id == categoryId },
                    { c ->
                        c?.let { x ->
                            x.parentId?.let { byId[it]?.name }?.let { "$it › ${x.name}" } ?: x.name
                        } ?: "By type (Fixed Deposits)"
                    }) { categoryId = it?.id }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(enabled = canSave, modifier = Modifier.fillMaxWidth(), onClick = {
                if (existing == null) onCreate(
                    NewFixedDeposit(
                        name = name,
                        amount = amountValue!!,
                        date = date!!,
                        fromAccountId = fromAccount,
                        terms = terms(),
                        owner = owner.trim().ifBlank { Instrument.OWNER_SELF },
                        categoryId = categoryId,
                        notes = notes.ifBlank { null },
                    )
                ) else onSaveTerms(
                    existing.instrument.copy(
                        name = name.trim(),
                        owner = owner.trim().ifBlank { Instrument.OWNER_SELF },
                        categoryId = categoryId,
                        notes = notes.ifBlank { null }),
                    terms(),
                )
            }) { Text(if (existing == null) "Open deposit" else "Save") }
        }
    }
}
