package com.nagabhushansn95.financetracker.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** Opens the app's drawer; provided by the home shell so list screens with their own top bar can offer it. */
val LocalOpenDrawer = compositionLocalOf<(() -> Unit)?> { null }

/**
 * Navigation slot for a list screen that sits one level under the hub: the back arrow keeps the stack, the menu
 * button beside it opens the drawer, so the other screens are one tap away without going back first.
 */
@Composable
fun BackAndMenu(onBack: () -> Unit) {
    val openDrawer = LocalOpenDrawer.current
    Row {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        if (openDrawer != null) IconButton(onClick = openDrawer) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu")
        }
    }
}
