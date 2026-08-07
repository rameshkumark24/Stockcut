package com.stockcut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.stockcut.ui.navigation.StockCutNavHost
import com.stockcut.ui.theme.StockCutTheme
import com.stockcut.ui.theme.ThemeMode
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The single Activity. Everything else is Compose.
 *
 * enableEdgeToEdge is called because API 36 enforces edge-to-edge display for
 * apps targeting it — gap audit §B1 warns this is cheap to design for now and
 * expensive to retrofit, which is why targetSdk is 36 from the start rather
 * than 35 with an upgrade "later".
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as StockCutApplication).container

        // 🔴 ORDER MATTERS: consent first, ads only once it resolves.
        // Initialising the ads SDK before UMP has decided is the violation, and
        // it is easy to miss because ads still appear to work.
        container.consent.gather(this) {
            container.ads.initialise()
        }

        // Billing connects on every launch. This is also what re-grants the
        // unlock after a reinstall, and what picks up a refund — but only ever
        // from a POSITIVE answer from Play (CLAUDE.md rule 10).
        container.billing.connect()

        val themeFlow = container.settings.settings.map { stored ->
            runCatching { ThemeMode.valueOf(stored.theme) }.getOrDefault(ThemeMode.SYSTEM)
        }

        setContent {
            val theme by themeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            StockCutTheme(themeMode = theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StockCutNavHost(container = container)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-verify with Play whenever the app comes forward with a connection.
        // A refund issued in Play Console lands here on the next online launch.
        lifecycleScope.launch { container().billing.refreshPurchases() }
    }

    override fun onDestroy() {
        super.onDestroy()
        container().billing.release()
    }

    private fun container() = (application as StockCutApplication).container
}
