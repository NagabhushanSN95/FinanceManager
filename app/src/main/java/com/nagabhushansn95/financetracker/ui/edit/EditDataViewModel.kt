package com.nagabhushansn95.financetracker.ui.edit

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.Template
import com.nagabhushansn95.financetracker.data.repository.EditDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the Manage tabs show: visible and deleted rows side by side, so "show deleted" is a pure UI toggle. */
class EditDataViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = EditDataRepository(db)

    val accounts: StateFlow<List<Account>> = db.accountDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories: StateFlow<List<Category>> = db.categoryDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val instruments: StateFlow<List<Instrument>> = db.instrumentDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val templates: StateFlow<List<Template>> = db.transactionDao().observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun messageShown() {
        _message.value = null
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { e ->
                Log.e("EditData", "failed", e)
                _message.value = e.message ?: e.toString()
            }
        }
    }

    fun saveAccount(account: Account) = run {
        if (account.id == 0L) repository.addAccount(account) else repository.updateAccount(account)
    }
    fun deleteAccount(id: Long) = run { repository.deleteAccount(id) }
    fun restoreAccount(id: Long) = run { repository.restoreAccount(id) }
    fun reorderAccounts(ids: List<Long>) = run { repository.reorderAccounts(ids) }
    suspend fun accountTransactionCount(id: Long) = repository.accountTransactionCount(id)

    fun saveCategory(category: Category) = run {
        if (category.id == 0L) repository.addCategory(category) else repository.updateCategory(
            category
        )
    }
    fun deleteCategory(id: Long) = run { repository.deleteCategory(id) }
    fun restoreCategory(id: Long) = run { repository.restoreCategory(id) }
    fun reorderCategories(ids: List<Long>) = run { repository.reorderCategories(ids) }
    suspend fun categoryTransactionCount(id: Long) = repository.categoryTransactionCount(id)

    fun saveInstrument(instrument: Instrument) = run {
        if (instrument.id == 0L) repository.addInstrument(instrument) else repository.updateInstrument(
            instrument
        )
    }

    fun setInstrumentActive(id: Long, active: Boolean) =
        run { repository.setInstrumentActive(id, active) }
    fun reorderInstruments(ids: List<Long>) = run { repository.reorderInstruments(ids) }
    suspend fun instrumentTransactionCount(id: Long) = repository.instrumentTransactionCount(id)

    fun deleteTemplate(template: Template) = run { db.transactionDao().deleteTemplate(template) }
    fun setTemplateHidden(template: Template, hidden: Boolean) =
        run { db.transactionDao().updateTemplate(template.copy(hidden = hidden)) }
}
