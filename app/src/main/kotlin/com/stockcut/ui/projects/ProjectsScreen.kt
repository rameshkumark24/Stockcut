package com.stockcut.ui.projects

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockcut.data.model.ProjectSummary
import com.stockcut.ui.components.EmptyState
import com.stockcut.ui.theme.Space
import com.stockcut.ui.theme.TouchTarget
import java.text.DateFormat
import java.util.Date

/**
 * S1 — the projects list. The start destination.
 *
 * States implemented, per docs/03 S1:
 *   Empty      — first run with the example deleted
 *   Populated  — cards sorted by last modified
 *   Free limit — the FAB stays visible and opens the paywall instead of a job
 *   Loading    — deliberately absent; a local Room read is instant
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    container: com.stockcut.AppContainer,
    viewModel: ProjectsViewModel,
    onOpenProject: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf<ProjectSummary?>(null) }

    // A newly created job opens immediately — the user asked for a job, not for
    // a row in a list.
    // LaunchedEffect, NOT a bare call in the composition body.
    //
    // Navigating during composition is a side effect in a phase that must be
    // free of them: composition can run more than once before the state clears,
    // firing navigate() twice and pushing two copies of the editor onto the back
    // stack — so Back appears to do nothing the first time it is pressed.
    LaunchedEffect(state.navigateToProject) {
        val id = state.navigateToProject ?: return@LaunchedEffect
        viewModel.onNavigationHandled()
        onOpenProject(id)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Jobs", style = MaterialTheme.typography.headlineSmall) },
                actions = { OverflowMenu(onOpenSettings, onOpenAbout) },
            )
        },
        bottomBar = {
            // The ONLY banner in the app, and only on the free tier.
            //
            // This is a browsing screen, not a working one — the user is picking
            // a job, not entering measurements. It collapses to nothing when no
            // ad loads, and the padding below keeps it clear of the FAB, since
            // AdMob policy forbids ads adjacent to buttons and a mis-tap here
            // would be invalid traffic.
            if (com.stockcut.data.entitlement.Entitlement.showsAds(state.tier)) {
                val canRequestAds by container.consent.canRequestAds.collectAsStateWithLifecycle()
                // navigationBarsPadding is not cosmetic here — it is an AdMob
                // policy matter. Without it the banner is drawn under the
                // navigation bar, which both obscures the ad and puts the Home
                // and Back keys directly over it. Accidental taps on a partly
                // hidden ad are exactly what invalid-traffic enforcement looks
                // for, and that risks the whole AdMob account.
                // The cutout is unioned in as well, restricted with .only() to
                // the edges a bottom bar can actually meet.
                //
                // targetSdk 36 plus enableEdgeToEdge() lays the window into the
                // display cutout in EVERY orientation. Rotated, a top-corner
                // punch-hole maps to a side edge low down, and AdSize.BANNER is
                // a fixed 320×50 pinned to the start of the bar — so on a
                // corner-cutout phone in one of the two landscape rotations the
                // hole lands on the ad itself.
                //
                // 🔴 NOT safeDrawingPadding() and NOT a bare displayCutoutPadding().
                // A bottomBar receives unconsumed insets, so either of those
                // would also apply the STATUS BAR top inset here — 24-48dp of
                // dead space above the ad in every orientation, trading a
                // landscape-only corner nick for a permanent layout shift.
                // .only(Horizontal + Bottom) leaves portrait byte-for-byte
                // identical to the navigationBarsPadding() it replaces.
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(
                            WindowInsets.navigationBars
                                .union(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                        )
                        .padding(bottom = Space.lg),
                ) {
                    com.stockcut.ads.BannerAd(
                        adUnitId = container.ads.bannerUnitId,
                        canRequestAds = canRequestAds,
                    )
                }
            }
        },
        floatingActionButton = {
            // Visible even at the free limit. Hiding it would leave the user with
            // no way to discover why they cannot add another job.
            ExtendedFloatingActionButton(
                onClick = viewModel::onNewJobClicked,
                modifier = Modifier
                    .heightIn(min = TouchTarget.primaryButtonHeight)
                    .semantics { contentDescription = "New job" },
            ) {
                Text("New job")
            }
        },
    ) { padding ->
        if (state.isEmpty) {
            EmptyState(
                headline = "No jobs yet.",
                explanation = "Add your stock lengths and the pieces you need — " +
                    "StockCut works out the cuts.",
                actionLabel = "New job",
                onAction = viewModel::onNewJobClicked,
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = Space.screenHorizontal,
                    end = Space.screenHorizontal,
                    top = Space.md,
                    // Clears the FAB, so the last card is never trapped under it.
                    bottom = Space.xxxl + Space.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.betweenCards),
            ) {
                items(state.summaries, key = { it.project.id }) { summary ->
                    ProjectCard(
                        summary = summary,
                        onClick = { onOpenProject(summary.project.id) },
                        onDuplicate = { viewModel.onDuplicateProject(summary.project.id) },
                        onDelete = { confirmDelete = summary },
                    )
                }
            }
        }
    }

    // Deleting a job is destructive and represents real work, so it gets a
    // dialog rather than the undo snackbar a single row would get (docs/04 §9).
    confirmDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this job?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteProject(target.project.id)
                    confirmDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            },
        )
    }

    com.stockcut.billing.PaywallHost(
        container = container,
        trigger = if (state.showPaywall) {
            com.stockcut.data.entitlement.PaywallTrigger.PROJECTS
        } else {
            null
        },
        onDismiss = viewModel::onPaywallDismissed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverflowMenu(onOpenSettings: () -> Unit, onOpenAbout: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    TextButton(
        onClick = { open = true },
        modifier = Modifier.semantics { contentDescription = "More options" },
    ) {
        Text("More")
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = { open = false; onOpenSettings() },
        )
        DropdownMenuItem(
            text = { Text("About") },
            onClick = { open = false; onOpenAbout() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectCard(
    summary: ProjectSummary,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val project = summary.project

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // heightIn, never height — the card must grow at max font scale.
            .heightIn(min = TouchTarget.listRowMinHeight)
            .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
            .semantics { contentDescription = project.name },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space.cardInner),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    // A long job name ellipsises in the list, wraps on detail.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(pieceLabel(summary.totalPieces))
                        append(" · ")
                        append(
                            DateFormat.getDateInstance(DateFormat.MEDIUM)
                                .format(Date(project.updatedAt)),
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    onClick = { menuOpen = false; onDuplicate() },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

/** "1 piece", not "1 pieces". Cheap, and its absence is the kind of thing that reads as sloppy. */
private fun pieceLabel(count: Int): String = when (count) {
    0 -> "No pieces yet"
    1 -> "1 piece"
    else -> "$count pieces"
}
