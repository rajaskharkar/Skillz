package com.kingkharnivore.skillz.utils.score

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

        val sixtyMinuteBonuses = minutes / 60
        val thirtyMinuteBonuses = (minutes / 30) - (minutes / 60)
        val tenMinuteBonuses = (minutes / 10) - (minutes / 30)

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
        val plannedMinutes = (plannedMs.toDouble() / MILLIS_PER_MINUTE).coerceAtLeast(1.0)
        val actualMinutes = (actualDurationMs.toDouble() / MILLIS_PER_MINUTE).coerceAtLeast(1.0)

        val error = abs(actualMinutes - plannedMinutes) / plannedMinutes
        val maxBonus = 0.35
        val sharpness = 5.0
        val multiplier = 1.0 + (maxBonus * exp(-sharpness * error))

        val raw = (actualMinutes * multiplier).roundToInt()

        return if (actualMinutes <= plannedMinutes) {
            raw
        } else {
            raw.coerceAtMost(plannedMinutes.toInt())
        }
    }

    data class ArcMathResult(
        val arcMultiplierUsed: Double,
        val arcBonusPoints: Int,
        val finalPoints: Int,
        val nextChainBase: Double,
        val didLevelUp: Boolean
    )

    fun arcMath(
        beforeArcPoints: Int,
        chainBase: Double,
        durationMs: Long,
        stepMs: Long = 10 * MILLIS_PER_MINUTE,
        step: Double = 0.1
    ): ArcMathResult {
        val tierExtra = arcTierExtra(durationMs)
        val used = chainBase + tierExtra

        val boosted = (beforeArcPoints * used).roundToInt()
        val bonus = (boosted - beforeArcPoints).coerceAtLeast(0)
        val final = beforeArcPoints + bonus

        val didLevel = durationMs >= stepMs
        val nextBase = if (didLevel) chainBase + step else chainBase

        return ArcMathResult(
            arcMultiplierUsed = used,
            arcBonusPoints = bonus,
            finalPoints = final,
            nextChainBase = nextBase,
            didLevelUp = didLevel
        )
    }

    private fun arcTierExtra(durationMs: Long): Double {
        return when {
            durationMs < 10 * MILLIS_PER_MINUTE -> 0.0
            durationMs < 20 * MILLIS_PER_MINUTE -> 0.0
            durationMs < 40 * MILLIS_PER_MINUTE -> 0.1
            durationMs < 60 * MILLIS_PER_MINUTE -> 0.2
            durationMs < 90 * MILLIS_PER_MINUTE -> 0.3
            else -> 0.4
        }
    }
}