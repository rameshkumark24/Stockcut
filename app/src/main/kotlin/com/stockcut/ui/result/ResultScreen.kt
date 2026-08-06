package com.stockcut.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockcut.optimizer.Plan
import com.stockcut.ui.components.BannerKind
import com.stockcut.ui.components.InlineBanner
import com.stockcut.ui.theme.DisplayNumberTextStyle
import com.stockcut.ui.theme.LocalStockCutColors
import com.stockcut.ui.theme.MeasurementTextStyle
import com.stockcut.ui.theme.Space
import com.stockcut.units.UnitSystem
import com.stockcut.units.format

/**
 * S4 — the cut plan. The payoff screen, and the app's first store screenshot.
 *
 * There is no error state and no empty state here, by construction: S3 refuses
 * to navigate unless a real plan exists, and the optimizer returns results
 * rather than throwing. If this screen ever needs an error state, something
 * upstream has stopped doing its job.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Cut plan", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" },
                    ) { Text("Back") }
                },
            )
        },
    ) { padding ->
        val plan = state.plan
        if (plan == null) {
            // Only reachable if the job was emptied while this screen was open.
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(Space.xl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No plan to show.", style = MaterialTheme.typography.titleMedium)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Space.betweenCards),
        ) {
            item {
                SummaryStrip(plan, state.unitSystem, state.denominator)
            }

            state.shortfallMessage?.let { message ->
                item {
                    // The plan is still shown and still useful — the user just
                    // needs to buy more. Hiding it would waste correct work.
                    InlineBanner(
                        kind = BannerKind.WARNING,
                        headline = "Not enough stock for everything",
                        detail = message,
                    )
                }
            }

            items(state.groups, key = { it.firstIndex }) { group ->
                BarCard(
                    group = group,
                    kerfU = state.kerfU,
                    unitSystem = state.unitSystem,
                    denominator = state.denominator,
                )
            }
        }
    }
}

/** `7 bars · 4.2% waste · 336 mm offcut total` (docs/03 S4). */
@Composable
private fun SummaryStrip(plan: Plan, unitSystem: UnitSystem, denominator: Int) {
    val colors = LocalStockCutColors.current
    val band = wasteBand(plan.wastePercent)
    val bandColor = when (band) {
        WasteBand.GOOD -> colors.success
        WasteBand.FAIR -> colors.warning
        WasteBand.POOR -> colors.error
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Space.cardInner),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.1f".format(plan.wastePercent) + "%",
                    style = DisplayNumberTextStyle,
                    color = bandColor,
                )
                Text(
                    // Colour is never the only signal — the band is spelled out.
                    text = "  ${band.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bandColor,
                    modifier = Modifier.padding(bottom = Space.xs),
                )
            }
            Text(
                text = buildString {
                    append(barLabel(plan.bars.size))
                    append(" · ")
                    append(format(plan.totalOffcutU, unitSystem, denominator))
                    append(" offcut total")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BarCard(
    group: BarGroup,
    kerfU: Long,
    unitSystem: UnitSystem,
    denominator: Int,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Space.cardInner),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Text(
                text = if (group.count == 1) {
                    "Bar ${group.firstIndex}"
                } else {
                    "Bars ${group.firstIndex}–${group.firstIndex + group.count - 1} · " +
                        "×${group.count} identical bars"
                },
                style = MaterialTheme.typography.titleMedium,
            )

            CutPlanBar(
                bar = group.bar,
                kerfU = kerfU,
                unitSystem = unitSystem,
                denominator = denominator,
            )

            // The same information as text, so the plan survives being read
            // aloud, printed in black and white, or screenshotted small.
            Text(
                text = buildString {
                    append(
                        group.bar.parts.joinToString(" · ") {
                            format(it.lengthU, unitSystem, denominator)
                        },
                    )
                    append("  →  offcut ")
                    append(format(group.bar.offcutU, unitSystem, denominator))
                },
                style = MeasurementTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun barLabel(count: Int): String = if (count == 1) "1 bar" else "$count bars"
