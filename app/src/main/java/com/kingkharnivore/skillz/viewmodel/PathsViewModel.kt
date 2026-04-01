package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.ArcPlanRepository
import com.kingkharnivore.skillz.data.repository.FlowPlanRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.model.state.paths.PathsPrimaryTab
import com.kingkharnivore.skillz.model.state.paths.PathsTimeLens
import com.kingkharnivore.skillz.model.state.paths.PathsUiState
import com.kingkharnivore.skillz.model.ui.ArcPlanListItemUiModel
import com.kingkharnivore.skillz.model.ui.FlowPlanListItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PathsViewModel @Inject constructor(
    private val flowPlanRepository: FlowPlanRepository,
    private val arcPlanRepository: ArcPlanRepository,
    private val journeyRepository: JourneyRepository
) : ViewModel() {

    private val selectedPrimaryTab = MutableStateFlow(PathsPrimaryTab.FLOWS)
    private val selectedTimeLens = MutableStateFlow(PathsTimeLens.DAY)
    private val isSaving = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(PathsUiState())
    val uiState: StateFlow<PathsUiState> = _uiState.asStateFlow()

    init {
        observePaths()
    }

    fun onPrimaryTabSelected(tab: PathsPrimaryTab) {
        selectedPrimaryTab.value = tab
    }

    fun onTimeLensSelected(lens: PathsTimeLens) {
        selectedTimeLens.value = lens
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun createFlowPlan(
        title: String,
        tagName: String,
        isSoftMode: Boolean,
        targetMinutesText: String,
        launchWithSurge: Boolean,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val trimmedTitle = title.trim()
            val trimmedTag = tagName.trim()
            val parsedTargetMinutes = targetMinutesText.trim().toIntOrNull()

            if (trimmedTitle.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Title is required.") }
                return@launch
            }

            if (parsedTargetMinutes != null && parsedTargetMinutes <= 0) {
                _uiState.update { it.copy(errorMessage = "Target minutes must be greater than 0.") }
                return@launch
            }

            isSaving.value = true
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val tagId = if (trimmedTag.isBlank()) {
                    null
                } else {
                    journeyRepository.getOrCreateTagId(trimmedTag)
                }

                flowPlanRepository.createFlowPlan(
                    title = trimmedTitle,
                    tagId = tagId,
                    isSoftMode = isSoftMode,
                    targetMinutes = parsedTargetMinutes,
                    launchWithSurge = launchWithSurge
                )

                isSaving.value = false
                _uiState.update { it.copy(isSaving = false, errorMessage = null) }
                onSaved()
            } catch (e: Exception) {
                isSaving.value = false
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save planned flow."
                    )
                }
            }
        }
    }

    fun moveFlowPlanToDreams(planId: Long) {
        viewModelScope.launch {
            try {
                flowPlanRepository.setArchived(planId, true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to move flow into Dreams.")
                }
            }
        }
    }

    fun restoreFlowPlanFromDreams(planId: Long) {
        viewModelScope.launch {
            try {
                flowPlanRepository.setArchived(planId, false)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to bring flow back from Dreams.")
                }
            }
        }
    }

    fun deleteFlowPlan(planId: Long) {
        viewModelScope.launch {
            try {
                flowPlanRepository.deleteFlowPlanById(planId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to delete planned flow.")
                }
            }
        }
    }

    fun setFlowPlanPinned(planId: Long, pinned: Boolean) {
        viewModelScope.launch {
            try {
                flowPlanRepository.setPinned(planId, pinned)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to update pin state.")
                }
            }
        }
    }

    fun onFlowPlanLaunched(planId: Long) {
        viewModelScope.launch {
            try {
                flowPlanRepository.markLaunched(planId)
            } catch (_: Exception) {
            }
        }
    }

    fun addArcToStudio(arcPlanId: Long) {
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

    fun removeArcFromStudio(arcPlanId: Long) {
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

    private fun observePaths() {
        viewModelScope.launch {
            combine(
                combine(
                    flowPlanRepository.getActiveFlowPlans(),
                    flowPlanRepository.getArchivedFlowPlans(),
                    arcPlanRepository.getStudioArcPlans(),
                    arcPlanRepository.getActiveArcPlans(),
                    journeyRepository.getAllTags()
                ) { activeFlowPlans, dreamFlowPlans, studioArcPlans, activeArcPlans, tags ->
                    DataBundle(
                        activeFlowPlans = activeFlowPlans,
                        dreamFlowPlans = dreamFlowPlans,
                        studioArcPlans = studioArcPlans,
                        activeArcPlans = activeArcPlans,
                        tags = tags
                    )
                },
                combine(
                    selectedPrimaryTab,
                    selectedTimeLens,
                    isSaving
                ) { primaryTab, timeLens, saving ->
                    Triple(primaryTab, timeLens, saving)
                }
            ) { data, uiBits ->
                val (primaryTab, timeLens, saving) = uiBits

                val studioIds = data.studioArcPlans.map { it.id }.toSet()
                val nonStudioArcPlans = data.activeArcPlans.filterNot { it.id in studioIds }

                PathsUiState(
                    isLoading = false,
                    isSaving = saving,
                    errorMessage = _uiState.value.errorMessage,
                    selectedPrimaryTab = primaryTab,
                    selectedTimeLens = timeLens,
                    flowPlans = data.activeFlowPlans.toFlowUiModels(data.tags),
                    dreamFlowPlans = data.dreamFlowPlans.toFlowUiModels(data.tags),
                    studioArcPlans = data.studioArcPlans.toArcUiModels(),
                    arcPlans = nonStudioArcPlans.toArcUiModels(),
                    tags = data.tags.toTagUiModels()
                )
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSaving = false,
                            errorMessage = e.message ?: "Something went wrong"
                        )
                    }
                }
                .collect { newState ->
                    _uiState.value = newState
                }
        }
    }

    private fun List<FlowPlanEntity>.toFlowUiModels(
        tags: List<TagEntity>
    ): List<FlowPlanListItemUiModel> {
        val tagNameById = tags.associate { it.id to it.name }

        return map { plan ->
            FlowPlanListItemUiModel(
                id = plan.id,
                title = plan.title,
                tagId = plan.tagId,
                tagName = plan.tagId?.let { tagNameById[it] }.orEmpty(),
                isSoftMode = plan.isSoftMode,
                targetMinutes = plan.targetMinutes,
                launchWithSurge = plan.launchWithSurge,
                pinned = plan.pinned,
                launchCount = plan.launchCount,
                lastLaunchedAt = plan.lastLaunchedAt
            )
        }
    }

    private fun List<com.kingkharnivore.skillz.data.model.entity.ArcPlanEntity>.toArcUiModels():
            List<ArcPlanListItemUiModel> =
        map { plan ->
            ArcPlanListItemUiModel(
                id = plan.id,
                title = plan.title,
                isInStudio = plan.isInStudio,
                launchCount = plan.launchCount,
                lastLaunchedAt = plan.lastLaunchedAt
            )
        }

    private fun List<TagEntity>.toTagUiModels(): List<TagUiModel> =
        map { TagUiModel(id = it.id, name = it.name) }

    private data class DataBundle(
        val activeFlowPlans: List<FlowPlanEntity>,
        val dreamFlowPlans: List<FlowPlanEntity>,
        val studioArcPlans: List<com.kingkharnivore.skillz.data.model.entity.ArcPlanEntity>,
        val activeArcPlans: List<com.kingkharnivore.skillz.data.model.entity.ArcPlanEntity>,
        val tags: List<TagEntity>
    )

    fun deleteArcPlan(arcPlanId: Long) {
        viewModelScope.launch {
            try {
                arcPlanRepository.deleteArcPlanById(arcPlanId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to delete arc.")
                }
            }
        }
    }
}