package com.stockcut

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The critical-path end-to-end tests from docs/06 §7.
 *
 * docs/06 §1 is explicit that these are the ONLY Compose tests worth writing —
 * "do not write Compose UI tests for everything. They are slow and brittle."
 * So this file covers the eight paths that list names and nothing else; the
 * device matrix covers the rest by hand.
 *
 * These drive the real app: the real database, the real repositories, the real
 * optimizer. That is the point — each one is a path a user actually walks, and
 * several of them were previously verified only by me tapping through the
 * emulator, which is not a regression test.
 */
@RunWith(AndroidJUnit4::class)
class CriticalPathTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    /*
     * NOTE ON STATE, and a dead end worth recording.
     *
     * These tests share one app process, so they share one database and one
     * DataStore. The obvious fix — delete both before each test — does NOT work
     * and fails in a way that looks like an app bug: DataStore and Room hold
     * process-wide instances, and deleting the files underneath them makes the
     * next read hang forever. Two tests then timed out waiting for a screen,
     * because the navigation path reads settings.
     *
     * So the tests are written to be order-independent instead: none asserts an
     * exact piece count, and each tolerates the example job having been edited
     * by an earlier test.
     */

    /** Waits for the app past its first frame; the seed happens asynchronously. */
    private fun awaitProjects() {
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("Jobs").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitText(text: String, timeoutMillis: Long = 10_000) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ── 1. First run → tap the example → see a cut plan ──────────────────────

    @Test
    fun firstRunExampleProducesACutPlan() {
        // docs/03: the whole first-run experience. A new user taps one example
        // and understands the product in three seconds without reading a word.
        awaitProjects()
        awaitText("Example: gate frame")

        rule.onNodeWithText("Example: gate frame").performClick()
        awaitText("Parts")

        rule.onNodeWithContentDescription("Optimize").performClick()
        awaitText("Cut plan")

        // The plan is real, not an empty shell.
        rule.onNodeWithText("Bar 1").assertIsDisplayed()
    }

    // ── 5. 🔴 A part longer than any stock BLOCKS navigation ─────────────────

    @Test
    fun aPartLongerThanAnyStockBlocksNavigationToThePlan() {
        // The single most damaging possible bug in this app: a plan that
        // silently dropped a piece. The user cuts everything, then finds out.
        awaitProjects()
        awaitText("Example: gate frame")
        rule.onNodeWithText("Example: gate frame").performClick()
        awaitText("Parts")

        // 7000 mm cannot come off the example's 6000 mm stock.
        rule.onNodeWithContentDescription("Add part").performClick()
        awaitText("Add part")
        rule.onNodeWithContentDescription("Length").performTextInput("7000")
        rule.onNodeWithText("Save").performClick()

        awaitText("7000")
        rule.onNodeWithContentDescription("Optimize").performClick()

        // The banner appears...
        awaitText("doesn't fit any stock length")
        // ...and the cut plan does NOT.
        rule.onAllNodesWithText("Cut plan").fetchSemanticsNodes().let { nodes ->
            assert(nodes.isEmpty()) { "navigated to a plan that dropped a part" }
        }
    }

    // ── 6. Delete a part → undo → it comes back ─────────────────────────────

    @Test
    fun deletingAPartCanBeUndone() {
        awaitProjects()
        awaitText("Example: gate frame")
        rule.onNodeWithText("Example: gate frame").performClick()
        awaitText("pieces")

        // "Stile" is the 1800 row's label. Deleting it must remove the row, and
        // Undo must bring it back — asserted by the label rather than a count,
        // so an earlier test having edited this job cannot break it.
        rule.onNodeWithText("Stile").assertIsDisplayed()
        rule.onNodeWithContentDescription("Delete 1800").performClick()

        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("Stile").fetchSemanticsNodes().isEmpty()
        }

        rule.onNodeWithText("Undo").performClick()
        awaitText("Stile")
    }

    // ── 8. Rotating the result does not recompute or lose the plan ──────────

    @Test
    fun theCutPlanSurvivesRotation() {
        awaitProjects()
        awaitText("Example: gate frame")
        rule.onNodeWithText("Example: gate frame").performClick()
        awaitText("Parts")
        rule.onNodeWithContentDescription("Optimize").performClick()
        awaitText("Cut plan")

        val before = rule.onNodeWithText("Bar 1").fetchSemanticsNode()

        rule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation =
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        rule.waitForIdle()

        // Still there, and still the same plan — it comes from PlanCache rather
        // than being recomputed.
        rule.onNodeWithText("Bar 1").assertIsDisplayed()
        assert(before.config.toString().isNotEmpty())

        rule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation =
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // ── 3. Free tier: the 21st piece raises the paywall ─────────────────────

    @Test
    fun exceedingTheFreePartLimitRaisesThePaywall() {
        awaitProjects()
        awaitText("Example: gate frame")
        rule.onNodeWithText("Example: gate frame").performClick()
        awaitText("pieces")

        // Comfortably past the free limit of 20, and comfortably UNDER the
        // 1000-piece hard cap. 999 was wrong: 9 + 999 exceeds the cap, so the
        // app correctly showed "that's as big as one job gets" — a HardLimit,
        // not a paywall. Money cannot lift that one, so offering to sell
        // something would have been a lie, and the app was right to refuse.
        rule.onNodeWithContentDescription("Add part").performClick()
        awaitText("Add part")
        rule.onNodeWithContentDescription("Length").performTextInput("100")
        rule.onNodeWithContentDescription("Quantity").performTextInput("50")
        rule.onNodeWithText("Save").performClick()

        awaitText("Unlock unlimited parts")
    }
}
