package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.ArcPlanEntity
import com.kingkharnivore.skillz.data.model.entity.ArcPlanStepEntity
import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.ArcPlanRepository
import com.kingkharnivore.skillz.data.repository.FlowPlanRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.model.state.paths.PlanArcUiState
import com.kingkharnivore.skillz.model.ui.PlanArcFlowPickerItemUiModel
import com.kingkharnivore.skillz.ui.navigation.SkillzDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanArcViewModel @Inject constructor(
    private val arcPlanRepository: ArcPlanRepository,
    private val flowPlanRepository: FlowPlanRepository,
    private val journeyRepository: JourneyRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editArcPlanId: Long? =
        savedStateHandle
            .get<Long>(SkillzDestinations.PLAN_ARC_ARG_EDIT_ID)
            ?.takeIf { it > 0L }

    private val _uiState = MutableStateFlow(PlanArcUiState())
    val uiState: StateFlow<PlanArcUiState> = _uiState.asStateFlow()

    init {
        observeAvailableFlows()
        preloadIfEditing()
    }

    private fun observeAvailableFlows() {
        viewModelScope.launch {
            combine(
                flowPlanRepository.getActiveFlowPlans(),
                journeyRepository.getAllTags()
            ) { flowPlans, tags ->
                flowPlans to tags
            }.collect { (flowPlans, tags) ->
                val uiFlows = flowPlans.toPickerUiModels(tags).filterNot { it.isSoftMode }
                val availableTags = tags
                    .filter { tag -> uiFlows.any { it.tagId == tag.id } }
                    .map { TagUiModel(id = it.id, name = it.name) }

                _uiState.update { current ->
                    val selectedIds = current.selectedFlowIdsInOrder
                        .filter { selectedId -> uiFlows.any { it.id == selectedId } }

                    current.copy(
                        availableFlows = uiFlows,
                        availableTags = availableTags,
                        selectedTagId = current.selectedTagId
                            ?.takeIf { selected -> availableTags.any { it.id == selected } },
                        selectedFlowIdsInOrder = selectedIds,
                        targetMinutesTextByFlowId = current.targetMinutesTextByFlowId
                            .filterKeys { it in selectedIds.toSet() },
                        launchWithSurgeByFlowId = current.launchWithSurgeByFlowId
                            .filterKeys { it in selectedIds.toSet() }
                    )
                }
            }
        }
    }

    private fun preloadIfEditing() {
        val arcId = editArcPlanId ?: return

        viewModelScope.launch {
            val arc = arcPlanRepository.getArcPlanById(arcId) ?: return@launch
            val steps = arcPlanRepository.getStepsForArcPlanOnce(arcId).sortedBy { it.orderIndex }

            _uiState.update { current ->
                current.copy(
                    title = arc.title,
                    isEditing = true,
                    recurrenceType = arc.recurrenceType,
                    recurrenceDays = arc.recurrenceDaysCsv
                        .split(",")
                        .mapNotNull { it.toIntOrNull() }
                        .toSet(),
                    selectedFlowIdsInOrder = steps.mapNotNull { it.sourceFlowPlanId },
                    targetMinutesTextByFlowId = steps
                        .mapNotNull { step ->
                            step.sourceFlowPlanId?.let { flowId ->
                                flowId to (step.targetMinutesSnapshot?.toString().orEmpty())
                            }
                        }
                        .toMap(),
                    launchWithSurgeByFlowId = steps
                        .mapNotNull { step ->
                            step.sourceFlowPlanId?.let { flowId ->
                                flowId to step.launchWithSurgeSnapshot
                            }
                        }
                        .toMap()
                )
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update {
            it.copy(
                title = value,
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun continueFromIdentity() {
        val title = _uiState.value.title.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Arc title is required.") }
            return
        }

        _uiState.update {
            it.copy(
                currentStep = 1,
                errorMessage = null
            )
        }
    }

    fun continueFromPicker() {
        if (_uiState.value.selectedFlowIdsInOrder.size < 2) {
            _uiState.update { it.copy(errorMessage = "An Arc needs at least two Flows.") }
            return
        }

        _uiState.update {
            it.copy(
                currentStep = 2,
                errorMessage = null
            )
        }
    }

    fun continueFromShape() {
        if (_uiState.value.selectedFlowIdsInOrder.size < 2) {
            _uiState.update { it.copy(errorMessage = "An Arc needs at least two Flows.") }
            return
        }

        _uiState.update {
            it.copy(
                currentStep = 3,
                errorMessage = null
            )
        }
    }

    fun continueFromTiming() {
        val state = _uiState.value

        if (state.selectedFlowIdsInOrder.size < 2) {
            _uiState.update { it.copy(errorMessage = "An Arc needs at least two Flows.") }
            return
        }

        val flowByIdForValidation = state.availableFlows.associateBy { it.id }
        val softFlowSelected = state.selectedFlowIdsInOrder.firstOrNull { flowId ->
            flowByIdForValidation[flowId]?.isSoftMode == true
        }
        if (softFlowSelected != null) {
            _uiState.update { it.copy(errorMessage = "Soft Flows cannot be added to Arcs.") }
            return
        }

        val invalidTarget = state.selectedFlowIdsInOrder.firstOrNull { flowId ->
            val text = state.targetMinutesTextByFlowId[flowId].orEmpty().trim()
            text.isNotBlank() && (text.toIntOrNull() == null || text.toIntOrNull()!! <= 0)
        }
        if (invalidTarget != null) {
            _uiState.update { it.copy(errorMessage = "Target minutes must be greater than 0.") }
            return
        }

        if (
            state.recurrenceType == ArcPlanEntity.RECURRENCE_CUSTOM &&
            state.recurrenceDays.isEmpty()
        ) {
            _uiState.update { it.copy(errorMessage = "Choose at least one day.") }
            return
        }

        _uiState.update {
            it.copy(
                currentStep = 4,
                errorMessage = null
            )
        }
    }

    fun goBack() {
        _uiState.update { current ->
            current.copy(
                currentStep = (current.currentStep - 1).coerceAtLeast(0),
                errorMessage = null
            )
        }
    }

    fun onTagFilterSelected(tagId: Long?) {
        _uiState.update { it.copy(selectedTagId = tagId) }
    }

    fun onFlowToggled(flowPlanId: Long) {
        _uiState.update { current ->
            val flow = current.availableFlows.firstOrNull { it.id == flowPlanId } ?: return@update current
            if (flow.isSoftMode) {
                return@update current.copy(errorMessage = "Soft Flows cannot be added to Arcs.")
            }
            val alreadySelected = flowPlanId in current.selectedFlowIdsInOrder

            if (alreadySelected) {
                current.copy(
                    selectedFlowIdsInOrder = current.selectedFlowIdsInOrder.filterNot { it == flowPlanId },
                    targetMinutesTextByFlowId = current.targetMinutesTextByFlowId - flowPlanId,
                    launchWithSurgeByFlowId = current.launchWithSurgeByFlowId - flowPlanId,
                    errorMessage = null
                )
            } else {
                current.copy(
                    selectedFlowIdsInOrder = current.selectedFlowIdsInOrder + flowPlanId,
                    targetMinutesTextByFlowId = current.targetMinutesTextByFlowId + (
                            flowPlanId to (flow.targetMinutes?.toString().orEmpty())
                            ),
                    launchWithSurgeByFlowId = current.launchWithSurgeByFlowId + (
                            flowPlanId to flow.launchWithSurge
                            ),
                    errorMessage = null
                )
            }
        }
    }

    fun moveSelectedFlowUp(flowPlanId: Long) {
        _uiState.update { current ->
            val list = current.selectedFlowIdsInOrder.toMutableList()
            val index = list.indexOf(flowPlanId)
            if (index <= 0) return@update current

            val temp = list[index - 1]
            list[index - 1] = list[index]
            list[index] = temp

            current.copy(selectedFlowIdsInOrder = list)
        }
    }

    fun moveSelectedFlowDown(flowPlanId: Long) {
        _uiState.update { current ->
            val list = current.selectedFlowIdsInOrder.toMutableList()
            val index = list.indexOf(flowPlanId)
            if (index == -1 || index >= list.lastIndex) return@update current

            val temp = list[index + 1]
            list[index + 1] = list[index]
            list[index] = temp

            current.copy(selectedFlowIdsInOrder = list)
        }
    }

    fun removeSelectedFlow(flowPlanId: Long) {
        _uiState.update { current ->
            current.copy(
                selectedFlowIdsInOrder = current.selectedFlowIdsInOrder.filterNot { it == flowPlanId },
                targetMinutesTextByFlowId = current.targetMinutesTextByFlowId - flowPlanId,
                launchWithSurgeByFlowId = current.launchWithSurgeByFlowId - flowPlanId,
                errorMessage = null
            )
        }
    }

    fun createFlowAndSelect(
        title: String,
        tagName: String,
        targetMinutesText: String,
        launchWithSurge: Boolean,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val trimmedTitle = title.trim()
            val trimmedTag = tagName.trim()
            val parsedTargetMinutes = targetMinutesText.trim().toIntOrNull()

            if (trimmedTitle.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Flow title is required.") }
                return@launch
            }

            if (parsedTargetMinutes != null && parsedTargetMinutes <= 0) {
                _uiState.update { it.copy(errorMessage = "Target minutes must be greater than 0.") }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val tagId = if (trimmedTag.isBlank()) {
                    null
                } else {
                    journeyRepository.getOrCreateTagId(trimmedTag)
                }

                val normalizedTarget = parsedTargetMinutes?.takeIf { it > 0 }
                val normalizedSurge = normalizedTarget != null && launchWithSurge

                val flowPlanId = flowPlanRepository.createFlowPlan(
                    title = trimmedTitle,
                    tagId = tagId,
                    isSoftMode = false,
                    targetMinutes = normalizedTarget,
                    launchWithSurge = normalizedSurge
                )

                _uiState.update { current ->
                    current.copy(
                        isSaving = false,
                        errorMessage = null,
                        selectedFlowIdsInOrder = if (flowPlanId in current.selectedFlowIdsInOrder) {
                            current.selectedFlowIdsInOrder
                        } else {
                            current.selectedFlowIdsInOrder + flowPlanId
                        },
                        targetMinutesTextByFlowId = current.targetMinutesTextByFlowId +
                                (flowPlanId to (normalizedTarget?.toString().orEmpty())),
                        launchWithSurgeByFlowId = current.launchWithSurgeByFlowId +
                                (flowPlanId to normalizedSurge)
                    )
                }
                onSaved()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to add Flow to this Arc."
                    )
                }
            }
        }
    }

    fun onStepTargetMinutesChanged(flowPlanId: Long, value: String) {
        val digitsOnly = value.filter(Char::isDigit)

        _uiState.update { current ->
            val flow = current.availableFlows.firstOrNull { it.id == flowPlanId } ?: return@update current
            val updatedTargetMap = current.targetMinutesTextByFlowId + (flowPlanId to digitsOnly)

            val hasValidTarget = digitsOnly.toIntOrNull()?.let { it > 0 } == true
            val updatedSurgeMap = if (flow.isSoftMode || !hasValidTarget) {
                current.launchWithSurgeByFlowId + (flowPlanId to false)
            } else {
                current.launchWithSurgeByFlowId
            }

            current.copy(
                targetMinutesTextByFlowId = updatedTargetMap,
                launchWithSurgeByFlowId = updatedSurgeMap,
                errorMessage = null
            )
        }
    }

    fun onStepLaunchWithSurgeChanged(flowPlanId: Long, enabled: Boolean) {
        _uiState.update { current ->
            val flow = current.availableFlows.firstOrNull { it.id == flowPlanId } ?: return@update current
            val targetText = current.targetMinutesTextByFlowId[flowPlanId].orEmpty()
            val hasValidTarget = targetText.toIntOrNull()?.let { it > 0 } == true
            val normalized = !flow.isSoftMode && hasValidTarget && enabled

            current.copy(
                launchWithSurgeByFlowId = current.launchWithSurgeByFlowId + (flowPlanId to normalized),
                errorMessage = null
            )
        }
    }

    fun onRecurrenceTypeSelected(type: String) {
        _uiState.update { current ->
            current.copy(
                recurrenceType = type,
                recurrenceDays = when (type) {
                    ArcPlanEntity.RECURRENCE_WEEKDAYS -> setOf(1, 2, 3, 4, 5)
                    ArcPlanEntity.RECURRENCE_WEEKLY -> setOf(1)
                    ArcPlanEntity.RECURRENCE_ONE_TIME,
                    ArcPlanEntity.RECURRENCE_DAILY -> emptySet()
                    else -> current.recurrenceDays
                },
                errorMessage = null
            )
        }
    }

    fun onCustomDayToggled(day: Int) {
        _uiState.update { current ->
            val next = current.recurrenceDays.toMutableSet()
            if (day in next) next.remove(day) else next.add(day)

            current.copy(
                recurrenceType = ArcPlanEntity.RECURRENCE_CUSTOM,
                recurrenceDays = next,
                errorMessage = null
            )
        }
    }

    fun saveArcWithSelectedFlows(
        onSaved: () -> Unit
    ) {
        val state = _uiState.value
        val title = state.title.trim()

        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Arc title is required.") }
            return
        }

        if (state.selectedFlowIdsInOrder.size < 2) {
            _uiState.update { it.copy(errorMessage = "An Arc needs at least two Flows.") }
            return
        }

        val flowByIdForValidation = state.availableFlows.associateBy { it.id }
        val softFlowSelected = state.selectedFlowIdsInOrder.firstOrNull { flowId ->
            flowByIdForValidation[flowId]?.isSoftMode == true
        }
        if (softFlowSelected != null) {
            _uiState.update { it.copy(errorMessage = "Soft Flows cannot be added to Arcs.") }
            return
        }

        val invalidTarget = state.selectedFlowIdsInOrder.firstOrNull { flowId ->
            val text = state.targetMinutesTextByFlowId[flowId].orEmpty().trim()
            text.isNotBlank() && (text.toIntOrNull() == null || text.toIntOrNull()!! <= 0)
        }
        if (invalidTarget != null) {
            _uiState.update { it.copy(errorMessage = "Target minutes must be greater than 0.") }
            return
        }

        if (
            state.recurrenceType == ArcPlanEntity.RECURRENCE_CUSTOM &&
            state.recurrenceDays.isEmpty()
        ) {
            _uiState.update { it.copy(errorMessage = "Choose at least one day.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val flowById = state.availableFlows.associateBy { it.id }

                val steps = state.selectedFlowIdsInOrder.mapIndexedNotNull { index, flowId ->
                    flowById[flowId]?.let { flow ->
                        val targetText = state.targetMinutesTextByFlowId[flowId].orEmpty().trim()
                        val targetMinutes = targetText.toIntOrNull()?.takeIf { it > 0 }
                        val launchWithSurge =
                            !flow.isSoftMode &&
                                    targetMinutes != null &&
                                    (state.launchWithSurgeByFlowId[flowId] ?: flow.launchWithSurge)

                        ArcPlanStepEntity(
                            arcPlanId = 0L,
                            orderIndex = index,
                            sourceFlowPlanId = flow.id,
                            titleSnapshot = flow.title,
                            tagIdSnapshot = flow.tagId,
                            isSoftModeSnapshot = flow.isSoftMode,
                            targetMinutesSnapshot = targetMinutes,
                            launchWithSurgeSnapshot = launchWithSurge,
                            linkState = ArcPlanStepEntity.LINK_STATE_LINKED
                        )
                    }
                }

                val recurrenceDaysCsv = state.recurrenceDays
                    .toList()
                    .sorted()
                    .joinToString(",")

                val editId = editArcPlanId
                if (editId == null) {
                    arcPlanRepository.createArcPlanWithSteps(
                        title = title,
                        steps = steps,
                        recurrenceType = state.recurrenceType,
                        recurrenceDaysCsv = recurrenceDaysCsv
                    )
                } else {
                    val existing = arcPlanRepository.getArcPlanById(editId)
                    if (existing == null) {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = "Arc not found."
                            )
                        }
                        return@launch
                    }

                    arcPlanRepository.updateArcPlan(
                        existing.copy(
                            title = title,
                            recurrenceType = state.recurrenceType,
                            recurrenceDaysCsv = recurrenceDaysCsv
                        )
                    )

                    arcPlanRepository.replaceAllSteps(
                        arcPlanId = editId,
                        steps = steps.mapIndexed { index, step ->
                            step.copy(
                                arcPlanId = editId,
                                orderIndex = index
                            )
                        }
                    )
                }

                _uiState.update { it.copy(isSaving = false) }
                onSaved()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save planned arc."
                    )
                }
            }
        }
    }

    private fun List<FlowPlanEntity>.toPickerUiModels(
        tags: List<TagEntity>
    ): List<PlanArcFlowPickerItemUiModel> {
        val tagNameById = tags.associate { it.id to it.name }

        return map { plan ->
            PlanArcFlowPickerItemUiModel(
                id = plan.id,
                title = plan.title,
                tagId = plan.tagId,
                tagName = plan.tagId?.let { tagNameById[it] }.orEmpty(),
                isSoftMode = plan.isSoftMode,
                targetMinutes = plan.targetMinutes,
                launchWithSurge = plan.launchWithSurge,
                pinned = plan.pinned
            )
        }
    }
}