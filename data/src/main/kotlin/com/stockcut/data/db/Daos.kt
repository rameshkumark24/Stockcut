package com.stockcut.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM project ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM project WHERE id = :id")
    suspend fun byId(id: Long): ProjectEntity?

    @Query("SELECT COUNT(*) FROM project WHERE is_example = 0")
    suspend fun countReal(): Int

    @Query("SELECT COUNT(*) FROM project WHERE is_example = 1")
    suspend fun countExample(): Int

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    /** Cascades to stock_entry and part_entry via the foreign keys. */
    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("UPDATE project SET updated_at = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)
}

@Dao
interface StockDao {

    @Query("SELECT * FROM stock_entry WHERE project_id = :projectId ORDER BY sort_order")
    fun observeForProject(projectId: Long): Flow<List<StockEntryEntity>>

    @Query("SELECT * FROM stock_entry WHERE project_id = :projectId ORDER BY sort_order")
    suspend fun forProject(projectId: Long): List<StockEntryEntity>

    @Query("SELECT COUNT(*) FROM stock_entry WHERE project_id = :projectId")
    suspend fun countForProject(projectId: Long): Int

    @Insert
    suspend fun insert(entry: StockEntryEntity): Long

    @Update
    suspend fun update(entry: StockEntryEntity)

    @Delete
    suspend fun delete(entry: StockEntryEntity)

    @Query("DELETE FROM stock_entry WHERE project_id = :projectId")
    suspend fun clearProject(projectId: Long)
}

@Dao
interface PartDao {

    @Query("SELECT * FROM part_entry WHERE project_id = :projectId ORDER BY sort_order")
    fun observeForProject(projectId: Long): Flow<List<PartEntryEntity>>

    @Query("SELECT * FROM part_entry WHERE project_id = :projectId ORDER BY sort_order")
    suspend fun forProject(projectId: Long): List<PartEntryEntity>

    /** Sum of quantities, not row count — the free-tier limit counts pieces. */
    @Query("SELECT COALESCE(SUM(quantity), 0) FROM part_entry WHERE project_id = :projectId")
    suspend fun totalQuantityForProject(projectId: Long): Int

    @Insert
    suspend fun insert(entry: PartEntryEntity): Long

    @Update
    suspend fun update(entry: PartEntryEntity)

    @Delete
    suspend fun delete(entry: PartEntryEntity)

    @Query("DELETE FROM part_entry WHERE project_id = :projectId")
    suspend fun clearProject(projectId: Long)
}

@Dao
interface StockProfileDao {

    @Query("SELECT * FROM stock_profile ORDER BY last_used_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 8): Flow<List<StockProfileEntity>>

    @Insert
    suspend fun insert(profile: StockProfileEntity): Long

    @Delete
    suspend fun delete(profile: StockProfileEntity)
}

// Duplicating a project spans three DAOs, so it cannot live in any one of them.
// See duplicateProject() in ProjectDuplication.kt.
