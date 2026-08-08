package com.stockcut.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

// EVERY slot a Material component might reach for is mapped, not just the four
// the brief names. An unset slot silently falls back to M3's baseline lavender,
// which is exactly the generic look docs/04 §2 exists to avoid — and it does not
// look like a bug in code review, only on a screen. Three separate components
// (Scaffold's background, Card, the FAB) were caught doing this by running the
// app, which is why the whole scheme is now written out rather than patched
// one slot at a time.
//
// The brief defines four surfaces and one brand colour, so the *Container slots
// deliberately collapse onto them: a primary action is orange with white text,
// wherever it appears and whichever slot the component happens to read.
private val LightScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = OnPrimaryLight,
    inversePrimary = PrimaryDark,

    secondary = PrimaryLight,
    onSecondary = OnPrimaryLight,
    secondaryContainer = PrimaryLight,
    onSecondaryContainer = OnPrimaryLight,

    tertiary = PrimaryLight,
    onTertiary = OnPrimaryLight,
    tertiaryContainer = PrimaryLight,
    onTertiaryContainer = OnPrimaryLight,

    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = PrimaryLight,
    inverseSurface = OnSurfaceLight,
    inverseOnSurface = SurfaceLight,

    surfaceContainerLowest = SurfaceContainerLight,
    surfaceContainerLow = SurfaceContainerLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerLight,
    surfaceContainerHighest = SurfaceContainerLight,

    outline = OutlineLight,
    outlineVariant = OutlineLight,

    error = ErrorLight,
    onError = OnPrimaryLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorLight,
)

private val DarkScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = OnPrimaryDark,
    inversePrimary = PrimaryLight,

    secondary = PrimaryDark,
    onSecondary = OnPrimaryDark,
    secondaryContainer = PrimaryDark,
    onSecondaryContainer = OnPrimaryDark,

    tertiary = PrimaryDark,
    onTertiary = OnPrimaryDark,
    tertiaryContainer = PrimaryDark,
    onTertiaryContainer = OnPrimaryDark,

    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = PrimaryDark,
    inverseSurface = OnSurfaceDark,
    inverseOnSurface = SurfaceDark,

    surfaceContainerLowest = SurfaceContainerDark,
    surfaceContainerLow = SurfaceContainerDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerDark,
    surfaceContainerHighest = SurfaceContainerDark,

    outline = OutlineDark,
    outlineVariant = OutlineDark,

    error = ErrorDark,
    onError = OnPrimaryDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorDark,
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

    // 🔴 The system bar ICONS must follow this app's theme, not the phone's.
    //
    // `enableEdgeToEdge()` in MainActivity picks icon colour from the SYSTEM
    // dark-mode setting, which is only correct while the two agree. Set the
    // phone to dark and the app to Light — a combination this app explicitly
    // offers in Settings — and Android draws white status-bar icons over the
    // app's white background. The clock and battery become invisible.
    //
    // Found by running the release build on a real phone in dark mode; every
    // emulator run had the two settings agreeing, so it never showed.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalStockCutColors provides if (dark) DarkExtras else LightExtras) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = StockCutTypography,
            content = content,
        )
    }
}
