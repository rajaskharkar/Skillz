package com.kingkharnivore.skillz.viewmodel.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.utils.shell.voyage.VoyageHallStats
import com.kingkharnivore.skillz.utils.shell.voyage.VoyageSourceFlow
import com.kingkharnivore.skillz.utils.shell.voyage.VoyageStatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoyageHallUiState(
    val isLoading: Boolean = true,
    val stats: VoyageHallStats? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class VoyageHallViewModel @Inject constructor(
    private val flowRepository: FlowRepository,
    private val journeyRepository: JourneyRepository,
    private val voyageStatsCalculator: VoyageStatsCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoyageHallUiState())
    val uiState: StateFlow<VoyageHallUiState> = _uiState

    private var latestSourceFlows: List<VoyageSourceFlow> = emptyList()
    private var hasObservedSourceFlows = false
    private var dayBoundaryRefreshJob: Job? = null

    init {
        observeVoyageStats()
        scheduleNextDayBoundaryRefresh()
    }

    fun refresh() {
        if (hasObservedSourceFlows) {
            recalculate()
        }
    }

    private fun observeVoyageStats() {
        viewModelScope.launch {
            combine(
                flowRepository.getAllSessions(),
                journeyRepository.getAllTags()
            ) { sessions, tags ->
                val tagNameById = tags.associate { it.id to it.name }
                sessions.toVoyageSourceFlows(tagNameById)
            }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
                .collect { sourceFlows ->
                    latestSourceFlows = sourceFlows
                    hasObservedSourceFlows = true
                    recalculate(sourceFlows)
                }
        }
    }

    private fun recalculate(sourceFlows: List<VoyageSourceFlow> = latestSourceFlows) {
        _uiState.update {
            it.copy(
                isLoading = false,
                stats = voyageStatsCalculator.calculate(
                    sessions = sourceFlows,
                    now = Instant.now(),
                    zoneId = ZoneId.systemDefault()
                ),
                errorMessage = null
            )
        }
    }

    private fun scheduleNextDayBoundaryRefresh() {
        if (dayBoundaryRefreshJob?.isActive == true) return

        dayBoundaryRefreshJob = viewModelScope.launch {
            while (true) {
                delay(millisUntilNextLocalDayBoundary())
                if (hasObservedSourceFlows) {
                    recalculate()
                }
            }
        }
    }

    private fun millisUntilNextLocalDayBoundary(): Long {
        val zoneId = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zoneId)
        val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(zoneId).plusSeconds(1)
        return Duration.between(now, nextDay).toMillis().coerceAtLeast(1_000L)
    }

    private fun List<SessionEntity>.toVoyageSourceFlows(tagNameById: Map<Long, String>): List<VoyageSourceFlow> =
        map { session ->
            session.toVoyageSourceFlow(tagNameById[session.tagId])
        }

    private fun SessionEntity.toVoyageSourceFlow(tagName: String?) = VoyageSourceFlow(
        id = id,
        title = title,
        tagName = tagName,
        startTime = startTime,
        endTime = endTime,
        durationMs = durationMs,
        scyraPoints = scyraPoints,
        isSoftMode = isSoftMode,
        arcId = arcId,
        arcIndex = arcIndex,
        arcMultiplierUsed = arcMultiplierUsed
    )
}
