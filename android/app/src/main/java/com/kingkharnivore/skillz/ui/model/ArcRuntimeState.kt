package com.kingkharnivore.skillz.ui.model

data class ArcRuntimeState(
    val arcId: Long,
    val isPending: Boolean,
    val multiplier: Double,
    val progressMs: Long,
    val lastSessionEndTimeMs: Long,
    val sessionCountInArc: Int,
    val pauseUsedMs: Long = 0L,
    val pauseStartedAtMs: Long? = null
) {
    companion object {
        const val BASE_MULTIPLIER = 1.0
    }

    /** Resets reward momentum without changing any Arc-continuity state. */
    fun resetMultiplierForSoftFlow(): ArcRuntimeState = copy(multiplier = BASE_MULTIPLIER)

    /** Records a completed Soft Flow as the next session in this same Arc. */
    fun afterCompletedSoftFlow(sessionEndTimeMs: Long): ArcRuntimeState = copy(
        isPending = sessionCountInArc + 1 < 2,
        multiplier = BASE_MULTIPLIER,
        progressMs = 0L,
        lastSessionEndTimeMs = sessionEndTimeMs,
        sessionCountInArc = sessionCountInArc + 1
    )
}
