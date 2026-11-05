package com.nagabhushansn95.financetracker.ui

import android.os.Bundle
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nagabhushansn95.financetracker.data.backup.AutoBackup
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import com.nagabhushansn95.financetracker.ui.common.InvestmentsLock
import com.nagabhushansn95.financetracker.ui.home.HomeScreen
import com.nagabhushansn95.financetracker.ui.setup.WelcomeScreen
import com.nagabhushansn95.financetracker.ui.theme.FinanceTrackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Single Compose activity hosting the whole v5 UI. A FragmentActivity because the biometric prompt needs one. */
class MainActivity : FragmentActivity() {
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

    override fun onStart() {
        super.onStart()
        InvestmentsLock.onForeground()
    }

    override fun onStop() {
        super.onStop()
        InvestmentsLock.onBackground()
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
            val backedUp = AutoBackup(
                context,
                AppDatabase.getInstance(context),
                prefs
            ).runIfDue()
            // The same snapshot goes to Google Drive when the user turned that on (waits for a network).
            if (backedUp && prefs.driveUploadEnabled) com.nagabhushansn95.financetracker.data.drive.DriveUploadWorker.enqueue(
                context
            )
        }
    }
    if (setupComplete) HomeScreen() else WelcomeScreen(onFinished = { setupComplete = true })
}
