package com.stockcut.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stockcut.AppContainer
import com.stockcut.data.entitlement.Entitlement
import com.stockcut.data.entitlement.Gate
import com.stockcut.data.entitlement.Tier
import com.stockcut.data.repository.CutListRepository
import com.stockcut.data.repository.toOptimizeRequest
import com.stockcut.optimizer.OptimizeResult
import com.stockcut.optimizer.Plan
import com.stockcut.optimizer.optimize
import com.stockcut.units.UnitSystem
import com.stockcut.data.settings.SettingsStore
import com.stockcut.units.format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ResultUiState(
    val plan: Plan? = null,
    val groups: List<BarGroup> = emptyList(),
    val shortfallMessage: String? = null,
    val unitSystem: UnitSystem = UnitSystem.MM,
    val denominator: Int = 16,
    val kerfU: Long = 0,
    val jobName: String = "",
    val tier: Tier = Tier.FREE,
    /** True while the paywall for PDF export is showing. */
    val showPdfPaywall: Boolean = false,
) {
    /** PDF is the paid export; sharing an image is free at both tiers. */
    val canExportPdf: Boolean get() = Entitlement.canExportPdf(tier) is Gate.Allowed
}

/**
 * S4's state.
 *
 * Reads the plan S3 already computed and cached. Recomputing here would be
 * wasteful but harmless — the optimizer is deterministic, so the same job always
 * gives the same plan. The fallback path exists for process death, where the
 * in-memory cache is gone but the job is still on disk.
 */
class ResultViewModel(
    private val projectId: Long,
    private val cutLists: CutListRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    /** Exposed so S4 can drive the review prompt, which belongs on this screen. */
    val settingsStore: SettingsStore get() = settings

    suspend fun settingsSnapshot() = runCatching { settings.settings.first() }.getOrNull()

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        load()
        observeTier()
    }

    /**
     * The tier is observed, not read once: a purchase completed on the paywall
     * must unlock the PDF button on this screen without the user navigating away
     * and back.
     */
    private fun observeTier() {
        viewModelScope.launch {
            settings.settings.collect { s ->
                _uiState.value = _uiState.value.copy(tier = s.tier)
            }
        }
    }

    /**
     * @return true if the export may proceed. False raises the paywall instead —
     * PDF is the one output money actually buys (docs/02 §6).
     */
    fun onExportPdfRequested(): Boolean {
        if (_uiState.value.canExportPdf) return true
        _uiState.value = _uiState.value.copy(showPdfPaywall = true)
        return false
    }

    fun onPdfPaywallDismissed() {
        _uiState.value = _uiState.value.copy(showPdfPaywall = false)
    }

    private fun load() {
        viewModelScope.launch {
            val cutList = cutLists.loadCutList(projectId) ?: return@launch

            val result = PlanCache.get(projectId, cutList)
                ?: withContext(Dispatchers.Default) { optimize(cutList.toOptimizeRequest()) }
                    .also { PlanCache.put(projectId, cutList, it) }

            val unit = cutList.project.unitSystem
            val denominator = cutList.project.fractionDenominator

            val tier = _uiState.value.tier
            _uiState.value = when (result) {
                is OptimizeResult.Success -> ResultUiState(
                    plan = result.plan,
                    groups = result.plan.groupIdenticalBars(),
                    unitSystem = unit,
                    denominator = denominator,
                    kerfU = cutList.project.kerfU,
                    jobName = cutList.project.name,
                    tier = tier,
                )
                is OptimizeResult.Shortfall -> ResultUiState(
                    plan = result.plan,
                    groups = result.plan.groupIdenticalBars(),
                    shortfallMessage = shortfallMessage(result, cutList, unit, denominator),
                    unitSystem = unit,
                    denominator = denominator,
                    kerfU = cutList.project.kerfU,
                    jobName = cutList.project.name,
                    tier = tier,
                )
                // Unreachable: S3 refuses to navigate for either of these.
                is OptimizeResult.Infeasible,
                is OptimizeResult.InvalidInput,
                -> ResultUiState(unitSystem = unit, denominator = denominator, tier = tier)
            }
        }
    }

    /**
     * "You need 2 more 6 m lengths to cut everything" — says what to buy, in the
     * user's own units, rather than just reporting that something did not fit.
     */
    private fun shortfallMessage(
        result: OptimizeResult.Shortfall,
        cutList: com.stockcut.data.model.CutList,
        unit: UnitSystem,
        denominator: Int,
    ): String {
        val parts = result.additionalStockNeeded.entries.mapNotNull { (stockId, extra) ->
            val stock = cutList.stock.firstOrNull { it.id == stockId } ?: return@mapNotNull null
            val length = format(stock.lengthU, unit, denominator)
            if (extra == 1) "1 more $length length" else "$extra more $length lengths"
        }
        val unplaced = result.unplacedParts.sumOf { it.quantity }
        val pieces = if (unplaced == 1) "1 piece" else "$unplaced pieces"

        return if (parts.isEmpty()) {
            "$pieces could not be placed with the stock you have."
        } else {
            "You need ${parts.joinToString(" and ")} to cut everything. " +
                "$pieces are not in this plan."
        }
    }

    companion object {
        fun factory(container: AppContainer, projectId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ResultViewModel(projectId, container.cutLists, container.settings)
                }
            }
    }
}
