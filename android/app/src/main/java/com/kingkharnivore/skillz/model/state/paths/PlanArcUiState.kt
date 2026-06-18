package com.kingkharnivore.skillz.model.state.paths

import com.kingkharnivore.skillz.model.ui.PlanArcFlowPickerItemUiModel
import com.kingkharnivore.skillz.viewmodel.TagUiModel

data class PlanArcUiState(
    val title: String = "",
    val currentStep: Int = 0,
    val totalSteps: Int = 5,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val availableFlows: List<PlanArcFlowPickerItemUiModel> = emptyList(),
    val availableTags: List<TagUiModel> = emptyList(),
    val selectedTagId: Long? = null,
    val selectedFlowIdsInOrder: List<Long> = emptyList(),
    val targetMinutesTextByFlowId: Map<Long, String> = emptyMap(),
    val launchWithSurgeByFlowId: Map<Long, Boolean> = emptyMap(),
    val recurrenceType: String = "one_time",
    val recurrenceDays: Set<Int> = emptySet(),
    val isEditing: Boolean = false
)