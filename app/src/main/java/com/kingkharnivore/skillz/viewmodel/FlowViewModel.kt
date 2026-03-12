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
import com.kingkharnivore.skillz.model.state.flow.ArcSummaryUiModel
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel
import com.kingkharnivore.skillz.model.state.flow.FlowUiState
import com.kingkharnivore.skillz.model.state.flow.StopwatchState
import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import com.kingkharnivore.skillz.ui.service.AliveFlowServiceController
import com.kingkharnivore.skillz.ui.service.SurgeHapticsManager
import com.kingkharnivore.skillz.utils.arc.ArcPrefs
import com.kingkharnivore.skillz.utils.arc.ArcRules
import com.kingkharnivore.skillz.utils.score.ScoreCalculator
import com.kingkharnivore.skillz.utils.user.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class BeamOutcome(
    val beamId: Long? = null,
    val eligibleMs: Long = 0L,
    val bonusPoints: Int = 0,
    val multiplier: Double? = null
)

enum class FlowEndAction {
    SAVE_FLOW,
    CONTINUE_ARC,
    COMPLETE_ARC
}

const val BEAM_MIN_ELIGIBLE_MS = 60_000L // 1 minute

@HiltViewModel
class FlowViewModel @Inject constructor(
    private val tagRepository: JourneyRepository,
    private val sessionRepository: FlowRepository,
    private val focusSessionRepository: AliveFlowRepository,
    private val aliveFlowServiceController: AliveFlowServiceController,
    private val beamRepository: BeamRepository,
    private val arcPrefs: ArcPrefs,
    private val userPrefs: UserPrefs,
    private val surgeHapticsManager: SurgeHapticsManager
) : ViewModel() {

    val ongoingSession: StateFlow<OngoingSessionEntity?> =
        focusSessionRepository.getOngoingSession()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(FlowUiState())
    val uiState: StateFlow<FlowUiState> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _lastReward = MutableStateFlow<FlowRewardUiModel?>(null)
    val lastReward: StateFlow<FlowRewardUiModel?> = _lastReward.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * ✅ Continue Arc UX:
     * - When Continue Arc is clicked, we save + set lastReward
     * - UI shows reward dialog
     * - When dialog closes, FlowScreen calls beginNextFlowAfterContinue()
     */
    private val _awaitingNextFlowAfterContinue = MutableStateFlow(false)
    val awaitingNextFlowAfterContinue: StateFlow<Boolean> = _awaitingNextFlowAfterContinue.asStateFlow()

    val tags: StateFlow<List<TagEntity>> =
        tagRepository.getAllTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suggestedTags = combine(
        tagRepository.getAllTags(),
        sessionRepository.getAllSessions()
    ) { tags, sessions ->
        val usedTagIds: Set<Long> = sessions.mapTo(mutableSetOf()) { it.tagId }
        tags.filter { it.id in usedTagIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Stopwatch internals
    private var baseStartTimeMs: Long? = null
    private var accumulatedBeforeStartMs: Long = 0L
    private var tickerJob: Job? = null

    // ARC runtime
    private var arcState: ArcRuntimeState? = null

    private val _exitAfterReward = MutableStateFlow(false)
    val exitAfterReward: StateFlow<Boolean> = _exitAfterReward.asStateFlow()

    fun consumeExitAfterReward(): Boolean {
        val shouldExit = _exitAfterReward.value
        _exitAfterReward.value = false
        return shouldExit
    }

    private fun isArcExpired(nowMs: Long, state: ArcRuntimeState): Boolean {
        val delta = nowMs - state.lastSessionEndTimeMs
        return delta > ArcRules.GRACE_WINDOW_MS
    }

    private fun isZeroDuration(durationMs: Long): Boolean = durationMs <= 0L

    private fun nextArcMultiplier(chainBase: Double, realDurationMs: Long): Pair<Double, Boolean> {
        val leveledUp = realDurationMs >= ArcRules.PROGRESS_STEP_MS // 10m+
        val next = if (leveledUp) chainBase + ArcRules.STEP else chainBase
        return next to leveledUp
    }

    /**
     * ✅ Refresh grace window WITHOUT reviving expired arcs.
     * This prevents "Arc expires while reward dialog is open", but does not extend beyond grace.
     */
    private fun refreshArcGraceWindowNowIfValid() {
        val s = arcState ?: return
        val now = System.currentTimeMillis()
        if (isArcExpired(now, s)) return // do NOT revive expired arcs

        val refreshed = s.copy(lastSessionEndTimeMs = now)
        arcState = refreshed
        viewModelScope.launch { arcPrefs.save(refreshed) }
        syncArcUi()
    }

    private fun syncArcUi() {
        val s = arcState
        val now = System.currentTimeMillis()

        val isRunning = _uiState.value.stopwatch.isRunning
        val elapsed = _uiState.value.stopwatch.elapsedMs

        // ✅ "Between flows" means: no active flow time banked
        val isBetweenFlows = !isRunning && elapsed == 0L

        // ✅ "Paused mid-flow" means: not running, but elapsed > 0
        val isPausedMidFlow = !isRunning && elapsed > 0L

        _uiState.update { old ->
            if (s == null) {
                old.copy(
                    isInArc = false,
                    arcIsPending = false,
                    arcMultiplier = null,
                    arcProgressMs = 0L,
                    arcNextIndex = null,
                    arcGraceRemainingMs = null,
                    arcPauseRemainingMs = null
                )
            } else {
                val graceLeft = if (isBetweenFlows) computeGraceRemainingMs(now, s) else null
                val pauseLeft = if (isPausedMidFlow) computePauseRemainingMs(now, s) else null

                old.copy(
                    isInArc = true,
                    arcIsPending = s.isPending,
                    arcMultiplier = s.multiplier,
                    arcProgressMs = s.progressMs,
                    arcNextIndex = s.sessionCountInArc + 1,
                    arcGraceRemainingMs = graceLeft,
                    arcPauseRemainingMs = pauseLeft
                )
            }
        }
        ensureArcCountdownRunningIfNeeded()
    }

    private fun computeGraceRemainingMs(
        now: Long,
        s: ArcRuntimeState
    ): Long {
        val elapsedSinceLastEnd = now - s.lastSessionEndTimeMs
        val remaining = ArcRules.GRACE_WINDOW_MS - elapsedSinceLastEnd
        return remaining.coerceAtLeast(0L)
    }

    private fun computePauseRemainingMs(
        now: Long,
        s: ArcRuntimeState
    ): Long {
        val pauseBudget = arcPauseBudgetMs(s)

        val activePauseElapsed = if (s.pauseStartedAtMs != null) {
            now - s.pauseStartedAtMs
        } else {
            0L
        }

        val totalPauseUsed = s.pauseUsedMs + activePauseElapsed
        val remaining = pauseBudget - totalPauseUsed

        return remaining.coerceAtLeast(0L)
    }

    init {
        viewModelScope.launch {
            userPrefs.showScoreUi.collect { enabled ->
                _uiState.update { it.copy(showScoreUi = enabled) }
            }
        }
        viewModelScope.launch {
            userPrefs.calmMode.collect { enabled ->
                _uiState.update { it.copy(calmMode = enabled) }
            }
        }
        viewModelScope.launch {
            // 1) Restore ongoing session
            val ongoing = focusSessionRepository.getOngoingSession().firstOrNull()

            // 2) Restore Arc state (Room beats DataStore)
            if (ongoing?.arcId != null) {
                arcState = ArcRuntimeState(
                    arcId = ongoing.arcId,
                    isPending = (ongoing.arcSessionCountInArc ?: 0) < 2,
                    multiplier = ongoing.arcChainBase ?: ArcRules.START_MULTIPLIER,
                    progressMs = 0L, // strict arcs: no banking
                    lastSessionEndTimeMs = ongoing.arcLastSessionEndTimeMs ?: 0L,
                    sessionCountInArc = ongoing.arcSessionCountInArc ?: 0
                )
                arcPrefs.save(arcState!!)
            } else {
                arcState = arcPrefs.load()

                val now = System.currentTimeMillis()
                if (ongoing == null) {
                    arcState?.let { s ->
                        if (isArcExpired(now, s)) {
                            arcState = null
                            syncArcUi()
                            arcPrefs.clear()
                        }
                    }
                }
            }

            syncArcUi()

            // 3) Restore stopwatch
            ongoing?.let { entity ->
                baseStartTimeMs = entity.baseStartTimeMs
                accumulatedBeforeStartMs = entity.accumulatedBeforeStartMs

                val elapsed = if (entity.isRunning && baseStartTimeMs != null) {
                    accumulatedBeforeStartMs + (System.currentTimeMillis() - baseStartTimeMs!!).coerceAtLeast(0L)
                } else {
                    accumulatedBeforeStartMs
                }

                _uiState.update {
                    it.copy(
                        title = entity.title,
                        description = entity.description,
                        tagName = entity.tagName,
                        isInFlowMode = entity.isInFlowMode,
                        isSurgeOn = entity.isSurgeOn,
                        surgePlannedMs = entity.surgePlannedMs,
                        stopwatch = StopwatchState(
                            isRunning = entity.isRunning,
                            elapsedMs = elapsed
                        )
                    )
                }

                if (entity.isRunning) startTicker()
            }
        }
    }

    fun clearLastReward() {
        _lastReward.value = null
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

    // -------- Stopwatch / Focus Mode logic --------

    fun startOrResumeStopwatch() {
        if (_uiState.value.stopwatch.isRunning) return

        // ✅ If we're resuming a flow (already has elapsed), do NOT apply arc grace expiration.
        val isResumingSameFlow = accumulatedBeforeStartMs > 0L

        if (!isResumingSameFlow) {
            arcState?.let { s ->
                val now = System.currentTimeMillis()
                if (isArcExpired(now, s)) {
                    concludeArc(reason = "expired")
                }
            }
        }

        val now = System.currentTimeMillis()
        baseStartTimeMs = now
        _uiState.update { it.copy(stopwatch = it.stopwatch.copy(isRunning = true)) }
        applyArcPauseAccountingOnResume(now)
        arcCountdownJob?.cancel()
        arcCountdownJob = null
        if (arcState == null) {
            // arc was concluded due to pause limit; resume flow normally
        }
        startTicker()
        saveOngoing()

        if (!isResumingSameFlow && _uiState.value.isSurgeOn) {
            surgeHapticsManager.playStarted()
        }
    }

    private fun concludeArc(reason: String) {
        val s = arcState ?: return
        val arcId = s.arcId

        viewModelScope.launch {
            val arcSessions = sessionRepository.getSessionsForArc(arcId)
            val summary = if (arcSessions.isNotEmpty()) {
                ArcSummaryUiModel(
                    totalSessions = arcSessions.size,
                    totalDurationMs = arcSessions.sumOf { it.durationMs },
                    totalFinalPoints = arcSessions.sumOf { it.scyraPoints },
                    totalArcBonusPoints = arcSessions.sumOf { it.arcBonusPoints },
                    peakMultiplier = arcSessions.mapNotNull { it.arcMultiplierUsed }.maxOrNull()
                        ?: 1.0
                )
            } else null

            if (summary != null) {
                // ✅ reuse reward dialog to show Arc Summary
                _lastReward.value = FlowRewardUiModel(
                    minutes = 0,
                    baseScyraPoints = 0,
                    tenMinuteBonuses = 0,
                    thirtyMinuteBonuses = 0,
                    sixtyMinuteBonuses = 0,
                    beamEligibleMs = 0L,
                    beamBonusPoints = 0,
                    beamMultiplier = null,
                    finalScyraPoints = 0,
                    surgePoints = 0,
                    arcSummary = summary,
                    isArcOnlySummary = true
                )
            }

            arcState = null
            syncArcUi()
            arcPrefs.clear()
            saveOngoing()
        }
    }

    private fun arcPauseBudgetMs(s: ArcRuntimeState): Long {
        val nextIndex = s.sessionCountInArc + 1
        return when {
            nextIndex <= 3 -> ArcRules.PAUSE_BUDGET_EARLY_MS  // flows 1..3
            nextIndex >= 10 -> ArcRules.PAUSE_BUDGET_ULTRA_MS // 10+
            else -> ArcRules.PAUSE_BUDGET_LATE_MS            // flows 4..9
        }
    }

    private fun clearArcPersistedAsync() {
        viewModelScope.launch {
            arcPrefs.clear()
        }
    }

    fun pauseStopwatch() {
        if (!_uiState.value.stopwatch.isRunning) return
        val now = System.currentTimeMillis()

        baseStartTimeMs?.let { base ->
            accumulatedBeforeStartMs += (now - base).coerceAtLeast(0L)
        }
        baseStartTimeMs = null

        // ✅ Track arc pause start
        arcState = arcState?.let { s ->
            if (s.pauseStartedAtMs == null) s.copy(pauseStartedAtMs = now) else s
        }

        arcState?.let { s ->
            viewModelScope.launch { arcPrefs.save(s) } // ✅ persist pause start
        }
        syncArcUi()

        _uiState.update {
            it.copy(stopwatch = it.stopwatch.copy(isRunning = false, elapsedMs = accumulatedBeforeStartMs))
        }
        stopTicker()
        startArcCountdown() // ✅ keep countdown ticking while paused
        saveOngoing()
    }

    private var arcCountdownJob: Job? = null

    private fun startArcCountdown() {
        if (arcCountdownJob?.isActive == true) return

        arcCountdownJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)

                if (arcState == null) {
                    arcCountdownJob?.cancel()
                    arcCountdownJob = null
                    return@launch
                }

                val running = _uiState.value.stopwatch.isRunning
                val now = System.currentTimeMillis()

                // Only tick/compute countdowns when NOT running (paused mid-flow OR between flows)
                if (!running) {
                    syncArcUi()

                    // re-read latest state after sync
                    val s = arcState ?: continue

                    // Prefer using the canonical stopwatch source (avoids 1s UI lag)
                    val elapsed = accumulatedBeforeStartMs

                    val isBetweenFlows = elapsed == 0L
                    val isPausedMidFlow = elapsed > 0L

                    // 1) Grace expiry between flows
                    if (isBetweenFlows && isArcExpired(now, s)) {
                        concludeArc("expired")
                        continue
                    }

                    // 2) Pause budget expiry while paused mid-flow
                    if (isPausedMidFlow && s.pauseStartedAtMs != null) {
                        val pauseLeft = computePauseRemainingMs(now, s)
                        if (pauseLeft <= 0L) {
                            concludeArc("pause_limit")
                            continue
                        }
                    }
                }
            }
        }
    }

    private fun ensureArcCountdownRunningIfNeeded() {
        val s = arcState ?: return
        if (_uiState.value.stopwatch.isRunning) return
        // only if arc is still valid or pause is active; job itself handles expiry
        startArcCountdown()
    }

    private fun applyArcPauseAccountingOnResume(now: Long) {
        val s = arcState ?: return
        val started = s.pauseStartedAtMs ?: return

        val pausedDelta = (now - started).coerceAtLeast(0L)
        val newUsed = s.pauseUsedMs + pausedDelta
        val budget = arcPauseBudgetMs(s)

        if (newUsed > budget) {
            concludeArc(reason = "pause_limit") // ✅ show summary and clear arc
            return
        }

        arcState = s.copy(
            pauseUsedMs = newUsed,
            pauseStartedAtMs = null
        )
        viewModelScope.launch { arcPrefs.save(arcState!!) }
        syncArcUi()
    }

    fun resetStopwatch() {
        baseStartTimeMs = null
        accumulatedBeforeStartMs = 0L
        _uiState.update { it.copy(stopwatch = StopwatchState(isRunning = false, elapsedMs = 0L)) }
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
                _uiState.update { it.copy(stopwatch = it.stopwatch.copy(elapsedMs = elapsed)) }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun enterFocusMode() {
        if (!_uiState.value.stopwatch.isRunning) startOrResumeStopwatch()
        _uiState.update { it.copy(isInFlowMode = true) }
        saveOngoing()
        aliveFlowServiceController.start()
    }

    fun exitFocusMode() {
        if (uiState.value.stopwatch.isRunning) pauseStopwatch()
        _uiState.update { it.copy(isInFlowMode = false) }
        saveOngoing()
        aliveFlowServiceController.stop()
    }

    private fun saveOngoing() {
        val state = _uiState.value
        val arc = arcState

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
                surgePlannedMs = state.surgePlannedMs,
                createdAt = System.currentTimeMillis(),
                arcId = arc?.arcId,
                arcChainBase = arc?.multiplier,
                arcSessionCountInArc = arc?.sessionCountInArc,
                arcLastSessionEndTimeMs = arc?.lastSessionEndTimeMs
            )
            focusSessionRepository.saveOngoingSession(entity)
        }
    }

    private suspend fun clearOngoing() {
        focusSessionRepository.clearOngoingSession()
    }

    // -------- Surge --------

    fun setSurgePlannedMinutes(minutes: Int) {
        val mins = minutes.coerceAtLeast(1)
        val plannedMs = mins * 60_000L

        _uiState.update {
            it.copy(
                isSurgeOn = true,
                surgePlannedMs = plannedMs
            )
        }

        surgeHapticsManager.playArmed()
        saveOngoing()
    }

    fun clearSurgeIfAllowed() {
        if (_uiState.value.stopwatch.elapsedMs > 0L) return
        _uiState.update { it.copy(isSurgeOn = false, surgePlannedMs = null) }
        saveOngoing()
    }

    fun isSurgeLocked(): Boolean = _uiState.value.stopwatch.elapsedMs > 0L

    // -------- Beam overlap scoring --------

    private suspend fun computeBeamOutcomeForSession(
        tagId: Long,
        sessionStart: Long,
        sessionEnd: Long,
        sessionDurationMs: Long
    ): BeamOutcome {
        val beams = beamRepository.getBeamsOverlappingWindow(sessionStart, sessionEnd)

        val candidates = beams.asSequence()
            .filter { it.tagId == tagId }
            .map { beam ->
                val overlap = ScoreCalculator.overlapMs(
                    aStart = sessionStart,
                    aEnd = sessionEnd,
                    bStart = beam.startTime,
                    bEnd = beam.endTime
                )
                beam to overlap
            }
            .filter { (_, overlap) -> overlap > 0L }
            .toList()

        val (bestBeam, eligibleMs) = candidates
            .maxWithOrNull(
                compareBy<Pair<BeamEntity, Long>> { it.second }
                    .thenByDescending { -kotlin.math.abs(it.first.startTime - sessionStart) }
            )
            ?: return BeamOutcome()

        if (eligibleMs < BEAM_MIN_ELIGIBLE_MS) {
            return BeamOutcome(
                beamId = bestBeam.id,
                eligibleMs = eligibleMs,
                bonusPoints = 0,
                multiplier = null
            )
        }

        val res = ScoreCalculator.scoreWithBeam(
            sessionStart = sessionStart,
            sessionEnd = sessionEnd,
            sessionDurationMs = sessionDurationMs,
            beamStart = bestBeam.startTime,
            beamEnd = bestBeam.endTime,
            beamDurationMs = bestBeam.durationMs,
            continuousEngagedMsInThisSession = eligibleMs
        )

        return BeamOutcome(
            beamId = bestBeam.id,
            eligibleMs = eligibleMs,
            bonusPoints = res.beamBonusPoints.coerceAtLeast(0),
            multiplier = res.appliedMultiplier
        )
    }

    fun prefillFromBeam(tagName: String) {
        _uiState.update { it.copy(tagName = tagName) }
        saveOngoing()
    }

    /**
     * Called by FlowScreen when user closes the reward dialog after CONTINUE_ARC.
     */
    fun beginNextFlowAfterContinue() {
        if (!_awaitingNextFlowAfterContinue.value) return

        val keepTag = _uiState.value.tagName
        val keepArc = arcState

        _lastReward.value = null
        _awaitingNextFlowAfterContinue.value = false

        baseStartTimeMs = null
        accumulatedBeforeStartMs = 0L
        stopTicker()
        aliveFlowServiceController.stop()

        // ✅ Preserve user prefs (calmMode/showScoreUi) by copying from old state
        _uiState.update { old ->
            old.copy(
                title = "",
                description = "",
                tagName = keepTag,
                stopwatch = StopwatchState(isRunning = false, elapsedMs = 0L),
                isInFlowMode = false,
                isSurgeOn = false,
                surgePlannedMs = null,

                // ARC UI
                isInArc = keepArc != null,
                arcIsPending = keepArc?.isPending ?: false,
                arcMultiplier = keepArc?.multiplier,
                arcProgressMs = keepArc?.progressMs ?: 0L,
                arcNextIndex = keepArc?.let { it.sessionCountInArc + 1 },
                arcGraceRemainingMs = null,
                arcPauseRemainingMs = null
            )
        }

        viewModelScope.launch { clearOngoing() }
    }

    // -------- Arc end actions --------

    fun onEndFlowClicked(action: FlowEndAction) {
        viewModelScope.launch { saveWithArcBehavior(endMode = action) }
    }

    private suspend fun saveWithArcBehavior(endMode: FlowEndAction) {
        val state = _uiState.value
        val title = state.title.trim()
        val tagName = state.tagName.trim()
        val description = state.description.trim()

        if (title.isBlank() || tagName.isBlank()) {
            _error.value = "Title and Skill are required"
            return
        }

        val realDurationMs = state.stopwatch.elapsedMs.coerceAtLeast(0L)
        if (isZeroDuration(realDurationMs)) {
            _error.value = "Start the timer before saving."
            return
        }

        _isSaving.value = true
        _error.value = null

        try {
            val sessionEnd = System.currentTimeMillis()
            val sessionStart = (sessionEnd - realDurationMs).coerceAtLeast(0L)

            val tagId = tagRepository.getOrCreateTagId(tagName)

            val surgePoints = ScoreCalculator.surgePoints(
                surgePlannedMs = state.surgePlannedMs,
                actualDurationMs = realDurationMs
            )

            val planned = state.surgePlannedMs

            val surgeSucceeded =
                state.isSurgeOn &&
                        planned != null &&
                        realDurationMs <= planned

            val breakdown = ScoreCalculator.breakdownFromDuration(realDurationMs)
            val baseScyra = breakdown.totalPoints

            val beam = computeBeamOutcomeForSession(
                tagId = tagId,
                sessionStart = sessionStart,
                sessionEnd = sessionEnd,
                sessionDurationMs = realDurationMs
            )

            val beforeArc = baseScyra + beam.bonusPoints

            // expire arc if grace exceeded
            // ✅ expire arc based on whether it was valid WHEN THIS FLOW STARTED
            var localArc = arcState
            if (localArc != null && isArcExpired(sessionStart, localArc)) {
                clearArcPersistedAsync()
                localArc = null
                arcState = null
                syncArcUi()
            }

            val arcIdForSummary: Long? = localArc?.arcId

            val isInExistingArc = (localArc != null)

            // If Continue Arc with no arc yet: create arc with session #1 at multiplier 1.0 (used), next base = 1.3
            if (!isInExistingArc && endMode == FlowEndAction.CONTINUE_ARC) {
                val sessionId1 = sessionRepository.addSession(
                    title = title,
                    description = description,
                    tagId = tagId,
                    startTime = sessionStart,
                    endTime = sessionEnd,
                    durationMs = realDurationMs,
                    surgePlannedMs = state.surgePlannedMs,
                    surgePoints = surgePoints,
                    beamId = beam.beamId,
                    beamEligibleMs = beam.eligibleMs,
                    beamBonusPoints = beam.bonusPoints,
                    beamMultiplier = beam.multiplier,
                    scyraPoints = beforeArc
                )

                val arcId = System.currentTimeMillis()

                sessionRepository.updateArcFields(
                    sessionId = sessionId1,
                    arcId = arcId,
                    arcIndex = 1,
                    arcMultiplierUsed = 1.0,
                    arcBonusPoints = 0,
                    finalScyraPoints = beforeArc
                )

                arcState = ArcRuntimeState(
                    arcId = arcId,
                    isPending = true,
                    multiplier = ArcRules.START_MULTIPLIER,
                    progressMs = 0L,
                    lastSessionEndTimeMs = sessionEnd,
                    sessionCountInArc = 1
                )
                arcPrefs.save(arcState!!)
                syncArcUi()

                // ✅ First flow in arc: do NOT show arc UI in rewards
                _lastReward.value = FlowRewardUiModel(
                    minutes = breakdown.minutes,
                    baseScyraPoints = baseScyra,
                    tenMinuteBonuses = breakdown.tenMinuteBonuses,
                    thirtyMinuteBonuses = breakdown.thirtyMinuteBonuses,
                    sixtyMinuteBonuses = breakdown.sixtyMinuteBonuses,
                    beamEligibleMs = beam.eligibleMs,
                    beamBonusPoints = beam.bonusPoints,
                    beamMultiplier = beam.multiplier,
                    finalScyraPoints = beforeArc,
                    surgePoints = surgePoints,
                    arcIndexInArc = 1,
                    arcMultiplierUsed = null,
                    arcBonusPoints = 0,
                    arcNextMultiplier = arcState?.multiplier,
                    arcProgressTowardNextMs = arcState?.progressMs ?: 0L,
                    arcDidLevelUp = false
                )

                _awaitingNextFlowAfterContinue.value = true

                baseStartTimeMs = null
                accumulatedBeforeStartMs = 0L
                stopTicker()
                aliveFlowServiceController.stop()
                clearOngoing()

                return
            }

            // If arc exists, apply it
            var arcMultiplierUsed: Double? = null
            var arcBonusPoints = 0
            var arcIndex: Int? = null
            var arcDidLevelUp = false
            var nextMultiplier: Double? = null

            var finalScyra = beforeArc

            if (isInExistingArc) {
                val s = localArc!!

                arcIndex = s.sessionCountInArc + 1

                val res = ScoreCalculator.arcMath(
                    beforeArcPoints = beforeArc,
                    chainBase = s.multiplier,
                    durationMs = realDurationMs
                )

                arcMultiplierUsed = res.arcMultiplierUsed
                arcBonusPoints = res.arcBonusPoints
                finalScyra = res.finalPoints

                // ✅ enforce leveling rule here (source of truth)
                val (forcedNext, forcedDidLevel) = nextArcMultiplier(
                    chainBase = s.multiplier,
                    realDurationMs = realDurationMs
                )
                arcDidLevelUp = forcedDidLevel
                nextMultiplier = forcedNext
            }

            val insertedId = sessionRepository.addSession(
                title = title,
                description = description,
                tagId = tagId,
                startTime = sessionStart,
                endTime = sessionEnd,
                durationMs = realDurationMs,
                surgePlannedMs = state.surgePlannedMs,
                surgePoints = surgePoints,
                beamId = beam.beamId,
                beamEligibleMs = beam.eligibleMs,
                beamBonusPoints = beam.bonusPoints,
                beamMultiplier = beam.multiplier,
                scyraPoints = finalScyra
            )

            if (isInExistingArc) {
                val s = localArc!!

                sessionRepository.updateArcFields(
                    sessionId = insertedId,
                    arcId = s.arcId,
                    arcIndex = arcIndex ?: (s.sessionCountInArc + 1),
                    arcMultiplierUsed = arcMultiplierUsed ?: s.multiplier,
                    arcBonusPoints = arcBonusPoints,
                    finalScyraPoints = finalScyra
                )

                val newCount = s.sessionCountInArc + 1

                arcState = s.copy(
                    isPending = newCount < 2,
                    multiplier = nextMultiplier ?: s.multiplier,
                    progressMs = 0L,
                    lastSessionEndTimeMs = sessionEnd,
                    sessionCountInArc = newCount
                )

                arcPrefs.save(arcState!!)
                syncArcUi()
            }

            val baseReward = FlowRewardUiModel(
                minutes = breakdown.minutes,
                baseScyraPoints = baseScyra,
                tenMinuteBonuses = breakdown.tenMinuteBonuses,
                thirtyMinuteBonuses = breakdown.thirtyMinuteBonuses,
                sixtyMinuteBonuses = breakdown.sixtyMinuteBonuses,
                beamEligibleMs = beam.eligibleMs,
                beamBonusPoints = beam.bonusPoints,
                beamMultiplier = beam.multiplier,
                finalScyraPoints = finalScyra,
                surgePoints = surgePoints,
                arcIndexInArc = arcIndex,
                arcMultiplierUsed = arcMultiplierUsed,
                arcBonusPoints = arcBonusPoints,
                arcNextMultiplier = arcState?.multiplier,
                arcProgressTowardNextMs = arcState?.progressMs ?: 0L,
                arcDidLevelUp = arcDidLevelUp
            )

            when (endMode) {

                FlowEndAction.COMPLETE_ARC -> {

                    if (state.isSurgeOn && state.surgePlannedMs != null) {
                        if (surgeSucceeded) {
                            surgeHapticsManager.playCompletedSuccess()
                        } else {
                            surgeHapticsManager.playCompletedFail()
                        }
                    }

                    val arcId = arcIdForSummary
                    val summary = if (arcId != null) {
                        val arcSessions = sessionRepository.getSessionsForArc(arcId)
                        if (arcSessions.isNotEmpty()) {
                            ArcSummaryUiModel(
                                totalSessions = arcSessions.size,
                                totalDurationMs = arcSessions.sumOf { it.durationMs },
                                totalFinalPoints = arcSessions.sumOf { it.scyraPoints },
                                totalArcBonusPoints = arcSessions.sumOf { it.arcBonusPoints },
                                peakMultiplier = arcSessions.mapNotNull { it.arcMultiplierUsed }
                                    .maxOrNull() ?: 1.0
                            )
                        } else null
                    } else null

                    _lastReward.value = baseReward.copy(arcSummary = summary)
                    _exitAfterReward.value = true

                    clearArcPersistedAsync()
                    arcState = null

                    // ✅ hard reset stopwatch UI so Arc UI doesn't think we're "paused mid-flow"
                    baseStartTimeMs = null
                    accumulatedBeforeStartMs = 0L
                    _uiState.update { it.copy(stopwatch = StopwatchState(
                        isRunning = false,
                        elapsedMs = 0L
                    )
                    ) }

                    stopTicker()
                    syncArcUi() // (will no-op arc UI since arcState=null)

                    aliveFlowServiceController.stop()
                    clearOngoing()
                }

                FlowEndAction.SAVE_FLOW -> {

                    if (state.isSurgeOn && state.surgePlannedMs != null) {
                        if (surgeSucceeded) {
                            surgeHapticsManager.playCompletedSuccess()
                        } else {
                            surgeHapticsManager.playCompletedFail()
                        }
                    }

                    _lastReward.value = baseReward
                    _exitAfterReward.value = true

                    if (arcState != null) {
                        clearArcPersistedAsync()
                        arcState = null
                    }

                    // ✅ reset stopwatch UI
                    baseStartTimeMs = null
                    accumulatedBeforeStartMs = 0L
                    _uiState.update { it.copy(stopwatch = StopwatchState(
                        isRunning = false,
                        elapsedMs = 0L
                    )
                    ) }

                    stopTicker()
                    syncArcUi()

                    aliveFlowServiceController.stop()
                    clearOngoing()
                }

                FlowEndAction.CONTINUE_ARC -> {

                    if (state.isSurgeOn && state.surgePlannedMs != null) {
                        if (surgeSucceeded) {
                            surgeHapticsManager.playCompletedSuccess()
                        } else {
                            surgeHapticsManager.playCompletedFail()
                        }
                    }

                    _lastReward.value = baseReward
                    _awaitingNextFlowAfterContinue.value = true

                    baseStartTimeMs = null
                    accumulatedBeforeStartMs = 0L
                    _uiState.update {
                        it.copy(
                            stopwatch = StopwatchState(
                                isRunning = false,
                                elapsedMs = 0L
                            )
                        )
                    }

                    stopTicker()
                    syncArcUi() // arcState still exists; countdown will start via syncArcUi()

                    aliveFlowServiceController.stop()
                    clearOngoing()
                }
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to save session"
        } finally {
            _isSaving.value = false
        }
    }
}
