package com.nagabhushansn95.financetracker.ui.investments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.prices.AmfiNav
import com.nagabhushansn95.financetracker.data.prices.AmfiScheme
import com.nagabhushansn95.financetracker.data.prices.UrlHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Search AMFI's scheme list and pick the plan this fund is held in (Direct-Growth plans listed first). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmfiSchemeSheet(initialQuery: String, onPick: (AmfiScheme) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf(initialQuery) }
    var schemes by remember { mutableStateOf<List<AmfiScheme>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching { withContext(Dispatchers.IO) { AmfiNav(UrlHttpClient()).fetchAll() } }
            .onSuccess { schemes = it }
            .onFailure { error = it.message ?: "Could not download the AMFI list" }
    }
    val hits = schemes?.let { AmfiNav.search(query, it) }.orEmpty()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)) {
            Text("Link AMFI scheme", style = MaterialTheme.typography.titleLarge)
            Text(
                "Pick the exact plan (Zerodha Coin holds Direct Plan · Growth). The scheme code and ISIN are saved on the fund and its NAV is fetched with the prices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            when {
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                schemes == null -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                hits.isEmpty() -> Text(
                    "No scheme matches.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(hits, key = { it.code }) { s ->
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(s) }
                            .padding(vertical = 8.dp)) {
                            Text(s.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                listOf(
                                    s.plan,
                                    s.option,
                                    "NAV %.4f".format(s.nav),
                                    s.date.toString(),
                                    s.code
                                ).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
