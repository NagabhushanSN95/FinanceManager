package com.nagabhushansn95.financetracker.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class Sheet { FROM, TO, CATEGORY }

/**
 * Add or edit one cash transaction (income, expense, transfer): kind selector, date, account pickers in bottom
 * sheets, category, particulars with template suggestions. Anything on an instrument is recorded on the
 * instrument's own screen under Investments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorScreen(
    transactionId: Long?,
    initialKind: TransactionKind?,
    initialAccountId: Long?,
    onDone: () -> Unit,
    viewModel: TransactionEditorViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { viewModel.start(transactionId, initialKind, initialAccountId) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    val data by viewModel.data.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(event) {
        when (val e = event) {
            EditorEvent.Saved, EditorEvent.Deleted -> {
                viewModel.eventHandled(); onDone()
            }

            is EditorEvent.Error -> {
                snackbar.showSnackbar(e.message); viewModel.eventHandled()
            }

            null -> Unit
        }
    }

    val lookups = data.lookups
    // Invest and Redeem are recorded only from the instrument's screen under Investments, so the editor offers the
    // cash kinds. An investment row that still reaches it (a deposit's, say) keeps its kind and shows all five.
    val investmentKinds = setOf(TransactionKind.INVEST, TransactionKind.REDEEM)
    val kinds =
        if (form.kind in investmentKinds) TransactionKind.entries
        else TransactionKind.entries.filter { it !in investmentKinds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.isEdit) "Edit transaction" else "Add transaction") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (form.isEdit) IconButton(onClick = {
                        confirmDelete = true
                    }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            // imePadding keeps the form above the keyboard so the focused field and everything below stay reachable.
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            // Chips in a scrollable row: five segmented buttons do not fit a narrow phone without truncating.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                kinds.forEach { kind ->
                    FilterChip(
                        selected = form.kind == kind,
                        onClick = { viewModel.setKind(kind) },
                        label = { Text(kind.label) },
                    )
                }
            }

            FieldRow(
                label = "Date",
                value = form.date.format(DATE_FORMAT),
                onClick = { showDate = true })

            if (form.showsFrom) FieldRow(
                label = if (form.kind == TransactionKind.INVEST) "Paid from" else "From",
                value = form.fromAccountId?.let { lookups.accountName(it) } ?: "Choose account",
                onClick = { sheet = Sheet.FROM },
            )
            if (form.showsTo) FieldRow(
                label = if (form.kind == TransactionKind.REDEEM) "Credited to" else "To",
                value = form.toAccountId?.let { lookups.accountName(it) } ?: "Choose account",
                onClick = { sheet = Sheet.TO },
            )
            if (form.showsCategory) FieldRow(
                label = "Category",
                value = lookups.categoryPath(form.categoryId)
                    ?: if (form.kind == TransactionKind.EXPENSE) "Choose category" else "None",
                onClick = { sheet = Sheet.CATEGORY },
            )
            ParticularsField(
                value = form.particulars,
                suggestions = data.templates.filter { it.kind == form.kind },
                onChange = viewModel::setParticulars,
                onPickTemplate = viewModel::applyTemplate,
            )

            // Rate, quantity and amount fill each other in as Finance Manager did: a rate alone means one unit;
            // rate × quantity gives the amount; amount ÷ quantity gives the rate.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.rate,
                    onValueChange = viewModel::setRate,
                    label = { Text("Rate") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.quantity,
                    onValueChange = viewModel::setQuantity,
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = form.amount,
                onValueChange = viewModel::setAmount,
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!form.isEdit) CheckRow(
                "Save as template",
                form.saveAsTemplate,
                viewModel::setSaveAsTemplate
            )
            CheckRow(
                "Exclude from statistics",
                !form.includeInStatistics,
            ) { viewModel.setIncludeInStatistics(!it) }
            CheckRow("Hidden", form.hidden, viewModel::setHidden)

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (form.isEdit) "Save changes" else "Add") }
            Spacer(Modifier.height(24.dp))
        }
    }

    when (sheet) {
        Sheet.FROM -> AccountPickerSheet(
            title = "From",
            accounts = data.accounts,
            recent = data.recentFrom,
            exclude = form.toAccountId.takeIf { form.kind == TransactionKind.TRANSFER },
            onPick = { viewModel.setFrom(it); sheet = null },
            onDismiss = { sheet = null },
        )

        Sheet.TO -> AccountPickerSheet(
            title = "To",
            accounts = data.accounts,
            recent = data.recentTo,
            exclude = form.fromAccountId.takeIf { form.kind == TransactionKind.TRANSFER },
            onPick = { viewModel.setTo(it); sheet = null },
            onDismiss = { sheet = null },
        )

        Sheet.CATEGORY -> CategoryPickerSheet(
            scope = if (form.kind == TransactionKind.INCOME) CategoryScope.INCOME else CategoryScope.EXPENSE,
            categories = data.categories, recent = data.recentCategories,
            onPick = { viewModel.setCategory(it); sheet = null }, onDismiss = { sheet = null },
        )

        null -> Unit
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = form.date.atStartOfDay().toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        viewModel.setDate(
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this transaction?") },
            text = { Text("The account balances are adjusted back.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false; viewModel.delete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

@Composable
private fun FieldRow(label: String, value: String, onClick: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Free text with matching saved templates listed beneath while typing. */
@Composable
private fun ParticularsField(
    value: String,
    suggestions: List<com.nagabhushansn95.financetracker.data.db.entity.Template>,
    onChange: (String) -> Unit,
    onPickTemplate: (com.nagabhushansn95.financetracker.data.db.entity.Template) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text("Particulars") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
        )
        val matches = if (focused && value.length >= 2) suggestions.filter {
            it.particulars.contains(
                value,
                ignoreCase = true
            ) && it.particulars != value
        }.take(5) else emptyList()
        matches.forEach { t ->
            Text(
                t.particulars, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickTemplate(t) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
