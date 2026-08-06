package com.stockcut.data.repository

import com.stockcut.data.db.StockCutDatabase
import com.stockcut.data.db.duplicateProject
import com.stockcut.data.model.Project
import com.stockcut.data.model.ProjectSummary
import com.stockcut.data.seed.ExampleProject
import com.stockcut.units.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Projects: the list, and the lifecycle of one job.
 *
 * The repository layer exists so `:app` never imports Room (docs/07 W2). A
 * screen asks for a [Project]; where it is stored is not its business.
 *
 * @param now injected so tests do not depend on the wall clock, and so every
 *   timestamp in one operation is identical rather than a few millis apart.
 */
class ProjectRepository(
    private val db: StockCutDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Sorted by last modified, which is the order docs/03 S1 specifies. */
    fun observeProjects(): Flow<List<Project>> =
        db.projectDao().observeAll().map { rows -> rows.map { it.toDomain() } }

    /**
     * Projects with their piece counts, for the S1 cards.
     *
     * Combined here rather than in the ViewModel so the two flows cannot be
     * observed out of step and briefly render a job's name beside another job's
     * part count.
     */
    fun observeProjectSummaries(): Flow<List<ProjectSummary>> =
        combine(
            db.projectDao().observeAll(),
            db.partDao().observePieceTotals(),
        ) { rows, totals ->
            val byProject = totals.associate { it.projectId to it.totalQuantity }
            rows.map { entity ->
                ProjectSummary(
                    project = entity.toDomain(),
                    // Absent means no parts yet, not missing data.
                    totalPieces = byProject[entity.id] ?: 0,
                )
            }
        }

    suspend fun getProject(id: Long): Project? = db.projectDao().byId(id)?.toDomain()

    /** Real jobs only. The seeded example must not consume the free tier's one slot. */
    suspend fun realProjectCount(): Int = db.projectDao().countReal()

    suspend fun createProject(
        name: String,
        unitSystem: UnitSystem,
        fractionDenominator: Int,
        kerfU: Long,
        trimU: Long = 0,
    ): Long {
        val timestamp = now()
        return db.projectDao().insert(
            Project(
                id = 0,
                name = name,
                unitSystem = unitSystem,
                fractionDenominator = fractionDenominator,
                kerfU = kerfU,
                trimU = trimU,
                isExample = false,
                createdAt = timestamp,
                updatedAt = timestamp,
            ).toEntity(),
        )
    }

    /** Stamps updatedAt, so the list re-sorts without every caller remembering to. */
    suspend fun updateProject(project: Project) {
        db.projectDao().update(project.copy(updatedAt = now()).toEntity())
    }

    suspend fun rename(id: Long, newName: String) {
        val existing = db.projectDao().byId(id) ?: return
        db.projectDao().update(existing.copy(name = newName, updatedAt = now()))
    }

    /** Hard delete. Stock and parts go with it, via ON DELETE CASCADE. */
    suspend fun deleteProject(id: Long) {
        val existing = db.projectDao().byId(id) ?: return
        db.projectDao().delete(existing)
    }

    /** @return the new project's id, or null if the source is gone. */
    suspend fun duplicate(sourceId: Long, newName: String): Long? =
        db.duplicateProject(sourceId, newName, now())

    /** Called when a project's contents change, so the list order stays truthful. */
    suspend fun touch(id: Long) = db.projectDao().touch(id, now())

    /**
     * Seed the first-run example job.
     *
     * docs/03 "First-run experience": no onboarding carousel. The user taps one
     * read-only example, sees a finished cut plan in three seconds, and
     * understands the product without reading a word.
     *
     * Two guards, because this runs on every launch:
     *  - [alreadyDeleted] — once the user deletes it, it never comes back. That
     *    flag lives in DataStore, not here, because the row is gone by then and
     *    there would be nothing left to check.
     *  - an existing example row — a second copy on every cold start would be
     *    the single most irritating bug in the app.
     *
     * @return the example's id if it was created, null if it was not needed.
     */
    suspend fun seedExampleIfNeeded(alreadyDeleted: Boolean): Long? {
        if (alreadyDeleted) return null
        if (db.projectDao().countExample() > 0) return null

        val timestamp = now()
        val projectId = db.projectDao().insert(
            Project(
                id = 0,
                name = ExampleProject.NAME,
                unitSystem = UnitSystem.MM,
                fractionDenominator = 16,
                kerfU = ExampleProject.kerfU,
                trimU = 0,
                isExample = true,
                createdAt = timestamp,
                updatedAt = timestamp,
            ).toEntity(),
        )

        ExampleProject.stock.forEachIndexed { index, seed ->
            db.stockDao().insert(
                com.stockcut.data.model.StockEntry(
                    id = 0,
                    lengthU = seed.lengthU,
                    quantity = seed.quantity,
                    label = seed.label,
                    sortOrder = index,
                ).toEntity(projectId),
            )
        }
        ExampleProject.parts.forEachIndexed { index, seed ->
            db.partDao().insert(
                com.stockcut.data.model.PartEntry(
                    id = 0,
                    lengthU = seed.lengthU,
                    quantity = seed.quantity,
                    label = seed.label,
                    sortOrder = index,
                ).toEntity(projectId),
            )
        }
        return projectId
    }
}
