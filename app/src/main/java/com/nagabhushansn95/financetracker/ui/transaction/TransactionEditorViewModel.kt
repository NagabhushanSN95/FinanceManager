package com.nagabhushansn95.financetracker.ui.transaction

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.bills.BillPeriod
import com.nagabhushansn95.financetracker.data.bills.Bills
import com.nagabhushansn95.financetracker.data.bills.Validity
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Bill
import com.nagabhushansn95.financetracker.data.db.entity.BillSchedule
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.Template
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import com.nagabhushansn95.financetracker.data.repository.BillsRepository
import com.nagabhushansn95.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToLong

/**
 * The bill a row pays. Recording: the editor was opened from the bill for [period] (a calendar bill), or for a
 * duration bill, whose period follows from the date and the bill's validity on save unless [period] was set by hand.
 * Existing: the row being edited already pays the bill; shown, never changed here.
 */
data class BillTarget(
    val bill: Bill,
    val period: BillPeriod?,
    /** DURATION: the day the current validity ends, so a payment made before it queues behind. */
    val previousEnd: LocalDate? = null,
    /** DURATION, recording: the validities to offer when the period is changed by hand. */
    val validities: List<Validity> = emptyList(),
    val existing: Boolean = false,
    /** CALENDAR, recording: the periods the payment may be for (unsettled ones, then a few ahead). */
    val choices: List<BillPeriod> = emptyList(),
    /** What a card's statement asked for; it applies to [statementFor] only. */
    val statementAmount: Double? = null,
    val statementFor: LocalDate? = null,
    /** The bill's expected amount, else the last payment's: what a period other than the statement's is likely to cost. */
    val usualAmount: Double? = null,
) {
    val recording: Boolean get() = !existing
    val duration: Boolean get() = bill.schedule == BillSchedule.DURATION
    val label: String get() = period?.label ?: "—"

    /** DURATION, recording: the period a payment on [date] covers when none was set by hand. */
    fun periodFor(date: LocalDate): BillPeriod? =
        period ?: if (duration) Bills.durationPeriod(
            previousEnd,
            date,
            Bills.validityOf(bill)
        ) else null

    /** The statement amount for the statement's period, else the usual amount. */
    fun amountFor(period: BillPeriod?): Double? =
        if (period != null && period.start == statementFor && statementAmount != null) statementAmount else usualAmount
}

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
    val bill: BillTarget? = null,
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

    /** The three money figures of a row as they are saved. */
    data class Money(val rate: Double, val quantity: Double, val amount: Double)

    /**
     * The money fields filled in from what was typed, by 4.x's rule, applied on save and never while typing: a rate
     * or an amount alone is one unit; rate and amount give the quantity, rounded to whole units (at least one); rate
     * and quantity give the amount; amount and quantity give the rate. All three typed are kept exactly as typed.
     * Null when neither a rate nor an amount was typed, or a typed field is not a number.
     */
    fun resolvedMoney(): Money? {
        val r = rateValue
        val q = quantityValue
        val a = amountValue
        if (rate.isNotBlank() && r == null || quantity.isNotBlank() && q == null || amount.isNotBlank() && a == null) return null
        return when {
            r == null && a == null -> null
            q == null && a == null -> Money(r!!, 1.0, r)
            q == null && r == null -> Money(a!!, 1.0, a)
            q == null -> Money(
                r!!,
                if (r > 0) (a!! / r).roundToLong().toDouble().coerceAtLeast(1.0) else 1.0,
                a!!
            )

            a == null -> Money(r!!, q, r * q)
            r == null -> Money(if (q > 0) a / q else a, q, a)
            else -> Money(r, q, a)
        }
    }

    /**
     * The form with a template copied in: its kind and particulars, and every other field it holds a value for,
     * over whatever is there; a field the template leaves empty stays as typed. A template carries a rate and a
     * quantity, never an amount: the amount follows on save. Fields the new kind has no use for are cleared.
     */
    fun applying(t: Template): EditorForm = copy(
        kind = t.kind,
        particulars = t.particulars,
        fromAccountId = (t.fromAccountId
            ?: fromAccountId).takeIf { t.kind != TransactionKind.INCOME },
        toAccountId = (t.toAccountId ?: toAccountId).takeIf { t.kind != TransactionKind.EXPENSE },
        categoryId = (t.categoryId ?: categoryId).takeIf { t.kind != TransactionKind.TRANSFER },
        rate = t.rate?.let { TransactionEditorViewModel.plain(it) } ?: rate,
        quantity = t.quantity?.let { TransactionEditorViewModel.plain(it) } ?: quantity,
    )

    /**
     * The form switched to another kind. The one account chosen so far follows the money, as Finance Manager did:
     * an income's "to" becomes an expense's "from" and a transfer's "to"; an expense's "from" becomes an income's
     * "to" and a transfer's "from"; a transfer hands its "to" to an income and its "from" to an expense. The other
     * side of a transfer starts empty. The category stays only if it applies to the new kind too (one for both), else
     * it is to be chosen again; [categories] tells what each applies to, and an unknown one is dropped.
     */
    fun withKind(
        newKind: TransactionKind,
        categories: Map<Long, Category> = emptyMap()
    ): EditorForm {
        if (newKind == kind) return this
        val scope = when (newKind) {
            TransactionKind.EXPENSE -> CategoryScope.EXPENSE
            TransactionKind.INCOME -> CategoryScope.INCOME
            else -> null
        }
        val category = categoryId?.takeIf { id ->
            scope != null && categories[id]?.appliesTo.let { it == scope || it == CategoryScope.BOTH }
        }
        val (from, to) = when (newKind) {
            TransactionKind.INCOME -> null to (if (kind == TransactionKind.EXPENSE) fromAccountId else toAccountId)
            TransactionKind.EXPENSE -> (if (kind == TransactionKind.INCOME) toAccountId else fromAccountId) to null
            TransactionKind.TRANSFER -> if (kind == TransactionKind.INCOME) null to toAccountId else fromAccountId to null
            else -> fromAccountId to toAccountId
        }
        return copy(
            kind = newKind,
            fromAccountId = from,
            toAccountId = to,
            categoryId = category,
        )
    }

    /** Why the form cannot be saved yet, or null. A transfer may go without particulars, as in 4.x. */
    fun problem(accounts: Map<Long, Account>): String? = when {
        particulars.isBlank() && kind != TransactionKind.TRANSFER -> "Enter the particulars"
        rate.isBlank() && amount.isBlank() -> "Enter the rate or the amount"
        rate.isNotBlank() && (rateValue == null || rateValue!! < 0) -> "Enter a valid rate"
        quantity.isNotBlank() && (quantityValue == null || quantityValue!! <= 0) -> "Enter a quantity above zero"
        amount.isNotBlank() && (amountValue == null || amountValue!! < 0) -> "Enter a valid amount"
        kind == TransactionKind.EXPENSE && fromAccountId == null -> "Choose the account"
        kind == TransactionKind.EXPENSE && categoryId == null -> "Choose a category"
        kind == TransactionKind.INCOME && toAccountId == null -> "Choose the account"
        kind == TransactionKind.TRANSFER && (fromAccountId == null || toAccountId == null) -> "Choose both accounts"
        kind == TransactionKind.TRANSFER && fromAccountId == toAccountId -> "Choose two different accounts"
        crossesCurrencies(accounts) && (toAmountValue == null || toAmountValue!! < 0) -> "Enter the amount arriving"
        fxRate.isNotBlank() && fxRateValue == null -> "Rate is not a number"
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
    private val bills = BillsRepository(db)

    private val _form = MutableStateFlow(EditorForm())
    val form: StateFlow<EditorForm> = _form
    private val _data = MutableStateFlow(EditorData())
    val data: StateFlow<EditorData> = _data
    private val _event = MutableStateFlow<EditorEvent?>(null)
    val event: StateFlow<EditorEvent?> = _event
    private var initialised = false

    /** The form as it was opened; leaving with anything changed asks first. */
    private var opened = EditorForm()

    fun isDirty(): Boolean = _form.value != opened

    /** Loads lookups and either the transaction to edit or a new form with defaults. Safe to call again. */
    fun start(
        transactionId: Long?,
        kind: TransactionKind?,
        accountId: Long?,
        billId: Long? = null,
        periodStart: LocalDate? = null,
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
                    // A row that pays a bill says so; the link itself is changed from the bill's screen.
                    val payment = db.billDao().getPaymentOf(t.id)
                    val paid = payment?.let { p ->
                        db.billDao().getById(p.billId)?.let { b ->
                            BillTarget(
                                b,
                                BillPeriod(p.periodStart, p.periodEnd, p.dueDate),
                                existing = true
                            )
                        }
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
                        bill = paid,
                    )
                    opened = _form.value
                    return@launch
                }
            }
            if (billId != null) {
                val bill = db.billDao().getById(billId)
                if (bill != null) {
                    _form.value = formForBill(bill, periodStart)
                    opened = _form.value
                    return@launch
                }
            }
            // No account is chosen for the user (the picker lists the recent ones first); only a press and hold on an
            // account on Home opens the editor with that account, as asked.
            val k = kind ?: TransactionKind.EXPENSE
            _form.value = EditorForm(
                kind = k,
                fromAccountId = accountId.takeIf { k != TransactionKind.INCOME },
                toAccountId = accountId.takeIf { k == TransactionKind.INCOME },
            )
            opened = _form.value
        }
    }

    /**
     * The form filled in from a bill for the period starting on [periodStart] (or the first unsettled one): the
     * bill's kind, accounts and category, the particulars for the period, and the expected amount (a card's
     * statement when known). A duration bill asks for the validity instead of a period.
     */
    private suspend fun formForBill(bill: Bill, periodStart: LocalDate?): EditorForm {
        val today = LocalDate.now()
        val accounts = db.accountDao().getAll().associateBy { it.id }
        val payments = db.billDao().getPayments(bill.id)
        val card = bill.toAccountId?.let { accounts[it] }
            ?.takeIf { it.accountType == AccountType.CREDIT_CARD && bill.kind == TransactionKind.TRANSFER }
        val ledger = card?.let { db.transactionDao().getForAccount(it.id) } ?: emptyList()
        val view = Bills.build(listOf(bill), payments, emptyMap(), accounts, today, ledger).single()
        val open = Bills.unsettled(bill, payments, today, view.dueDay)
        val period = if (bill.schedule == BillSchedule.DURATION) null
        else open.firstOrNull { it.start == periodStart } ?: view.next ?: open.firstOrNull()
        // The last payment's amount stands in for a varying bill with nothing expected.
        val last =
            payments.mapNotNull { p -> p.transactionId?.let { db.transactionDao().getById(it) } }
                .maxWithOrNull(compareBy({ it.date }, { it.id }))
        val target = BillTarget(
            bill = bill,
            period = period,
            previousEnd = payments.maxOfOrNull { it.periodEnd },
            validities = if (bill.schedule == BillSchedule.DURATION) Bills.validityChoices(
                bill,
                payments
            ) else emptyList(),
            choices = Bills.periodChoices(bill, payments, today, view.dueDay),
            statementAmount = view.statementAmount,
            statementFor = view.next?.start,
            usualAmount = bill.amount ?: last?.amount,
        )
        val particulars = Bills.particularsFor(bill, period ?: target.periodFor(today)!!)
        val amount = target.amountFor(period)
        return EditorForm(
            kind = bill.kind,
            fromAccountId = (bill.fromAccountId
                ?: last?.fromAccountId).takeIf { bill.kind != TransactionKind.INCOME },
            toAccountId = (bill.toAccountId
                ?: last?.toAccountId).takeIf { bill.kind != TransactionKind.EXPENSE },
            categoryId = (bill.categoryId
                ?: last?.categoryId).takeIf { bill.kind != TransactionKind.TRANSFER },
            particulars = particulars,
            amount = amount?.let { plain(it) } ?: "",
            bill = target,
        )
    }

    fun setKind(kind: TransactionKind) =
        _form.update { it.withKind(kind, _data.value.categories.associateBy { c -> c.id }) }

    /** Bill payment: another period; the particulars and amount follow while they are still the bill's own. */
    fun setBillPeriod(period: BillPeriod) = _form.update { f ->
        val target = f.bill ?: return@update f
        if (!target.recording || target.period == period) return@update f
        val oldParticulars = target.periodFor(f.date)?.let { Bills.particularsFor(target.bill, it) }
        val oldAmount = target.amountFor(target.period)?.let { plain(it) } ?: ""
        f.copy(
            bill = target.copy(period = period),
            particulars = if (f.particulars == oldParticulars) Bills.particularsFor(
                target.bill,
                period
            ) else f.particulars,
            amount = if (f.amount == oldAmount) target.amountFor(period)?.let { plain(it) }
                ?: "" else f.amount,
        )
    }

    /** A duration payment's period follows the date until it is set by hand; the particulars follow it while still the bill's own. */
    fun setDate(date: LocalDate) = _form.update { f ->
        val target = f.bill
        if (target == null || !target.recording || !target.duration || target.period != null) return@update f.copy(
            date = date
        )
        val old = target.periodFor(f.date)?.let { Bills.particularsFor(target.bill, it) }
        val new = target.periodFor(date)?.let { Bills.particularsFor(target.bill, it) }
        f.copy(
            date = date,
            particulars = if (f.particulars == old && new != null) new else f.particulars
        )
    }
    fun setFrom(id: Long?) = _form.update { it.copy(fromAccountId = id) }
    fun setTo(id: Long?) = _form.update { it.copy(toAccountId = id) }
    fun setCategory(id: Long?) = _form.update { it.copy(categoryId = id) }
    fun setParticulars(text: String) = _form.update { it.copy(particulars = text) }
    fun setToAmount(text: String) = _form.update { it.copy(toAmount = text) }
    fun setFxRate(text: String) = _form.update { it.copy(fxRate = text) }
    fun setSaveAsTemplate(b: Boolean) = _form.update { it.copy(saveAsTemplate = b) }
    fun setHidden(b: Boolean) = _form.update { it.copy(hidden = b) }
    fun setIncludeInStatistics(b: Boolean) = _form.update { it.copy(includeInStatistics = b) }

    // The money fields are kept exactly as typed; they fill each other in on save (see EditorForm.resolvedMoney).
    fun setAmount(text: String) = _form.update { it.copy(amount = text) }
    fun setQuantity(text: String) = _form.update { it.copy(quantity = text) }
    fun setRate(text: String) = _form.update { it.copy(rate = text) }

    /** Copies a saved template into the form, see [EditorForm.applying]. */
    fun applyTemplate(t: Template) = _form.update { it.applying(t) }

    fun save() {
        val f = _form.value
        val data = _data.value
        f.problem(data.accountsById)?.let { _event.value = EditorEvent.Error(it); return }
        val money = f.resolvedMoney() ?: run {
            _event.value = EditorEvent.Error("Enter the rate or the amount"); return
        }
        // Shown filled in, as they are saved.
        _form.update {
            it.copy(
                rate = plain(money.rate),
                quantity = plain(money.quantity),
                amount = plain(money.amount)
            )
        }
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
            quantity = money.quantity,
            rate = money.rate,
            amount = money.amount,
            // A load of a foreign card: the rupees out, the foreign money in, and the rate the two imply. A credit into
            // a foreign account keeps the rate typed, if any; everything else carries neither.
            toAmount = f.toAmountValue.takeIf { f.crossesCurrencies(data.accountsById) },
            fxRate = when {
                f.crossesCurrencies(data.accountsById) -> {
                    val (from, to) = f.currencies(data.accountsById)
                    val toValue = f.toAmountValue
                    if (from == "INR" && toValue != null && toValue > 0) money.amount / toValue
                    else if (to == "INR" && money.amount > 0 && toValue != null) toValue / money.amount
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
                val target = f.bill
                if (f.isEdit) repository.update(
                    entity,
                    f.saveAsTemplate
                ) else if (target != null && target.recording) {
                    // A duration payment's period follows from its date and the bill's validity unless set by hand.
                    val period = target.periodFor(f.date)
                        ?: throw IllegalStateException("This bill has nothing due")
                    bills.recordPayment(target.bill, period, entity, f.saveAsTemplate)
                } else repository.add(entity, f.saveAsTemplate)
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
