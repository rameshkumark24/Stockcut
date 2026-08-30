package com.stockcut.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.stockcut.ui.theme.MeasurementTextStyle
import com.stockcut.ui.theme.Space
import com.stockcut.ui.theme.TouchTarget

/**
 * One length row: `Length · ×Qty · Label`, used by both the Parts and Stock tabs.
 *
 * The length is right-aligned and rendered in [MeasurementTextStyle], which
 * carries tabular figures. docs/04 §3: "A column of right-aligned lengths that
 * doesn't line up looks broken to someone who reads numbers for a living."
 *
 * @param formattedLength already formatted by :units — this component never sees
 *   a raw Long, and never formats one. Formatting happens once, at the boundary.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeasurementRow(
    formattedLength: String,
    quantityLabel: String,
    label: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            // heightIn, never height: the row must grow at max font scale.
            .heightIn(min = TouchTarget.listRowMinHeight)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Space.cardInner, top = Space.md, bottom = Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // FlowRow, not Row — the same fix as InlineBanner's actions.
                //
                // A plain Row has no weights here, so it measures the length
                // first and hands the quantity label whatever is left. A long
                // fractional length beside "unlimited" (the common case for
                // stock) leaves too little, and the label breaks mid-word —
                // there is no hyphenation, so it simply splits.
                //
                // FlowRow drops the label to its own line intact instead.
                FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedLength,
                        style = MeasurementTextStyle,
                        textAlign = TextAlign.End,
                    )
                    Text(
                        text = "  $quantityLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!label.isNullOrBlank()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(TouchTarget.iconButton)
                    .semantics { contentDescription = "Delete $formattedLength" },
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(TouchTarget.iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
