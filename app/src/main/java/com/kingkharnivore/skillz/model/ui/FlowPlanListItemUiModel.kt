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
    val lastLaunchedAt: Long?
)