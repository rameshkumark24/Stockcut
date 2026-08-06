package com.stockcut.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockcut.data.entitlement.PaywallTrigger
import com.stockcut.data.model.PartEntry
import com.stockcut.data.model.StockEntry
import com.stockcut.ui.components.BannerKind
import com.stockcut.ui.components.InlineBanner
import com.stockcut.ui.theme.Space
import com.stockcut.ui.theme.TouchTarget
import com.stockcut.units.format

/** Which entry sheet, if any, is open. */
private sealed interface SheetTarget {
    data object NewPart : SheetTarget
    data object NewStock : SheetTarget
    data class EditPart(val entry: PartEntry) : SheetTarget
    data class EditStock(val entry: StockEntry) : SheetTarget
}

/**
 * S2 — the project editor.
 *
 * Optimize sits in a sticky bottom bar, reachable from every tab, because the
 * user's goal is the plan and making them find the right tab first would be an
 * obstacle of our own making.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditorScreen(
    viewModel: ProjectEditorViewModel,
    onOptimize: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<SheetTarget?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    // 🔴 The ONLY path to the cut plan. The ViewModel sets this exclusively for
    // Success and Shortfall; an Infeasible result never sets it, so a plan that
    // dropped a part is unreachable rather than merely discouraged.
    LaunchedEffect(state.navigateToResult) {
        if (state.navigateToResult) {
            viewModel.onResultNavigationHandled()
            onOptimize()
        }
    }

    state.invalidInputMessage?.let { message ->
        LaunchedEffect(message) { snackbarHost.showSnackbar(message) }
    }

    // Undo snackbar, 5 s (docs/04 §9). A single row gets undo rather than a
    // confirmation dialog — undo is cheaper than a prompt for something this small.
    LaunchedEffect(state.pendingUndo) {
        val pending = state.pendingUndo ?: return@LaunchedEffect
        val what = if (pending is PendingUndo.Part) "Part" else "Stock"
        val result = snackbarHost.showSnackbar(
            message = "$what deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.onUndo()
        else viewModel.onUndoWindowClosed()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.project?.name ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" },
                    ) { Text("Back") }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::onOptimize,
                enabled = state.canOptimize && !state.optimizing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Space.screenHorizontal)
                    .heightIn(min = TouchTarget.primaryButtonHeight)
                    .semantics { contentDescription = "Optimize" },
            ) {
                // Progress appears only past 300 ms (docs/03 S3): a flash of
                // spinner on a 40 ms job is worse than no spinner at all.
                Text(
                    text = if (state.optimizing) "Optimizing…" else "Optimize",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            state.infeasible?.let { infeasible ->
                val unit = state.unitSystem
                val denominator = state.denominator
                val lengths = infeasible.parts
                    .map { format(it.lengthU, unit, denominator) }
                    .distinct()
                val count = infeasible.parts.sumOf { it.quantity }
                InlineBanner(
                    kind = BannerKind.ERROR,
                    headline = if (count == 1) {
                        "1 part doesn't fit any stock length."
                    } else {
                        "$count parts don't fit any stock length."
                    },
                    detail = lengths.joinToString(" and ") +
                        " " + (if (lengths.size == 1) "is" else "are") +
                        " longer than your longest stock (" +
                        format(infeasible.longestUsableU, unit, denominator) + ").",
                    primaryAction = "Add longer stock" to viewModel::onAddLongerStock,
                    secondaryAction = "Edit those parts" to viewModel::onFixInfeasibleParts,
                    modifier = Modifier.padding(
                        start = Space.screenHorizontal,
                        end = Space.screenHorizontal,
                        top = Space.sm,
                        bottom = Space.sm,
                    ),
                )
            }

            TabRow(selectedTabIndex = state.tab.ordinal) {
                EditorTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == state.tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = { Text(tabLabel(tab)) },
                        modifier = Modifier.semantics { contentDescription = tabLabel(tab) },
                    )
                }
            }

            when (state.tab) {
                EditorTab.PARTS -> PartsTab(
                    state = state,
                    onAdd = { sheet = SheetTarget.NewPart },
                    onEdit = { sheet = SheetTarget.EditPart(it) },
                    onDelete = viewModel::onDeletePart,
                )
                EditorTab.STOCK -> StockTab(
                    state = state,
                    onAdd = { sheet = SheetTarget.NewStock },
                    onQuickAdd = { lengthU ->
                        viewModel.onAddStock(lengthU, StockEntry.UNLIMITED, null)
                    },
                    onEdit = { sheet = SheetTarget.EditStock(it) },
                    onDelete = viewModel::onDeleteStock,
                )
                EditorTab.SETUP -> SetupTab(
                    state = state,
                    onNameChanged = viewModel::onNameChanged,
                    onUnitSystemChanged = viewModel::onUnitSystemChanged,
                    onKerfChanged = viewModel::onKerfChanged,
                    onTrimChanged = viewModel::onTrimChanged,
                )
            }
        }
    }

    when (val target = sheet) {
        null -> Unit
        SheetTarget.NewPart -> EntrySheet(
            title = "Add part",
            unitSystem = state.unitSystem,
            denominator = state.denominator,
            allowUnlimited = false,
            initialLengthU = null,
            initialQuantity = 1,
            initialLabel = null,
            onDismiss = { sheet = null },
            onSubmit = { lengthU, qty, label -> viewModel.onAddPart(lengthU, qty, label) },
        )
        SheetTarget.NewStock -> EntrySheet(
            title = "Add stock",
            unitSystem = state.unitSystem,
            denominator = state.denominator,
            allowUnlimited = true,
            initialLengthU = null,
            // Unlimited is the default because it is what most people mean.
            initialQuantity = StockEntry.UNLIMITED,
            initialLabel = null,
            onDismiss = { sheet = null },
            onSubmit = { lengthU, qty, label -> viewModel.onAddStock(lengthU, qty, label) },
        )
        is SheetTarget.EditPart -> EntrySheet(
            title = "Edit part",
            unitSystem = state.unitSystem,
            denominator = state.denominator,
            allowUnlimited = false,
            initialLengthU = target.entry.lengthU,
            initialQuantity = target.entry.quantity,
            initialLabel = target.entry.label,
            onDismiss = { sheet = null },
            onSubmit = { lengthU, qty, label ->
                // Editing an existing row is never gated, even past the limit.
                viewModel.onUpdatePart(
                    target.entry.copy(lengthU = lengthU, quantity = qty, label = label),
                )
                true
            },
        )
        is SheetTarget.EditStock -> EntrySheet(
            title = "Edit stock",
            unitSystem = state.unitSystem,
            denominator = state.denominator,
            allowUnlimited = true,
            initialLengthU = target.entry.lengthU,
            initialQuantity = target.entry.quantity,
            initialLabel = target.entry.label,
            onDismiss = { sheet = null },
            onSubmit = { lengthU, qty, label ->
                viewModel.onUpdateStock(
                    target.entry.copy(lengthU = lengthU, quantity = qty, label = label),
                )
                true
            },
        )
    }

    state.paywallTrigger?.let { trigger ->
        AlertDialog(
            onDismissRequest = viewModel::onPaywallDismissed,
            title = { Text(paywallHeadline(trigger)) },
            text = { Text("$4.99, one time. Not a subscription.") },
            confirmButton = {
                TextButton(onClick = viewModel::onPaywallDismissed) { Text("Close") }
            },
        )
    }

    // A ceiling money cannot lift gets an explanation, never an offer.
    state.hardLimitMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::onHardLimitDismissed,
            title = { Text("That's as big as one job gets") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::onHardLimitDismissed) { Text("Close") }
            },
        )
    }
}

private fun tabLabel(tab: EditorTab): String = when (tab) {
    EditorTab.PARTS -> "Parts"
    EditorTab.STOCK -> "Stock"
    EditorTab.SETUP -> "Setup"
}

/** Names what the user just hit, not a generic "Go Pro" (docs/03 S5). */
private fun paywallHeadline(trigger: PaywallTrigger): String = when (trigger) {
    PaywallTrigger.PARTS -> "Unlock unlimited parts"
    PaywallTrigger.PROJECTS -> "Unlock unlimited jobs"
    PaywallTrigger.STOCK -> "Unlock unlimited stock lengths"
    PaywallTrigger.PDF_EXPORT -> "Unlock PDF export"
}
