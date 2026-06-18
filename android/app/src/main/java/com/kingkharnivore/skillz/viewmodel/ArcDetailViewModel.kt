package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.ActiveArcRunEntity
import com.kingkharnivore.skillz.data.model.entity.ArcPlanEntity
import com.kingkharnivore.skillz.data.model.entity.ArcPlanStepEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.ActiveArcRunRepository
import com.kingkharnivore.skillz.data.repository.ArcPlanRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.ui.navigation.SkillzDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArcDetailStepUiModel(
    val id: Long,
    val orderIndex: Int,
    val title: String,
    val tagName: String,
    val isSoftMode: Boolean,
    val targetMinutes: Int?,
    val launchWithSurge: Boolean
)

data class ArcDetailLaunchPayload(
    val title: String,
    val tagName: String?,
    val isSoftMode: Boolean,
    val plannedArcTitle: String,
    val plannedArcStepIndex: Int,
    val plannedArcTotalSteps: Int
)

data class ArcDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val arcId: Long = 0L,
    val title: String = "",
    val isInStudio: Boolean = false,
    val launchCount: Int = 0,
    val recurrenceType: String = ArcPlanEntity.RECURRENCE_ONE_TIME,
    val recurrenceDaysCsv: String = "",
    val steps: List<ArcDetailStepUiModel> = emptyList(),

    // recovery / polish
    val hasActiveRun: Boolean = false,
    val activeRunStepIndex: Int? = null,
    val activeRunTotalSteps: Int? = null
) {
    val totalMinutes: Int
        get() = steps.sumOf { it.targetMinutes ?: 0 }

    val untimedCount: Int
        get() = steps.count { it.targetMinutes == null }

    val softCount: Int
        get() = steps.count { it.isSoftMode }

    val surgeCount: Int
        get() = steps.count { it.launchWithSurge }

    val firstStep: ArcDetailStepUiModel?
        get() = steps.firstOrNull()

    val activeStepNumber: Int?
        get() = activeRunStepIndex?.plus(1)
}

@HiltViewModel
class ArcDetailViewModel @Inject constructor(
    private val arcPlanRepository: ArcPlanRepository,
    private val journeyRepository: JourneyRepository,
    private val activeArcRunRepository: ActiveArcRunRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val arcPlanId: Long =
        savedStateHandle.get<String>(SkillzDestinations.ARC_DETAIL_ARG_ID)
            ?.toLongOrNull()
            ?: 0L

    private val _uiState = MutableStateFlow(ArcDetailUiState())
    val uiState: StateFlow<ArcDetailUiState> = _uiState.asStateFlow()

    init {
        observe()
    }

    private fun observe() {
        viewModelScope.launch {
            val plan = arcPlanRepository.getArcPlanById(arcPlanId)
            if (plan == null) {
                _uiState.value = ArcDetailUiState(
                    isLoading = false,
                    errorMessage = "Arc not found."
                )
                return@launch
            }

            combine(
                arcPlanRepository.getStepsForArcPlan(arcPlanId),
                journeyRepository.getAllTags(),
                activeArcRunRepository.getActiveArcRun()
            ) { steps, tags, activeRun ->
                buildState(plan, steps, tags, activeRun)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun buildState(
        plan: ArcPlanEntity,
        steps: List<ArcPlanStepEntity>,
        tags: List<TagEntity>,
        activeRun: ActiveArcRunEntity?
    ): ArcDetailUiState {
        val tagNameById = tags.associate { it.id to it.name }
        val isThisArcActive = activeRun?.arcPlanId == plan.id

        return ArcDetailUiState(
            isLoading = false,
            arcId = plan.id,
            title = plan.title,
            isInStudio = plan.isInStudio,
            launchCount = plan.launchCount,
            recurrenceType = plan.recurrenceType,
            recurrenceDaysCsv = plan.recurrenceDaysCsv,
            hasActiveRun = isThisArcActive,
            activeRunStepIndex = activeRun?.currentStepIndex?.takeIf { isThisArcActive },
            activeRunTotalSteps = activeRun?.totalSteps?.takeIf { isThisArcActive },
            steps = steps.sortedBy { it.orderIndex }.map { step ->
                ArcDetailStepUiModel(
                    id = step.id,
                    orderIndex = step.orderIndex,
                    title = step.titleSnapshot,
                    tagName = step.tagIdSnapshot?.let { tagNameById[it] }.orEmpty(),
                    isSoftMode = step.isSoftModeSnapshot,
                    targetMinutes = step.targetMinutesSnapshot,
                    launchWithSurge = step.launchWithSurgeSnapshot
                )
            }
        )
    }

    fun addToStudio() {
        viewModelScope.launch {
            try {
                arcPlanRepository.setInStudio(arcPlanId, true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to add arc to Studio.")
                }
            }
        }
    }

    fun removeFromStudio() {
        viewModelScope.launch {
            try {
                arcPlanRepository.setInStudio(arcPlanId, false)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to remove arc from Studio.")
                }
            }
        }
    }

    fun beginArc(onReady: (ArcDetailLaunchPayload) -> Unit) {
        val state = _uiState.value
        val resumeIndex = state.activeRunStepIndex

        if (state.steps.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "This arc has no steps.") }
            return
        }

        val targetIndex = when {
            resumeIndex != null && resumeIndex in state.steps.indices -> resumeIndex
            else -> 0
        }

        val targetStep = state.steps[targetIndex]

        viewModelScope.launch {
            try {
                // only mark launched + start run fresh if not already active for this arc
                if (!state.hasActiveRun) {
                    arcPlanRepository.markLaunched(state.arcId)

                    activeArcRunRepository.startRun(
                        arcPlanId = state.arcId,
                        arcTitle = state.title,
                        currentStepIndex = 0,
                        totalSteps = state.steps.size,
                        currentStepTitle = state.steps.first().title,
                        currentTagName = state.steps.first().tagName,
                        currentIsSoftMode = state.steps.first().isSoftMode
                    )
                }

                onReady(
                    ArcDetailLaunchPayload(
                        title = targetStep.title,
                        tagName = targetStep.tagName.takeIf { it.isNotBlank() },
                        isSoftMode = targetStep.isSoftMode,
                        plannedArcTitle = state.title,
                        plannedArcStepIndex = targetIndex,
                        plannedArcTotalSteps = state.steps.size
                    )
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to begin arc.")
                }
            }
        }
    }

    fun restartArc(onReady: (ArcDetailLaunchPayload) -> Unit) {
        val state = _uiState.value
        val first = state.firstStep ?: run {
            _uiState.update { it.copy(errorMessage = "This arc has no steps.") }
            return
        }

        viewModelScope.launch {
            try {
                arcPlanRepository.markLaunched(state.arcId)

                activeArcRunRepository.startRun(
                    arcPlanId = state.arcId,
                    arcTitle = state.title,
                    currentStepIndex = 0,
                    totalSteps = state.steps.size,
                    currentStepTitle = first.title,
                    currentTagName = first.tagName,
                    currentIsSoftMode = first.isSoftMode
                )

                onReady(
                    ArcDetailLaunchPayload(
                        title = first.title,
                        tagName = first.tagName.takeIf { it.isNotBlank() },
                        isSoftMode = first.isSoftMode,
                        plannedArcTitle = state.title,
                        plannedArcStepIndex = 0,
                        plannedArcTotalSteps = state.steps.size
                    )
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to restart arc.")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}