package com.stockcut.data.seed

import com.stockcut.units.U_PER_MM

/**
 * The first-run example job.
 *
 * There is no onboarding carousel. On first launch the projects list holds one
 * read-only example; the user taps it, sees a finished cut plan in three seconds,
 * and understands the whole product without reading a word. It can be deleted and
 * never comes back (docs/03-app-flow.md, First-run experience).
 *
 * A gate frame is the right example: every fabricator has made one, the numbers
 * are recognisable, and it produces a plan with visible offcut rather than a
 * suspiciously perfect fit.
 */
object ExampleProject {

    const val NAME = "Example: gate frame"

    /** 40x40 box section, bought in 6 m lengths. */
    val stock: List<SeedStock> = listOf(
        SeedStock(lengthMm = 6_000, quantity = UNLIMITED, label = "40x40 SHS 6 m"),
    )

    val parts: List<SeedPart> = listOf(
        SeedPart(lengthMm = 1_800, quantity = 2, label = "Stile"),
        SeedPart(lengthMm = 1_200, quantity = 2, label = "Brace"),
        SeedPart(lengthMm = 900, quantity = 2, label = "Rail"),
        SeedPart(lengthMm = 850, quantity = 3, label = "Infill bar"),
    )

    /** 3 mm blade — a typical metal-cutting chop saw. */
    const val KERF_MM = 3L

    const val UNLIMITED = -1

    data class SeedStock(val lengthMm: Long, val quantity: Int, val label: String?) {
        val lengthU: Long get() = lengthMm * U_PER_MM
    }

    data class SeedPart(val lengthMm: Long, val quantity: Int, val label: String?) {
        val lengthU: Long get() = lengthMm * U_PER_MM
    }

    val kerfU: Long get() = KERF_MM * U_PER_MM

    /** Total pieces to cut — must stay inside the free tier so it always opens. */
    val totalPieces: Int get() = parts.sumOf { it.quantity }
}
