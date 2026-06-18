package com.kingkharnivore.skillz.model.state.flow

import com.kingkharnivore.skillz.BuildConfig

data class FlowUiState(
    val title: String = "",
    val description: String = "",
    val tagName: String = "",
    val stopwatch: StopwatchState = StopwatchState(),
    val isInFlowMode: Boolean = false,
    val isSoftMode: Boolean = false,
    val isSurgeOn: Boolean = false,
    val surgePlannedMs: Long? = null,
    val showScoreUi: Boolean = BuildConfig.SHOW_SCORE,
    val calmMode: Boolean = false,
    val isInArc: Boolean = false,
    val arcIsPending: Boolean = false,
    val arcMultiplier: Double? = null,
    val arcProgressMs: Long = 0L,
    val arcNextIndex: Int? = null,
    val arcGraceRemainingMs: Long? = null,
    val arcPauseRemainingMs: Long? = null,
    val recentlyResumedArcMessage: String? = null,
    val plannedArcTitle: String? = null,
    val plannedArcStepIndex: Int? = null,
    val plannedArcTotalSteps: Int? = null,
    val originPulseId: Long? = null,
    val originPulseTitle: String? = null,
    val originPulseJourneyName: String? = null,
    val healthEnabledAtStart: Boolean = false,
    val healthPermissionGrantedAtStart: Boolean = false,
    val movementBonusEligibleAtStart: Boolean = false
)
