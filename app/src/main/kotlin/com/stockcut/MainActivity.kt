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
import com.stockcut.ui.navigation.StockCutNavHost
import com.stockcut.ui.theme.StockCutTheme
import com.stockcut.ui.theme.ThemeMode
import kotlinx.coroutines.flow.map

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
        // Read here rather than inside a screen, because the theme wraps the
        // whole app: choosing Dark in Settings has to repaint the screen behind
        // the chooser too, not just the next screen the user opens.
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
}
