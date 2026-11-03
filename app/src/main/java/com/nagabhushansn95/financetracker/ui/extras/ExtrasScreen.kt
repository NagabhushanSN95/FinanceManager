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

/** Backup actions for the v5 database. Export, Zerodha import and restore return in later milestones. */
@Composable
fun ExtrasScreen(snackbarHostState: SnackbarHostState, viewModel: ExtrasViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val saveAs = rememberLauncherForActivityResult(
        DatabaseFilePickers.Create(DatabaseFilePickers.BACKUP_FOLDER)
    ) { uri -> if (uri != null) viewModel.backupTo(uri) }

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
            description = "Copies the whole database to Documents/${DatabaseBackup.BACKUP_FOLDER}.",
            enabled = !state.busy,
            onClick = backupToDocuments,
        )
        ActionCard(
            title = "Save database file to…",
            description = "Pick a folder or a cloud drive (Google Drive, OneDrive…) with the system file picker.",
            enabled = !state.busy,
            onClick = { saveAs.launch(viewModel.suggestedBackupFileName()) },
        )
        state.lastBackup?.let {
            Text("Last backup: $it", style = MaterialTheme.typography.bodySmall)
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
