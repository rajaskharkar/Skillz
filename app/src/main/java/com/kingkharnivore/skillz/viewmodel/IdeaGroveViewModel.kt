package com.kingkharnivore.skillz.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.PulseGroveStatusValues
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.data.repository.IdeaGroveRepository
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemType
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveSort
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    aliveFlowRepository: AliveFlowRepository
) : ViewModel() {
    private val sort = MutableStateFlow(IdeaGroveSort.Newest)
    private val expandedPulseId = MutableStateFlow<Long?>(null)
    private val eventsChannel = Channel<IdeaGroveEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<IdeaGroveUiState> = combine(
        repository.observeIdeaGroveItems(),
        sort,
        expandedPulseId,
        aliveFlowRepository.getOngoingSession()
    ) { items, aliveSort, expanded, ongoing ->
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
            aliveTotalDurationMs = alive.sumOf { it.totalFlowDurationMs },
            aliveFlowCount = alive.sumOf { it.flowCount },
            completedTotalDurationMs = completed.sumOf { it.totalFlowDurationMs },
            completedFlowCount = completed.sumOf { it.flowCount },
            aliveSort = aliveSort,
            expandedPulseId = expanded,
            isFlowRunning = ongoing != null,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IdeaGroveUiState())

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

    private fun sortAlive(
        items: List<com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemUiModel>,
        sort: IdeaGroveSort
    ) = when (sort) {
        IdeaGroveSort.Newest -> items.sortedByDescending { it.createdAt }
        IdeaGroveSort.Oldest -> items.sortedBy { it.createdAt }
        IdeaGroveSort.MostTime -> items.sortedWith(
            compareByDescending<com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemUiModel> { it.totalFlowDurationMs }
                .thenByDescending { it.lastWorkedAt ?: 0L }
                .thenByDescending { it.createdAt }
        )
        IdeaGroveSort.LeastTime -> items.sortedWith(
            compareBy<com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemUiModel> { it.totalFlowDurationMs }
                .thenByDescending { it.createdAt }
        )
    }
}
