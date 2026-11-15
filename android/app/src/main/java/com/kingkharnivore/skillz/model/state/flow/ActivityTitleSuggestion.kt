package com.kingkharnivore.skillz.model.state.flow

import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.data.model.entity.HorizonKind
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemUiModel
import com.kingkharnivore.skillz.data.model.entity.PulseGroveStatusValues

data class ActivityTitleSuggestion(
    val id: Long,
    val title: String,
    val tagName: String?,
    val kind: String = HorizonKind.HABIT,
    val mode: FlowMode = FlowMode.FLOW
) {
    val key: String get() = "$kind:$id"
}

internal fun activityTitleSuggestions(
    plans: List<FlowPlanEntity>,
    tags: List<TagEntity>,
    state: FlowUiState,
    ideas: List<IdeaGroveItemUiModel> = emptyList()
): List<ActivityTitleSuggestion> {
    val query = state.title.trim()
    if (query.isEmpty()) return emptyList()
    val tagsById = tags.associateBy { it.id }
    val activities = plans.asSequence()
        .filter { it.kind in setOf(HorizonKind.HABIT, HorizonKind.PLAN) && !it.archived && it.completedAt == null }
        .filter { !state.isModeLocked || it.mode == state.mode }
        .filter { it.title.contains(query, ignoreCase = true) }
        .filterNot { it.id == state.originPlanId && it.title == state.title }
        .sortedByDescending { it.title.startsWith(query, ignoreCase = true) }
        .map { ActivityTitleSuggestion(it.id, it.title, tagsById[it.tagId]?.name, it.kind, it.mode) }
        .toList()
    val activeIdeas = ideas.asSequence()
        .filter { it.groveStatus == PulseGroveStatusValues.ALIVE }
        .filter { it.title.contains(query, ignoreCase = true) }
        .filterNot { it.pulseId == state.originPulseId && it.title == state.title }
        // Ideas have no prescribed timer mode. Keep the current Flow, including a running timer.
        .map { ActivityTitleSuggestion(it.pulseId, it.title, it.journeyName, HorizonKind.IDEA, state.mode) }
        .toList()
    return (activities + activeIdeas).sortedByDescending { it.title.startsWith(query, ignoreCase = true) }
}

internal fun FlowUiState.withActivityTitle(activity: ActivityTitleSuggestion): FlowUiState {
    if (activity.kind == HorizonKind.IDEA) return copy(
        title = activity.title,
        tagName = activity.tagName.orEmpty(),
        originPlanId = null,
        originPlanKind = null,
        originPulseId = activity.id,
        originPulseTitle = activity.title,
        originPulseJourneyName = activity.tagName
    )
    if (isModeLocked && mode != activity.mode) return this
    return copy(
        title = activity.title,
        tagName = activity.tagName.orEmpty(),
        originPlanId = activity.id,
        originPlanKind = activity.kind,
        originPulseId = null,
        originPulseTitle = null,
        originPulseJourneyName = null,
        mode = activity.mode,
        isSurgeOn = if (activity.mode == FlowMode.SOFT) false else isSurgeOn,
        surgePlannedMs = if (activity.mode == FlowMode.SOFT) null else surgePlannedMs
    )
}

internal fun FlowUiState.requiresSoftArcConfirmation(): Boolean =
    !isSoftMode && !isModeLocked && isInArc && (arcMultiplier ?: 1.0) > 1.0

private fun FlowUiState.withoutActivityLink(): FlowUiState = copy(
    originPlanId = null, originPlanKind = null,
    originPulseId = null, originPulseTitle = null, originPulseJourneyName = null
)

internal fun FlowUiState.withEditedTitle(title: String): FlowUiState =
    (if (title != this.title) withoutActivityLink() else this).copy(title = title)

internal fun FlowUiState.withEditedTagName(tagName: String): FlowUiState =
    (if (tagName != this.tagName) withoutActivityLink() else this).copy(tagName = tagName)

internal fun FlowUiState.withResetStopwatch(): FlowUiState = withoutActivityLink().copy(
    stopwatch = StopwatchState(isRunning = false, elapsedMs = 0L)
)
