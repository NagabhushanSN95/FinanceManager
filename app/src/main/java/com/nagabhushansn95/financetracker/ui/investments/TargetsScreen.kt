package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.RiskLevel
import com.nagabhushansn95.financetracker.data.repository.EditDataRepository
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TargetsViewModel(application: Application) : InvestmentSummaryViewModel(application) {
    val categories: StateFlow<List<InvestmentCategory>> = db.instrumentDao().observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val riskLevels: StateFlow<List<RiskLevel>> = db.instrumentDao().observeRiskLevels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun messageShown() {
        _message.value = null
    }

    fun save(goalAmount: Double?, updated: List<InvestmentCategory>) {
        viewModelScope.launch {
            runCatching { EditDataRepository(db).saveTargets(goalAmount, updated) }
                .onSuccess { _message.value = "Targets saved" }
                .onFailure { _message.value = it.message ?: it.toString() }
        }
    }
}

/** What the user sets for one category: its risk level and the percentages as typed (e.g. "12.5"). */
private data class Entry(
    val goal: String,
    val wealth: String,
    val tolerance: String,
    val riskLevelId: Long? = null
) {
    val valid
        get() = listOf(goal, wealth, tolerance).all {
            it.isBlank() || it.trim().toDoubleOrNull() != null
        }

    fun pct(text: String) = text.trim().toDoubleOrNull()?.div(100)
}

private fun entryOf(c: InvestmentCategory) = Entry(
    goal = c.targetGoalPct?.let { fmt(it) } ?: "",
    wealth = c.targetWealthPct?.let { fmt(it) } ?: "",
    tolerance = c.tolerancePct?.let { fmt(it) } ?: "",
    riskLevelId = c.riskLevelId,
)

private fun fmt(share: Double) = (share * 100).let {
    if (it == it.toLong().toDouble()) it.toLong().toString() else "%.2f".format(it).trimEnd('0')
        .trimEnd('.')
}

/**
 * Categories & targets: the one place for what each investment category is: its risk level, and its share of the
 * goal corpus and of the wealth corpus (the rest) with a tolerance. Shares are entered on the leaves and roll up;
 * each set should add up to 100%. The categories themselves (names, groups, order) are kept under Manage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetsScreen(onBack: () -> Unit, viewModel: TargetsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val riskLevels by viewModel.riskLevels.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    var goalText by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<Map<Long, Entry>?>(null) }
    LaunchedEffect(categories, state.loaded) {
        if (entries == null && categories.isNotEmpty()) entries =
            categories.associate { it.id to entryOf(it) }
        if (goalText == null && state.loaded) goalText = state.summary?.goalAmount?.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        } ?: ""
    }
    val edits = entries ?: emptyMap()
    val goal = goalText ?: ""
    val goalValue = goal.trim().toDoubleOrNull()
    val totalCurrent = state.summary?.total?.current ?: 0.0
    val parents = categories.filter { it.parentId == null }
    val childrenOf = categories.filter { it.parentId != null }.groupBy { it.parentId!! }

    fun leafShare(id: Long, pick: (Entry) -> String): Double? = edits[id]?.let { it.pct(pick(it)) }
    fun share(c: InvestmentCategory, pick: (Entry) -> String): Double? {
        val kids = childrenOf[c.id].orEmpty()
        val kidShares = kids.map { leafShare(it.id, pick) }
        return if (kids.isNotEmpty() && kidShares.any { it != null }) kidShares.sumOf {
            it ?: 0.0
        } else leafShare(c.id, pick)
    }

    fun target(g: Double?, w: Double?): Double? {
        if (g == null && w == null || totalCurrent <= 0) return null
        val goalPart = (goalValue ?: 0.0).coerceIn(0.0, totalCurrent)
        return (goalPart * (g ?: 0.0) + (totalCurrent - goalPart) * (w ?: 0.0)) / totalCurrent
    }

    val goalSum = parents.sumOf { share(it) { e -> e.goal } ?: 0.0 }
    val wealthSum = parents.sumOf { share(it) { e -> e.wealth } ?: 0.0 }
    val allValid =
        edits.values.all { it.valid } && (goal.isBlank() || goalValue != null && goalValue >= 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories & targets") },
                navigationIcon = { com.nagabhushansn95.financetracker.ui.common.BackAndMenu(onBack) })
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(modifier = Modifier
                .padding(16.dp)
                .imePadding()) {
                SumLine("Goal shares", goalSum); SumLine("Wealth shares", wealthSum)
                Button(
                    enabled = allValid && entries != null,
                    onClick = {
                        val updated = categories.mapNotNull { c ->
                            val e = edits[c.id] ?: return@mapNotNull null
                            val next = c.copy(
                                targetGoalPct = e.pct(e.goal),
                                targetWealthPct = e.pct(e.wealth),
                                tolerancePct = e.pct(e.tolerance),
                                riskLevelId = e.riskLevelId,
                            )
                            if (next == c) null else next
                        }
                        viewModel.save(goalValue, updated)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) { Text("Save") }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = goal,
                            onValueChange = { goalText = it },
                            label = { Text("Goal amount (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "Each category has a risk level (for the \"By risk\" table) and two target shares. " +
                                    "The goal corpus follows the goal shares; everything above it (${
                                MoneyFormat.compact(
                                    (totalCurrent - (goalValue ?: 0.0)).coerceAtLeast(0.0)
                                )
                            } of the current " +
                                    "${MoneyFormat.compact(totalCurrent)}) follows the wealth shares. The overall target of each category is the blend. " +
                                    "Add, rename, group or reorder categories under Manage › Investment categories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            parents.forEach { parent ->
                val kids = childrenOf[parent.id].orEmpty()
                item(key = parent.id) {
                    Column {
                        HorizontalDivider()
                        CategoryHeader(
                            parent.name,
                            bold = true,
                            target = target(share(parent) { it.goal }, share(parent) { it.wealth }),
                            summaryShare = currentShareOf(state, parent.name)
                        )
                        val parentEntry = edits[parent.id] ?: Entry("", "", "")
                        RiskRow(parentEntry, riskLevels) { entries = edits + (parent.id to it) }
                        if (kids.isEmpty()) EntryFields(parentEntry) {
                            entries = edits + (parent.id to it)
                        }
                        else Text(
                            "goal ${pct(share(parent) { it.goal })} · wealth ${pct(share(parent) { it.wealth })} from the sub-categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                kids.forEach { child ->
                    item(key = child.id) {
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            CategoryHeader(
                                child.name,
                                bold = false,
                                target = target(
                                    leafShare(child.id) { it.goal },
                                    leafShare(child.id) { it.wealth }),
                                summaryShare = currentShareOf(state, parent.name, child.name)
                            )
                            val childEntry = edits[child.id] ?: Entry("", "", "")
                            RiskRow(childEntry, riskLevels) { entries = edits + (child.id to it) }
                            EntryFields(childEntry) { entries = edits + (child.id to it) }
                        }
                    }
                }
            }
        }
    }
}

private fun currentShareOf(
    state: InvestmentsUiState,
    parent: String,
    child: String? = null
): Double? {
    val s = state.summary ?: return null
    val top = s.categories.firstOrNull { it.name == parent } ?: return null
    val line =
        if (child == null) top else top.children.firstOrNull { it.name == child } ?: return null
    return s.currentShare(line)
}

@Composable
private fun CategoryHeader(name: String, bold: Boolean, target: Double?, summaryShare: Double?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            "target ${pct(target)} · now ${pct(summaryShare)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The category's risk level as a small dropdown; "no risk level" leaves it out of the By-risk table. */
@Composable
private fun RiskRow(entry: Entry, riskLevels: List<RiskLevel>, onChange: (Entry) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Risk",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.material3.TextButton(onClick = { open = true }) {
            Text(riskLevels.firstOrNull { it.id == entry.riskLevelId }?.name ?: "no risk level")
        }
        androidx.compose.material3.DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false }) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("No risk level") },
                onClick = { onChange(entry.copy(riskLevelId = null)); open = false })
            riskLevels.forEach { r ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(r.name) },
                    onClick = { onChange(entry.copy(riskLevelId = r.id)); open = false })
            }
        }
    }
}

@Composable
private fun EntryFields(entry: Entry, onChange: (Entry) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        PctField(entry.goal, "Goal %", Modifier.weight(1f)) { onChange(entry.copy(goal = it)) }
        PctField(
            entry.wealth,
            "Wealth %",
            Modifier.weight(1f)
        ) { onChange(entry.copy(wealth = it)) }
        PctField(
            entry.tolerance,
            "± %",
            Modifier.weight(1f)
        ) { onChange(entry.copy(tolerance = it)) }
    }
}

@Composable
private fun PctField(value: String, label: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.bodyMedium,
        isError = value.isNotBlank() && value.trim().toDoubleOrNull() == null,
    )
}

@Composable
private fun SumLine(label: String, sum: Double) {
    val ok = kotlin.math.abs(sum - 1.0) < 0.0005
    Text(
        "$label add up to ${pct(sum)}" + if (ok) "" else " (should be 100%)",
        style = MaterialTheme.typography.bodySmall,
        color = if (ok) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
    )
}
