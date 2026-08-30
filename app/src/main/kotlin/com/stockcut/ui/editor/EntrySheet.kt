package com.stockcut.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.stockcut.ui.components.MeasurementField
import com.stockcut.ui.components.MeasurementFieldState
import com.stockcut.ui.theme.Space
import com.stockcut.ui.theme.TouchTarget
import com.stockcut.units.UnitSystem

/**
 * Add or edit one length. Used for both parts and stock.
 *
 * A sheet rather than a route, deliberately — see Routes.kt. It is dismissed by
 * the caller, so a gate that fires (a paywall) can keep it open instead of
 * throwing away what the user typed.
 *
 * @param allowUnlimited stock can be "I'll buy as many as needed"; a count of
 *   pieces to cut cannot.
 * @param onSubmit returns false when a gate blocked the write, which keeps the
 *   sheet open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    title: String,
    unitSystem: UnitSystem,
    denominator: Int,
    allowUnlimited: Boolean,
    initialLengthU: Long?,
    initialQuantity: Int,
    initialLabel: String?,
    onDismiss: () -> Unit,
    onSubmit: (lengthU: Long, quantity: Int, label: String?) -> Boolean,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val length = remember(unitSystem, denominator) {
        MeasurementFieldState(initialLengthU, unitSystem, denominator)
    }
    var quantityText by remember { mutableStateOf(initialQuantity.takeIf { it > 0 }?.toString() ?: "1") }
    var unlimited by remember { mutableStateOf(allowUnlimited && initialQuantity < 0) }
    var label by remember { mutableStateOf(initialLabel.orEmpty()) }
    var quantityError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // verticalScroll, because this form has to survive the keyboard.
        //
        // ModalBottomSheet lifts itself above the IME, so the sheet is not
        // hidden — but that lift SHRINKS the height its content is given, and
        // this Column had no scroll at all. Anything that no longer fits was
        // simply clipped, with no way to reach it.
        //
        // The tail of this form is Quantity, Label and Save, so Save is the
        // first thing to go. On a short phone, in landscape, or at a larger
        // font scale, that strands the user in the app's MOST-USED sheet:
        // every part and every stock length is entered through here.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Space.screenHorizontal,
                    end = Space.screenHorizontal,
                    bottom = Space.xxl,
                ),
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)

            MeasurementField(
                state = length,
                label = "Length",
                helperText = helperFor(unitSystem),
            )

            if (allowUnlimited) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Unlimited", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "I'll buy as many as needed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = unlimited,
                        onCheckedChange = { unlimited = it },
                        modifier = Modifier.semantics { contentDescription = "Unlimited" },
                    )
                }
            }

            if (!unlimited) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it; quantityError = null },
                    label = { Text("Quantity") },
                    singleLine = true,
                    isError = quantityError != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TouchTarget.fieldHeight)
                        .semantics { contentDescription = "Quantity" },
                )
                quantityError?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchTarget.fieldHeight)
                    .semantics { contentDescription = "Label" },
            )

            Button(
                onClick = {
                    // Commit explicitly: the user can tap Save without ever
                    // blurring the length field, and an uncommitted field holds
                    // raw text rather than a value.
                    if (!length.commit()) return@Button
                    val lengthU = length.valueU ?: return@Button

                    val quantity = if (unlimited) {
                        com.stockcut.data.model.StockEntry.UNLIMITED
                    } else {
                        val parsed = quantityText.trim().toIntOrNull()
                        if (parsed == null || parsed <= 0) {
                            quantityError = "Quantity must be more than 0."
                            return@Button
                        }
                        parsed
                    }

                    if (onSubmit(lengthU, quantity, label.trim().ifBlank { null })) onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchTarget.primaryButtonHeight),
            ) {
                Text("Save", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun helperFor(system: UnitSystem): String = when (system) {
    UnitSystem.INCH_FRACTIONAL -> "Try 3/4, 1 5/16, or 8' 3 1/2\""
    UnitSystem.INCH_DECIMAL -> "Try 47 or 47.25"
    else -> "Try 1200 or 1200.5"
}
