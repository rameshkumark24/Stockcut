package com.stockcut.data

import com.stockcut.data.db.ProjectEntity
import com.stockcut.data.model.CutList
import com.stockcut.data.model.PartEntry
import com.stockcut.data.model.Project
import com.stockcut.data.model.StockEntry
import com.stockcut.data.repository.toDomain
import com.stockcut.data.repository.toEntity
import com.stockcut.data.repository.toOptimizeRequest
import com.stockcut.optimizer.OptimizeResult
import com.stockcut.optimizer.StockSpec
import com.stockcut.optimizer.optimize
import com.stockcut.units.UnitSystem
import com.stockcut.units.U_PER_MM
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The Room ↔ domain ↔ optimizer boundary. Pure functions, so JVM tests.
 *
 * The mapping is where a saved job becomes the optimizer's input, which makes
 * it a place a wrong plan can originate without any arithmetic being wrong.
 */
class MapperTest {

    private fun project(
        unitSystem: UnitSystem = UnitSystem.MM,
        kerfU: Long = 960,
        trimU: Long = 0,
    ) = Project(
        id = 1,
        name = "Gate",
        unitSystem = unitSystem,
        fractionDenominator = 16,
        kerfU = kerfU,
        trimU = trimU,
        isExample = false,
        createdAt = 1_000,
        updatedAt = 2_000,
    )

    @Test
    fun `a project survives a round trip through the entity and back`() {
        val original = project(UnitSystem.INCH_FRACTIONAL)
        // toEntity/toDomain are internal to :data, which this test lives in.
        val restored = original.toEntity().toDomain()
        assertEquals(original, restored)
    }

    @Test
    fun `every unit system name round-trips through its stored text`() {
        // If a name ever stops matching, projects silently reopen in millimetres.
        for (system in UnitSystem.entries) {
            val restored = project(system).toEntity().toDomain()
            assertEquals(system, restored.unitSystem, "$system did not survive storage")
        }
    }

    @Test
    fun `an unrecognised unit system falls back to mm instead of throwing`() {
        // A hand-edited or future-version row must not make the projects list
        // impossible to open. Millimetres is safe: the stored length does not
        // move, so the user just changes the unit back in Setup.
        val entity = ProjectEntity(
            id = 1,
            name = "Gate",
            unitSystem = "FURLONGS",
            createdAt = 1,
            updatedAt = 1,
        )
        val restored = entity.toDomain()
        assertEquals(UnitSystem.MM, restored.unitSystem)
        assertEquals("Gate", restored.name, "the rest of the row must be intact")
    }

    @Test
    fun `a cut list becomes an optimize request with kerf and trim from the project`() {
        // Not from global settings — changing a default later must never alter a
        // plan the user already produced (docs/03 S6).
        val cutList = CutList(
            project = project(kerfU = 3 * U_PER_MM, trimU = 50 * U_PER_MM),
            stock = listOf(StockEntry(7, 6_000 * U_PER_MM, StockEntry.UNLIMITED, "6 m", 0)),
            parts = listOf(PartEntry(9, 1_800 * U_PER_MM, 2, "Stile", 0)),
        )

        val request = cutList.toOptimizeRequest()

        assertEquals(3 * U_PER_MM, request.kerfU)
        assertEquals(50 * U_PER_MM, request.trimU)
        assertEquals(1, request.stock.size)
        assertEquals(1, request.parts.size)
    }

    @Test
    fun `row ids carry through, so a plan can point back at the row it came from`() {
        val cutList = CutList(
            project = project(),
            stock = listOf(StockEntry(42, 6_000 * U_PER_MM, StockEntry.UNLIMITED, null, 0)),
            parts = listOf(PartEntry(99, 1_800 * U_PER_MM, 1, null, 0)),
        )

        val request = cutList.toOptimizeRequest()

        assertEquals(42L, request.stock.single().id)
        assertEquals(99L, request.parts.single().id)

        val plan = assertIs<OptimizeResult.Success>(optimize(request)).plan
        assertEquals(42L, plan.bars.single().stockId)
        assertEquals(99L, plan.bars.single().parts.single().partId)
    }

    @Test
    fun `unlimited stock maps onto the optimizer's own sentinel`() {
        // Both are -1. If they ever diverge, "I'll buy as many as needed" becomes
        // an invalid quantity and every such job returns InvalidInput.
        assertEquals(StockSpec.UNLIMITED, StockEntry.UNLIMITED)

        val cutList = CutList(
            project = project(),
            stock = listOf(StockEntry(1, 6_000 * U_PER_MM, StockEntry.UNLIMITED, null, 0)),
            parts = listOf(PartEntry(1, 1_000 * U_PER_MM, 3, null, 0)),
        )
        assertIs<OptimizeResult.Success>(optimize(cutList.toOptimizeRequest()))
    }

    @Test
    fun `a saved job optimizes end to end, in the units it is stored in`() {
        // The example gate frame, assembled the way the app will assemble it.
        val cutList = CutList(
            project = project(kerfU = 3 * U_PER_MM),
            stock = listOf(StockEntry(1, 6_000 * U_PER_MM, StockEntry.UNLIMITED, "40x40 SHS", 0)),
            parts = listOf(
                PartEntry(1, 1_800 * U_PER_MM, 2, "Stile", 0),
                PartEntry(2, 1_200 * U_PER_MM, 2, "Brace", 1),
                PartEntry(3, 900 * U_PER_MM, 2, "Rail", 2),
                PartEntry(4, 850 * U_PER_MM, 3, "Infill bar", 3),
            ),
        )

        val plan = assertIs<OptimizeResult.Success>(optimize(cutList.toOptimizeRequest())).plan

        assertEquals(cutList.totalPieces, plan.bars.sumOf { it.parts.size })
        assertTrue(plan.bars.isNotEmpty())
        // The invariant, on data that came through the mapping.
        for (bar in plan.bars) {
            assertEquals(
                bar.stockLengthU,
                bar.parts.sumOf { it.lengthU } + bar.cutCount * (3 * U_PER_MM) +
                    bar.offcutU + bar.trimU,
            )
        }
    }

    @Test
    fun `total pieces counts quantities, not rows`() {
        val cutList = CutList(
            project = project(),
            stock = emptyList(),
            parts = listOf(
                PartEntry(1, 100, 7, null, 0),
                PartEntry(2, 200, 5, null, 1),
            ),
        )
        assertEquals(12, cutList.totalPieces)
    }
}
