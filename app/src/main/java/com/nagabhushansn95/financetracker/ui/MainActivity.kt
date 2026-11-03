package com.nagabhushansn95.financetracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nagabhushansn95.financetracker.data.backup.AutoBackup
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import com.nagabhushansn95.financetracker.ui.home.HomeScreen
import com.nagabhushansn95.financetracker.ui.setup.WelcomeScreen
import com.nagabhushansn95.financetracker.ui.theme.FinanceTrackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Single Compose activity hosting the whole v5 UI. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefs = AppPreferences(this)
        setContent {
            val themeMode by prefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = prefs.themeMode)
            val dark = when (themeMode) {
                AppPreferences.THEME_LIGHT -> false
                AppPreferences.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }
            FinanceTrackerTheme(darkTheme = dark) {
                Surface { FinanceTrackerApp() }
            }
        }
    }
}

@Composable
private fun FinanceTrackerApp() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var setupComplete by remember { mutableStateOf(prefs.setupComplete) }
    LaunchedEffect(setupComplete) {
        // Daily automatic backup: a preference read on most launches, one snapshot on the first launch of a day.
        if (setupComplete) withContext(Dispatchers.IO) {
            AutoBackup(
                context,
                AppDatabase.getInstance(context),
                prefs
            ).runIfDue()
        }
    }
    if (setupComplete) HomeScreen() else WelcomeScreen(onFinished = { setupComplete = true })
}
