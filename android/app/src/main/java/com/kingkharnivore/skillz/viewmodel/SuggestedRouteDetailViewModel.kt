package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.ArcPlanEntity
import com.kingkharnivore.skillz.data.model.entity.ArcPlanStepEntity
import com.kingkharnivore.skillz.data.repository.ActiveArcRunRepository
import com.kingkharnivore.skillz.data.repository.ArcPlanRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.model.ui.SuggestedRouteUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuggestedRouteDetailUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SuggestedRouteDetailViewModel @Inject constructor(
    private val arcPlanRepository: ArcPlanRepository,
    private val journeyRepository: JourneyRepository,
    private val activeArcRunRepository: ActiveArcRunRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuggestedRouteDetailUiState())
    val uiState: StateFlow<SuggestedRouteDetailUiState> = _uiState.asStateFlow()

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveSuggestedRoute(
        route: SuggestedRouteUiModel,
        addToStudio: Boolean,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val steps = route.steps.mapIndexed { index, step ->
                    val tagId = step.tagName
                        .trim()
                        .takeIf { it.isNotBlank() }
                        ?.let { journeyRepository.getOrCreateTagId(it) }

                    ArcPlanStepEntity(
                        arcPlanId = 0L,
                        orderIndex = index,
                        sourceFlowPlanId = null,
                        titleSnapshot = step.title,
                        tagIdSnapshot = tagId,
                        isSoftModeSnapshot = false,
                        targetMinutesSnapshot = step.targetMinutes,
                        launchWithSurgeSnapshot =
                            step.launchWithSurge && step.targetMinutes != null,
                        linkState = ArcPlanStepEntity.LINK_STATE_CUSTOMIZED
                    )
                }

                arcPlanRepository.createArcPlanWithSteps(
                    title = route.title,
                    steps = steps,
                    isInStudio = addToStudio,
                    recurrenceType = ArcPlanEntity.RECURRENCE_ONE_TIME,
                    recurrenceDaysCsv = ""
                )

                _uiState.update { it.copy(isSaving = false) }
                onSaved()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save suggested route."
                    )
                }
            }
        }
    }

    fun beginSuggestedRoute(
        route: SuggestedRouteUiModel,
        onReady: (ArcDetailLaunchPayload) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val steps = route.steps.mapIndexed { index, step ->
                    val tagId = step.tagName
                        .trim()
                        .takeIf { it.isNotBlank() }
                        ?.let { journeyRepository.getOrCreateTagId(it) }

                    ArcPlanStepEntity(
                        arcPlanId = 0L,
                        orderIndex = index,
                        sourceFlowPlanId = null,
                        titleSnapshot = step.title,
                        tagIdSnapshot = tagId,
                        isSoftModeSnapshot = false,
                        targetMinutesSnapshot = step.targetMinutes,
                        launchWithSurgeSnapshot =
                            step.launchWithSurge && step.targetMinutes != null,
                        linkState = ArcPlanStepEntity.LINK_STATE_CUSTOMIZED
                    )
                }

                val arcPlanId = arcPlanRepository.createArcPlanWithSteps(
                    title = route.title,
                    steps = steps,
                    isInStudio = false,
                    recurrenceType = ArcPlanEntity.RECURRENCE_ONE_TIME,
                    recurrenceDaysCsv = ""
                )

                val first = route.steps.firstOrNull()
                if (first == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "This route has no steps."
                        )
                    }
                    return@launch
                }

                activeArcRunRepository.startRun(
                    arcPlanId = arcPlanId,
                    arcTitle = route.title,
                    currentStepIndex = 0,
                    totalSteps = route.steps.size,
                    currentStepTitle = first.title,
                    currentTagName = first.tagName,
                    currentIsSoftMode = false
                )

                _uiState.update { it.copy(isSaving = false) }

                onReady(
                    ArcDetailLaunchPayload(
                        title = first.title,
                        tagName = first.tagName.takeIf { it.isNotBlank() },
                        isSoftMode = false,
                        plannedArcTitle = route.title,
                        plannedArcStepIndex = 0,
                        plannedArcTotalSteps = route.steps.size
                    )
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to begin suggested route."
                    )
                }
            }
        }
    }
}