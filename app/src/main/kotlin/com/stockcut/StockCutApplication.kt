package com.stockcut

import android.app.Application

/**
 * Holds the one [AppContainer].
 *
 * Nothing else belongs here. There is no background work to start (NFR-10: no
 * WorkManager, no services, no alarms), no SDK to initialise yet, and no network
 * to warm up. Ads and Crashlytics arrive in Phase 6 and will initialise here;
 * until then this class staying almost empty is the point.
 */
class StockCutApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Construction is cheap — every field inside is lazy.
        container = AppContainer(this)
    }
}
