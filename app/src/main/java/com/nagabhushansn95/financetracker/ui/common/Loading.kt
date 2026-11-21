package com.nagabhushansn95.financetracker.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * What a screen shows in place of its content: a spinner while the data is still being read, then [message] when
 * there turns out to be nothing to show ("Not found.", "Nothing here yet."). Centred in [modifier]'s box.
 */
@Composable
fun LoadingOrMessage(loaded: Boolean, message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (loaded) Text(
            message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        else CircularProgressIndicator(modifier = Modifier.padding(32.dp))
    }
}
