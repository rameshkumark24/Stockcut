package com.stockcut

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.stockcut.ui.components.MeasurementField
import com.stockcut.ui.components.MeasurementFieldState
import com.stockcut.ui.theme.Space
import com.stockcut.units.UnitSystem

/**
 * Temporary harness so the app is runnable while the navigation graph and the
 * real screens are built. It exercises MeasurementField in both the metric and
 * fractional-inch modes, which is what needs looking at on a real device first.
 *
 * 🔴 DELETE THIS when S1 (projects list) lands. CLAUDE.md: "Delete dead code and
 * unused files the agent leaves behind."
 */
@Composable
fun Placeholder(modifier: Modifier = Modifier) {
    val metric = remember { MeasurementFieldState(unitSystem = UnitSystem.MM) }
    val imperial = remember {
        MeasurementFieldState(unitSystem = UnitSystem.INCH_FRACTIONAL, denominator = 16)
    }
    val kerf = remember { MeasurementFieldState(initialValueU = 960L, unitSystem = UnitSystem.MM) }

    Column(
        modifier = modifier
            .fillMaxSize()
            // Scrolls, so nothing clips at maximum font scale.
            .verticalScroll(rememberScrollState())
            .padding(Space.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Space.xl),
    ) {
        Text("MeasurementField", style = MaterialTheme.typography.headlineSmall)

        MeasurementField(
            state = metric,
            label = "Stock length (mm)",
            helperText = "Try 1200 or 1200.5",
        )

        MeasurementField(
            state = imperial,
            label = "Part length (fractional inch)",
            helperText = "Try 3/4, 1 5/16, or 8' 3 1/2\"",
        )

        MeasurementField(
            state = kerf,
            label = "Kerf",
            helperText = "Your saw blade width. Typical: 3 mm / 1/8\"",
        )
    }
}
