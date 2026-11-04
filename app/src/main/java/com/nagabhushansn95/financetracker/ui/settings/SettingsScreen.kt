package com.nagabhushansn95.financetracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.backup.AutoBackup
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import kotlinx.coroutines.launch

/** The few settings v5 has: theme, automatic prices and the daily automatic backup. */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var theme by remember { mutableStateOf(prefs.themeMode) }
    var daily by remember { mutableStateOf(prefs.dailyBackupEnabled) }
    var autoPrices by remember { mutableStateOf(prefs.autoPrices) }
    var priceInterval by remember { mutableStateOf(prefs.priceRefreshInterval) }
    var drive by remember { mutableStateOf(prefs.driveUploadEnabled) }
    var driveStatus by remember { mutableStateOf(prefs.lastDriveUpload) }
    var driveFolder by remember { mutableStateOf(prefs.driveFolderPath) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val driveBackup =
        remember { com.nagabhushansn95.financetracker.data.drive.DriveBackup(context) }
    val consent =
        androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != android.app.Activity.RESULT_OK && result.data == null) {
                driveStatus = "Sign-in was cancelled"
            } else driveBackup.authorizeFromResult(result.data)
                .onSuccess {
                    drive = true; prefs.driveUploadEnabled = true
                    driveStatus = "Signed in · first upload queued"; prefs.lastDriveUpload =
                    driveStatus
                    com.nagabhushansn95.financetracker.data.drive.DriveUploadWorker.enqueue(context)
                }
                .onFailure { driveStatus = "Google sign-in failed: ${it.message}" }
        }

    fun enableDrive() {
        scope.launch {
            runCatching { driveBackup.authorize() }
                .onSuccess { auth ->
                    when (auth) {
                        is com.nagabhushansn95.financetracker.data.drive.DriveBackup.Auth.NeedsConsent ->
                            consent.launch(
                                androidx.activity.result.IntentSenderRequest.Builder(auth.pendingIntent)
                                    .build()
                            )

                        is com.nagabhushansn95.financetracker.data.drive.DriveBackup.Auth.Token -> {
                            drive = true; prefs.driveUploadEnabled = true
                            driveStatus = "Signed in · first upload queued"; prefs.lastDriveUpload =
                                driveStatus
                            com.nagabhushansn95.financetracker.data.drive.DriveUploadWorker.enqueue(
                                context
                            )
                        }
                    }
                }
                .onFailure {
                    driveStatus = "Google sign-in failed: ${it.message ?: it.javaClass.simpleName}"
                }
        }
    }
    val lastAuto = prefs.lastAutoBackupDate

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AppPreferences.THEME_SYSTEM to "System",
                        AppPreferences.THEME_LIGHT to "Light",
                        AppPreferences.THEME_DARK to "Dark"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = theme == mode,
                            onClick = { theme = mode; prefs.themeMode = mode },
                            label = { Text(label) })
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Prices", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = autoPrices,
                            onValueChange = { autoPrices = it; prefs.autoPrices = it }),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Fetch prices automatically",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "When Investments opens: stock prices and the USD/INR rate from Yahoo Finance, fund NAVs from AMFI. " +
                                    "\"Update prices\" on the Investments screen fetches at any time. Prices are stored with their date and shown \"as of\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = autoPrices, onCheckedChange = null)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("At most", style = MaterialTheme.typography.bodyMedium)
                    listOf(
                        AppPreferences.PRICES_DAILY to "Once a day",
                        AppPreferences.PRICES_HOURLY to "Once an hour"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = priceInterval == mode,
                            enabled = autoPrices,
                            onClick = { priceInterval = mode; prefs.priceRefreshInterval = mode },
                            label = { Text(label) })
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Backups", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = daily,
                            onValueChange = { daily = it; prefs.dailyBackupEnabled = it }),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily automatic backup", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Once a day, on first launch, a copy of the database goes to Documents/${AutoBackup.FOLDER}. The newest ${AutoBackup.KEEP} are kept." +
                                    (lastAuto?.let { "\nLast automatic backup: $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = daily, onCheckedChange = null)
                }
                Text(
                    "The manual backup and the summary workbook export are under Extras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Google Drive", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = drive,
                            onValueChange = { on ->
                                if (on) enableDrive() else {
                                    drive = false; prefs.driveUploadEnabled = false
                                }
                            }),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Upload the daily backup to Google Drive",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "After each daily backup the same snapshot is uploaded to the folder below in your Drive, keeping the newest ${AutoBackup.KEEP}. " +
                                    "Turning this on asks you to sign in once.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = drive, onCheckedChange = null)
                }
                androidx.compose.material3.OutlinedTextField(
                    value = driveFolder,
                    onValueChange = { driveFolder = it; prefs.driveFolderPath = it },
                    label = { Text("Folder in My Drive") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Levels separated by '/'; missing ones are created. The app can only see folders it created itself (Google's 'drive.file' permission), " +
                            "so if these folders already exist in your Drive, let the app create its own and then move the app's folder into place with the Drive app: uploads follow it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        driveStatus?.let { "Last upload: $it" } ?: "No upload yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.TextButton(enabled = drive, onClick = {
                        driveStatus = "Upload queued"; prefs.lastDriveUpload = driveStatus
                        com.nagabhushansn95.financetracker.data.drive.DriveUploadWorker.enqueue(
                            context
                        )
                    }) { Text("Upload now") }
                }
            }
        }
    }
}
