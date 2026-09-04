package com.stockcut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.stockcut.ui.theme.LocalStockCutColors
import com.stockcut.ui.theme.Space
import com.stockcut.ui.theme.TouchTarget

enum class BannerKind { INFO, WARNING, ERROR }

/**
 * An inline message with optional actions.
 *
 * docs/04 §7: errors are "inline and specific. Never a modal dialog for a
 * validation problem." A dialog would also let the user dismiss the infeasible
 * warning and carry on to a plan that silently dropped their parts, which is the
 * single thing S3 exists to prevent.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineBanner(
    kind: BannerKind,
    headline: String,
    detail: String? = null,
    modifier: Modifier = Modifier,
    primaryAction: Pair<String, () -> Unit>? = null,
    secondaryAction: Pair<String, () -> Unit>? = null,
) {
    val colors = LocalStockCutColors.current
    val accent = when (kind) {
        BannerKind.INFO -> MaterialTheme.colorScheme.primary
        BannerKind.WARNING -> colors.warning
        BannerKind.ERROR -> colors.error
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Space.md))
            // A tint of the accent rather than the accent itself, so the text on
            // top keeps its contrast in both themes.
            .background(accent.copy(alpha = 0.12f))
            .padding(Space.cardInner),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            color = accentText(accent),
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (primaryAction != null || secondaryAction != null) {
            // 🔴 FlowRow, not Row: two actions do not fit one line at large text.
            //
            // The infeasible-stock banner (ProjectEditorScreen) is the only call
            // site passing BOTH actions, and its labels are "Add longer stock"
            // and "Edit those parts" — 16 characters each. A plain Row does not
            // wrap and has no weights: it measures children in order and hands
            // the leftover width to the second one. On a 360dp phone the two
            // buttons need close to the whole 296dp available, so one notch up
            // on Android's text-size setting squeezes the second button and its
            // label wraps mid-phrase inside the button.
            //
            // That button is the only offered route to fix the offending parts,
            // on the app's one blocking error, for exactly the users who most
            // need larger text. FlowRow lets it drop to a second line instead.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                primaryAction?.let { (label, onClick) ->
                    TextButton(
                        onClick = onClick,
                        modifier = Modifier.heightIn(min = TouchTarget.minimum),
                    ) { Text(label, color = accentText(accent)) }
                }
                secondaryAction?.let { (label, onClick) ->
                    TextButton(
                        onClick = onClick,
                        modifier = Modifier.heightIn(min = TouchTarget.minimum),
                    ) { Text(label, color = accentText(accent)) }
                }
            }
        }
    }
}

/** The accent reads as text at full strength; the tint above is only the fill. */
private fun accentText(accent: Color): Color = accent
