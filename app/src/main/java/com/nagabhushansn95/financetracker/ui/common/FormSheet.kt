package com.nagabhushansn95.financetracker.ui.common

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A full-height bottom sheet for a form. It does not follow swipes on its body, so scrolling the fields or an
 * accidental pull cannot close it: it closes from a swipe down on the handle at the top, the back button, a tap
 * outside, or whatever the form itself offers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSheet(
    title: String?,
    onDismiss: () -> Unit,
    scrollable: Boolean = true,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = {
            val dragged = remember { floatArrayOf(0f) }
            val threshold = with(density) { 56.dp.toPx() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> dragged[0] += delta },
                        onDragStarted = { dragged[0] = 0f },
                        onDragStopped = {
                            if (dragged[0] > threshold) scope.launch { sheetState.hide() }
                                .invokeOnCompletion { onDismiss() }
                            dragged[0] = 0f
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) { BottomSheetDefaults.DragHandle() }
        },
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .imePadding()
                .let { if (scrollable) it.verticalScroll(rememberScrollState()) else it },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            title?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
            content()
        }
    }
}
