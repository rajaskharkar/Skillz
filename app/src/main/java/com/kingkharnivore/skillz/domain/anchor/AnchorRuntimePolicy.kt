package com.kingkharnivore.skillz.domain.anchor

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity

const val ANCHOR_MEANINGFUL_FLOW_THRESHOLD_MS = 1_000L

fun effectiveFlowElapsedMs(entity: OngoingSessionEntity, now: Long): Long {
    val runningDelta = if (entity.isRunning && entity.baseStartTimeMs != null) {
        (now - entity.baseStartTimeMs).coerceAtLeast(0L)
    } else {
        0L
    }
    return entity.accumulatedBeforeStartMs + runningDelta
}

fun hasMeaningfulActiveFlow(entity: OngoingSessionEntity?, now: Long): Boolean =
    entity != null && entity.isInFlowMode && effectiveFlowElapsedMs(entity, now) >= ANCHOR_MEANINGFUL_FLOW_THRESHOLD_MS

data class AnchorRuntimeInput(
    val entity: OngoingSessionEntity?,
    val now: Long,
    val mode: AnchorMode,
    val globallyEnabled: Boolean,
    val selectedPackageCount: Int,
    val usageAccessGranted: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val accessibilityEnabled: Boolean = false
)

object AnchorRuntimePolicy {
    fun anchorEnabled(input: AnchorRuntimeInput): Boolean {
        val entity = input.entity ?: return false
        return !entity.anchorDisabledForFlow && (entity.anchorEnabledForFlow || input.globallyEnabled)
    }

    fun shouldRunGuide(input: AnchorRuntimeInput): Boolean {
        val entity = input.entity ?: return false
        return input.mode == AnchorMode.GUIDE &&
            hasMeaningfulActiveFlow(entity, input.now) &&
            entity.isRunning &&
            anchorEnabled(input) &&
            !entity.anchorPaused &&
            !isBreakActive(entity, input.now) &&
            input.usageAccessGranted &&
            input.selectedPackageCount > 0
    }

    fun shouldRunGuard(input: AnchorRuntimeInput): Boolean {
        val entity = input.entity ?: return false
        return input.mode == AnchorMode.GUARD &&
            hasMeaningfulActiveFlow(entity, input.now) &&
            entity.isRunning &&
            anchorEnabled(input) &&
            !entity.anchorPaused &&
            !isBreakActive(entity, input.now) &&
            input.accessibilityEnabled &&
            input.selectedPackageCount > 0
    }

    fun isBreakActive(entity: OngoingSessionEntity, now: Long): Boolean =
        entity.anchorBreakEndsAtMs?.let { it > now } == true
}
