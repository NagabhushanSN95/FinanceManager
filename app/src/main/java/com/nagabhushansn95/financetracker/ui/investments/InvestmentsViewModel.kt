package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.investments.InvestmentCategories
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummary
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummaryBuilder
import com.nagabhushansn95.financetracker.data.investments.ProvidentFunds
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import com.nagabhushansn95.financetracker.data.prices.AmfiNav
import com.nagabhushansn95.financetracker.data.prices.PriceRefresher
import com.nagabhushansn95.financetracker.data.prices.UrlHttpClient
import com.nagabhushansn95.financetracker.data.prices.YahooQuotes
import com.nagabhushansn95.financetracker.data.repository.InstrumentMaintenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Whether a price refresh is running and what the last one did. */
data class PriceStatus(val refreshing: Boolean = false, val message: String? = null)

data class InvestmentsUiState(val summary: InvestmentSummary? = null, val loaded: Boolean = false)

/** The live Investment Summary for any investments screen. */
open class InvestmentSummaryViewModel(application: Application) : AndroidViewModel(application) {
    protected val db: AppDatabase = AppDatabase.getInstance(application)

    val state: StateFlow<InvestmentsUiState> = InvestmentSummaryBuilder.observe(db)
        .map { InvestmentsUiState(summary = it, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvestmentsUiState())
}

/** What the Savings tile shows: the net saved this month and this year, from the sheet's Savings tables. */
data class SavingsGlance(val thisMonth: Double, val thisYear: Double)

/** The Investments hub: the summary plus the housekeeping that belongs to opening it. */
class InvestmentsViewModel(application: Application) : InvestmentSummaryViewModel(application) {
    private val prefs = AppPreferences(application)

    val savings: StateFlow<SavingsGlance?> = kotlinx.coroutines.flow.combine(
        db.transactionDao().observeAll(),
        db.instrumentDao().observeAll(),
        db.instrumentDao().observeCategories(),
        db.accountDao().observeAll(),
    ) { ledger, instruments, categories, accounts ->
        val today = java.time.LocalDate.now().toString()
        val month = com.nagabhushansn95.financetracker.data.investments.Savings.build(
            ledger,
            instruments,
            categories,
            accounts,
            monthly = true
        )
            .firstOrNull { it.key == today.take(7) }?.total ?: 0.0
        val year = com.nagabhushansn95.financetracker.data.investments.Savings.build(
            ledger,
            instruments,
            categories,
            accounts,
            monthly = false
        )
            .firstOrNull { it.key == today.take(4) }?.total ?: 0.0
        SavingsGlance(month, year)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _prices = MutableStateFlow(PriceStatus(message = prefs.lastPriceRefreshSummary))
    val prices: StateFlow<PriceStatus> = _prices

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // The category tree and risk levels on first open; retired holdings after 90 idle days.
            InvestmentCategories.seedIfEmpty(db)
            com.nagabhushansn95.financetracker.data.investments.SummaryGroups.seedIfEmpty(db)
            ProvidentFunds.fileCategories(db)
            com.nagabhushansn95.financetracker.data.investments.Insurance.fileCategories(db)
            InstrumentMaintenance(db).retireIdleHoldings()
            // Live prices at most once a day (or hour), when the setting allows it.
            if (prefs.autoPrices && System.currentTimeMillis() - prefs.lastPriceRefreshMillis > prefs.priceRefreshIntervalMillis) refreshPrices()
        }
    }

    /** Fetches stock prices, fund NAVs and the USD/INR rate now; the hub's "Update prices" button. */
    fun refreshPrices() {
        if (_prices.value.refreshing) return
        _prices.value = _prices.value.copy(refreshing = true)
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                runCatching {
                    val http = UrlHttpClient()
                    PriceRefresher(db, YahooQuotes(http), AmfiNav(http)).refresh().summary()
                }.getOrElse { "Price update failed: ${it.message ?: "no connection"}" }
            }
            prefs.lastPriceRefreshMillis = System.currentTimeMillis()
            prefs.lastPriceRefreshSummary = message
            _prices.value = PriceStatus(refreshing = false, message = message)
        }
    }
}
