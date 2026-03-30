package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.FlowPlanRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.model.state.paths.PathsPrimaryTab
import com.kingkharnivore.skillz.model.state.paths.PathsTimeLens
import com.kingkharnivore.skillz.model.state.paths.PathsUiState
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
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val trimmedTitle = title.trim()
            val trimmedTag = tagName.trim()

            if (trimmedTitle.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Title is required.") }
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
                    isSoftMode = isSoftMode
                )

                isSaving.value = false
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = null
                    )
                }
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

    fun onFlowPlanLaunched(planId: Long) {
        viewModelScope.launch {
            try {
                flowPlanRepository.markLaunched(planId)
            } catch (_: Exception) {
                // Launch should still continue even if stat tracking fails.
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

    private fun observePaths() {
        viewModelScope.launch {
            combine(
                combine(
                    flowPlanRepository.getActiveFlowPlans(),
                    flowPlanRepository.getArchivedFlowPlans(),
                    journeyRepository.getAllTags()
                ) { activeFlowPlans, dreamFlowPlans, tags ->
                    Triple(activeFlowPlans, dreamFlowPlans, tags)
                },
                combine(
                    selectedPrimaryTab,
                    selectedTimeLens,
                    isSaving
                ) { primaryTab, timeLens, saving ->
                    Triple(primaryTab, timeLens, saving)
                }
            ) { dataTriple, uiTriple ->
                val (activeFlowPlans, dreamFlowPlans, tags) = dataTriple
                val (primaryTab, timeLens, saving) = uiTriple

                PathsUiState(
                    isLoading = false,
                    isSaving = saving,
                    errorMessage = _uiState.value.errorMessage,
                    selectedPrimaryTab = primaryTab,
                    selectedTimeLens = timeLens,
                    flowPlans = activeFlowPlans.toUiModels(tags),
                    dreamFlowPlans = dreamFlowPlans.toUiModels(tags),
                    tags = tags.toTagUiModels()
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

    private fun List<FlowPlanEntity>.toUiModels(
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
                pinned = plan.pinned,
                launchCount = plan.launchCount,
                lastLaunchedAt = plan.lastLaunchedAt
            )
        }
    }

    private fun List<TagEntity>.toTagUiModels(): List<TagUiModel> =
        map { TagUiModel(id = it.id, name = it.name) }
}