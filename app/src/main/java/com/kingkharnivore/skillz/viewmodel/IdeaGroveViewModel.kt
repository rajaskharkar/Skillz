package com.kingkharnivore.skillz.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.data.model.entity.PulseGroveStatusValues
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.data.repository.IdeaGroveRepository
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemType
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemUiModel
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveSort
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal fun OngoingSessionEntity?.isMeaningfulActiveFlow(): Boolean {
    if (this == null) return false
    return isRunning ||
            isInFlowMode ||
            accumulatedBeforeStartMs > 0L ||
            baseStartTimeMs != null
}

internal fun OngoingSessionEntity?.isAbandonedPulseOriginDraft(): Boolean {
    if (this == null) return false
    return originPulseId != null && !isMeaningfulActiveFlow()
}

sealed interface IdeaGroveEvent {
    data class NavigateToFlow(
        val pulseId: Long,
        val title: String,
        val journeyName: String?
    ) : IdeaGroveEvent

    data object NavigateToCurrentFlow : IdeaGroveEvent

    data class ShowSnackbar(
        @StringRes val messageRes: Int,
        @StringRes val actionLabelRes: Int? = null
    ) : IdeaGroveEvent
}

@HiltViewModel
class IdeaGroveViewModel @Inject constructor(
    private val repository: IdeaGroveRepository,
    private val aliveFlowRepository: AliveFlowRepository
) : ViewModel() {
    private val sort = MutableStateFlow(IdeaGroveSort.Recents)
    private val expandedPulseId = MutableStateFlow<Long?>(null)
    private val pendingDeletePulseId = MutableStateFlow<Long?>(null)
    private val eventsChannel = Channel<IdeaGroveEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<IdeaGroveUiState> = combine(
        repository.observeIdeaGroveItems(),
        sort,
        expandedPulseId,
        pendingDeletePulseId,
        aliveFlowRepository.getOngoingSession()
    ) { items, aliveSort, expanded, pendingDelete, ongoing ->
        val alive = sortAlive(
            items.filter { it.groveStatus == PulseGroveStatusValues.ALIVE },
            aliveSort
        )
        val completed = items
            .filter { it.type == IdeaGroveItemType.INSIGHT || it.type == IdeaGroveItemType.COMPLETED_IDEA }
            .sortedByDescending { it.groveStatusChangedAt ?: it.updatedAt }

        IdeaGroveUiState(
            aliveItems = alive,
            completedItems = completed,
            totalPulseFlowDurationMs = items.sumOf { it.totalFlowDurationMs },
            totalPulseFlowCount = items.sumOf { it.flowCount },
            completedPulseFlowDurationMs = completed.sumOf { it.totalFlowDurationMs },
            completedPulseFlowCount = completed.sumOf { it.flowCount },
            aliveSort = aliveSort,
            expandedPulseId = expanded,
            pendingDeletePulseId = pendingDelete,
            isFlowRunning = ongoing.isMeaningfulActiveFlow(),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IdeaGroveUiState())

    init {
        viewModelScope.launch {
            aliveFlowRepository.getOngoingSession()
                .distinctUntilChanged()
                .collect { ongoing ->
                    if (ongoing.isAbandonedPulseOriginDraft()) {
                        aliveFlowRepository.clearOngoingSession()
                    }
                }
        }
    }

    fun onPulseClicked(pulseId: Long) {
        expandedPulseId.update { current -> if (current == pulseId) null else pulseId }
    }

    fun onSortChanged(newSort: IdeaGroveSort) {
        sort.value = newSort
    }

    fun onFlowClicked(pulseId: Long) {
        viewModelScope.launch {
            if (uiState.value.isFlowRunning) {
                eventsChannel.send(
                    IdeaGroveEvent.ShowSnackbar(
                        messageRes = R.string.idea_grove_flow_already_running,
                        actionLabelRes = R.string.idea_grove_view_flow
                    )
                )
                return@launch
            }
            val context = repository.getPulseLaunchContext(pulseId) ?: return@launch
            eventsChannel.send(
                IdeaGroveEvent.NavigateToFlow(
                    pulseId = context.pulseId,
                    title = context.title,
                    journeyName = context.journeyName
                )
            )
        }
    }

    fun onMarkAsInsightClicked(pulseId: Long) {
        viewModelScope.launch {
            repository.markPulseAsInsight(pulseId)
            expandedPulseId.value = null
            eventsChannel.send(IdeaGroveEvent.ShowSnackbar(R.string.idea_grove_marked_as_insight))
        }
    }

    fun onMarkCompletedClicked(pulseId: Long) {
        viewModelScope.launch {
            repository.markPulseCompleted(pulseId)
            expandedPulseId.value = null
            eventsChannel.send(IdeaGroveEvent.ShowSnackbar(R.string.idea_grove_moved_completed))
        }
    }

    fun onReviveClicked(pulseId: Long) {
        viewModelScope.launch {
            repository.revivePulse(pulseId)
            expandedPulseId.value = null
            eventsChannel.send(IdeaGroveEvent.ShowSnackbar(R.string.idea_grove_moved_alive))
        }
    }

    fun onDeletePulseClicked(pulseId: Long) {
        pendingDeletePulseId.value = pulseId
    }

    fun onDismissDeletePulse() {
        pendingDeletePulseId.value = null
    }

    fun onConfirmDeletePulse() {
        val pulseId = pendingDeletePulseId.value ?: return
        viewModelScope.launch {
            repository.deletePulse(pulseId)
            pendingDeletePulseId.value = null
            expandedPulseId.value = null
            eventsChannel.send(IdeaGroveEvent.ShowSnackbar(R.string.idea_grove_deleted_pulse))
        }
    }

    private fun sortAlive(
        items: List<IdeaGroveItemUiModel>,
        sort: IdeaGroveSort
    ) = when (sort) {
        IdeaGroveSort.Recents -> items.sortedWith(
            compareByDescending<IdeaGroveItemUiModel> { it.lastWorkedAt ?: it.updatedAt }
                .thenByDescending { it.createdAt }
        )
        IdeaGroveSort.Newest -> items.sortedByDescending { it.createdAt }
        IdeaGroveSort.Oldest -> items.sortedBy { it.createdAt }
        IdeaGroveSort.MostTime -> items.sortedWith(
            compareByDescending<IdeaGroveItemUiModel> { it.totalFlowDurationMs }
                .thenByDescending { it.lastWorkedAt ?: 0L }
                .thenByDescending { it.createdAt }
        )
        IdeaGroveSort.LeastTime -> items.sortedWith(
            compareBy<IdeaGroveItemUiModel> { it.totalFlowDurationMs }
                .thenByDescending { it.createdAt }
        )
    }
}
