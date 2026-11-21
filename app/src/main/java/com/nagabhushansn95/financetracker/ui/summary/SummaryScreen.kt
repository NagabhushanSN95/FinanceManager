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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.investments.Alert
import com.nagabhushansn95.financetracker.data.investments.InvestmentCategories
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummary
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.investments.InvestmentRoutes

/**
 * Home: alerts (deposits past maturity, premiums and cover falling due), account balances grouped by type, each
 * group collapsible (state remembered across launches), and the investments by category with the provident-fund
 * accounts. A single tap on an account does nothing; press and hold one to add a transaction to it. The investments
 * leave no trace until the "Cash & bank" card is pressed and held and the phone's unlock passed; holding it again
 * hides them. The alerts show names and dates only, so they stay visible while locked, but open nothing until the
 * investments are unlocked.
 */
@Composable
fun SummaryScreen(
    onAccountLongPress: (Long) -> Unit = {},
    /** Press-and-hold on a provident-fund row opens its screen under Investments: its entries are recorded there. */
    onOpenPfAccount: (Long) -> Unit = {},
    /** Press-and-hold on a category row opens its screen under Investments (null: the Investments hub itself). */
    onOpenInvestments: (String?) -> Unit = {},
    /** A tap on an alert opens the deposit, policy or vehicle it is about (only while the investments are unlocked). */
    onOpenAlert: (InstrumentType, Long) -> Unit = { _, _ -> },
    /** A tap on a bill's alert opens the bill; bills are not behind the unlock. */
    onOpenBill: (Long) -> Unit = {},
    investmentsUnlocked: Boolean = true,
    onToggleInvestments: () -> Unit = {},
    viewModel: SummaryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val investments by viewModel.investments.collectAsStateWithLifecycle()
    val investmentsCollapsed by viewModel.investmentsCollapsed.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()

    LazyColumn(
        // Room at the bottom so the last card can scroll clear of the + button.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (alerts.isNotEmpty()) item {
            AlertsCard(
                alerts,
                investmentsUnlocked,
                onOpenAlert,
                onOpenBill
            )
        }
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
                onAccountLongPress = onOpenPfAccount,
                onOpenInvestments = onOpenInvestments,
            )
        }
        else if (state.providentFunds.isNotEmpty()) item {
            ProvidentFundsCard(
                state,
                onOpenPfAccount
            )
        }
    }
}

/** What needs doing, soonest first; overdue lines in the error colour. */
@Composable
private fun AlertsCard(
    alerts: List<Alert>,
    unlocked: Boolean,
    onOpen: (InstrumentType, Long) -> Unit,
    onOpenBill: (Long) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                "Alerts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            // Month beginnings bring many lines at once: the list scrolls within a fixed height and draws its own
            // thumb, since the card sits inside Home's lazy column and would otherwise grow without bound.
            val scroll = rememberScrollState()
            val thumb = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            Column(
                modifier = Modifier
                    .heightIn(max = ALERTS_MAX_HEIGHT)
                    .drawWithContent {
                        drawContent()
                        if (scroll.maxValue in 1 until Int.MAX_VALUE) {
                            val viewport = size.height
                            val content = viewport + scroll.maxValue
                            val thumbHeight =
                                (viewport * viewport / content).coerceAtLeast(24.dp.toPx())
                            val top = (viewport - thumbHeight) * scroll.value / scroll.maxValue
                            drawRoundRect(
                                thumb,
                                topLeft = Offset(size.width - 6.dp.toPx(), top),
                                size = Size(3.dp.toPx(), thumbHeight),
                                cornerRadius = CornerRadius(2.dp.toPx()),
                            )
                        }
                    }
                    .verticalScroll(scroll)
            ) {
            alerts.forEach { a ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = a.billId != null || unlocked) {
                            val bill = a.billId
                            if (bill != null) onOpenBill(bill)
                            else if (a.instrumentType != null && a.instrumentId != null) onOpen(
                                a.instrumentType,
                                a.instrumentId
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(a.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            a.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (a.daysLeft <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        a.date.format(SHORT_DATE),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (a.daysLeft <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            }
        }
    }
}

/** About four and a half alert rows; more than that scrolls. */
private val ALERTS_MAX_HEIGHT = 220.dp

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
 * header folds the list like the account sections; press-and-hold on the header opens the Investments hub, on a
 * category its screen (deposits, holdings, provident funds, insurance), on a provident-fund account its screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InvestmentsCard(
    summary: InvestmentSummary,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onAccountLongPress: (Long) -> Unit,
    onOpenInvestments: (String?) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onToggle, onLongClick = { onOpenInvestments(null) })
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
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onOpenInvestments(categoryRoute(line.name)) })
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

/** The screen under Investments for a top-level category by its name; null (the hub) for a category of the user's own. */
private fun categoryRoute(name: String): String? = when {
    name.equals(
        InvestmentCategories.FIXED_DEPOSITS,
        ignoreCase = true
    ) -> InvestmentRoutes.FIXED_DEPOSITS

    name.equals(InvestmentCategories.STOCKS, ignoreCase = true) -> InvestmentRoutes.STOCKS
    name.equals(InvestmentCategories.MUTUAL_FUNDS, ignoreCase = true) -> InvestmentRoutes.FUNDS
    name.equals(
        InvestmentCategories.PROVIDENT_FUNDS,
        ignoreCase = true
    ) -> InvestmentRoutes.PROVIDENT_FUNDS

    name.equals(
        InvestmentCategories.LIFE_INSURANCE,
        ignoreCase = true
    ) -> InvestmentRoutes.INSURANCE

    else -> null
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
