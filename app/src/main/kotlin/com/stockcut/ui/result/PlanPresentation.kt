package com.stockcut.ui.result

import com.stockcut.optimizer.CutBar
import com.stockcut.optimizer.Plan

/**
 * Turning a [Plan] into what S4 draws. Pure Kotlin, so it is JVM-tested.
 *
 * No Android, no Compose, no formatting — this decides WHAT to show, and the
 * composables decide how.
 */

/**
 * A group of bars cut identically, shown as one card.
 *
 * docs/03 S4: "Identical bars are collapsed: '×4 identical bars' — a plan with
 * 40 bars must not be 40 cards." Scrolling forty identical cards to check one is
 * how a user loses confidence in a plan they otherwise trust.
 */
data class BarGroup(
    val bar: CutBar,
    val count: Int,
    /** 1-based index of the first bar in this group, for "Bar 3". */
    val firstIndex: Int,
)

/**
 * Collapses consecutive identical bars.
 *
 * Two bars are identical when they come off the same stock, hold the same part
 * lengths in the same order, and leave the same offcut. Part IDs are ignored
 * deliberately: two 1200 mm pieces cut from different rows are the same cut to
 * the person at the saw, and treating them as different would defeat the whole
 * point of collapsing.
 *
 * Only CONSECUTIVE runs are collapsed. The optimizer emits bars in the order it
 * opened them, and reordering them to group more aggressively would change the
 * sequence someone is meant to work through.
 */
fun Plan.groupIdenticalBars(): List<BarGroup> {
    if (bars.isEmpty()) return emptyList()

    val groups = ArrayList<BarGroup>()
    var runStart = 0
    var count = 1

    for (i in 1..bars.size) {
        val same = i < bars.size && bars[i].isSameCutAs(bars[runStart])
        if (same) {
            count++
        } else {
            groups.add(BarGroup(bars[runStart], count, runStart + 1))
            runStart = i
            count = 1
        }
    }
    return groups
}

private fun CutBar.isSameCutAs(other: CutBar): Boolean =
    stockId == other.stockId &&
        stockLengthU == other.stockLengthU &&
        trimU == other.trimU &&
        cutCount == other.cutCount &&
        offcutU == other.offcutU &&
        parts.size == other.parts.size &&
        parts.indices.all { parts[it].lengthU == other.parts[it].lengthU }

/** Waste bands from docs/03 S4. Colour is never the only signal — each has a label. */
enum class WasteBand(val label: String) {
    GOOD("Low waste"),
    FAIR("Some waste"),
    POOR("High waste"),
}

/**
 * Green < 5%, amber 5–15%, red > 15% (docs/03 S4).
 *
 * Takes a Double because wastePercent is one of exactly two places floating
 * point is permitted (CLAUDE.md rule 2) — it is a display figure, never an input
 * to a cut.
 */
fun wasteBand(wastePercent: Double): WasteBand = when {
    wastePercent < 5.0 -> WasteBand.GOOD
    wastePercent <= 15.0 -> WasteBand.FAIR
    else -> WasteBand.POOR
}

/**
 * Proportional widths for one bar's segments, as fractions of the stock length.
 *
 * Returned as Float because these are drawing coordinates — the other place
 * CLAUDE.md rule 2 allows floating point. The arithmetic that decides the CUT is
 * all Long; this only decides how many pixels wide to paint it.
 *
 * Order matches the physical bar left to right: trim, then each part in cut
 * order, each followed by its kerf, then the offcut.
 */
fun CutBar.segmentWeights(kerfU: Long): List<Segment> {
    val total = stockLengthU.toDouble()
    if (total <= 0.0) return emptyList()

    val segments = ArrayList<Segment>()
    if (trimU > 0) segments.add(Segment(SegmentKind.TRIM, (trimU / total).toFloat(), trimU))

    var kerfsLeft = cutCount
    for (part in parts) {
        segments.add(Segment(SegmentKind.PART, (part.lengthU / total).toFloat(), part.lengthU, part.label))
        if (kerfsLeft > 0 && kerfU > 0) {
            segments.add(Segment(SegmentKind.KERF, (kerfU / total).toFloat(), kerfU))
            kerfsLeft--
        }
    }
    if (offcutU > 0) segments.add(Segment(SegmentKind.OFFCUT, (offcutU / total).toFloat(), offcutU))
    return segments
}

enum class SegmentKind { TRIM, PART, KERF, OFFCUT }

data class Segment(
    val kind: SegmentKind,
    val weight: Float,
    /** The real length, so a label can be rendered from a Long rather than a Float. */
    val lengthU: Long,
    val label: String? = null,
)
