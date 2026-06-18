package com.kingkharnivore.skillz.model.ui

data class PlanArcFlowPickerItemUiModel(
    val id: Long,
    val title: String,
    val tagId: Long?,
    val tagName: String,
    val isSoftMode: Boolean,
    val targetMinutes: Int?,
    val launchWithSurge: Boolean,
    val pinned: Boolean
)