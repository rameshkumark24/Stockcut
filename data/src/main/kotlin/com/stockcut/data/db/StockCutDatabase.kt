package com.stockcut.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        StockEntryEntity::class,
        PartEntryEntity::class,
        StockProfileEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class StockCutDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun stockDao(): StockDao
    abstract fun partDao(): PartDao
    abstract fun stockProfileDao(): StockProfileDao

    companion object {
        const val NAME = "stockcut.db"

        /**
         * NOTE: fallbackToDestructiveMigration() is BANNED (CLAUDE.md rule 6).
         * It silently deletes a tradesman's saved jobs on upgrade. Every schema
         * change ships a written Migration and a migration test instead.
         */
        fun build(context: Context): StockCutDatabase =
            Room.databaseBuilder(context.applicationContext, StockCutDatabase::class.java, NAME)
                .addCallback(
                    object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            // Room enables this per-connection; without it the
                            // ON DELETE CASCADE declarations are inert on older APIs.
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    },
                )
                .build()
    }
}
