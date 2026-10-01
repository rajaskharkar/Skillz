package com.kingkharnivore.skillz.utils.arc

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import com.kingkharnivore.skillz.utils.health.FlowActiveIntervalCodec

object ArcPauseTiming {
    private fun ageAtPauseMs(state: ArcRuntimeState, nowMs: Long): Long =
        ((state.pauseStartedAtMs ?: nowMs) - (state.startedAtMs ?: nowMs)).coerceAtLeast(0L)

    // Lock the tier at pause start so crossing a threshold cannot revive an expired pause.
    fun budgetMs(state: ArcRuntimeState, nowMs: Long): Long = when {
        ageAtPauseMs(state, nowMs) >= ArcRules.EXTENDED_PAUSE_THRESHOLD_MS -> ArcRules.PAUSE_BUDGET_EXTENDED_MS
        ageAtPauseMs(state, nowMs) >= ArcRules.LONG_PAUSE_THRESHOLD_MS -> ArcRules.PAUSE_BUDGET_LONG_MS
        else -> ArcRules.PAUSE_BUDGET_DEFAULT_MS
    }

    fun remainingMs(state: ArcRuntimeState, nowMs: Long): Long {
        val activePause = state.pauseStartedAtMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L
        val used = if (ageAtPauseMs(state, nowMs) < ArcRules.FRESH_PAUSE_WINDOW_MS) 0L else state.pauseUsedMs
        return (budgetMs(state, nowMs) - used - activePause).coerceAtLeast(0L)
    }

    fun beginPause(state: ArcRuntimeState, nowMs: Long): ArcRuntimeState {
        if (state.pauseStartedAtMs != null) return state
        return state.copy(
            pauseStartedAtMs = nowMs,
            pauseUsedMs = if (ageAtPauseMs(state, nowMs) < ArcRules.FRESH_PAUSE_WINDOW_MS) 0L else state.pauseUsedMs
        )
    }

    fun endPause(state: ArcRuntimeState, nowMs: Long): ArcRuntimeState {
        val started = state.pauseStartedAtMs ?: return state
        return state.copy(
            pauseUsedMs = if (ageAtPauseMs(state, nowMs) < ArcRules.FRESH_PAUSE_WINDOW_MS) {
                0L
            } else {
                state.pauseUsedMs + (nowMs - started).coerceAtLeast(0L)
            },
            pauseStartedAtMs = null
        )
    }

    fun restore(
        saved: ArcRuntimeState?,
        ongoing: OngoingSessionEntity,
        firstFlowStartedAtMs: Long? = null
    ): ArcRuntimeState? {
        val arcId = ongoing.arcId ?: return saved
        val existing = saved?.takeIf { it.arcId == arcId } ?: ArcRuntimeState(
            arcId = arcId,
            isPending = (ongoing.arcSessionCountInArc ?: 0) < 2,
            multiplier = ongoing.arcChainBase ?: ArcRules.START_MULTIPLIER,
            progressMs = 0L,
            lastSessionEndTimeMs = ongoing.arcLastSessionEndTimeMs ?: 0L,
            sessionCountInArc = ongoing.arcSessionCountInArc ?: 0
        )
        val intervals = FlowActiveIntervalCodec.decode(ongoing.activeIntervalJson)
        val hasStarted = ongoing.isRunning || ongoing.accumulatedBeforeStartMs > 0L || intervals.isNotEmpty()
        val ongoingStartedAt = if (hasStarted) {
            intervals.firstOrNull()?.startTimeMs ?: ongoing.baseStartTimeMs ?: ongoing.createdAt
        } else {
            null
        }
        val state = existing.copy(
            startedAtMs = existing.startedAtMs ?: firstFlowStartedAtMs ?: ongoingStartedAt
        )
        if (ongoing.isRunning || ongoing.accumulatedBeforeStartMs == 0L || state.pauseStartedAtMs != null) {
            return state
        }
        // Upgrade paused Flows saved before pause timestamps were persisted. The last active
        // interval ends at Exit Flow, so opening a screen must never supply a new start time.
        val pausedAt = intervals.lastOrNull()?.endTimeMs ?: ongoing.createdAt
        return state.copy(pauseStartedAtMs = pausedAt)
    }
}
