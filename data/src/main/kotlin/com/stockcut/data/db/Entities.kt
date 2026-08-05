package com.stockcut.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local schema. Three tables, all on-device. No server, no sync, no user table.
 *
 * EVERY length column is INTEGER, in internal units of 1/320 mm (see :units).
 * There is no REAL column anywhere in this schema and there never will be —
 * floating point does not appear in the persistence layer.
 *
 * Constraints live in the DATABASE, not only in Kotlin: NOT NULL, CHECK, and
 * ON DELETE CASCADE. An app-layer check is a convention; a DB constraint is a
 * guarantee.
 */

@Entity(tableName = "project")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "unit_system") val unitSystem: String,
    @ColumnInfo(name = "fraction_denominator", defaultValue = "16") val fractionDenominator: Int = 16,
    /** 960 U = 3 mm, a typical blade. */
    @ColumnInfo(name = "kerf_u", defaultValue = "960") val kerfU: Long = 960,
    @ColumnInfo(name = "trim_u", defaultValue = "0") val trimU: Long = 0,
    @ColumnInfo(name = "is_example", defaultValue = "0") val isExample: Boolean = false,
    /** Epoch millis, UTC. Converted at display time only. */
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "stock_entry",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("project_id")],
)
data class StockEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "project_id") val projectId: Long,
    @ColumnInfo(name = "length_u") val lengthU: Long,
    /** A positive count, or -1 for unlimited ("I'll buy as many as needed"). */
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "label") val label: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(
    tableName = "part_entry",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("project_id")],
)
data class PartEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "project_id") val projectId: Long,
    @ColumnInfo(name = "length_u") val lengthU: Long,
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "label") val label: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

/** Reusable stock sizes, so "my usual 6 m length" is not retyped every job. */
@Entity(tableName = "stock_profile")
data class StockProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "length_u") val lengthU: Long,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long,
)
