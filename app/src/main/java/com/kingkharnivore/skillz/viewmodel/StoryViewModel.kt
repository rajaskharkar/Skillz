package com.kingkharnivore.skillz.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.PulseEntity
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.health.FlowHealthRepository
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSnapshotEntity
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.data.repository.PulseRepository
import com.kingkharnivore.skillz.domain.health.HealthRefreshUseCase
import com.kingkharnivore.skillz.model.state.FlowListUiState
import com.kingkharnivore.skillz.model.ui.ArcFlowItemUiModel
import com.kingkharnivore.skillz.model.ui.ChronicleUiModel
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.model.ui.Journey7dStatUiModel
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
import com.kingkharnivore.skillz.utils.localization.AppLocaleManager
import com.kingkharnivore.skillz.utils.time.StoryPeriod
import com.kingkharnivore.skillz.utils.time.TimeWindowUtils
import com.kingkharnivore.skillz.utils.user.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagUiModel(
    val id: Long,
    val name: String
)

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val sessionRepository: FlowRepository,
    private val pulseRepository: PulseRepository,
    private val tagRepository: JourneyRepository,
    private val aliveFlowRepository: AliveFlowRepository,
    private val flowHealthRepository: FlowHealthRepository,
    private val healthRefreshUseCase: HealthRefreshUseCase,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    private val showScoreUiFlow = userPrefs.showScoreUi
    private val calmModeFlow = userPrefs.calmMode
    private val appLanguageTagFlow = userPrefs.appLanguageTag

    private val period = MutableStateFlow(StoryPeriod.WEEK)
    private val anchorDayStartMs = MutableStateFlow(
        TimeWindowUtils.startOfPeriodMs(System.currentTimeMillis(), StoryPeriod.WEEK)
    )

    private val viewJourneysTagId = MutableStateFlow<Long?>(null)
    private val isViewJourneysOpen = MutableStateFlow(false)

    private val journeyColorMemory = linkedMapOf<Long, Color>()
    private var nextJourneyColorIndex = 0

    private val sessionsFlow: Flow<List<SessionEntity>> = sessionRepository.getAllSessions()
    private val healthSnapshotsFlow: Flow<List<FlowHealthSnapshotEntity>> = flowHealthRepository.observeSnapshots()
    private val pulsesFlow: Flow<List<PulseEntity>> = pulseRepository.getAllPulses()
    private val tagsFlow: Flow<List<TagEntity>> = tagRepository.getAllTags()

    val uiState = MutableStateFlow(
        FlowListUiState(
            isLoading = true,
            period = period.value,
            anchorDayStartMs = anchorDayStartMs.value
        )
    )

    init {
        observeSessions()
        viewModelScope.launch { runCatching { healthRefreshUseCase.refreshForeground(force = true) } }
    }

    fun setShowScoreUi(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setShowScoreUi(enabled)
        }
    }

    fun setCalmMode(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setCalmMode(enabled)
        }
    }

    fun setAppLanguage(tag: String?) {
        viewModelScope.launch {
            userPrefs.setAppLanguageTag(tag)
            AppLocaleManager.applyLanguage(tag)
        }
    }

    fun openViewJourneys(tagId: Long) {
        viewJourneysTagId.value = tagId
        isViewJourneysOpen.value = true
    }

    fun closeViewJourneys() {
        isViewJourneysOpen.value = false
        viewJourneysTagId.value = null
    }

    fun selectTag(tagId: Long?) {
        selectedTagIds.value = tagId?.let { setOf(it) } ?: emptySet()
    }

    fun onTagToggled(tagId: Long) {
        selectedTagIds.update { current ->
            current.toMutableSet().apply {
                if (contains(tagId)) remove(tagId) else add(tagId)
            }
        }
    }

    fun onClearAllTags() {
        selectedTagIds.value = emptySet()
    }

    fun onPeriodSelected(newPeriod: StoryPeriod) {
        val nowMs = System.currentTimeMillis()
        period.value = newPeriod

        val todayAnchor = TimeWindowUtils.startOfPeriodMs(nowMs, newPeriod)
        anchorDayStartMs.value = TimeWindowUtils.clampToFirstAndToday(
            anchorStartMs = todayAnchor,
            period = newPeriod,
            firstSessionStartMs = uiState.value.firstSessionStartMs,
            nowMs = nowMs
        )
    }

    fun goPrev() {
        val p = period.value
        val current = TimeWindowUtils.normalizeAnchor(anchorDayStartMs.value, p)
        val candidate = TimeWindowUtils.shiftAnchor(current, p, -1)
        setAnchorClamped(candidate, p)
    }

    fun goNext() {
        val p = period.value
        val current = TimeWindowUtils.normalizeAnchor(anchorDayStartMs.value, p)
        val candidate = TimeWindowUtils.shiftAnchor(current, p, +1)
        setAnchorClamped(candidate, p)
    }

    fun goToday() {
        val p = period.value
        val todayAnchor = TimeWindowUtils.startOfPeriodMs(System.currentTimeMillis(), p)
        setAnchorClamped(todayAnchor, p)
    }

    fun updateSessionDescription(sessionId: Long, description: String) {
        viewModelScope.launch {
            sessionRepository.updateSessionDescription(sessionId, description)
        }
    }

    fun createPulseFromStory(
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
                uiState.value = uiState.value.copy(
                    errorMessage = "Add a title or description to save this moment."
                )
                return@launch
            }

            val tagId = if (trimmedTag.isBlank()) {
                null
            } else {
                tagRepository.getOrCreateTagId(trimmedTag)
            }

            val ongoing = aliveFlowRepository.getOngoingSession().firstOrNull()
            val shouldAttach = attachToCurrentFlow && ongoing?.isInFlowMode == true

            pulseRepository.addPulse(
                title = trimmedTitle,
                description = trimmedDescription,
                tagId = tagId,
                parentSessionId = null,
                parentFlowInstanceId = if (shouldAttach) ongoing?.flowInstanceId else null,
                arcId = if (shouldAttach) ongoing?.arcId else null
            )

            uiState.value = uiState.value.copy(errorMessage = null)
        }
    }

    fun updatePulse(
        pulseId: Long,
        title: String,
        description: String,
        tagName: String
    ) {
        viewModelScope.launch {
            val trimmedTitle = title.trim()
            val trimmedDescription = description.trim()
            val trimmedTag = tagName.trim()

            if (trimmedTitle.isBlank() && trimmedDescription.isBlank()) return@launch

            val tagId = if (trimmedTag.isBlank()) {
                null
            } else {
                tagRepository.getOrCreateTagId(trimmedTag)
            }

            val removedTagId = pulseRepository.updatePulseDetails(
                pulseId = pulseId,
                title = trimmedTitle,
                description = trimmedDescription,
                tagId = tagId
            )

            if (removedTagId != null) {
                selectedTagIds.value = selectedTagIds.value - removedTagId
            }
        }
    }

    fun createPulseForSession(
        sessionId: Long,
        title: String,
        description: String,
        tagName: String
    ) {
        viewModelScope.launch {
            val session = sessionRepository.getSessionById(sessionId) ?: return@launch

            val trimmedTitle = title.trim()
            val trimmedDescription = description.trim()
            val trimmedTag = tagName.trim()

            if (trimmedTitle.isBlank() && trimmedDescription.isBlank()) return@launch

            val tagId = if (trimmedTag.isBlank()) {
                null
            } else {
                tagRepository.getOrCreateTagId(trimmedTag)
            }

            pulseRepository.addPulse(
                title = trimmedTitle,
                description = trimmedDescription,
                tagId = tagId,
                parentSessionId = sessionId,
                parentFlowInstanceId = null,
                arcId = session.arcId
            )
        }
    }

    fun deletePulse(pulseId: Long) {
        viewModelScope.launch {
            pulseRepository.deletePulseAndCleanupTag(pulseId)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            try {
                val removedTagId = sessionRepository.deleteSessionAndCleanupTag(sessionId)
                if (removedTagId != null) {
                    selectedTagIds.value = selectedTagIds.value - removedTagId
                }
            } catch (_: Exception) {
            }
        }
    }

    fun clearError() {
        uiState.value = uiState.value.copy(errorMessage = null)
    }

    private fun observeSessions() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            combine(
                sessionsFlow,
                healthSnapshotsFlow,
                pulsesFlow,
                tagsFlow,
                selectedTagIds,
                period,
                anchorDayStartMs,
                viewJourneysTagId,
                isViewJourneysOpen,
                showScoreUiFlow,
                calmModeFlow,
                appLanguageTagFlow
            ) { arr: Array<Any?> ->
                val sessions = arr[0] as List<SessionEntity>
                val healthSnapshots = arr[1] as List<FlowHealthSnapshotEntity>
                val pulses = arr[2] as List<PulseEntity>
                val tags = arr[3] as List<TagEntity>
                val currentTagIds = arr[4] as Set<Long>
                val currentPeriod = arr[5] as StoryPeriod
                val anchorStartMs = arr[6] as Long
                val viewTagId = arr[7] as Long?
                val viewOpen = arr[8] as Boolean
                val showScoreUi = arr[9] as Boolean
                val calmMode = arr[10] as Boolean
                val appLanguageTag = arr[11] as String?

                val hasAnyRecordedFlows = sessions.isNotEmpty()
                val hasAnyRecordedArtifacts = sessions.isNotEmpty() || pulses.isNotEmpty()
                val nowMs = System.currentTimeMillis()

                val tagNameById: Map<Long, String> = tags.associate { it.id to it.name }

                val firstRecordedAtMs: Long? =
                    listOfNotNull(
                        sessions.minOfOrNull { it.createdAt },
                        pulses.minOfOrNull { it.createdAt }
                    ).minOrNull()

                val tagUsageCount: Map<Long, Int> =
                    buildList {
                        addAll(sessions.map { it.tagId })
                        addAll(pulses.mapNotNull { it.tagId })
                    }.groupingBy { it }.eachCount()

                val visibleTags: List<TagEntity> = tags.filter { (tagUsageCount[it.id] ?: 0) > 0 }

                val effectiveSelectedTagIds: Set<Long> =
                    currentTagIds.filterTo(linkedSetOf()) { (tagUsageCount[it] ?: 0) > 0 }

                val sessionsForTags: List<SessionEntity> =
                    if (effectiveSelectedTagIds.isEmpty()) {
                        sessions
                    } else {
                        sessions.filter { it.tagId in effectiveSelectedTagIds }
                    }

                val pulsesForTags: List<PulseEntity> =
                    if (effectiveSelectedTagIds.isEmpty()) {
                        pulses
                    } else {
                        pulses.filter { it.tagId != null && it.tagId in effectiveSelectedTagIds }
                    }

                val normalizedAnchor = TimeWindowUtils.normalizeAnchor(anchorStartMs, currentPeriod)
                val window = TimeWindowUtils.windowFor(normalizedAnchor, currentPeriod)

                val visibleSessions =
                    sessionsForTags.filter { it.createdAt in window.startMs until window.endMs }

                val visiblePulses =
                    pulsesForTags.filter { it.createdAt in window.startMs until window.endMs }

                val visibleJourneyIdsInPriorityOrder: List<Long> =
                    buildList {
                        addAll(visibleSessions.map { it.tagId })
                        addAll(visiblePulses.mapNotNull { it.tagId })
                    }
                        .groupingBy { it }
                        .eachCount()
                        .entries
                        .sortedWith(
                            compareByDescending<Map.Entry<Long, Int>> { it.value }
                                .thenByDescending { entry ->
                                    val latestSession = visibleSessions
                                        .filter { it.tagId == entry.key }
                                        .maxOfOrNull { it.createdAt } ?: 0L

                                    val latestPulse = visiblePulses
                                        .filter { it.tagId == entry.key }
                                        .maxOfOrNull { it.createdAt } ?: 0L

                                    maxOf(latestSession, latestPulse)
                                }
                        )
                        .map { it.key }

                val journeyColors = getOrCreateJourneyColors(visibleJourneyIdsInPriorityOrder)

                val chronicleItems = buildChronicleItems(
                    allSessions = sessions,
                    visibleSessionsInWindow = visibleSessions,
                    allPulses = pulses,
                    visiblePulsesInWindow = visiblePulses,
                    tags = tags,
                    healthSnapshots = healthSnapshots,
                    journeyColors = journeyColors,
                    selectedTagIds = effectiveSelectedTagIds
                )

                val todayAnchor = TimeWindowUtils.startOfPeriodMs(nowMs, currentPeriod)
                val isCurrentPeriod = normalizedAnchor == todayAnchor

                val topJourneysLast7d: List<Journey7dStatUiModel> =
                    if (isCurrentPeriod) {
                        val sevenDaysMs = 7L * 24L * 60L * 60L * 1000L
                        val last7dStart = nowMs - sevenDaysMs

                        sessions.asSequence()
                            .filter { it.createdAt >= last7dStart }
                            .groupBy { it.tagId }
                            .map { (tagId, ss) ->
                                Journey7dStatUiModel(
                                    tagId = tagId,
                                    tagName = tagNameById[tagId].orEmpty(),
                                    totalScore = ss.sumOf { it.scyraPoints },
                                    totalDurationMs = ss.sumOf { it.durationMs },
                                    sessionsCount = ss.size
                                )
                            }
                            .filter { it.tagName.isNotBlank() }
                            .sortedWith(
                                compareByDescending<Journey7dStatUiModel> { it.totalScore }
                                    .thenByDescending { it.totalDurationMs }
                                    .thenByDescending { it.sessionsCount }
                            )
                            .take(5)
                            .toList()
                    } else {
                        emptyList()
                    }

                val sagasInView: List<Journey7dStatUiModel> =
                    sessions.asSequence()
                        .filter { it.createdAt in window.startMs until window.endMs }
                        .groupBy { it.tagId }
                        .map { (tagId, ss) ->
                            Journey7dStatUiModel(
                                tagId = tagId,
                                tagName = tagNameById[tagId].orEmpty(),
                                totalScore = ss.sumOf { it.scyraPoints },
                                totalDurationMs = ss.sumOf { it.durationMs },
                                sessionsCount = ss.size
                            )
                        }
                        .filter { it.tagName.isNotBlank() }
                        .sortedWith(
                            compareByDescending<Journey7dStatUiModel> { it.totalScore }
                                .thenByDescending { it.totalDurationMs }
                                .thenByDescending { it.sessionsCount }
                        )
                        .toList()

                val viewJourneysSessions: List<FlowListItemUiModel> =
                    if (viewOpen && viewTagId != null) {
                        sessions.asSequence()
                            .filter { it.tagId == viewTagId }
                            .filter { it.createdAt in window.startMs until window.endMs }
                            .sortedByDescending { it.createdAt }
                            .toList()
                            .toUiModels(tags, journeyColors, healthSnapshots)
                    } else {
                        emptyList()
                    }

                val viewJourneysTitle =
                    if (viewOpen && viewTagId != null) tagNameById[viewTagId].orEmpty() else ""

                val pulsesBySessionId: Map<Long, List<PulseListItemUiModel>> =
                    visiblePulses
                        .filter { it.parentSessionId != null }
                        .groupBy { it.parentSessionId!! }
                        .mapValues { (_, grouped) ->
                            grouped
                                .sortedByDescending { it.createdAt }
                                .toUiModels(tags)
                        }

                val totalDurationMs = visibleSessions.sumOf { it.durationMs }
                val totalScore = visibleSessions.sumOf { it.scyraPoints }
                val currentSurgeScore = visibleSessions.sumOf { it.surgePoints }

                FlowListUiState(
                    isLoading = false,
                    sessions = visibleSessions.toUiModels(tags, journeyColors, healthSnapshots),
                    pulses = visiblePulses.toUiModels(tags),
                    chronicleItems = chronicleItems,
                    tags = visibleTags.toUiModels(),
                    selectedTagIds = effectiveSelectedTagIds,
                    totalDurationMs = totalDurationMs,
                    pulseCountInView = visiblePulses.size,
                    errorMessage = null,
                    period = currentPeriod,
                    anchorDayStartMs = normalizedAnchor,
                    currentScore = totalScore,
                    currentSurgeScore = currentSurgeScore,
                    topJourneysLast7d = topJourneysLast7d,
                    firstSessionStartMs = firstRecordedAtMs,
                    isCurrentPeriod = isCurrentPeriod,
                    hasAnyRecordedFlows = hasAnyRecordedFlows,
                    hasAnyRecordedArtifacts = hasAnyRecordedArtifacts,
                    sagasInView = sagasInView,
                    sagaPulsesInView = visiblePulses.toUiModels(tags),
                    pulsesBySessionId = pulsesBySessionId,
                    isViewJourneysOpen = viewOpen,
                    viewJourneysTitle = viewJourneysTitle,
                    viewJourneysSessions = viewJourneysSessions,
                    showScoreUi = showScoreUi,
                    calmMode = calmMode,
                    appLanguageTag = appLanguageTag
                )
            }
                .catch { e ->
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Something went wrong"
                    )
                }
                .collect { newState ->
                    uiState.value = newState
                }
        }
    }

    private fun setAnchorClamped(
        anchorCandidateMs: Long,
        periodValue: StoryPeriod,
        nowMs: Long = System.currentTimeMillis()
    ) {
        anchorDayStartMs.value = TimeWindowUtils.clampToFirstAndToday(
            anchorStartMs = TimeWindowUtils.normalizeAnchor(anchorCandidateMs, periodValue),
            period = periodValue,
            firstSessionStartMs = uiState.value.firstSessionStartMs,
            nowMs = nowMs
        )
    }

    private fun getOrCreateJourneyColors(tagIds: List<Long>): Map<Long, Color> {
        val missingTagIds = tagIds.filter { it !in journeyColorMemory }

        if (missingTagIds.isNotEmpty()) {
            val palette = buildJourneyPalette()
            missingTagIds.forEach { tagId ->
                val color = palette[nextJourneyColorIndex % palette.size]
                journeyColorMemory[tagId] = color
                nextJourneyColorIndex++
            }
        }

        return tagIds.associateWith { tagId ->
            journeyColorMemory[tagId] ?: Color.Gray
        }
    }

    private fun buildJourneyPalette(): List<Color> {
        val base = listOf(
            Color(0xFF8B1E1E),
            Color(0xFF3A5F8C),
            Color(0xFF2F8F86),
            Color(0xFF6F9E91),
            Color(0xFFD1B45A),
            Color(0xFFCC8A3E),
            Color(0xFF7A4A32),
            Color(0xFF8C6AA8),
            Color(0xFF3E8F6B)
        )

        val lighter = base.map { androidx.compose.ui.graphics.lerp(it, Color.White, 0.18f) }
        val darker = base.map { androidx.compose.ui.graphics.lerp(it, Color.Black, 0.12f) }

        return buildList {
            addAll(base)
            addAll(lighter)
            addAll(darker)
        }
    }

    private fun List<SessionEntity>.toUiModels(
        tags: List<TagEntity>,
        journeyColors: Map<Long, Color>,
        healthSnapshots: List<FlowHealthSnapshotEntity> = emptyList()
    ): List<FlowListItemUiModel> {
        val tagNameById = tags.associate { it.id to it.name }
        val healthBySessionId = healthSnapshots.associateBy { it.sessionId }

        return map { session ->
            val health = healthBySessionId[session.id]
            FlowListItemUiModel(
                sessionId = session.id,
                title = session.title,
                description = session.description,
                tagId = session.tagId,
                tagName = tagNameById[session.tagId].orEmpty(),
                journeyColor = journeyColors[session.tagId] ?: Color.Gray,
                durationMs = session.durationMs,
                createdAt = session.createdAt,
                score = session.scyraPoints,
                isSoftMode = session.isSoftMode,
                isSurge = session.surgePlannedMs != null,
                surgePoints = session.surgePoints,
                arcId = session.arcId,
                arcIndex = session.arcIndex,
                arcMultiplierUsed = session.arcMultiplierUsed,
                arcBonusPoints = session.arcBonusPoints,
                movementSteps = health?.finalAwardedSteps ?: health?.steps,
                movementPoints = health?.finalAwardedMovementPoints ?: health?.rawMovementPoints ?: 0L,
                movementBonusUpdatedAfterSync = health?.updatedAfterSync ?: false,
                movementDataSource = health?.movementDataSource,
                movementIsPhoneEstimate = health?.movementDataSource == com.kingkharnivore.skillz.data.model.entity.health.MovementDataSourceType.PHONE_SENSOR
            )
        }
    }

    private fun List<PulseEntity>.toUiModels(
        tags: List<TagEntity>
    ): List<PulseListItemUiModel> {
        val tagNameById = tags.associate { it.id to it.name }

        return map { pulse ->
            PulseListItemUiModel(
                pulseId = pulse.id,
                title = pulse.title,
                description = pulse.description,
                tagId = pulse.tagId,
                tagName = pulse.tagId?.let { tagNameById[it].orEmpty() }.orEmpty(),
                createdAt = pulse.createdAt,
                parentSessionId = pulse.parentSessionId,
                arcId = pulse.arcId
            )
        }
    }

    private fun List<TagEntity>.toUiModels(): List<TagUiModel> =
        map { TagUiModel(id = it.id, name = it.name) }

    private fun buildChronicleItems(
        allSessions: List<SessionEntity>,
        visibleSessionsInWindow: List<SessionEntity>,
        allPulses: List<PulseEntity>,
        visiblePulsesInWindow: List<PulseEntity>,
        tags: List<TagEntity>,
        healthSnapshots: List<FlowHealthSnapshotEntity>,
        journeyColors: Map<Long, Color>,
        selectedTagIds: Set<Long>
    ): List<ChronicleUiModel> {
        val visibleFlowUi = visibleSessionsInWindow.toUiModels(tags, journeyColors, healthSnapshots)
        val visibleFlowUiBySessionId = visibleFlowUi.associateBy { it.sessionId }

        val visiblePulseUi = visiblePulsesInWindow.toUiModels(tags)
        val visiblePulseUiById = visiblePulseUi.associateBy { it.pulseId }

        val visiblePulsesBySessionId = visiblePulsesInWindow
            .filter { it.parentSessionId != null }
            .groupBy { it.parentSessionId!! }

        val allByArcId = allSessions
            .filter { it.arcId != null }
            .groupBy { it.arcId!! }

        val visibleByArcId = visibleSessionsInWindow
            .filter { it.arcId != null }
            .groupBy { it.arcId!! }

        val topLevel = mutableListOf<Pair<Long, ChronicleUiModel>>()
        val emittedArcIds = mutableSetOf<Long>()

        visibleSessionsInWindow.forEach { session ->
            val arcId = session.arcId

            if (arcId == null) {
                val flowUi = visibleFlowUiBySessionId[session.id] ?: return@forEach
                val childPulses = visiblePulsesBySessionId[session.id]
                    .orEmpty()
                    .sortedByDescending { it.createdAt }
                    .mapNotNull { visiblePulseUiById[it.id] }

                topLevel += session.createdAt to ChronicleUiModel.StandaloneFlow(
                    flow = flowUi,
                    childPulses = childPulses
                )
                return@forEach
            }

            if (!emittedArcIds.add(arcId)) return@forEach

            val allArcSessions = allByArcId[arcId].orEmpty().sortedByDescending { it.createdAt }
            val visibleArcSessions = visibleByArcId[arcId].orEmpty().sortedByDescending { it.createdAt }

            if (allArcSessions.size < 2) {
                val flowUi = visibleFlowUiBySessionId[session.id] ?: return@forEach
                val childPulses = visiblePulsesBySessionId[session.id]
                    .orEmpty()
                    .sortedByDescending { it.createdAt }
                    .mapNotNull { visiblePulseUiById[it.id] }

                topLevel += session.createdAt to ChronicleUiModel.StandaloneFlow(
                    flow = flowUi,
                    childPulses = childPulses
                )
                return@forEach
            }

            val visibleFlows = visibleArcSessions.mapIndexed { index, s ->
                ArcFlowItemUiModel(
                    flow = visibleFlowUiBySessionId[s.id]
                        ?: error("Missing ui model for visible session ${s.id}"),
                    childPulses = visiblePulsesBySessionId[s.id]
                        .orEmpty()
                        .sortedByDescending { it.createdAt }
                        .mapNotNull { visiblePulseUiById[it.id] },
                    isFirstVisibleInArc = index == 0,
                    isLastVisibleInArc = index == visibleArcSessions.lastIndex
                )
            }

            val hiddenFlowsCount = (allArcSessions.size - visibleArcSessions.size).coerceAtLeast(0)
            val totalArcDurationMs = allArcSessions.sumOf { it.durationMs }
            val totalArcScore = allArcSessions.sumOf { it.scyraPoints }
            val peakMultiplier = allArcSessions.maxOfOrNull { it.arcMultiplierUsed ?: 0.0 }
                ?.takeIf { it > 0.0 }

            val visibleJourneyIds = visibleArcSessions.map { it.tagId }.distinct()
            val headerAccentColor = if (visibleJourneyIds.size == 1) {
                journeyColors[visibleJourneyIds.first()]
            } else {
                null
            }

            val filteredJourneyDurationMs =
                if (selectedTagIds.isNotEmpty()) {
                    allArcSessions
                        .filter { it.tagId in selectedTagIds }
                        .sumOf { it.durationMs }
                        .takeIf { it > 0L }
                } else {
                    null
                }

            val filteredJourneyPercentOfArc =
                if (selectedTagIds.isNotEmpty() && totalArcDurationMs > 0L) {
                    val duration = allArcSessions
                        .filter { it.tagId in selectedTagIds }
                        .sumOf { it.durationMs }

                    ((duration.toDouble() / totalArcDurationMs.toDouble()) * 100.0)
                        .toInt()
                        .coerceIn(0, 100)
                } else {
                    null
                }

            val arcTopTime = visibleArcSessions.maxOfOrNull { it.createdAt } ?: session.createdAt

            topLevel += arcTopTime to ChronicleUiModel.ArcGroup(
                arcId = arcId,
                headerAccentColor = headerAccentColor,
                totalArcDurationMs = totalArcDurationMs,
                totalArcScore = totalArcScore,
                peakMultiplier = peakMultiplier,
                visibleFlows = visibleFlows,
                hiddenFlowsCount = hiddenFlowsCount,
                totalFlowsCount = allArcSessions.size,
                filteredJourneyDurationMs = filteredJourneyDurationMs,
                filteredJourneyPercentOfArc = filteredJourneyPercentOfArc
            )
        }

        visiblePulsesInWindow
            .filter { it.parentSessionId == null }
            .forEach { pulse ->
                val pulseUi = visiblePulseUiById[pulse.id] ?: return@forEach
                topLevel += pulse.createdAt to ChronicleUiModel.StandalonePulse(pulseUi)
            }

        return topLevel
            .sortedByDescending { it.first }
            .map { it.second }
    }
}