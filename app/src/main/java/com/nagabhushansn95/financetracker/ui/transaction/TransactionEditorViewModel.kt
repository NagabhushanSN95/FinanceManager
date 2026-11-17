package com.nagabhushansn95.financetracker.ui.transaction

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
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
    /** Carried through an edit untouched: rows on an instrument are edited on the instrument's screen. */
    val instrumentId: Long? = null,
    val particulars: String = "",
    val quantity: String = "",
    val rate: String = "",
    val amount: String = "",
    /** A transfer between currencies: what arrives in the to-account, in its currency. */
    val toAmount: String = "",
    /** A credit into a foreign account with no load behind it: rupees per unit, for statistics. */
    val fxRate: String = "",
    /** The provident-fund contributor, carried through an edit untouched like [instrumentId]. */
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
    val toAmountValue get() = toAmount.trim().toDoubleOrNull()
    val fxRateValue get() = fxRate.trim().toDoubleOrNull()

    /** The currencies of the two sides, when both accounts are chosen. */
    fun currencies(accounts: Map<Long, Account>): Pair<String?, String?> =
        fromAccountId?.let { accounts[it]?.currency } to toAccountId?.let { accounts[it]?.currency }

    /** A transfer whose two accounts hold different currencies: the amount arriving is asked separately. */
    fun crossesCurrencies(accounts: Map<Long, Account>): Boolean {
        if (kind != TransactionKind.TRANSFER) return false
        val (from, to) = currencies(accounts)
        return from != null && to != null && from != to
    }

    /** An income into a foreign-currency account: its rupee rate may be typed. */
    fun intoForeign(accounts: Map<Long, Account>): Boolean =
        kind == TransactionKind.INCOME && (toAccountId?.let { accounts[it]?.currency }
            ?: "INR") != "INR"

    val showsFrom get() = kind == TransactionKind.EXPENSE || kind == TransactionKind.TRANSFER
    val showsTo get() = kind == TransactionKind.INCOME || kind == TransactionKind.TRANSFER
    val showsCategory get() = kind == TransactionKind.EXPENSE || kind == TransactionKind.INCOME

    /** Why the form cannot be saved yet, or null. A transfer may go without particulars, as in 4.x. */
    fun problem(accounts: Map<Long, Account>): String? = when {
        particulars.isBlank() && kind != TransactionKind.TRANSFER -> "Enter the particulars"
        amountValue == null || amountValue!! < 0 -> "Enter a valid amount"
        kind == TransactionKind.EXPENSE && fromAccountId == null -> "Choose the account"
        kind == TransactionKind.EXPENSE && categoryId == null -> "Choose a category"
        kind == TransactionKind.INCOME && toAccountId == null -> "Choose the account"
        kind == TransactionKind.TRANSFER && (fromAccountId == null || toAccountId == null) -> "Choose both accounts"
        kind == TransactionKind.TRANSFER && fromAccountId == toAccountId -> "Choose two different accounts"
        crossesCurrencies(accounts) && (toAmountValue == null || toAmountValue!! < 0) -> "Enter the amount arriving"
        fxRate.isNotBlank() && fxRateValue == null -> "Rate is not a number"
        quantity.isNotBlank() && quantityValue == null -> "Quantity is not a number"
        rate.isNotBlank() && rateValue == null -> "Rate is not a number"
        else -> null
    }
}

data class EditorData(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val templates: List<Template> = emptyList(),
    val recentFrom: List<Long> = emptyList(),
    val recentTo: List<Long> = emptyList(),
    val recentCategories: List<Long> = emptyList(),
    val loaded: Boolean = false,
) {
    val accountsById by lazy { accounts.associateBy { it.id } }
    val lookups by lazy { LedgerLookups(accounts, categories, emptyList()) }
}

sealed interface EditorEvent {
    data object Saved : EditorEvent
    data object Deleted : EditorEvent
    data class Error(val message: String) : EditorEvent

    /** The transaction cannot be edited as things stand (an account of it is deleted); the screen explains and leaves. */
    data class Blocked(val message: String) : EditorEvent
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
    fun start(
        transactionId: Long?,
        kind: TransactionKind?,
        accountId: Long?,
    ) {
        if (initialised) return
        initialised = true
        viewModelScope.launch {
            _data.value = EditorData(
                // Provident-fund rows are recorded on the account's screen under Investments and insurance rows on
                // the policy's screen, never here.
                accounts = db.accountDao().getVisible()
                    .filter { it.accountType != AccountType.PROVIDENT_FUND },
                categories = db.categoryDao().getVisible(),
                templates = db.transactionDao().getVisibleTemplates(),
                recentFrom = prefs.recentIds(RECENT_FROM),
                recentTo = prefs.recentIds(RECENT_TO),
                recentCategories = prefs.recentIds(RECENT_CATEGORY),
                loaded = true,
            )
            if (transactionId != null) {
                val t = db.transactionDao().getById(transactionId)
                if (t != null) {
                    // As in 4.x: a row on a deleted account is read-only until the account is restored, so that the
                    // edit cannot move money through an account that is no longer offered.
                    val gone = listOfNotNull(t.fromAccountId, t.toAccountId)
                        .mapNotNull { db.accountDao().getById(it) }.firstOrNull { it.deleted }
                    if (gone != null) {
                        _event.value = EditorEvent.Blocked(
                            "This transaction is on the deleted account '${gone.name}'. Restore the account under " +
                                    "Manage › Accounts (Show deleted) before editing it."
                        )
                        return@launch
                    }
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
                        toAmount = t.toAmount?.let { plain(it) } ?: "",
                        fxRate = t.fxRate?.let { plain(it) } ?: "",
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
                fromAccountId = accountId.takeIf { k != TransactionKind.INCOME }
                    ?: data.recentFrom.firstOrNull { id -> data.accountsById.containsKey(id) },
                toAccountId = accountId.takeIf { k == TransactionKind.INCOME }
                    ?: data.recentTo.firstOrNull { id -> data.accountsById.containsKey(id) },
            )
        }
    }

    fun setKind(kind: TransactionKind) = _form.update { f ->
        f.copy(
            kind = kind,
            // Keep what still applies, clear what does not.
            fromAccountId = if (kind == TransactionKind.INCOME) null else f.fromAccountId,
            toAccountId = if (kind == TransactionKind.EXPENSE) null else f.toAccountId,
            categoryId = if (kind == TransactionKind.EXPENSE || kind == TransactionKind.INCOME) f.categoryId else null,
        )
    }

    fun setDate(date: LocalDate) = _form.update { it.copy(date = date) }
    fun setFrom(id: Long?) = _form.update { it.copy(fromAccountId = id) }
    fun setTo(id: Long?) = _form.update { it.copy(toAccountId = id) }
    fun setCategory(id: Long?) = _form.update { it.copy(categoryId = id) }
    fun setParticulars(text: String) = _form.update { it.copy(particulars = text) }
    fun setToAmount(text: String) = _form.update { it.copy(toAmount = text) }
    fun setFxRate(text: String) = _form.update { it.copy(fxRate = text) }
    fun setSaveAsTemplate(b: Boolean) = _form.update { it.copy(saveAsTemplate = b) }
    fun setHidden(b: Boolean) = _form.update { it.copy(hidden = b) }
    fun setIncludeInStatistics(b: Boolean) = _form.update { it.copy(includeInStatistics = b) }

    /**
     * The three money fields fill each other in, as Finance Manager did: typing a rate with no quantity means one
     * unit; rate × quantity gives the amount; an amount with a quantity gives the rate; an amount alone is one
     * unit at that rate.
     */
    fun setAmount(text: String) = _form.update { f ->
        val amount = text.trim().toDoubleOrNull()
        val q = f.quantityValue
        when {
            amount == null -> f.copy(amount = text)
            q != null && q > 0 && q != 1.0 -> f.copy(amount = text, rate = plain(amount / q))
            else -> f.copy(amount = text, quantity = "1", rate = text.trim())
        }
    }

    fun setQuantity(text: String) = _form.update { f ->
        val q = text.trim().toDoubleOrNull()
        val r = f.rateValue
        val a = f.amountValue
        when {
            q == null -> f.copy(quantity = text)
            r != null -> f.copy(quantity = text, amount = plain(q * r))
            a != null && q > 0 -> f.copy(quantity = text, rate = plain(a / q))
            else -> f.copy(quantity = text)
        }
    }

    fun setRate(text: String) = _form.update { f ->
        val r = text.trim().toDoubleOrNull()
        if (r == null) return@update f.copy(rate = text)
        val quantity = if (f.quantity.isBlank()) "1" else f.quantity
        val q = quantity.trim().toDoubleOrNull()
        f.copy(rate = text, quantity = quantity, amount = if (q != null) plain(q * r) else f.amount)
    }

    /**
     * Copies a saved template into the form: particulars, endpoints, and exactly the money fields it holds. A rate
     * alone fills only the rate (the quantity is typed, or defaults to one when the row is added); rate and
     * quantity together give the amount.
     */
    fun applyTemplate(t: Template) = _form.update { f ->
        val rate = t.rate?.let { plain(it) } ?: f.rate
        val quantity = t.quantity?.let { plain(it) } ?: f.quantity
        val r = rate.trim().toDoubleOrNull()
        val q = quantity.trim().toDoubleOrNull()
        f.copy(
            kind = t.kind,
            particulars = t.particulars,
            fromAccountId = t.fromAccountId,
            toAccountId = t.toAccountId,
            categoryId = t.categoryId,
            rate = rate,
            quantity = quantity,
            amount = if (t.rate != null && r != null && q != null) plain(r * q) else f.amount,
        )
    }


    fun save() {
        // A rate with no amount means one unit (or the typed quantity) at that rate.
        _form.update { f ->
            val r = f.rateValue
            if (f.amount.isNotBlank() || r == null) f
            else {
                val quantity = if (f.quantity.isBlank()) "1" else f.quantity
                val q = quantity.trim().toDoubleOrNull() ?: return@update f
                f.copy(quantity = quantity, amount = plain(q * r))
            }
        }
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
            // Kept as it was: rows on an instrument are edited on the instrument's screen, never retagged here.
            instrumentId = f.instrumentId,
            particulars = f.particulars.trim(),
            quantity = f.quantityValue,
            rate = f.rateValue,
            amount = f.amountValue!!,
            // A load of a foreign card: the rupees out, the foreign money in, and the rate the two imply. A credit into
            // a foreign account keeps the rate typed, if any; everything else carries neither.
            toAmount = f.toAmountValue.takeIf { f.crossesCurrencies(data.accountsById) },
            fxRate = when {
                f.crossesCurrencies(data.accountsById) -> {
                    val (from, to) = f.currencies(data.accountsById)
                    val toValue = f.toAmountValue
                    if (from == "INR" && toValue != null && toValue > 0) f.amountValue!! / toValue
                    else if (to == "INR" && f.amountValue!! > 0 && toValue != null) toValue / f.amountValue!!
                    else null
                }

                f.intoForeign(data.accountsById) -> f.fxRateValue
                else -> null
            },
            contributor = f.contributor,
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
        if (f.kind == TransactionKind.INCOME) f.toAccountId else f.fromAccountId

    private fun accountCurrency(accountId: Long?, data: EditorData): String =
        accountId?.let { data.accountsById[it]?.currency } ?: "INR"

    companion object {
        private const val TAG = "TransactionEditor"
        const val RECENT_FROM = "account_from"
        const val RECENT_TO = "account_to"
        const val RECENT_CATEGORY = "category"

        /** Numbers as the user would type them: no exponent, no trailing zeros. */
        fun plain(value: Double): String =
            if (value == value.toLong().toDouble()) value.toLong().toString()
            else java.math.BigDecimal(value).setScale(4, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()
    }
}
