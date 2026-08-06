package com.stockcut.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockcut.BuildConfig
import com.stockcut.ui.settings.SettingsViewModel
import com.stockcut.ui.theme.MeasurementTextStyle
import com.stockcut.ui.theme.Space
import com.stockcut.ui.theme.TouchTarget

/**
 * S7 — About and support.
 *
 * What is here: version, email support, rate on Play, licences.
 *
 * What is deliberately NOT here yet: the "Report a problem" link to the Google
 * Form, and the privacy policy link. Both are blocked on W0 — the form does not
 * exist, and neither does the GitHub Pages redirect the app must point at
 * INSTEAD of the form (docs/09 §9.5). That redirect is the only kill switch
 * this app has without a server, and retrofitting it needs exactly the app
 * update it exists to avoid. So the URL is not hardcoded to a guess.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard: ClipboardManager = LocalClipboardManager.current

    val diagnostics = buildDiagnostics(
        appVersionName = BuildConfig.VERSION_NAME,
        appVersionCode = BuildConfig.VERSION_CODE,
        androidRelease = Build.VERSION.RELEASE ?: "unknown",
        deviceModel = Build.MODEL ?: "unknown",
        unitSystem = state.defaultUnitSystem,
        denominator = state.defaultDenominator,
        tier = state.tier,
        optimizeCount = state.optimizeCount,
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("About", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" },
                    ) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Space.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            Text("StockCut", style = MaterialTheme.typography.headlineSmall)
            Text(
                // Support asks for this first (docs/03 S7).
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = diagnostics,
                    style = MeasurementTextStyle,
                    modifier = Modifier.padding(Space.cardInner),
                )
            }
            Text(
                // Transparency is the design, not a compromise (docs/09 §4.2).
                "Sent only if you choose to. It contains no name, no email, " +
                    "no location, and nothing from your jobs.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_SUBJECT, "StockCut feedback")
                        putExtra(Intent.EXTRA_TEXT, "\n\n---\n$diagnostics")
                    }
                    // Always leave one working path (docs/03 S7): if no mail app
                    // exists, the diagnostics go to the clipboard instead of the
                    // user hitting a dead end.
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        clipboard.setText(AnnotatedString(diagnostics))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchTarget.primaryButtonHeight)
                    .semantics { contentDescription = "Email support" },
            ) { Text("Email support") }

            OutlinedButton(
                onClick = {
                    // market:// opens the Play app directly; the https fallback
                    // covers devices without it.
                    val market = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=${context.packageName}"),
                    )
                    val web = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://play.google.com/store/apps/details?id=${context.packageName}",
                        ),
                    )
                    val chosen = if (market.resolveActivity(context.packageManager) != null) {
                        market
                    } else {
                        web
                    }
                    if (chosen.resolveActivity(context.packageManager) != null) {
                        context.startActivity(chosen)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchTarget.primaryButtonHeight)
                    .semantics { contentDescription = "Rate on Play" },
            ) { Text("Rate on Play") }

            Text("Not built yet", style = MaterialTheme.typography.titleMedium)
            Text(
                // Named honestly rather than shown as buttons that do nothing.
                "Report a problem, the privacy policy link, and Restore purchases " +
                    "are not wired up yet. The first two need the feedback form and " +
                    "its redirect page to exist; the third needs in-app billing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
