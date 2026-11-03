package com.nagabhushansn95.financetracker.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.repository.TransactionRepository
import com.nagabhushansn95.financetracker.ui.common.LedgerFilter
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.Period
import com.nagabhushansn95.financetracker.ui.transaction.LedgerLookups
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LedgerRow(
    val id: Long,
    val date: LocalDate,
    val kind: TransactionKind,
    val particulars: String,
    val description: String,
    val amountText: String,
    /** Money leaving cash (expense, invest) is shown differently from money arriving. */
    val outflow: Boolean,
    val hidden: Boolean,
)

data class LedgerDay(val date: LocalDate, val rows: List<LedgerRow>)

data class TransactionsUiState(
    val days: List<LedgerDay> = emptyList(),
    /** Rows shown after filtering, and the size of the whole ledger. */
    val shown: Int = 0,
    val total: Int = 0,
    val filter: LedgerFilter = LedgerFilter(period = Period.ALL),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val loaded: Boolean = false,
)

/** The ledger, newest first, grouped by day, narrowed by a [LedgerFilter]. */
class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = TransactionRepository(db)
    private val filter = MutableStateFlow(LedgerFilter(period = Period.ALL))

    fun setFilter(f: LedgerFilter) {
        filter.value = f
    }

    fun delete(id: Long, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }.onFailure { onError(it.message ?: it.toString()) }
        }
    }

    val state: StateFlow<TransactionsUiState> = combine(
        db.transactionDao().observeAll(),
        db.accountDao().observeAll(),
        db.categoryDao().observeAll(),
        db.instrumentDao().observeAll(),
        filter,
    ) { transactions, accounts, categories, instruments, f ->
        // All rows (deleted accounts and categories included) so old transactions still get names.
        val lookups = LedgerLookups(accounts, categories, instruments)
        val range = f.range(earliest = transactions.lastOrNull()?.date)
        val rows = transactions.filter { f.matches(it, range) }.map { it.toRow(lookups) }
        TransactionsUiState(
            days = rows.groupBy { it.date }.map { (date, list) -> LedgerDay(date, list) },
            shown = rows.size,
            total = transactions.size,
            filter = f,
            accounts = accounts.filter { !it.deleted },
            categories = categories.filter { !it.deleted },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    private fun TransactionEntity.toRow(lookups: LedgerLookups) = LedgerRow(
        id = id,
        date = date,
        kind = kind,
        particulars = particulars,
        description = lookups.describe(this),
        amountText = MoneyFormat.amount(amount, currency),
        outflow = kind == TransactionKind.EXPENSE || kind == TransactionKind.INVEST,
        hidden = hidden,
    )
}
