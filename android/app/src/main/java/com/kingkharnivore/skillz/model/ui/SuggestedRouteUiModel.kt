package com.kingkharnivore.skillz.model.ui

data class SuggestedRouteUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val approxMinutes: Int?,
    val steps: List<SuggestedRouteStepUiModel>
)

data class SuggestedRouteStepUiModel(
    val title: String,
    val tagName: String = "",
    val isSoftMode: Boolean = false,
    val targetMinutes: Int? = null,
    val launchWithSurge: Boolean = false
)