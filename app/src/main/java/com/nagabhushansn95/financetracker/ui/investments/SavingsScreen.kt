package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.investments.Savings
import com.nagabhushansn95.financetracker.data.investments.SavingsPeriod
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max

data class SavingsState(
    val monthly: List<SavingsPeriod> = emptyList(),
    val yearly: List<SavingsPeriod> = emptyList(),
    val loaded: Boolean = false
)

class SavingsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val state: StateFlow<SavingsState> = combine(
        db.transactionDao().observeAll(),
        db.instrumentDao().observeAll(),
        db.instrumentDao().observeCategories(),
        db.accountDao().observeAll(),
    ) { ledger, instruments, categories, accounts ->
        SavingsState(
            monthly = Savings.build(ledger, instruments, categories, accounts, monthly = true),
            yearly = Savings.build(ledger, instruments, categories, accounts, monthly = false),
            loaded = true,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavingsState())
}

private val MONTH = DateTimeFormatter.ofPattern("MMM yyyy")
private fun periodLabel(key: String) =
    if (key.length == 7) YearMonth.parse(key).format(MONTH) else key

/** The sheet's Savings Monthly / Yearly tables: a bar chart, then one expandable row per period. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(onBack: () -> Unit, viewModel: SavingsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var monthly by rememberSaveable { mutableStateOf(true) }
    var expanded by rememberSaveable { mutableStateOf(setOf<String>()) }
    val periods = if (monthly) state.monthly else state.yearly
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings") },
                navigationIcon = { com.nagabhushansn95.financetracker.ui.common.BackAndMenu(onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = monthly,
                        onClick = { monthly = true },
                        label = { Text("Monthly") })
                    FilterChip(
                        selected = !monthly,
                        onClick = { monthly = false },
                        label = { Text("Yearly") })
                }
                val charted = periods.take(if (monthly) 12 else 10).reversed()
                if (charted.isNotEmpty()) Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            if (monthly) "Net savings, last ${charted.size} months" else "Net savings by year",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        SavingsBars(charted)
                        Row {
                            Text(
                                periodLabel(charted.first().key),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                periodLabel(charted.last().key),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Text(
                    "Put in minus taken out, per investment type. Renewals of deposits count on both sides, as in the sheet. Tap a period for the breakdown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        "Period",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        "Put in",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Taken out",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider()
            }
            if (periods.isEmpty()) item {
                Text(
                    if (state.loaded) "No investments recorded yet." else "Loading…",
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(periods, key = { it.key }) { p ->
                val open = p.key in expanded
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expanded = if (open) expanded - p.key else expanded + p.key
                        }) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            periodLabel(p.key),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1.2f)
                        )
                        Cell(MoneyFormat.compact(p.credits))
                        Cell(MoneyFormat.compact(p.debits))
                        Cell(MoneyFormat.compact(p.total), p.total)
                    }
                    if (open) p.lines.forEach { l ->
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 3.dp)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                l.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1.2f)
                            )
                            Cell(MoneyFormat.compact(l.credits), small = true)
                            Cell(MoneyFormat.compact(l.debits), small = true)
                            Cell(MoneyFormat.compact(l.net), l.net, small = true)
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(
    text: String,
    signed: Double? = null,
    small: Boolean = false
) {
    Text(
        text,
        style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.weight(1f),
        color = when {
            signed == null -> if (small) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            signed < 0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.tertiary
        },
    )
}

/** Vertical bars around a zero line: savings up, net withdrawals down. */
@Composable
private fun SavingsBars(periods: List<SavingsPeriod>) {
    val up = MaterialTheme.colorScheme.tertiary
    val down = MaterialTheme.colorScheme.error
    val axis = MaterialTheme.colorScheme.outlineVariant
    val maxAbs = max(periods.maxOf { abs(it.total) }, 1.0)
    val positive = periods.maxOf { it.total }.coerceAtLeast(0.0)
    val negative = -periods.minOf { it.total }.coerceAtMost(0.0)
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
        .padding(vertical = 8.dp)) {
        val span = (positive + negative).coerceAtLeast(maxAbs)
        val zeroY = size.height * (positive / span).toFloat()
        drawLine(axis, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 2f)
        val slot = size.width / periods.size
        val barWidth = slot * 0.6f
        periods.forEachIndexed { i, p ->
            val h = (abs(p.total) / span * size.height).toFloat()
            val x = slot * i + (slot - barWidth) / 2
            if (p.total >= 0) drawRect(up, Offset(x, zeroY - h), Size(barWidth, h))
            else drawRect(down, Offset(x, zeroY), Size(barWidth, h))
        }
    }
}
