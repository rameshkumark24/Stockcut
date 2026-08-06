package com.stockcut.data.model

import com.stockcut.units.UnitSystem

/**
 * Domain models — what the rest of the app sees.
 *
 * These exist so `:app` never touches Room. docs/07 W2 lists "Repositories —
 * Room ↔ domain mapping" as a deliverable; these types are the domain half of
 * that mapping.
 *
 * The difference that earns their keep is [Project.unitSystem]. The database
 * column is TEXT, because SQLite has no enum. Every screen that read the entity
 * directly would be handling a raw String and would have to decide, again, what
 * to do when it is not one of the five valid values. Here it is decided once,
 * at the boundary, and the rest of the app gets a typed enum it cannot get
 * wrong.
 *
 * Every length is still a Long in internal units (1/320 mm). Crossing into the
 * domain layer is not an excuse to start formatting.
 */

data class Project(
    val id: Long,
    val name: String,
    val unitSystem: UnitSystem,
    val fractionDenominator: Int,
    val kerfU: Long,
    val trimU: Long,
    val isExample: Boolean,
    /** Epoch millis, UTC. Converted at display time only. */
    val createdAt: Long,
    val updatedAt: Long,
)

data class StockEntry(
    val id: Long,
    val lengthU: Long,
    /** A positive count, or [UNLIMITED]. */
    val quantity: Int,
    val label: String?,
    val sortOrder: Int,
) {
    val isUnlimited: Boolean get() = quantity == UNLIMITED

    companion object {
        /** Matches StockSpec.UNLIMITED in :optimizer. Both are -1, deliberately. */
        const val UNLIMITED = -1
    }
}

data class PartEntry(
    val id: Long,
    val lengthU: Long,
    val quantity: Int,
    val label: String?,
    val sortOrder: Int,
)

data class StockProfile(
    val id: Long,
    val name: String,
    val lengthU: Long,
    val lastUsedAt: Long,
)

/**
 * A project with everything needed to optimize it. Assembled by the repository
 * in one pass so a screen cannot render a project against another project's
 * parts.
 */
data class CutList(
    val project: Project,
    val stock: List<StockEntry>,
    val parts: List<PartEntry>,
) {
    /** Total pieces to cut — what the free-tier limit counts, not the row count. */
    val totalPieces: Int get() = parts.sumOf { it.quantity }
}

/**
 * One row of the projects list (docs/03 S1: name, part count, last modified).
 *
 * Note what is NOT here: last known waste %. The S1 spec asks for it, but
 * nothing stores it — it exists only in an OptimizeResult, which is not
 * persisted. Adding it means a schema column, and it cannot be a REAL one
 * (docs/05 §1.3 bans floating point in the schema), so it would have to be
 * hundredths of a percent in an INTEGER plus a migration. Deliberately left
 * out rather than faked.
 */
data class ProjectSummary(
    val project: Project,
    val totalPieces: Int,
)
