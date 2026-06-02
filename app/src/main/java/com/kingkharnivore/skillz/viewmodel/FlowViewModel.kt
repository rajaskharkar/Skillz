package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.ActiveArcRunRepository
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.data.repository.ArcPlanRepository
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.IdeaGroveRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.data.repository.PulseRepository
import com.kingkharnivore.skillz.domain.shell.ShellRewardEventRecorder
import com.kingkharnivore.skillz.domain.shell.ShellRewardOrchestrator
import com.kingkharnivore.skillz.domain.shell.ShellRewardResult
import com.kingkharnivore.skillz.model.state.flow.ArcSummaryUiModel
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel
import com.kingkharnivore.skillz.model.state.flow.FlowUiState
import com.kingkharnivore.skillz.model.state.flow.StopwatchState
import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import com.kingkharnivore.skillz.ui.navigation.SkillzDestinations
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
import java.util.UUID
import javax.inject.Inject

private fun FlowRewardUiModel.withShellReward(shellReward: ShellRewardResult): FlowRewardUiModel = copy(
    shellPearlsEarned = shellReward.pearlsEarned,
    shellStillwaterUnits = shellReward.stillwaterUnits,
    shellGrantedFindIds = shellReward.grantedFindIds,
    shellDiscoveryIds = shellReward.discoveryIds,
    shellBadgeIds = shellReward.badgeIds
)

enum class FlowEndAction {
    SAVE_FLOW,
    CONTINUE_ARC,
    COMPLETE_ARC
}

data class PendingArcIdeaContinuation(
    val pulseId: Long,
    val pulseTitle: String?,
    val pulseJourneyName: String?
)

@HiltViewModel
class FlowViewModel @Inject constructor(
    private val tagRepository: JourneyRepository,
    private val sessionRepository: FlowRepository,
    private val pulseRepository: PulseRepository,
    private val ideaGroveRepository: IdeaGroveRepository,
    private val focusSessionRepository: AliveFlowRepository,
    private val activeArcRunRepository: ActiveArcRunRepository,
    private val arcPlanRepository: ArcPlanRepository,
    private val aliveFlowServiceController: AliveFlowServiceController,
    private val arcPrefs: ArcPrefs,
    private val userPrefs: UserPrefs,
    private val surgeHapticsManager: SurgeHapticsManager,
    private val shellRewardOrchestrator: ShellRewardOrchestrator,
    private val shellRewardEventRecorder: ShellRewardEventRecorder,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val plannedArcTitleOverride: String? =
        savedStateHandle
            .get<String>(SkillzDestinations.ADD_SKILL_ARG_PLANNED_ARC_TITLE)
            ?.takeIf { it.isNotBlank() }

    private val plannedArcStepIndexOverride: Int? =
        savedStateHandle
            .get<Int>(SkillzDestinations.ADD_SKILL_ARG_PLANNED_ARC_STEP_INDEX)
            ?.takeIf { it >= 0 }

    private val plannedArcTotalStepsOverride: Int? =
        savedStateHandle
            .get<Int>(SkillzDestinations.ADD_SKILL_ARG_PLANNED_ARC_TOTAL_STEPS)
            ?.takeIf { it > 0 }

    private val atlasJourneyOverride: String? =
        savedStateHandle
            .get<String>(SkillzDestinations.ADD_SKILL_ARG_PREFILL_JOURNEY)
            ?.takeIf { it.isNotBlank() }

    private val prefillTitleOverride: String? =
        savedStateHandle
            .get<String>(SkillzDestinations.ADD_SKILL_ARG_PREFILL_TITLE)
            ?.takeIf { it.isNotBlank() }

    private val prefillSoftModeOverride: Boolean =
        savedStateHandle.get<Boolean>(
            SkillzDestinations.ADD_SKILL_ARG_PREFILL_SOFT_MODE
        ) ?: false

    private val originPulseIdOverride: Long? =
        savedStateHandle.get<Long>(SkillzDestinations.ADD_SKILL_ARG_ORIGIN_PULSE_ID)
            ?.takeIf { it > 0L }

    private val hasLaunchOverrides: Boolean =
        !atlasJourneyOverride.isNullOrBlank() ||
                !prefillTitleOverride.isNullOrBlank() ||
                prefillSoftModeOverride ||
                originPulseIdOverride != null ||
                !plannedArcTitleOverride.isNullOrBlank() ||
                plannedArcStepIndexOverride != null ||
                plannedArcTotalStepsOverride != null

    private fun applyLaunchOverrides(state: FlowUiState): FlowUiState {
        val isPulseLaunch = originPulseIdOverride != null
        return state.copy(
            title = prefillTitleOverride ?: state.title,
            tagName = atlasJourneyOverride ?: state.tagName,
            isSoftMode = if (prefillSoftModeOverride) true else state.isSoftMode,
            isSurgeOn = if (prefillSoftModeOverride) false else state.isSurgeOn,
            surgePlannedMs = if (prefillSoftModeOverride) null else state.surgePlannedMs,
            plannedArcTitle = plannedArcTitleOverride ?: state.plannedArcTitle,
            plannedArcStepIndex = plannedArcStepIndexOverride ?: state.plannedArcStepIndex,
            plannedArcTotalSteps = plannedArcTotalStepsOverride ?: state.plannedArcTotalSteps,
            originPulseId = if (isPulseLaunch) originPulseIdOverride else null,
            originPulseTitle = if (isPulseLaunch) prefillTitleOverride else null,
            originPulseJourneyName = if (isPulseLaunch) atlasJourneyOverride else null
        )
    }

    private fun shouldTreatOngoingAsDraft(entity: OngoingSessionEntity): Boolean {
        return !entity.isInFlowMode &&
                !entity.isRunning &&
                entity.accumulatedBeforeStartMs == 0L &&
                entity.baseStartTimeMs == null
    }

    private fun isAbandonedPulseOriginDraft(entity: OngoingSessionEntity?): Boolean {
        return entity?.originPulseId != null && shouldTreatOngoingAsDraft(entity)
    }

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

    private val _awaitingNextFlowAfterContinue = MutableStateFlow(false)
    val awaitingNextFlowAfterContinue: StateFlow<Boolean> =
        _awaitingNextFlowAfterContinue.asStateFlow()

    private val _pendingArcIdeaContinuation = MutableStateFlow<PendingArcIdeaContinuation?>(null)
    val pendingArcIdeaContinuation: StateFlow<PendingArcIdeaContinuation?> =
        _pendingArcIdeaContinuation.asStateFlow()

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

    private var baseStartTimeMs: Long? = null
    private var accumulatedBeforeStartMs: Long = 0L
    private var tickerJob: Job? = null
    private var ongoingCreatedAtMs: Long = System.currentTimeMillis()
    private var currentFlowInstanceId: String = UUID.randomUUID().toString()

    private var arcState: ArcRuntimeState? = null

    private val _exitAfterReward = MutableStateFlow(false)
    val exitAfterReward: StateFlow<Boolean> = _exitAfterReward.asStateFlow()

    private var arcCountdownJob: Job? = null

    fun consumeExitAfterReward(): Boolean {
        val shouldExit = _exitAfterReward.value
        _exitAfterReward.value = false
        return shouldExit
    }

    fun isModeLocked(): Boolean = _uiState.value.stopwatch.elapsedMs > 0L

    fun setSoftMode(enabled: Boolean) {
        if (isModeLocked()) return

        _uiState.update { current ->
            current.copy(
                isSoftMode = enabled,
                isSurgeOn = if (enabled) false else current.isSurgeOn,
                surgePlannedMs = if (enabled) null else current.surgePlannedMs
            )
        }
        saveOngoing()
    }

    fun setSoftModeAndConcludeArc() {
        if (isModeLocked()) return

        val hadArc = arcState != null

        _uiState.update { current ->
            current.copy(
                isSoftMode = true,
                isSurgeOn = false,
                surgePlannedMs = null
            )
        }
        saveOngoing()

        if (hadArc) {
            concludeArc("soft_mode")
        }
    }

    fun recordPulse(
        title: String,
        description: String,
        tagName: String,
        attachToCurrentFlow: Boolean
    ) {
        viewModelScope.launch {
            val trimmedTitle = title.trim()
            val trimmedDescription = description.trim()
            val trimmedTag = tagName.trim()

            if (trimmedTitle.isBlank() && trimmedDescription.isBlank()) {
                _error.value = "Add a title or description to record this moment"
                return@launch
            }

            try {
                val tagId = if (trimmedTag.isBlank()) {
                    null
                } else {
                    tagRepository.getOrCreateTagId(trimmedTag)
                }

                val shouldAttach = attachToCurrentFlow && _uiState.value.isInFlowMode

                pulseRepository.addPulse(
                    title = trimmedTitle,
                    description = trimmedDescription,
                    tagId = tagId,
                    parentSessionId = null,
                    parentFlowInstanceId = if (shouldAttach) currentFlowInstanceId else null,
                    arcId = if (shouldAttach) arcState?.arcId else null
                )

                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save pulse"
            }
        }
    }

    private fun isArcExpired(nowMs: Long, state: ArcRuntimeState): Boolean {
        val delta = nowMs - state.lastSessionEndTimeMs
        return delta > ArcRules.GRACE_WINDOW_MS
    }

    private fun isZeroDuration(durationMs: Long): Boolean = durationMs <= 0L

    private fun nextArcMultiplier(chainBase: Double, realDurationMs: Long): Pair<Double, Boolean> {
        val leveledUp = realDurationMs >= ArcRules.PROGRESS_STEP_MS
        val next = if (leveledUp) chainBase + ArcRules.STEP else chainBase
        return next to leveledUp
    }

    private fun refreshArcGraceWindowNowIfValid() {
        val s = arcState ?: return
        val now = System.currentTimeMillis()
        if (isArcExpired(now, s)) return

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

        val isBetweenFlows = !isRunning && elapsed == 0L
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
            val storedOngoing = focusSessionRepository.getOngoingSession().firstOrNull()
            val ongoing = if (isAbandonedPulseOriginDraft(storedOngoing)) {
                clearOngoing()
                null
            } else {
                storedOngoing
            }

            if (ongoing?.arcId != null) {
                arcState = ArcRuntimeState(
                    arcId = ongoing.arcId,
                    isPending = (ongoing.arcSessionCountInArc ?: 0) < 2,
                    multiplier = ongoing.arcChainBase ?: ArcRules.START_MULTIPLIER,
                    progressMs = 0L,
                    lastSessionEndTimeMs = ongoing.arcLastSessionEndTimeMs ?: 0L,
                    sessionCountInArc = ongoing.arcSessionCountInArc ?: 0
                )
                arcPrefs.save(arcState!!)
            } else {
                arcState = loadActiveArcOrRestoreRecentIfEligible(ongoing)
            }

            syncArcUi()

            ongoing?.let { entity ->
                val shouldOverrideDraft = hasLaunchOverrides && shouldTreatOngoingAsDraft(entity)

                if (shouldOverrideDraft) {
                    currentFlowInstanceId = UUID.randomUUID().toString()
                    ongoingCreatedAtMs = System.currentTimeMillis()
                    baseStartTimeMs = null
                    accumulatedBeforeStartMs = 0L

                    _uiState.update { old ->
                        applyLaunchOverrides(
                            old.copy(
                                title = "",
                                description = "",
                                tagName = "",
                                isInFlowMode = false,
                                isSoftMode = false,
                                isSurgeOn = false,
                                surgePlannedMs = null,
                                stopwatch = StopwatchState(
                                    isRunning = false,
                                    elapsedMs = 0L
                                ),
                                plannedArcTitle = null,
                                plannedArcStepIndex = null,
                                plannedArcTotalSteps = null,
                                originPulseId = null,
                                originPulseTitle = null,
                                originPulseJourneyName = null
                            )
                        )
                    }

                    clearOngoing()
                    saveOngoing()
                } else {
                    currentFlowInstanceId = entity.flowInstanceId
                    ongoingCreatedAtMs = entity.createdAt
                    baseStartTimeMs = entity.baseStartTimeMs
                    accumulatedBeforeStartMs = entity.accumulatedBeforeStartMs

                    val elapsed = if (entity.isRunning && baseStartTimeMs != null) {
                        accumulatedBeforeStartMs +
                                (System.currentTimeMillis() - baseStartTimeMs!!).coerceAtLeast(0L)
                    } else {
                        accumulatedBeforeStartMs
                    }

                    _uiState.update { old ->
                        old.copy(
                            title = entity.title,
                            description = entity.description,
                            tagName = entity.tagName,
                            isInFlowMode = entity.isInFlowMode,
                            isSoftMode = entity.isSoftMode,
                            isSurgeOn = entity.isSurgeOn,
                            surgePlannedMs = entity.surgePlannedMs,
                            originPulseId = entity.originPulseId,
                            originPulseTitle = entity.originPulseTitleSnapshot,
                            originPulseJourneyName = entity.originPulseJourneyNameSnapshot,
                            stopwatch = StopwatchState(
                                isRunning = entity.isRunning,
                                elapsedMs = elapsed
                            )
                        )
                    }

                    if (entity.isRunning) startTicker()
                }
            } ?: run {
                currentFlowInstanceId = UUID.randomUUID().toString()

                _uiState.update { old ->
                    val cleared = old.copy(
                        plannedArcTitle = null,
                        plannedArcStepIndex = null,
                        plannedArcTotalSteps = null,
                        originPulseId = null,
                        originPulseTitle = null,
                        originPulseJourneyName = null
                    )

                    if (hasLaunchOverrides) {
                        applyLaunchOverrides(cleared)
                    } else {
                        cleared
                    }
                }
                if (hasLaunchOverrides) saveOngoing()
            }
        }
    }

    fun clearLastReward() {
        _lastReward.value = null
    }

    fun abandonPendingArcContinuationForShellEntry() {
        _pendingArcIdeaContinuation.value = null
        _awaitingNextFlowAfterContinue.value = false
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

    fun discardDraftIfIdle() {
        val state = _uiState.value
        val isIdle = state.stopwatch.elapsedMs == 0L &&
                !state.stopwatch.isRunning &&
                !state.isInFlowMode
        val isAbandonedPulseOriginDraft = state.originPulseId != null && isIdle
        val hasNoArc = arcState == null

        if (isIdle && (hasNoArc || isAbandonedPulseOriginDraft)) {
            viewModelScope.launch {
                clearOngoing()
            }

            currentFlowInstanceId = UUID.randomUUID().toString()
            ongoingCreatedAtMs = System.currentTimeMillis()
            baseStartTimeMs = null
            accumulatedBeforeStartMs = 0L

            val keepArc = arcState
            val resetState = FlowUiState(
                showScoreUi = state.showScoreUi,
                calmMode = state.calmMode,
                plannedArcTitle = null,
                plannedArcStepIndex = null,
                plannedArcTotalSteps = null,
                isInArc = keepArc != null,
                arcIsPending = keepArc?.isPending ?: false,
                arcMultiplier = keepArc?.multiplier,
                arcProgressMs = keepArc?.progressMs ?: 0L,
                arcNextIndex = keepArc?.let { it.sessionCountInArc + 1 },
                originPulseId = null,
                originPulseTitle = null,
                originPulseJourneyName = null
            )

            _uiState.value = if (isAbandonedPulseOriginDraft) {
                resetState
            } else {
                applyLaunchOverrides(resetState)
            }
        }
    }

    fun startOrResumeStopwatch() {
        if (_uiState.value.stopwatch.isRunning) return

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
                        ?: 1.0,
                    shellSummary = shellRewardEventRecorder.summaryForArc(arcId)
                )
            } else {
                null
            }

            if (summary != null) {
                _lastReward.value = FlowRewardUiModel(
                    minutes = 0,
                    baseScyraPoints = 0,
                    tenMinuteBonuses = 0,
                    thirtyMinuteBonuses = 0,
                    sixtyMinuteBonuses = 0,
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
            nextIndex <= 3 -> ArcRules.PAUSE_BUDGET_EARLY_MS
            nextIndex >= 10 -> ArcRules.PAUSE_BUDGET_ULTRA_MS
            else -> ArcRules.PAUSE_BUDGET_LATE_MS
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

        arcState = arcState?.let { s ->
            if (s.pauseStartedAtMs == null) {
                s.copy(pauseStartedAtMs = now)
            } else {
                s
            }
        }

        arcState?.let { s ->
            viewModelScope.launch { arcPrefs.save(s) }
        }
        syncArcUi()

        _uiState.update {
            it.copy(
                stopwatch = it.stopwatch.copy(
                    isRunning = false,
                    elapsedMs = accumulatedBeforeStartMs
                )
            )
        }
        stopTicker()
        startArcCountdown()
        saveOngoing()
    }

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

                if (!running) {
                    syncArcUi()
                    val s = arcState ?: continue
                    val elapsed = accumulatedBeforeStartMs

                    val isBetweenFlows = elapsed == 0L
                    val isPausedMidFlow = elapsed > 0L

                    if (isBetweenFlows && isArcExpired(now, s)) {
                        concludeArc("expired")
                        continue
                    }

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
        arcState ?: return
        if (_uiState.value.stopwatch.isRunning) return
        startArcCountdown()
    }

    private fun applyArcPauseAccountingOnResume(now: Long) {
        val s = arcState ?: return
        val started = s.pauseStartedAtMs ?: return

        val pausedDelta = (now - started).coerceAtLeast(0L)
        val newUsed = s.pauseUsedMs + pausedDelta
        val budget = arcPauseBudgetMs(s)

        if (newUsed > budget) {
            concludeArc(reason = "pause_limit")
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
        _uiState.update {
            it.copy(
                stopwatch = StopwatchState(
                    isRunning = false,
                    elapsedMs = 0L
                )
            )
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
                    it.copy(
                        stopwatch = it.stopwatch.copy(elapsedMs = elapsed)
                    )
                }
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
                flowInstanceId = currentFlowInstanceId,
                title = state.title,
                description = state.description,
                tagName = state.tagName,
                isInFlowMode = state.isInFlowMode,
                isRunning = state.stopwatch.isRunning,
                isSoftMode = state.isSoftMode,
                baseStartTimeMs = baseStartTimeMs,
                accumulatedBeforeStartMs = accumulatedBeforeStartMs,
                isSurgeOn = state.isSurgeOn,
                surgePlannedMs = state.surgePlannedMs,
                createdAt = ongoingCreatedAtMs,
                arcId = arc?.arcId,
                arcChainBase = arc?.multiplier,
                arcSessionCountInArc = arc?.sessionCountInArc,
                arcLastSessionEndTimeMs = arc?.lastSessionEndTimeMs,
                originPulseId = state.originPulseId,
                originPulseTitleSnapshot = state.originPulseTitle,
                originPulseJourneyNameSnapshot = state.originPulseJourneyName
            )
            focusSessionRepository.saveOngoingSession(entity)
        }
    }

    private suspend fun clearOngoing() {
        focusSessionRepository.clearOngoingSession()
    }

    fun setSurgePlannedMinutes(minutes: Int) {
        if (_uiState.value.isSoftMode) return

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
        _uiState.update {
            it.copy(
                isSurgeOn = false,
                surgePlannedMs = null
            )
        }
        saveOngoing()
    }

    fun isSurgeLocked(): Boolean = _uiState.value.stopwatch.elapsedMs > 0L

    fun beginNextFlowAfterContinue() {
        if (_pendingArcIdeaContinuation.value != null) return
        beginNextFlowAfterContinueInternal(continuationOrigin = null)
    }

    fun continueArcOnlyAfterIdeaPrompt() {
        _pendingArcIdeaContinuation.value = null
        beginNextFlowAfterContinueInternal(continuationOrigin = null)
    }

    fun continueArcAndIdeaAfterIdeaPrompt() {
        val pending = _pendingArcIdeaContinuation.value ?: return
        _pendingArcIdeaContinuation.value = null
        beginNextFlowAfterContinueInternal(continuationOrigin = pending)
    }

    private fun beginNextFlowAfterContinueInternal(
        continuationOrigin: PendingArcIdeaContinuation?
    ) {
        if (!_awaitingNextFlowAfterContinue.value) return

        viewModelScope.launch {
            val hydrated = hydrateNextPlannedArcStepIfAny(continuationOrigin)
            if (hydrated) return@launch

            val keepTag = _uiState.value.tagName
            val keepArc = arcState

            _lastReward.value = null
            _awaitingNextFlowAfterContinue.value = false
            ongoingCreatedAtMs = System.currentTimeMillis()
            currentFlowInstanceId = UUID.randomUUID().toString()

            baseStartTimeMs = null
            accumulatedBeforeStartMs = 0L
            stopTicker()
            aliveFlowServiceController.stop()

            _uiState.update { old ->
                old.copy(
                    title = "",
                    description = "",
                    tagName = keepTag,
                    stopwatch = StopwatchState(isRunning = false, elapsedMs = 0L),
                    isInFlowMode = false,
                    isSoftMode = false,
                    isSurgeOn = false,
                    surgePlannedMs = null,
                    isInArc = keepArc != null,
                    arcIsPending = keepArc?.isPending ?: false,
                    arcMultiplier = keepArc?.multiplier,
                    arcProgressMs = keepArc?.progressMs ?: 0L,
                    arcNextIndex = keepArc?.let { it.sessionCountInArc + 1 },
                    arcGraceRemainingMs = null,
                    arcPauseRemainingMs = null,
                    plannedArcTitle = null,
                    plannedArcStepIndex = null,
                    plannedArcTotalSteps = null,
                    originPulseId = continuationOrigin?.pulseId,
                    originPulseTitle = continuationOrigin?.pulseTitle,
                    originPulseJourneyName = continuationOrigin?.pulseJourneyName
                )
            }

            clearOngoing()
        }
    }

    private fun queueArcIdeaContinuationPromptIfNeeded(state: FlowUiState) {
        _pendingArcIdeaContinuation.value = state.originPulseId?.let { pulseId ->
            PendingArcIdeaContinuation(
                pulseId = pulseId,
                pulseTitle = state.originPulseTitle,
                pulseJourneyName = state.originPulseJourneyName
            )
        }
    }

    fun onEndFlowClicked(action: FlowEndAction) {
        viewModelScope.launch {
            val effectiveAction = if (
                action == FlowEndAction.CONTINUE_ARC &&
                isLastPlannedArcStep()
            ) {
                FlowEndAction.COMPLETE_ARC
            } else {
                action
            }

            saveWithArcBehavior(endMode = effectiveAction)
        }
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
        if (endMode != FlowEndAction.CONTINUE_ARC) {
            _pendingArcIdeaContinuation.value = null
        }

        try {
            val sessionEnd = System.currentTimeMillis()
            val sessionStart = (sessionEnd - realDurationMs).coerceAtLeast(0L)

            val tagId = tagRepository.getOrCreateTagId(tagName)
            val isSoft = state.isSoftMode

            val surgePoints = if (isSoft) {
                0
            } else {
                ScoreCalculator.surgePoints(
                    surgePlannedMs = state.surgePlannedMs,
                    actualDurationMs = realDurationMs
                )
            }

            val planned = if (isSoft) null else state.surgePlannedMs

            val surgeSucceeded =
                !isSoft &&
                        state.isSurgeOn &&
                        planned != null &&
                        realDurationMs <= planned

            val breakdown = ScoreCalculator.breakdownFromDuration(realDurationMs)
            val baseScyra = if (isSoft) 0 else breakdown.totalPoints
            val beforeArc = baseScyra

            var localArc = arcState
            if (localArc != null && isArcExpired(sessionStart, localArc)) {
                clearArcPersistedAsync()
                localArc = null
                arcState = null
                syncArcUi()
            }

            if (state.isSoftMode && localArc != null) {
                clearArcPersistedAsync()
                localArc = null
                arcState = null
                syncArcUi()
            }

            val arcIdForSummary: Long? = localArc?.arcId
            val isInExistingArc = localArc != null

            if (!state.isSoftMode && !isInExistingArc && endMode == FlowEndAction.CONTINUE_ARC) {
                val firstSessionId = sessionRepository.addSession(
                    title = title,
                    description = description,
                    tagId = tagId,
                    startTime = sessionStart,
                    endTime = sessionEnd,
                    durationMs = realDurationMs,
                    surgePlannedMs = state.surgePlannedMs,
                    surgePoints = surgePoints,
                    scyraPoints = beforeArc,
                    isSoftMode = state.isSoftMode
                )

                val arcId = System.currentTimeMillis()

                arcPrefs.clearRecentlyEnded()

                sessionRepository.updateArcFields(
                    sessionId = firstSessionId,
                    arcId = arcId,
                    arcIndex = 1,
                    arcMultiplierUsed = 1.0,
                    arcBonusPoints = 0,
                    finalScyraPoints = beforeArc
                )

                pulseRepository.attachLivePulsesToSession(
                    flowInstanceId = currentFlowInstanceId,
                    sessionId = firstSessionId,
                    arcId = arcId
                )
                // TODO(Idea Grove): move Session insert, Arc field updates, and optional
                // PulseFlowLink creation into one transaction-safe Flow completion use case.
                // linkCompletedFlowToPulse is best-effort and must never fail the saved Flow.
                state.originPulseId?.let { pulseId ->
                    ideaGroveRepository.linkCompletedFlowToPulse(pulseId, firstSessionId)
                }

                val shellReward = runCatching { shellRewardOrchestrator.onSessionCompleted(
                    SessionEntity(
                        id = firstSessionId,
                        title = title,
                        description = description,
                        tagId = tagId,
                        startTime = sessionStart,
                        endTime = sessionEnd,
                        durationMs = realDurationMs,
                        surgePlannedMs = state.surgePlannedMs,
                        surgePoints = surgePoints,
                        scyraPoints = beforeArc,
                        isSoftMode = state.isSoftMode,
                        arcId = arcId,
                        arcIndex = 1
                    )
                )
                }.getOrDefault(ShellRewardResult())

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

                _lastReward.value = FlowRewardUiModel(
                    minutes = breakdown.minutes,
                    baseScyraPoints = baseScyra,
                    tenMinuteBonuses = breakdown.tenMinuteBonuses,
                    thirtyMinuteBonuses = breakdown.thirtyMinuteBonuses,
                    sixtyMinuteBonuses = breakdown.sixtyMinuteBonuses,
                    finalScyraPoints = beforeArc,
                    surgePoints = surgePoints,
                    arcIndexInArc = 1,
                    arcMultiplierUsed = null,
                    arcBonusPoints = 0,
                    arcNextMultiplier = arcState?.multiplier,
                    arcProgressTowardNextMs = arcState?.progressMs ?: 0L,
                    arcDidLevelUp = false
                ).withShellReward(shellReward)

                _awaitingNextFlowAfterContinue.value = true
                queueArcIdeaContinuationPromptIfNeeded(state)

                baseStartTimeMs = null
                accumulatedBeforeStartMs = 0L
                stopTicker()
                aliveFlowServiceController.stop()
                clearOngoing()

                return
            }

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
                scyraPoints = finalScyra,
                isSoftMode = state.isSoftMode
            )

            pulseRepository.attachLivePulsesToSession(
                flowInstanceId = currentFlowInstanceId,
                sessionId = insertedId,
                arcId = localArc?.arcId
            )
            // TODO(Idea Grove): move Session insert and optional PulseFlowLink creation into
            // one transaction-safe Flow completion use case. linkCompletedFlowToPulse is
            // best-effort and must never fail the saved Flow.
            state.originPulseId?.let { pulseId ->
                ideaGroveRepository.linkCompletedFlowToPulse(pulseId, insertedId)
            }

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

            val shellReward = runCatching { shellRewardOrchestrator.onSessionCompleted(
                SessionEntity(
                    id = insertedId,
                    title = title,
                    description = description,
                    tagId = tagId,
                    startTime = sessionStart,
                    endTime = sessionEnd,
                    durationMs = realDurationMs,
                    surgePlannedMs = state.surgePlannedMs,
                    surgePoints = surgePoints,
                    scyraPoints = finalScyra,
                    isSoftMode = state.isSoftMode,
                    arcId = localArc?.arcId,
                    arcIndex = arcIndex,
                    arcMultiplierUsed = arcMultiplierUsed,
                    arcBonusPoints = arcBonusPoints
                )
            )
            }.getOrDefault(ShellRewardResult())

            val baseReward = FlowRewardUiModel(
                minutes = breakdown.minutes,
                baseScyraPoints = baseScyra,
                tenMinuteBonuses = breakdown.tenMinuteBonuses,
                thirtyMinuteBonuses = breakdown.thirtyMinuteBonuses,
                sixtyMinuteBonuses = breakdown.sixtyMinuteBonuses,
                finalScyraPoints = finalScyra,
                surgePoints = surgePoints,
                arcIndexInArc = arcIndex,
                arcMultiplierUsed = arcMultiplierUsed,
                arcBonusPoints = arcBonusPoints,
                arcNextMultiplier = arcState?.multiplier,
                arcProgressTowardNextMs = arcState?.progressMs ?: 0L,
                arcDidLevelUp = arcDidLevelUp
            ).withShellReward(shellReward)

            when (if (state.isSoftMode) FlowEndAction.SAVE_FLOW else endMode) {
                FlowEndAction.COMPLETE_ARC -> {
                    if (!state.isSoftMode && state.isSurgeOn && state.surgePlannedMs != null) {
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
                                    .maxOrNull() ?: 1.0,
                                shellSummary = shellRewardEventRecorder.summaryForArc(arcId)
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                    _lastReward.value = baseReward.copy(arcSummary = summary)
                    _exitAfterReward.value = true

                    saveRecentlyEndedArcSnapshot(
                        state = arcState,
                        endedAtMs = sessionEnd
                    )

                    clearArcPersistedAsync()
                    arcState = null

                    baseStartTimeMs = null
                    accumulatedBeforeStartMs = 0L
                    _uiState.update {
                        it.copy(
                            stopwatch = StopwatchState(
                                isRunning = false,
                                elapsedMs = 0L
                            ),
                            plannedArcTitle = null,
                            plannedArcStepIndex = null,
                            plannedArcTotalSteps = null
                        )
                    }

                    stopTicker()
                    syncArcUi()

                    aliveFlowServiceController.stop()
                    clearOngoing()
                    activeArcRunRepository.clear()
                    ongoingCreatedAtMs = System.currentTimeMillis()
                    currentFlowInstanceId = UUID.randomUUID().toString()
                }

                FlowEndAction.SAVE_FLOW -> {
                    if (!state.isSoftMode && state.isSurgeOn && state.surgePlannedMs != null) {
                        if (surgeSucceeded) {
                            surgeHapticsManager.playCompletedSuccess()
                        } else {
                            surgeHapticsManager.playCompletedFail()
                        }
                    }

                    _lastReward.value = baseReward
                    _exitAfterReward.value = true

                    if (arcState != null) {
                        saveRecentlyEndedArcSnapshot(
                            state = arcState,
                            endedAtMs = sessionEnd
                        )

                        clearArcPersistedAsync()
                        arcState = null
                    }

                    baseStartTimeMs = null
                    accumulatedBeforeStartMs = 0L
                    _uiState.update {
                        it.copy(
                            stopwatch = StopwatchState(
                                isRunning = false,
                                elapsedMs = 0L
                            ),
                            plannedArcTitle = null,
                            plannedArcStepIndex = null,
                            plannedArcTotalSteps = null
                        )
                    }

                    stopTicker()
                    syncArcUi()

                    aliveFlowServiceController.stop()
                    clearOngoing()
                    activeArcRunRepository.clear()
                    ongoingCreatedAtMs = System.currentTimeMillis()
                    currentFlowInstanceId = UUID.randomUUID().toString()
                }

                FlowEndAction.CONTINUE_ARC -> {
                    if (!state.isSoftMode && state.isSurgeOn && state.surgePlannedMs != null) {
                        if (surgeSucceeded) {
                            surgeHapticsManager.playCompletedSuccess()
                        } else {
                            surgeHapticsManager.playCompletedFail()
                        }
                    }

                    _lastReward.value = baseReward
                    _awaitingNextFlowAfterContinue.value = true
                    queueArcIdeaContinuationPromptIfNeeded(state)

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
                    syncArcUi()

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

    private suspend fun hydrateNextPlannedArcStepIfAny(
        continuationOrigin: PendingArcIdeaContinuation?
    ): Boolean {
        val activeRun = activeArcRunRepository.getActiveArcRunOnce() ?: return false
        val nextIndex = activeRun.currentStepIndex + 1

        if (nextIndex >= activeRun.totalSteps) {
            return false
        }

        val nextStep = arcPlanRepository
            .getStepsForArcPlanOnce(activeRun.arcPlanId)
            .sortedBy { it.orderIndex }
            .getOrNull(nextIndex)
            ?: return false

        val tagNameById = tagRepository.getAllTags()
            .firstOrNull()
            .orEmpty()
            .associate { it.id to it.name }
        val nextTagName = nextStep.tagIdSnapshot
            ?.let { tagId -> tagNameById[tagId] }
            .orEmpty()

        activeArcRunRepository.updateCurrentStep(
            currentStepIndex = nextIndex,
            currentStepTitle = nextStep.titleSnapshot,
            currentTagName = nextTagName,
            currentIsSoftMode = nextStep.isSoftModeSnapshot
        )

        ongoingCreatedAtMs = System.currentTimeMillis()
        currentFlowInstanceId = UUID.randomUUID().toString()

        baseStartTimeMs = null
        accumulatedBeforeStartMs = 0L
        stopTicker()
        aliveFlowServiceController.stop()

        _lastReward.value = null
        _awaitingNextFlowAfterContinue.value = false

        _uiState.update { old ->
            old.copy(
                title = nextStep.titleSnapshot,
                description = "",
                tagName = nextTagName,
                stopwatch = StopwatchState(isRunning = false, elapsedMs = 0L),
                isInFlowMode = false,
                isSoftMode = nextStep.isSoftModeSnapshot,
                isSurgeOn = !nextStep.isSoftModeSnapshot && nextStep.launchWithSurgeSnapshot,
                surgePlannedMs = if (
                    !nextStep.isSoftModeSnapshot &&
                    nextStep.launchWithSurgeSnapshot &&
                    nextStep.targetMinutesSnapshot != null
                ) {
                    nextStep.targetMinutesSnapshot * 60_000L
                } else {
                    null
                },
                plannedArcTitle = activeRun.arcTitle,
                plannedArcStepIndex = nextIndex,
                plannedArcTotalSteps = activeRun.totalSteps,
                isInArc = arcState != null,
                arcIsPending = arcState?.isPending ?: false,
                arcMultiplier = arcState?.multiplier,
                arcProgressMs = arcState?.progressMs ?: 0L,
                arcNextIndex = arcState?.let { it.sessionCountInArc + 1 },
                arcGraceRemainingMs = null,
                arcPauseRemainingMs = null,
                originPulseId = continuationOrigin?.pulseId,
                originPulseTitle = continuationOrigin?.pulseTitle,
                originPulseJourneyName = continuationOrigin?.pulseJourneyName
            )
        }

        clearOngoing()
        return true
    }

    private suspend fun isLastPlannedArcStep(): Boolean {
        val activeRun = activeArcRunRepository.getActiveArcRunOnce() ?: return false
        return activeRun.currentStepIndex >= activeRun.totalSteps - 1
    }

    private fun isPlannedArcLaunch(): Boolean {
        return !plannedArcTitleOverride.isNullOrBlank() ||
                plannedArcStepIndexOverride != null ||
                plannedArcTotalStepsOverride != null
    }

    private fun canRestoreRecentlyEndedArcForFreshFlow(
        ongoing: OngoingSessionEntity?
    ): Boolean {
        if (ongoing != null) return false
        if (prefillSoftModeOverride) return false
        if (isPlannedArcLaunch()) return false
        return true
    }

    private suspend fun loadActiveArcOrRestoreRecentIfEligible(
        ongoing: OngoingSessionEntity?
    ): ArcRuntimeState? {
        val now = System.currentTimeMillis()

        val active = arcPrefs.load()
        if (active != null) {
            if (ongoing == null && isArcExpired(now, active)) {
                arcPrefs.clear()
                return null
            }

            return active
        }

        if (!canRestoreRecentlyEndedArcForFreshFlow(ongoing)) {
            return null
        }

        val recent = arcPrefs.loadRecentlyEnded() ?: return null

        if (isArcExpired(now, recent)) {
            arcPrefs.clearRecentlyEnded()
            return null
        }

        arcPrefs.save(recent)
        arcPrefs.clearRecentlyEnded()

        _uiState.update {
            it.copy(
                recentlyResumedArcMessage = "Arc resumed. Momentum preserved."
            )
        }

        return recent
    }

    private suspend fun saveRecentlyEndedArcSnapshot(
        state: ArcRuntimeState?,
        endedAtMs: Long
    ) {
        if (state == null) return

        arcPrefs.saveRecentlyEnded(
            state = state.copy(
                lastSessionEndTimeMs = endedAtMs,
                progressMs = 0L
            ),
            completedAtMs = endedAtMs
        )
    }

    fun consumeRecentlyResumedArcMessage() {
        _uiState.update {
            it.copy(recentlyResumedArcMessage = null)
        }
    }
}