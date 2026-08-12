package com.kingkharnivore.skillz.utils.arc

import com.kingkharnivore.skillz.ui.model.ArcRuntimeState

/** Authoritative, timestamp-based decision for attaching a new Flow to an Arc. */
object ArcContinuationResolver {
    fun resolveArcForNewFlow(
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?,
        flowStartTimeMs: Long
    ): ArcRuntimeState? {
        activeArc
            ?.takeIf { it.sessionCountInArc > 0 && isWithinContinuationWindow(it, flowStartTimeMs) }
            ?.let { return it }

        recentlyEndedArc
            ?.takeIf { isWithinContinuationWindow(it, flowStartTimeMs) }
            ?.let { return it }

        // A planned Arc may be pre-created before its first Flow starts. It has no
        // previous Flow timestamp and is used only when no completed Arc can continue.
        return activeArc?.takeIf { it.sessionCountInArc == 0 }
    }

    fun isWithinContinuationWindow(arc: ArcRuntimeState, flowStartTimeMs: Long): Boolean {
        val gapMs = flowStartTimeMs - arc.lastSessionEndTimeMs
        return gapMs >= 0L && gapMs <= ArcRules.GRACE_WINDOW_MS
    }
}
