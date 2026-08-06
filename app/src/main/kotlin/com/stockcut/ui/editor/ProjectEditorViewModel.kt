package com.stockcut.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stockcut.AppContainer
import com.stockcut.data.entitlement.Entitlement
import com.stockcut.data.entitlement.Gate
import com.stockcut.data.entitlement.Limits
import com.stockcut.data.entitlement.Tier
import com.stockcut.data.model.PartEntry
import com.stockcut.data.model.Project
import com.stockcut.data.model.StockEntry
import com.stockcut.data.repository.CutListRepository
import com.stockcut.data.repository.ProjectRepository
import com.stockcut.data.repository.toOptimizeRequest
import com.stockcut.data.settings.SettingsStore
import com.stockcut.optimizer.OptimizeResult
import com.stockcut.optimizer.optimize
import com.stockcut.ui.result.PlanCache
import com.stockcut.units.UnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditorTab { PARTS, STOCK, SETUP }

/**
 * A row the user just deleted, held in memory for the undo window.
 *
 * docs/05 §1.3 specifies hard delete with no tombstone, so undo cannot be a
 * database flag — the row only exists here until the snackbar goes away.
 */
sealed interface PendingUndo {
    data class Part(val entry: PartEntry) : PendingUndo
    data class Stock(val entry: StockEntry) : PendingUndo
}

/**
 * S3-ERR-1. Parts that fit no stock length, and the longest bar they were
 * measured against.
 *
 * 🔴 While this is non-null the editor MUST NOT navigate to a plan. docs/03:
 * "Never navigate to a plan that silently omitted parts. This is the single most
 * damaging possible bug — the user cuts to a plan and discovers at the end that
 * two pieces were never included."
 */
data class InfeasibleParts(
    val parts: List<com.stockcut.optimizer.PartSpec>,
    val longestUsableU: Long,
)

data class EditorUiState(
    val project: Project? = null,
    val parts: List<PartEntry> = emptyList(),
    val stock: List<StockEntry> = emptyList(),
    val tier: Tier = Tier.FREE,
    val tab: EditorTab = EditorTab.PARTS,
    val pendingUndo: PendingUndo? = null,
    val paywallTrigger: com.stockcut.data.entitlement.PaywallTrigger? = null,
    val hardLimitMessage: String? = null,
    val infeasible: InfeasibleParts? = null,
    /** Shown on the Optimize button only past 300 ms — see onOptimize. */
    val optimizing: Boolean = false,
    val invalidInputMessage: String? = null,
    /** Set once a plan is ready and cached. Cleared when consumed. */
    val navigateToResult: Boolean = false,
) {
    val totalPieces: Int get() = parts.sumOf { it.quantity }

    /** "14 / 20 parts" on the free tier; a plain count once unlocked. */
    val partsCountLabel: String
        get() = if (tier == Tier.FREE) {
            "$totalPieces / ${Limits.FREE_PARTS_PER_PROJECT} pieces"
        } else {
            "$totalPieces pieces"
        }

    val unitSystem: UnitSystem get() = project?.unitSystem ?: UnitSystem.MM
    val denominator: Int get() = project?.fractionDenominator ?: 16

    /** Optimize needs both sides of the problem. */
    val canOptimize: Boolean get() = parts.isNotEmpty() && stock.isNotEmpty()
}

/**
 * S2 — the project editor. Parts · Stock · Setup.
 *
 * Two rules from docs/03 that this class is responsible for:
 *
 *  - Changing units re-formats the display and NEVER changes a stored value.
 *    That falls out of only ever storing Long and formatting at render time, but
 *    it is pinned by a test because it is the kind of thing a later refactor
 *    quietly breaks.
 *
 *  - Free-tier limits block ADDING only. Existing rows stay fully editable and
 *    deletable at any tier — never lock someone out of data they already
 *    entered (docs/02 §6).
 */
class ProjectEditorViewModel(
    private val projectId: Long,
    private val projects: ProjectRepository,
    private val cutLists: CutListRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val local = MutableStateFlow(LocalState())

    private data class LocalState(
        val project: Project? = null,
        val tab: EditorTab = EditorTab.PARTS,
        val pendingUndo: PendingUndo? = null,
        val paywallTrigger: com.stockcut.data.entitlement.PaywallTrigger? = null,
        val hardLimitMessage: String? = null,
        val infeasible: InfeasibleParts? = null,
        val optimizing: Boolean = false,
        val invalidInputMessage: String? = null,
        val navigateToResult: Boolean = false,
    )

    val uiState: StateFlow<EditorUiState> = combine(
        cutLists.observeParts(projectId),
        cutLists.observeStock(projectId),
        settings.settings.map { it.tier },
        local,
    ) { parts, stock, tier, state ->
        EditorUiState(
            project = state.project,
            parts = parts,
            stock = stock,
            tier = tier,
            tab = state.tab,
            pendingUndo = state.pendingUndo,
            paywallTrigger = state.paywallTrigger,
            hardLimitMessage = state.hardLimitMessage,
            infeasible = state.infeasible,
            optimizing = state.optimizing,
            invalidInputMessage = state.invalidInputMessage,
            navigateToResult = state.navigateToResult,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState())

    init {
        refreshProject()
    }

    private fun refreshProject() {
        viewModelScope.launch {
            local.value = local.value.copy(project = projects.getProject(projectId))
        }
    }

    /**
     * Clears a shown infeasible banner. Called after any edit, because the edit
     * may well be the user fixing exactly what the banner complained about, and
     * a stale red banner would say they had not.
     */
    private fun clearValidation() {
        if (local.value.infeasible != null || local.value.invalidInputMessage != null) {
            local.value = local.value.copy(infeasible = null, invalidInputMessage = null)
        }
    }

    fun onTabSelected(tab: EditorTab) {
        local.value = local.value.copy(tab = tab)
    }

    // ── Parts ────────────────────────────────────────────────────────────────

    /**
     * @return true if the part was added. False means a gate fired and the UI
     *   should keep the entry sheet open rather than clearing it.
     */
    fun onAddPart(lengthU: Long, quantity: Int, label: String?): Boolean {
        val state = uiState.value
        when (val gate = Entitlement.canAddParts(state.tier, state.totalPieces, quantity)) {
            is Gate.Allowed -> Unit
            is Gate.NeedsUpgrade -> {
                local.value = local.value.copy(paywallTrigger = gate.trigger)
                return false
            }
            // A paid user at the 1000-piece cap must see an explanation, not an
            // offer. Showing a paywall for a limit money cannot lift is a lie.
            is Gate.HardLimit -> {
                local.value = local.value.copy(hardLimitMessage = gate.message)
                return false
            }
        }
        viewModelScope.launch {
            cutLists.addPart(projectId, lengthU, quantity, label?.takeIf { it.isNotBlank() })
            refreshProject()
            clearValidation()
        }
        return true
    }

    /** Editing is never gated, at any tier, even past the free limit. */
    fun onUpdatePart(entry: PartEntry) {
        viewModelScope.launch {
            cutLists.updatePart(projectId, entry)
            refreshProject()
            clearValidation()
        }
    }

    fun onDeletePart(entry: PartEntry) {
        viewModelScope.launch {
            cutLists.deletePart(projectId, entry)
            local.value = local.value.copy(pendingUndo = PendingUndo.Part(entry))
            refreshProject()
            clearValidation()
        }
    }

    // ── Stock ────────────────────────────────────────────────────────────────

    fun onAddStock(lengthU: Long, quantity: Int, label: String?): Boolean {
        val state = uiState.value
        val gate = Entitlement.canAddStock(state.tier, state.stock.size)
        if (gate is Gate.NeedsUpgrade) {
            local.value = local.value.copy(paywallTrigger = gate.trigger)
            return false
        }
        viewModelScope.launch {
            cutLists.addStock(projectId, lengthU, quantity, label?.takeIf { it.isNotBlank() })
            refreshProject()
            clearValidation()
        }
        return true
    }

    fun onUpdateStock(entry: StockEntry) {
        viewModelScope.launch {
            cutLists.updateStock(projectId, entry)
            refreshProject()
            clearValidation()
        }
    }

    fun onDeleteStock(entry: StockEntry) {
        viewModelScope.launch {
            cutLists.deleteStock(projectId, entry)
            local.value = local.value.copy(pendingUndo = PendingUndo.Stock(entry))
            refreshProject()
            clearValidation()
        }
    }

    // ── Undo ─────────────────────────────────────────────────────────────────

    fun onUndo() {
        val pending = local.value.pendingUndo ?: return
        viewModelScope.launch {
            when (pending) {
                is PendingUndo.Part -> cutLists.restorePart(projectId, pending.entry)
                is PendingUndo.Stock -> cutLists.restoreStock(projectId, pending.entry)
            }
            local.value = local.value.copy(pendingUndo = null)
            refreshProject()
            clearValidation()
        }
    }

    /** Called when the snackbar goes, whether it timed out or was dismissed. */
    fun onUndoWindowClosed() {
        local.value = local.value.copy(pendingUndo = null)
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    /**
     * 🔴 Changes how measurements are DISPLAYED. Every stored length is a Long in
     * internal units and is not touched — a tradesman switching to inches to read
     * a plan must not find their 6 m stock has become something else.
     */
    fun onUnitSystemChanged(system: UnitSystem, denominator: Int? = null) {
        val project = local.value.project ?: return
        updateProject(
            project.copy(
                unitSystem = system,
                fractionDenominator = denominator ?: project.fractionDenominator,
            ),
        )
    }

    fun onKerfChanged(kerfU: Long) {
        local.value.project?.let { updateProject(it.copy(kerfU = kerfU)) }
    }

    fun onTrimChanged(trimU: Long) {
        local.value.project?.let { updateProject(it.copy(trimU = trimU)) }
    }

    fun onNameChanged(name: String) {
        if (name.isBlank()) return
        local.value.project?.let { updateProject(it.copy(name = name.trim())) }
    }

    private fun updateProject(updated: Project) {
        // Optimistic: the field reflects the change immediately rather than
        // waiting for a round trip, which at Room's speed would just be a flicker.
        local.value = local.value.copy(project = updated)
        viewModelScope.launch { projects.updateProject(updated) }
    }

    // ── S3 — Optimize ────────────────────────────────────────────────────────

    /**
     * Runs the optimizer and decides whether the user may see a plan.
     *
     * 🔴 THE RULE THIS FUNCTION EXISTS FOR: if any part fits no stock length, it
     * sets [InfeasibleParts] and does NOT navigate. docs/03 S3-ERR-1 calls a plan
     * that silently omitted parts "the single most damaging possible bug" —
     * someone cuts the whole job and finds out at the end that two pieces were
     * never in it.
     *
     * Shortfall is different and DOES navigate: limited stock ran out, but every
     * part is accounted for, the partial plan is real, and S4 shows a banner
     * saying how much more to buy. Infeasible means impossible; Shortfall means
     * "buy more" — conflating them would either hide a real plan or show a false
     * one.
     *
     * Optimizing itself is never gated by tier (CLAUDE.md rule 9). Free and paid
     * run the identical optimizer.
     */
    fun onOptimize() {
        viewModelScope.launch {
            val cutList = cutLists.loadCutList(projectId) ?: return@launch

            if (cutList.parts.isEmpty() || cutList.stock.isEmpty()) {
                local.value = local.value.copy(
                    invalidInputMessage = "Add at least one part and one stock length.",
                )
                return@launch
            }

            // Only show progress if it is actually slow. docs/03 S3: under 300 ms
            // navigate straight through — "a flash of spinner is worse than none".
            val progress = launch {
                delay(300)
                local.value = local.value.copy(optimizing = true)
            }

            // Off the main thread: 1000 parts has a 10 s budget (docs/05 §2.4),
            // and blocking the main thread for that is an ANR.
            val result = withContext(Dispatchers.Default) {
                optimize(cutList.toOptimizeRequest())
            }
            progress.cancel()

            when (result) {
                is OptimizeResult.Infeasible -> {
                    local.value = local.value.copy(
                        optimizing = false,
                        infeasible = InfeasibleParts(result.impossibleParts, result.longestUsableU),
                        navigateToResult = false,
                    )
                }
                is OptimizeResult.InvalidInput -> {
                    // Should be unreachable — the UI validates first. If it does
                    // happen it is a bug, and saying so beats a blank screen.
                    local.value = local.value.copy(
                        optimizing = false,
                        invalidInputMessage = result.reason,
                    )
                }
                is OptimizeResult.Success, is OptimizeResult.Shortfall -> {
                    PlanCache.put(projectId, cutList, result)
                    settings.recordOptimize()
                    local.value = local.value.copy(
                        optimizing = false,
                        infeasible = null,
                        invalidInputMessage = null,
                        navigateToResult = true,
                    )
                }
            }
        }
    }

    fun onResultNavigationHandled() {
        local.value = local.value.copy(navigateToResult = false)
    }

    /** Jumps the user to the tab holding what they need to fix. */
    fun onFixInfeasibleParts() {
        local.value = local.value.copy(infeasible = null, tab = EditorTab.PARTS)
    }

    fun onAddLongerStock() {
        local.value = local.value.copy(infeasible = null, tab = EditorTab.STOCK)
    }

    // ── Dismissals ───────────────────────────────────────────────────────────

    fun onPaywallDismissed() {
        local.value = local.value.copy(paywallTrigger = null)
    }

    fun onHardLimitDismissed() {
        local.value = local.value.copy(hardLimitMessage = null)
    }

    companion object {
        fun factory(container: AppContainer, projectId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ProjectEditorViewModel(
                        projectId = projectId,
                        projects = container.projects,
                        cutLists = container.cutLists,
                        settings = container.settings,
                    )
                }
            }
    }
}
