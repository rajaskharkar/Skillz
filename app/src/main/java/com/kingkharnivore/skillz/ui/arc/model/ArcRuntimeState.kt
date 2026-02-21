package com.kingkharnivore.skillz.ui.arc.model

data class ArcRuntimeState(
    val arcId: Long,
    val isPending: Boolean,           // pending until session #2 is saved
    val multiplier: Double,           // current multiplier used for next arc session (session 2 starts at 1.3)
    val progressMs: Long,             // bank toward next +0.1
    val lastSessionEndTimeMs: Long,   // used for 5-min grace window
    val sessionCountInArc: Int        // how many sessions are already tagged into this arc (>=1 once Continue Arc is chosen)
)