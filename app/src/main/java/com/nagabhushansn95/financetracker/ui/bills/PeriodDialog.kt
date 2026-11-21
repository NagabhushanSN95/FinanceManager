package com.nagabhushansn95.financetracker.ui.bills

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.bills.BillPeriod
import com.nagabhushansn95.financetracker.data.bills.Validity
import com.nagabhushansn95.financetracker.data.db.entity.ValidityUnit
import com.nagabhushansn95.financetracker.ui.common.DateField

/** The unit names as the form shows them. */
fun ValidityUnit.title(): String = when (this) {
    ValidityUnit.DAYS -> "Days"
    ValidityUnit.MONTHS -> "Months"
    ValidityUnit.YEARS -> "Years"
}

/**
 * The stretch a validity payment covers: a start date and an end date, with the validities a bill usually runs for
 * as shortcuts. Picking a validity places the end that far from the start; moving the start carries the end along
 * while a validity is chosen; the end may also be picked on its own (a pack cut short, a trial month). The due date
 * stays what it was unless the start is moved before it. [choices] come from [com.nagabhushansn95.financetracker.data.bills.Bills.validityChoices].
 */
@Composable
fun ValidityPeriodDialog(
    title: String,
    initial: BillPeriod,
    choices: List<Validity>,
    onDismiss: () -> Unit,
    onConfirm: (BillPeriod) -> Unit,
) {
    var start by remember { mutableStateOf(initial.start) }
    var end by remember { mutableStateOf(initial.end) }
    val current = Validity.between(start, end)
    val listed = choices.firstOrNull { it == current }
    var other by remember { mutableStateOf(listed == null) }
    var otherCount by remember { mutableStateOf(current.count.toString()) }
    var otherUnit by remember { mutableStateOf(current.unit) }
    val otherValidity =
        otherCount.trim().toIntOrNull()?.takeIf { it >= 1 }?.let { Validity(it, otherUnit) }
    val valid = !end.isBefore(start)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField("Starts on", start, { d ->
                    if (d != null) {
                        val keep = if (other) otherValidity else listed
                        start = d
                        if (keep != null) end = keep.endFrom(d)
                    }
                }, Modifier.fillMaxWidth())
                Text(
                    "Holds for",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    choices.forEach { v ->
                        FilterChip(
                            selected = !other && v == current,
                            onClick = { other = false; end = v.endFrom(start) },
                            label = { Text(v.label) },
                        )
                    }
                    FilterChip(
                        selected = other,
                        onClick = {
                            other = true; otherCount = current.count.toString(); otherUnit =
                            current.unit
                        },
                        label = { Text("Other") },
                    )
                }
                if (other) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = otherCount,
                            onValueChange = { text ->
                                otherCount = text.filter { it.isDigit() }.take(4)
                                otherCount.trim().toIntOrNull()?.takeIf { it >= 1 }
                                    ?.let { end = Validity(it, otherUnit).endFrom(start) }
                            },
                            label = { Text("Count") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(2f)) {
                            ValidityUnit.entries.forEachIndexed { i, u ->
                                SegmentedButton(
                                    selected = otherUnit == u,
                                    onClick = {
                                        otherUnit = u
                                        otherCount.trim().toIntOrNull()?.takeIf { it >= 1 }
                                            ?.let { end = Validity(it, u).endFrom(start) }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        i,
                                        ValidityUnit.entries.size
                                    ),
                                    label = { Text(u.title()) },
                                )
                            }
                        }
                    }
                }
                DateField("Ends on", end, { d ->
                    if (d != null) {
                        end = d
                        val v = Validity.between(start, d)
                        if (choices.none { it == v }) {
                            other = true; otherCount = v.count.toString(); otherUnit = v.unit
                        } else other = false
                    }
                }, Modifier.fillMaxWidth())
                Text(
                    if (valid) "Covers ${current.label}; the next falls due the day after it ends." else "The end lies before the start.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (valid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onConfirm(
                        BillPeriod(
                            start,
                            end,
                            minOf(initial.due, start)
                        )
                    )
                }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
