package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.repository.BeamError
import com.kingkharnivore.skillz.data.repository.BeamRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import javax.inject.Inject

data class ScheduleBeamUiState(
    val tagName: String = "",
    val selectedDateEpochMs: Long? = null,
    val selectedHour: Int? = null,
    val selectedMinute: Int? = null,
    val durationMinutes: Int? = null,
    val customMinutesText: String = "",
    val datePreset: String? = null, // "today" | "tomorrow" | null
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

    init {
        viewModelScope.launch {
            applySmartDefaultsIfMissingConsideringBeams()
        }
    }

    /**
     * Default rules (updated):
     * 1) Base candidate = ceil((now + 2 minutes) to next 5-min marker)
     * 2) Consider all currently scheduled beams in a near future horizon
     * 3) Choose the earliest start that begins a free 30-minute block (no overlap)
     * 4) Also keep your "after 8pm => tomorrow" vibe implicitly by allowing the chosen time to roll into tomorrow
     */
    private suspend fun applySmartDefaultsIfMissingConsideringBeams() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)

        // If user already picked date/time, do not override
        val current = _ui.value
        if (current.selectedDateEpochMs != null && current.selectedHour != null && current.selectedMinute != null) return

        val candidateStart = ceilToNextFiveMinutes(now.plusMinutes(2)).withSecond(0).withNano(0)

        // Horizon: look ahead 72 hours for conflicts (adjust if you want)
        val windowStartMs = candidateStart.toInstant().toEpochMilli()
        val windowEndMs = candidateStart.plusHours(72).toInstant().toEpochMilli()

        // ✅ This assumes your BeamRepository exposes this (you used it elsewhere in your codebase)
        // and BeamEntity has startTime/endTime in epoch millis.
        val beams = beamRepository.getBeamsOverlappingWindow(windowStartMs, windowEndMs)

        val chosenStart = findNextFreeBlockStart(
            base = candidateStart,
            beams = beams.map { BeamWindow(it.startTime, it.endTime) },
            blockMinutes = 30,
            stepMinutes = 5
        )

        val chosenLocalDate = chosenStart.withZoneSameInstant(zone).toLocalDate()
        val chosenLocalTime = chosenStart.withZoneSameInstant(zone).toLocalTime()

        _ui.update { s ->
            val utcMidnightMillis = localDateToUtcMidnightMillis(chosenLocalDate)
            s.copy(
                selectedDateEpochMs = s.selectedDateEpochMs ?: utcMidnightMillis,
                selectedHour = s.selectedHour ?: chosenLocalTime.hour,
                selectedMinute = s.selectedMinute ?: chosenLocalTime.minute,
                datePreset = s.datePreset ?: inferPreset(now.toLocalDate(), chosenLocalDate),
                error = null
            )
        }
    }

    private data class BeamWindow(val startMs: Long, val endMs: Long)

    private fun inferPreset(today: LocalDate, chosen: LocalDate): String? =
        when (chosen) {
            today -> "today"
            today.plusDays(1) -> "tomorrow"
            else -> null
        }

    private fun localDateToUtcMidnightMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun ceilToNextFiveMinutes(dt: ZonedDateTime): ZonedDateTime {
        val minute = dt.minute
        val mod = minute % 5
        if (mod == 0) return dt
        val add = 5 - mod
        return dt.plusMinutes(add.toLong())
    }

    /**
     * Find the earliest start >= base such that [start, start+blockMinutes) does not overlap any beam.
     * We advance in stepMinutes (5) increments for a tactical “calendar-ish” feel.
     */
    private fun findNextFreeBlockStart(
        base: ZonedDateTime,
        beams: List<BeamWindow>,
        blockMinutes: Int,
        stepMinutes: Int
    ): ZonedDateTime {
        if (beams.isEmpty()) return base

        // Sort for efficient scanning
        val sorted = beams.sortedBy { it.startMs }

        var cursor = base.toInstant().toEpochMilli()
        val stepMs = stepMinutes * 60_000L
        val blockMs = blockMinutes * 60_000L

        // Hard safety cap so we never infinite-loop
        val maxIterations = 10_000

        repeat(maxIterations) {
            val startMs = cursor
            val endMs = startMs + blockMs

            val overlaps = sorted.any { w ->
                // overlap if start < w.end && end > w.start
                startMs < w.endMs && endMs > w.startMs
            }

            if (!overlaps) {
                return Instant.ofEpochMilli(startMs).atZone(base.zone)
            }

            cursor += stepMs
        }

        // Fallback: if somehow everything is packed, just return base
        return base
    }

    // ---- UI events ----

    fun pickToday() {
        val zone = ZoneId.systemDefault()
        val today = ZonedDateTime.now(zone).toLocalDate()
        _ui.update { it.copy(selectedDateEpochMs = localDateToUtcMidnightMillis(today), datePreset = "today", error = null) }
    }

    fun pickTomorrow() {
        val zone = ZoneId.systemDefault()
        val tomorrow = ZonedDateTime.now(zone).toLocalDate().plusDays(1)
        _ui.update { it.copy(selectedDateEpochMs = localDateToUtcMidnightMillis(tomorrow), datePreset = "tomorrow", error = null) }
    }

    fun onTimePicked(hour: Int, minute: Int) {
        _ui.update { it.copy(selectedHour = hour, selectedMinute = minute, error = null) }
    }

    fun onTagNameChange(s: String) {
        _ui.update { it.copy(tagName = s, error = null) }
    }

    fun onPickTag(name: String) {
        _ui.update { it.copy(tagName = name, error = null) }
    }

    fun onDatePicked(epochMsAtUtcMidnight: Long) {
        _ui.update { it.copy(selectedDateEpochMs = epochMsAtUtcMidnight, datePreset = null, error = null) }
    }

    fun setDurationMinutes(minutes: Int) {
        _ui.update { it.copy(durationMinutes = minutes.coerceAtLeast(1), error = null) }
    }

    fun onCustomMinutesChange(raw: String) {
        _ui.update { it.copy(customMinutesText = raw.filter(Char::isDigit), error = null) }
    }

    fun applyCustomMinutes() {
        val m = _ui.value.customMinutesText.toIntOrNull()
        if (m == null || m <= 0) {
            _ui.update { it.copy(error = "Duration must be greater than 0.") }
            return
        }
        _ui.update { it.copy(durationMinutes = m, error = null) }
    }

    fun schedule(onDone: () -> Unit) {
        viewModelScope.launch {
            val state = _ui.value
            val tagName = state.tagName.trim()

            if (tagName.isBlank()) {
                _ui.update { it.copy(error = "Pick a journey or enter a new one.") }
                return@launch
            }

            val minutes = state.durationMinutes ?: 0
            val durationMs = minutes * 60_000L
            if (durationMs <= 0L) {
                _ui.update { it.copy(error = "Pick a duration.") }
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

        // DatePicker millis represent a UTC date boundary
        val date: LocalDate = if (selectedDateMillis != null) {
            Instant.ofEpochMilli(selectedDateMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
        } else {
            now.toLocalDate()
        }

        val hour = selectedHour ?: now.hour
        val minute = selectedMinute ?: now.minute

        return ZonedDateTime.of(
            date,
            LocalTime.of(hour, minute, 0, 0),
            zone
        ).toInstant().toEpochMilli()
    }
}
