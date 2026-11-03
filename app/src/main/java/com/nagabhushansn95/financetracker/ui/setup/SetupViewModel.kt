package com.nagabhushansn95.financetracker.ui.setup

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagabhushansn95.financetracker.data.backup.DatabaseRestore
import com.nagabhushansn95.financetracker.data.backup.DatabaseRestore.Companion.isSqliteFile
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.importer.v4.SqliteV4Source
import com.nagabhushansn95.financetracker.data.importer.v4.V4ImportReport
import com.nagabhushansn95.financetracker.data.importer.v4.V4Importer
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import com.nagabhushansn95.financetracker.data.seed.DefaultData
import com.nagabhushansn95.financetracker.data.seed.SetupChoices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

sealed interface SetupState {
    data object Choosing : SetupState
    data object Wizard : SetupState
    data class Working(val message: String) : SetupState
    data class Imported(val report: V4ImportReport) : SetupState
    data class Done(val title: String, val lines: List<String>) : SetupState
    data class Failed(val title: String, val message: String) : SetupState
}

class SetupViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)

    // Resolved per call: a restore closes and replaces the shared instance.
    private val db: AppDatabase get() = AppDatabase.getInstance(getApplication())

    private val _state = MutableStateFlow<SetupState>(SetupState.Choosing)
    val state: StateFlow<SetupState> = _state

    fun openWizard() {
        if (_state.value == SetupState.Choosing) _state.value = SetupState.Wizard
    }

    fun backToChoices() {
        _state.value = SetupState.Choosing
    }

    /** Option 1: the set-up wizard's result. */
    fun finishSetup(choices: SetupChoices) =
        work("Creating your accounts and categories…", "Set-up failed") {
            DefaultData.seed(db, choices)
            prefs.setupComplete = true
            val lines = buildList {
                add(
                    listOf(
                        "${choices.wallets.size} wallet(s)",
                        "${choices.banks.size} bank account(s)".takeIf { choices.banks.isNotEmpty() },
                        "${choices.creditCards.size} credit card(s)".takeIf { choices.creditCards.isNotEmpty() },
                    ).filterNotNull().joinToString(", ")
                )
                add(
                    "${choices.expenseCategories.size} expense categories, ${choices.incomeCategories.size} income categories" +
                            if (choices.sharedCategories.isNotEmpty()) ", ${choices.sharedCategories.size} for both" else ""
                )
                add("Demat and provident-fund accounts, and more of anything, can be added under Edit Data.")
            }
            SetupState.Done("Set-up complete", lines)
        }

    /** Option 2: restore a Finance Tracker database backup. */
    fun restoreFrom(uri: Uri) = work("Restoring the backup…", "Restore failed") {
        val result = DatabaseRestore(getApplication()).restoreFrom(uri)
        prefs.setupComplete = true
        SetupState.Done(
            "Restore complete",
            listOf(
                "%,d accounts".format(result.accounts),
                "%,d transactions".format(result.transactions)
            ),
        )
    }

    /** Option 3: skip set-up and start with a zero-balance wallet and default categories. */
    fun skipSetup() = work("Preparing your first wallet and categories…", "Set-up failed") {
        DefaultData.seed(db)
        prefs.setupComplete = true
        SetupState.Done(
            "Ready",
            listOf("A 'Cash' wallet with zero balance and a starter set of categories were created."),
        )
    }

    /** Option 4: one-time import of a Finance Manager 4.x database file. */
    fun importFrom(uri: Uri) = work("Reading the database file…", "Import failed") {
        val report = copyAndImport(uri)
        prefs.lastImportSummary = report.summary()
        prefs.setupComplete = true
        SetupState.Imported(report)
    }

    private fun work(message: String, failureTitle: String, block: suspend () -> SetupState) {
        if (_state.value is SetupState.Working) return
        _state.value = SetupState.Working(message)
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { block() } }
            result.onSuccess { _state.value = it }.onFailure { e ->
                Log.e(TAG, failureTitle, e)
                _state.value = SetupState.Failed(failureTitle, e.message ?: e.toString())
            }
        }
    }

    private suspend fun copyAndImport(uri: Uri): V4ImportReport {
        val context = getApplication<Application>()
        val temp = File(context.cacheDir, "v4-import.db")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Could not open the selected file")
            input.use { source -> temp.outputStream().use { source.copyTo(it) } }
            if (!temp.isSqliteFile()) {
                throw IOException("The selected file is not an SQLite database. Pick a 'Database Backup - ….db' file created by Finance Manager 4.5.4.")
            }
            _state.value = SetupState.Working("Importing transactions…")
            val started = System.currentTimeMillis()
            return SqliteV4Source(temp.path).use { source -> V4Importer(db).import(source) }.also {
                Log.i(
                    TAG,
                    "Imported ${it.transactions} transactions in ${System.currentTimeMillis() - started} ms"
                )
            }
        } finally {
            temp.delete()
        }
    }

    companion object {
        private const val TAG = "SetupViewModel"
    }
}
