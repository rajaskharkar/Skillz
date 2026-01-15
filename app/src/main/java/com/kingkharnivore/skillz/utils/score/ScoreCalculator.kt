package com.kingkharnivore.skillz.utils.score

import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

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
        val n = (plannedMs.toDouble() / MILLIS_PER_MINUTE).coerceAtLeast(1.0)
        val a = (actualDurationMs.toDouble() / MILLIS_PER_MINUTE).coerceAtLeast(1.0)

        val error = abs(a - n) / n
        val maxBonus = 0.35
        val sharpness = 5.0
        val multiplier = 1.0 + (maxBonus * exp(-sharpness * error))

        val raw = (a * multiplier).roundToInt()

        return if (a <= n) raw else raw.coerceAtMost(n.toInt())
    }

    fun sessionScore(session: SessionEntity): Int {
        return breakdownFromDuration(session.durationMs).totalPoints
    }

    fun totalScoreForSessions(sessions: List<SessionEntity>): Int {
        return sessions.sumOf { sessionScore(it) }
    }
}
