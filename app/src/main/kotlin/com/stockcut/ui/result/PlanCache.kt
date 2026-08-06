package com.stockcut.ui.result

import com.stockcut.data.model.CutList
import com.stockcut.optimizer.OptimizeResult

/**
 * Holds the last computed plan, in memory only.
 *
 * Two reasons this exists:
 *
 *  1. docs/03 S4: "Back returns to the editor with the plan cached, so
 *     re-entering doesn't recompute."
 *  2. S3 has to run the optimizer BEFORE navigating, because an Infeasible
 *     result must block navigation entirely (docs/03 S3-ERR-1). Without a cache
 *     the result screen would immediately run the identical computation again.
 *
 * Deliberately NOT persisted. A plan is derived data — the job is the source of
 * truth, and a stale plan surviving a restart is a plan that might no longer
 * match the parts on screen. It is also why the entry is keyed by a signature of
 * the input rather than by project id alone: edit the job and the cached plan
 * stops matching and is discarded, rather than being shown against parts that
 * have since changed.
 */
object PlanCache {

    private var key: Key? = null
    private var value: OptimizeResult? = null

    private data class Key(val projectId: Long, val signature: Int)

    fun put(projectId: Long, cutList: CutList, result: OptimizeResult) {
        key = Key(projectId, cutList.signature())
        value = result
    }

    /** @return the cached result, or null if the job has changed since. */
    fun get(projectId: Long, cutList: CutList): OptimizeResult? =
        if (key == Key(projectId, cutList.signature())) value else null

    fun clear() {
        key = null
        value = null
    }

    /**
     * Everything the optimizer reads. Anything absent here could change without
     * invalidating the plan, so this list must stay in step with
     * CutList.toOptimizeRequest().
     */
    private fun CutList.signature(): Int {
        var hash = project.kerfU.hashCode()
        hash = 31 * hash + project.trimU.hashCode()
        for (s in stock) {
            hash = 31 * hash + s.id.hashCode()
            hash = 31 * hash + s.lengthU.hashCode()
            hash = 31 * hash + s.quantity
        }
        for (p in parts) {
            hash = 31 * hash + p.id.hashCode()
            hash = 31 * hash + p.lengthU.hashCode()
            hash = 31 * hash + p.quantity
        }
        return hash
    }
}
