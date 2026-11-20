package com.nagabhushansn95.financetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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

// The 4.x look (Holo Light with a dark action bar): white screens, black text, Holo blue accents, grey panels,
// green credits and red debits. Light only.
val HoloBlue = Color(0xFF33B5E5)
val HoloBlueDark = Color(0xFF0099CC)
val ClassicBar = Color(0xFF222222)

private val ClassicColors = lightColorScheme(
    primary = HoloBlueDark,
    onPrimary = Color.White,
    primaryContainer = HoloBlue,          // the FAB
    onPrimaryContainer = Color.White,
    secondary = HoloBlueDark,
    onSecondary = Color.White,
    secondaryContainer = HoloBlue,        // selected chips and tabs
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF008A00),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFDDDDDD),
    onSurfaceVariant = Color(0xFF444444),
    // Material's container tiers are tinted by default; the 4.x panels were plain greys
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F4F4),
    surfaceContainer = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE6E6E6),
    surfaceContainerHighest = Color(0xFFDDDDDD),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDDDDDD),
    inverseSurface = Color(0xFF222222),
    inverseOnSurface = Color.White,
    inversePrimary = HoloBlue,
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFDDDDDD),
    error = Color(0xFFD00000),
)

/** True while the Classic theme is on, so the top bars can take their 4.x colours. */
val LocalClassicTheme = staticCompositionLocalOf { false }

/** The colours of every top bar: the 4.x dark bar with white text under Classic, Material's defaults otherwise. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appTopBarColors(): TopAppBarColors =
    if (LocalClassicTheme.current) TopAppBarDefaults.topAppBarColors(
        containerColor = ClassicBar,
        scrolledContainerColor = ClassicBar,
        titleContentColor = Color.White,
        navigationIconContentColor = Color.White,
        actionIconContentColor = Color.White,
    ) else TopAppBarDefaults.topAppBarColors()

@Composable
fun FinanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    classic: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalClassicTheme provides classic) {
        MaterialTheme(
            colorScheme = if (classic) ClassicColors else if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
