package com.stockcut.ui.components

import com.stockcut.units.ParseResult
import com.stockcut.units.UnitSystem
import com.stockcut.units.format
import com.stockcut.units.parse
import com.stockcut.units.snap

/**
 * The logic behind MeasurementField — the one custom component in this app that
 * docs/04-uiux-brief.md §6 says "deserves its own week".
 *
 * Deliberately free of Android and Compose imports so it is testable on the JVM
 * in milliseconds. The composable is a thin shell over this; every rule about
 * what the field accepts, when it reformats, and what it stores lives here.
 *
 * THE THREE RULES THIS CLASS EXISTS TO ENFORCE
 *
 * 1. Parse at the input boundary, format at the display boundary, and store a
 *    Long. Nothing in between sees a unit or a decimal (TRD §3).
 *
 * 2. Changing units re-formats the display and NEVER changes the stored value
 *    (docs/03 S2c). The internal Long is unit-agnostic; a user switching mm to
 *    inches is changing how they read a bar, not cutting a different bar. Test
 *    plan item 03 pins this.
 *
 * 3. Do not nag while someone is typing. `1 5/16` passes through the invalid
 *    states `1`, `1 `, `1 5`, `1 5/` on its way to being valid. Errors appear on
 *    blur, never per keystroke.
 */
class MeasurementFieldState(
    initialValueU: Long? = null,
    unitSystem: UnitSystem = UnitSystem.MM,
    denominator: Int = 16,
) {

    var unitSystem: UnitSystem = unitSystem
        private set

    var denominator: Int = denominator
        private set

    /** Null until something valid has been committed. */
    var valueU: Long? = initialValueU
        private set

    /** Exactly what is in the text box, including mid-typing rubbish. */
    var text: String = initialValueU?.let { format(it, unitSystem, denominator) } ?: ""
        private set

    /** Null when there is nothing to complain about. Rendered inline, never as a dialog. */
    var error: String? = null
        private set

    val isValid: Boolean get() = error == null && valueU != null

    /**
     * Called on every keystroke.
     *
     * Clears a stale error so the message disappears the moment the user starts
     * fixing it, but does NOT attempt to parse — see rule 3.
     */
    fun onTextChange(new: String) {
        text = new
        error = null
    }

    /**
     * Called on blur, and before the value is used.
     *
     * On success the text is rewritten in canonical form: `3/4` becomes `3/4"`,
     * `8'3.5` becomes `8' 3 1/2"`. That is the moment the user finds out the app
     * understood them.
     *
     * @return true when the field now holds a usable value.
     */
    fun commit(): Boolean {
        if (text.isBlank()) {
            // An empty field is not an error the instant it is empty — it is an
            // error when something needs a value from it.
            valueU = null
            error = "Enter a length."
            return false
        }

        return when (val result = parse(text, unitSystem)) {
            is ParseResult.Error -> {
                error = result.message
                false
            }
            is ParseResult.Ok -> {
                // Snap to the display grid BEFORE storing.
                //
                // Without this the field would lie: type 1200.55 mm and the box
                // reformats to "1200.6" while 1200.55 is what got stored. Every
                // later reading of that bar would disagree with what is on screen
                // by 0.05 mm, and the arithmetic that makes this app worth
                // trusting would be arithmetic on a number the user never saw.
                val snapped = snap(result.valueU, unitSystem, denominator)
                valueU = snapped
                text = format(snapped, unitSystem, denominator)
                error = null
                true
            }
        }
    }

    /**
     * Switch units or fraction denominator.
     *
     * 🔴 The stored value does not move. Only its rendering does. This is the
     * single most important behaviour in this class: a tradesman who switches the
     * display to inches to read a plan must not discover their 6 m stock has
     * silently become something else.
     *
     * Any uncommitted text is discarded, because text typed in mm is meaningless
     * once the field is reading inches.
     */
    fun setUnits(system: UnitSystem, denominator: Int = this.denominator) {
        this.unitSystem = system
        this.denominator = denominator
        error = null
        text = valueU?.let { format(it, system, denominator) } ?: ""
    }

    /** Load a value from storage — already in internal units, so no parse. */
    fun setValueU(newValueU: Long?) {
        valueU = newValueU
        error = null
        text = newValueU?.let { format(it, unitSystem, denominator) } ?: ""
    }

    /** For a field whose emptiness is legitimate, such as an optional end trim. */
    fun clear() {
        valueU = null
        text = ""
        error = null
    }
}
