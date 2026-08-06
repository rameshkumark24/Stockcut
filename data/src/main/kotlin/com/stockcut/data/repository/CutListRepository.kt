package com.stockcut.data.repository

import com.stockcut.data.db.StockCutDatabase
import com.stockcut.data.model.CutList
import com.stockcut.data.model.PartEntry
import com.stockcut.data.model.StockEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The contents of one job: what you have, and what you need cut.
 *
 * Every write touches the parent project's updatedAt, so the projects list
 * re-sorts when a job is edited rather than only when it is renamed. Callers
 * get that for free instead of having to remember it — forgetting is invisible
 * until a user wonders why last week's job is at the top.
 */
class CutListRepository(
    private val db: StockCutDatabase,
    private val projects: ProjectRepository,
) {

    fun observeStock(projectId: Long): Flow<List<StockEntry>> =
        db.stockDao().observeForProject(projectId).map { rows -> rows.map { it.toDomain() } }

    fun observeParts(projectId: Long): Flow<List<PartEntry>> =
        db.partDao().observeForProject(projectId).map { rows -> rows.map { it.toDomain() } }

    /** Sum of quantities, not row count — the free-tier limit counts pieces. */
    suspend fun totalPieces(projectId: Long): Int =
        db.partDao().totalQuantityForProject(projectId)

    suspend fun stockCount(projectId: Long): Int = db.stockDao().countForProject(projectId)

    /**
     * Everything needed to optimize, read together.
     *
     * One call rather than three so a screen cannot pair a project with another
     * project's parts, and so the optimizer never sees a half-loaded job.
     */
    suspend fun loadCutList(projectId: Long): CutList? {
        val project = projects.getProject(projectId) ?: return null
        return CutList(
            project = project,
            stock = db.stockDao().forProject(projectId).map { it.toDomain() },
            parts = db.partDao().forProject(projectId).map { it.toDomain() },
        )
    }

    // ── Stock ────────────────────────────────────────────────────────────────

    suspend fun addStock(
        projectId: Long,
        lengthU: Long,
        quantity: Int = StockEntry.UNLIMITED,
        label: String? = null,
    ): Long {
        val sortOrder = db.stockDao().forProject(projectId).size
        val id = db.stockDao().insert(
            StockEntry(0, lengthU, quantity, label, sortOrder).toEntity(projectId),
        )
        projects.touch(projectId)
        return id
    }

    suspend fun updateStock(projectId: Long, entry: StockEntry) {
        db.stockDao().update(entry.toEntity(projectId))
        projects.touch(projectId)
    }

    suspend fun deleteStock(projectId: Long, entry: StockEntry) {
        db.stockDao().delete(entry.toEntity(projectId))
        projects.touch(projectId)
    }

    // ── Parts ────────────────────────────────────────────────────────────────

    suspend fun addPart(
        projectId: Long,
        lengthU: Long,
        quantity: Int,
        label: String? = null,
    ): Long {
        val sortOrder = db.partDao().forProject(projectId).size
        val id = db.partDao().insert(
            PartEntry(0, lengthU, quantity, label, sortOrder).toEntity(projectId),
        )
        projects.touch(projectId)
        return id
    }

    suspend fun updatePart(projectId: Long, entry: PartEntry) {
        db.partDao().update(entry.toEntity(projectId))
        projects.touch(projectId)
    }

    suspend fun deletePart(projectId: Long, entry: PartEntry) {
        db.partDao().delete(entry.toEntity(projectId))
        projects.touch(projectId)
    }

    /**
     * Re-insert a deleted row, for the undo snackbar (docs/04 §9).
     *
     * The undo window holds the row in memory, not in the database — docs/05
     * §1.3 specifies hard delete with no tombstone. So undo is an insert, and it
     * deliberately does NOT preserve the old id: that id is gone, and inventing
     * a row with a reused primary key is how you get two parts pointing at the
     * same thing.
     */
    suspend fun restorePart(projectId: Long, entry: PartEntry): Long {
        val id = db.partDao().insert(entry.copy(id = 0).toEntity(projectId))
        projects.touch(projectId)
        return id
    }

    suspend fun restoreStock(projectId: Long, entry: StockEntry): Long {
        val id = db.stockDao().insert(entry.copy(id = 0).toEntity(projectId))
        projects.touch(projectId)
        return id
    }
}
