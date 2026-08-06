package com.stockcut.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The CHECK constraints from docs/05 §1.2, expressed as triggers.
 *
 * docs/05 §1.3: "Constraints live in the DATABASE, not only in Kotlin — an
 * app-layer check is a convention; a DB constraint is a guarantee."
 *
 * Room's annotations cannot emit a CHECK clause, and SQLite has no
 * ALTER TABLE ... ADD CONSTRAINT, so a BEFORE INSERT / BEFORE UPDATE trigger
 * that calls RAISE(ABORT) is the only way to get a real database-level
 * rejection without hand-writing every CREATE TABLE and losing Room's
 * generated schema. A rejected row raises SQLiteConstraintException, exactly
 * as a CHECK would.
 *
 * These are invisible to Room's schema validation, which compares
 * PRAGMA table_info, foreign_key_list and index_list — none of which cover
 * triggers. So they neither appear in the exported schema JSON nor risk an
 * identity-hash mismatch.
 *
 * They are created in onCreate, so they exist in the database file from its
 * first open. 🔴 A future migration that adds a table must create that table's
 * triggers inside the same Migration — onCreate does not run again for an
 * existing install. CREATE TRIGGER IF NOT EXISTS makes re-running harmless.
 */
object SchemaConstraints {

    /**
     * Rejects a row when [condition] is true, on both INSERT and UPDATE.
     *
     * @param condition SQL over `NEW`, written as the INVALID case.
     */
    private fun reject(
        table: String,
        column: String,
        condition: String,
        message: String,
    ): List<String> = listOf("insert" to "INSERT", "update" to "UPDATE").map { (suffix, event) ->
        """
        CREATE TRIGGER IF NOT EXISTS ${table}_${column}_check_$suffix
        BEFORE $event ON $table
        FOR EACH ROW WHEN $condition
        BEGIN
            SELECT RAISE(ABORT, '$message');
        END
        """.trimIndent()
    }

    /**
     * Every length in this schema is an internal unit count (1/320 mm) and must
     * be positive. Quantity rules differ per table and are spelled out below.
     */
    private val statements: List<String> = buildList {
        addAll(
            reject(
                table = "stock_entry",
                column = "length_u",
                condition = "NEW.length_u <= 0",
                message = "stock_entry.length_u must be greater than 0",
            ),
        )
        // -1 means "unlimited — I'll buy as many as needed". Any other
        // non-positive value is meaningless and the optimizer rejects it, so it
        // must never reach the database in the first place.
        addAll(
            reject(
                table = "stock_entry",
                column = "quantity",
                condition = "NEW.quantity <= 0 AND NEW.quantity != -1",
                message = "stock_entry.quantity must be greater than 0, or -1 for unlimited",
            ),
        )
        addAll(
            reject(
                table = "part_entry",
                column = "length_u",
                condition = "NEW.length_u <= 0",
                message = "part_entry.length_u must be greater than 0",
            ),
        )
        // Unlike stock, there is no "unlimited" number of pieces to cut.
        addAll(
            reject(
                table = "part_entry",
                column = "quantity",
                condition = "NEW.quantity <= 0",
                message = "part_entry.quantity must be greater than 0",
            ),
        )
        addAll(
            reject(
                table = "stock_profile",
                column = "length_u",
                condition = "NEW.length_u <= 0",
                message = "stock_profile.length_u must be greater than 0",
            ),
        )
    }

    /** Applied by [callback] on create, and by any migration that adds a table. */
    fun applyTo(db: SupportSQLiteDatabase) = statements.forEach(db::execSQL)

    /**
     * Install on every builder — production in [StockCutDatabase.build], and in
     * tests. A test database built without this would pass while the shipped one
     * rejects the same row, which is worse than having no constraint at all.
     */
    val callback: RoomDatabase.Callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) = applyTo(db)

        override fun onOpen(db: SupportSQLiteDatabase) {
            // Room enables this per-connection; without it the ON DELETE CASCADE
            // declarations are inert on older APIs.
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }
}
