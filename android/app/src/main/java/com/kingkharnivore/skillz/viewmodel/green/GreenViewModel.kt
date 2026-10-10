package com.kingkharnivore.skillz.viewmodel.green

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.green.*
import com.kingkharnivore.skillz.data.repository.green.*
import com.kingkharnivore.skillz.domain.green.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

 data class GreenUiState(
    val drops: Long = 0, val specimens: List<PlantSpecimenEntity> = emptyList(),
    val awards: List<GreenBadgeAwardEntity> = emptyList(), val busy: Boolean = false,
    val messageRes: Int? = null, val action: GreenActionEntity? = null, val feedbackId: String? = null
)
private data class GreenFeedback(val id: String = java.util.UUID.randomUUID().toString(), val busy: Boolean = false, val messageRes: Int? = null, val action: GreenActionEntity? = null)

@HiltViewModel
class GreenViewModel @Inject constructor(private val repository: GreenRepository) : ViewModel() {
    private val feedback = MutableStateFlow(GreenFeedback())
    val state = combine(repository.observeDropBalance(), repository.observeSpecimens(), repository.observeAwards(), feedback) { drops, plants, awards, response ->
        GreenUiState(drops, plants, awards, response.busy, response.messageRes, response.action, response.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GreenUiState())

    fun plant(species: String, token: String) = act { repository.plantSeed(species, token) }
    fun water(id: String, level: Int, token: String) = act { repository.water(id, level, token) }
    /** A snackbar or dialog may finish after another action; only acknowledge its own feedback. */
    fun dismissFeedback(feedbackId: String?) {
        feedback.update { current ->
            if (current.id == feedbackId && !current.busy)
                current.copy(messageRes = null, action = null) else current
        }
    }
    fun view(name: String, environment: GreenEnvironment? = null, speciesId: String? = null) = viewModelScope.launch {
        try { repository.recordView(name, environment, speciesId) } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Reading remains usable if local telemetry fails. */ }
    }
    private fun act(block: suspend () -> PlantActionResult) {
        if (feedback.value.busy) return
        feedback.value = GreenFeedback(busy = true)
        viewModelScope.launch {
            try {
                feedback.value = when (val result = block()) {
                    is PlantActionResult.Success -> GreenFeedback(action = result.action)
                    PlantActionResult.InsufficientDrops -> GreenFeedback(messageRes = R.string.green_insufficient)
                    PlantActionResult.Mastered -> GreenFeedback(messageRes = R.string.green_mastered_no_water)
                    PlantActionResult.StaleLevel -> GreenFeedback(messageRes = R.string.green_stale)
                    PlantActionResult.NotFound -> GreenFeedback(messageRes = R.string.green_not_found)
                }
            } catch (e: CancellationException) { feedback.value = GreenFeedback(); throw e
            } catch (_: Exception) { feedback.value = GreenFeedback(messageRes = R.string.green_error) }
        }
    }
}
