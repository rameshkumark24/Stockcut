package com.stockcut

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stockcut.units.U_PER_INCH
import com.stockcut.units.U_PER_MM
import com.stockcut.units.UnitSystem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Captures the Play Store screenshots from the real app.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * WHY THIS IS A TEST AND NOT A SCRIPT OF adb TAPS
 *
 * Store screenshots have to show specific screens with specific data in them,
 * and driving that by tapping blind coordinates breaks the moment a layout
 * moves. This reuses the navigation the critical-path tests already prove works.
 *
 * It also asserts as it goes, so it is a real (if shallow) test: if a screen
 * fails to render, this fails rather than quietly saving a screenshot of a blank
 * page — which is the failure mode that would otherwise reach the store.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Output lands in the app's external files dir. Pull it with:
 *
 *   adb pull /sdcard/Android/data/com.measure.stockcut/files/screenshots
 *
 * `docs/16-store-listing.md` says what caption goes on which file.
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshotTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val timeout = 30_000L

    private val outputDir: File by lazy {
        File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots")
            .apply { mkdirs() }
    }

    /**
     * A real device screenshot, including the status bar — Play expects the frame
     * a user would actually see, and a Compose-only capture omits it.
     */
    private fun capture(name: String) {
        rule.waitForIdle()
        // Let the last frame settle. Compose reports idle before the surface has
        // necessarily been composited, and capturing early yields a half-drawn
        // screen that looks like a rendering bug in the store listing.
        Thread.sleep(600)
        val bitmap: Bitmap = instrumentation.uiAutomation.takeScreenshot()
            ?: error("uiAutomation returned no screenshot for $name")
        FileOutputStream(File(outputDir, "$name.png")).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
    }

    private fun awaitText(text: String) {
        rule.waitUntil(timeout) {
            rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private val container by lazy {
        (instrumentation.targetContext.applicationContext as StockCutApplication).container
    }

    /**
     * A believable job, seeded straight through the repository.
     *
     * Typed through the UI it would take a minute of flaky taps, and the point
     * here is the picture, not the data entry — which the critical-path tests
     * already cover.
     */
    /**
     * Removes whatever is already on the device, including the seeded example.
     *
     * Without this the test is not re-runnable: a second run adds a second
     * "Balcony railing", `onNodeWithText` then matches two nodes and fails. App
     * data survives `adb install -r` and even a cold boot, so "just clear it
     * first" is a step that gets forgotten exactly once and costs a confusing
     * failure — better for the test to guarantee its own starting state.
     *
     * It also makes the jobs screenshot deterministic: two real jobs, no example.
     */
    private fun clearExistingProjects() = runBlocking {
        container.projects.observeProjects().first().forEach {
            container.projects.deleteProject(it.id)
        }
    }

    private fun seedMetricJob(): Long = runBlocking {
        val id = container.projects.createProject(
            name = "Balcony railing",
            unitSystem = UnitSystem.MM,
            fractionDenominator = 16,
            kerfU = 3 * U_PER_MM,
        )
        container.cutLists.addStock(id, 6_000 * U_PER_MM)
        // 🔴 These lengths are not arbitrary. The first attempt used round
        // numbers and produced 16.1% waste, which the result screen correctly
        // labelled "High waste" in red — a hero screenshot for a waste-reduction
        // tool, advertising bad packing.
        //
        // This set was chosen by searching for a job that is BOTH low-waste and
        // visually mixed: 7 bars at 1.95% (green), with six of the seven bars
        // holding more than one length. The gap-fillers matter — 1190 is what
        // fits in what two 2400s leave over, and 590 fits after three 1790s.
        // That is what makes bar 1 read as "2400 + 2400 + 1190" instead of three
        // identical blocks, which is the whole point of the diagram.
        //
        // If you change any of these, re-check the waste band before shipping
        // the screenshot.
        container.cutLists.addPart(id, 2_400 * U_PER_MM, 6, "Post")
        container.cutLists.addPart(id, 1_790 * U_PER_MM, 6, "Top rail")
        container.cutLists.addPart(id, 1_490 * U_PER_MM, 4, "Bottom rail")
        container.cutLists.addPart(id, 1_190 * U_PER_MM, 2, "Newel")
        container.cutLists.addPart(id, 890 * U_PER_MM, 6, "Baluster")
        container.cutLists.addPart(id, 590 * U_PER_MM, 4, "Spacer")
        id
    }

    /** The same idea in fractional inches — the feature nothing else on the store has. */
    private fun seedImperialJob(): Long = runBlocking {
        val id = container.projects.createProject(
            name = "Workbench frame",
            unitSystem = UnitSystem.INCH_FRACTIONAL,
            fractionDenominator = 16,
            kerfU = U_PER_INCH / 8,
        )
        container.cutLists.addStock(id, 96 * U_PER_INCH)
        // 1 5/16" is the caption's promise, so it has to be visible on screen.
        container.cutLists.addPart(id, U_PER_INCH + U_PER_INCH * 5 / 16, 12, "Spacer")
        container.cutLists.addPart(id, 34 * U_PER_INCH + U_PER_INCH / 2, 4, "Leg")
        container.cutLists.addPart(id, 22 * U_PER_INCH + U_PER_INCH * 3 / 4, 6, "Stretcher")
        id
    }

    @Test
    fun captureStoreScreenshots() {
        rule.waitUntil(timeout) {
            rule.onAllNodesWithText("Jobs").fetchSemanticsNodes().isNotEmpty()
        }

        clearExistingProjects()
        seedMetricJob()
        seedImperialJob()
        rule.waitForIdle()

        // ── 4. The jobs list ────────────────────────────────────────────────
        awaitText("Balcony railing")
        capture("04-jobs")

        // ── 1. THE CUT PLAN — the one that sells the app ────────────────────
        rule.onNodeWithText("Balcony railing").performClick()
        awaitText("Parts")
        capture("02-parts-metric")

        // ── 3. The stock screen ─────────────────────────────────────────────
        rule.onNodeWithContentDescription("Stock").performClick()
        awaitText("6000")
        capture("03-stock")

        // Optimize straight from the Stock tab — it lives in the app bar and is
        // reachable from either tab, so switching back to Parts first was a step
        // that could only fail.
        rule.onNodeWithContentDescription("Optimize").performClick()
        awaitText("Cut plan")
        // NOT "Bar 1" — identical bars collapse into one card reading
        // "Bars 1–2 · ×2 identical bars", which does not contain "Bar 1". This
        // job has repeats by design, so the plural form is the expected one.
        awaitText("offcut total")
        awaitText("Bars 1")
        // The summary strip (bars · waste % · total offcut) sits at the TOP of
        // this screen, so this one shot carries both the plan and the waste
        // figure. A separate "waste summary" screenshot would have been the same
        // pixels twice — which is how a listing ends up with five images and
        // three ideas.
        capture("01-cut-plan")

        // ── 5. Fractional inches ────────────────────────────────────────────
        rule.onNodeWithContentDescription("Back").performClick()
        awaitText("Parts")
        rule.onNodeWithContentDescription("Back").performClick()
        awaitText("Jobs")

        rule.onNodeWithText("Workbench frame").performClick()
        awaitText("Parts")
        // Proves the caption is honest before the picture is taken.
        awaitText("1 5/16\"")
        capture("05-fractional-inches")
    }
}
