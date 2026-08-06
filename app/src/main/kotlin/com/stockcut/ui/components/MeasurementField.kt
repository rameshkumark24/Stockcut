package com.stockcut.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.stockcut.ui.theme.MeasurementTextStyle
import com.stockcut.ui.theme.Space
import com.stockcut.ui.theme.TouchTarget

/**
 * A length input. All the behaviour lives in [MeasurementFieldState]; this is
 * the shell.
 *
 * Keyboard choice, per docs/04 §6 "numeric-first keyboard with a / and ' key":
 * KeyboardType.Number gives digits only — no slash, no apostrophe, no way to
 * type `1 5/16`. So fractional modes get the full text keyboard, which does
 * have them, and metric modes get Decimal. Getting this backwards would make
 * fractional entry literally impossible on most keyboards, which is the exact
 * failure docs/09 §4.1 predicts for the US market.
 *
 * The error is rendered inline beneath the field, never as a dialog (docs/04 §7).
 */
@Composable
fun MeasurementField(
    state: MeasurementFieldState,
    label: String,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    // The state holder is a plain class, so Compose needs an explicit signal to
    // recompose. Bumping a counter on each mutation is cheaper and easier to
    // follow than making every field in the holder a MutableState.
    var revision by remember { mutableStateOf(0) }

    fun mutate(block: () -> Unit) {
        block()
        revision++
    }

    @Suppress("UNUSED_EXPRESSION")
    revision // read it, so this composable subscribes

    val keyboard = when (state.unitSystem) {
        com.stockcut.units.UnitSystem.INCH_FRACTIONAL -> KeyboardType.Text
        else -> KeyboardType.Decimal
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = state.text,
            onValueChange = { new -> mutate { state.onTextChange(new) } },
            label = { Text(label) },
            singleLine = true,
            isError = state.error != null,
            textStyle = MeasurementTextStyle,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction),
            modifier = Modifier
                .fillMaxWidth()
                // heightIn, not height — a fixed height clips at max font scale,
                // which docs/06 §6 requires every screen to survive.
                .heightIn(min = TouchTarget.fieldHeight)
                .onFocusChanged { focus ->
                    if (!focus.isFocused && state.text.isNotBlank()) {
                        mutate { state.commit() }
                    }
                }
                .semantics { contentDescription = label },
        )

        val message = state.error ?: helperText
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = if (state.error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(start = Space.md, top = Space.xs),
            )
        }
    }
}
