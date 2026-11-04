package com.nagabhushansn95.financetracker.ui.extras

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.backup.DatabaseBackup
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.importer.zerodha.ImportPlan
import com.nagabhushansn95.financetracker.data.importer.zerodha.ParsedStatement
import com.nagabhushansn95.financetracker.data.importer.zerodha.ZerodhaImportSettings
import com.nagabhushansn95.financetracker.data.importer.zerodha.ZerodhaImporter
import com.nagabhushansn95.financetracker.data.importer.zerodha.ZerodhaStatements
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A parsed Zerodha statement with its import plan, shown in the preview sheet until imported or dismissed. */
data class ZerodhaPreview(
    val fileName: String,
    val statement: ParsedStatement,
    val settings: ZerodhaImportSettings,
    val plan: ImportPlan,
    val accounts: List<Account>,
)

data class ExtrasUiState(
    val busy: Boolean = false,
    val lastBackup: String? = null,
    val lastImportSummary: String? = null,
    val lastZerodhaImport: String? = null,
    val lastExport: String? = null,
    val zerodha: ZerodhaPreview? = null,
    /** One-off message for a snackbar; cleared with [ExtrasViewModel.messageShown]. */
    val message: String? = null,
)

class ExtrasViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    private val db = AppDatabase.getInstance(application)
    private val backup = DatabaseBackup(application, db)
    private val importer = ZerodhaImporter(db)
    private val workbook = com.nagabhushansn95.financetracker.data.export.SummaryWorkbook(db)

    private val _state = MutableStateFlow(
        ExtrasUiState(
            lastBackup = prefs.lastBackupDescription,
            lastImportSummary = prefs.lastImportSummary,
            lastZerodhaImport = prefs.lastZerodhaImportSummary,
            lastExport = prefs.lastExportDescription,
        )
    )
    val state: StateFlow<ExtrasUiState> = _state


    /** Writes the summary workbook to Documents/Finance Tracker/Exports. */
    fun exportToDocuments() = export {
        com.nagabhushansn95.financetracker.data.export.SharedDocuments.write(
            getApplication(),
            com.nagabhushansn95.financetracker.data.export.SharedDocuments.EXPORT_FOLDER,
            workbook.suggestedFileName(),
            com.nagabhushansn95.financetracker.data.export.SharedDocuments.XLSX_MIME,
        ) { workbook.write(it) }
    }

    private fun export(action: suspend () -> String) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { action() } }
            result.onSuccess { prefs.lastExportDescription = it }
            _state.value = _state.value.copy(
                busy = false,
                lastExport = prefs.lastExportDescription,
                message = result.fold({ "Exported to $it" }, { "Export failed: ${it.message}" }),
            )
        }
    }

    fun backupToDocuments() = run("Backed up to ") { backup.saveToDocuments() }


    private fun run(successPrefix: String, action: suspend () -> DatabaseBackup.Result) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { action() } }
            result.onSuccess {
                prefs.lastBackupDescription = it.description
                _state.value = _state.value.copy(
                    busy = false,
                    lastBackup = it.description,
                    message = successPrefix + it.description
                )
            }.onFailure { e ->
                Log.e("ExtrasViewModel", "Backup failed", e)
                _state.value =
                    _state.value.copy(busy = false, message = "Backup failed: ${e.message}")
            }
        }
    }

    // ------------------------------------------------------------------ Zerodha

    /** Where the statement picker should open: the file picked last time, if any. */
    fun lastZerodhaStatementUri(): Uri? = prefs.lastZerodhaStatementUri?.let(Uri::parse)

    /** Reads and parses the picked statement and builds the plan with the remembered (or inferred) accounts. */
    fun previewZerodha(uri: Uri) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true)
        prefs.lastZerodhaStatementUri = uri.toString()
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val text =
                        resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: throw IllegalStateException("Cannot open the file")
                    val statement = ZerodhaStatements.parse(text)
                    val accounts = db.accountDao().getVisible()
                    val inferred = importer.defaultSettings()
                    val settings = ZerodhaImportSettings(
                        dematAccountId = prefs.zerodhaDematAccountId.takeIf { id -> accounts.any { it.id == id } }
                            ?: inferred.dematAccountId,
                        bankAccountId = prefs.zerodhaBankAccountId.takeIf { id -> accounts.any { it.id == id } }
                            ?: inferred.bankAccountId,
                    )
                    ZerodhaPreview(
                        fileName(uri),
                        statement,
                        settings,
                        importer.plan(statement, settings),
                        accounts
                    )
                }
            }
            result.onSuccess { _state.value = _state.value.copy(busy = false, zerodha = it) }
                .onFailure { e ->
                    Log.e("ExtrasViewModel", "Zerodha preview failed", e)
                    _state.value =
                        _state.value.copy(busy = false, message = "Cannot import: ${e.message}")
                }
        }
    }

    /** Re-plans with different accounts or DP-charge choice. */
    fun updateZerodhaSettings(settings: ZerodhaImportSettings) {
        val current = _state.value.zerodha ?: return
        viewModelScope.launch {
            val plan = withContext(Dispatchers.IO) { importer.plan(current.statement, settings) }
            _state.value =
                _state.value.copy(zerodha = current.copy(settings = settings, plan = plan))
        }
    }

    fun commitZerodha() {
        val preview = _state.value.zerodha ?: return
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
                } · ${preview.fileName}\n" + r.summary(preview.plan.type)
                prefs.lastZerodhaImportSummary = summary
                _state.value = _state.value.copy(
                    busy = false,
                    zerodha = null,
                    lastZerodhaImport = summary,
                    message = r.summary(preview.plan.type)
                )
            }.onFailure { e ->
                Log.e("ExtrasViewModel", "Zerodha import failed", e)
                _state.value =
                    _state.value.copy(busy = false, message = "Import failed: ${e.message}")
            }
        }
    }

    fun dismissZerodha() {
        _state.value = _state.value.copy(zerodha = null)
    }

    private fun fileName(uri: Uri): String {
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

    fun messageShown() {
        _state.value = _state.value.copy(message = null)
    }
}
