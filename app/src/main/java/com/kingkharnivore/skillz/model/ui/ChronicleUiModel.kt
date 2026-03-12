package com.kingkharnivore.skillz.model.ui

import androidx.compose.ui.graphics.Color

sealed interface ChronicleUiModel {
    val key: String

    data class StandaloneFlow(
        val flow: FlowListItemUiModel
    ) : ChronicleUiModel {
        override val key: String = "flow_${flow.sessionId}"
    }

    data class ArcGroup(
        val arcId: Long,
        val headerAccentColor: Color?,
        val totalArcDurationMs: Long,
        val totalArcScore: Int,
        val peakMultiplier: Double?,
        val visibleFlows: List<ArcFlowItemUiModel>,
        val hiddenFlowsCount: Int,
        val totalFlowsCount: Int,
        val filteredJourneyDurationMs: Long? = null,
        val filteredJourneyPercentOfArc: Int? = null
    ) : ChronicleUiModel {
        override val key: String = "arc_$arcId"
    }
}

data class ArcFlowItemUiModel(
    val flow: FlowListItemUiModel,
    val isFirstVisibleInArc: Boolean,
    val isLastVisibleInArc: Boolean
)