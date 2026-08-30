package com.stockcut.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.stockcut.data.model.PartEntry
import com.stockcut.data.model.StockEntry
import com.stockcut.ui.components.EmptyState
import com.stockcut.ui.components.MeasurementField
import com.stockcut.ui.components.MeasurementFieldState
import com.stockcut.ui.components.MeasurementRow
import com.stockcut.ui.components.unitLabel
import com.stockcut.ui.theme.Space
import com.stockcut.ui.theme.TouchTarget
import com.stockcut.units.SUPPORTED_DENOMINATORS
import com.stockcut.units.UnitSystem
import com.stockcut.units.U_PER_FOOT
import com.stockcut.units.U_PER_M
import com.stockcut.units.format

/** S2a — Parts. The default tab, because it is where the work is. */
@Composable
fun PartsTab(
    state: EditorUiState,
    onAdd: () -> Unit,
    onEdit: (PartEntry) -> Unit,
    onDelete: (PartEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.parts.isEmpty()) {
        EmptyState(
            headline = "What do you need to cut?",
            explanation = "Add each piece and how many of it you need.",
            actionLabel = "Add part",
            onAction = onAdd,
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Space.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        item {
            Text(
                text = state.partsCountLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Space.sm),
            )
        }
        items(state.parts, key = { it.id }) { part ->
            MeasurementRow(
                formattedLength = format(part.lengthU, state.unitSystem, state.denominator),
                quantityLabel = "×${part.quantity}",
                label = part.label,
                onClick = { onEdit(part) },
                onDelete = { onDelete(part) },
            )
        }
        item {
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.md)
                    .heightIn(min = TouchTarget.primaryButtonHeight)
                    .semantics { contentDescription = "Add part" },
            ) {
                Text("Add part")
            }
        }
    }
}

/** S2b — Stock. */
@Composable
fun StockTab(
    state: EditorUiState,
    onAdd: () -> Unit,
    onQuickAdd: (Long) -> Unit,
    onEdit: (StockEntry) -> Unit,
    onDelete: (StockEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.stock.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            EmptyState(
                headline = "What lengths are you buying?",
                explanation = "Add the stock lengths you have, or pick a common size.",
                actionLabel = "Add stock",
                onAction = onAdd,
                modifier = Modifier.weight(1f),
            )
            QuickAddChips(
                state.unitSystem,
                state.denominator,
                onQuickAdd,
                // Space.xl, not Space.screenHorizontal: it matches EmptyState's
                // own inset, so the chips line up with the "Add stock" button
                // directly above them instead of sitting 8dp to its left.
                contentPadding = Space.xl,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Space.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        items(state.stock, key = { it.id }) { entry ->
            MeasurementRow(
                formattedLength = format(entry.lengthU, state.unitSystem, state.denominator),
                // Unlimited is the common case, so it reads as a word not a number.
                quantityLabel = if (entry.isUnlimited) "unlimited" else "×${entry.quantity}",
                label = entry.label,
                onClick = { onEdit(entry) },
                onDelete = { onDelete(entry) },
            )
        }
        // 0.dp: this LazyColumn's contentPadding has already inset the item.
        item {
            QuickAddChips(
                state.unitSystem,
                state.denominator,
                onQuickAdd,
                contentPadding = 0.dp,
            )
        }
        item {
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.sm)
                    .heightIn(min = TouchTarget.primaryButtonHeight)
                    .semantics { contentDescription = "Add stock" },
            ) {
                Text("Add stock")
            }
        }
    }
}

/**
 * Common stock sizes, unit-aware (docs/03 S2b).
 *
 * Metric gets 3/6/12 m; imperial gets 8/10/12/16/20 ft. Offering metres to
 * someone working in feet is the kind of detail that tells a tradesman the app
 * was not built for them.
 *
 * 🔴 [contentPadding] is required, not decorative, and the caller must supply the
 * value that suits ITS context — there is no single right answer:
 *
 *  - In the empty state the chips are a plain sibling of [EmptyState] inside a
 *    bare Column, so nothing insets them. Passing 0 there left the first chip
 *    flush against the left screen edge, visibly clipped, while the "Add stock"
 *    button above it sat inset by [Space.xl]. Found on a real phone, 2026-08-30.
 *  - In the populated list the row is a LazyColumn item and the LazyColumn's own
 *    `contentPadding` has already inset it. Adding more here would double it.
 *
 * The padding is applied AFTER [horizontalScroll] on purpose, so it behaves like
 * `LazyRow`'s `contentPadding`: it insets the chips at rest but scrolls away with
 * them, letting the row use the full width once scrolled.
 *
 * That full-width behaviour is real only at the EMPTY-STATE call site. In the
 * populated list the LazyColumn's own `contentPadding` has already narrowed the
 * item by 32dp before this row is measured, so the five imperial chips scroll
 * and clip 16dp inside the screen edge whatever is passed here. That is correct
 * there — the row lines up with the stock rows above it — but it is a property
 * of the parent, not something this parameter can grant.
 */
@Composable
private fun QuickAddChips(
    unitSystem: UnitSystem,
    denominator: Int,
    onQuickAdd: (Long) -> Unit,
    contentPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val imperial = unitSystem == UnitSystem.INCH_FRACTIONAL || unitSystem == UnitSystem.INCH_DECIMAL
    val lengths = if (imperial) {
        listOf(8, 10, 12, 16, 20).map { it * U_PER_FOOT }
    } else {
        listOf(3, 6, 12).map { it * U_PER_M }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = Space.sm, horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        lengths.forEach { lengthU ->
            val text = format(lengthU, unitSystem, denominator)
            AssistChip(
                onClick = { onQuickAdd(lengthU) },
                label = { Text(text) },
                modifier = Modifier.semantics { contentDescription = "Add $text" },
            )
        }
    }
}

/** S2c — Setup. Units, denominator, kerf, trim, name. */
@Composable
fun SetupTab(
    state: EditorUiState,
    onNameChanged: (String) -> Unit,
    onUnitSystemChanged: (UnitSystem, Int?) -> Unit,
    onKerfChanged: (Long) -> Unit,
    onTrimChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val project = state.project ?: return
    var name by remember(project.id) { mutableStateOf(project.name) }

    // Keyed on the unit system so the fields re-render their stored value in the
    // new unit the moment it changes. The Long behind them never moves.
    val kerf = remember(project.unitSystem, project.fractionDenominator, project.kerfU) {
        MeasurementFieldState(project.kerfU, project.unitSystem, project.fractionDenominator)
    }
    val trim = remember(project.unitSystem, project.fractionDenominator, project.trimU) {
        MeasurementFieldState(
            project.trimU.takeIf { it > 0 },
            project.unitSystem,
            project.fractionDenominator,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Space.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Job name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TouchTarget.fieldHeight)
                .semantics { contentDescription = "Job name" },
        )
        Button(
            onClick = { onNameChanged(name) },
            modifier = Modifier.heightIn(min = TouchTarget.primaryButtonHeight),
        ) { Text("Save name") }

        Text("Units", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            UnitSystem.entries.forEach { system ->
                FilterChip(
                    selected = system == state.unitSystem,
                    onClick = { onUnitSystemChanged(system, null) },
                    label = { Text(unitLabel(system, state.denominator)) },
                    modifier = Modifier.semantics {
                        contentDescription = unitLabel(system, state.denominator)
                    },
                )
            }
        }

        // Only meaningful in fractional inches, so it only appears there.
        if (state.unitSystem == UnitSystem.INCH_FRACTIONAL) {
            Text("Fraction", style = MaterialTheme.typography.titleMedium)
            // horizontalScroll, matching the same row in Settings. The four
            // denominators (SUPPORTED_DENOMINATORS = 8/16/32/64) fit a plain Row
            // today, so this is insurance, not a fix for anything observed:
            // 1/64 is last, so it is what a longer list, a wider translation or
            // a larger font scale would squeeze first.
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                SUPPORTED_DENOMINATORS.forEach { denominator ->
                    FilterChip(
                        selected = denominator == state.denominator,
                        onClick = { onUnitSystemChanged(state.unitSystem, denominator) },
                        label = { Text("1/$denominator") },
                    )
                }
            }
        }

        MeasurementField(
            state = kerf,
            label = "Kerf",
            helperText = "Your saw blade width. Typical: 3 mm / 1/8\"",
        )
        Button(
            onClick = { if (kerf.commit()) kerf.valueU?.let(onKerfChanged) },
            modifier = Modifier.heightIn(min = TouchTarget.primaryButtonHeight),
        ) { Text("Save kerf") }

        MeasurementField(
            state = trim,
            label = "End trim",
            helperText = "Removed from the start of each length if the end is damaged.",
        )
        Button(
            // 🔴 An EMPTY field means "no trim". A TYPO does not.
            //
            // This was `if (commit()) ... else onTrimChanged(0)`, and commit()
            // returns false for two unrelated reasons: the field is blank, and
            // the text does not parse. Treating both as "clear it" meant that a
            // job with a 50 mm trim, plus one fat-fingered entry, silently
            // persisted trimU = 0 to Room — and then re-rendered the field blank
            // via remember(project.trimU), so the typo that caused it vanished
            // too. Every later cut plan would be 50 mm out with nothing on
            // screen to explain why.
            //
            // Blank clears. A parse error leaves the saved value alone and lets
            // commit()'s inline message stand. The Kerf button above always did
            // the right thing here; this one did not.
            onClick = {
                if (trim.text.isBlank()) {
                    trim.clear()
                    onTrimChanged(0)
                } else if (trim.commit()) {
                    trim.valueU?.let(onTrimChanged)
                }
            },
            modifier = Modifier.heightIn(min = TouchTarget.primaryButtonHeight),
        ) { Text("Save trim") }
    }
}

