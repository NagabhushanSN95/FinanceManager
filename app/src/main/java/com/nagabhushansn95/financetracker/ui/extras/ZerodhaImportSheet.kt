package com.nagabhushansn95.financetracker.ui.extras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.importer.zerodha.Side
import com.nagabhushansn95.financetracker.data.importer.zerodha.StatementType
import com.nagabhushansn95.financetracker.data.importer.zerodha.TradeRow
import com.nagabhushansn95.financetracker.data.importer.zerodha.ZerodhaImportSettings
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import java.time.format.DateTimeFormatter

private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy")

/** What a Zerodha statement will add, with the account choices, before anything is written. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZerodhaImportSheet(
    preview: ZerodhaPreview,
    busy: Boolean,
    onSettings: (ZerodhaImportSettings) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val plan = preview.plan
    val rows = preview.statement.rows
    val hasStocks = rows.any { it is TradeRow && !it.mutualFund }
    val hasStockSales = rows.any { it is TradeRow && !it.mutualFund && it.side == Side.SELL }
    val needsDemat = hasStocks || plan.type == StatementType.CHARGES
    val needsBank =
        rows.any { it is TradeRow && it.mutualFund } || plan.type == StatementType.DIVIDENDS
    val ready =
        (!needsDemat || preview.settings.dematAccountId != null) && (!needsBank || preview.settings.bankAccountId != null)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(plan.type.title, style = MaterialTheme.typography.titleLarge)
            Text(
                listOfNotNull(
                    preview.fileName,
                    plan.from?.let { f -> "${f.format(DATE)} – ${plan.to?.format(DATE)}" }).joinToString(
                    " · "
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                buildString {
                    append("${plan.toInsert.size} new row${if (plan.toInsert.size == 1) "" else "s"}")
                    if (plan.duplicates > 0) append(" · ${plan.duplicates} already present")
                    if (plan.legacyMatches.isNotEmpty()) append(" (${plan.legacyMatches.size} recorded earlier, will get the reference)")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (plan.newInstruments.isNotEmpty()) Text(
                "New instruments: " + plan.newInstruments.joinToString {
                    it.name + (it.isin?.let { i -> " ($i)" } ?: "")
                },
                style = MaterialTheme.typography.bodySmall,
            )
            if (plan.skipped.isNotEmpty()) Text(
                "Skipped: " + plan.skipped.take(3)
                    .joinToString("; ") + if (plan.skipped.size > 3) " …" else "",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
            )
            if (needsDemat) AccountChoice(
                "Demat account",
                preview.accounts,
                preview.settings.dematAccountId
            ) { onSettings(preview.settings.copy(dematAccountId = it)) }
            if (needsBank) AccountChoice(
                "Bank account",
                preview.accounts,
                preview.settings.bankAccountId
            ) { onSettings(preview.settings.copy(bankAccountId = it)) }
            if (hasStockSales) Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Add DP charge of ${MoneyFormat.amount(preview.settings.dpCharge)} per sale",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = preview.settings.addDpCharges,
                    onCheckedChange = { onSettings(preview.settings.copy(addDpCharges = it)) })
            }
            HorizontalDivider()
            plan.toInsert.take(40).forEach { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        p.transaction.date.format(DATE),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(88.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.label, style = MaterialTheme.typography.bodySmall)
                        p.transaction.quantity?.let { q ->
                            Text(
                                "${trim(q)} @ ${trim(p.transaction.rate ?: 0.0)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        MoneyFormat.amount(p.transaction.amount),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (plan.toInsert.size > 40) Text(
                "… and ${plan.toInsert.size - 40} more",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                enabled = ready && !busy && (plan.toInsert.isNotEmpty() || plan.legacyMatches.isNotEmpty()),
                onClick = onImport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (plan.toInsert.isEmpty()) "Nothing new to import" else "Import ${plan.toInsert.size} row${if (plan.toInsert.size == 1) "" else "s"}")
            }
        }
    }
}

private fun trim(v: Double) =
    if (v == v.toLong().toDouble()) v.toLong().toString() else "%.3f".format(v).trimEnd('0')
        .trimEnd('.')

@Composable
private fun AccountChoice(
    label: String,
    accounts: List<Account>,
    selected: Long?,
    onPick: (Long) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(120.dp))
        TextButton(onClick = {
            open = true
        }) { Text(accounts.firstOrNull { it.id == selected }?.name ?: "Choose…") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            accounts.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.name) },
                    onClick = { onPick(a.id); open = false })
            }
        }
    }
}
