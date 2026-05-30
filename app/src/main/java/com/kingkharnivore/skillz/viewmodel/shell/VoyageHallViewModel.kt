package com.kingkharnivore.skillz.viewmodel.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.domain.voyage.VoyageHallStats
import com.kingkharnivore.skillz.domain.voyage.VoyageStatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
    private val voyageStatsCalculator: VoyageStatsCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoyageHallUiState())
    val uiState: StateFlow<VoyageHallUiState> = _uiState

    init {
        observeVoyageStats()
    }

    private fun observeVoyageStats() {
        viewModelScope.launch {
            flowRepository.getAllSessions()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
                .collect { sessions ->
                    val stats = voyageStatsCalculator.calculate(
                        sessions = sessions,
                        now = Instant.now(),
                        zoneId = ZoneId.systemDefault()
                    )
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
}
