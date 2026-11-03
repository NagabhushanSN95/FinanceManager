package com.nagabhushansn95.financetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colours taken from the launcher icon: navy background, gold coin.
val Navy = Color(0xFF0B1A3A)
val NavyLight = Color(0xFF24365C)
val Gold = Color(0xFFF2B01E)
val GoldDark = Color(0xFF9A6A00)
val GoldPale = Color(0xFFFFE9B3)
val Cream = Color(0xFFFCFAF5)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Navy,
    secondary = GoldDark,
    onSecondary = Color.White,
    secondaryContainer = GoldPale,
    onSecondaryContainer = Color(0xFF3D2A00),
    tertiary = Color(0xFF2E6B4F),
    background = Cream,
    onBackground = Color(0xFF1B1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1B1C1E),
    surfaceVariant = Color(0xFFE6E8EE),
    onSurfaceVariant = Color(0xFF44474F),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF3D2A00),
    primaryContainer = NavyLight,
    onPrimaryContainer = GoldPale,
    secondary = GoldPale,
    onSecondary = Color(0xFF3D2A00),
    secondaryContainer = GoldDark,
    onSecondaryContainer = GoldPale,
    tertiary = Color(0xFF8ED1B0),
    background = Navy,
    onBackground = Color(0xFFE4E6EE),
    surface = Color(0xFF13234A),
    onSurface = Color(0xFFE4E6EE),
    surfaceVariant = NavyLight,
    onSurfaceVariant = Color(0xFFC4C7D0),
    error = Color(0xFFF2B8B5),
)

@Composable
fun FinanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
