package com.nagabhushansn95.financetracker.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.db.entity.Account
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

/** A read-only text field that opens the Material date picker; [onChange] gets null when cleared (if [clearable]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value?.format(SHORT_DATE) ?: "",
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(label) },
        trailingIcon = if (clearable && value != null) {
            { TextButton(onClick = { onChange(null) }) { Text("Clear") } }
        } else null,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.clickable { open = true },
    )
    if (open) PickDateDialog(
        value ?: LocalDate.now(),
        onDismiss = { open = false }) { onChange(it); open = false }
}

/** The Material date picker on its own; [onPick] gets the chosen date, [onDismiss] runs on Cancel or tapping outside. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDateDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    )
                }
                    ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}

@Composable
fun NumberField(
    value: String,
    label: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

/** Label plus a dropdown of accounts; [none] adds an entry that picks null. */
@Composable
fun AccountDropdown(
    label: String,
    accounts: List<Account>,
    selected: Long?,
    onPick: (Long?) -> Unit,
    none: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(120.dp))
        TextButton(onClick = { open = true }) {
            Text(
                accounts.firstOrNull { it.id == selected }?.name
                    ?: (if (selected == null && none != null) none else "Choose…")
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            none?.let {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = { onPick(null); open = false })
            }
            accounts.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.name) },
                    onClick = { onPick(a.id); open = false })
            }
        }
    }
}

/** Label plus a dropdown over arbitrary choices. */
@Composable
fun <T> ChoiceDropdown(
    label: String,
    choices: List<T>,
    selected: T,
    text: (T) -> String,
    onPick: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(120.dp))
        TextButton(onClick = { open = true }) { Text(text(selected)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEach { c ->
                DropdownMenuItem(
                    text = { Text(text(c)) },
                    onClick = { onPick(c); open = false })
            }
        }
    }
}

/** Fills the row's remaining width: a small label above a value. */
@Composable
fun androidx.compose.foundation.layout.RowScope.StatCell(
    label: String,
    value: String,
    signed: Double? = null,
    weight: Float = 1f
) {
    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(weight)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
            color = when {
                signed == null -> MaterialTheme.colorScheme.onSurface
                signed < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.tertiary
            },
        )
    }
}

/**
 * A text field that suggests earlier values as you type: [suggestions] filtered by what has been typed, shown under
 * the field while it has focus. Picking one replaces the text; typing something new is fine too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteField(
    value: String,
    label: String,
    suggestions: List<String>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val matches = remember(value, suggestions) {
        val typed = value.trim()
        suggestions.filter {
            typed.isEmpty() || (it.contains(
                typed,
                ignoreCase = true
            ) && it != typed)
        }
    }
    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = focused && matches.isNotEmpty(),
        onExpandedChange = {},
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = focused && matches.isNotEmpty(),
            onDismissRequest = {},
        ) {
            matches.forEach { s ->
                DropdownMenuItem(text = { Text(s) }, onClick = { onChange(s) })
            }
        }
    }
}

/** A full-width field that looks like the text fields around it and opens a dropdown of accounts; [none] picks null. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountField(
    label: String,
    accounts: List<Account>,
    selected: Long?,
    onPick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    none: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = accounts.firstOrNull { it.id == selected }?.name
                ?: (if (selected == null && none != null) none else ""),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = {
                androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = open
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            none?.let {
                DropdownMenuItem(text = { Text(it) }, onClick = { onPick(null); open = false })
            }
            accounts.forEach { a ->
                DropdownMenuItem(text = { Text(a.name) }, onClick = { onPick(a.id); open = false })
            }
        }
    }
}

/**
 * A value that is shown rather than typed: a small label over the text, with a pencil on the right when it can be
 * changed. Used for a deposit's name and its computed maturity date.
 */
@Composable
fun LabelRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        if (onEdit != null) androidx.compose.material3.IconButton(onClick = onEdit) {
            androidx.compose.material3.Icon(
                Icons.Filled.Edit,
                contentDescription = "Edit $label"
            )
        }
    }
}
