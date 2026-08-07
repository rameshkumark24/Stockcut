package com.stockcut

import android.app.Application
import android.os.StrictMode
import com.google.firebase.crashlytics.FirebaseCrashlytics

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
        configureCrashlytics()
        // Construction is cheap — every field inside is lazy.
        container = AppContainer(this)
    }

    /**
     * Crashlytics: RELEASE ONLY (docs/02 §7).
     *
     * Debug crashes are already in front of whoever caused them, on the machine
     * that built the app. Sending them would bury the real ones from real users
     * under noise from development — and the Gradle config deliberately skips
     * Firebase for debug builds, so there is nothing to report to anyway.
     */
    private fun configureCrashlytics() {
        // 🔴 RETURN FIRST. Debug builds have no Firebase config at all — the
        // Gradle setup skips google-services for them — so getInstance() throws
        // "Default FirebaseApp is not initialized" and takes down every debug
        // launch before the first frame.
        //
        // Setting the flag to !BuildConfig.DEBUG was not enough: the crash is in
        // the getInstance() call itself, before any value is assigned. Found by
        // running the app, not by reading it.
        if (BuildConfig.DEBUG) return

        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
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
