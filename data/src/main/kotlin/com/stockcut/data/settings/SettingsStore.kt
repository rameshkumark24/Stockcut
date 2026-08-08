package com.stockcut.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stockcut.data.entitlement.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Global defaults and the offline entitlement cache.
 *
 * Defaults apply to NEW projects only — changing them never mutates a project the
 * user already saved.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val defaultUnitSystem = stringPreferencesKey("default_unit_system")
        val defaultFractionDenominator = intPreferencesKey("default_fraction_denominator")
        val defaultKerfU = longPreferencesKey("default_kerf_u")
        val theme = stringPreferencesKey("theme")
        val isUnlocked = booleanPreferencesKey("is_unlocked")
        val optimizeCount = intPreferencesKey("optimize_count")
        val lastReviewPromptAt = longPreferencesKey("last_review_prompt_at")
        val exampleProjectDeleted = booleanPreferencesKey("example_project_deleted")
        val lastInterstitialAt = longPreferencesKey("last_interstitial_at")
        val firstRunAt = longPreferencesKey("first_run_at")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            defaultUnitSystem = prefs[Keys.defaultUnitSystem] ?: "MM",
            defaultFractionDenominator = prefs[Keys.defaultFractionDenominator] ?: 16,
            defaultKerfU = prefs[Keys.defaultKerfU] ?: 960L,
            theme = prefs[Keys.theme] ?: "SYSTEM",
            isUnlocked = prefs[Keys.isUnlocked] ?: false,
            optimizeCount = prefs[Keys.optimizeCount] ?: 0,
            lastReviewPromptAt = prefs[Keys.lastReviewPromptAt] ?: 0L,
            exampleProjectDeleted = prefs[Keys.exampleProjectDeleted] ?: false,
            lastInterstitialAt = prefs[Keys.lastInterstitialAt] ?: 0L,
            firstRunAt = prefs[Keys.firstRunAt] ?: 0L,
        )
    }

    /**
     * Stamps when this user first ran the app, once, and never again.
     *
     * 🔴 This exists for ONE reason and it is not analytics — nothing is sent
     * anywhere; the value never leaves the device.
     *
     * StockCut v1 ships completely free (see
     * [com.stockcut.data.entitlement.Monetization]). When the paywall is
     * eventually switched on, everyone already using the app must keep what they
     * have — silently taking features back from existing users is the surest way
     * to turn a working app into one-star reviews.
     *
     * Answering "was this person here before the cutoff?" is impossible unless
     * the app wrote it down at the time, which is why this ships in v1 rather
     * than alongside the paywall that needs it.
     *
     * Never overwritten: a second call on an existing install is a no-op, so
     * reinstalling cannot quietly reclassify a long-time user as new.
     */
    suspend fun recordFirstRunIfAbsent(installedAtMillis: Long) {
        context.dataStore.edit { prefs ->
            if ((prefs[Keys.firstRunAt] ?: 0L) == 0L) {
                prefs[Keys.firstRunAt] = installedAtMillis
            }
        }
    }

    /**
     * The offline entitlement cache is AUTHORITATIVE when there is no network.
     *
     * CLAUDE.md rule 10: this may be set true by a Play verification, but it must
     * NEVER be set false because a check failed offline. A paying customer locked
     * out in a workshop with no signal writes a one-star review that costs far
     * more than any pirate.
     */
    suspend fun grantUnlock() {
        context.dataStore.edit { it[Keys.isUnlocked] = true }
    }

    /** Only call after Play Billing has positively confirmed the entitlement is gone. */
    suspend fun revokeUnlockConfirmedByPlay() {
        context.dataStore.edit { it[Keys.isUnlocked] = false }
    }

    suspend fun recordOptimize(): Int {
        var next = 0
        context.dataStore.edit {
            next = (it[Keys.optimizeCount] ?: 0) + 1
            it[Keys.optimizeCount] = next
        }
        return next
    }

    /** Feeds the minimum-gap rule that protects someone iterating on one job. */
    suspend fun recordInterstitial(nowMillis: Long) {
        context.dataStore.edit { it[Keys.lastInterstitialAt] = nowMillis }
    }

    suspend fun recordReviewPrompt(nowMillis: Long) {
        context.dataStore.edit { it[Keys.lastReviewPromptAt] = nowMillis }
    }

    suspend fun markExampleDeleted() {
        context.dataStore.edit { it[Keys.exampleProjectDeleted] = true }
    }

    suspend fun setDefaults(unitSystem: String, fractionDenominator: Int, kerfU: Long) {
        context.dataStore.edit {
            it[Keys.defaultUnitSystem] = unitSystem
            it[Keys.defaultFractionDenominator] = fractionDenominator
            it[Keys.defaultKerfU] = kerfU
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[Keys.theme] = theme }
    }

}

data class Settings(
    val defaultUnitSystem: String,
    val defaultFractionDenominator: Int,
    val defaultKerfU: Long,
    val theme: String,
    val isUnlocked: Boolean,
    val optimizeCount: Int,
    val lastReviewPromptAt: Long,
    val exampleProjectDeleted: Boolean,
    val lastInterstitialAt: Long = 0L,
    /** When this user first ran the app; 0 until the first launch records it. */
    val firstRunAt: Long = 0L,
) {
    val tier: Tier get() = if (isUnlocked) Tier.PAID else Tier.FREE
}
