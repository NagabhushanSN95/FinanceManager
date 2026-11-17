package com.nagabhushansn95.financetracker.ui.extras

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.backup.DatabaseBackup
import com.nagabhushansn95.financetracker.data.backup.DatabaseRestore
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExtrasUiState(
    val busy: Boolean = false,
    val lastBackup: String? = null,
    val lastImportSummary: String? = null,
    val lastExport: String? = null,
    /** One-off message for a snackbar; cleared with [ExtrasViewModel.messageShown]. */
    val message: String? = null,
    /** A restore is staged: the screen restarts the app so it is swapped in. */
    val restartForRestore: Boolean = false,
)

class ExtrasViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    private val db = AppDatabase.getInstance(application)
    private val backup = DatabaseBackup(application, db)
    private val workbook = com.nagabhushansn95.financetracker.data.export.SummaryWorkbook(db)

    private val _state = MutableStateFlow(
        ExtrasUiState(
            lastBackup = prefs.lastBackupDescription,
            lastImportSummary = prefs.lastImportSummary,
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

    /**
     * Restores a backup picked by the user: the current data is backed up to Documents first, the picked file is
     * checked and staged, its settings applied, and the app restarts to swap the database in.
     */
    fun lastRestoreUri(): Uri? = prefs.lastRestoreUri?.let(Uri::parse)

    /** Remembers the file picked for a restore, so the picker opens in its folder next time (even if not confirmed). */
    fun rememberRestorePick(uri: Uri) {
        prefs.lastRestoreUri = uri.toString()
    }

    fun restoreFrom(uri: Uri) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val safety = backup.saveToDocuments()
                    prefs.lastBackupDescription = safety.description
                    DatabaseRestore(getApplication()).stage(uri)
                    // Staging imports the backup's own preferences, which may carry an older pick: set it again.
                    prefs.lastRestoreUri = uri.toString()
                }
            }
            result.onSuccess {
                _state.value = _state.value.copy(busy = false, restartForRestore = true)
            }.onFailure { e ->
                Log.e("ExtrasViewModel", "Restore failed", e)
                _state.value =
                    _state.value.copy(busy = false, message = "Restore failed: ${e.message}")
            }
        }
    }


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

    fun messageShown() {
        _state.value = _state.value.copy(message = null)
    }
}
