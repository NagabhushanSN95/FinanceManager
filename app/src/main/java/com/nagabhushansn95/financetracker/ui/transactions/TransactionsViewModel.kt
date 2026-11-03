package com.nagabhushansn95.financetracker.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.repository.TransactionRepository
import com.nagabhushansn95.financetracker.ui.transaction.LedgerLookups
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
    /** Money leaving cash (expense, invest, transfer out) is shown differently from money arriving. */
    val outflow: Boolean,
    val hidden: Boolean,
)

data class LedgerDay(val date: LocalDate, val rows: List<LedgerRow>)

data class TransactionsUiState(
    val days: List<LedgerDay> = emptyList(),
    val total: Int = 0,
    val loaded: Boolean = false
)

/** The whole ledger, newest first, grouped by day. Filters come with the Statistics work. */
class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = TransactionRepository(db)

    fun delete(id: Long, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }.onFailure { onError(it.message ?: it.toString()) }
        }
    }

    val state: StateFlow<TransactionsUiState> = combine(
        db.transactionDao().observeAll(),
        db.accountDao().observeVisible(),
        db.categoryDao().observeVisible(),
        db.instrumentDao().observeActive(),
    ) { transactions, accounts, categories, instruments ->
        // Deleted accounts/categories still need names for old rows.
        val lookups = LedgerLookups(
            db.accountDao().getAll(),
            db.categoryDao().getAll(),
            db.instrumentDao().getAll()
        )
        val rows = transactions.map { it.toRow(lookups) }
        TransactionsUiState(
            days = rows.groupBy { it.date }.map { (date, list) -> LedgerDay(date, list) },
            total = rows.size,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    private fun TransactionEntity.toRow(lookups: LedgerLookups) = LedgerRow(
        id = id,
        date = date,
        kind = kind,
        particulars = particulars,
        description = lookups.describe(this),
        amountText = com.nagabhushansn95.financetracker.ui.common.MoneyFormat.amount(
            amount,
            currency
        ),
        outflow = kind == TransactionKind.EXPENSE || kind == TransactionKind.INVEST,
        hidden = hidden,
    )
}
