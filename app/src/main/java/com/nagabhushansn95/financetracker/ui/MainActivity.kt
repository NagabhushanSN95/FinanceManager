package com.nagabhushansn95.financetracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import com.nagabhushansn95.financetracker.ui.home.HomeScreen
import com.nagabhushansn95.financetracker.ui.setup.WelcomeScreen
import com.nagabhushansn95.financetracker.ui.theme.FinanceTrackerTheme

/** Single Compose activity hosting the whole v5 UI. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FinanceTrackerTheme {
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
    if (setupComplete) HomeScreen() else WelcomeScreen(onFinished = { setupComplete = true })
}
