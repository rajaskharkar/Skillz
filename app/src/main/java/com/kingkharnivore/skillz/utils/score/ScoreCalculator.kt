package com.kingkharnivore.skillz.utils.score

import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object ScoreCalculator {

    private const val MILLIS_PER_MINUTE = 60_000L

    fun breakdownFromDuration(durationMs: Long): ScoreBreakdown {
        val minutes = (durationMs / MILLIS_PER_MINUTE)
            .coerceAtLeast(0L)
            .toInt()

        val basePoints = minutes

        // ✅ Exclusive milestone bonuses:
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
        val base = breakdownFromDuration(session.durationMs).totalPoints
        return base + session.beamBonusPoints
    }

    fun totalScoreForSessions(sessions: List<SessionEntity>): Int {
        return sessions.sumOf { sessionScore(it) }
    }

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

    data class ArcMathResult(
        val arcMultiplierUsed: Double,   // chainBase + tierExtra for THIS session
        val arcBonusPoints: Int,         // boosted - beforeArc
        val finalPoints: Int,            // beforeArc + arcBonusPoints
        val nextChainBase: Double,       // chainBase advanced for NEXT flow (no tier)
        val didLevelUp: Boolean          // whether chain grew by +0.1
    )

    fun arcMath(
        beforeArcPoints: Int,
        chainBase: Double,
        durationMs: Long,
        startChainBase: Double = 1.3,           // optional, not required
        stepMs: Long = 10 * 60_000L,
        step: Double = 0.1
    ): ArcMathResult {
        val tierExtra = arcTierExtra(durationMs) // internal helper

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
            durationMs < 10 * 60_000L -> 0.0
            durationMs < 20 * 60_000L -> 0.0
            durationMs < 40 * 60_000L -> 0.1
            durationMs < 60 * 60_000L -> 0.2
            durationMs < 90 * 60_000L -> 0.3
            else -> 0.4
        }
    }
}
