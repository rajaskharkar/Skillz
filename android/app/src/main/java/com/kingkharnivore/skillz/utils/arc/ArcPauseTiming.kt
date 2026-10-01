package com.kingkharnivore.skillz.utils.arc

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import com.kingkharnivore.skillz.utils.health.FlowActiveIntervalCodec

object ArcPauseTiming {
    fun budgetMs(state: ArcRuntimeState): Long = when {
        state.sessionCountInArc + 1 <= 3 -> ArcRules.PAUSE_BUDGET_EARLY_MS
        state.sessionCountInArc + 1 >= 10 -> ArcRules.PAUSE_BUDGET_ULTRA_MS
        else -> ArcRules.PAUSE_BUDGET_LATE_MS
    }

    fun remainingMs(state: ArcRuntimeState, nowMs: Long): Long {
        val activePause = state.pauseStartedAtMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L
        return (budgetMs(state) - state.pauseUsedMs - activePause).coerceAtLeast(0L)
    }

    fun endPause(state: ArcRuntimeState, nowMs: Long): ArcRuntimeState {
        val started = state.pauseStartedAtMs ?: return state
        return state.copy(
            pauseUsedMs = state.pauseUsedMs + (nowMs - started).coerceAtLeast(0L),
            pauseStartedAtMs = null
        )
    }

    fun restore(saved: ArcRuntimeState?, ongoing: OngoingSessionEntity): ArcRuntimeState? {
        val arcId = ongoing.arcId ?: return saved
        val state = saved?.takeIf { it.arcId == arcId } ?: ArcRuntimeState(
            arcId = arcId,
            isPending = (ongoing.arcSessionCountInArc ?: 0) < 2,
            multiplier = ongoing.arcChainBase ?: ArcRules.START_MULTIPLIER,
            progressMs = 0L,
            lastSessionEndTimeMs = ongoing.arcLastSessionEndTimeMs ?: 0L,
            sessionCountInArc = ongoing.arcSessionCountInArc ?: 0
        )
        if (ongoing.isRunning || ongoing.accumulatedBeforeStartMs == 0L || state.pauseStartedAtMs != null) {
            return state
        }
        // Upgrade paused Flows saved before pause timestamps were persisted. The last active
        // interval ends at Exit Flow, so opening a screen must never supply a new start time.
        val pausedAt = FlowActiveIntervalCodec.decode(ongoing.activeIntervalJson)
            .lastOrNull()?.endTimeMs ?: ongoing.createdAt
        return state.copy(pauseStartedAtMs = pausedAt)
    }
}
