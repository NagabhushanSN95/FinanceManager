package com.nagabhushansn95.financetracker.ui.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.investments.PfEntryKind
import com.nagabhushansn95.financetracker.data.investments.ProvidentFunds
import com.nagabhushansn95.financetracker.ui.common.AccountField
import com.nagabhushansn95.financetracker.ui.common.ChoiceDropdown
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.NumberField
import java.time.LocalDate

/** Formats an amount for an editable field: no exponent, no trailing zeros. */
private fun plain(value: Double): String =
    if (value == Math.rint(value)) value.toLong().toString() else "%.2f".format(value).trimEnd('0')
        .trimEnd('.')

/**
 * One provident-fund entry: the kind (from the account's "+" menu, changeable here), its date and amount, the bank
 * account when money moves against one, and the particulars (the last entry of the kind's wording, else the house
 * style, unless typed; the keyboard capitalises each word as 4.x did). A new
 * contribution starts with the amount of the last one of its kind ([defaultAmounts]), so a regular deposit is a
 * glance and a tap. The button reads "Record" for a new entry and "Save" for an existing one, which can also be
 * deleted from here.
 */
@Composable
fun ProvidentFundEntrySheet(
    account: Account,
    banks: List<Account>,
    defaultBankId: Long?,
    defaultAmounts: Map<PfEntryKind, Double> = emptyMap(),
    /** The account's ledger rows, for the wording of the last entry of each kind. */
    rows: List<TransactionEntity> = emptyList(),
    initialKind: PfEntryKind,
    existing: TransactionEntity?,
    onSave: (PfEntryKind, LocalDate, Double, String, Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val kinds = ProvidentFunds.kindsFor(account)
    var kind by remember { mutableStateOf(initialKind) }
    var date by remember { mutableStateOf<LocalDate?>(existing?.date ?: LocalDate.now()) }
    var amount by remember { mutableStateOf(existing?.amount?.let(::plain) ?: "") }
    var amountTyped by remember { mutableStateOf(existing != null) }
    val amountShown = if (amountTyped) amount else defaultAmounts[kind]?.let(::plain) ?: ""
    var bankId by remember {
        mutableStateOf(
            existing?.let { if (it.fromAccountId == account.id) it.toAccountId else it.fromAccountId }
                ?: defaultBankId ?: banks.firstOrNull()?.id
        )
    }
    // Particulars follow the kind and date until the user types their own.
    var particulars by remember { mutableStateOf(existing?.particulars ?: "") }
    var typed by remember { mutableStateOf(existing != null) }
    val suggested = ProvidentFunds.suggestedParticulars(kind, account, rows)
    val value = amountShown.toDoubleOrNull()
    val valid = date != null && value != null && value > 0 && (!kind.transfer || bankId != null)
    FormSheet(
        title = if (existing == null) kind.title else "Edit ${kind.title.lowercase()}",
        onDismiss = onDismiss
    ) {
        ChoiceDropdown("Entry", kinds, kind, { it.title }) { kind = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Date", date, { date = it }, Modifier.weight(1f))
            NumberField(
                amountShown,
                "Amount",
                { amount = it; amountTyped = true },
                Modifier.weight(1f)
            )
        }
        kind.bankLabel?.let { label ->
            AccountField(label, banks, bankId, { bankId = it }, Modifier.fillMaxWidth())
        }
        OutlinedTextField(
            value = if (typed) particulars else suggested,
            onValueChange = { particulars = it; typed = true },
            label = { Text("Particulars") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            when (kind) {
                PfEntryKind.INTEREST_CREDIT -> "Interest stays in the account and counts as its return, not as a contribution."
                PfEntryKind.TRANSFER_IN -> "The balance moved in from a previous employer's account counts as a contribution made earlier."
                PfEntryKind.WITHDRAWAL -> "The amount leaves the account into the bank account chosen above."
                PfEntryKind.SELF_CONTRIBUTION -> "Paid into the account from the bank account chosen above."
                else -> "Credited to the account from your salary; no bank account of yours is touched."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    kind,
                    date!!,
                    value!!,
                    if (typed) particulars else suggested,
                    if (kind.transfer) bankId else null
                )
            },
        ) { Text(if (existing == null) "Record" else "Save") }
        if (onDelete != null) OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete entry", color = MaterialTheme.colorScheme.error)
        }
    }
}
