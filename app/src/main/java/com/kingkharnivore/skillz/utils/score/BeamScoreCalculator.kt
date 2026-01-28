package com.kingkharnivore.skillz.utils.score

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object BeamScoreCalculator {

    // ─────────────────────────────────────────────────────────────────────────────
    // Overlap
    // ─────────────────────────────────────────────────────────────────────────────

    /** half-open overlap duration in ms: [aStart,aEnd) with [bStart,bEnd) */
    fun overlapMs(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Long {
        val start = max(aStart, bStart)
        val end = min(aEnd, bEnd)
        return (end - start).coerceAtLeast(0L)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Multiplier (Hybrid Model A + B)
    // - starts gently (showing up)
    // - grows smoothly with continuous engagement
    // - hard-capped by progress tiers
    // ─────────────────────────────────────────────────────────────────────────────

    private const val MIN_MULTIPLIER = 1.3

    private data class Tier(val startP: Double, val endP: Double, val cap: Double)

    // Progress is p = engaged / beamDuration, clamped to [0..1]
    private val tiers = listOf(
        Tier(0.00, 0.25, 1.30),
        Tier(0.25, 0.50, 1.55),
        Tier(0.50, 0.70, 1.85),
        Tier(0.70, 1.00, 2.00)
    )

    /** Ease-out cubic curve [0..1] -> [0..1] */
    private fun easeOutCubic(t: Double): Double {
        val x = t.coerceIn(0.0, 1.0)
        return 1.0 - (1.0 - x).pow(3.0)
    }

    /**
     * Multiplier:
     * - based on continuousEngagedMs / beamDurationMs
     * - increases smoothly
     * - cannot exceed the tier cap for the current progress bucket
     */
    fun multiplier(
        continuousEngagedMs: Long,
        beamDurationMs: Long
    ): Double {
        if (beamDurationMs <= 0L) return MIN_MULTIPLIER

        val p = (continuousEngagedMs.toDouble() / beamDurationMs.toDouble())
            .coerceIn(0.0, 1.0)

        val tier = tiers.firstOrNull { p < it.endP } ?: tiers.last()

        // normalize p to [0..1] within this tier range
        val localT = if (tier.endP - tier.startP <= 0.0) {
            1.0
        } else {
            ((p - tier.startP) / (tier.endP - tier.startP)).coerceIn(0.0, 1.0)
        }

        val eased = easeOutCubic(localT)

        // We ramp from MIN_MULTIPLIER up to the tier cap, but never exceed it.
        val m = MIN_MULTIPLIER + (tier.cap - MIN_MULTIPLIER) * eased
        return m.coerceIn(MIN_MULTIPLIER, tier.cap)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Result + Scoring
    // ─────────────────────────────────────────────────────────────────────────────

    data class BeamScoreResult(
        val sessionTotalPointsBase: Int,   // base total points for full duration (for reference)
        val eligibleMs: Long,
        val eligiblePointsBase: Int,       // base points for eligible overlap time (recomputed)
        val eligiblePointsBoosted: Int,    // boosted eligible points
        val beamBonusPoints: Int,          // guaranteed non-negative
        val appliedMultiplier: Double
    )

    /**
     * Beam bonus rules:
     * - Eligible time = overlap(session, beam)
     * - Recompute base points for eligibleMs
     * - Boost ONLY eligiblePointsBase using multiplier(continuousEngagedMs, beamDurationMs)
     * - Beam bonus = boostedEligible - eligibleBase (never negative)
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

        // Continuous engagement can't exceed eligible region.
        val contMs = continuousEngagedMsInThisSession.coerceIn(0L, eligibleMs)

        val m = multiplier(contMs, beamDurationMs)

        val boostedEligible = (eligiblePointsBase * m).roundToInt()
        val bonus = (boostedEligible - eligiblePointsBase).coerceAtLeast(0)

        return BeamScoreResult(
            sessionTotalPointsBase = sessionTotalBase,
            eligibleMs = eligibleMs,
            eligiblePointsBase = eligiblePointsBase,
            eligiblePointsBoosted = boostedEligible,
            beamBonusPoints = bonus,
            appliedMultiplier = m
        )
    }
}
