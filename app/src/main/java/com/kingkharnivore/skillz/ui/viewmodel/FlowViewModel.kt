package com.kingkharnivore.skillz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.ui.service.AliveFlowServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StopwatchState(
    val isRunning: Boolean = false,
    val elapsedMs: Long = 0L
)

data class FlowUiState(
    val title: String = "",
    val description: String = "",
    val tagName: String = "",
    val stopwatch: StopwatchState = StopwatchState(),
    val isInFlowMode: Boolean = false
)

@HiltViewModel
class FlowViewModel @Inject constructor(
    private val tagRepository: JourneyRepository,
    private val sessionRepository: FlowRepository,
    private val focusSessionRepository: AliveFlowRepository,
    private val aliveFlowServiceController: AliveFlowServiceController
) : ViewModel() {

    val ongoingSession: StateFlow<OngoingSessionEntity?> =
        focusSessionRepository.getOngoingSession()
            .stateIn(
                viewModelScope,
                SharingStarted.Companion.WhileSubscribed(5_000),
                null
            )

    private val _uiState = MutableStateFlow(FlowUiState())
    val uiState: StateFlow<FlowUiState> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val tags: StateFlow<List<TagEntity>> =
        tagRepository.getAllTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Stopwatch internal fields for correct elapsed computation
    private var baseStartTimeMs: Long? = null
    private var accumulatedBeforeStartMs: Long = 0L
    private var tickerJob: Job? = null

    init {
        // On creation, restore any ongoing session
        viewModelScope.launch {
            focusSessionRepository.getOngoingSession()
                .firstOrNull()
                ?.let { entity ->
                    val now = System.currentTimeMillis()
                    accumulatedBeforeStartMs = entity.accumulatedBeforeStartMs
                    baseStartTimeMs = entity.baseStartTimeMs

                    val elapsed = if (entity.isRunning && baseStartTimeMs != null) {
                        accumulatedBeforeStartMs + (now - baseStartTimeMs!!).coerceAtLeast(0L)
                    } else {
                        accumulatedBeforeStartMs
                    }

                    _uiState.value = FlowUiState(
                        title = entity.title,
                        description = entity.description,
                        tagName = entity.tagName,
                        stopwatch = StopwatchState(
                            isRunning = entity.isRunning,
                            elapsedMs = elapsed
                        ),
                        isInFlowMode = entity.isInFlowMode
                    )

                    if (entity.isRunning) {
                        startTicker()
                    }
                }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
        saveOngoing()
    }

    fun onDescriptionChange(newDescription: String) {
        _uiState.update { it.copy(description = newDescription) }
        saveOngoing()
    }

    fun onTagNameChange(newTagName: String) {
        _uiState.update { it.copy(tagName = newTagName) }
        saveOngoing()
    }

    // -------- Stopwatch / Focus Mode logic (same as before, plus save) --------

    fun startOrResumeStopwatch() {
        if (_uiState.value.stopwatch.isRunning) return

        val now = System.currentTimeMillis()
        baseStartTimeMs = now
        _uiState.update {
            it.copy(stopwatch = it.stopwatch.copy(isRunning = true))
        }
        startTicker()
        saveOngoing()
    }

    fun pauseStopwatch() {
        if (!_uiState.value.stopwatch.isRunning) return

        val now = System.currentTimeMillis()
        val base = baseStartTimeMs
        if (base != null) {
            accumulatedBeforeStartMs += (now - base).coerceAtLeast(0L)
        }
        baseStartTimeMs = null

        _uiState.update {
            it.copy(
                stopwatch = it.stopwatch.copy(
                    isRunning = false,
                    elapsedMs = accumulatedBeforeStartMs
                )
            )
        }
        stopTicker()
        saveOngoing()
    }

    fun resetStopwatch() {
        baseStartTimeMs = null
        accumulatedBeforeStartMs = 0L
        _uiState.update {
            it.copy(stopwatch = StopwatchState(isRunning = false, elapsedMs = 0L))
        }
        stopTicker()
        saveOngoing()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val base = baseStartTimeMs
                val elapsed = if (_uiState.value.stopwatch.isRunning && base != null) {
                    accumulatedBeforeStartMs + (now - base).coerceAtLeast(0L)
                } else {
                    accumulatedBeforeStartMs
                }
                _uiState.update {
                    it.copy(stopwatch = it.stopwatch.copy(elapsedMs = elapsed))
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    // Focus mode toggle
    fun enterFocusMode() {
        if (!_uiState.value.stopwatch.isRunning) {
            startOrResumeStopwatch()
        }
        _uiState.update { it.copy(isInFlowMode = true) }
        saveOngoing()
        aliveFlowServiceController.start()
    }

    fun exitFocusMode() {
        if (uiState.value.stopwatch.isRunning) {
            pauseStopwatch()
        }
        _uiState.update { it.copy(isInFlowMode = false) }
        saveOngoing()
        aliveFlowServiceController.stop()
    }

    // Persist current state to DB
    private fun saveOngoing() {
        val state = _uiState.value
        viewModelScope.launch {
            val entity = OngoingSessionEntity(
                id = 1,
                title = state.title,
                description = state.description,
                tagName = state.tagName,
                isInFlowMode = state.isInFlowMode,
                isRunning = state.stopwatch.isRunning,
                baseStartTimeMs = baseStartTimeMs,
                accumulatedBeforeStartMs = accumulatedBeforeStartMs
            )
            focusSessionRepository.saveOngoingSession(entity)
        }
    }

    // Clear persisted focus session (after saving real session or aborting)
    private suspend fun clearOngoing() {
        focusSessionRepository.clearOngoingSession()
    }

    // -------- Saving the real session --------

    fun saveSession(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank() || state.tagName.isBlank()) {
            _error.value = "Title and Skill are required"
            return
        }

        viewModelScope.launch {
            try {
                _isSaving.value = true
                _error.value = null

                val tagId = tagRepository.getOrCreateTagId(state.tagName.trim())
                val durationMs = state.stopwatch.elapsedMs.coerceAtLeast(0L)
                val endTime = System.currentTimeMillis()
                val startTime = (endTime - durationMs).coerceAtLeast(0L)

                sessionRepository.addSession(
                    title = state.title.trim(),
                    description = state.description.trim(),
                    tagId = tagId,
                    startTime = startTime,
                    endTime = endTime,
                    durationMs = durationMs
                )

                resetStopwatch()
                _uiState.value = FlowUiState()
                clearOngoing()
                aliveFlowServiceController.stop()
                onDone()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save session"
            } finally {
                _isSaving.value = false
            }
        }
    }
}