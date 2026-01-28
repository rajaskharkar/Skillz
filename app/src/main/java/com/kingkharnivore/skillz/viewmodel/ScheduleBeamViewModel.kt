package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.repository.BeamError
import com.kingkharnivore.skillz.data.repository.BeamRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.*
import javax.inject.Inject

data class ScheduleBeamUiState(
    val tagName: String = "",
    val selectedDateEpochMs: Long? = null,   // date picked (DatePicker millis)
    val selectedHour: Int? = null,
    val selectedMinute: Int? = null,
    val minutesText: String = "",
    val secondsText: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class ScheduleBeamViewModel @Inject constructor(
    private val beamRepository: BeamRepository,
    private val journeyRepository: JourneyRepository
) : ViewModel() {

    val tags = journeyRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(ScheduleBeamUiState())
    val ui: StateFlow<ScheduleBeamUiState> = _ui.asStateFlow()

    fun onTimePicked(hour: Int, minute: Int) {
        _ui.update { it.copy(selectedHour = hour, selectedMinute = minute, error = null) }
    }

    fun clearTime() {
        _ui.update { it.copy(selectedHour = null, selectedMinute = null, error = null) }
    }

    fun onTagNameChange(s: String) {
        _ui.update { it.copy(tagName = s, error = null) }
    }

    fun onPickTag(name: String) {
        _ui.update { it.copy(tagName = name, error = null) }
    }

    fun onDatePicked(epochMsAtMidnight: Long) {
        _ui.update { it.copy(selectedDateEpochMs = epochMsAtMidnight, error = null) }
    }

    fun onMinutesChange(raw: String) {
        _ui.update { it.copy(minutesText = raw.filter(Char::isDigit), error = null) }
    }

    fun onSecondsChange(raw: String) {
        _ui.update { it.copy(secondsText = raw.filter(Char::isDigit), error = null) }
    }

    fun schedule(onDone: () -> Unit) {
        viewModelScope.launch {
            val state = _ui.value
            val tagName = state.tagName.trim()

            if (tagName.isBlank()) {
                _ui.update { it.copy(error = "Pick a tag or enter a new one.") }
                return@launch
            }

            val min = state.minutesText.toIntOrNull() ?: 0
            val secRaw = state.secondsText.toIntOrNull() ?: 0

            // normalize seconds -> minutes
            val extraMin = secRaw / 60
            val sec = secRaw % 60
            val minutes = min + extraMin

            val durationMs = ((minutes * 60L) + sec.toLong()) * 1000L
            if (durationMs <= 0L) {
                _ui.update { it.copy(error = "Duration must be greater than 0.") }
                return@launch
            }

            val startTimeMs = computeStartTimeMs(
                selectedDateMillis = state.selectedDateEpochMs,
                selectedHour = state.selectedHour,
                selectedMinute = state.selectedMinute
            )
            val nowMs = System.currentTimeMillis()
            if (startTimeMs < nowMs - 60_000L) {
                _ui.update { it.copy(error = "Start time is in the past. Pick a future time.") }
                return@launch
            }


            _ui.update { it.copy(isSaving = true, error = null, success = false) }
            try {
                val tagId = journeyRepository.getOrCreateTagId(tagName)
                beamRepository.scheduleBeam(
                    tagId = tagId,
                    startTime = startTimeMs,
                    durationMs = durationMs
                )
                _ui.update { it.copy(isSaving = false, success = true) }
                onDone()
            } catch (e: BeamError.Overlap) {
                _ui.update { it.copy(isSaving = false, error = "Overlaps another Beam. Pick a different time/date.") }
            } catch (e: BeamError.InvalidTime) {
                _ui.update { it.copy(isSaving = false, error = e.message ?: "Invalid time.") }
            } catch (e: Exception) {
                _ui.update { it.copy(isSaving = false, error = e.message ?: "Failed to schedule Beam.") }
            }
        }
    }

    private fun computeStartTimeMs(
        selectedDateMillis: Long?,
        selectedHour: Int?,
        selectedMinute: Int?
    ): Long {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)

        val date = if (selectedDateMillis != null) {
            Instant.ofEpochMilli(selectedDateMillis)
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate()
        } else {
            now.toLocalDate()
        }

        val hour = selectedHour ?: now.hour
        val minute = selectedMinute ?: now.minute

        val start = ZonedDateTime.of(
            date,
            LocalTime.of(hour, minute, 0, 0),
            zone
        )

        return start.toInstant().toEpochMilli()
    }
}
