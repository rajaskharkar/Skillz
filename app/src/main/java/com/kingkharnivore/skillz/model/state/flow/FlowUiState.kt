package com.kingkharnivore.skillz.model.state.flow

data class FlowUiState(
    val title: String = "",
    val description: String = "",
    val tagName: String = "",
    val stopwatch: StopwatchState = StopwatchState(),
    val isInFlowMode: Boolean = false,
    val isSoftMode: Boolean = false,
    val isSurgeOn: Boolean = false,
    val surgePlannedMs: Long? = null,
    val showScoreUi: Boolean = false,
    val calmMode: Boolean = false,
    val isInArc: Boolean = false,
    val arcIsPending: Boolean = false,
    val arcMultiplier: Double? = null,
    val arcProgressMs: Long = 0L,
    val arcNextIndex: Int? = null,
    val arcGraceRemainingMs: Long? = null,
    val arcPauseRemainingMs: Long? = null,
    val plannedArcTitle: String? = null,
    val plannedArcStepIndex: Int? = null,
    val plannedArcTotalSteps: Int? = null
)