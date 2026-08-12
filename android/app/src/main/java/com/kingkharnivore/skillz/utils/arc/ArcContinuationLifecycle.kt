package com.kingkharnivore.skillz.utils.arc

import com.kingkharnivore.skillz.ui.model.ArcRuntimeState

interface ArcContinuationStore {
    suspend fun loadActive(): ArcRuntimeState?
    suspend fun saveActive(state: ArcRuntimeState)
    suspend fun clearActive()
    suspend fun loadRecentlyEnded(): ArcRuntimeState?
    suspend fun saveRecentlyEnded(
        state: ArcRuntimeState,
        completedAtMs: Long = System.currentTimeMillis()
    )
    suspend fun clearRecentlyEnded()
}

/** Owns the persisted transition between active and recently-ended Arc state. */
class ArcContinuationLifecycle(private val store: ArcContinuationStore) {
    suspend fun completeArc(finalState: ArcRuntimeState, flowEndTimeMs: Long) {
        store.saveRecentlyEnded(
            state = finalState.copy(lastSessionEndTimeMs = flowEndTimeMs),
            completedAtMs = flowEndTimeMs
        )
        store.clearActive()
    }

    suspend fun resolveForFlowStart(flowStartTimeMs: Long, isSoftFlow: Boolean): ArcRuntimeState? {
        val resolved = ArcContinuationResolver.resolveArcForNewFlow(
            activeArc = store.loadActive(),
            recentlyEndedArc = store.loadRecentlyEnded(),
            flowStartTimeMs = flowStartTimeMs
        )

        if (resolved == null) {
            store.clearActive()
            store.clearRecentlyEnded()
            return null
        }

        val started = if (isSoftFlow) resolved.resetMultiplierForSoftFlow() else resolved
        store.saveActive(started)
        store.clearRecentlyEnded()
        return started
    }
}
