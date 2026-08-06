package com.stockcut

import android.app.Application
import android.os.StrictMode

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
        enableStrictModeInDebug()
        // Construction is cheap — every field inside is lazy.
        container = AppContainer(this)
    }

    /**
     * StrictMode, debug builds only.
     *
     * This app's responsiveness budget is tight — NFR-2 gives cold start 1.5 s on
     * a low-end phone, and NFR-6 puts ANR rate under Play's bad-behaviour
     * threshold. Both are broken by the same thing: disk work on the main thread.
     * Room and DataStore are easy to call from the wrong place, and the mistake
     * is invisible on a fast emulator and obvious on a user's three-year-old
     * device.
     *
     * penaltyLog rather than penaltyDeath: a violation should be loud in logcat
     * during development, not a crash that stops someone mid-task. Read it with
     *   adb logcat -s StrictMode
     */
    private fun enableStrictModeInDebug() {
        if (!BuildConfig.DEBUG) return

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build(),
        )
    }
}
