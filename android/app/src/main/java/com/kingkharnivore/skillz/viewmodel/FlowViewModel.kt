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
import java.time.Instant
import com.kingkharnivore.skillz.utils.health.MovementBonusEligibilityPolicy
import com.kingkharnivore.skillz.utils.health.MovementBonusEligibilityInput
import com.kingkharnivore.skillz.utils.health.MovementBonusCalculator
import com.kingkharnivore.skillz.utils.health.FlowActiveIntervalNormalizer
import com.kingkharnivore.skillz.utils.health.FlowActiveIntervalCodec
import com.kingkharnivore.skillz.utils.health.FlowActiveInterval
import com.kingkharnivore.skillz.data.model.entity.health.FlowRewardBreakdownEntity
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSyncStatus
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSnapshotEntity
import com.kingkharnivore.skillz.data.health.MovementReadResult
import com.kingkharnivore.skillz.data.health.HealthConnectMovementDataSource
import com.kingkharnivore.skillz.data.repository.health.FlowHealthRepository
import com.kingkharnivore.skillz.data.repository.health.HealthSettingsRepository
import com.kingkharnivore.skillz.data.repository.health.HealthPermissionRepository
import com.kingkharnivore.skillz.data.repository.shell.IdeaGroveRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.data.repository.PulseRepository
import com.kingkharnivore.skillz.data.repository.ChronicleRepository
import com.kingkharnivore.skillz.data.model.entity.ChronicleOwnerType
import com.kingkharnivore.skillz.ui.screen.chronicle.ChronicleStateHolder
import com.kingkharnivore.skillz.utils.shell.ShellRewardEventRecorder
import com.kingkharnivore.skillz.utils.shell.ShellRewardOrchestrator
import com.kingkharnivore.skillz.utils.shell.ShellRewardResult
import com.kingkharnivore.skillz.model.state.flow.ArcSummaryUiModel
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel
import com.kingkharnivore.skillz.model.state.flow.FlowUiState
import com.kingkharnivore.skillz.model.state.flow.StopwatchState
import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import com.kingkharnivore.skillz.ui.navigation.SkillzDestinations
import com.kingkharnivore.skillz.ui.service.AliveFlowServiceController
import com.kingkharnivore.skillz.ui.service.SurgeHapticsManager
import com.kingkharnivore.skillz.utils.arc.ArcPrefs
import com.kingkharnivore.skillz.utils.arc.ArcContinuationLifecycle
import com.kingkharnivore.skillz.utils.arc.ArcContinuationResolver
import com.kingkharnivore.skillz.utils.arc.ArcFlowStartCoordinator
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
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

internal enum class PlannedArcAdvanceResult { Advanced, Completed, NotPlannedArc }

internal fun plannedArcAdvanceResult(currentStepIndex: Int, totalSteps: Int): PlannedArcAdvanceResult =
    if (currentStepIndex + 1 >= totalSteps) {
        PlannedArcAdvanceResult.Completed
    } else {
        PlannedArcAdvanceResult.Advanced
    }

internal fun resolveFlowEndMode(
    requested: FlowEndAction,
    isSoftMode: Boolean,
    isInArc: Boolean
): FlowEndAction {
    return if (isSoftMode && !isInArc) FlowEndAction.SAVE_FLOW else requested
}

internal fun usesBaseArcReward(isSoftMode: Boolean, completedArcSessionCount: Int): Boolean =
    isSoftMode || completedArcSessionCount == 0

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
    private val healthSettingsRepository: HealthSettingsRepository,
    private val healthPermissionRepository: HealthPermissionRepository,
    private val healthMovementDataSource: HealthConnectMovementDataSource,
    private val flowHealthRepository: FlowHealthRepository,
    private val movementBonusCalculator: MovementBonusCalculator,
    private val movementBonusEligibilityPolicy: MovementBonusEligibilityPolicy,
    private val chronicleRepository: ChronicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val flowStartMutex = Mutex()
    private val arcContinuationLifecycle = ArcContinuationLifecycle(arcPrefs)
    private val arcFlowStartCoordinator = ArcFlowStartCoordinator(arcContinuationLifecycle)

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
        val lastUsedByTagId: Map<Long, Long> = sessions
            .groupBy { it.tagId }
            .mapValues { (_, tagSessions) ->
                tagSessions.maxOf { session ->
                    maxOf(session.endTime, session.startTime, session.createdAt)
                }
            }

        tags
            .filter { tag -> tag.id in lastUsedByTagId }
            .sortedWith(
                compareByDescending<TagEntity> { tag ->
                    lastUsedByTagId[tag.id] ?: Long.MIN_VALUE
                }
                    .thenByDescending { tag -> tag.createdAt }
                    .thenBy { tag -> tag.name.lowercase(Locale.ROOT) }
            )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private var baseStartTimeMs: Long? = null
    private var accumulatedBeforeStartMs: Long = 0L
    private val activeIntervals = mutableListOf<FlowActiveInterval>()
    private var activeIntervalStartMs: Long? = null
    private var tickerJob: Job? = null
    private var ongoingCreatedAtMs: Long = System.currentTimeMillis()
    private val _chronicleOwnerKey = MutableStateFlow(UUID.randomUUID().toString())
    val chronicleOwnerKey: StateFlow<String> = _chronicleOwnerKey.asStateFlow()
    private var currentFlowInstanceId: String = _chronicleOwnerKey.value
        set(value) { field = value; _chronicleOwnerKey.value = value }

    fun createChronicleStateHolder(ownerKey: String) = ChronicleStateHolder(
        ChronicleOwnerType.ACTIVE_FLOW, ownerKey, chronicleRepository, viewModelScope
    )

    private var arcState: ArcRuntimeState? = null

    private val _exitAfterReward = MutableStateFlow(false)
    val exitAfterReward: StateFlow<Boolean> = _exitAfterReward.asStateFlow()
    private var isConsumingExitAfterReward = false
    private var isPreparingShellContinuation = false
    private var hasPreparedShellContinuation = false

    private var arcCountdownJob: Job? = null

    fun consumeExitAfterReward(onConsumed: () -> Unit) {
        if (!_exitAfterReward.value || isConsumingExitAfterReward) return
        isConsumingExitAfterReward = true

        viewModelScope.launch {
            try {
                // Navigation may destroy this ViewModel, so the durable terminal marker
                // must be removed before invoking the navigation callback.
                arcPrefs.clearPlannedFlowHandoff()
                _exitAfterReward.value = false
                onConsumed()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to finish Flow"
            } finally {
                isConsumingExitAfterReward = false
            }
        }
    }

    fun isModeLocked(): Boolean = _uiState.value.stopwatch.elapsedMs > 0L

    fun setSoftMode(enabled: Boolean) {
        if (isModeLocked()) return

        if (enabled) {
            viewModelScope.launch { enterSoftModePreservingArc() }
            return
        }

        _uiState.update { current ->
            current.copy(
                isSoftMode = false
            )
        }
        saveOngoing()
    }

    fun setSoftModeAndResetArcMultiplier() {
        if (isModeLocked()) return

        viewModelScope.launch { enterSoftModePreservingArc() }
    }

    /** The single entry point for manual, restored, prefilled, and planned Soft Flows. */
    private suspend fun enterSoftModePreservingArc(
        persistSnapshotIfAlreadyApplied: Boolean = false
    ) {
        val alreadyApplied = _uiState.value.isSoftMode &&
                (arcState == null || arcState?.multiplier == ArcRuntimeState.BASE_MULTIPLIER)
        if (alreadyApplied) {
            if (persistSnapshotIfAlreadyApplied) saveOngoingNow()
            return
        }

        _uiState.update { current ->
            current.copy(isSoftMode = true, isSurgeOn = false, surgePlannedMs = null)
        }
        syncArcUi()

        saveOngoingNow()
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
        if (state.sessionCountInArc == 0) return false
        return !ArcContinuationResolver.isWithinContinuationWindow(state, nowMs)
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
        if (s.sessionCountInArc == 0) return ArcRules.GRACE_WINDOW_MS
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
            val activePlannedRun = activeArcRunRepository.getActiveArcRunOnce()
            val plannedHandoff = arcPrefs.loadPlannedFlowHandoff()

            if (activePlannedRun != null && isPlannedArcLaunch()) {
                arcState = arcPrefs.load()
            } else if (ongoing?.arcId != null) {
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
                arcState = loadActiveArc(ongoing)
            }

            if (arcState == null && activePlannedRun != null) {
                arcState = ArcRuntimeState(
                    arcId = System.currentTimeMillis(),
                    isPending = true,
                    multiplier = ArcRuntimeState.BASE_MULTIPLIER,
                    progressMs = 0L,
                    lastSessionEndTimeMs = System.currentTimeMillis(),
                    sessionCountInArc = 0
                )
                arcPrefs.save(arcState!!)
            }

            syncArcUi()

            if (plannedHandoff == ArcPrefs.PlannedFlowHandoff.BLANK_ARC_CONTINUATION) {
                activeArcRunRepository.clear()
                clearOngoing()
                ongoingCreatedAtMs = System.currentTimeMillis()
                currentFlowInstanceId = UUID.randomUUID().toString()
                _uiState.value = FlowUiState(
                    showScoreUi = _uiState.value.showScoreUi,
                    calmMode = _uiState.value.calmMode
                )
                syncArcUi()
                saveOngoingNow()
                arcPrefs.clearPlannedFlowHandoff()
                return@launch
            }

            if (plannedHandoff == ArcPrefs.PlannedFlowHandoff.COMPLETED_ARC_EXIT) {
                activeArcRunRepository.clear()
                clearOngoing()
                arcPrefs.clear()
                arcState = null
                _uiState.value = FlowUiState(
                    showScoreUi = _uiState.value.showScoreUi,
                    calmMode = _uiState.value.calmMode
                )
                syncArcUi()
                _exitAfterReward.value = true
                return@launch
            }

            ongoing?.let { entity ->
                val shouldOverrideDraft = hasLaunchOverrides && shouldTreatOngoingAsDraft(entity)

                if (shouldOverrideDraft) {
                    currentFlowInstanceId = UUID.randomUUID().toString()
                    ongoingCreatedAtMs = System.currentTimeMillis()
                    baseStartTimeMs = null
                    accumulatedBeforeStartMs = 0L
                    activeIntervals.clear()
                    activeIntervalStartMs = null

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
                    activeIntervals.clear()
                    activeIntervals += FlowActiveIntervalCodec.decode(entity.activeIntervalJson)
                    activeIntervalStartMs = if (entity.isRunning) entity.baseStartTimeMs else null

                    val elapsed = if (entity.isRunning && baseStartTimeMs != null) {
                        accumulatedBeforeStartMs +
                                (System.currentTimeMillis() - baseStartTimeMs!!).coerceAtLeast(0L)
                    } else {
                        accumulatedBeforeStartMs
                    }

                    _uiState.update { old ->
                        old.copy(
                            title = entity.title,
                            description = "",
                            tagName = entity.tagName,
                            isInFlowMode = entity.isInFlowMode,
                            isSoftMode = entity.isSoftMode,
                            isSurgeOn = entity.isSurgeOn,
                            surgePlannedMs = entity.surgePlannedMs,
                            originPulseId = entity.originPulseId,
                            originPulseTitle = entity.originPulseTitleSnapshot,
                            originPulseJourneyName = entity.originPulseJourneyNameSnapshot,
                            healthEnabledAtStart = entity.healthEnabledAtStart,
                            healthPermissionGrantedAtStart = entity.healthPermissionGrantedAtStart,
                            movementBonusEligibleAtStart = entity.movementBonusEligibleAtStart,
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
                activeIntervals.clear()
                activeIntervalStartMs = null

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
                if (hasLaunchOverrides && !_uiState.value.isSoftMode) saveOngoing()
            }

            if (
                activePlannedRun != null &&
                plannedArcStepIndexOverride != null &&
                activePlannedRun.currentStepIndex > plannedArcStepIndexOverride
            ) {
                advancePlannedArcAfterCompletedSession(continuationOrigin = null)
                arcPrefs.clearPlannedFlowHandoff()
            }

            if (_uiState.value.isSoftMode) enterSoftModePreservingArc(persistSnapshotIfAlreadyApplied = true)
        }
    }

    fun clearLastReward() {
        _lastReward.value = null
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
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
            activeIntervals.clear()
            activeIntervalStartMs = null

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

    private suspend fun captureMovementEligibilityAtFlowStart(): FlowUiState {
        val settings = healthSettingsRepository.settings.first()
        val available = healthPermissionRepository.isHealthConnectAvailable()
        val granted = if (available) healthPermissionRepository.isReadStepsGranted() else false
        val current = _uiState.value
        val eligible = movementBonusEligibilityPolicy.isEligible(
            MovementBonusEligibilityInput(
                movementBonusEnabled = settings.movementBonusEnabled,
                healthConnectAvailable = available,
                readStepsPermissionGranted = granted,
                isRegularPointEligibleFlow = !current.isSoftMode,
                isSoftFlow = current.isSoftMode
            )
        )
        val updated = current.copy(
            healthEnabledAtStart = settings.movementBonusEnabled,
            healthPermissionGrantedAtStart = granted,
            movementBonusEligibleAtStart = eligible
        )
        _uiState.value = updated
        return updated
    }

    private fun currentActiveIntervals(nowMs: Long): List<FlowActiveInterval> {
        val openInterval = activeIntervalStartMs
            ?.takeIf { _uiState.value.stopwatch.isRunning }
            ?.let { FlowActiveInterval(it, nowMs) }
        return FlowActiveIntervalNormalizer.normalize(activeIntervals + listOfNotNull(openInterval))
    }

    fun startOrResumeStopwatch() {
        viewModelScope.launch { startOrResumeStopwatchInternal() }
    }

    private suspend fun startOrResumeStopwatchInternal() {
        flowStartMutex.withLock {
            if (_uiState.value.stopwatch.isRunning) return@withLock
            val isResumingSameFlow = accumulatedBeforeStartMs > 0L || activeIntervals.isNotEmpty()
            var flowStartTimeMs: Long? = null

            if (!isResumingSameFlow) {
                // Start has already happened. Stop the between-Flow countdown before
                // any suspending persistence or health work can cross the boundary.
                arcCountdownJob?.cancel()
                arcCountdownJob = null
                val start = arcFlowStartCoordinator.start(_uiState.value.isSoftMode) { resolvedArc ->
                    val resumedRecentlyEndedArc = arcState == null && resolvedArc != null
                    arcState = resolvedArc
                    if (resumedRecentlyEndedArc) {
                        _uiState.update {
                            it.copy(recentlyResumedArcMessage = "Arc resumed. Momentum preserved.")
                        }
                    }
                    _uiState.update {
                        it.copy(
                            healthEnabledAtStart = false,
                            healthPermissionGrantedAtStart = false,
                            movementBonusEligibleAtStart = false
                        )
                    }
                    activeIntervals.clear()
                    activeIntervalStartMs = null
                    captureMovementEligibilityAtFlowStart()
                }
                flowStartTimeMs = start.startedAtMs
            }

            val now = flowStartTimeMs ?: System.currentTimeMillis()
            baseStartTimeMs = now
            activeIntervalStartMs = now
            _uiState.update { it.copy(stopwatch = it.stopwatch.copy(isRunning = true)) }
            syncArcUi()
            applyArcPauseAccountingOnResume(now)
            arcCountdownJob?.cancel()
            arcCountdownJob = null

            startTicker()
            saveOngoing()

            if (!isResumingSameFlow && _uiState.value.isSurgeOn) {
                surgeHapticsManager.playStarted()
            }
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

    private suspend fun clearArcPersisted() = arcPrefs.clear()

    fun pauseStopwatch() {
        if (!_uiState.value.stopwatch.isRunning) return
        val now = System.currentTimeMillis()

        baseStartTimeMs?.let { base ->
            accumulatedBeforeStartMs += (now - base).coerceAtLeast(0L)
        }
        activeIntervalStartMs?.let { start ->
            if (now > start) activeIntervals += FlowActiveInterval(start, now)
        }
        activeIntervalStartMs = null
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
        activeIntervals.clear()
        activeIntervalStartMs = null
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
        viewModelScope.launch {
            if (!_uiState.value.stopwatch.isRunning) startOrResumeStopwatchInternal()
            _uiState.update { it.copy(isInFlowMode = true) }
            saveOngoing()
            aliveFlowServiceController.start()
        }
    }

    fun exitFocusMode() {
        if (uiState.value.stopwatch.isRunning) pauseStopwatch()
        _uiState.update { it.copy(isInFlowMode = false) }
        saveOngoing()
        aliveFlowServiceController.stop()
    }

    private fun saveOngoing() {
        viewModelScope.launch { saveOngoingNow() }
    }

    private suspend fun saveOngoingNow() {
        val state = _uiState.value
        val arc = arcState

        val entity = OngoingSessionEntity(
            id = 1,
            flowInstanceId = currentFlowInstanceId,
            title = state.title,
            description = "",
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
            originPulseJourneyNameSnapshot = state.originPulseJourneyName,
            healthEnabledAtStart = state.healthEnabledAtStart,
            healthPermissionGrantedAtStart = state.healthPermissionGrantedAtStart,
            movementBonusEligibleAtStart = state.movementBonusEligibleAtStart,
            activeIntervalJson = FlowActiveIntervalCodec.encode(
                currentActiveIntervals(System.currentTimeMillis())
            )
        )
        focusSessionRepository.saveOngoingSession(entity)
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

    fun prepareContinuationAndEnterShell(onPrepared: () -> Unit) {
        _pendingArcIdeaContinuation.value = null
        beginNextFlowAfterContinueInternal(
            continuationOrigin = null,
            isShellEntry = true,
            onPrepared = onPrepared
        )
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
        continuationOrigin: PendingArcIdeaContinuation?,
        isShellEntry: Boolean = false,
        onPrepared: () -> Unit = {}
    ) {
        if (!_awaitingNextFlowAfterContinue.value || isPreparingShellContinuation) return
        isPreparingShellContinuation = true

        viewModelScope.launch {
            try {
                if (!hasPreparedShellContinuation) {
                    val advanceResult = advancePlannedArcAfterCompletedSession(continuationOrigin)
                    if (advanceResult != PlannedArcAdvanceResult.Advanced) {
                        val keepTag = _uiState.value.tagName
                        val keepArc = arcState

                        ongoingCreatedAtMs = System.currentTimeMillis()
                        currentFlowInstanceId = UUID.randomUUID().toString()

                        baseStartTimeMs = null
                        accumulatedBeforeStartMs = 0L
                        activeIntervals.clear()
                        activeIntervalStartMs = null
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

                        saveOngoingNow()
                    }
                    hasPreparedShellContinuation = isShellEntry
                }
                arcPrefs.clearPlannedFlowHandoff()
                hasPreparedShellContinuation = false
                _lastReward.value = null
                _awaitingNextFlowAfterContinue.value = false
                onPrepared()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to prepare Arc continuation"
            } finally {
                isPreparingShellContinuation = false
            }
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
        if (_isSaving.value) return
        viewModelScope.launch {
            saveWithArcBehavior(endMode = action)
        }
    }

    private data class CompletionMovementRead(
        val steps: Long?,
        val movementPoints: Long,
        val status: FlowHealthSyncStatus,
        val checkedAtMs: Long?
    )

    private suspend fun readMovementForCompletionIfEligible(
        state: FlowUiState,
        activeIntervals: List<FlowActiveInterval>
    ): CompletionMovementRead {
        if (!state.movementBonusEligibleAtStart || state.isSoftMode || activeIntervals.isEmpty()) {
            return CompletionMovementRead(null, 0L, FlowHealthSyncStatus.NOT_ELIGIBLE, null)
        }
        val checkedAt = System.currentTimeMillis()
        var totalSteps = 0L
        var sawSuccess = false
        var sawNoData = false
        activeIntervals.forEach { interval ->
            when (val result = healthMovementDataSource.readStepsBetween(
                Instant.ofEpochMilli(interval.startTimeMs),
                Instant.ofEpochMilli(interval.endTimeMs)
            )) {
                MovementReadResult.HealthConnectUnavailable -> return CompletionMovementRead(null, 0L, FlowHealthSyncStatus.ERROR_RETRYABLE, checkedAt)
                MovementReadResult.PermissionMissing -> return CompletionMovementRead(null, 0L, FlowHealthSyncStatus.PERMISSION_REVOKED, checkedAt)
                MovementReadResult.NoData -> sawNoData = true
                is MovementReadResult.Error -> return CompletionMovementRead(null, 0L, FlowHealthSyncStatus.ERROR_RETRYABLE, checkedAt)
                is MovementReadResult.Success -> {
                    sawSuccess = true
                    totalSteps += result.steps
                }
            }
        }
        if (!sawSuccess) {
            return CompletionMovementRead(null, 0L, if (sawNoData) FlowHealthSyncStatus.PENDING else FlowHealthSyncStatus.NO_REWARD, checkedAt)
        }
        val points = movementBonusCalculator.calculateMovementPoints(totalSteps)
        return CompletionMovementRead(
            steps = totalSteps,
            movementPoints = points,
            status = if (points > 0L) FlowHealthSyncStatus.CAPTURED else FlowHealthSyncStatus.NO_REWARD,
            checkedAtMs = checkedAt
        )
    }

    private suspend fun saveMovementSnapshotAndBreakdown(
        sessionId: Long,
        state: FlowUiState,
        movementRead: CompletionMovementRead,
        sessionStart: Long,
        sessionEnd: Long,
        activeIntervalJson: String?,
        baseScyra: Int,
        finalWithoutMovement: Int,
        finalScyra: Int,
        arcMultiplierUsed: Double?,
        arcBonusPoints: Int
    ) {
        if (!state.healthEnabledAtStart && !state.movementBonusEligibleAtStart) return
        val now = System.currentTimeMillis()
        val snapshot = FlowHealthSnapshotEntity(
            sessionId = sessionId,
            healthEnabledAtStart = state.healthEnabledAtStart,
            permissionGrantedAtStart = state.healthPermissionGrantedAtStart,
            status = if (state.movementBonusEligibleAtStart) movementRead.status else FlowHealthSyncStatus.NOT_ELIGIBLE,
            steps = movementRead.steps,
            rawMovementPoints = movementRead.movementPoints,
            finalMovementScyraContribution = (finalScyra - finalWithoutMovement).coerceAtLeast(0).toLong(),
            finalMovementPearlContribution = if (!state.isSoftMode) (finalScyra - finalWithoutMovement).coerceAtLeast(0).toLong() else 0L,
            firstCheckedAtMs = movementRead.checkedAtMs,
            lastCheckedAtMs = movementRead.checkedAtMs,
            capturedAtMs = if (movementRead.movementPoints > 0L) now else null,
            expiresAtMs = sessionEnd + FlowHealthRepository.REFRESH_WINDOW_MS,
            checkCount = if (movementRead.checkedAtMs != null) 1 else 0,
            flowStartTimeMs = sessionStart,
            flowEndTimeMs = sessionEnd,
            activeIntervalJson = activeIntervalJson
        )
        val breakdown = FlowRewardBreakdownEntity(
            sessionId = sessionId,
            nonMovementPreMultiplierPoints = baseScyra.toLong(),
            pulseBonusPoints = 0L,
            surgeBonusPoints = 0L,
            otherPreMultiplierBonusPoints = 0L,
            movementPoints = movementRead.movementPoints,
            preMultiplierTotal = (baseScyra + movementRead.movementPoints).toLong(),
            arcMultiplier = arcMultiplierUsed ?: 1.0,
            streakMultiplier = 1.0,
            otherMultiplier = 1.0,
            arcBonusPoints = arcBonusPoints.toLong(),
            finalScyraPoints = finalScyra.toLong(),
            pearlsEarned = if (!state.isSoftMode) finalScyra.toLong() else 0L,
            pearlEligible = !state.isSoftMode
        )
        flowHealthRepository.upsertCompletion(snapshot, breakdown)
    }

    private suspend fun saveWithArcBehavior(endMode: FlowEndAction) {
        val state = _uiState.value
        val title = state.title.trim()
        val tagName = state.tagName.trim()
        val description = ""

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
            val completionActiveIntervals = currentActiveIntervals(sessionEnd)
            val activeIntervalJson = FlowActiveIntervalCodec.encode(completionActiveIntervals)
            val sessionStart = completionActiveIntervals.firstOrNull()?.startTimeMs
                ?: (sessionEnd - realDurationMs).coerceAtLeast(0L)

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

            val movementRead = readMovementForCompletionIfEligible(
                state = state,
                activeIntervals = completionActiveIntervals
            )
            val beforeArc = baseScyra + movementRead.movementPoints.toInt()

            var localArc = arcState
            if (localArc != null && isArcExpired(sessionStart, localArc)) {
                clearArcPersisted()
                localArc = null
                arcState = null
                syncArcUi()
            }

            val arcIdForSummary: Long? = localArc?.arcId
            val isInExistingArc = localArc != null

            if (!state.isSoftMode && !isInExistingArc && endMode == FlowEndAction.CONTINUE_ARC) {
                val firstSessionId = sessionRepository.addSessionAndPromoteChronicle(
                    currentFlowInstanceId,
                    SessionEntity(title = title, description = "", tagId = tagId,
                        startTime = sessionStart, endTime = sessionEnd, durationMs = realDurationMs,
                        surgePlannedMs = state.surgePlannedMs, surgePoints = surgePoints,
                        scyraPoints = beforeArc, isSoftMode = state.isSoftMode)
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

                saveMovementSnapshotAndBreakdown(
                    sessionId = firstSessionId,
                    state = state,
                    movementRead = movementRead,
                    sessionStart = sessionStart,
                    sessionEnd = sessionEnd,
                    activeIntervalJson = activeIntervalJson,
                    baseScyra = baseScyra,
                    finalWithoutMovement = baseScyra,
                    finalScyra = beforeArc,
                    arcMultiplierUsed = 1.0,
                    arcBonusPoints = 0
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
                    movementSteps = movementRead.steps,
                    movementPoints = movementRead.movementPoints,
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
                activeIntervals.clear()
                activeIntervalStartMs = null
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
            var finalWithoutMovement = baseScyra

            if (isInExistingArc) {
                val s = localArc!!

                arcIndex = s.sessionCountInArc + 1

                if (usesBaseArcReward(isSoft, s.sessionCountInArc)) {
                    // Soft Flows and the first session of a pre-created Arc establish
                    // continuity without duration tiers or accumulated Arc rewards.
                    finalWithoutMovement = baseScyra
                    finalScyra = beforeArc
                    arcMultiplierUsed = ArcRuntimeState.BASE_MULTIPLIER
                    arcBonusPoints = 0
                    arcDidLevelUp = false
                    nextMultiplier = ArcRuntimeState.BASE_MULTIPLIER
                } else {
                    val resWithoutMovement = ScoreCalculator.arcMath(
                        beforeArcPoints = baseScyra,
                        chainBase = s.multiplier,
                        durationMs = realDurationMs
                    )
                    finalWithoutMovement = resWithoutMovement.finalPoints

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
            }

            val insertedId = sessionRepository.addSessionAndPromoteChronicle(
                currentFlowInstanceId,
                SessionEntity(title = title, description = "", tagId = tagId,
                    startTime = sessionStart, endTime = sessionEnd, durationMs = realDurationMs,
                    surgePlannedMs = state.surgePlannedMs, surgePoints = surgePoints,
                    scyraPoints = finalScyra, isSoftMode = state.isSoftMode)
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

                arcState = if (isSoft) {
                    s.afterCompletedSoftFlow(sessionEnd)
                } else {
                    s.copy(
                        isPending = newCount < 2,
                        multiplier = nextMultiplier ?: s.multiplier,
                        progressMs = 0L,
                        lastSessionEndTimeMs = sessionEnd,
                        sessionCountInArc = newCount
                    )
                }

                arcPrefs.save(arcState!!)
                syncArcUi()
            }

            if (isInExistingArc && endMode == FlowEndAction.CONTINUE_ARC) {
                when (persistPlannedStepAdvanceAfterSave()) {
                    PlannedArcAdvanceResult.Advanced -> arcPrefs.savePlannedFlowHandoff(
                        ArcPrefs.PlannedFlowHandoff.NEXT_PLANNED_STEP
                    )
                    PlannedArcAdvanceResult.Completed -> arcPrefs.savePlannedFlowHandoff(
                        ArcPrefs.PlannedFlowHandoff.BLANK_ARC_CONTINUATION
                    )
                    PlannedArcAdvanceResult.NotPlannedArc -> Unit
                }
            }

            saveMovementSnapshotAndBreakdown(
                sessionId = insertedId,
                state = state,
                movementRead = movementRead,
                sessionStart = sessionStart,
                sessionEnd = sessionEnd,
                activeIntervalJson = activeIntervalJson,
                baseScyra = baseScyra,
                finalWithoutMovement = finalWithoutMovement,
                finalScyra = finalScyra,
                arcMultiplierUsed = arcMultiplierUsed,
                arcBonusPoints = arcBonusPoints
            )

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
                movementSteps = movementRead.steps,
                movementPoints = movementRead.movementPoints,
                arcIndexInArc = arcIndex,
                arcMultiplierUsed = arcMultiplierUsed,
                arcBonusPoints = arcBonusPoints,
                arcNextMultiplier = arcState?.multiplier,
                arcProgressTowardNextMs = arcState?.progressMs ?: 0L,
                arcDidLevelUp = arcDidLevelUp,
                isSoftSession = state.isSoftMode
            ).withShellReward(shellReward)

            val resolvedEndMode = resolveFlowEndMode(
                requested = endMode,
                isSoftMode = state.isSoftMode,
                isInArc = isInExistingArc
            )

            when (resolvedEndMode) {
                FlowEndAction.COMPLETE_ARC -> {
                    if (activeArcRunRepository.getActiveArcRunOnce() != null) {
                        arcPrefs.savePlannedFlowHandoff(
                            ArcPrefs.PlannedFlowHandoff.COMPLETED_ARC_EXIT
                        )
                    }
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
                    _awaitingNextFlowAfterContinue.value = false

                    arcState?.let { completedArc ->
                        arcContinuationLifecycle.completeArc(completedArc, sessionEnd)
                    } ?: clearArcPersisted()
                    arcState = null

                    baseStartTimeMs = null
                    accumulatedBeforeStartMs = 0L
                    activeIntervals.clear()
                    activeIntervalStartMs = null
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
                    _awaitingNextFlowAfterContinue.value = false

                    if (!state.isSoftMode && arcState != null) {
                        saveRecentlyEndedArcSnapshot(
                            state = arcState,
                            endedAtMs = sessionEnd
                        )

                        clearArcPersisted()
                        arcState = null
                    }

                    baseStartTimeMs = null
                    accumulatedBeforeStartMs = 0L
                    activeIntervals.clear()
                    activeIntervalStartMs = null
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
                    if (!state.isSoftMode) {
                        activeArcRunRepository.clear()
                    }
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
                    _exitAfterReward.value = false
                    queueArcIdeaContinuationPromptIfNeeded(state)

                    baseStartTimeMs = null
                    accumulatedBeforeStartMs = 0L
                    activeIntervals.clear()
                    activeIntervalStartMs = null
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

    private suspend fun advancePlannedArcAfterCompletedSession(
        continuationOrigin: PendingArcIdeaContinuation?
    ): PlannedArcAdvanceResult {
        val activeRun = activeArcRunRepository.getActiveArcRunOnce()
            ?: return PlannedArcAdvanceResult.NotPlannedArc
        val displayedStepIndex = _uiState.value.plannedArcStepIndex
        val persistedAlreadyAdvanced = displayedStepIndex != null &&
                activeRun.currentStepIndex > displayedStepIndex
        val nextIndex = if (persistedAlreadyAdvanced) {
            activeRun.currentStepIndex
        } else {
            activeRun.currentStepIndex + 1
        }

        if (!persistedAlreadyAdvanced && plannedArcAdvanceResult(
                activeRun.currentStepIndex,
                activeRun.totalSteps
            ) ==
            PlannedArcAdvanceResult.Completed
        ) {
            activeArcRunRepository.clear()
            return PlannedArcAdvanceResult.Completed
        }

        val nextStep = arcPlanRepository
            .getStepsForArcPlanOnce(activeRun.arcPlanId)
            .sortedBy { it.orderIndex }
            .getOrNull(nextIndex)
            ?: run {
                // A malformed or edited plan must not leave a completed step resumable.
                activeArcRunRepository.clear()
                return PlannedArcAdvanceResult.Completed
            }

        val tagNameById = tagRepository.getAllTags()
            .firstOrNull()
            .orEmpty()
            .associate { it.id to it.name }
        val nextTagName = nextStep.tagIdSnapshot
            ?.let { tagId -> tagNameById[tagId] }
            .orEmpty()

        if (!persistedAlreadyAdvanced) {
            activeArcRunRepository.updateCurrentStep(
                currentStepIndex = nextIndex,
                currentStepTitle = nextStep.titleSnapshot,
                currentTagName = nextTagName,
                currentIsSoftMode = nextStep.isSoftModeSnapshot
            )
        }

        ongoingCreatedAtMs = System.currentTimeMillis()
        currentFlowInstanceId = UUID.randomUUID().toString()

        baseStartTimeMs = null
        accumulatedBeforeStartMs = 0L
        activeIntervals.clear()
        activeIntervalStartMs = null
        stopTicker()
        aliveFlowServiceController.stop()

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

        if (nextStep.isSoftModeSnapshot) {
            enterSoftModePreservingArc(persistSnapshotIfAlreadyApplied = true)
        } else {
            saveOngoingNow()
        }
        return PlannedArcAdvanceResult.Advanced
    }

    /**
     * Durably marks a planned step complete before its reward is dismissed. UI hydration
     * is intentionally deferred; process recreation therefore resumes the next step,
     * while the live process continues to show the completed session's reward.
     */
    private suspend fun persistPlannedStepAdvanceAfterSave(): PlannedArcAdvanceResult {
        val activeRun = activeArcRunRepository.getActiveArcRunOnce()
            ?: return PlannedArcAdvanceResult.NotPlannedArc
        val nextIndex = activeRun.currentStepIndex + 1
        if (nextIndex >= activeRun.totalSteps) {
            activeArcRunRepository.clear()
            return PlannedArcAdvanceResult.Completed
        }

        val nextStep = arcPlanRepository.getStepsForArcPlanOnce(activeRun.arcPlanId)
            .sortedBy { it.orderIndex }
            .getOrNull(nextIndex)
            ?: run {
                activeArcRunRepository.clear()
                return PlannedArcAdvanceResult.Completed
            }
        val tagNameById = tagRepository.getAllTags().firstOrNull().orEmpty()
            .associate { it.id to it.name }

        activeArcRunRepository.updateCurrentStep(
            currentStepIndex = nextIndex,
            currentStepTitle = nextStep.titleSnapshot,
            currentTagName = nextStep.tagIdSnapshot?.let(tagNameById::get).orEmpty(),
            currentIsSoftMode = nextStep.isSoftModeSnapshot
        )
        return PlannedArcAdvanceResult.Advanced
    }

    private fun isPlannedArcLaunch(): Boolean {
        return !plannedArcTitleOverride.isNullOrBlank() ||
                plannedArcStepIndexOverride != null ||
                plannedArcTotalStepsOverride != null
    }

    private suspend fun loadActiveArc(
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

        return null
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
