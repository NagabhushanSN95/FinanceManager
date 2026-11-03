package com.nagabhushansn95.financetracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
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

/** The few settings v5 has: theme and the daily automatic backup. (Currency symbols come from each account.) */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var theme by remember { mutableStateOf(prefs.themeMode) }
    var daily by remember { mutableStateOf(prefs.dailyBackupEnabled) }
    val lastAuto = prefs.lastAutoBackupDate

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                    "Manual backups and 'save to…' are under Extras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
