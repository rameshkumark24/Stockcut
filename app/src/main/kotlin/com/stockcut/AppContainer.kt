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
}
