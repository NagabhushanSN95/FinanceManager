package com.nagabhushansn95.financetracker.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummary
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat

/**
 * Home: account balances grouped by type, each group collapsible (state remembered across launches), and the
 * investments by category with the provident-fund accounts. A single tap on an account does nothing; press and
 * hold one to add a transaction to it. The investments leave no trace until the "Cash & bank" card is pressed and
 * held and the phone's unlock passed; holding it again hides them.
 */
@Composable
fun SummaryScreen(
    onAccountLongPress: (Long) -> Unit = {},
    investmentsUnlocked: Boolean = true,
    onToggleInvestments: () -> Unit = {},
    viewModel: SummaryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val investments by viewModel.investments.collectAsStateWithLifecycle()
    val investmentsCollapsed by viewModel.investmentsCollapsed.collectAsStateWithLifecycle()

    LazyColumn(
        // Room at the bottom so the last card can scroll clear of the + button.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TotalsCard(state, onLongPress = onToggleInvestments) }
        items(state.sections, key = { it.type }) { section ->
            SectionCard(
                section,
                onToggle = { viewModel.toggleSection(section.type) },
                onAccountLongPress = onAccountLongPress
            )
        }
        val summary = investments
        if (!investmentsUnlocked) Unit
        else if (summary != null && summary.categories.isNotEmpty()) item {
            InvestmentsCard(
                summary,
                collapsed = investmentsCollapsed,
                onToggle = viewModel::toggleInvestments,
                onAccountLongPress = onAccountLongPress,
            )
        }
        else if (state.providentFunds.isNotEmpty()) item {
            ProvidentFundsCard(
                state,
                onAccountLongPress
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TotalsCard(state: SummaryUiState, onLongPress: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Cash & bank", style = MaterialTheme.typography.labelLarge)
            Text(MoneyFormat.amount(state.cashInr), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/**
 * Investments by top-level category (current value), with the provident-fund accounts listed under theirs. The
 * header folds the list like the account sections; the Investments screens are opened from the drawer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InvestmentsCard(
    summary: InvestmentSummary,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onAccountLongPress: (Long) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Investments",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                MoneyFormat.amount(summary.total.current),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = if (collapsed) "Expand" else "Collapse",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        AnimatedVisibility(visible = !collapsed) {
            Column {
        HorizontalDivider()
        summary.categories.forEach { line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    line.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(MoneyFormat.amount(line.current), style = MaterialTheme.typography.bodyLarge)
            }
            line.children.filter { it.accountId != null }.forEach { pf ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { onAccountLongPress(pf.accountId!!) })
                        .padding(start = 32.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        pf.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        MoneyFormat.amount(pf.current),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
            }
        }
    }
}

/** Fallback while the investment summary is still loading: the provident-fund balances. */
@Composable
private fun ProvidentFundsCard(state: SummaryUiState, onAccountLongPress: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Investments",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                MoneyFormat.amount(state.providentFundsInr),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        HorizontalDivider()
        state.providentFunds.forEach { AccountRow(it, onLongPress = { onAccountLongPress(it.id) }) }
    }
}

@Composable
private fun SectionCard(
    section: SummarySection,
    onToggle: () -> Unit,
    onAccountLongPress: (Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                section.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                MoneyFormat.amount(section.totalInr),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = if (section.collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = if (section.collapsed) "Expand" else "Collapse",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        AnimatedVisibility(visible = !section.collapsed) {
            Column {
                HorizontalDivider()
                section.accounts.forEach {
                    AccountRow(
                        it,
                        onLongPress = { onAccountLongPress(it.id) })
                }
            }
        }
    }
}

/** A tap does nothing (stray touches must not open anything); press and hold adds a transaction to the account. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountRow(account: Account, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            account.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            MoneyFormat.amount(account.balance, account.currency),
            style = MaterialTheme.typography.bodyLarge,
            color = if (account.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
