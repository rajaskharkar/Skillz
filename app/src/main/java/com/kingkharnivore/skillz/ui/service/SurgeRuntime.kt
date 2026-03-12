package com.kingkharnivore.skillz.ui.service

private const val TEN_SECONDS_MS = 10_000L
private const val ONE_MINUTE_MS = 60_000L
private const val TWO_MINUTES_MS = 2 * 60_000L
private const val FIVE_MINUTES_MS = 5 * 60_000L
private const val LONG_SURGE_THRESHOLD_MS = 15 * 60_000L

data class SurgeRuntimeState(
    val plannedMs: Long,
    val midpointEmitted: Boolean = false,
    val fiveMinutesLeftEmitted: Boolean = false,
    val twoMinutesLeftEmitted: Boolean = false,
    val oneMinuteLeftEmitted: Boolean = false,
    val thirtySecondsLeftEmitted: Boolean = false,
    val tenSecondsLeftEmitted: Boolean = false,
    val countdownSecondsEmitted: Set<Int> = emptySet(),
    val targetReached: Boolean = false,
    val targetReachedAtMs: Long? = null,
)

sealed interface SurgeTickEvent {
    data object Midpoint : SurgeTickEvent
    data object FiveMinutesLeft : SurgeTickEvent
    data object TwoMinutesLeft : SurgeTickEvent
    data object OneMinuteLeft : SurgeTickEvent
    data object ThirtySecondsLeft : SurgeTickEvent
    data object TenSecondsLeft : SurgeTickEvent
    data class CountdownTick(val secondsRemaining: Int) : SurgeTickEvent
    data object TargetReached : SurgeTickEvent
}

object SurgeRuntimeEvaluator {

    fun evaluate(
        runtime: SurgeRuntimeState,
        elapsedMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<SurgeRuntimeState, List<SurgeTickEvent>> {
        var next = runtime
        val events = mutableListOf<SurgeTickEvent>()

        val plannedMs = runtime.plannedMs
        val remainingMs = (plannedMs - elapsedMs).coerceAtLeast(0L)
        val midpointMs = plannedMs / 2L

        // 1) Midpoint
        if (!next.midpointEmitted && elapsedMs >= midpointMs && midpointMs > 0L) {
            next = next.copy(midpointEmitted = true)
            events += SurgeTickEvent.Midpoint
        }

        // 2) 5 minutes left (only for surges > 15 min)
        if (
            plannedMs > LONG_SURGE_THRESHOLD_MS &&
            !next.fiveMinutesLeftEmitted &&
            remainingMs <= FIVE_MINUTES_MS &&
            elapsedMs < plannedMs
        ) {
            next = next.copy(fiveMinutesLeftEmitted = true)
            events += SurgeTickEvent.FiveMinutesLeft
        }

        // 3) 2 minutes left
        if (
            !next.twoMinutesLeftEmitted &&
            remainingMs <= TWO_MINUTES_MS &&
            elapsedMs < plannedMs
        ) {
            next = next.copy(twoMinutesLeftEmitted = true)
            events += SurgeTickEvent.TwoMinutesLeft
        }

        // 4) 1 minute left
        if (
            !next.oneMinuteLeftEmitted &&
            remainingMs <= ONE_MINUTE_MS &&
            elapsedMs < plannedMs
        ) {
            next = next.copy(oneMinuteLeftEmitted = true)
            events += SurgeTickEvent.OneMinuteLeft
        }

        // 4.5) 30 seconds left
        if (
            !next.thirtySecondsLeftEmitted &&
            remainingMs <= 30_000L &&
            elapsedMs < plannedMs
        ) {
            next = next.copy(thirtySecondsLeftEmitted = true)
            events += SurgeTickEvent.ThirtySecondsLeft
        }

        // 5) 10 seconds left
        if (
            !next.tenSecondsLeftEmitted &&
            remainingMs <= TEN_SECONDS_MS &&
            elapsedMs < plannedMs
        ) {
            next = next.copy(tenSecondsLeftEmitted = true)
            events += SurgeTickEvent.TenSecondsLeft
        }

        // 6) Countdown buzzes at 5,4,3,2,1
        if (remainingMs in 1..5_000L) {
            val secondsRemaining = ((remainingMs + 999L) / 1000L).toInt() // ceil
            if (secondsRemaining in 1..5 && secondsRemaining !in next.countdownSecondsEmitted) {
                next = next.copy(
                    countdownSecondsEmitted = next.countdownSecondsEmitted + secondsRemaining
                )
                events += SurgeTickEvent.CountdownTick(secondsRemaining)
            }
        }

        // 7) Target reached
        if (elapsedMs >= plannedMs && !next.targetReached) {
            next = next.copy(
                targetReached = true,
                targetReachedAtMs = nowMs
            )
            events += SurgeTickEvent.TargetReached
        }

        return next to events
    }

    fun silentCatchUp(
        runtime: SurgeRuntimeState,
        elapsedMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): SurgeRuntimeState {
        val plannedMs = runtime.plannedMs
        val remainingMs = (plannedMs - elapsedMs).coerceAtLeast(0L)
        val midpointMs = plannedMs / 2L

        val caughtCountdown = buildSet {
            if (remainingMs in 1..5_000L) {
                val sec = ((remainingMs + 999L) / 1000L).toInt()
                for (s in sec..5) {
                    if (s in 1..5) add(s)
                }
            } else if (elapsedMs >= plannedMs) {
                addAll(setOf(1, 2, 3, 4, 5))
            }
        }

        val caughtTarget = elapsedMs >= plannedMs

        return runtime.copy(
            midpointEmitted = runtime.midpointEmitted || elapsedMs >= midpointMs,
            fiveMinutesLeftEmitted = runtime.fiveMinutesLeftEmitted ||
                    (plannedMs > LONG_SURGE_THRESHOLD_MS && remainingMs <= FIVE_MINUTES_MS),
            twoMinutesLeftEmitted = runtime.twoMinutesLeftEmitted || remainingMs <= TWO_MINUTES_MS,
            oneMinuteLeftEmitted = runtime.oneMinuteLeftEmitted || remainingMs <= ONE_MINUTE_MS,
            thirtySecondsLeftEmitted = runtime.thirtySecondsLeftEmitted || remainingMs <= 30_000L,
            tenSecondsLeftEmitted = runtime.tenSecondsLeftEmitted || remainingMs <= TEN_SECONDS_MS,
            countdownSecondsEmitted = runtime.countdownSecondsEmitted + caughtCountdown,
            targetReached = runtime.targetReached || caughtTarget,
            targetReachedAtMs = when {
                runtime.targetReachedAtMs != null -> runtime.targetReachedAtMs
                caughtTarget -> nowMs
                else -> null
            }
        )
    }
}