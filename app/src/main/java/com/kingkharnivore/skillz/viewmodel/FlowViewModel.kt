package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.BeamEntity
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.data.repository.BeamRepository
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.ui.service.AliveFlowServiceController
import com.kingkharnivore.skillz.utils.score.BeamScoreCalculator
import com.kingkharnivore.skillz.utils.score.ScoreCalculator
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
    val isInFlowMode: Boolean = false,
    val isSurgeOn: Boolean = false,
    val surgePlannedMs: Long? = null
)

data class FlowRewardUiModel(
    val minutes: Int,
    val baseScyraPoints: Int,

    // Bonuses from core engine breakdown
    val tenMinuteBonuses: Int,
    val thirtyMinuteBonuses: Int,
    val sixtyMinuteBonuses: Int,

    // Beam
    val beamBonusPoints: Int,
    val beamMultiplier: Double?,      // null if no beam applied

    // Final
    val finalScyraPoints: Int,

    // Surge (independent)
    val surgePoints: Int
)


@HiltViewModel
class FlowViewModel @Inject constructor(
    private val tagRepository: JourneyRepository,
    private val sessionRepository: FlowRepository,
    private val focusSessionRepository: AliveFlowRepository,
    private val aliveFlowServiceController: AliveFlowServiceController,
    private val beamRepository: BeamRepository
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

    private val _lastReward = MutableStateFlow<FlowRewardUiModel?>(null)
    val lastReward: StateFlow<FlowRewardUiModel?> = _lastReward.asStateFlow()

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
                        isInFlowMode = entity.isInFlowMode,
                        isSurgeOn = entity.isSurgeOn,
                        surgePlannedMs = entity.surgePlannedMs
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
                accumulatedBeforeStartMs = accumulatedBeforeStartMs,
                isSurgeOn = state.isSurgeOn,
                surgePlannedMs = state.surgePlannedMs
            )
            focusSessionRepository.saveOngoingSession(entity)
        }
    }

    fun prepareRewardPreview() {
        viewModelScope.launch {
            _lastReward.value = computeRewardPreview()
        }
    }

    private suspend fun computeRewardPreview(): FlowRewardUiModel {
        val state = _uiState.value

        val durationMs = state.stopwatch.elapsedMs.coerceAtLeast(0L)
        val endTime = System.currentTimeMillis()
        val startTime = (endTime - durationMs).coerceAtLeast(0L)

        // Surge is independent
        val surgePoints = ScoreCalculator.surgePoints(state.surgePlannedMs, durationMs)

        // Base Scyra points (duration bonuses etc.)
        val breakdown = ScoreCalculator.breakdownFromDuration(durationMs)
        val baseScyra = breakdown.totalPoints

        // Beam
        val beamResult = computeBeamForPreview(
            tagName = state.tagName,
            sessionStart = startTime,
            sessionEnd = endTime,
            sessionDurationMs = durationMs
        )

        val finalScyra = baseScyra + beamResult.bonusPoints

        return FlowRewardUiModel(
            minutes = breakdown.minutes,
            baseScyraPoints = baseScyra,

            tenMinuteBonuses = breakdown.tenMinuteBonuses,
            thirtyMinuteBonuses = breakdown.thirtyMinuteBonuses,
            sixtyMinuteBonuses = breakdown.sixtyMinuteBonuses,

            beamBonusPoints = beamResult.bonusPoints,
            beamMultiplier = beamResult.multiplier,

            finalScyraPoints = finalScyra,

            surgePoints = surgePoints
        )
    }

    private data class BeamPreviewResult(
        val beamId: Long? = null,
        val bonusPoints: Int = 0,
        val multiplier: Double? = null
    )

    /**
     * Preview is allowed to be “best effort”.
     * NOTE: this will create the tag if it doesn't exist because you're using getOrCreateTagId.
     * If you want to avoid that side effect, add a JourneyRepository.getTagIdIfExists(name) later.
     */
    private suspend fun computeBeamForPreview(
        tagName: String,
        sessionStart: Long,
        sessionEnd: Long,
        sessionDurationMs: Long
    ): BeamPreviewResult {
        val trimmed = tagName.trim()
        if (trimmed.isBlank()) return BeamPreviewResult()

        val tagId = tagRepository.getOrCreateTagId(trimmed)

        val overlapping: List<BeamEntity> =
            beamRepository.getBeamsOverlappingWindow(sessionStart, sessionEnd)

        val matchingBeam = overlapping.firstOrNull { it.tagId == tagId } ?: return BeamPreviewResult()

        val eligibleMs = BeamScoreCalculator.overlapMs(
            aStart = maxOf(sessionStart, matchingBeam.startTime),
            aEnd = sessionEnd,
            bStart = matchingBeam.startTime,
            bEnd = matchingBeam.endTime
        )

        if (eligibleMs <= 0L) return BeamPreviewResult(beamId = matchingBeam.id)

        val res = BeamScoreCalculator.scoreWithBeam(
            sessionStart = sessionStart,
            sessionEnd = sessionEnd,
            sessionDurationMs = sessionDurationMs,
            beamStart = matchingBeam.startTime,
            beamEnd = matchingBeam.endTime,
            beamDurationMs = matchingBeam.durationMs,
            continuousEngagedMsInThisSession = eligibleMs
        )


        return BeamPreviewResult(
            beamId = matchingBeam.id,
            bonusPoints = res.beamBonusPoints,
            multiplier = res.appliedMultiplier
        )
    }

    fun setSurgePlannedMinutes(minutes: Int) {
        val mins = minutes.coerceAtLeast(1)
        val plannedMs = mins * 60_000L
        _uiState.update {
            it.copy(
                isSurgeOn = true,
                surgePlannedMs = plannedMs
            )
        }
        saveOngoing()
    }

    fun clearSurgeIfAllowed() {
        val elapsed = _uiState.value.stopwatch.elapsedMs
        if (elapsed > 0L) return // 🔒 locked once time starts

        _uiState.update {
            it.copy(
                isSurgeOn = false,
                surgePlannedMs = null
            )
        }
        saveOngoing()
    }

    fun isSurgeLocked(): Boolean {
        return _uiState.value.stopwatch.elapsedMs > 0L
    }

    // Clear persisted focus session (after saving real session or aborting)
    private suspend fun clearOngoing() {
        focusSessionRepository.clearOngoingSession()
    }

    // -------- Saving the real session --------

    fun saveSession(onDone: () -> Unit) {
        val state = _uiState.value
        val title = state.title.trim()
        val tagName = state.tagName.trim()
        val description = state.description.trim()

        if (title.isBlank() || tagName.isBlank()) {
            _error.value = "Title and Skill are required"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null

            try {
                val durationMs = state.stopwatch.elapsedMs.coerceAtLeast(0L)
                val endTime = System.currentTimeMillis()
                val startTime = (endTime - durationMs).coerceAtLeast(0L)

                val tagId = tagRepository.getOrCreateTagId(tagName)

                val surgePoints = ScoreCalculator.surgePoints(
                    surgePlannedMs = state.surgePlannedMs,
                    actualDurationMs = durationMs
                )

                val (beamId, beamBonusPoints, beamMultiplier) = computeBeamOutcome(
                    tagId = tagId,
                    sessionStart = startTime,
                    sessionEnd = endTime,
                    sessionDurationMs = durationMs
                )

                sessionRepository.addSession(
                    title = title,
                    description = description,
                    tagId = tagId,
                    startTime = startTime,
                    endTime = endTime,
                    durationMs = durationMs,
                    surgePlannedMs = state.surgePlannedMs,
                    surgePoints = surgePoints,
                    beamId = beamId,
                    beamBonusPoints = beamBonusPoints,
                    beamMultiplier = beamMultiplier
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

    private suspend fun computeBeamOutcome(
        tagId: Long,
        sessionStart: Long,
        sessionEnd: Long,
        sessionDurationMs: Long
    ): Triple<Long?, Int, Double?> {
        val beams = beamRepository.getBeamsOverlappingWindow(sessionStart, sessionEnd)
        val matchingBeam = beams
            .filter { it.tagId == tagId }
            .minByOrNull { it.startTime } // deterministic
            ?: return Triple(null, 0, null)

        val eligibleMs = BeamScoreCalculator.overlapMs(
            aStart = maxOf(sessionStart, matchingBeam.startTime),
            aEnd = sessionEnd,
            bStart = matchingBeam.startTime,
            bEnd = matchingBeam.endTime
        )
        if (eligibleMs <= 0L) return Triple(matchingBeam.id, 0, null)

        val res = BeamScoreCalculator.scoreWithBeam(
            sessionStart = sessionStart,
            sessionEnd = sessionEnd,
            sessionDurationMs = sessionDurationMs,
            beamStart = matchingBeam.startTime,
            beamEnd = matchingBeam.endTime,
            beamDurationMs = matchingBeam.durationMs,
            continuousEngagedMsInThisSession = eligibleMs
        )
        if (res.beamBonusPoints <= 0) return Triple(null, 0, null)
        return Triple(matchingBeam.id, res.beamBonusPoints, res.appliedMultiplier)
    }

    fun prefillFromBeam(tagName: String) {
        _uiState.update { it.copy(tagName = tagName) }
        saveOngoing()
    }
}