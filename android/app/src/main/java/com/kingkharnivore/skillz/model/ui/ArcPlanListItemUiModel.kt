package com.kingkharnivore.skillz.model.ui

data class ArcPlanListItemUiModel(
    val id: Long,
    val title: String,
    val isInStudio: Boolean,
    val launchCount: Int,
    val lastLaunchedAt: Long?,
    val steps: List<ArcPlanStepPreviewUiModel> = emptyList()
) {
    val stepCount: Int get() = steps.size
    val totalTargetMinutes: Int? get() = steps.mapNotNull { it.targetMinutes }.sum().takeIf { it > 0 }
    val hasSurge: Boolean get() = steps.any { it.launchWithSurge }
    val hasSoftFlow: Boolean get() = steps.any { it.isSoftMode }
}

data class ArcPlanStepPreviewUiModel(
    val title: String,
    val targetMinutes: Int?,
    val isSoftMode: Boolean,
    val launchWithSurge: Boolean
)
