package com.kingkharnivore.skillz.utils.time

import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.utils.health.FlowActiveIntervalCodec

/** Projects recorded Flow time into a local calendar window without changing stored history. */
object StorySessionWindow {
    fun durationMs(session: SessionEntity, window: TimeWindow): Long {
        if (window.endMs <= window.startMs || session.durationMs <= 0L) return 0L
        val intervals = FlowActiveIntervalCodec.decode(session.activeIntervalJson)
        if (intervals.isNotEmpty()) {
            return intervals.sumOf {
                (minOf(it.endTimeMs, window.endMs) - maxOf(it.startTimeMs, window.startMs))
                    .coerceAtLeast(0L)
            }
        }

        // Older Flows stored only elapsed time and wall-clock bounds. Their pause locations
        // cannot be recovered, so distribute elapsed time proportionally across those bounds.
        val span = session.endTime - session.startTime
        if (span <= 0L) return 0L
        val duration = session.durationMs.coerceAtMost(span)
        fun elapsedAt(timeMs: Long): Long {
            val offset = (timeMs - session.startTime).coerceIn(0L, span)
            if (offset == span) return duration
            return (duration.toDouble() * offset / span).toLong()
        }
        return elapsedAt(window.endMs) - elapsedAt(window.startMs)
    }

    fun project(sessions: List<SessionEntity>, window: TimeWindow): List<SessionEntity> =
        sessions.mapNotNull { session ->
            val duration = durationMs(session, window)
            val recordedHere = session.createdAt in window.startMs until window.endMs
            if (duration == 0L && !recordedHere) return@mapNotNull null
            session.copy(
                durationMs = duration,
                // Scores are awarded once, on completion; time belongs to when it was spent.
                scyraPoints = if (recordedHere) session.scyraPoints else 0,
                surgePoints = if (recordedHere) session.surgePoints else 0,
                arcBonusPoints = if (recordedHere) session.arcBonusPoints else 0
            )
        }
}
