package com.stockcut.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's theme.
 *
 * Material's dynamic colour is deliberately NOT used. The cut-plan segments and
 * the waste badge carry meaning in their colour, and letting the OS wallpaper
 * recolour them would break the one thing docs/04 §2 insists on: that the plan
 * stays readable, in sunlight, to a colour-blind user, in either mode.
 */

/**
 * Colours Material has no slot for. Reached via [LocalStockCutColors] so a
 * composable physically cannot pick a light-mode value while in dark mode.
 */
@Immutable
data class StockCutColors(
    val cutSegment: Color,
    val cutSegmentAlt: Color,
    val offcut: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
)

private val LightExtras = StockCutColors(
    cutSegment = CutSegmentLight,
    cutSegmentAlt = CutSegmentAltLight,
    offcut = OffcutLight,
    success = SuccessLight,
    warning = WarningLight,
    error = ErrorLight,
)

private val DarkExtras = StockCutColors(
    cutSegment = CutSegmentDark,
    cutSegmentAlt = CutSegmentAltDark,
    offcut = OffcutDark,
    success = SuccessDark,
    warning = WarningDark,
    error = ErrorDark,
)

/**
 * No sensible default exists — a wrong default would silently render dark-mode
 * colours in light mode rather than failing, so this throws instead.
 */
val LocalStockCutColors = staticCompositionLocalOf<StockCutColors> {
    error("StockCutColors accessed outside StockCutTheme")
}

// `background` must be set explicitly alongside `surface`. Scaffold and several
// other Material containers default to `background`, and an unset slot falls
// back to M3's baseline lavender — which is exactly the generic look docs/04 §2
// says to avoid. Caught by running the app, not by reading the theme.
private val LightScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerLight,
    surfaceContainerLow = SurfaceContainerLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorLight,
)

private val DarkScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerDark,
    surfaceContainerLow = SurfaceContainerDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorDark,
)

/** Theme setting from docs/03-app-flow.md S6. Stored in DataStore as a String. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun StockCutTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    CompositionLocalProvider(LocalStockCutColors provides if (dark) DarkExtras else LightExtras) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = StockCutTypography,
            content = content,
        )
    }
}
