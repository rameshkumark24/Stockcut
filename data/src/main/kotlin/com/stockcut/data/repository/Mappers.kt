package com.stockcut.data.repository

import com.stockcut.data.db.PartEntryEntity
import com.stockcut.data.db.ProjectEntity
import com.stockcut.data.db.StockEntryEntity
import com.stockcut.data.db.StockProfileEntity
import com.stockcut.data.model.CutList
import com.stockcut.data.model.PartEntry
import com.stockcut.data.model.Project
import com.stockcut.data.model.StockEntry
import com.stockcut.data.model.StockProfile
import com.stockcut.optimizer.OptimizeRequest
import com.stockcut.optimizer.PartSpec
import com.stockcut.optimizer.StockSpec
import com.stockcut.units.UnitSystem

/**
 * Room ↔ domain ↔ optimizer mapping. Pure functions, no Android, so they are
 * tested on the JVM.
 *
 * This file is the only place in the app that knows a unit system is stored as
 * text, and the only place that turns saved rows into an [OptimizeRequest].
 */

/**
 * The stored value should always be one of the five enum names — the app writes
 * it and nothing else can. But "should" is doing load-bearing work there: a
 * restored backup from a future version, or a hand-edited database, can produce
 * anything.
 *
 * Falling back to MM is the right failure. Throwing would mean a single bad row
 * makes the projects list uncrashable-by-any-means, and a tradesman would lose
 * access to every saved job because one field was wrong. Millimetres is also
 * the safe default: the length itself is unit-agnostic, so the worst case is
 * that a plan reads in the wrong unit and the user changes it back in Setup.
 * Nothing is corrupted and no measurement moves.
 */
internal fun String.toUnitSystem(): UnitSystem =
    UnitSystem.entries.firstOrNull { it.name == this } ?: UnitSystem.MM

internal fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    name = name,
    unitSystem = unitSystem.toUnitSystem(),
    fractionDenominator = fractionDenominator,
    kerfU = kerfU,
    trimU = trimU,
    isExample = isExample,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    unitSystem = unitSystem.name,
    fractionDenominator = fractionDenominator,
    kerfU = kerfU,
    trimU = trimU,
    isExample = isExample,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun StockEntryEntity.toDomain(): StockEntry = StockEntry(
    id = id,
    lengthU = lengthU,
    quantity = quantity,
    label = label,
    sortOrder = sortOrder,
)

internal fun StockEntry.toEntity(projectId: Long): StockEntryEntity = StockEntryEntity(
    id = id,
    projectId = projectId,
    lengthU = lengthU,
    quantity = quantity,
    label = label,
    sortOrder = sortOrder,
)

internal fun PartEntryEntity.toDomain(): PartEntry = PartEntry(
    id = id,
    lengthU = lengthU,
    quantity = quantity,
    label = label,
    sortOrder = sortOrder,
)

internal fun PartEntry.toEntity(projectId: Long): PartEntryEntity = PartEntryEntity(
    id = id,
    projectId = projectId,
    lengthU = lengthU,
    quantity = quantity,
    label = label,
    sortOrder = sortOrder,
)

internal fun StockProfileEntity.toDomain(): StockProfile = StockProfile(
    id = id,
    name = name,
    lengthU = lengthU,
    lastUsedAt = lastUsedAt,
)

/**
 * Turn a saved job into the optimizer's input.
 *
 * Row ids carry straight through as [StockSpec.id] / [PartSpec.id], which is
 * what lets the result screen point back at the row a piece came from. Do not
 * renumber them.
 *
 * Kerf and trim come from the project, not from global settings: settings supply
 * the DEFAULTS for a new job, and changing them later must not silently alter a
 * plan the user already produced (docs/03 S6).
 */
fun CutList.toOptimizeRequest(): OptimizeRequest = OptimizeRequest(
    stock = stock.map { StockSpec(it.id, it.lengthU, it.quantity, it.label) },
    parts = parts.map { PartSpec(it.id, it.lengthU, it.quantity, it.label) },
    kerfU = project.kerfU,
    trimU = project.trimU,
)
