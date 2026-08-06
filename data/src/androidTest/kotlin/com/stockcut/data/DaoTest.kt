package com.stockcut.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stockcut.data.db.PartEntryEntity
import com.stockcut.data.db.ProjectEntity
import com.stockcut.data.db.SchemaConstraints
import com.stockcut.data.db.StockCutDatabase
import com.stockcut.data.db.StockEntryEntity
import com.stockcut.data.db.StockProfileEntity
import com.stockcut.data.db.duplicateProject
import kotlinx.coroutines.runBlocking
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented — needs a device or emulator:
 *   ./gradlew :data:connectedDebugAndroidTest
 *
 * These cover the things that only SQLite can tell you: that CASCADE actually
 * cascades, and that a schema migration preserves a tradesman's saved jobs.
 */
@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: StockCutDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StockCutDatabase::class.java,
        )
            // The same callback the shipped database uses: CHECK triggers plus
            // foreign keys, without which ON DELETE CASCADE is inert.
            .addCallback(SchemaConstraints.callback)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun newProject(name: String = "Gate"): Long =
        db.projectDao().insert(
            ProjectEntity(
                name = name,
                unitSystem = "MM",
                createdAt = 1_000L,
                updatedAt = 1_000L,
            ),
        )

    @Test
    fun deletingAProjectCascadesToItsStockAndParts() = runBlocking {
        val id = newProject()
        db.stockDao().insert(StockEntryEntity(projectId = id, lengthU = 1_920_000, quantity = -1, sortOrder = 0))
        db.partDao().insert(PartEntryEntity(projectId = id, lengthU = 576_000, quantity = 2, sortOrder = 0))

        assertEquals(1, db.stockDao().countForProject(id))
        assertEquals(2, db.partDao().totalQuantityForProject(id))

        db.projectDao().delete(db.projectDao().byId(id)!!)

        assertNull(db.projectDao().byId(id))
        assertEquals(0, db.stockDao().countForProject(id))
        assertEquals(0, db.partDao().totalQuantityForProject(id))
    }

    @Test
    fun deletingOneProjectLeavesOthersUntouched() = runBlocking {
        val keep = newProject("Keep")
        val drop = newProject("Drop")
        db.partDao().insert(PartEntryEntity(projectId = keep, lengthU = 100, quantity = 3, sortOrder = 0))
        db.partDao().insert(PartEntryEntity(projectId = drop, lengthU = 100, quantity = 4, sortOrder = 0))

        db.projectDao().delete(db.projectDao().byId(drop)!!)

        assertNotNull(db.projectDao().byId(keep))
        assertEquals(3, db.partDao().totalQuantityForProject(keep))
    }

    @Test
    fun partTotalCountsQuantitiesNotRows() = runBlocking {
        // The free-tier limit counts pieces to be cut, not how many rows were typed.
        val id = newProject()
        db.partDao().insert(PartEntryEntity(projectId = id, lengthU = 100, quantity = 7, sortOrder = 0))
        db.partDao().insert(PartEntryEntity(projectId = id, lengthU = 200, quantity = 5, sortOrder = 1))
        assertEquals(12, db.partDao().totalQuantityForProject(id))
    }

    @Test
    fun exampleProjectsAreCountedSeparatelyFromRealOnes() = runBlocking {
        // The seeded example must not consume the free tier's single project slot.
        db.projectDao().insert(
            ProjectEntity(
                name = "Example",
                unitSystem = "MM",
                isExample = true,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        newProject("Real job")
        assertEquals(1, db.projectDao().countReal())
        assertEquals(1, db.projectDao().countExample())
    }

    @Test
    fun labelsSurviveUnicodeAndSqlCharacters() = runBlocking {
        val nasty = "'; DROP TABLE part_entry; -- 🔩 ਪੰਜਾਬੀ"
        val id = newProject()
        db.partDao().insert(
            PartEntryEntity(projectId = id, lengthU = 100, quantity = 1, label = nasty, sortOrder = 0),
        )
        assertEquals(nasty, db.partDao().forProject(id).single().label)
    }

    @Test
    fun duplicatingAProjectCopiesItsStockAndParts() = runBlocking {
        val id = newProject("June gate")
        db.stockDao().insert(StockEntryEntity(projectId = id, lengthU = 1_920_000, quantity = -1, sortOrder = 0))
        db.partDao().insert(PartEntryEntity(projectId = id, lengthU = 576_000, quantity = 2, sortOrder = 0))
        db.partDao().insert(PartEntryEntity(projectId = id, lengthU = 288_000, quantity = 3, sortOrder = 1))

        val copyId = db.duplicateProject(id, "July gate", now = 2_000L)!!

        assertEquals("July gate", db.projectDao().byId(copyId)!!.name)
        assertEquals(1, db.stockDao().countForProject(copyId))
        assertEquals(5, db.partDao().totalQuantityForProject(copyId))
        // The original is untouched — this is a copy, not a move.
        assertEquals(5, db.partDao().totalQuantityForProject(id))
    }

    @Test
    fun duplicatingDetachesTheCopyFromTheOriginal() = runBlocking {
        val id = newProject()
        db.partDao().insert(PartEntryEntity(projectId = id, lengthU = 576_000, quantity = 2, sortOrder = 0))
        val copyId = db.duplicateProject(id, "Copy", now = 2_000L)!!

        db.projectDao().delete(db.projectDao().byId(id)!!)

        // Deleting the source must not cascade into the copy's rows.
        assertNotNull(db.projectDao().byId(copyId))
        assertEquals(2, db.partDao().totalQuantityForProject(copyId))
    }

    @Test
    fun duplicatingTheExampleProducesAnOrdinaryJob() = runBlocking {
        // Otherwise the copy would keep is_example = 1 and stay outside the
        // free tier's project count, which is not what the user asked for.
        val exampleId = db.projectDao().insert(
            ProjectEntity(
                name = "Example: gate frame",
                unitSystem = "MM",
                isExample = true,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        val copyId = db.duplicateProject(exampleId, "My gate", now = 2_000L)!!

        assertEquals(false, db.projectDao().byId(copyId)!!.isExample)
        assertEquals(1, db.projectDao().countReal())
    }

    @Test
    fun duplicatingAMissingProjectReturnsNullRatherThanCrashing() = runBlocking {
        assertNull(db.duplicateProject(sourceId = 9_999L, newName = "Ghost", now = 2_000L))
    }
}

/**
 * The CHECK constraints from docs/05 §1.2, verified at the DAO level as
 * docs/06 §4 requires — a Kotlin-side check would pass whether or not the
 * database enforces anything.
 *
 * A zero or negative length reaching storage is not a cosmetic problem: the
 * optimizer rejects it as InvalidInput, so the row becomes a saved job that can
 * never be optimized, and the user cannot see why.
 */
@RunWith(AndroidJUnit4::class)
class ConstraintTest {

    private lateinit var db: StockCutDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StockCutDatabase::class.java,
        )
            .addCallback(SchemaConstraints.callback)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun newProject(): Long = db.projectDao().insert(
        ProjectEntity(name = "Gate", unitSystem = "MM", createdAt = 1L, updatedAt = 1L),
    )

    @Test
    fun stockLengthMustBePositive() = runBlocking {
        val id = newProject()
        assertFailsWith<SQLiteConstraintException> {
            db.stockDao().insert(StockEntryEntity(projectId = id, lengthU = 0, quantity = -1, sortOrder = 0))
        }
        assertFailsWith<SQLiteConstraintException> {
            db.stockDao().insert(StockEntryEntity(projectId = id, lengthU = -5, quantity = -1, sortOrder = 0))
        }
        assertEquals(0, db.stockDao().countForProject(id))
    }

    @Test
    fun stockQuantityIsPositiveOrExactlyMinusOne() = runBlocking {
        val id = newProject()
        // -1 is "unlimited" and must be accepted.
        db.stockDao().insert(StockEntryEntity(projectId = id, lengthU = 1_920_000, quantity = -1, sortOrder = 0))
        db.stockDao().insert(StockEntryEntity(projectId = id, lengthU = 1_920_000, quantity = 3, sortOrder = 1))
        assertEquals(2, db.stockDao().countForProject(id))

        assertFailsWith<SQLiteConstraintException> {
            db.stockDao().insert(StockEntryEntity(projectId = id, lengthU = 1_920_000, quantity = 0, sortOrder = 2))
        }
        assertFailsWith<SQLiteConstraintException> {
            db.stockDao().insert(StockEntryEntity(projectId = id, lengthU = 1_920_000, quantity = -2, sortOrder = 3))
        }
        assertEquals(2, db.stockDao().countForProject(id))
    }

    @Test
    fun partLengthAndQuantityMustBePositive() = runBlocking {
        val id = newProject()
        assertFailsWith<SQLiteConstraintException> {
            db.partDao().insert(PartEntryEntity(projectId = id, lengthU = 0, quantity = 1, sortOrder = 0))
        }
        assertFailsWith<SQLiteConstraintException> {
            db.partDao().insert(PartEntryEntity(projectId = id, lengthU = 576_000, quantity = 0, sortOrder = 0))
        }
        // There is no "unlimited" number of pieces to cut, so -1 is invalid here.
        assertFailsWith<SQLiteConstraintException> {
            db.partDao().insert(PartEntryEntity(projectId = id, lengthU = 576_000, quantity = -1, sortOrder = 0))
        }
        assertEquals(0, db.partDao().totalQuantityForProject(id))
    }

    @Test
    fun stockProfileLengthMustBePositive() = runBlocking {
        assertFailsWith<SQLiteConstraintException> {
            db.stockProfileDao().insert(
                StockProfileEntity(name = "50x50 SHS", lengthU = 0, lastUsedAt = 1L),
            )
        }
        // assertFailsWith returns the exception; JUnit4 requires a void method.
        Unit
    }

    @Test
    fun anUpdateCannotSneakPastTheConstraint() = runBlocking {
        // The trigger fires BEFORE UPDATE as well as BEFORE INSERT. Without that,
        // a valid row could be edited into an invalid one and stored.
        val id = newProject()
        db.partDao().insert(PartEntryEntity(projectId = id, lengthU = 576_000, quantity = 2, sortOrder = 0))
        val row = db.partDao().forProject(id).single()

        assertFailsWith<SQLiteConstraintException> {
            db.partDao().update(row.copy(lengthU = 0))
        }
        assertFailsWith<SQLiteConstraintException> {
            db.partDao().update(row.copy(quantity = 0))
        }
        assertEquals(576_000L, db.partDao().forProject(id).single().lengthU)
    }
}

/**
 * Migration infrastructure, live from version 1.
 *
 * At v1 there is nothing to migrate yet — this exists so that the FIRST schema
 * change has a working harness and a committed v1 schema to migrate from, rather
 * than someone reaching for fallbackToDestructiveMigration() under time pressure
 * and wiping every saved job on the next update.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StockCutDatabase::class.java,
    )

    @Test
    fun version1SchemaOpensAndAcceptsData() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO project (name, unit_system, fraction_denominator, kerf_u, trim_u, " +
                    "is_example, created_at, updated_at) " +
                    "VALUES ('Gate', 'MM', 16, 960, 0, 0, 1, 1)",
            )
            db.query("SELECT COUNT(*) FROM project").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
