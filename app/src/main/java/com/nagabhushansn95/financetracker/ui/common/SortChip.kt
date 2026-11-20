package com.nagabhushansn95.financetracker.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate

/**
 * "Sort: Maturity date" with the keys in a menu, and beside it an arrow button that flips the direction (up:
 * ascending, down: descending). Picking a key is the caller's chance to start it in its own natural direction.
 */
@Composable
fun <T> SortChip(
    options: List<T>,
    selected: T,
    descending: Boolean,
    titleOf: (T) -> String,
    onPick: (T) -> Unit,
    onFlip: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            FilterChip(
                selected = false,
                onClick = { open = true },
                label = { Text("Sort: ${titleOf(selected)}") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(if (s == selected) "✓ ${titleOf(s)}" else titleOf(s)) },
                        onClick = { onPick(s); open = false },
                    )
                }
            }
        }
        IconButton(onClick = onFlip) {
            // A real arrow (not a chevron, which reads as fold/unfold): the back arrow turned up or down.
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (descending) "Descending; tap for ascending" else "Ascending; tap for descending",
                modifier = Modifier.rotate(if (descending) -90f else 90f),
            )
        }
    }
}
