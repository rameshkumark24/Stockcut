package com.stockcut.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stockcut.AppContainer
import com.stockcut.data.entitlement.Entitlement
import com.stockcut.data.entitlement.Gate
import com.stockcut.data.entitlement.Tier
import com.stockcut.data.model.ProjectSummary
import com.stockcut.data.repository.ProjectRepository
import com.stockcut.data.settings.SettingsStore
import com.stockcut.units.UnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * S1 — the projects list.
 *
 * Note the absence of a Loading state. docs/03 S1 is explicit: "None. Local Room
 * read is instant — do NOT add a spinner." A spinner on a 5 ms read makes the
 * app feel slower than no spinner at all.
 */
data class ProjectsUiState(
    val summaries: List<ProjectSummary> = emptyList(),
    val tier: Tier = Tier.FREE,
    /** Whether tapping New job opens the editor or the paywall. */
    val newProjectGate: Gate = Gate.Allowed,
    /** Set when the user should be taken somewhere; cleared once consumed. */
    val navigateToProject: Long? = null,
    val showPaywall: Boolean = false,
) {
    val isEmpty: Boolean get() = summaries.isEmpty()
}

class ProjectsViewModel(
    private val projects: ProjectRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val events = MutableStateFlow(Events())

    private data class Events(
        val navigateToProject: Long? = null,
        val showPaywall: Boolean = false,
    )

    val uiState: StateFlow<ProjectsUiState> =
        combine(
            projects.observeProjectSummaries(),
            settings.settings,
            events,
        ) { summaries, settings, event ->
            val tier = settings.tier
            ProjectsUiState(
                summaries = summaries,
                tier = tier,
                // The example does not count against the free tier's one slot.
                newProjectGate = Entitlement.canAddProject(
                    tier = tier,
                    currentProjectCount = summaries.count { !it.project.isExample },
                ),
                navigateToProject = event.navigateToProject,
                showPaywall = event.showPaywall,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProjectsUiState(),
        )

    init {
        seedExample()
    }

    /**
     * Runs on every launch; the repository decides whether anything is needed.
     *
     * The DataStore flag is read here rather than inside the repository because
     * `:data`'s repositories deliberately know nothing about settings — keeping
     * that boundary is what stops the two stores growing into each other.
     */
    private fun seedExample() {
        viewModelScope.launch {
            // first(), not collect(): the DataStore flow never completes, so a
            // collect here would suspend forever and the example would never
            // be seeded.
            val deleted = settings.settings.first().exampleProjectDeleted
            projects.seedExampleIfNeeded(alreadyDeleted = deleted)
        }
    }

    fun onNewJobClicked() {
        val gate = uiState.value.newProjectGate
        if (gate is Gate.NeedsUpgrade) {
            events.value = events.value.copy(showPaywall = true)
            return
        }
        viewModelScope.launch {
            val defaults = currentSettings()
            val id = projects.createProject(
                name = "New job",
                // Defaults apply to NEW projects only; changing them later never
                // mutates a job the user already saved (docs/03 S6).
                unitSystem = UnitSystem.entries
                    .firstOrNull { it.name == defaults.defaultUnitSystem } ?: UnitSystem.MM,
                fractionDenominator = defaults.defaultFractionDenominator,
                kerfU = defaults.defaultKerfU,
            )
            events.value = events.value.copy(navigateToProject = id)
        }
    }

    fun onDeleteProject(id: Long) {
        viewModelScope.launch {
            val project = projects.getProject(id) ?: return@launch
            projects.deleteProject(id)
            // Once the example is gone it must never return, and the row that
            // would have proved it existed is gone too — so the flag is the only
            // record. Set it after the delete succeeds, not before.
            if (project.isExample) settings.markExampleDeleted()
        }
    }

    fun onDuplicateProject(id: Long) {
        viewModelScope.launch {
            val source = projects.getProject(id) ?: return@launch
            projects.duplicate(id, "${source.name} copy")
        }
    }

    fun onRenameProject(id: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { projects.rename(id, newName.trim()) }
    }

    fun onNavigationHandled() {
        events.value = events.value.copy(navigateToProject = null)
    }

    fun onPaywallDismissed() {
        events.value = events.value.copy(showPaywall = false)
    }

    /** One snapshot of the global defaults, for a brand-new job. */
    private suspend fun currentSettings() = settings.settings.first()

    companion object {
        /**
         * Manual DI reaches the ViewModel here. TRD §2 bans Hilt; this factory is
         * the entire cost of that decision.
         */
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProjectsViewModel(container.projects, container.settings) }
        }
    }
}
