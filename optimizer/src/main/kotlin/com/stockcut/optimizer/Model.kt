package com.stockcut.optimizer

/**
 * Public model for the cut-list optimizer.
 *
 * All lengths are [Long], in internal units of 1/320 mm (see :units).
 * This module has ZERO Android dependencies — it is plain Kotlin/JVM so its
 * tests run in milliseconds without an emulator.
 */

data class StockSpec(
    val id: Long,
    val lengthU: Long,
    /** A positive count, or [UNLIMITED]. */
    val quantity: Int,
    val label: String? = null,
) {
    companion object {
        const val UNLIMITED = -1
    }
}

data class PartSpec(
    val id: Long,
    val lengthU: Long,
    val quantity: Int,
    val label: String? = null,
)

data class OptimizeRequest(
    val stock: List<StockSpec>,
    val parts: List<PartSpec>,
    /** Saw blade width. Consumed by every cut. May be 0 (waterjet, shear). */
    val kerfU: Long,
    /** Removed from the start of each stock length, e.g. a damaged end. */
    val trimU: Long = 0,
)

data class PlacedPart(val partId: Long, val lengthU: Long, val label: String?)

data class CutBar(
    val stockId: Long,
    val stockLengthU: Long,
    val trimU: Long,
    /** In cut order — longest first, which is how people actually work. */
    val parts: List<PlacedPart>,
    /** Saw cuts made on this bar. Each consumes one kerf. */
    val cutCount: Int,
    val offcutU: Long,
)

data class Plan(
    val bars: List<CutBar>,
    val totalStockUsedU: Long,
    val totalPartsU: Long,
    val totalKerfU: Long,
    val totalTrimU: Long,
    val totalOffcutU: Long,
    val wastePercent: Double,
)

sealed interface OptimizeResult {
    /** Every requested part was placed. */
    data class Success(val plan: Plan) : OptimizeResult

    /**
     * One or more parts are longer than any available stock after trim.
     * No plan is produced — a plan that silently omitted parts is the worst
     * possible bug in this app.
     */
    data class Infeasible(
        val impossibleParts: List<PartSpec>,
        val longestUsableU: Long,
    ) : OptimizeResult

    /** Limited stock ran out. The partial plan is still returned and still useful. */
    data class Shortfall(
        val plan: Plan,
        val unplacedParts: List<PartSpec>,
        /** stockId -> how many extra bars of that stock are needed. */
        val additionalStockNeeded: Map<Long, Int>,
    ) : OptimizeResult

    /** A precondition was violated. The UI validates first, so this is a backstop. */
    data class InvalidInput(val reason: String) : OptimizeResult
}

/** Convenience for callers and tests. */
fun OptimizeResult.planOrNull(): Plan? = when (this) {
    is OptimizeResult.Success -> plan
    is OptimizeResult.Shortfall -> plan
    else -> null
}
