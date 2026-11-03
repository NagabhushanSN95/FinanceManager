package com.nagabhushansn95.financetracker.ui.investments

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.repository.InstrumentMaintenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Investments dashboard (milestone C). Opening it is also when housekeeping runs: stocks and funds sold out for
 * 90+ days retire from the pickers, so the check happens where it matters instead of on every launch.
 */
@Composable
fun InvestmentsScreen() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching { InstrumentMaintenance(AppDatabase.getInstance(context)).retireIdleHoldings() }
                .onFailure { Log.e("Investments", "Instrument maintenance failed", it) }
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Investments", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Dashboard, deposits, funds and stocks arrive in the next milestone.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
