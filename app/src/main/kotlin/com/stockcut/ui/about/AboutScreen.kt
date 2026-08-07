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
import androidx.compose.runtime.setValue
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

/** Published from this repo's root by GitHub Pages. */
private const val PRIVACY_POLICY_URL =
    "https://rameshkumark24.github.io/Stockcut/privacy-policy.html"

/**
 * S7 — About and support.
 *
 * What is here: version, email support, rate on Play, licences.
 *
 * 🔴 There is NO feedback form, and that is a deliberate product decision, not a
 * missing feature. This app collects nothing from its users — no bug reports, no
 * suggestions, no submissions of any kind.
 *
 * The only way to reach the developer is the mailto link below, which is not
 * collection: it opens the user's OWN mail app, they see and edit everything,
 * and they decide whether to send. Nothing reaches us unless a person chooses to
 * write it and press send. Play also requires a support contact on the listing,
 * so removing this too would leave a one-star review as the only channel.
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
                // Shown so the user knows exactly what an email would carry.
                "Nothing here is sent anywhere. If you email support, this line " +
                    "is added to the message, and you can delete it first. " +
                    "It contains no name, no email, no location, and nothing " +
                    "from your jobs.",
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

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchTarget.primaryButtonHeight)
                    .semantics { contentDescription = "Privacy policy" },
            ) { Text("Privacy policy") }

            var showLicences by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(false)
            }
            OutlinedButton(
                onClick = { showLicences = !showLicences },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchTarget.primaryButtonHeight)
                    .semantics { contentDescription = "Open-source licences" },
            ) { Text(if (showLicences) "Hide licences" else "Open-source licences") }

            if (showLicences) {
                // Every dependency requires attribution; docs/14 confirms none
                // is copyleft, so notices satisfy the whole obligation.
                Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    LICENCES.forEach { licence ->
                        Column {
                            Text(licence.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                licence.copyright,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                licence.licence,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        APACHE_NOTICE,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text("Not built yet", style = MaterialTheme.typography.titleMedium)
            Text(
                // Named honestly rather than shown as a button that does nothing.
                "Restore purchases arrives with in-app billing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
