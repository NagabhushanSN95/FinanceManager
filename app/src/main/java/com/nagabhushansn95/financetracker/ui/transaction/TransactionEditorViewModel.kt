package com.nagabhushansn95.financetracker.ui.transaction

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.Template
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import com.nagabhushansn95.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** Everything the editor form shows. Amount fields are kept as text so the user can type freely. */
data class EditorForm(
    val id: Long = 0,
    val kind: TransactionKind = TransactionKind.EXPENSE,
    val date: LocalDate = LocalDate.now(),
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val instrumentId: Long? = null,
    val particulars: String = "",
    val quantity: String = "",
    val rate: String = "",
    val amount: String = "",
    val contributor: String? = null,
    val saveAsTemplate: Boolean = false,
    val hidden: Boolean = false,
    val includeInStatistics: Boolean = true,
    val createdTime: LocalDateTime? = null,
) {
    val isEdit get() = id != 0L
    val amountValue get() = amount.trim().toDoubleOrNull()
    val quantityValue get() = quantity.trim().toDoubleOrNull()
    val rateValue get() = rate.trim().toDoubleOrNull()

    val showsFrom get() = kind == TransactionKind.EXPENSE || kind == TransactionKind.TRANSFER || kind == TransactionKind.INVEST
    val showsTo get() = kind == TransactionKind.INCOME || kind == TransactionKind.TRANSFER || kind == TransactionKind.REDEEM
    val showsCategory get() = kind == TransactionKind.EXPENSE || kind == TransactionKind.INCOME
    val instrumentRequired get() = kind == TransactionKind.INVEST || kind == TransactionKind.REDEEM
    val showsInstrument get() = instrumentRequired || kind == TransactionKind.INCOME || kind == TransactionKind.EXPENSE

    /** Why the form cannot be saved yet, or null. */
    fun problem(accounts: Map<Long, Account>): String? = when {
        particulars.isBlank() -> "Enter the particulars"
        amountValue == null || amountValue!! < 0 -> "Enter a valid amount"
        kind == TransactionKind.EXPENSE && fromAccountId == null -> "Choose the account"
        kind == TransactionKind.EXPENSE && categoryId == null -> "Choose a category"
        kind == TransactionKind.INCOME && toAccountId == null -> "Choose the account"
        kind == TransactionKind.TRANSFER && (fromAccountId == null || toAccountId == null) -> "Choose both accounts"
        kind == TransactionKind.TRANSFER && fromAccountId == toAccountId -> "Choose two different accounts"
        instrumentRequired && instrumentId == null -> "Choose the instrument"
        quantity.isNotBlank() && quantityValue == null -> "Quantity is not a number"
        rate.isNotBlank() && rateValue == null -> "Rate is not a number"
        toAccountId?.let { accounts[it]?.accountType } == AccountType.PROVIDENT_FUND && kind == TransactionKind.INCOME && contributor == null ->
            "Say who contributed"

        else -> null
    }
}

data class EditorData(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val instruments: List<Instrument> = emptyList(),
    val templates: List<Template> = emptyList(),
    val recentFrom: List<Long> = emptyList(),
    val recentTo: List<Long> = emptyList(),
    val recentCategories: List<Long> = emptyList(),
    val recentInstruments: List<Long> = emptyList(),
    val loaded: Boolean = false,
) {
    val accountsById by lazy { accounts.associateBy { it.id } }
    val lookups by lazy { LedgerLookups(accounts, categories, instruments) }
}

sealed interface EditorEvent {
    data object Saved : EditorEvent
    data object Deleted : EditorEvent
    data class Error(val message: String) : EditorEvent
}

class TransactionEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val prefs = AppPreferences(application)
    private val repository = TransactionRepository(db)

    private val _form = MutableStateFlow(EditorForm())
    val form: StateFlow<EditorForm> = _form
    private val _data = MutableStateFlow(EditorData())
    val data: StateFlow<EditorData> = _data
    private val _event = MutableStateFlow<EditorEvent?>(null)
    val event: StateFlow<EditorEvent?> = _event
    private var initialised = false

    /** Loads lookups and either the transaction to edit or a new form with defaults. Safe to call again. */
    fun start(transactionId: Long?, kind: TransactionKind?, accountId: Long?) {
        if (initialised) return
        initialised = true
        viewModelScope.launch {
            _data.value = EditorData(
                accounts = db.accountDao().getVisible(),
                categories = db.categoryDao().getVisible(),
                instruments = db.instrumentDao().getAll(),
                templates = db.transactionDao().getVisibleTemplates(),
                recentFrom = prefs.recentIds(RECENT_FROM),
                recentTo = prefs.recentIds(RECENT_TO),
                recentCategories = prefs.recentIds(RECENT_CATEGORY),
                recentInstruments = prefs.recentIds(RECENT_INSTRUMENT),
                loaded = true,
            )
            if (transactionId != null) {
                val t = db.transactionDao().getById(transactionId)
                if (t != null) {
                    _form.value = EditorForm(
                        id = t.id,
                        kind = t.kind,
                        date = t.date,
                        fromAccountId = t.fromAccountId,
                        toAccountId = t.toAccountId,
                        categoryId = t.categoryId,
                        instrumentId = t.instrumentId,
                        particulars = t.particulars,
                        quantity = t.quantity?.let { plain(it) } ?: "",
                        rate = t.rate?.let { plain(it) } ?: "",
                        amount = plain(t.amount),
                        contributor = t.contributor,
                        hidden = t.hidden,
                        includeInStatistics = t.includeInStatistics,
                        createdTime = t.createdTime,
                    )
                    return@launch
                }
            }
            val k = kind ?: TransactionKind.EXPENSE
            val data = _data.value
            _form.value = EditorForm(
                kind = k,
                fromAccountId = accountId.takeIf { k != TransactionKind.INCOME && k != TransactionKind.REDEEM }
                    ?: data.recentFrom.firstOrNull { id -> data.accountsById.containsKey(id) },
                toAccountId = accountId.takeIf { k == TransactionKind.INCOME || k == TransactionKind.REDEEM }
                    ?: data.recentTo.firstOrNull { id -> data.accountsById.containsKey(id) },
            )
        }
    }

    fun setKind(kind: TransactionKind) = _form.update { f ->
        f.copy(
            kind = kind,
            // Keep what still applies, clear what does not.
            fromAccountId = if (kind == TransactionKind.INCOME || kind == TransactionKind.REDEEM) null else f.fromAccountId,
            toAccountId = if (kind == TransactionKind.EXPENSE || kind == TransactionKind.INVEST) null else f.toAccountId,
            categoryId = if (kind == TransactionKind.EXPENSE || kind == TransactionKind.INCOME) f.categoryId else null,
            instrumentId = if (kind == TransactionKind.TRANSFER) null else f.instrumentId,
            contributor = if (kind == TransactionKind.INCOME) f.contributor else null,
        )
    }

    fun setDate(date: LocalDate) = _form.update { it.copy(date = date) }
    fun setFrom(id: Long?) = _form.update { it.copy(fromAccountId = id) }
    fun setTo(id: Long?) = _form.update { it.copy(toAccountId = id) }
    fun setCategory(id: Long?) = _form.update { it.copy(categoryId = id) }
    fun setInstrument(id: Long?) = _form.update { it.copy(instrumentId = id) }
    fun setParticulars(text: String) = _form.update { it.copy(particulars = text) }
    fun setContributor(c: String?) = _form.update { it.copy(contributor = c) }
    fun setSaveAsTemplate(b: Boolean) = _form.update { it.copy(saveAsTemplate = b) }
    fun setHidden(b: Boolean) = _form.update { it.copy(hidden = b) }
    fun setIncludeInStatistics(b: Boolean) = _form.update { it.copy(includeInStatistics = b) }
    fun setAmount(text: String) = _form.update { it.copy(amount = text) }

    /** Quantity × rate fills the amount when both are numbers. */
    fun setQuantity(text: String) = _form.update { it.copy(quantity = text).recomputeAmount() }
    fun setRate(text: String) = _form.update { it.copy(rate = text).recomputeAmount() }

    private fun EditorForm.recomputeAmount(): EditorForm {
        val q = quantityValue
        val r = rateValue
        return if (q != null && r != null) copy(amount = plain(q * r)) else this
    }

    /** Copies a saved template into the form (particulars, endpoints, amount). */
    fun applyTemplate(t: Template) = _form.update { f ->
        f.copy(
            kind = t.kind,
            particulars = t.particulars,
            fromAccountId = t.fromAccountId,
            toAccountId = t.toAccountId,
            categoryId = t.categoryId,
            instrumentId = t.instrumentId,
            amount = t.amount?.let { plain(it) } ?: f.amount,
        )
    }

    /** Minimal "new instrument" for the picker; full editing comes with the Investments screens. */
    fun createInstrument(
        name: String,
        type: InstrumentType,
        symbol: String?,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val id = db.instrumentDao().insert(
                Instrument(
                    name = name.trim(),
                    instrumentType = type,
                    symbol = symbol?.trim()?.takeIf { it.isNotEmpty() },
                    exchange = if (type == InstrumentType.STOCK) "NSE" else if (type == InstrumentType.MUTUAL_FUND) "AMFI" else null,
                    displayOrder = db.instrumentDao().maxDisplayOrder() + 1,
                    createdTime = now,
                    modifiedTime = now,
                )
            )
            _data.update {
                it.copy(
                    instruments = db.instrumentDao().getAll().filter { i -> i.active })
            }
            setInstrument(id)
            onCreated(id)
        }
    }

    fun save() {
        val f = _form.value
        val data = _data.value
        f.problem(data.accountsById)?.let { _event.value = EditorEvent.Error(it); return }
        val entity = TransactionEntity(
            id = f.id,
            createdTime = f.createdTime ?: LocalDateTime.now(),
            modifiedTime = LocalDateTime.now(),
            date = f.date,
            kind = f.kind,
            fromAccountId = f.fromAccountId.takeIf { f.showsFrom },
            toAccountId = f.toAccountId.takeIf { f.showsTo },
            categoryId = f.categoryId.takeIf { f.showsCategory },
            instrumentId = f.instrumentId.takeIf { f.showsInstrument },
            particulars = f.particulars.trim(),
            quantity = f.quantityValue.takeIf { f.showsInstrument && f.instrumentId != null },
            rate = f.rateValue.takeIf { f.showsInstrument && f.instrumentId != null },
            amount = f.amountValue!!,
            currency = accountCurrency(entityAccountFor(f), data),
            contributor = f.contributor.takeIf { f.kind == TransactionKind.INCOME },
            hidden = f.hidden,
            includeInStatistics = f.includeInStatistics,
        )
        viewModelScope.launch {
            val result = runCatching {
                if (f.isEdit) repository.update(entity) else repository.add(
                    entity,
                    f.saveAsTemplate
                )
            }
            result.onSuccess {
                entity.fromAccountId?.let { prefs.pushRecentId(RECENT_FROM, it) }
                entity.toAccountId?.let { prefs.pushRecentId(RECENT_TO, it) }
                entity.categoryId?.let { prefs.pushRecentId(RECENT_CATEGORY, it) }
                entity.instrumentId?.let { prefs.pushRecentId(RECENT_INSTRUMENT, it) }
                _event.value = EditorEvent.Saved
            }.onFailure { e ->
                Log.e(TAG, "Save failed", e)
                _event.value = EditorEvent.Error(e.message ?: e.toString())
            }
        }
    }

    fun delete() {
        val id = _form.value.id
        if (id == 0L) return
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onSuccess { _event.value = EditorEvent.Deleted }
                .onFailure { e -> _event.value = EditorEvent.Error(e.message ?: e.toString()) }
        }
    }

    fun eventHandled() {
        _event.value = null
    }

    /** The cash side that decides the row's currency: `from` for outflows, `to` for inflows. */
    private fun entityAccountFor(f: EditorForm): Long? =
        if (f.kind == TransactionKind.INCOME || f.kind == TransactionKind.REDEEM) f.toAccountId else f.fromAccountId

    private fun accountCurrency(accountId: Long?, data: EditorData): String =
        accountId?.let { data.accountsById[it]?.currency } ?: "INR"

    companion object {
        private const val TAG = "TransactionEditor"
        const val RECENT_FROM = "account_from"
        const val RECENT_TO = "account_to"
        const val RECENT_CATEGORY = "category"
        const val RECENT_INSTRUMENT = "instrument"
        val PF_CONTRIBUTORS = listOf("Employee", "Employer", "Self", "Interest")

        /** Numbers as the user would type them: no exponent, no trailing zeros. */
        fun plain(value: Double): String =
            if (value == value.toLong().toDouble()) value.toLong().toString()
            else java.math.BigDecimal(value).setScale(4, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()
    }
}
