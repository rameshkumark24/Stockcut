package com.stockcut.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockcut.ui.components.MeasurementField
import com.stockcut.ui.components.MeasurementFieldState
import com.stockcut.ui.theme.Space
import com.stockcut.ui.theme.ThemeMode
import com.stockcut.ui.theme.TouchTarget
import com.stockcut.units.SUPPORTED_DENOMINATORS
import com.stockcut.units.UnitSystem

/**
 * S6 — Settings.
 *
 * Everything here applies to NEW jobs only. That is stated on screen, not just
 * in the code, because a user changing a default and finding last week's plan
 * had silently moved would rightly stop trusting the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: com.stockcut.AppContainer,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val activity = androidx.compose.ui.platform.LocalContext.current

    // Re-created when the unit or stored value changes, so the field always
    // renders the stored Long in the currently chosen unit.
    val kerf = remember(state.defaultUnitSystem, state.defaultDenominator, state.defaultKerfU) {
        MeasurementFieldState(
            state.defaultKerfU,
            state.defaultUnitSystem,
            state.defaultDenominator,
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" },
                    ) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Space.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            Text("Defaults for new jobs", style = MaterialTheme.typography.titleMedium)
            Text(
                "Changing these never alters a job you have already saved.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                UnitSystem.entries.forEach { system ->
                    FilterChip(
                        selected = system == state.defaultUnitSystem,
                        onClick = { viewModel.onDefaultsChanged(unitSystem = system) },
                        label = { Text(unitLabel(system)) },
                        modifier = Modifier.semantics { contentDescription = unitLabel(system) },
                    )
                }
            }

            if (state.defaultUnitSystem == UnitSystem.INCH_FRACTIONAL) {
                Text("Fraction", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    SUPPORTED_DENOMINATORS.forEach { denominator ->
                        FilterChip(
                            selected = denominator == state.defaultDenominator,
                            onClick = { viewModel.onDefaultsChanged(denominator = denominator) },
                            label = { Text("1/$denominator") },
                        )
                    }
                }
            }

            MeasurementField(
                state = kerf,
                label = "Default kerf",
                helperText = "Your saw blade width. Typical: 3 mm / 1/8\"",
            )
            Button(
                onClick = { if (kerf.commit()) kerf.valueU?.let { viewModel.onDefaultsChanged(kerfU = it) } },
                modifier = Modifier.heightIn(min = TouchTarget.primaryButtonHeight),
            ) { Text("Save default kerf") }

            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == state.theme,
                        onClick = { viewModel.onThemeChanged(mode) },
                        label = { Text(themeLabel(mode)) },
                        modifier = Modifier.semantics { contentDescription = themeLabel(mode) },
                    )
                }
            }

            // Nothing is for sale in this build, so nothing here offers a sale.
            //
            // "Restore purchases" is mandatory when an app HAS purchases, but
            // showing it when there are none is worse than useless: it implies a
            // paid tier exists, and every tap reports "nothing to restore" to
            // someone who never bought anything and now suspects they lost
            // something. Kept behind the flag rather than deleted so it returns
            // intact with the paywall.
            if (com.stockcut.data.entitlement.Monetization.PAYWALL_ENABLED) {
                Text("Purchase", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (state.tier == com.stockcut.data.entitlement.Tier.PAID) {
                        "Unlocked. Thanks."
                    } else {
                        "Free plan. Unlock for unlimited parts and jobs, PDF export " +
                            "and no ads."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 🔴 Mandatory for reinstalls and new devices, and reviewers look
                // for it (gap audit §B4). Present here AND on the paywall.
                Button(
                    onClick = { scope.launch { container.billing.restorePurchases() } },
                    modifier = Modifier
                        .heightIn(min = TouchTarget.primaryButtonHeight)
                        .semantics { contentDescription = "Restore purchases" },
                ) { Text("Restore purchases") }
            }

            // Must stay reachable for users whose region grants ongoing consent
            // options — they have the right to change their mind.
            val privacyOptionsRequired by container.consent.privacyOptionsRequired
                .collectAsStateWithLifecycle()
            if (privacyOptionsRequired) {
                Text("Privacy", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = {
                        (activity as? android.app.Activity)?.let {
                            container.consent.showPrivacyOptions(it)
                        }
                    },
                    modifier = Modifier
                        .heightIn(min = TouchTarget.primaryButtonHeight)
                        .semantics { contentDescription = "Ad privacy settings" },
                ) { Text("Ad privacy settings") }
            }
        }
    }
}

private fun unitLabel(system: UnitSystem): String = when (system) {
    UnitSystem.MM -> "mm"
    UnitSystem.CM -> "cm"
    UnitSystem.M -> "m"
    UnitSystem.INCH_DECIMAL -> "inch"
    UnitSystem.INCH_FRACTIONAL -> "inch ¹⁄₁₆"
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
