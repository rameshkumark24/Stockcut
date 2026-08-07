package com.stockcut.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stockcut.AppContainer
import com.stockcut.data.entitlement.Tier
import com.stockcut.data.settings.SettingsStore
import com.stockcut.ui.theme.ThemeMode
import com.stockcut.units.UnitSystem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val defaultUnitSystem: UnitSystem = UnitSystem.MM,
    val defaultDenominator: Int = 16,
    val defaultKerfU: Long = 960,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val tier: Tier = Tier.FREE,
    /**
     * Lifetime successful optimizes. Carried here because the About screen puts
     * it in the diagnostics line — docs/09 §4.1 keeps it because it separates a
     * first-run confusion from a power user hitting a real limit.
     */
    val optimizeCount: Int = 0,
)

/**
 * S6 — global settings.
 *
 * 🔴 These are DEFAULTS FOR NEW JOBS ONLY (docs/03 S6). Changing the default
 * kerf here must never touch a job the user already saved — someone who
 * produced a plan last week and cut to it would find the app now disagrees with
 * the steel in their hand. Each project stores its own copy at creation time,
 * which is what makes that guarantee structural rather than a promise.
 */
class SettingsViewModel(
    private val settings: SettingsStore,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settings.settings
        .map { stored ->
            SettingsUiState(
                defaultUnitSystem = UnitSystem.entries
                    .firstOrNull { it.name == stored.defaultUnitSystem } ?: UnitSystem.MM,
                defaultDenominator = stored.defaultFractionDenominator,
                defaultKerfU = stored.defaultKerfU,
                theme = runCatching { ThemeMode.valueOf(stored.theme) }
                    .getOrDefault(ThemeMode.SYSTEM),
                tier = stored.tier,
                optimizeCount = stored.optimizeCount,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onDefaultsChanged(
        unitSystem: UnitSystem = uiState.value.defaultUnitSystem,
        denominator: Int = uiState.value.defaultDenominator,
        kerfU: Long = uiState.value.defaultKerfU,
    ) {
        viewModelScope.launch {
            settings.setDefaults(unitSystem.name, denominator, kerfU)
        }
    }

    fun onThemeChanged(theme: ThemeMode) {
        viewModelScope.launch { settings.setTheme(theme.name) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container.settings) }
        }
    }
}
