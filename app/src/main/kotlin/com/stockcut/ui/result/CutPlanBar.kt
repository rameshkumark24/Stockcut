package com.stockcut.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockcut.optimizer.CutBar
import com.stockcut.ui.theme.LocalStockCutColors
import com.stockcut.ui.theme.MeasurementTextStyle
import com.stockcut.units.UnitSystem
import com.stockcut.units.format

/**
 * The signature visual: one stock bar drawn as proportional segments.
 *
 * docs/04 §6 calls this the app's main store screenshot, and docs/03 S4 says to
 * design it first, not last.
 *
 * Two rules it has to satisfy at once:
 *  - Segments alternate colour so adjacent cuts are distinguishable.
 *  - Colour is NEVER the only signal (docs/04 §2). Every segment wide enough to
 *    hold text carries its length, so the plan reads identically to a
 *    colour-blind user and in black and white when printed.
 */
@Composable
fun CutPlanBar(
    bar: CutBar,
    kerfU: Long,
    unitSystem: UnitSystem,
    denominator: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStockCutColors.current
    val segments = bar.segmentWeights(kerfU)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        segments.forEachIndexed { index, segment ->
            // Alternate only between PART segments, so the pattern tracks the
            // pieces rather than resetting on every kerf.
            val partIndex = segments.take(index).count { it.kind == SegmentKind.PART }
            val fill = when (segment.kind) {
                SegmentKind.PART ->
                    if (partIndex % 2 == 0) colors.cutSegment else colors.cutSegmentAlt
                SegmentKind.OFFCUT -> colors.offcut
                SegmentKind.TRIM -> colors.offcut.copy(alpha = 0.6f)
                // A 3 mm kerf on a 6 m bar is 0.05% — a hairline, but drawing it
                // is what makes the picture add up to the whole bar.
                SegmentKind.KERF -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .weight(segment.weight.coerceAtLeast(0.0005f))
                    .fillMaxSize()
                    .background(fill),
                contentAlignment = Alignment.Center,
            ) {
                if (segment.kind == SegmentKind.PART || segment.kind == SegmentKind.OFFCUT) {
                    // 🔴 A clipped number reads as a DIFFERENT number — a 288 mm
                    // offcut rendered in a narrow segment showed as "28". Worse
                    // than showing nothing, on a screen whose entire job is
                    // telling someone what length to cut.
                    //
                    // So: measure, and if it does not fit, make it invisible
                    // rather than truncating it. Alpha rather than removal keeps
                    // the full value in the semantics tree, so a screen reader
                    // still announces the real length, and the text row beneath
                    // the bar lists every length anyway.
                    var overflowed by remember(segment) { mutableStateOf(false) }
                    Text(
                        text = format(segment.lengthU, unitSystem, denominator),
                        style = MeasurementTextStyle.copy(fontSize = 11.sp),
                        color = if (segment.kind == SegmentKind.OFFCUT) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            Color.White
                        },
                        maxLines = 1,
                        // Clip, never ellipsis: "1 8…" is as misleading as "28".
                        overflow = TextOverflow.Clip,
                        onTextLayout = { overflowed = it.hasVisualOverflow },
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .alpha(if (overflowed) 0f else 1f),
                    )
                }
            }
        }
    }
}
