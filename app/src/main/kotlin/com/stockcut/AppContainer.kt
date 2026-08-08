package com.stockcut

import android.content.Context
import com.stockcut.data.db.StockCutDatabase
import com.stockcut.data.repository.CutListRepository
import com.stockcut.data.repository.ProjectRepository
import com.stockcut.ads.AdsManager
import com.stockcut.ads.ConsentManager
import com.stockcut.billing.BillingManager
import com.stockcut.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manual dependency injection. The whole of it.
 *
 * TRD §2: "DI — Manual (a single AppContainer). Hilt is overhead for four
 * screens. Do not add it." CLAUDE.md repeats the ban. Four screens and three
 * collaborators do not need a graph, an annotation processor, or a second build
 * step; they need one object created once and passed down.
 *
 * Only repositories are exposed. The database and its DAOs are private, so a
 * ViewModel cannot reach past the repository layer into Room — which is the
 * whole reason that layer exists (docs/07 W2).
 *
 * Everything is `by lazy` so nothing touches the disk during Application.onCreate
 * — cold start has a 1.5 s budget on a low-end device (NFR-2), and opening Room
 * eagerly would spend part of it before the first frame.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: StockCutDatabase by lazy { StockCutDatabase.build(appContext) }

    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    val projects: ProjectRepository by lazy { ProjectRepository(database) }

    val cutLists: CutListRepository by lazy { CutListRepository(database, projects) }

    /**
     * Application-lifetime scope for billing.
     *
     * Not a ViewModel scope: a purchase must be acknowledged even if the user
     * closes the paywall the instant it completes, and Google auto-refunds
     * anything unacknowledged after 3 days. Tying that to a screen would make a
     * money bug out of a back press.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val billing: BillingManager by lazy { BillingManager(appContext, settings, appScope) }

    val consent: ConsentManager by lazy { ConsentManager(appContext) }

    val ads: AdsManager by lazy { AdsManager(appContext) }

    init {
        stampFirstRun()
    }

    /**
     * Records when this user first ran the app, for the grandfathering rule
     * described on [com.stockcut.data.settings.SettingsStore.recordFirstRunIfAbsent].
     *
     * On [appScope], never on the main thread: it is a DataStore write, and cold
     * start has a 1.5 s budget (NFR-2). Nothing waits on the result — no screen
     * reads this value in v1, and the stamp being a few milliseconds late is
     * irrelevant when it is only ever compared against a date months away.
     */
    private fun stampFirstRun() = appScope.launch {
        // firstInstallTime is more truthful than "now" for anyone who installed
        // before this field existed, and identical to it on a genuine first run.
        val installedAt = runCatching {
            appContext.packageManager
                .getPackageInfo(appContext.packageName, 0)
                .firstInstallTime
        }.getOrNull()?.takeIf { it > 0L } ?: System.currentTimeMillis()

        // Failing to stamp must never take the app down on launch. The cost of a
        // miss is one user wrongly treated as new in six months; the cost of a
        // crash here is the app not starting at all.
        runCatching { settings.recordFirstRunIfAbsent(installedAt) }
    }
}
