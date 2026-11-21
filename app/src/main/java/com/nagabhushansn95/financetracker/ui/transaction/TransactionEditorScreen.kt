package com.nagabhushansn95.financetracker.ui.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.nagabhushansn95.financetracker.ui.bills.ValidityPeriodDialog
import com.nagabhushansn95.financetracker.ui.common.PICKER_LOCALE
import com.nagabhushansn95.financetracker.ui.common.pickerFormatter
import com.nagabhushansn95.financetracker.ui.theme.appTopBarColors
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
    /** Opened from a bill to record its payment for the period starting on [periodStart]. */
    billId: Long? = null,
    periodStart: java.time.LocalDate? = null,
    viewModel: TransactionEditorViewModel = viewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.start(
            transactionId,
            initialKind,
            initialAccountId,
            billId,
            periodStart
        )
    }
    val form by viewModel.form.collectAsStateWithLifecycle()
    val data by viewModel.data.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf<String?>(null) }
    // Back (the arrow or the system's) asks before losing anything typed, as Finance Manager did; untouched, it just closes.
    val close = { if (viewModel.isDirty()) confirmDiscard = true else onDone() }
    BackHandler(onBack = close)

    LaunchedEffect(event) {
        when (val e = event) {
            EditorEvent.Saved, EditorEvent.Deleted -> {
                viewModel.eventHandled(); onDone()
            }

            is EditorEvent.Error -> {
                snackbar.showSnackbar(e.message); viewModel.eventHandled()
            }

            is EditorEvent.Blocked -> {
                blocked = e.message; viewModel.eventHandled()
            }

            null -> Unit
        }
    }

    blocked?.let { message ->
        AlertDialog(
            onDismissRequest = onDone,
            title = { Text("Restore the account first") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = onDone) { Text("OK") } },
        )
    }

    val lookups = data.lookups

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
                title = { Text(if (form.isEdit) "Edit transaction" else if (form.bill?.recording == true) (if (form.bill?.bill?.income == true) "Record receipt" else "Record payment") else "Add transaction") },
                navigationIcon = {
                    IconButton(onClick = close) {
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
            // A bill payment keeps the bill's kind; otherwise the row says what the row pays, and stays as it is.
            form.bill?.let { target ->
                Column {
                    Text(
                        "Bill",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val canChoose = target.recording && !target.duration && target.choices.size > 1
                    var choosing by remember { mutableStateOf(false) }
                    val shown =
                        if (target.recording && target.duration) target.periodFor(form.date) else target.period
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${target.bill.name} · ${if (target.duration) "covers" else "for"} ${shown?.label ?: "—"}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (target.recording && target.duration && shown != null) {
                            TextButton(onClick = { choosing = true }) { Text("Change") }
                            if (choosing) ValidityPeriodDialog(
                                title = "What does this payment cover?",
                                initial = shown,
                                choices = target.validities,
                                onDismiss = { choosing = false },
                                onConfirm = { choosing = false; viewModel.setBillPeriod(it) },
                            )
                        }
                        if (canChoose) Box {
                            TextButton(onClick = { choosing = true }) { Text("Change period") }
                            DropdownMenu(
                                expanded = choosing,
                                onDismissRequest = { choosing = false }) {
                                target.choices.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.label + if (p == target.period) "  ✓" else "") },
                                        onClick = { choosing = false; viewModel.setBillPeriod(p) },
                                    )
                                }
                            }
                        }
                    }
                    if (target.existing) Text(
                        if (target.bill.income) "This row is the receipt for the income; unlink it from the income's screen if it should not be."
                        else "This row pays the bill; unlink it from the bill's screen if it should not.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Chips in a scrollable row: five segmented buttons do not fit a narrow phone without truncating.
            if (form.bill?.recording != true) Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Invest and Redeem are recorded from the instrument's screen under Investments, never here.
                com.nagabhushansn95.financetracker.ui.edit.CASH_KINDS.forEach { kind ->
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
                label = "From",
                value = form.fromAccountId?.let { lookups.accountName(it) } ?: "Choose account",
                onClick = { sheet = Sheet.FROM },
            )
            if (form.showsTo) FieldRow(
                label = "To",
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
            // Rate, quantity and amount stay as typed; whatever is left empty is filled in on save, as Finance Manager
            // did (see EditorForm.resolvedMoney).
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
            val (fromCurrency, toCurrency) = form.currencies(data.accountsById)
            val crossing = form.crossesCurrencies(data.accountsById)
            OutlinedTextField(
                value = form.amount,
                onValueChange = viewModel::setAmount,
                label = { Text(if (crossing) "Amount leaving ($fromCurrency)" else "Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            // A load of a forex card or a foreign demat account: the two sides are in different currencies.
            if (crossing) {
                OutlinedTextField(
                    value = form.toAmount,
                    onValueChange = viewModel::setToAmount,
                    label = { Text("Amount arriving ($toCurrency)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                val a = form.amountValue
                val b = form.toAmountValue
                if (a != null && b != null && a > 0 && b > 0) Text(
                    if (fromCurrency == "INR") "%.2f ₹ per $toCurrency".format(a / b)
                    else if (toCurrency == "INR") "%.2f ₹ per $fromCurrency".format(b / a)
                    else "%.4f $toCurrency per $fromCurrency".format(b / a),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Money credited into a foreign account with no load behind it (cash back, money paid back): the rate it
            // counts at in statistics, optional.
            if (form.intoForeign(data.accountsById)) OutlinedTextField(
                value = form.fxRate,
                onValueChange = viewModel::setFxRate,
                label = { Text("Rupees per $toCurrency (optional)") },
                supportingText = { Text("For statistics; left empty, the account's average cost applies.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            // Also while editing, as in 4.x: an existing row becomes a template, or a picked template is updated with
            // the changed figures. The template with the same kind and particulars is replaced.
            CheckRow(
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
        val state = remember {
            DatePickerState(PICKER_LOCALE,
                initialSelectedDateMillis = form.date.atStartOfDay().toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            )
        }
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
        ) { DatePicker(state = state, dateFormatter = pickerFormatter()) }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(if (form.isEdit) "Discard the changes?" else "Discard this transaction?") },
            text = { Text("What you entered here will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false; onDone()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                }) { Text("Keep editing") }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this transaction?") },
            text = {
                Text(
                    "The account balances are adjusted back." +
                            (form.bill?.takeIf { it.existing }
                                ?.let { "\nIt is linked to ${it.bill.name} for ${it.label}; that period reads as open again." }
                                ?: "")
                )
            },
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
