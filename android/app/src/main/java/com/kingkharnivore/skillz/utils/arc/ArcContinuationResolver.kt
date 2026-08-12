package com.kingkharnivore.skillz.utils.arc

import com.kingkharnivore.skillz.ui.model.ArcRuntimeState

/** Authoritative, timestamp-based decision for attaching a new Flow to an Arc. */
object ArcContinuationResolver {
    fun resolveArcForNewFlow(
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?,
        flowStartTimeMs: Long
    ): ArcRuntimeState? {
        activeArc?.let { active ->
            if (active.sessionCountInArc == 0 || isWithinContinuationWindow(active, flowStartTimeMs)) {
                return active
            }
        }

        return recentlyEndedArc?.takeIf { isWithinContinuationWindow(it, flowStartTimeMs) }
    }

    fun isWithinContinuationWindow(arc: ArcRuntimeState, flowStartTimeMs: Long): Boolean {
        val gapMs = flowStartTimeMs - arc.lastSessionEndTimeMs
        return gapMs >= 0L && gapMs <= ArcRules.GRACE_WINDOW_MS
    }
}
