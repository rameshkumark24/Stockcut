package com.stockcut.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.stockcut.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The unlock. One product, bought once, kept forever.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * THE TWO RULES THIS CLASS EXISTS TO GET RIGHT
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * 🔴 1. NEVER downgrade a paid user because a check failed offline.
 *    (CLAUDE.md rule 10, docs/02 §6.) The DataStore cache is authoritative when
 *    Play cannot be reached. A tradesman locked out of features he paid for,
 *    in a workshop with no signal, writes a one-star review that costs far more
 *    than any pirate ever could. So `revokeUnlock` is called ONLY when Play has
 *    positively told us the entitlement is gone — never on a failure, a
 *    timeout, or a disconnect.
 *
 * 🔴 2. Acknowledge a purchase IMMEDIATELY.
 *    Google automatically refunds anything left unacknowledged for 3 days. That
 *    makes this a money bug, not a polish item, and it is why acknowledgement
 *    happens in the same coroutine as the unlock rather than being scheduled.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Piracy: the local cache can be flipped on a rooted device. That cannot be
 * prevented without a server and there is no server (docs/02 §13.1). The posture
 * is to accept it — someone patching an APK to avoid $4.99 was never going to
 * pay — and to never risk a false positive against a real customer.
 */
class BillingManager(
    context: Context,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    /**
     * 🔴 Must match the product ID created in Play Console exactly.
     * Play Console → Monetise → In-app products. Permanent once created.
     */
    companion object {
        const val UNLOCK_PRODUCT_ID = "stockcut_unlock"
    }

    sealed interface State {
        /** Not yet connected, or reconnecting. */
        data object Connecting : State
        data object Ready : State

        /**
         * Play Billing is unreachable — no Play Store, an emulator, a China ROM,
         * or simply offline. The app stays FULLY usable on the free tier; this
         * only means the buy button shows a retry (docs/02 §10).
         */
        data class Unavailable(val reason: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Connecting)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _productPrice = MutableStateFlow<String?>(null)

    /** Localised price string from Play, e.g. "$4.99" or "₹449". Null until known. */
    val productPrice: StateFlow<String?> = _productPrice.asStateFlow()

    private var productDetails: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        // Required from Billing 8; a one-time product can still end up pending
        // (slow card, parental approval), and those must be handled not ignored.
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect() {
        if (client.isReady) return
        _state.value = State.Connecting
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = State.Ready
                    scope.launch {
                        loadProduct()
                        // Re-verify on every launch that reaches Play. This is
                        // also the path that grants the unlock after a reinstall.
                        refreshPurchases()
                    }
                } else {
                    _state.value = State.Unavailable(result.debugMessage.ifBlank { "Play Billing unavailable" })
                }
            }

            override fun onBillingServiceDisconnected() {
                // 🔴 NOT a downgrade. Losing the connection says nothing about
                // whether the user paid.
                _state.value = State.Connecting
            }
        })
    }

    private suspend fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(UNLOCK_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()

        val result = client.queryProductDetails(params)
        val details = result.productDetailsList?.firstOrNull()
        productDetails = details
        _productPrice.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    /**
     * Ask Play what this user owns.
     *
     * 🔴 A failure here does nothing. Only a successful response that does NOT
     * contain the unlock revokes it — and even then only if Play says so
     * positively, which is the refund case.
     */
    suspend fun refreshPurchases() {
        if (!client.isReady) return

        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        )
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return

        val owned = result.purchasesList.any { purchase ->
            purchase.products.contains(UNLOCK_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (owned) {
            result.purchasesList.forEach { handlePurchase(it) }
        } else {
            // Play answered successfully and the unlock is not there — a refund
            // or a chargeback. This is the ONLY path that revokes.
            settings.revokeUnlockConfirmedByPlay()
        }
    }

    /** "Restore purchases" — mandatory for reinstalls, and reviewers look for it. */
    suspend fun restorePurchases(): Boolean {
        refreshPurchases()
        return client.isReady
    }

    fun launchPurchase(activity: Activity) {
        val details = productDetails ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                purchases?.forEach { purchase -> scope.launch { handlePurchase(purchase) } }

            // Cancelled: sheet closes silently. NO nag, no second prompt
            // (docs/03 S5). Nothing to do is the correct behaviour.
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit

            // Already owned but not reflected locally — grant it.
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                scope.launch { refreshPurchases() }

            else -> Unit
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (!purchase.products.contains(UNLOCK_PRODUCT_ID)) return

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                // Unlock FIRST, so a user who paid is never left waiting on a
                // network call to use what they bought.
                settings.grantUnlock()

                // 🔴 Then acknowledge, immediately. Unacknowledged for 3 days
                // means Google refunds it automatically.
                if (!purchase.isAcknowledged) {
                    client.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build(),
                    )
                }
            }

            // Slow payment method or parental approval. Do NOT unlock yet — the
            // money has not moved — and do not revoke either.
            Purchase.PurchaseState.PENDING -> Unit

            else -> Unit
        }
    }

    fun release() {
        if (client.isReady) client.endConnection()
    }
}
