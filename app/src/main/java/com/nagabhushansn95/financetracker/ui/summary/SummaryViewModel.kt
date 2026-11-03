package com.nagabhushansn95.financetracker.ui.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One collapsible block on the Summary screen. */
data class SummarySection(
    val type: AccountType,
    val title: String,
    val accounts: List<Account>,
    /** Sum of the INR accounts in this section. Foreign-currency accounts are listed but not summed. */
    val totalInr: Double,
    val collapsed: Boolean,
)

data class SummaryUiState(
    val sections: List<SummarySection> = emptyList(),
    /** Cash accounts only (wallets, banks, credit cards, demat), INR. Provident funds are shown separately. */
    val cashInr: Double = 0.0,
    val providentFundsInr: Double = 0.0,
    val loaded: Boolean = false,
)

class SummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val prefs = AppPreferences(application)

    private val collapsed = MutableStateFlow(
        AccountType.entries.associateWith { prefs.isSummarySectionCollapsed(it.name) }
    )

    val state: StateFlow<SummaryUiState> =
        combine(db.accountDao().observeVisible(), collapsed) { accounts, collapsedMap ->
            val sections = SECTION_ORDER.mapNotNull { type ->
                val ofType = accounts.filter { it.accountType == type }
                if (ofType.isEmpty()) null
                else SummarySection(
                    type = type,
                    title = titleOf(type),
                    accounts = ofType,
                    totalInr = ofType.filter { it.currency == "INR" }.sumOf { it.balance },
                    collapsed = collapsedMap[type] ?: false,
                )
            }
            SummaryUiState(
                sections = sections,
                cashInr = sections.filter { it.type != AccountType.PROVIDENT_FUND }
                    .sumOf { it.totalInr },
                providentFundsInr = sections.filter { it.type == AccountType.PROVIDENT_FUND }
                    .sumOf { it.totalInr },
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState())

    fun toggleSection(type: AccountType) {
        val now = !(collapsed.value[type] ?: false)
        prefs.setSummarySectionCollapsed(type.name, now)
        collapsed.value = collapsed.value + (type to now)
    }

    companion object {
        val SECTION_ORDER = listOf(
            AccountType.WALLET,
            AccountType.BANK,
            AccountType.CREDIT_CARD,
            AccountType.DEMAT,
            AccountType.PROVIDENT_FUND,
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
