package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.CashFlow
import com.nagabhushansn95.financetracker.data.investments.Holdings
import com.nagabhushansn95.financetracker.data.investments.PfEntryKind
import com.nagabhushansn95.financetracker.data.investments.ProvidentFunds
import com.nagabhushansn95.financetracker.data.repository.TransactionRepository
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
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

/** Totals for one contributor (Employer, Employee, Self, Interest) or one financial year. */
data class PfBucket(val name: String, val amount: Double, val count: Int)

data class PfYear(val label: String, val byContributor: Map<String, Double>) {
    val total: Double get() = byContributor.values.sum()
}

data class PfAccountState(
    val account: Account? = null,
    val rows: List<TransactionEntity> = emptyList(),
    val contributors: List<PfBucket> = emptyList(),
    val years: List<PfYear> = emptyList(),
    val contributed: Double = 0.0,
    val interest: Double = 0.0,
    val withdrawn: Double = 0.0,
    val xirr: Double? = null,
    val lookups: LedgerLookups? = null,
    /** Accounts a PPF deposit can be paid from or a withdrawal paid to: everything but provident funds. */
    val banks: List<Account> = emptyList(),
    /** The bank account the last deposit or withdrawal on this account moved against: the sheet's default. */
    val lastBankId: Long? = null,
    /** Income categories by name (lower case), for the category an entry is filed under. */
    val incomeCategoryIds: Map<String, Long> = emptyMap(),
    /** The amount of the latest contribution of each kind: the sheet's starting amount for the next one. */
    val lastAmounts: Map<PfEntryKind, Double> = emptyMap(),
    val loaded: Boolean = false,
)

/** Indian financial year label for a date: April 2025 to March 2026 is "2025-26". */
fun financialYear(date: LocalDate): String = ProvidentFunds.financialYearLabel(date)

class ProvidentFundAccountViewModel(application: Application, private val accountId: Long) :
    AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = TransactionRepository(db)

    /** One-off outcome of a save or delete, shown in a snackbar. */
    val message = MutableStateFlow<String?>(null)
    fun messageShown() {
        message.value = null
    }

    init {
        // The income category entries are filed under, whichever way the screen was reached.
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                ProvidentFunds.fileCategories(
                    db
                )
            }
        }
    }

    /** Records a new entry or rewrites [existing] as the sheet describes it. */
    fun save(
        kind: PfEntryKind,
        date: LocalDate,
        amount: Double,
        particulars: String,
        bankAccountId: Long?,
        existing: TransactionEntity?,
    ) {
        val account = state.value.account ?: return
        viewModelScope.launch {
            runCatching {
                val categoryId = ProvidentFunds.categoryNameFor(kind)
                    ?.let { state.value.incomeCategoryIds[it.lowercase()] }
                val row = ProvidentFunds.toTransaction(
                    kind,
                    account,
                    date,
                    amount,
                    particulars,
                    bankAccountId,
                    categoryId,
                    existing
                )
                if (existing == null) repository.add(row) else repository.update(row)
            }.onSuccess {
                message.value = if (existing == null) "${kind.title} recorded" else "Entry saved"
            }
                .onFailure { message.value = it.message ?: it.toString() }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onSuccess { message.value = "Entry deleted" }
                .onFailure { message.value = it.message ?: it.toString() }
        }
    }

    val state: StateFlow<PfAccountState> = combine(
        db.accountDao().observeAll(),
        db.transactionDao().observeAll(),
        db.instrumentDao().observeAll(),
        db.categoryDao().observeAll(),
    ) { accounts, ledger, instruments, categories ->
        val account = accounts.firstOrNull { it.id == accountId } ?: return@combine PfAccountState(
            loaded = true
        )
        val rows = ledger.filter { it.toAccountId == accountId || it.fromAccountId == accountId }
        fun label(t: TransactionEntity) = when {
            t.fromAccountId == accountId -> "Withdrawn"
            t.kind == TransactionKind.TRANSFER -> t.contributor ?: "Self"
            else -> t.contributor ?: "Other"
        }

        val inRows = rows.filter { it.toAccountId == accountId }
        val outRows = rows.filter { it.fromAccountId == accountId }
        val order = listOf("Employer", "Employee", "Self", "Interest")
        val contributors = inRows.groupBy { label(it) }
            .map { (k, v) -> PfBucket(k, v.sumOf { it.amountInr }, v.size) }
            .sortedWith(compareBy({
                order.indexOf(it.name).let { i -> if (i < 0) order.size else i }
            }, { it.name })) +
                outRows.takeIf { it.isNotEmpty() }
                    ?.let { listOf(PfBucket("Withdrawn", -it.sumOf { r -> r.amountInr }, it.size)) }
                    .orEmpty()
        val years = rows.groupBy { financialYear(it.date) }.map { (fy, v) ->
            PfYear(
                fy,
                v.groupBy { label(it) }
                    .mapValues { (_, l) -> l.sumOf { if (it.fromAccountId == accountId) -it.amountInr else it.amountInr } })
        }.sortedByDescending { it.label }
        // Interest is a return on the account, not money paid in, so it stays out of the contributions and the XIRR flows.
        val isInterest = { t: TransactionEntity -> t.contributor == "Interest" }
        val flows = rows.filterNot(isInterest).map {
            CashFlow(
                it.date,
                if (it.toAccountId == accountId) -it.amountInr else it.amountInr
            )
        }
        PfAccountState(
            account = account,
            rows = rows.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id }),
            contributors = contributors,
            years = years,
            contributed = inRows.filterNot(isInterest).sumOf { it.amountInr },
            interest = inRows.filter(isInterest).sumOf { it.amountInr },
            withdrawn = outRows.sumOf { it.amountInr },
            xirr = Holdings.xirr(flows, account.balance, LocalDate.now()),
            lookups = LedgerLookups(accounts, categories, instruments),
            banks = accounts.filter { it.accountType != AccountType.PROVIDENT_FUND && !it.deleted },
            lastBankId = rows.filter { it.kind == TransactionKind.TRANSFER }
                .maxWithOrNull(compareBy({ it.date }, { it.id }))
                ?.let { if (it.fromAccountId == accountId) it.toAccountId else it.fromAccountId }
                ?: accounts.firstOrNull { it.accountType == AccountType.BANK && !it.deleted }?.id,
            incomeCategoryIds = categories
                .filter { !it.deleted && it.appliesTo != com.nagabhushansn95.financetracker.data.db.entity.CategoryScope.EXPENSE }
                .associate { it.name.lowercase() to it.id },
            lastAmounts = rows.sortedWith(compareBy({ it.date }, { it.id }))
                .mapNotNull { t ->
                    ProvidentFunds.entryKindOf(t, accountId)?.let { it to t.amount }
                }
                .filter { (kind, _) -> kind.contributor in ProvidentFunds.CONTRIBUTION_TAGS }
                .toMap(),
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PfAccountState())
}

/**
 * One provident-fund account: balance, contributor breakdown, financial-year table and the ledger. The "+" offers
 * the entries the account takes (see [PfEntryKind]); press-and-hold on a ledger row edits it in the same sheet.
 * Provident-fund rows are touched only here: the transaction editor never offers these accounts.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProvidentFundAccountScreen(
    accountId: Long,
    onBack: () -> Unit,
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val viewModel: ProvidentFundAccountViewModel = viewModel(
        key = "pf$accountId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ProvidentFundAccountViewModel(app, accountId) as T
        })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account = state.account
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<PfEntryKind?>(null) }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (account != null) androidx.compose.foundation.layout.Box {
                FloatingActionButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Record an entry")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    ProvidentFunds.kindsFor(account).forEach { kind ->
                        DropdownMenuItem(
                            text = { Text(kind.title) },
                            onClick = { menuOpen = false; adding = kind })
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "Provident fund") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (account == null) {
            Text(
                if (state.loaded) "Account not found." else "Loading…",
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
            contentPadding = PaddingValues(bottom = 88.dp)
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
                            MoneyFormat.amount(account.balance, account.currency),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Row {
                            StatCell("Contributed", MoneyFormat.amount(state.contributed))
                            StatCell(
                                "Interest",
                                MoneyFormat.amount(state.interest),
                                state.interest
                            )
                            if (state.withdrawn > 0) StatCell(
                                "Withdrawn",
                                MoneyFormat.amount(state.withdrawn)
                            )
                            StatCell("XIRR", pct(state.xirr))
                        }
                        Row {
                            state.contributors.filter { it.name != "Withdrawn" && it.name != "Interest" }
                                .take(4)
                                .forEach { StatCell(it.name, MoneyFormat.amount(it.amount)) }
                        }
                        account.accountNumber?.let {
                            Text(
                                "Account $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (state.years.isNotEmpty()) {
                item {
                    Text(
                        "By financial year",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    val names =
                        state.contributors.map { it.name }.filter { it != "Withdrawn" }.take(3)
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            "FY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.8f)
                        )
                        names.forEach {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            "Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider()
                }
                items(state.years, key = { it.label }) { y ->
                    val names =
                        state.contributors.map { it.name }.filter { it != "Withdrawn" }.take(3)
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(
                            y.label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.8f)
                        )
                        names.forEach { n ->
                            Text(
                                MoneyFormat.compact(y.byContributor[n] ?: 0.0),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            MoneyFormat.compact(y.total),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item {
                Text(
                    "Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider()
            }
            items(state.rows, key = { it.id }) { t ->
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
                            "${t.date.format(SHORT_DATE)} · ${state.lookups?.describe(t) ?: t.kind.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        (if (t.fromAccountId == accountId) "−" else "") + MoneyFormat.amount(
                            t.amount,
                            t.currency
                        ),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    )
                }
                HorizontalDivider()
            }
        }
    }

    val sheetKind = adding ?: editing?.let { ProvidentFunds.entryKindOf(it, accountId) }
    if (account != null && sheetKind != null) ProvidentFundEntrySheet(
        account = account,
        banks = state.banks,
        defaultBankId = state.lastBankId,
        defaultAmounts = state.lastAmounts,
        rows = state.rows,
        initialKind = sheetKind,
        existing = editing,
        onSave = { kind, date, amount, particulars, bank ->
            viewModel.save(kind, date, amount, particulars, bank, editing); adding = null; editing =
            null
        },
        onDelete = editing?.let { row -> { viewModel.delete(row.id); editing = null } },
        onDismiss = { adding = null; editing = null },
    )
    // A row the sheet cannot describe (an old import, say) is left alone rather than edited blind.
    LaunchedEffect(editing) {
        val row = editing ?: return@LaunchedEffect
        if (ProvidentFunds.entryKindOf(row, accountId) == null) {
            editing =
                null; snackbar.showSnackbar("This row is not a provident-fund entry the sheet knows; it stays as it is.")
        }
    }
}
