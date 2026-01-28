package com.kingkharnivore.skillz.utils.score

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object BeamScoreCalculator {

    private const val MILLIS_PER_MINUTE = 60_000.0

    /** half-open overlap duration in ms: [aStart,aEnd) with [bStart,bEnd) */
    fun overlapMs(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Long {
        val start = max(aStart, bStart)
        val end = min(aEnd, bEnd)
        return (end - start).coerceAtLeast(0L)
    }

    /**
     * ✅ Your rule:
     * - multiplier starts at 2x at the beginning of the beam
     * - ramps up to 5x as engaged time approaches the beam's scheduled duration
     * - NO duration buckets, NO 60-min caps
     */
    private fun capMultiplier(): Double = 5.0

    /** Ease-out cubic curve [0..1] -> [0..1] */
    private fun easeOutCubic(t: Double): Double {
        val x = t.coerceIn(0.0, 1.0)
        return 1.0 - (1.0 - x).pow(3.0)
    }

    /**
     * Multiplier starts at 2x and ramps toward 5x based on:
     * continuousEngagedMs / beamDurationMs
     *
     * - If beamDurationMs is 120 mins, it ramps across 120 mins.
     * - If beamDurationMs is 20 mins, it ramps across 20 mins.
     */
    fun multiplier(
        continuousEngagedMs: Long,
        beamDurationMs: Long
    ): Double {
        if (beamDurationMs <= 0L) return 2.0

        val p = (continuousEngagedMs.toDouble() / beamDurationMs.toDouble())
            .coerceIn(0.0, 1.0)

        val eased = easeOutCubic(p)
        val cap = capMultiplier()

        val m = 2.0 + (cap - 2.0) * eased
        return m.coerceIn(2.0, cap)
    }

    data class BeamScoreResult(
        val sessionTotalPointsBase: Int,          // base total points for full duration (for reference)
        val eligibleMs: Long,
        val eligiblePointsBase: Int,              // base points for eligible overlap time (recomputed)
        val eligiblePointsBoosted: Int,           // boosted eligible points
        val beamBonusPoints: Int,                 // ✅ guaranteed non-negative
        val appliedMultiplier: Double,
    )

    /**
     * ✅ Correct beam bonus math:
     * - Compute eligiblePoints from eligibleMs
     * - Boost only eligiblePoints with multiplier
     * - Bonus is ONLY (boostedEligible - eligibleBase)
     *
     * This avoids negative "bonus" due to ScoreCalculator thresholds being non-linear.
     */
    fun scoreWithBeam(
        sessionStart: Long,
        sessionEnd: Long,
        sessionDurationMs: Long,
        beamStart: Long,
        beamEnd: Long,
        beamDurationMs: Long,
        continuousEngagedMsInThisSession: Long
    ): BeamScoreResult {

        val sessionTotalBase = ScoreCalculator.breakdownFromDuration(sessionDurationMs).totalPoints

        val eligibleMs = overlapMs(sessionStart, sessionEnd, beamStart, beamEnd)
        if (eligibleMs <= 0L) {
            return BeamScoreResult(
                sessionTotalPointsBase = sessionTotalBase,
                eligibleMs = 0L,
                eligiblePointsBase = 0,
                eligiblePointsBoosted = 0,
                beamBonusPoints = 0,
                appliedMultiplier = 1.0
            )
        }

        val eligiblePointsBase = ScoreCalculator.breakdownFromDuration(eligibleMs).totalPoints

        // Continuous engaged time cannot exceed eligibleMs.
        val contMs = continuousEngagedMsInThisSession.coerceIn(0L, eligibleMs)

        val m = multiplier(contMs, beamDurationMs)

        val eligibleBoosted = (eligiblePointsBase * m).roundToInt()

        // ✅ Beam bonus only comes from eligible region's boost.
        val bonus = (eligibleBoosted - eligiblePointsBase).coerceAtLeast(0)

        return BeamScoreResult(
            sessionTotalPointsBase = sessionTotalBase,
            eligibleMs = eligibleMs,
            eligiblePointsBase = eligiblePointsBase,
            eligiblePointsBoosted = eligibleBoosted,
            beamBonusPoints = bonus,
            appliedMultiplier = m
        )
    }
}
