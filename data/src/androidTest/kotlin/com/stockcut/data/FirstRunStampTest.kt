package com.stockcut.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stockcut.data.settings.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The first-run stamp — the hook that makes grandfathering possible.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * WHY THIS SMALL THING HAS ITS OWN TEST
 *
 * StockCut v1 ships free. When the paywall is eventually switched on, everyone
 * already using the app must keep what they have; deciding who those people are
 * needs a date the app recorded at the time.
 *
 * That makes this a write-once value whose failure mode is invisible for months
 * and unfixable when it finally matters. If the stamp silently overwrites, a
 * long-time user gets reclassified as new and loses features they have had all
 * along — and there is no way to reconstruct the truth after the fact, because
 * the only copy was the one that got overwritten.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Written to be order-independent: DataStore is process-wide, so this cannot
 * assume it runs against an empty store, and deleting the file underneath a live
 * DataStore hangs the next read (see the note in CriticalPathTest). So it asserts
 * the INVARIANT — a second call changes nothing — rather than an exact value.
 */
@RunWith(AndroidJUnit4::class)
class FirstRunStampTest {

    private val store = SettingsStore(ApplicationProvider.getApplicationContext())

    @Test
    fun theStampIsWrittenOnceAndNeverOverwritten() = runBlocking {
        store.recordFirstRunIfAbsent(1_000_000L)
        val stamped = store.settings.first().firstRunAt
        assertTrue(stamped > 0L, "the first run was never stamped")

        // A later launch, a reinstall restoring a backup, a second AppContainer
        // in a test — none may move the date forward.
        store.recordFirstRunIfAbsent(9_999_999_999L)
        assertEquals(
            stamped,
            store.settings.first().firstRunAt,
            "the stamp moved, so a long-time user would be treated as new",
        )
    }

    @Test
    fun repeatedCallsAreIdempotent() = runBlocking {
        // AppContainer stamps on every construction, not only on first launch —
        // it has no cheap way to know which it is. So "called every time" is the
        // normal case, not an edge case.
        store.recordFirstRunIfAbsent(2_000_000L)
        val after = store.settings.first().firstRunAt
        repeat(5) { store.recordFirstRunIfAbsent(3_000_000L + it) }
        assertEquals(after, store.settings.first().firstRunAt)
    }
}
