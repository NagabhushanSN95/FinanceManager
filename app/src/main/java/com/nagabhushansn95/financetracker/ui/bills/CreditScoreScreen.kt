package com.nagabhushansn95.financetracker.ui.bills

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.entity.CreditAgency
import com.nagabhushansn95.financetracker.data.db.entity.CreditScore
import com.nagabhushansn95.financetracker.ui.common.ConfirmDeleteDialog
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.theme.appTopBarColors
import java.time.LocalDate

/**
 * Credit scores by agency: the readings newest first under each, with a line of the readings when there are a few.
 * + records a reading; "Add agency" in the top bar adds a bureau. Press and hold a reading to change or delete it,
 * or an agency's title to rename or delete it (its readings go with it).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CreditScoreScreen(onBack: () -> Unit, viewModel: BillsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CreditScore?>(null) }
    var addingAgency by remember { mutableStateOf(false) }
    var editingAgency by remember { mutableStateOf<CreditAgency?>(null) }
    var confirmDeleteScore by remember { mutableStateOf<CreditScore?>(null) }
    var confirmDeleteAgency by remember { mutableStateOf<CreditAgency?>(null) }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
                title = { Text("Credit score") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { TextButton(onClick = { addingAgency = true }) { Text("Add agency") } },
            )
        },
        floatingActionButton = {
            if (state.agencies.isNotEmpty()) FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Record a score")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Text(
                    "Not money, but close to it: the score each bureau gives, as and when you check it. CIBIL comes first; add the others as they come up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.loaded && state.agencies.isEmpty()) item {
                Text("No agency yet. Add one from the top bar.", modifier = Modifier.padding(16.dp))
            }
            state.agencies.forEach { agency ->
                val scores = state.scores.filter { it.agencyId == agency.id }
                    .sortedWith(compareBy({ it.date }, { it.id }))
                item(key = "a${agency.id}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { editingAgency = agency })
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            agency.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        scores.lastOrNull()?.let {
                            Text(
                                it.score.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (scores.size >= 2) ScoreLine(scores, Modifier.padding(horizontal = 16.dp))
                    HorizontalDivider()
                    if (scores.isEmpty()) Text(
                        "No reading yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                items(scores.asReversed(), key = { "s${it.id}" }) { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = {}, onLongClick = { editing = s })
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                s.date.format(SHORT_DATE),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            s.notes?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            s.score.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (adding || editing != null) ScoreSheet(
        existing = editing,
        agencies = state.agencies,
        onSave = { agencyId, date, score, notes ->
            val e = editing
            if (e == null) viewModel.addScore(agencyId, date, score, notes)
            else viewModel.updateScore(
                e.copy(
                    agencyId = agencyId,
                    date = date,
                    score = score,
                    notes = notes
                )
            )
            adding = false; editing = null
        },
        onDelete = editing?.let { s -> { confirmDeleteScore = s } },
        onDismiss = { adding = false; editing = null },
    )
    if (addingAgency || editingAgency != null) AgencyDialog(
        existing = editingAgency,
        onSave = { name ->
            val e = editingAgency
            if (e == null) viewModel.addAgency(name) else viewModel.updateAgency(e.copy(name = name))
            addingAgency = false; editingAgency = null
        },
        onDelete = editingAgency?.let { a -> { confirmDeleteAgency = a } },
        onDismiss = { addingAgency = false; editingAgency = null },
    )
    confirmDeleteScore?.let { s ->
        ConfirmDeleteDialog(
            title = "Delete this reading?",
            text = "${s.date.format(SHORT_DATE)} · ${s.score}",
            onConfirm = { confirmDeleteScore = null; editing = null; viewModel.deleteScore(s.id) },
            onDismiss = { confirmDeleteScore = null },
        )
    }
    confirmDeleteAgency?.let { a ->
        val n = state.scores.count { it.agencyId == a.id }
        ConfirmDeleteDialog(
            title = "Delete ${a.name}?",
            text = if (n == 0) "It has no readings." else "Its $n reading${if (n == 1) "" else "s"} go too. This cannot be undone.",
            onConfirm = {
                confirmDeleteAgency = null; editingAgency = null; viewModel.deleteAgency(a.id)
            },
            onDismiss = { confirmDeleteAgency = null },
        )
    }
}

/** The readings joined by a line, oldest to newest, scaled to their own range. */
@Composable
private fun ScoreLine(scores: List<CreditScore>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val faint = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(vertical = 8.dp)
    ) {
        val min = scores.minOf { it.score }.toFloat()
        val max = scores.maxOf { it.score }.toFloat()
        val span = (max - min).coerceAtLeast(10f)
        val first = scores.first().date.toEpochDay().toFloat()
        val last = scores.last().date.toEpochDay().toFloat()
        val width = (last - first).coerceAtLeast(1f)
        fun point(s: CreditScore) = Offset(
            x = (s.date.toEpochDay() - first) / width * size.width,
            y = size.height - (s.score - min) / span * size.height,
        )
        drawLine(
            faint,
            Offset(0f, size.height),
            Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx()
        )
        val path = Path()
        scores.forEachIndexed { i, s ->
            val p = point(s); if (i == 0) path.moveTo(
            p.x,
            p.y
        ) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
        scores.forEach { drawCircle(color, radius = 3.dp.toPx(), center = point(it)) }
        drawCircle(Color.Transparent, radius = 0f)
    }
}

/** One reading: the agency, the date it was checked, the score, a note. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreSheet(
    existing: CreditScore?,
    agencies: List<CreditAgency>,
    onSave: (Long, LocalDate, Int, String?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var agencyId by remember { mutableStateOf(existing?.agencyId ?: agencies.firstOrNull()?.id) }
    var date by remember { mutableStateOf<LocalDate?>(existing?.date ?: LocalDate.now()) }
    var score by remember { mutableStateOf(existing?.score?.toString() ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var open by remember { mutableStateOf(false) }
    val value = score.trim().toIntOrNull()
    val valid = agencyId != null && date != null && value != null && value in 1..2000
    FormSheet(
        title = if (existing == null) "Record a score" else "Edit the reading",
        onDismiss = onDismiss
    ) {
        ExposedDropdownMenuBox(
            expanded = open,
            onExpandedChange = { open = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = agencies.firstOrNull { it.id == agencyId }?.name ?: "",
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Agency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                agencies.forEach { a ->
                    DropdownMenuItem(
                        text = { Text(a.name) },
                        onClick = { agencyId = a.id; open = false })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Checked on", date, { date = it }, Modifier.weight(1f))
            OutlinedTextField(
                value = score,
                onValueChange = { score = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("Score") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSave(agencyId!!, date!!, value!!, notes.trim().ifBlank { null }) },
        ) { Text(if (existing == null) "Record" else "Save") }
        if (onDelete != null) OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete reading", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AgencyDialog(
    existing: CreditAgency?,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add agency" else "Rename agency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("Experian") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Text("Delete agency", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()) }) {
                Text(if (existing == null) "Add" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
