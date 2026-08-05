package com.stockcut.optimizer

/**
 * 1D cutting-stock optimizer: Best-Fit-Decreasing plus a bounded improvement pass.
 *
 * The problem is NP-hard, so this is a heuristic. It is consistently within a few
 * percent of optimal and fast enough to feel instant, which is the right trade for
 * a phone. Optimality is not promised; CORRECTNESS is.
 *
 * THE KERF MODEL — the physical truth this whole file exists to respect:
 *
 *   usable   = stockLength - trim
 *   cutCount = n      if offcut > 0   (a final cut separates the last part)
 *            = n - 1  if offcut == 0  (the last part ends at the bar's end)
 *   offcut   = usable - sum(parts) - cutCount * kerf
 *
 * THE INVARIANT, which must hold for every bar in every plan, always:
 *
 *   sum(parts) + cutCount * kerf + offcut + trim == stockLength
 *
 * [optimize] verifies this itself before returning. An unbalanced plan is never
 * handed to a caller.
 *
 * DETERMINISM: identical input produces identical output. No Random, no set
 * iteration order, no time-based tie-breaking. Ties break on (length desc, id asc).
 */

private const val MAX_PARTS = 1_000
private const val MAX_IMPROVEMENT_ITERATIONS = 50

fun optimize(request: OptimizeRequest): OptimizeResult {
    validate(request)?.let { return it }

    val trim = request.trimU
    val kerf = request.kerfU

    // Stock types that can actually hold anything, longest first.
    val stockTypes = request.stock
        .map { StockType(it.id, it.lengthU, it.lengthU - trim, it.quantity) }
        .filter { it.usableU > 0 }
        .sortedWith(compareByDescending<StockType> { it.usableU }.thenBy { it.id })

    if (stockTypes.isEmpty()) {
        return OptimizeResult.InvalidInput("Trim is longer than every stock length.")
    }

    val longestUsable = stockTypes.first().usableU
    // Feasibility is "can this part be cut alone from some stock", NOT "is it
    // shorter than the longest bar". A 5999 part will not come off a 6000 bar
    // with a 3mm blade: you would need 6002mm of steel. Comparing lengths alone
    // would let that part through and surface later as an unplaceable item.
    val impossible = request.parts.filter { part ->
        stockTypes.none { type -> cutsAndOffcut(type.usableU, part.lengthU, 1, kerf) != null }
    }
    if (impossible.isNotEmpty()) {
        return OptimizeResult.Infeasible(impossible, longestUsable)
    }

    // Expand quantities into individual items, longest first.
    val items = ArrayList<Item>()
    for (part in request.parts) {
        repeat(part.quantity) { i -> items.add(Item(part.id, part.lengthU, part.label, i)) }
    }
    items.sortWith(
        compareByDescending<Item> { it.lengthU }.thenBy { it.partId }.thenBy { it.seq },
    )

    val remaining = HashMap<Long, Int>()
    stockTypes.forEach { remaining[it.id] = it.quantity }

    val bars = ArrayList<WorkingBar>()
    val unplaced = ArrayList<Item>()

    for (item in items) {
        if (placeIntoExistingBar(bars, item, kerf)) continue
        val opened = openNewBar(stockTypes, remaining, item, kerf)
        if (opened == null) {
            unplaced.add(item)
        } else {
            opened.add(item)
            bars.add(opened)
        }
    }

    improve(bars, kerf)

    // Longest cut first on each bar — how people actually work down a length.
    bars.forEach { bar ->
        bar.items.sortWith(compareByDescending<Item> { it.lengthU }.thenBy { it.partId }.thenBy { it.seq })
    }

    val plan = buildPlan(bars, kerf, trim)
    verify(plan, kerf)?.let { return it }

    if (unplaced.isEmpty()) return OptimizeResult.Success(plan)

    val conservation = items.size - (plan.bars.sumOf { it.parts.size } + unplaced.size)
    if (conservation != 0) {
        return OptimizeResult.InvalidInput("Internal error: $conservation parts lost during planning.")
    }

    return OptimizeResult.Shortfall(
        plan = plan,
        unplacedParts = collapseToSpecs(unplaced),
        additionalStockNeeded = estimateAdditionalStock(unplaced, stockTypes, kerf),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Internals
// ─────────────────────────────────────────────────────────────────────────────

private class Item(
    val partId: Long,
    val lengthU: Long,
    val label: String?,
    val seq: Int,
)

private class StockType(
    val id: Long,
    val lengthU: Long,
    val usableU: Long,
    val quantity: Int,
)

private class WorkingBar(val stockId: Long, val stockLengthU: Long, val usableU: Long) {
    val items = ArrayList<Item>()
    var sumU = 0L
        private set

    fun add(item: Item) {
        items.add(item)
        sumU += item.lengthU
    }

    /** Identity removal — Item is intentionally not a data class, so each instance is unique. */
    fun remove(item: Item) {
        if (items.remove(item)) sumU -= item.lengthU
    }

    fun clear() {
        items.clear()
        sumU = 0L
    }
}

/**
 * Cuts and offcut for a bar holding [n] parts totalling [sum], or null if that
 * combination does not physically fit.
 */
private fun cutsAndOffcut(usable: Long, sum: Long, n: Int, kerf: Long): Pair<Int, Long>? {
    if (n == 0) return 0 to usable
    // Perfect fit: the last piece runs to the end of the bar, so no final cut.
    if (usable - sum - (n - 1) * kerf == 0L) return (n - 1) to 0L
    val offcut = usable - sum - n * kerf
    return if (offcut >= 0L) n to offcut else null
}

/** Best fit: the open bar left with the smallest offcut. Ties break on bar order. */
private fun placeIntoExistingBar(bars: List<WorkingBar>, item: Item, kerf: Long): Boolean {
    var best: WorkingBar? = null
    var bestOffcut = Long.MAX_VALUE
    for (bar in bars) {
        val fit = cutsAndOffcut(bar.usableU, bar.sumU + item.lengthU, bar.items.size + 1, kerf)
            ?: continue
        if (fit.second < bestOffcut) {
            bestOffcut = fit.second
            best = bar
        }
    }
    best?.add(item)
    return best != null
}

/**
 * Opens the stock size that leaves the least offcut for this part — i.e. the
 * smallest length that fits it. Later parts fill the bar via best-fit.
 */
private fun openNewBar(
    stockTypes: List<StockType>,
    remaining: MutableMap<Long, Int>,
    item: Item,
    kerf: Long,
): WorkingBar? {
    var best: StockType? = null
    var bestOffcut = Long.MAX_VALUE
    for (type in stockTypes) {
        val left = remaining[type.id] ?: 0
        if (left == 0) continue
        val fit = cutsAndOffcut(type.usableU, item.lengthU, 1, kerf) ?: continue
        if (fit.second < bestOffcut) {
            bestOffcut = fit.second
            best = type
        }
    }
    if (best == null) return null
    val left = remaining[best.id] ?: 0
    if (left != StockSpec.UNLIMITED) remaining[best.id] = left - 1
    return WorkingBar(best.id, best.lengthU, best.usableU)
}

/**
 * Bounded improvement: repeatedly try to empty the least-filled bar by relocating
 * its parts into the others. Removing a whole bar is the only improvement worth
 * chasing — it is a length of steel the user does not have to buy.
 */
private fun improve(bars: MutableList<WorkingBar>, kerf: Long) {
    var iterations = 0
    while (iterations++ < MAX_IMPROVEMENT_ITERATIONS && bars.size > 1) {
        val victim = bars.minByOrNull { it.sumU } ?: return
        val others = bars.filter { it !== victim }
        val moved = ArrayList<Pair<WorkingBar, Item>>()

        var allFit = true
        for (item in victim.items.sortedByDescending { it.lengthU }) {
            var target: WorkingBar? = null
            var bestOffcut = Long.MAX_VALUE
            for (bar in others) {
                val fit = cutsAndOffcut(bar.usableU, bar.sumU + item.lengthU, bar.items.size + 1, kerf)
                    ?: continue
                if (fit.second < bestOffcut) {
                    bestOffcut = fit.second
                    target = bar
                }
            }
            if (target == null) {
                allFit = false
                break
            }
            target.add(item)
            moved.add(target to item)
        }

        if (allFit) {
            victim.clear()
            bars.remove(victim)
        } else {
            // Roll back cleanly. A half-moved state would duplicate parts across
            // bars, which the conservation property test would catch — but the
            // user would have already cut to a wrong plan.
            for ((bar, item) in moved.asReversed()) bar.remove(item)
            return
        }
    }
}

private fun buildPlan(bars: List<WorkingBar>, kerf: Long, trim: Long): Plan {
    val cutBars = bars.map { bar ->
        val (cuts, offcut) = cutsAndOffcut(bar.usableU, bar.sumU, bar.items.size, kerf)
            ?: error("over-packed bar reached buildPlan")
        CutBar(
            stockId = bar.stockId,
            stockLengthU = bar.stockLengthU,
            trimU = trim,
            parts = bar.items.map { PlacedPart(it.partId, it.lengthU, it.label) },
            cutCount = cuts,
            offcutU = offcut,
        )
    }
    val stockUsed = cutBars.sumOf { it.stockLengthU }
    val partsTotal = cutBars.sumOf { bar -> bar.parts.sumOf { it.lengthU } }
    val kerfTotal = cutBars.sumOf { it.cutCount * kerf }
    val trimTotal = cutBars.size * trim
    val offcutTotal = cutBars.sumOf { it.offcutU }
    val waste = if (stockUsed == 0L) 0.0
    else (kerfTotal + trimTotal + offcutTotal).toDouble() / stockUsed.toDouble() * 100.0

    return Plan(
        bars = cutBars,
        totalStockUsedU = stockUsed,
        totalPartsU = partsTotal,
        totalKerfU = kerfTotal,
        totalTrimU = trimTotal,
        totalOffcutU = offcutTotal,
        wastePercent = waste,
    )
}

/** The optimizer checks its own invariant. A plan that does not balance never escapes. */
private fun verify(plan: Plan, kerf: Long): OptimizeResult.InvalidInput? {
    for ((index, bar) in plan.bars.withIndex()) {
        val sum = bar.parts.sumOf { it.lengthU } + bar.cutCount * kerf + bar.offcutU + bar.trimU
        if (sum != bar.stockLengthU) {
            return OptimizeResult.InvalidInput(
                "Internal error: bar $index does not balance " +
                    "($sum vs ${bar.stockLengthU}).",
            )
        }
        if (bar.offcutU < 0L) {
            return OptimizeResult.InvalidInput("Internal error: bar $index has a negative offcut.")
        }
        if (bar.parts.isEmpty()) {
            return OptimizeResult.InvalidInput("Internal error: bar $index is empty.")
        }
    }
    val totals = plan.totalPartsU + plan.totalKerfU + plan.totalTrimU + plan.totalOffcutU
    if (totals != plan.totalStockUsedU) {
        return OptimizeResult.InvalidInput("Internal error: totals do not balance.")
    }
    return null
}

private fun collapseToSpecs(items: List<Item>): List<PartSpec> =
    items.groupBy { it.partId to it.lengthU }
        .map { (key, group) ->
            PartSpec(key.first, key.second, group.size, group.first().label)
        }
        .sortedWith(compareByDescending<PartSpec> { it.lengthU }.thenBy { it.id })

/** How many more bars would clear the backlog, packed into the longest stock type. */
private fun estimateAdditionalStock(
    unplaced: List<Item>,
    stockTypes: List<StockType>,
    kerf: Long,
): Map<Long, Int> {
    val type = stockTypes.first() // longest usable; already sorted
    val bars = ArrayList<WorkingBar>()
    for (item in unplaced.sortedByDescending { it.lengthU }) {
        if (placeIntoExistingBar(bars, item, kerf)) continue
        if (cutsAndOffcut(type.usableU, item.lengthU, 1, kerf) == null) continue
        val bar = WorkingBar(type.id, type.lengthU, type.usableU)
        bar.add(item)
        bars.add(bar)
    }
    return if (bars.isEmpty()) emptyMap() else mapOf(type.id to bars.size)
}

private fun validate(request: OptimizeRequest): OptimizeResult.InvalidInput? {
    if (request.parts.isEmpty()) return OptimizeResult.InvalidInput("No parts to cut.")
    if (request.stock.isEmpty()) return OptimizeResult.InvalidInput("No stock lengths.")
    if (request.kerfU < 0L) return OptimizeResult.InvalidInput("Kerf can't be negative.")
    if (request.trimU < 0L) return OptimizeResult.InvalidInput("Trim can't be negative.")

    if (request.stock.map { it.id }.toSet().size != request.stock.size) {
        return OptimizeResult.InvalidInput("Duplicate stock id.")
    }
    if (request.parts.map { it.id }.toSet().size != request.parts.size) {
        return OptimizeResult.InvalidInput("Duplicate part id.")
    }
    for (s in request.stock) {
        if (s.lengthU <= 0L) return OptimizeResult.InvalidInput("Stock length must be more than 0.")
        if (s.quantity == 0 || s.quantity < StockSpec.UNLIMITED) {
            return OptimizeResult.InvalidInput("Stock quantity must be positive or unlimited.")
        }
    }
    var total = 0L
    for (p in request.parts) {
        if (p.lengthU <= 0L) return OptimizeResult.InvalidInput("Part length must be more than 0.")
        if (p.quantity <= 0) return OptimizeResult.InvalidInput("Part quantity must be more than 0.")
        total += p.quantity
        if (total > MAX_PARTS) {
            return OptimizeResult.InvalidInput("Too many parts. The limit is $MAX_PARTS.")
        }
    }
    return null
}
