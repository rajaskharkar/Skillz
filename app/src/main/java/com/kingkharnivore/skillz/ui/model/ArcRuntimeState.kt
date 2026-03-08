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
)