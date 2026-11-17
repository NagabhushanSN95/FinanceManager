package com.nagabhushansn95.financetracker.ui.extras

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.backup.DatabaseBackup
import com.nagabhushansn95.financetracker.ui.common.DatabaseFilePickers

/** Backup and restore and the summary workbook export. Zerodha statements are imported under Investments. */
@Composable
fun ExtrasScreen(snackbarHostState: SnackbarHostState, viewModel: ExtrasViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmRestore by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }
    // The staged restore is swapped in when the process starts again: relaunch the app from scratch.
    LaunchedEffect(state.restartForRestore) {
        if (state.restartForRestore) {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (launch != null) context.startActivity(launch)
            Runtime.getRuntime().exit(0)
        }
    }

    val pickRestore =
        rememberLauncherForActivityResult(
            DatabaseFilePickers.Open(DatabaseFilePickers.BACKUP_FOLDER, viewModel::lastRestoreUri)
        ) { uri ->
            if (uri != null) {
                viewModel.rememberRestorePick(uri); confirmRestore = uri
            }
        }

    // Android 9 and below need the legacy storage permission for the Documents folder.
    val storagePermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.backupToDocuments()
        }
    val backupToDocuments = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) viewModel.backupToDocuments()
        else storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        ActionCard(
            title = "Back up data",
            description = "Copies the whole database, with the app's settings inside it, to Documents/${DatabaseBackup.BACKUP_FOLDER} as one file.",
            enabled = !state.busy,
            onClick = backupToDocuments,
        )
        state.lastBackup?.let {
            Text("Last backup: $it", style = MaterialTheme.typography.bodySmall)
        }
        ActionCard(
            title = "Restore data",
            description = "Replaces everything with a backup file. The current data is backed up first, then the app restarts with the restored data and settings.",
            enabled = !state.busy,
            onClick = { pickRestore.launch(DatabaseFilePickers.DATABASE_MIME_TYPES) },
        )
        ActionCard(
            title = "Export summary workbook",
            description = "An Excel file with the sheets of the old 'My Savings' workbook, computed from the ledger: savings by year and month, investment summary, deposits, provident funds, insurance, funds, stocks, others and prices. Saved to Documents/${com.nagabhushansn95.financetracker.data.export.SharedDocuments.EXPORT_FOLDER}.",
            enabled = !state.busy,
            onClick = viewModel::exportToDocuments,
        )
        state.lastExport?.let {
            Text("Last export: $it", style = MaterialTheme.typography.bodySmall)
        }
        state.lastImportSummary?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Finance Manager 4.x import", style = MaterialTheme.typography.titleMedium)
                    Text(
                        it.trimEnd(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    confirmRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text("Restore this backup?") },
            text = {
                Text(
                    "Everything in the app is replaced by the backup: accounts, transactions, instruments and settings. " +
                            "The current data is backed up to Documents/${DatabaseBackup.BACKUP_FOLDER} first. The app then restarts; open it again if it does not come back by itself."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = null; viewModel.restoreFrom(uri)
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Cancel") } },
        )
    }

}

@Composable
private fun ActionCard(title: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
