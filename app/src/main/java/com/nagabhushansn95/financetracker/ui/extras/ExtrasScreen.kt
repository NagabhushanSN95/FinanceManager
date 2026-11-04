package com.nagabhushansn95.financetracker.ui.extras

import android.Manifest
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
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.backup.DatabaseBackup
import com.nagabhushansn95.financetracker.ui.common.DatabaseFilePickers

/** Backup actions and the Zerodha statement import. Export returns in milestone E. */
@Composable
fun ExtrasScreen(snackbarHostState: SnackbarHostState, viewModel: ExtrasViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val pickStatement = rememberLauncherForActivityResult(
        DatabaseFilePickers.OpenAny(
            "Download",
            viewModel::lastZerodhaStatementUri
        )
    ) { uri ->
        if (uri != null) viewModel.previewZerodha(uri)
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
            title = "Back up database file",
            description = "Copies the whole database to Documents/${DatabaseBackup.BACKUP_FOLDER}. Restore it from the welcome screen of a fresh install.",
            enabled = !state.busy,
            onClick = backupToDocuments,
        )
        state.lastBackup?.let {
            Text("Last backup: $it", style = MaterialTheme.typography.bodySmall)
        }
        ActionCard(
            title = "Export summary workbook",
            description = "An Excel file with the sheets of the old 'My Savings' workbook, computed from the ledger: savings by year and month, investment summary, deposits, provident funds, insurance, funds, stocks, others and prices. Saved to Documents/${com.nagabhushansn95.financetracker.data.export.SharedDocuments.EXPORT_FOLDER}.",
            enabled = !state.busy,
            onClick = viewModel::exportToDocuments,
        )
        state.lastExport?.let {
            Text("Last export: $it", style = MaterialTheme.typography.bodySmall)
        }
        ActionCard(
            title = "Import Zerodha statement",
            description = "A Coin order history, Kite tradebook, dividends or transaction-charges CSV. Rows already recorded are skipped.",
            enabled = !state.busy,
            onClick = { pickStatement.launch(arrayOf("*/*")) },
        )
        state.lastZerodhaImport?.let {
            Text("Last Zerodha import: $it", style = MaterialTheme.typography.bodySmall)
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

    state.zerodha?.let { preview ->
        ZerodhaImportSheet(
            preview = preview,
            busy = state.busy,
            onSettings = viewModel::updateZerodhaSettings,
            onImport = viewModel::commitZerodha,
            onDismiss = viewModel::dismissZerodha,
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
