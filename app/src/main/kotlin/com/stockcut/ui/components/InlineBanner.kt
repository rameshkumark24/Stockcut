package com.stockcut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
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
