package com.nagabhushansn95.financetracker.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity

/** Every delete of a ledger row goes through this: the row is named, and the balance effect spelled out, before it goes. */
@Composable
fun ConfirmDeleteDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** The dialog for one ledger row: its date, particulars and amount, and the note that the balances are put back. */
@Composable
fun ConfirmDeleteEntryDialog(
    row: TransactionEntity,
    currency: String = "INR",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmDeleteDialog(
        title = "Delete this entry?",
        text = listOfNotNull(
            row.date.format(SHORT_DATE),
            row.particulars.takeIf { it.isNotBlank() },
            MoneyFormat.amount(row.amount, currency),
        ).joinToString(" · ") + "\nThe account balances are adjusted back.",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
