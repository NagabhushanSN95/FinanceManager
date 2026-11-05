package com.nagabhushansn95.financetracker.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.R
import com.nagabhushansn95.financetracker.data.importer.v4.V4ImportReport
import com.nagabhushansn95.financetracker.ui.common.DatabaseFilePickers

/**
 * First-run screen with the same three choices as Finance Manager had (set up, restore, skip) plus the one-time
 * import of a Finance Manager 4.x database file as the last option.
 */
@Composable
fun WelcomeScreen(onFinished: () -> Unit, viewModel: SetupViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val restorePicker =
        rememberLauncherForActivityResult(DatabaseFilePickers.Open(DatabaseFilePickers.BACKUP_FOLDER)) { uri ->
            if (uri != null) viewModel.restoreFrom(uri)
        }
    val importPicker =
        rememberLauncherForActivityResult(DatabaseFilePickers.Open(DatabaseFilePickers.LEGACY_BACKUP_FOLDER)) { uri ->
            if (uri != null) viewModel.importFrom(uri)
        }

    if (state == SetupState.Wizard) {
        SetupWizard(onFinish = viewModel::finishSetup, onCancel = viewModel::backToChoices)
        return
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            AppLogo(modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(16.dp))
            Text("Finance Tracker", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            when (val s = state) {
                SetupState.Choosing, SetupState.Wizard -> Choices(
                    onSetUp = viewModel::openWizard,
                    onRestore = { restorePicker.launch(DatabaseFilePickers.DATABASE_MIME_TYPES) },
                    onSkip = viewModel::skipSetup,
                    onImport = { importPicker.launch(DatabaseFilePickers.DATABASE_MIME_TYPES) },
                )

                is SetupState.Working -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(s.message, textAlign = TextAlign.Center)
                    Text(
                        "Large ledgers take a few seconds.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }

                is SetupState.Imported -> {
                    ImportReportCard(s.report)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onFinished,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Continue") }
                }

                is SetupState.Done -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(s.title, style = MaterialTheme.typography.titleMedium)
                            s.lines.forEach {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onFinished,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Continue") }
                }

                is SetupState.Failed -> {
                    Text(
                        s.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.message,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Nothing was changed. You can try again or choose another option.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = viewModel::backToChoices,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Back") }
                }
            }
        }
    }
}

/** The launcher icon's raster layers; the adaptive-icon XML itself cannot be loaded by painterResource. */
@Composable
private fun AppLogo(modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(CircleShape)) {
        Image(
            painterResource(R.mipmap.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun Choices(
    onSetUp: () -> Unit,
    onRestore: () -> Unit,
    onSkip: () -> Unit,
    onImport: () -> Unit
) {
    Text(
        "Welcome. Let's begin.",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge
    )
    Spacer(Modifier.height(16.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OptionCard(
            title = "Set up",
            description = "Enter your wallet balance, bank accounts and the kinds of expenses you track.",
            onClick = onSetUp,
            primary = true,
        )
        OptionCard(
            title = "Restore a backup",
            description = "Pick a 'Finance Tracker Database - ….db' file saved with Extras › Back up data on another phone or an earlier install. The settings inside it come back too.",
            onClick = onRestore,
        )
        OptionCard(
            title = "Skip set-up",
            description = "Start right away with a zero-balance wallet, no bank accounts and default categories. Everything can be changed later.",
            onClick = onSkip,
        )
        OptionCard(
            title = "Import Finance Manager 4.x database",
            description = "One-time migration from the old app: in Finance Manager 4.5.4 use Extras → Back-up Database File, then pick that .db file here. The file is read only.",
            onClick = onImport,
        )
    }
}

@Composable
private fun OptionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (primary) Button(onClick = onClick) { Text(title) }
                else TextButton(onClick = onClick) { Text(title) }
            }
        }
    }
}

@Composable
fun ImportReportCard(report: V4ImportReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Import complete", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            ReportRow("Accounts", report.accounts)
            ReportRow("Categories", report.categories)
            ReportRow("Instruments", report.instruments)
            ReportRow("Templates", report.templates)
            ReportRow("Transactions", report.transactions)
            report.transactionsByKind.entries.sortedBy { it.key.ordinal }.forEach { (kind, count) ->
                ReportRow("    ${kind.name.lowercase().replaceFirstChar { it.uppercase() }}", count)
            }
            ReportRow("Dropped bookkeeping rows", report.dropped.size)
            if (report.notes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                report.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            "%,d".format(value),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}
