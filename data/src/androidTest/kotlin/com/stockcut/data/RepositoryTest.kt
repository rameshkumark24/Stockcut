package com.stockcut.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stockcut.data.db.SchemaConstraints
import com.stockcut.data.db.StockCutDatabase
import com.stockcut.data.model.StockEntry
import com.stockcut.data.repository.CutListRepository
import com.stockcut.data.repository.ProjectRepository
import com.stockcut.data.repository.toOptimizeRequest
import com.stockcut.data.seed.ExampleProject
import com.stockcut.optimizer.OptimizeResult
import com.stockcut.optimizer.optimize
import com.stockcut.units.UnitSystem
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The repository layer against real SQLite.
 *
 * The mappers are covered by fast JVM tests; what needs a database is the part
 * that talks to one — that seeding runs once and not once per launch, that
 * editing a job re-sorts the list, and that a saved job comes back out in a
 * shape the optimizer accepts.
 */
@RunWith(AndroidJUnit4::class)
class RepositoryTest {

    private lateinit var db: StockCutDatabase
    private lateinit var projects: ProjectRepository
    private lateinit var cutLists: CutListRepository

    /** Injected clock, so "did updatedAt move" is a real assertion, not a race. */
    private var clock = 1_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StockCutDatabase::class.java,
        )
            .addCallback(SchemaConstraints.callback)
            .allowMainThreadQueries()
            .build()
        projects = ProjectRepository(db) { clock }
        cutLists = CutListRepository(db, projects)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun newProject(name: String = "Gate"): Long = projects.createProject(
        name = name,
        unitSystem = UnitSystem.MM,
        fractionDenominator = 16,
        kerfU = 960,
    )

    // ── Projects ─────────────────────────────────────────────────────────────

    @Test
    fun aCreatedProjectComesBackAsADomainObject() = runBlocking {
        val id = newProject("June gate")
        val project = projects.getProject(id)

        assertNotNull(project)
        assertEquals("June gate", project!!.name)
        // Typed, not the raw String the column holds.
        assertEquals(UnitSystem.MM, project.unitSystem)
        assertEquals(1_000L, project.createdAt)
    }

    @Test
    fun theUnitSystemSurvivesStorageAsAnEnum() = runBlocking {
        val id = projects.createProject(
            name = "Deck",
            unitSystem = UnitSystem.INCH_FRACTIONAL,
            fractionDenominator = 32,
            kerfU = 1016,
        )
        val project = projects.getProject(id)!!
        assertEquals(UnitSystem.INCH_FRACTIONAL, project.unitSystem)
        assertEquals(32, project.fractionDenominator)
    }

    @Test
    fun editingAJobMovesItToTheTopOfTheList() = runBlocking {
        val older = newProject("Older")
        clock = 2_000L
        val newer = newProject("Newer")

        // Touching the older job must re-sort it above the newer one.
        clock = 3_000L
        cutLists.addPart(older, lengthU = 576_000, quantity = 2)

        assertEquals(3_000L, projects.getProject(older)!!.updatedAt)
        assertTrue(projects.getProject(older)!!.updatedAt > projects.getProject(newer)!!.updatedAt)
    }

    @Test
    fun deletingAProjectTakesItsContentsWithIt() = runBlocking {
        val id = newProject()
        cutLists.addStock(id, lengthU = 1_920_000)
        cutLists.addPart(id, lengthU = 576_000, quantity = 2)

        projects.deleteProject(id)

        assertNull(projects.getProject(id))
        assertEquals(0, cutLists.stockCount(id))
        assertEquals(0, cutLists.totalPieces(id))
    }

    @Test
    fun deletingAProjectThatIsAlreadyGoneIsNotAnError() = runBlocking {
        projects.deleteProject(9_999L)
        assertNull(projects.getProject(9_999L))
    }

    // ── Seeding ──────────────────────────────────────────────────────────────

    @Test
    fun theExampleIsSeededOnceAndNotAgainOnEveryLaunch() = runBlocking {
        val first = projects.seedExampleIfNeeded(alreadyDeleted = false)
        assertNotNull(first)

        // Four more cold starts.
        repeat(4) { assertNull(projects.seedExampleIfNeeded(alreadyDeleted = false)) }

        assertEquals(1, db.projectDao().countExample())
    }

    @Test
    fun theExampleNeverComesBackOnceDeleted() = runBlocking {
        val id = projects.seedExampleIfNeeded(alreadyDeleted = false)!!
        projects.deleteProject(id)

        // The row is gone, so only the DataStore flag can prevent a re-seed.
        assertNull(projects.seedExampleIfNeeded(alreadyDeleted = true))
        assertEquals(0, db.projectDao().countExample())
    }

    @Test
    fun theSeededExampleDoesNotConsumeTheFreeTiersOneProjectSlot() = runBlocking {
        projects.seedExampleIfNeeded(alreadyDeleted = false)
        assertEquals(0, projects.realProjectCount())

        newProject("A real job")
        assertEquals(1, projects.realProjectCount())
    }

    @Test
    fun theSeededExampleActuallyOptimizes() = runBlocking {
        // The first thing a new user taps. If it produces no plan, the app has
        // failed before they typed anything.
        val id = projects.seedExampleIfNeeded(alreadyDeleted = false)!!
        val cutList = cutLists.loadCutList(id)!!

        assertEquals(ExampleProject.totalPieces, cutList.totalPieces)
        val plan = assertIs<OptimizeResult.Success>(optimize(cutList.toOptimizeRequest())).plan
        assertEquals(ExampleProject.totalPieces, plan.bars.sumOf { it.parts.size })
        assertTrue(plan.totalOffcutU > 0)
    }

    // ── Cut list ─────────────────────────────────────────────────────────────

    @Test
    fun loadCutListPairsAProjectWithItsOwnContents() = runBlocking {
        val mine = newProject("Mine")
        val theirs = newProject("Theirs")
        cutLists.addPart(mine, lengthU = 576_000, quantity = 2)
        cutLists.addPart(theirs, lengthU = 100_000, quantity = 9)

        val loaded = cutLists.loadCutList(mine)!!
        assertEquals("Mine", loaded.project.name)
        assertEquals(2, loaded.totalPieces, "loaded another project's parts")
    }

    @Test
    fun loadCutListReturnsNullForAProjectThatIsGone() = runBlocking {
        assertNull(cutLists.loadCutList(9_999L))
    }

    @Test
    fun sortOrderIsAssignedSoRowsKeepTheOrderTheyWereEnteredIn() = runBlocking {
        val id = newProject()
        cutLists.addPart(id, lengthU = 100_000, quantity = 1, label = "first")
        cutLists.addPart(id, lengthU = 200_000, quantity = 1, label = "second")
        cutLists.addPart(id, lengthU = 300_000, quantity = 1, label = "third")

        val parts = cutLists.loadCutList(id)!!.parts
        assertEquals(listOf("first", "second", "third"), parts.map { it.label })
        assertEquals(listOf(0, 1, 2), parts.map { it.sortOrder })
    }

    @Test
    fun stockDefaultsToUnlimitedBecauseThatIsWhatMostPeopleMean() = runBlocking {
        val id = newProject()
        cutLists.addStock(id, lengthU = 1_920_000)
        assertTrue(cutLists.loadCutList(id)!!.stock.single().isUnlimited)
    }

    @Test
    fun undoReinsertsARowWithoutReusingItsOldId() = runBlocking {
        // The undo window holds the row in memory, not in the database, so undo
        // is an insert. Reusing the old primary key would be how two parts end
        // up pointing at the same thing.
        val id = newProject()
        cutLists.addPart(id, lengthU = 576_000, quantity = 2, label = "Stile")
        val original = cutLists.loadCutList(id)!!.parts.single()

        cutLists.deletePart(id, original)
        assertEquals(0, cutLists.totalPieces(id))

        val restoredId = cutLists.restorePart(id, original)
        val restored = cutLists.loadCutList(id)!!.parts.single()

        assertNotEquals(original.id, restoredId)
        assertEquals(original.lengthU, restored.lengthU)
        assertEquals(original.quantity, restored.quantity)
        assertEquals(original.label, restored.label)
    }

    @Test
    fun aDuplicatedJobIsIndependentOfTheOriginal() = runBlocking {
        val source = newProject("June gate")
        cutLists.addStock(source, lengthU = 1_920_000)
        cutLists.addPart(source, lengthU = 576_000, quantity = 2)

        val copyId = projects.duplicate(source, "July gate")!!
        projects.deleteProject(source)

        val copy = cutLists.loadCutList(copyId)!!
        assertEquals("July gate", copy.project.name)
        assertEquals(2, copy.totalPieces)
        assertEquals(1, copy.stock.size)
    }

    @Test
    fun aQuantityOfZeroIsRejectedByTheDatabaseNotJustByKotlin() = runBlocking {
        // The repository is not a place to re-implement the CHECK constraints;
        // this proves they still apply when writes go through it.
        val id = newProject()
        var threw = false
        try {
            cutLists.addPart(id, lengthU = 576_000, quantity = 0)
        } catch (expected: android.database.sqlite.SQLiteConstraintException) {
            threw = true
        }
        assertTrue(threw, "a zero-quantity part reached storage")
        assertEquals(0, cutLists.totalPieces(id))
    }

    @Test
    fun unlimitedIsTheSameSentinelTheOptimizerUses() {
        assertEquals(com.stockcut.optimizer.StockSpec.UNLIMITED, StockEntry.UNLIMITED)
    }
}
