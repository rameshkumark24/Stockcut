package com.stockcut.billing

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockcut.AppContainer
import com.stockcut.data.entitlement.PaywallTrigger
import kotlinx.coroutines.launch

/**
 * Renders [PaywallSheet] for whichever screen raised it.
 *
 * One host rather than a copy per screen. S1, S2 and S4 can all raise the
 * paywall, and three copies would be three places for the buy button, the
 * restore call, or the "billing unavailable" message to drift apart — on the one
 * screen where drift costs money.
 */
@Composable
fun PaywallHost(
    container: AppContainer,
    trigger: PaywallTrigger?,
    onDismiss: () -> Unit,
) {
    if (trigger == null) return

    val activity = LocalContext.current as? Activity ?: return
    val scope = rememberCoroutineScope()
    val billingState by container.billing.state.collectAsStateWithLifecycle()
    val price by container.billing.productPrice.collectAsStateWithLifecycle()

    PaywallSheet(
        trigger = trigger,
        price = price,
        billingUnavailable = billingState is BillingManager.State.Unavailable,
        onBuy = {
            container.billing.launchPurchase(activity)
            // The sheet closes now. onPurchasesUpdated grants the unlock
            // whenever Play answers — keeping the sheet open waiting on it
            // would leave the user staring at a spinner behind the Play dialog.
            onDismiss()
        },
        onRestore = {
            scope.launch {
                container.billing.restorePurchases()
                onDismiss()
            }
        },
        onDismiss = onDismiss,
    )
}
