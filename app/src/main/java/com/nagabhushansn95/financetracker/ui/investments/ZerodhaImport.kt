package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.importer.zerodha.ImportPlan
import com.nagabhushansn95.financetracker.data.importer.zerodha.ParsedStatement
import com.nagabhushansn95.financetracker.data.importer.zerodha.Side
import com.nagabhushansn95.financetracker.data.importer.zerodha.StatementType
import com.nagabhushansn95.financetracker.data.importer.zerodha.TradeRow
import com.nagabhushansn95.financetracker.data.importer.zerodha.ZerodhaImportSettings
import com.nagabhushansn95.financetracker.data.importer.zerodha.ZerodhaImporter
import com.nagabhushansn95.financetracker.data.importer.zerodha.ZerodhaStatements
import com.nagabhushansn95.financetracker.data.investments.FdMath
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The statements the menus of the Stocks & ETFs and Mutual funds screens import, each from its own entry. */
enum class ZerodhaImportKind(val title: String, val type: StatementType) {
    TRADEBOOK("Kite tradebook", StatementType.KITE_TRADEBOOK),
    DIVIDENDS("Dividends", StatementType.DIVIDENDS),
    CHARGES("Transaction charges", StatementType.CHARGES),
    COIN("Coin orders", StatementType.COIN_ORDERS);

    /** Where the statement is imported from, for the message when a file lands on the wrong screen. */
    val screen: String get() = if (this == COIN) "Mutual funds" else "Stocks & ETFs"
}

/** A parsed statement with its import plan, shown in the preview sheet until imported or dismissed. */
data class ZerodhaPreview(
    val fileName: String,
    val statement: ParsedStatement,
    val settings: ZerodhaImportSettings,
    val plan: ImportPlan,
    val accounts: List<Account>,
)

data class ZerodhaImportState(
    val busy: Boolean = false,
    val preview: ZerodhaPreview? = null,
    val lastImport: String? = null,
    val message: String? = null,
)

/**
 * Reads a Zerodha CSV, plans what it adds and writes it. A Kite tradebook on the stocks screen keeps only its
 * equity rows (fund orders come with the Coin import); the Coin import takes the order history and the MF tradebook
 * together, so every order carries its allotment price.
 */
class ZerodhaImportViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    private val db = AppDatabase.getInstance(application)
    private val importer = ZerodhaImporter(db)

    private val _state =
        MutableStateFlow(ZerodhaImportState(lastImport = prefs.lastZerodhaImportSummary))
    val state: StateFlow<ZerodhaImportState> = _state

    /** Where the file picker should open: the file picked last time, if any. */
    fun lastStatementUri(): Uri? = prefs.lastZerodhaStatementUri?.let(Uri::parse)

    fun fileName(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver
        runCatching {
            resolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        }
        return uri.lastPathSegment ?: "statement"
    }

    private fun read(uri: Uri): String =
        getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalStateException("Cannot open ${fileName(uri)}")

    /** One file: a tradebook, a dividends list or the charges CSV, checked against the menu entry it came from. */
    fun preview(kind: ZerodhaImportKind, uri: Uri) = start(fileName(uri), uri) {
        val parsed = ZerodhaStatements.parse(read(uri))
        if (parsed.type != kind.type) {
            val of = ZerodhaImportKind.entries.first { it.type == parsed.type }
            throw IllegalArgumentException("this is a ${parsed.type.title}; import it as ${of.title} under ${of.screen}")
        }
        if (kind == ZerodhaImportKind.TRADEBOOK) {
            val funds = parsed.rows.count { it is TradeRow && it.mutualFund }
            if (funds > 0) parsed.copy(
                rows = parsed.rows.filterNot { it is TradeRow && it.mutualFund },
                skipped = parsed.skipped + "$funds fund row${if (funds == 1) "" else "s"} left out: fund orders come in with the Coin import under Mutual funds",
            ) else parsed
        } else parsed
    }

    /** The Coin order history with the MF tradebook that carries its allotment prices. */
    fun previewCoin(coinUri: Uri, tradebookUri: Uri) = start(fileName(coinUri), coinUri) {
        val coin = ZerodhaStatements.parse(read(coinUri))
        val tradebook = ZerodhaStatements.parse(read(tradebookUri))
        ZerodhaStatements.mergeCoinWithTradebook(coin, tradebook)
    }

    private fun start(name: String, uri: Uri, parse: suspend () -> ParsedStatement) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true)
        prefs.lastZerodhaStatementUri = uri.toString()
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val statement = parse()
                    val accounts = db.accountDao().getVisible()
                    val inferred = importer.defaultSettings()
                    val settings = ZerodhaImportSettings(
                        dematAccountId = prefs.zerodhaDematAccountId.takeIf { id -> accounts.any { it.id == id } }
                            ?: inferred.dematAccountId,
                        bankAccountId = prefs.zerodhaBankAccountId.takeIf { id -> accounts.any { it.id == id } }
                            ?: inferred.bankAccountId,
                    )
                    ZerodhaPreview(
                        name,
                        statement,
                        settings,
                        importer.plan(statement, settings),
                        accounts
                    )
                }
            }
            result.onSuccess { _state.value = _state.value.copy(busy = false, preview = it) }
                .onFailure { e ->
                    Log.e(TAG, "Zerodha preview failed", e)
                    _state.value =
                        _state.value.copy(busy = false, message = "Cannot import: ${e.message}")
                }
        }
    }

    /** Re-plans with different accounts or DP-charge choice. */
    fun updateSettings(settings: ZerodhaImportSettings) {
        val current = _state.value.preview ?: return
        viewModelScope.launch {
            val plan = withContext(Dispatchers.IO) { importer.plan(current.statement, settings) }
            _state.value =
                _state.value.copy(preview = current.copy(settings = settings, plan = plan))
        }
    }

    fun commit() {
        val preview = _state.value.preview ?: return
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            val result =
                runCatching { withContext(Dispatchers.IO) { importer.commit(preview.plan) } }
            result.onSuccess { r ->
                preview.settings.dematAccountId?.let { prefs.zerodhaDematAccountId = it }
                preview.settings.bankAccountId?.let { prefs.zerodhaBankAccountId = it }
                val summary = "${
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                } · ${preview.fileName}\n" +
                        r.summary(preview.plan.type)
                prefs.lastZerodhaImportSummary = summary
                _state.value = _state.value.copy(
                    busy = false,
                    preview = null,
                    lastImport = summary,
                    message = r.summary(preview.plan.type)
                )
            }.onFailure { e ->
                Log.e(TAG, "Zerodha import failed", e)
                _state.value =
                    _state.value.copy(busy = false, message = "Import failed: ${e.message}")
            }
        }
    }

    fun dismiss() {
        _state.value = _state.value.copy(preview = null)
    }

    fun messageShown() {
        _state.value = _state.value.copy(message = null)
    }

    companion object {
        private const val TAG = "ZerodhaImport"
    }
}

private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy")

/** What a statement will add, with the account choices, before anything is written. */
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
    // A dividend waits for its credit, so no bank is chosen here; the entry sheet asks when it is recorded.
    val needsBank = rows.any { it is TradeRow && it.mutualFund }
    val ready =
        (!needsDemat || preview.settings.dematAccountId != null) && (!needsBank || preview.settings.bankAccountId != null)
    val count = plan.toInsert.size + plan.toHold.size
    val dividends = plan.type == StatementType.DIVIDENDS

    FormSheet(title = plan.type.title, onDismiss = onDismiss) {
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
                if (dividends) append("$count dividend${if (count == 1) "" else "s"} to await the bank credit")
                else append("$count new row${if (count == 1) "" else "s"}")
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
            "Notes: " + plan.skipped.take(3)
                .joinToString("; ") + if (plan.skipped.size > 3) " …" else "",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
        )
        if (needsDemat) AccountChoice(
            "Demat account",
            preview.accounts,
            preview.settings.dematAccountId
        ) {
            onSettings(preview.settings.copy(dematAccountId = it))
        }
        if (needsBank) AccountChoice(
            "Bank account",
            preview.accounts,
            preview.settings.bankAccountId
        ) {
            onSettings(preview.settings.copy(bankAccountId = it))
        }
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
            PreviewRow(
                p.transaction.date.format(DATE), p.label,
                p.transaction.quantity?.let { q -> "${FdMath.plain(q)} @ ${FdMath.plain(p.transaction.rate ?: 0.0)}" },
                MoneyFormat.amount(p.transaction.amount),
            )
        }
        plan.toHold.take(40).forEach { p ->
            PreviewRow(
                "ex ${p.dividend.exDate.format(DATE)}", p.label,
                "${FdMath.plain(p.dividend.quantity)} × ${FdMath.plain(p.dividend.perShare)}",
                MoneyFormat.amount(p.dividend.gross),
            )
        }
        if (count > 40) Text("… and ${count - 40} more", style = MaterialTheme.typography.bodySmall)
        if (dividends && count > 0) Text(
            "They show under the stock as awaiting credit; tap one there when the bank credits it to enter the date and the amount received.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            enabled = ready && !busy && (count > 0 || plan.legacyMatches.isNotEmpty()),
            onClick = onImport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    count == 0 -> "Nothing new to import"
                    dividends -> "Hold $count dividend${if (count == 1) "" else "s"} for credit"
                    else -> "Import $count row${if (count == 1) "" else "s"}"
                }
            )
        }
    }
}

@Composable
private fun PreviewRow(date: String, label: String, detail: String?, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(date, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(96.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(amount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

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

/** The two files a Coin import takes: the order history (amounts, order dates) and the MF tradebook (allotment prices). */
@Composable
fun CoinImportDialog(
    coinName: String?,
    tradebookName: String?,
    onPickCoin: () -> Unit,
    onPickTradebook: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Coin orders") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "The order history gives each order's amount and date; the MF tradebook gives its allotment price. Both come from Console.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onPickCoin, modifier = Modifier.fillMaxWidth()) {
                    Text(coinName?.let { "Order history: $it" } ?: "Choose the Coin order history…")
                }
                OutlinedButton(onClick = onPickTradebook, modifier = Modifier.fillMaxWidth()) {
                    Text(tradebookName?.let { "MF tradebook: $it" } ?: "Choose the MF tradebook…")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onImport,
                enabled = coinName != null && tradebookName != null
            ) { Text("Preview") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
