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
        val feedbackOpensToday = intPreferencesKey("feedback_opens_today")
        val feedbackWindowStartedAt = longPreferencesKey("feedback_window_started_at")
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
        )
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

    /** Soft cooldown for the feedback form — see docs/09 §9.3. Never a hard block. */
    suspend fun recordFeedbackOpen(nowMillis: Long): Int {
        var count = 0
        context.dataStore.edit { prefs ->
            val windowStart = prefs[Keys.feedbackWindowStartedAt] ?: 0L
            val dayMillis = 24L * 60 * 60 * 1000
            if (nowMillis - windowStart >= dayMillis) {
                prefs[Keys.feedbackWindowStartedAt] = nowMillis
                count = 1
            } else {
                count = (prefs[Keys.feedbackOpensToday] ?: 0) + 1
            }
            prefs[Keys.feedbackOpensToday] = count
        }
        return count
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
) {
    val tier: Tier get() = if (isUnlocked) Tier.PAID else Tier.FREE
}
