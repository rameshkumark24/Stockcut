package com.stockcut.data.db

import androidx.room.withTransaction

/**
 * Duplicate a saved job, with its stock and its parts (user story US-15:
 * "duplicate last month's job and change two numbers").
 *
 * This is a database-level operation rather than a DAO method because it spans
 * three DAOs, and Room's @Transaction only covers a single DAO's own queries.
 * An earlier ProjectWriteDao tried to take the other DAOs as parameters; it was
 * never exposed on [StockCutDatabase], so Room generated no implementation for
 * it and the transaction never applied. withTransaction is the supported way to
 * span DAOs, and it does apply.
 *
 * Atomicity matters here: a half-copied job — the project row saved but its
 * parts lost — looks to the user like the app silently deleted their work.
 *
 * The copy is never marked as the example job, so duplicating the seeded
 * "Example: gate frame" produces a normal, editable job that counts against the
 * free tier the way any other job does.
 *
 * @return the new project's id, or null if [sourceId] no longer exists.
 */
suspend fun StockCutDatabase.duplicateProject(
    sourceId: Long,
    newName: String,
    now: Long,
): Long? = withTransaction {
    val source = projectDao().byId(sourceId) ?: return@withTransaction null

    val copyId = projectDao().insert(
        source.copy(
            id = 0,
            name = newName,
            isExample = false,
            createdAt = now,
            updatedAt = now,
        ),
    )
    stockDao().forProject(sourceId).forEach {
        stockDao().insert(it.copy(id = 0, projectId = copyId))
    }
    partDao().forProject(sourceId).forEach {
        partDao().insert(it.copy(id = 0, projectId = copyId))
    }
    copyId
}
