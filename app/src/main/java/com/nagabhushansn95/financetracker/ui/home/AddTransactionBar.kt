package com.nagabhushansn95.financetracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.ui.theme.ClassicBar
import com.nagabhushansn95.financetracker.ui.theme.LocalClassicTheme

/**
 * The three add buttons at the foot of Home and Transactions, in Finance Manager's order: Income, Expense,
 * Transfer. One tap opens the editor on that kind; the kind can still be changed there.
 */
@Composable
fun AddTransactionBar(onAdd: (TransactionKind) -> Unit) {
    val classic = LocalClassicTheme.current
    Surface(
        color = if (classic) ClassicBar else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (classic) Color.White else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(56.dp),
        ) {
            AddButton("Income", "+", Modifier.weight(1f)) { onAdd(TransactionKind.INCOME) }
            AddButton("Expense", "\u2212", Modifier.weight(1f)) { onAdd(TransactionKind.EXPENSE) }
            AddButton("Transfer", "\u21C4", Modifier.weight(1f)) { onAdd(TransactionKind.TRANSFER) }
        }
    }
}

@Composable
private fun AddButton(label: String, glyph: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(glyph, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
