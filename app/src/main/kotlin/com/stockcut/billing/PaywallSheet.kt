package com.stockcut.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.stockcut.data.entitlement.PaywallTrigger
import com.stockcut.ui.theme.Space
import com.stockcut.ui.theme.TouchTarget

/**
 * S5 — the paywall.
 *
 * Design rules it has to satisfy, from docs/03 S5 and docs/02 §6:
 *
 *  - The headline names WHAT THEY JUST HIT, not a generic "Go Pro". Someone who
 *    tried to add a 21st part is told about parts.
 *  - "$4.99, one time. Not a subscription." is stated explicitly — docs/03 calls
 *    it a conversion driver, and it is the honest difference from most apps.
 *  - Restore purchases is present here as well as in Settings, because a
 *    reinstalled user hits the paywall before they think to look in Settings.
 *  - Dismissing is silent. NO nag, no second prompt.
 *  - 🔴 It never sells accuracy. Every bullet is scale — the optimizer is
 *    identical in both tiers (CLAUDE.md rule 9).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    trigger: PaywallTrigger,
    price: String?,
    billingUnavailable: Boolean,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Space.screenHorizontal,
                    end = Space.screenHorizontal,
                    bottom = Space.xxl,
                ),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Text(headline(trigger), style = MaterialTheme.typography.headlineSmall)

            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Bullet("Unlimited parts")
                Bullet("Unlimited saved jobs")
                Bullet("Export the plan as a PDF")
                Bullet("No ads")
            }

            Text(
                // The price comes from Play when it is known, so it is already
                // in the user's own currency. The fallback is only for the case
                // where Play could not be reached.
                text = "${price ?: "$4.99"}, one time. Not a subscription.",
                style = MaterialTheme.typography.titleMedium,
            )

            if (billingUnavailable) {
                Text(
                    "Can't reach Google Play right now. StockCut keeps working " +
                        "on the free plan — try again later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = onBuy,
                enabled = !billingUnavailable,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchTarget.primaryButtonHeight)
                    .semantics { contentDescription = "Unlock" },
            ) {
                Text("Unlock", style = MaterialTheme.typography.titleMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = onRestore,
                    modifier = Modifier
                        .heightIn(min = TouchTarget.minimum)
                        .semantics { contentDescription = "Restore purchases" },
                ) { Text("Restore purchases") }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = TouchTarget.minimum),
                ) { Text("Not now") }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Text("·  $text", style = MaterialTheme.typography.bodyMedium)
}

/** Names the limit they just hit (docs/03 S5), never a generic upsell. */
private fun headline(trigger: PaywallTrigger): String = when (trigger) {
    PaywallTrigger.PARTS -> "Unlock unlimited parts"
    PaywallTrigger.PROJECTS -> "Unlock unlimited jobs"
    PaywallTrigger.STOCK -> "Unlock unlimited stock lengths"
    PaywallTrigger.PDF_EXPORT -> "Unlock PDF export"
}
