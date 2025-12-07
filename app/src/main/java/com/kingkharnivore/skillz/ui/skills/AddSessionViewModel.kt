package com.kingkharnivore.skillz.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.TagEntity
import com.kingkharnivore.skillz.data.repository.SessionRepository
import com.kingkharnivore.skillz.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StopwatchState(
    val isRunning: Boolean = false,
    val elapsedMs: Long = 0L
)

@HiltViewModel
class AddSessionViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val tags: StateFlow<List<TagEntity>> =
        tagRepository.getAllTags()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // ⏱ Distraction-free stopwatch state
    private val _stopwatchState = MutableStateFlow(StopwatchState())
    val stopwatchState: StateFlow<StopwatchState> = _stopwatchState.asStateFlow()
    private var timerJob: Job? = null

    fun startOrResumeStopwatch() {
        if (_stopwatchState.value.isRunning) return
        _stopwatchState.value = _stopwatchState.value.copy(isRunning = true)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _stopwatchState.update { state ->
                    state.copy(elapsedMs = state.elapsedMs + 1000L)
                }
            }
        }
    }

    fun pauseStopwatch() {
        if (!_stopwatchState.value.isRunning) return
        _stopwatchState.value = _stopwatchState.value.copy(isRunning = false)
        timerJob?.cancel()
        timerJob = null
    }

    fun resetStopwatch() {
        _stopwatchState.value = StopwatchState(isRunning = false, elapsedMs = 0L)
        timerJob?.cancel()
        timerJob = null
    }

    fun saveSession(
        title: String,
        description: String,
        tagName: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isSaving.value = true
                _error.value = null

                val tagId = tagRepository.getOrCreateTagId(tagName)
                val durationMs = stopwatchState.value.elapsedMs.coerceAtLeast(0L)
                val endTime = System.currentTimeMillis()
                val startTime = (endTime - durationMs).coerceAtLeast(0L)

                sessionRepository.addSession(
                    title = title,
                    description = description,
                    tagId = tagId,
                    startTime = startTime,
                    endTime = endTime,
                    durationMs = durationMs
                )

                resetStopwatch()
                onDone()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save session"
            } finally {
                _isSaving.value = false
            }
        }
    }
}