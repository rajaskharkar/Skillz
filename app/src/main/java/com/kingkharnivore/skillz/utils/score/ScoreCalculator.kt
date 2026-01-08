package com.kingkharnivore.skillz.utils.score

import com.kingkharnivore.skillz.data.model.entity.SessionEntity

object ScoreCalculator {

    private const val MILLIS_PER_MINUTE = 60_000L

    fun breakdownFromDuration(durationMs: Long): ScoreBreakdown {
        val minutes = (durationMs / MILLIS_PER_MINUTE)
            .coerceAtLeast(0L)
            .toInt()

        val basePoints = minutes

        val tenMinuteBonuses = minutes / 10
        val thirtyMinuteBonuses = minutes / 30
        val sixtyMinuteBonuses = minutes / 60

        val totalPoints =
            basePoints +
                    tenMinuteBonuses * 5 +
                    thirtyMinuteBonuses * 15 +
                    sixtyMinuteBonuses * 50

        return ScoreBreakdown(
            minutes = minutes,
            basePoints = basePoints,
            tenMinuteBonuses = tenMinuteBonuses,
            thirtyMinuteBonuses = thirtyMinuteBonuses,
            sixtyMinuteBonuses = sixtyMinuteBonuses,
            totalPoints = totalPoints
        )
    }

    fun surgePoints(
        surgePlannedMs: Long?,
        actualDurationMs: Long
    ): Int {
        val plannedMs = surgePlannedMs ?: return 0
        val actualMs = actualDurationMs.coerceAtLeast(0L)

        val savedMs = plannedMs - actualMs
        if (savedMs <= 0L) return 0

        // Round UP to full minutes
        return ((savedMs + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt()
    }

    fun sessionScore(session: SessionEntity): Int {
        return breakdownFromDuration(session.durationMs).totalPoints
    }

    fun totalScoreForSessions(sessions: List<SessionEntity>): Int {
        return sessions.sumOf { sessionScore(it) }
    }
}
