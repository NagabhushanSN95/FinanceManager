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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.CashFlow
import com.nagabhushansn95.financetracker.data.investments.Holdings
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.common.StatCell
import com.nagabhushansn95.financetracker.ui.transaction.LedgerLookups
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val loaded: Boolean = false,
)

/** Indian financial year label for a date: April 2025 to March 2026 is "2025-26". */
fun financialYear(date: LocalDate): String {
    val start = if (date.monthValue >= 4) date.year else date.year - 1
    return "$start-${(start + 1) % 100}"
}

class ProvidentFundAccountViewModel(application: Application, private val accountId: Long) :
    AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)

    val state: StateFlow<PfAccountState> = combine(
        db.accountDao().observeAll(),
        db.transactionDao().observeAll(),
        db.instrumentDao().observeAll(),
    ) { accounts, ledger, instruments ->
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
            lookups = LedgerLookups(accounts, db.categoryDao().getAll(), instruments),
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PfAccountState())
}

/** One provident-fund account: balance, contributor breakdown, financial-year table and the ledger. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProvidentFundAccountScreen(
    accountId: Long,
    onBack: () -> Unit,
    onAdd: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit
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
    Scaffold(
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
                        Button(
                            onClick = { onAdd(account.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Add a contribution") }
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
                        .combinedClickable(onClick = {}, onLongClick = { onEditTransaction(t.id) })
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
}
