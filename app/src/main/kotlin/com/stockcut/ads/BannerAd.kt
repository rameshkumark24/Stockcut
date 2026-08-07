package com.stockcut.ads

import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * A banner that occupies NO space unless an ad actually renders.
 *
 * 🔴 docs/03 and CLAUDE.md: "Never reserve blank space for an ad that failed to
 * load — collapse the container." A permanent grey rectangle where an ad should
 * be looks like a broken app, and on a screen a tradesman is reading lengths off,
 * it is stealing room from the thing they came for.
 *
 * So this composable renders nothing at all until [AdListener.onAdLoaded] fires,
 * and removes itself again if a later load fails.
 */
@Composable
fun BannerAd(
    adUnitId: String,
    canRequestAds: Boolean,
    modifier: Modifier = Modifier,
) {
    // Nothing is requested before consent has been resolved.
    if (!canRequestAds) return

    var loaded by remember(adUnitId) { mutableStateOf(false) }

    AndroidView(
        modifier = if (loaded) modifier.fillMaxWidth() else Modifier,
        factory = { context ->
            FrameLayout(context).apply {
                addView(
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        this.adUnitId = adUnitId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                loaded = true
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                // Collapse. Not an error the user should see.
                                loaded = false
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    },
                )
            }
        },
    )
}
