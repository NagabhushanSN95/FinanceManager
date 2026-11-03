package com.nagabhushansn95.financetracker.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val SECTIONS = listOf(
    "Five kinds of transaction" to """
        Income adds to an account; Expense takes from one and needs a category; Transfer moves money between two accounts.
        Invest pays from an account into an instrument (stock, fund, deposit, policy); Redeem brings money back from one.
        The + button on Summary or Transactions opens the editor; tapping an account on Summary starts with it chosen.
    """,
    "Accounts and instruments" to """
        Accounts hold cash: wallets, banks, credit cards (negative balance = outstanding), demat and provident funds.
        Instruments are what you invest in. They never hold cash themselves; the Invest and Redeem rows carry the amounts and units.
        Both are managed under Manage, where lists can be reordered by their drag handle.
    """,
    "Provident funds" to """
        Record employee and employer contributions as Income into EPF with the contributor chip; PPF deposits as a Transfer from your bank;
        interest as Income with contributor "Interest". None of these count as income in Statistics; they count as savings.
    """,
    "Editing and deleting" to """
        A single tap on a list row does nothing, so stray touches are harmless. Press and hold a transaction, account, category,
        instrument or template to edit or delete it. Deleting an account or category hides it; its transactions stay and it can be restored.
    """,
    "Statistics" to """
        Expenses, income, savings and withdrawals per month or year, straight from the ledger, plus a breakdown by category.
        Untick "Include in statistics" on a transaction to leave it out of the totals.
    """,
    "Backups and your data" to """
        Everything is stored on this phone only. Extras writes a complete database file to Documents/Finance Tracker/Backups, or to any folder
        or cloud drive you pick; Settings can do this automatically once a day. The file opens in any SQLite tool and restores on a fresh install.
        Clearing the app's storage or uninstalling deletes the live database, so keep backups.
    """,
    "Disclaimer" to """
        The app does not connect to any bank or broker. Your data is yours; anyone with access to the phone can see it, so protect the phone.
        The developer is not responsible for losses arising from use of the app or from lost data.
    """,
)

@Composable
fun HelpScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SECTIONS.forEach { (title, body) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        body.trimIndent().replace("\n", " ").replace("  ", " ").trim(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
