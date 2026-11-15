package com.kingkharnivore.skillz.model.state.paths

import com.kingkharnivore.skillz.data.model.entity.*
import com.kingkharnivore.skillz.model.ui.FlowPlanListItemUiModel
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemUiModel
import com.kingkharnivore.skillz.viewmodel.TagUiModel

enum class HorizonSection { PLANS, PLANNED_ARCS }

enum class PathsPrimaryTab { HABITS, PLANS, MEMORIES }

data class MemoryFlowUi(val receipt: HorizonMemoryEntity, val session: SessionEntity?, val tagName: String)
data class MemoryGroupUi(val sourcePlanId: Long, val kind: String, val title: String, val flows: List<MemoryFlowUi>) {
    val key get() = "$kind:$sourcePlanId"
    val latest get() = flows.first()
}

/** Ideas retain their Grove identity and live history instead of duplicating memory receipts. */
sealed interface MemoryTimelineItem {
    val key: String
    val lastWorkedAt: Long

    data class Activity(val memory: MemoryGroupUi) : MemoryTimelineItem {
        override val key get() = memory.key
        override val lastWorkedAt get() = memory.latest.receipt.completedAt
    }
    data class Idea(val idea: IdeaGroveItemUiModel) : MemoryTimelineItem {
        override val key get() = "IDEA:${idea.pulseId}"
        override val lastWorkedAt get() = idea.lastWorkedAt ?: idea.createdAt
    }
}

internal fun memoryTimeline(memories: List<MemoryGroupUi>, ideas: List<IdeaGroveItemUiModel>): List<MemoryTimelineItem> =
    (memories.map { MemoryTimelineItem.Activity(it) } +
        ideas.filter { it.flows.isNotEmpty() }.map { MemoryTimelineItem.Idea(it) })
        .sortedWith(compareByDescending<MemoryTimelineItem> { it.lastWorkedAt }.thenBy { it.key })

data class PathsUiState(
    val isLoading: Boolean = true,
    val selectedSection: HorizonSection = HorizonSection.PLANS,
    val arcPlans: List<com.kingkharnivore.skillz.model.ui.ArcPlanListItemUiModel> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val selectedPrimaryTab: PathsPrimaryTab = PathsPrimaryTab.HABITS,
    val flowPlans: List<FlowPlanListItemUiModel> = emptyList(),
    val dreamFlowPlans: List<FlowPlanListItemUiModel> = emptyList(),
    val memories: List<MemoryGroupUi> = emptyList(),
    val ideaMemories: List<IdeaGroveItemUiModel> = emptyList(),
    val tags: List<TagUiModel> = emptyList(),
    val ongoing: OngoingSessionEntity? = null
)
