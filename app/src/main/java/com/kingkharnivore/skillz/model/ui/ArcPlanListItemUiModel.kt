package com.kingkharnivore.skillz.model.ui

data class ArcPlanListItemUiModel(
    val id: Long,
    val title: String,
    val isInStudio: Boolean,
    val launchCount: Int,
    val lastLaunchedAt: Long?
)