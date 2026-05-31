package com.kingkharnivore.skillz.viewmodel.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.domain.voyage.VoyageHallStats
import com.kingkharnivore.skillz.domain.voyage.VoyageSourceFlow
import com.kingkharnivore.skillz.domain.voyage.VoyageStatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
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

    init {
        observeVoyageStats()
    }

    private fun observeVoyageStats() {
        viewModelScope.launch {
            combine(
                flowRepository.getAllSessions(),
                journeyRepository.getAllTags()
            ) { sessions, tags ->
                val tagNameById = tags.associate { it.id to it.name }
                voyageStatsCalculator.calculate(
                    sessions = sessions.toVoyageSourceFlows(tagNameById),
                    now = Instant.now(),
                    zoneId = ZoneId.systemDefault()
                )
            }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
                .collect { stats ->
                    _uiState.update {
                        VoyageHallUiState(
                            isLoading = false,
                            stats = stats,
                            errorMessage = null
                        )
                    }
                }
        }
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
