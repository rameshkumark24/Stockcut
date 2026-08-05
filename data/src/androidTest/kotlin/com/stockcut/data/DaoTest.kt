package com.stockcut.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stockcut.data.db.PartEntryEntity
import com.stockcut.data.db.ProjectEntity
import com.stockcut.data.db.StockCutDatabase
import com.stockcut.data.db.StockEntryEntity
import kotlinx.coroutines.runBlocking
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
            .addCallback(
                object : androidx.room.RoomDatabase.Callback() {
                    override fun onOpen(connection: SupportSQLiteDatabase) {
                        // Without this, ON DELETE CASCADE is inert.
                        connection.execSQL("PRAGMA foreign_keys = ON")
                    }
                },
            )
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
