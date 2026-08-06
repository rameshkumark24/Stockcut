package com.stockcut.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.stockcut.ui.theme.Space
import com.stockcut.ui.theme.TouchTarget

/**
 * Icon-free empty state: headline, one line of explanation, exactly one action.
 *
 * docs/04 §7: "every empty state has a headline, a one-line explanation, and
 * exactly one action". The checklist calls the empty state the most-seen and
 * least-designed screen in most apps, which is why this is a component rather
 * than something each screen improvises.
 */
@Composable
fun EmptyState(
    headline: String,
    explanation: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onAction,
            modifier = Modifier
                .padding(top = Space.sm)
                .fillMaxWidth()
                // heightIn so the button grows with the font scale instead of
                // clipping its own label.
                .heightIn(min = TouchTarget.primaryButtonHeight),
        ) {
            Text(actionLabel, style = MaterialTheme.typography.titleMedium)
        }
    }
}
