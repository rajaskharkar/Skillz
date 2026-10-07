package com.kingkharnivore.skillz.model.ui

data class FlowPlanListItemUiModel(
    val id: Long,
    val title: String,
    val tagId: Long?,
    val tagName: String,
    val isSoftMode: Boolean,
    val targetMinutes: Int?,
    val launchWithSurge: Boolean,
    val pinned: Boolean,
    val launchCount: Int,
    val lastLaunchedAt: Long?,
    val kind: String = com.kingkharnivore.skillz.data.model.entity.HorizonKind.HABIT,
    val completionCount: Int = 0,
    val lastCompletedAt: Long? = null,
    val mode: com.kingkharnivore.skillz.model.FlowMode = com.kingkharnivore.skillz.model.FlowMode.fromSoft(isSoftMode)
)