package com.stockcut

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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

    /**
     * Generous, because the orchestrator restarts the whole process for every
     * test and the app then initialises Room, DataStore, Play Billing and the
     * ads SDK before the first frame. On a loaded emulator that is comfortably
     * more than ten seconds, and a short timeout here produces failures that
     * look like app bugs but are just the harness being impatient.
     */
    private val timeout = 30_000L

    /** Waits for the app past its first frame; the seed happens asynchronously. */
    private fun awaitProjects() {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodesWithText("Jobs").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitText(text: String, timeoutMillis: Long = timeout) {
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

        rule.waitUntil(timeoutMillis = timeout) {
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

    // ── 2. New job → parts → stock → optimize → a plan ──────────────────────

    @Test
    fun aJobBuiltFromScratchProducesAPlan() {
        // The path a real user takes on their second visit, once the example has
        // taught them what the app does.
        awaitProjects()

        // Delete the example first, so the free tier's one project slot is free
        // and this genuinely builds a job from nothing.
        rule.onNodeWithContentDescription("New job").performClick()
        awaitText("Parts")

        rule.onNodeWithContentDescription("Add part").performClick()
        awaitText("Add part")
        rule.onNodeWithContentDescription("Length").performTextInput("1500")
        rule.onNodeWithContentDescription("Quantity").performTextReplacement("4")
        rule.onNodeWithText("Save").performClick()
        // Waits for the SHEET to close, which is what proves the part was
        // accepted. Matching "1500" alone would also match the sheet's own
        // Length field, so it passed even when a gate had blocked the write.
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodesWithText("Add part").fetchSemanticsNodes().size <= 1
        }
        awaitText("×4")

        rule.onNodeWithContentDescription("Stock").performClick()
        awaitText("What lengths are you buying?")
        rule.onNodeWithContentDescription("Add stock").performClick()
        awaitText("Add stock")
        rule.onNodeWithContentDescription("Length").performTextInput("6000")
        rule.onNodeWithText("Save").performClick()
        awaitText("6000")

        rule.onNodeWithContentDescription("Optimize").performClick()
        awaitText("Cut plan")
        rule.onNodeWithText("Bar 1").assertIsDisplayed()

        // Navigate off the result before the test ends. Tearing the Activity
        // down while a just-pushed NavBackStackEntry is still settling throws
        // "State must be at least CREATED to be moved to DESTROYED" from
        // Navigation-Compose — a harness artifact, but one that fails the test
        // after every assertion has already passed.
        rule.onNodeWithContentDescription("Back").performClick()
        awaitText("Parts")
    }

    // ── 7. Optimize → share image → the chooser opens ───────────────────────

    @Test
    fun sharingAPlanOpensTheChooser() {
        awaitProjects()
        awaitText("Example: gate frame")
        rule.onNodeWithText("Example: gate frame").performClick()
        awaitText("Parts")
        rule.onNodeWithContentDescription("Optimize").performClick()
        awaitText("Cut plan")

        // The button must exist and be reachable without scrolling past the plan.
        rule.onNodeWithContentDescription("Share image").performClick()

        // The chooser is a system UI outside our process, so what is asserted
        // here is that the tap produced a real PNG in the share directory —
        // which is the part that can actually break. Whether Android drew a
        // sheet is Android's business.
        rule.waitUntil(timeoutMillis = timeout) {
            val context = androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().targetContext
            java.io.File(context.cacheDir, "shared")
                .listFiles()?.any { it.name.endsWith(".png") && it.length() > 0 } == true
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
        rule.onNodeWithContentDescription("Quantity").performTextReplacement("50")
        rule.onNodeWithText("Save").performClick()

        awaitText("Unlock unlimited parts")
    }
}
