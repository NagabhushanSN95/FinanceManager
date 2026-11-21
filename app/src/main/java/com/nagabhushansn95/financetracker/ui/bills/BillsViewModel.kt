package com.nagabhushansn95.financetracker.ui.bills

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.bills.BillPeriod
import com.nagabhushansn95.financetracker.data.bills.BillView
import com.nagabhushansn95.financetracker.data.bills.Bills
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Bill
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CreditAgency
import com.nagabhushansn95.financetracker.data.db.entity.CreditScore
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import com.nagabhushansn95.financetracker.data.repository.BillsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The sort keys of the Bills list; each starts in its own natural direction. */
enum class BillSort(val title: String, val naturallyDescending: Boolean) {
    DUE("Due date", false),
    NAME("Name", false),
    AMOUNT("Amount", true),
    LAST_PAID("Last paid", true);

    companion object {
        fun of(name: String): BillSort = entries.firstOrNull { it.name == name } ?: DUE
    }
}

object BillRoutes {
    const val BILL = "bills/{id}"
    const val CREDIT = "creditscore"
    fun bill(id: Long) = "bills/$id"

    /** The screens under Bills with their own top bar. */
    fun isSubRoute(route: String?) = route?.startsWith("bills/") == true || route == CREDIT
}

data class BillsState(
    val views: List<BillView> = emptyList(),
    val accounts: Map<Long, Account> = emptyMap(),
    /** The accounts a bill can be paid from or into: everything but the provident funds, deleted ones left out. */
    val cashAccounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    /** Newest first, for linking a row already in the ledger to a bill. */
    val ledger: List<TransactionEntity> = emptyList(),
    val linkedTransactionIds: Set<Long> = emptySet(),
    val agencies: List<CreditAgency> = emptyList(),
    val scores: List<CreditScore> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val loaded: Boolean = false,
) {
    private val categoriesById by lazy { categories.associateBy { it.id } }
    fun accountName(id: Long?): String = id?.let { accounts[it]?.name } ?: "—"
    fun categoryPath(id: Long?): String? {
        val c = id?.let { categoriesById[it] } ?: return null
        val parent = c.parentId?.let { categoriesById[it] }
        return if (parent != null) "${parent.name} › ${c.name}" else c.name
    }

    fun view(billId: Long): BillView? = views.firstOrNull { it.bill.id == billId }
    fun latestScore(agencyId: Long): CreditScore? =
        scores.filter { it.agencyId == agencyId }.maxWithOrNull(compareBy({ it.date }, { it.id }))
}

/** Bills, their payments and the credit scores, with every write that the Bills screens make. */
class BillsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val prefs = AppPreferences(application)
    private val repository = BillsRepository(db)

    /** One-off outcome of a save or delete, shown in a snackbar. */
    val message = MutableStateFlow<String?>(null)
    fun messageShown() {
        message.value = null
    }

    private val _sort = MutableStateFlow(BillSort.of(prefs.billsSort) to prefs.billsSortDescending)
    val sort: StateFlow<Pair<BillSort, Boolean>> = _sort

    fun pickSort(key: BillSort) = setSort(key to key.naturallyDescending)
    fun flipSort() = setSort(_sort.value.first to !_sort.value.second)
    private fun setSort(value: Pair<BillSort, Boolean>) {
        _sort.value = value
        prefs.billsSort = value.first.name
        prefs.billsSortDescending = value.second
    }

    val state: StateFlow<BillsState> = combine(
        combine(
            db.billDao().observeAll(),
            db.billDao().observePayments(),
            db.transactionDao().observeAll(),
        ) { bills, payments, ledger -> Triple(bills, payments, ledger) },
        db.accountDao().observeAll(),
        db.categoryDao().observeAll(),
        combine(db.billDao().observeAgencies(), db.billDao().observeScores()) { a, s -> a to s },
    ) { (bills, payments, ledger), accounts, categories, (agencies, scores) ->
        val today = LocalDate.now()
        val accountsById = accounts.associateBy { it.id }
        BillsState(
            views = Bills.build(
                bills,
                payments,
                ledger.associateBy { it.id },
                accountsById,
                today,
                ledger
            ),
            accounts = accountsById,
            cashAccounts = accounts.filter { !it.deleted && it.accountType != AccountType.PROVIDENT_FUND },
            categories = categories.filter { !it.deleted },
            ledger = ledger,
            linkedTransactionIds = payments.mapNotNull { it.transactionId }.toSet(),
            agencies = agencies,
            scores = scores,
            today = today,
            loaded = true,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillsState())

    /** Sorted as the chips say; ended bills keep their place by the same key. */
    fun sorted(views: List<BillView>, sort: Pair<BillSort, Boolean>): List<BillView> {
        val (key, descending) = sort
        val far = LocalDate.MAX
        val sorted = when (key) {
            BillSort.DUE -> views.sortedWith(
                compareBy(
                    { it.nextDue ?: far },
                    { it.bill.name.lowercase() })
            )

            BillSort.NAME -> views.sortedBy { it.bill.name.lowercase() }
            BillSort.AMOUNT -> views.sortedWith(
                compareBy(
                    { it.expected ?: -1.0 },
                    { it.bill.name.lowercase() })
            )

            BillSort.LAST_PAID -> views.sortedWith(
                compareBy(
                    { it.lastPaidOn ?: LocalDate.MIN },
                    { it.bill.name.lowercase() })
            )
        }
        return if (descending) sorted.asReversed() else sorted
    }

    fun save(bill: Bill, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                if (bill.id == 0L) repository.add(bill) else repository.update(bill).let { bill.id }
            }
                .onSuccess { onDone(it) }
                .onFailure { message.value = it.message ?: "Could not save the bill" }
        }
    }

    fun end(id: Long, on: LocalDate) = write("Could not end the bill") { repository.end(id, on) }
    fun reopen(id: Long) = write("Could not reopen the bill") { repository.reopen(id) }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onSuccess { onDone() }
                .onFailure { message.value = it.message ?: "Could not delete the bill" }
        }
    }

    fun skip(billId: Long, period: BillPeriod) =
        write("Could not skip the period") { repository.skip(billId, period) }

    fun skipAll(billId: Long, periods: List<BillPeriod>, note: String?) =
        write(
            "Could not skip the periods",
            if (periods.size == 1) "Skipped ${periods.single().label}" else "Skipped ${periods.size} periods"
        ) {
            repository.skipAll(billId, periods, note)
        }

    fun linkAll(bill: Bill, links: List<Pair<BillPeriod, Long>>) =
        write(
            "Could not link the transactions",
            if (links.size == 1) "Linked" else "Linked ${links.size} transactions"
        ) {
            repository.linkAll(bill, links)
        }

    fun unlink(paymentId: Long) = write("Could not unlink it") { repository.unlink(paymentId) }
    fun unlinkAll(paymentIds: Collection<Long>) =
        write(
            "Could not unlink them",
            if (paymentIds.size == 1) "Unlinked" else "Unlinked ${paymentIds.size} transactions"
        ) {
            repository.unlinkAll(paymentIds)
        }

    fun deletePayment(paymentId: Long) = write("Could not delete the payment", "Payment deleted") {
        repository.deletePayment(paymentId)
    }

    fun addAgency(name: String) = write("Could not add the agency") { repository.addAgency(name) }
    fun updateAgency(agency: CreditAgency) =
        write("Could not save the agency") { repository.updateAgency(agency) }

    fun deleteAgency(id: Long) =
        write("Could not delete the agency") { repository.deleteAgency(id) }

    fun addScore(agencyId: Long, date: LocalDate, score: Int, notes: String?) =
        write("Could not record the score") { repository.addScore(agencyId, date, score, notes) }

    fun updateScore(score: CreditScore) =
        write("Could not save the score") { repository.updateScore(score) }

    fun deleteScore(id: Long) = write("Could not delete the score") { repository.deleteScore(id) }

    private fun write(failure: String, success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { if (success != null) message.value = success }
                .onFailure { message.value = it.message ?: failure }
        }
    }
}
