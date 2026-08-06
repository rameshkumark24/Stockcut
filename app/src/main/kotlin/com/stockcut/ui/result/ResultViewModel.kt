package com.stockcut.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stockcut.AppContainer
import com.stockcut.data.repository.CutListRepository
import com.stockcut.data.repository.toOptimizeRequest
import com.stockcut.optimizer.OptimizeResult
import com.stockcut.optimizer.Plan
import com.stockcut.optimizer.optimize
import com.stockcut.units.UnitSystem
import com.stockcut.units.format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ResultUiState(
    val plan: Plan? = null,
    val groups: List<BarGroup> = emptyList(),
    val shortfallMessage: String? = null,
    val unitSystem: UnitSystem = UnitSystem.MM,
    val denominator: Int = 16,
    val kerfU: Long = 0,
)

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
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val cutList = cutLists.loadCutList(projectId) ?: return@launch

            val result = PlanCache.get(projectId, cutList)
                ?: withContext(Dispatchers.Default) { optimize(cutList.toOptimizeRequest()) }
                    .also { PlanCache.put(projectId, cutList, it) }

            val unit = cutList.project.unitSystem
            val denominator = cutList.project.fractionDenominator

            _uiState.value = when (result) {
                is OptimizeResult.Success -> ResultUiState(
                    plan = result.plan,
                    groups = result.plan.groupIdenticalBars(),
                    unitSystem = unit,
                    denominator = denominator,
                    kerfU = cutList.project.kerfU,
                )
                is OptimizeResult.Shortfall -> ResultUiState(
                    plan = result.plan,
                    groups = result.plan.groupIdenticalBars(),
                    shortfallMessage = shortfallMessage(result, cutList, unit, denominator),
                    unitSystem = unit,
                    denominator = denominator,
                    kerfU = cutList.project.kerfU,
                )
                // Unreachable: S3 refuses to navigate for either of these.
                is OptimizeResult.Infeasible,
                is OptimizeResult.InvalidInput,
                -> ResultUiState(unitSystem = unit, denominator = denominator)
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
                initializer { ResultViewModel(projectId, container.cutLists) }
            }
    }
}
