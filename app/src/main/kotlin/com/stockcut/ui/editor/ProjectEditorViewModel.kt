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
import com.stockcut.data.settings.SettingsStore
import com.stockcut.units.UnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

data class EditorUiState(
    val project: Project? = null,
    val parts: List<PartEntry> = emptyList(),
    val stock: List<StockEntry> = emptyList(),
    val tier: Tier = Tier.FREE,
    val tab: EditorTab = EditorTab.PARTS,
    val pendingUndo: PendingUndo? = null,
    val paywallTrigger: com.stockcut.data.entitlement.PaywallTrigger? = null,
    val hardLimitMessage: String? = null,
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
        }
        return true
    }

    /** Editing is never gated, at any tier, even past the free limit. */
    fun onUpdatePart(entry: PartEntry) {
        viewModelScope.launch {
            cutLists.updatePart(projectId, entry)
            refreshProject()
        }
    }

    fun onDeletePart(entry: PartEntry) {
        viewModelScope.launch {
            cutLists.deletePart(projectId, entry)
            local.value = local.value.copy(pendingUndo = PendingUndo.Part(entry))
            refreshProject()
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
        }
        return true
    }

    fun onUpdateStock(entry: StockEntry) {
        viewModelScope.launch {
            cutLists.updateStock(projectId, entry)
            refreshProject()
        }
    }

    fun onDeleteStock(entry: StockEntry) {
        viewModelScope.launch {
            cutLists.deleteStock(projectId, entry)
            local.value = local.value.copy(pendingUndo = PendingUndo.Stock(entry))
            refreshProject()
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
