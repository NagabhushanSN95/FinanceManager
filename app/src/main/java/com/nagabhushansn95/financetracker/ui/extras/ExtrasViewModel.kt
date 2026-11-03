package com.nagabhushansn95.financetracker.ui.extras

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.backup.DatabaseBackup
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
    /** One-off message for a snackbar; cleared with [ExtrasViewModel.messageShown]. */
    val message: String? = null,
)

class ExtrasViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    private val backup = DatabaseBackup(application, AppDatabase.getInstance(application))

    private val _state = MutableStateFlow(
        ExtrasUiState(
            lastBackup = prefs.lastBackupDescription,
            lastImportSummary = prefs.lastImportSummary
        )
    )
    val state: StateFlow<ExtrasUiState> = _state

    fun suggestedBackupFileName(): String = backup.suggestedFileName()

    fun backupToDocuments() = run("Backed up to ") { backup.saveToDocuments() }

    fun backupTo(uri: Uri) = run("Saved to ") { backup.saveTo(uri) }

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
