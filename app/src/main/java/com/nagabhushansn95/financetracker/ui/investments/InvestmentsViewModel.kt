package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.investments.InvestmentCategories
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummary
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummaryBuilder
import com.nagabhushansn95.financetracker.data.repository.InstrumentMaintenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InvestmentsUiState(val summary: InvestmentSummary? = null, val loaded: Boolean = false)

/** The live Investment Summary for any investments screen. */
open class InvestmentSummaryViewModel(application: Application) : AndroidViewModel(application) {
    protected val db: AppDatabase = AppDatabase.getInstance(application)

    val state: StateFlow<InvestmentsUiState> = InvestmentSummaryBuilder.observe(db)
        .map { InvestmentsUiState(summary = it, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvestmentsUiState())
}

/** The Investments hub: the summary plus the housekeeping that belongs to opening it. */
class InvestmentsViewModel(application: Application) : InvestmentSummaryViewModel(application) {
    init {
        viewModelScope.launch(Dispatchers.IO) {
            // The category tree and risk levels on first open; retired holdings after 90 idle days.
            InvestmentCategories.seedIfEmpty(db)
            InstrumentMaintenance(db).retireIdleHoldings()
        }
    }
}
