package com.kingkharnivore.skillz.model.state.flow

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity

/** A fresh explicit launch may replace an idle draft; recreating its screen must restore edits. */
internal fun shouldApplyLaunchToDraft(
    ongoing: OngoingSessionEntity,
    hasLaunchOverrides: Boolean,
    launchAlreadyApplied: Boolean
): Boolean = hasLaunchOverrides && !launchAlreadyApplied &&
    !ongoing.isInFlowMode && !ongoing.isRunning &&
    ongoing.accumulatedBeforeStartMs == 0L && ongoing.baseStartTimeMs == null
