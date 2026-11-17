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
        The + button on Home or Transactions opens the editor for the three cash kinds; press and hold an account on Home to start with it chosen.
        Invest and Redeem are recorded on the instrument's own screen under Investments (the + on a stock, a fund, a deposit, a policy, a provident fund, a loan or an account under Others),
        and any row on an instrument in the ledger opens that screen when pressed and held. The + on a stock or fund offers Buy, Sell, Dividend, Bonus shares, Stock split and Charges.
    """,
    "Accounts and instruments" to """
        Accounts hold cash: wallets, banks, credit cards (negative balance = outstanding), demat and provident funds. They are added and edited under Manage › Accounts.
        Instruments are what you invest in; they never hold cash themselves. Each kind lives on its own screen under Investments:
        the + there adds one (a deposit, a policy, a stock, a fund, a loan), the pencil on its screen edits it, and its screen is where it is deleted.
        Provident funds are accounts, so they come from Manage › Accounts and show under Investments once added.
    """,
    "Provident funds" to """
        Record employee and employer contributions as Income into EPF with the contributor chip; PPF deposits as a Transfer from your bank;
        interest as Income with contributor "Interest". None of these count as income in Statistics; contributions count as savings, interest as growth.
    """,
    "Editing and deleting" to """
        A single tap on a list row does nothing, so stray touches are harmless. Press and hold a transaction, account, category
        or template to edit or delete it; press and hold an entry on an instrument's screen to do the same there. Deleting an account
        or category hides it; its transactions stay and it can be restored. Deleting an instrument from its screen removes it with its entries.
    """,
    "Statistics" to """
        Expenses, income, savings and withdrawals per month or year, straight from the ledger, plus a breakdown by category.
        Tick "Exclude from statistics" on a transaction to leave it out of the totals.
    """,
    "Backups and your data" to """
        Everything is stored on this phone only. Extras › Back up data writes one database file, with the settings inside, to
        Documents/Finance Tracker/Backups; Settings can do this automatically once a day and upload it to Google Drive. The file opens in any
        SQLite tool. Extras › Restore data brings a backup back (the current data is backed up first); the welcome screen of a fresh install can too.
        Clearing the app's storage or uninstalling deletes the live database, so keep backups.
    """,
    "Investments stay hidden" to """
        Nothing about investments shows until you press and hold the "Cash & bank" card on Home and pass the phone's own unlock
        (fingerprint, pattern or PIN). The Investments card and the menu entry then appear, and stay while the app is in use and for five
        minutes after it goes to the background. Press and hold the card again to hide them.
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
