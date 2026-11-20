package com.nagabhushansn95.financetracker.ui.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.investments.Alert
import com.nagabhushansn95.financetracker.data.investments.Alerts
import com.nagabhushansn95.financetracker.data.investments.FixedDeposits
import com.nagabhushansn95.financetracker.data.investments.Insurance
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummary
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummaryBuilder
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One collapsible block on the Home screen. */
data class SummarySection(
    val type: AccountType,
    val title: String,
    val accounts: List<Account>,
    /** Sum of the INR accounts in this section. Foreign-currency accounts are listed but not summed. */
    val totalInr: Double,
    val collapsed: Boolean,
)

data class SummaryUiState(
    /** Wallets, Banks (with Demat), Credit Cards. */
    val sections: List<SummarySection> = emptyList(),
    /** Cash accounts only (wallets, banks, credit cards, demat), INR. */
    val cashInr: Double = 0.0,
    /** Provident funds, shown under Investments (plan 4.1); instruments join them in milestone C. */
    val providentFunds: List<Account> = emptyList(),
    val providentFundsInr: Double = 0.0,
    val loaded: Boolean = false,
)

class SummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val prefs = AppPreferences(application)

    private val collapsed = MutableStateFlow(
        AccountType.entries.associateWith { prefs.isSummarySectionCollapsed(it.name) }
    )

    /** The Investments card folds like the account sections; remembered across launches under its own key. */
    private val _investmentsCollapsed =
        MutableStateFlow(prefs.isSummarySectionCollapsed(INVESTMENTS_SECTION))
    val investmentsCollapsed: StateFlow<Boolean> = _investmentsCollapsed

    fun toggleInvestments() {
        val now = !_investmentsCollapsed.value
        prefs.setSummarySectionCollapsed(INVESTMENTS_SECTION, now)
        _investmentsCollapsed.value = now
    }

    val state: StateFlow<SummaryUiState> =
        combine(db.accountDao().observeVisible(), collapsed) { accounts, collapsedMap ->
            val sections = SUMMARY_SECTIONS.mapNotNull { (type, members) ->
                val ofType = accounts.filter { it.accountType in members }
                if (ofType.isEmpty()) null
                else SummarySection(
                    type = type,
                    title = titleOf(type),
                    accounts = ofType,
                    totalInr = ofType.filter { it.currency == "INR" }.sumOf { it.balance },
                    collapsed = collapsedMap[type] ?: false,
                )
            }
            val pf = accounts.filter { it.accountType == AccountType.PROVIDENT_FUND }
            SummaryUiState(
                sections = sections,
                cashInr = sections.sumOf { it.totalInr },
                providentFunds = pf,
                providentFundsInr = pf.filter { it.currency == "INR" }.sumOf { it.balance },
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState())

    /** The Investment Summary, for the Investments card. */
    val investments: StateFlow<InvestmentSummary?> = InvestmentSummaryBuilder.observe(db)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Home's Alerts card: deposits past maturity, premiums falling due, cover, service and emission tests ending,
     * within the run-up set in Settings.
     */
    val alerts: StateFlow<List<Alert>> = combine(
        combine(
            db.instrumentDao().observeAll(),
            db.instrumentDao().observeFixedDeposits(),
            db.transactionDao().observeWithInstrument(),
            db.holdingEventDao().observeAll(),
            db.vehicleDao().observeVehicles(),
        ) { instruments, terms, ledger, events, vehicles ->
            val today = java.time.LocalDate.now()
            Quad(
                instruments,
                ledger,
                FixedDeposits.build(instruments, terms, ledger, today, events),
                vehicles,
            )
        },
        db.instrumentDao().observeInsurancePolicies(),
        com.nagabhushansn95.financetracker.data.investments.Validity.observeAll(db),
        prefs.alertThresholdsFlow,
    ) { (instruments, ledger, deposits, vehicles), policies, periods, thresholds ->
        val today = java.time.LocalDate.now()
        Alerts.build(
            deposits,
            Insurance.build(instruments, policies, ledger, periods, today, vehicles),
            today,
            thresholds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleSection(type: AccountType) {
        val now = !(collapsed.value[type] ?: false)
        prefs.setSummarySectionCollapsed(type.name, now)
        collapsed.value = collapsed.value + (type to now)
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    companion object {
        private const val INVESTMENTS_SECTION = "INVESTMENTS"

        /** Picker sections, one per type. */
        val SECTION_ORDER = listOf(
            AccountType.WALLET,
            AccountType.BANK,
            AccountType.CREDIT_CARD,
            AccountType.DEMAT,
            AccountType.PROVIDENT_FUND,
        )

        /** Summary sections: Demat accounts are listed under Banks (plan 4.1); provident funds go to Investments. */
        val SUMMARY_SECTIONS = listOf(
            AccountType.WALLET to setOf(AccountType.WALLET),
            AccountType.BANK to setOf(AccountType.BANK, AccountType.DEMAT),
            AccountType.CREDIT_CARD to setOf(AccountType.CREDIT_CARD),
        )

        fun titleOf(type: AccountType): String = when (type) {
            AccountType.WALLET -> "Wallets"
            AccountType.BANK -> "Banks"
            AccountType.CREDIT_CARD -> "Credit Cards"
            AccountType.DEMAT -> "Demat"
            AccountType.PROVIDENT_FUND -> "Provident Funds"
        }
    }
}
