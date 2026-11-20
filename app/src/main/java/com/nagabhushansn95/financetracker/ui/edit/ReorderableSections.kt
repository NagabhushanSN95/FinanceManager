package com.nagabhushansn95.financetracker.ui.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** A group of rows that can be reordered among themselves by their drag handle. */
data class Section<T>(
    val key: String,
    val title: String,
    val items: List<T>,
    val reorderable: Boolean = true
)

/**
 * One LazyColumn of titled sections. Dragging is confined to a section: a move whose source and target sit in
 * different sections is ignored. When a drag ends, [onReordered] receives the section key and its new id order.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ReorderableSections(
    sections: List<Section<T>>,
    id: (T) -> Long,
    onReordered: (sectionKey: String, orderedIds: List<Long>) -> Unit,
    row: @Composable (item: T, dragHandle: @Composable () -> Unit) -> Unit,
    footer: (@Composable () -> Unit)? = null,
    /** Keys of the sections folded away; with [onToggleSection] set, tapping a header folds or unfolds it. */
    collapsed: Set<String> = emptySet(),
    onToggleSection: ((String) -> Unit)? = null,
) {
    // Local, mutable copy so the list follows the finger before the database catches up.
    var local by remember { mutableStateOf(sections) }
    LaunchedEffect(sections) { local = sections }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        val fromSection = fromKey.substringBefore('/')
        if (fromSection != toKey.substringBefore('/')) return@rememberReorderableLazyListState
        local = local.map { section ->
            if (section.key != fromSection) section
            else {
                val items = section.items.toMutableList()
                val fromIndex = items.indexOfFirst { "${section.key}/${id(it)}" == fromKey }
                val toIndex = items.indexOfFirst { "${section.key}/${id(it)}" == toKey }
                if (fromIndex >= 0 && toIndex >= 0) items.add(toIndex, items.removeAt(fromIndex))
                section.copy(items = items)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        local.forEach { section ->
            val folded = section.key in collapsed
            stickyHeader(key = "header:${section.key}") {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .let { m ->
                            onToggleSection?.let { t -> m.clickable { t(section.key) } } ?: m
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        if (onToggleSection != null) "${section.title} (${section.items.size})" else section.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (onToggleSection != null) androidx.compose.material3.Icon(
                        if (folded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                        contentDescription = if (folded) "Expand ${section.title}" else "Collapse ${section.title}",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                HorizontalDivider()
            }
            if (!folded) items(section.items, key = { "${section.key}/${id(it)}" }) { item ->
                ReorderableItem(reorderState, key = "${section.key}/${id(item)}") { isDragging ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = if (isDragging) 8.dp else 0.dp)
                    ) {
                        row(item) {
                            if (section.reorderable) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .draggableHandle(onDragStopped = {
                                            onReordered(
                                                section.key,
                                                local.first { it.key == section.key }.items.map(id)
                                            )
                                        }),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "≡",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
        footer?.let { item(key = "footer") { it() } }
    }
}

/** A row with a title, optional subtitle and trailing text. Press and hold to edit; a single tap does nothing. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditRow(
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    muted: Boolean = false,
    onLongPress: () -> Unit,
    handle: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        handle()
    }
}
