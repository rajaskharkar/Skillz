package com.kingkharnivore.skillz.model.state.paths

import com.kingkharnivore.skillz.model.ui.FlowPlanListItemUiModel
import com.kingkharnivore.skillz.viewmodel.TagUiModel

enum class PathsPrimaryTab {
    FLOWS,
    ARCS
}

enum class PathsTimeLens {
    DAY,
    WEEK,
    MONTH
}

data class PathsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val selectedPrimaryTab: PathsPrimaryTab = PathsPrimaryTab.FLOWS,
    val selectedTimeLens: PathsTimeLens = PathsTimeLens.DAY,
    val flowPlans: List<FlowPlanListItemUiModel> = emptyList(),
    val dreamFlowPlans: List<FlowPlanListItemUiModel> = emptyList(),
    val tags: List<TagUiModel> = emptyList()
)