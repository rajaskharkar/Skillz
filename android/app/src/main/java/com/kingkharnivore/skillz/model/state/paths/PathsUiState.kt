package com.kingkharnivore.skillz.model.state.paths

import com.kingkharnivore.skillz.data.model.entity.*
import com.kingkharnivore.skillz.model.ui.FlowPlanListItemUiModel
import com.kingkharnivore.skillz.viewmodel.TagUiModel

enum class HorizonSection { PLANS, PLANNED_ARCS }

enum class PathsPrimaryTab { HABITS, PLANS, MEMORIES }

data class MemoryFlowUi(val receipt: HorizonMemoryEntity, val session: SessionEntity?, val tagName: String)
data class MemoryGroupUi(val sourcePlanId: Long, val kind: String, val title: String, val flows: List<MemoryFlowUi>) {
    val key get() = "$kind:$sourcePlanId"
    val latest get() = flows.first()
}

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
    val tags: List<TagUiModel> = emptyList(),
    val ongoing: OngoingSessionEntity? = null
)
