package com.stockcut.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.stockcut.ui.theme.StockCutTheme
import com.stockcut.units.UnitSystem
import org.junit.Rule
import org.junit.Test

/**
 * Critical-path Compose test 4 from docs/06-test-plan.md §7:
 * "Enter 1 5/16 in fractional mode → displays 1 5/16" after blur".
 *
 * The state holder is covered exhaustively by JVM tests. What can only be
 * checked on a device is the wiring: that focus loss actually reaches commit()
 * and that the reformatted text actually reaches the text box. That wiring is
 * where a field looks fine in a preview and does nothing in a user's hands.
 *
 * docs/06 §1: "Do not write Compose UI tests for everything. They are slow and
 * brittle." This file stays at the critical path.
 */
class MeasurementFieldTest {

    @get:Rule
    val rule = createComposeRule()

    private fun twoFields(
        first: MeasurementFieldState,
        second: MeasurementFieldState,
    ) {
        rule.setContent {
            StockCutTheme {
                Column {
                    MeasurementField(state = first, label = "Part length")
                    // Something to move focus to — blur is the trigger under test.
                    MeasurementField(state = second, label = "Other")
                }
            }
        }
    }

    @Test
    fun fractionalInchReformatsOnBlur() {
        val part = MeasurementFieldState(unitSystem = UnitSystem.INCH_FRACTIONAL, denominator = 16)
        twoFields(part, MeasurementFieldState())

        rule.onNodeWithContentDescription("Part length").performTextInput("1 5/16")
        rule.onNodeWithContentDescription("Other").performClick()

        rule.onNodeWithContentDescription("Part length").assertTextContains("1 5/16\"")
    }

    @Test
    fun feetAndInchesReformatOnBlur() {
        val part = MeasurementFieldState(unitSystem = UnitSystem.INCH_FRACTIONAL, denominator = 16)
        twoFields(part, MeasurementFieldState())

        rule.onNodeWithContentDescription("Part length").performTextInput("8'3.5")
        rule.onNodeWithContentDescription("Other").performClick()

        rule.onNodeWithContentDescription("Part length").assertTextContains("8' 3 1/2\"")
    }

    @Test
    fun anInvalidEntryShowsItsMessageInlineRatherThanADialog() {
        val part = MeasurementFieldState(unitSystem = UnitSystem.MM)
        twoFields(part, MeasurementFieldState())

        rule.onNodeWithContentDescription("Part length").performTextInput("abc")
        rule.onNodeWithContentDescription("Other").performClick()

        // The message is rendered as a sibling Text, on the screen, not in a dialog.
        rule.onNodeWithContentDescription("Part length").assertTextContains("abc")
        rule.onNodeWithText("Try 1200 or 1200.5").assertExists()
    }

    @Test
    fun anInitialValueRendersWithoutTheUserTypingAnything() {
        // 960 U is 3 mm — the default kerf. A settings field that opens blank
        // when it has a value is a field the user will retype unnecessarily.
        val kerf = MeasurementFieldState(initialValueU = 960L, unitSystem = UnitSystem.MM)
        twoFields(kerf, MeasurementFieldState())

        rule.onNodeWithContentDescription("Part length").assertTextContains("3")
    }
}
