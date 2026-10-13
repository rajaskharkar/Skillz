package com.kingkharnivore.skillz.model.state.flow

import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.data.model.entity.HorizonKind
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.model.FlowMode

data class ActivityTitleSuggestion(
    val id: Long,
    val title: String,
    val tagName: String?,
    val kind: String = HorizonKind.HABIT,
    val mode: FlowMode = FlowMode.FLOW
)

internal fun activityTitleSuggestions(
    plans: List<FlowPlanEntity>,
    tags: List<TagEntity>,
    state: FlowUiState
): List<ActivityTitleSuggestion> {
    val query = state.title.trim()
    if (query.isEmpty()) return emptyList()
    val tagsById = tags.associateBy { it.id }
    return plans.asSequence()
        .filter { it.kind in setOf(HorizonKind.HABIT, HorizonKind.PLAN) && !it.archived && it.completedAt == null }
        .filter { !state.isModeLocked || it.mode == state.mode }
        .filter { it.title.contains(query, ignoreCase = true) }
        .filterNot { it.id == state.originPlanId && it.title == state.title }
        .sortedByDescending { it.title.startsWith(query, ignoreCase = true) }
        .map { ActivityTitleSuggestion(it.id, it.title, tagsById[it.tagId]?.name, it.kind, it.mode) }
        .toList()
}

internal fun FlowUiState.withActivityTitle(activity: ActivityTitleSuggestion): FlowUiState {
    if (isModeLocked && mode != activity.mode) return this
    return copy(
        title = activity.title,
        tagName = activity.tagName.orEmpty(),
        originPlanId = activity.id,
        originPlanKind = activity.kind,
        mode = activity.mode,
        isSurgeOn = if (activity.mode == FlowMode.SOFT) false else isSurgeOn,
        surgePlannedMs = if (activity.mode == FlowMode.SOFT) null else surgePlannedMs
    )
}

internal fun FlowUiState.requiresSoftArcConfirmation(): Boolean =
    !isSoftMode && !isModeLocked && isInArc && (arcMultiplier ?: 1.0) > 1.0

private fun FlowUiState.withoutHabitLink(): FlowUiState =
    if (originPlanKind == HorizonKind.HABIT) copy(originPlanId = null, originPlanKind = null) else this

internal fun FlowUiState.withEditedTitle(title: String): FlowUiState =
    (if (title != this.title) withoutHabitLink() else this).copy(title = title)

internal fun FlowUiState.withEditedTagName(tagName: String): FlowUiState =
    (if (tagName != this.tagName) withoutHabitLink() else this).copy(tagName = tagName)

internal fun FlowUiState.withResetStopwatch(): FlowUiState = withoutHabitLink().copy(
    stopwatch = StopwatchState(isRunning = false, elapsedMs = 0L)
)
