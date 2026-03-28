package com.kingkharnivore.skillz.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.model.state.FlowListUiState
import com.kingkharnivore.skillz.model.ui.Journey7dStatUiModel
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.model.ui.ArcFlowItemUiModel
import com.kingkharnivore.skillz.model.ui.ChronicleUiModel
import com.kingkharnivore.skillz.utils.time.StoryPeriod
import com.kingkharnivore.skillz.utils.time.TimeWindowUtils
import com.kingkharnivore.skillz.utils.user.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagUiModel(
    val id: Long,
    val name: String
)

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val sessionRepository: FlowRepository,
    private val tagRepository: JourneyRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    private val showScoreUiFlow = userPrefs.showScoreUi
    private val calmModeFlow = userPrefs.calmMode

    private val period = MutableStateFlow(StoryPeriod.WEEK)
    private val anchorDayStartMs = MutableStateFlow(
        TimeWindowUtils.startOfPeriodMs(System.currentTimeMillis(), StoryPeriod.WEEK)
    )

    private val viewJourneysTagId = MutableStateFlow<Long?>(null)
    private val isViewJourneysOpen = MutableStateFlow(false)

    private val journeyColorMemory = linkedMapOf<Long, Color>()
    private var nextJourneyColorIndex = 0

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

    fun openViewJourneys(tagId: Long) {
        viewJourneysTagId.value = tagId
        isViewJourneysOpen.value = true
    }

    fun closeViewJourneys() {
        isViewJourneysOpen.value = false
        viewJourneysTagId.value = null
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

    private val sessionsFlow: Flow<List<SessionEntity>> =
        sessionRepository.getAllSessions()

    private val tagsFlow: Flow<List<TagEntity>> =
        tagRepository.getAllTags()

    val uiState = MutableStateFlow(
        FlowListUiState(
            isLoading = true,
            period = period.value,
            anchorDayStartMs = anchorDayStartMs.value
        )
    )

    init {
        observeSessions()
    }

    fun setShowScoreUi(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setShowScoreUi(enabled) }
    }

    fun setCalmMode(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setCalmMode(enabled) }
    }

    fun selectTag(tagId: Long?) {
        selectedTagIds.value = tagId?.let { setOf(it) } ?: emptySet()
    }

    fun onTagToggled(tagId: Long) {
        selectedTagIds.value = selectedTagIds.value.toMutableSet().apply {
            if (contains(tagId)) remove(tagId) else add(tagId)
        }
    }

    fun onClearAllTags() {
        selectedTagIds.value = emptySet()
    }

    private fun nowMs(): Long = System.currentTimeMillis()

    private fun setClampedAnchor(
        period: StoryPeriod,
        anchorStartMs: Long,
        nowMs: Long = nowMs()
    ) {
        anchorDayStartMs.value = TimeWindowUtils.clampToFirstAndToday(
            anchorStartMs = anchorStartMs,
            period = period,
            firstSessionStartMs = uiState.value.firstSessionStartMs,
            nowMs = nowMs
        )
    }

    private fun navigatePeriod(dir: Int) {
        val p = period.value
        val normalized = TimeWindowUtils.normalizeAnchor(anchorDayStartMs.value, p)
        val shifted = TimeWindowUtils.shiftAnchor(normalized, p, dir)
        setClampedAnchor(period = p, anchorStartMs = shifted)
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

    fun clearError() {
        uiState.value = uiState.value.copy(errorMessage = null)
    }

    private fun List<SessionEntity>.toUiModels(
        tags: List<TagEntity>,
        journeyColors: Map<Long, Color>
    ): List<FlowListItemUiModel> {
        val tagNameById: Map<Long, String> = tags.associate { it.id to it.name }

        return map { session ->
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
                isSurge = session.surgePlannedMs != null,
                surgePoints = session.surgePoints,
                beamBonusPoints = session.beamBonusPoints,
                arcId = session.arcId,
                arcIndex = session.arcIndex,
                arcMultiplierUsed = session.arcMultiplierUsed,
                arcBonusPoints = session.arcBonusPoints
            )
        }
    }

    private fun List<TagEntity>.toUiModels(): List<TagUiModel> =
        map { TagUiModel(id = it.id, name = it.name) }

    private fun observeSessions() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            combine(
                sessionsFlow,
                tagsFlow,
                selectedTagIds,
                period,
                anchorDayStartMs,
                viewJourneysTagId,
                isViewJourneysOpen,
                showScoreUiFlow,
                calmModeFlow
            ) { arr: Array<Any?> ->

                val sessions = arr[0] as List<SessionEntity>
                val tags = arr[1] as List<TagEntity>
                val currentTagIds = arr[2] as Set<Long>
                val currentPeriod = arr[3] as StoryPeriod
                val anchorStartMs = arr[4] as Long
                val viewTagId = arr[5] as Long?
                val viewOpen = arr[6] as Boolean
                val showScoreUi = arr[7] as Boolean
                val calmMode = arr[8] as Boolean

                val hasAnyRecordedFlows = sessions.isNotEmpty()
                val nowMs = System.currentTimeMillis()

                val tagNameById: Map<Long, String> = tags.associate { it.id to it.name }

                val firstSessionStartMs: Long? = sessions.minOfOrNull { it.createdAt }

                val tagUsageCount: Map<Long, Int> = sessions.groupingBy { it.tagId }.eachCount()
                val visibleTags: List<TagEntity> = tags.filter { (tagUsageCount[it.id] ?: 0) > 0 }

                val effectiveSelectedTagIds: Set<Long> =
                    currentTagIds.filterTo(linkedSetOf()) { (tagUsageCount[it] ?: 0) > 0 }

                val sessionsForTags: List<SessionEntity> =
                    if (effectiveSelectedTagIds.isEmpty()) {
                        sessions
                    } else {
                        sessions.filter { it.tagId in effectiveSelectedTagIds }
                    }

                val normalizedAnchor =
                    TimeWindowUtils.normalizeAnchor(anchorStartMs, currentPeriod)

                val window =
                    TimeWindowUtils.windowFor(normalizedAnchor, currentPeriod)

                val visibleSessions: List<SessionEntity> =
                    sessionsForTags.filter { it.createdAt in window.startMs until window.endMs }

                val visibleJourneyIdsInPriorityOrder: List<Long> =
                    visibleSessions
                        .groupBy { it.tagId }
                        .entries
                        .sortedWith(
                            compareByDescending<Map.Entry<Long, List<SessionEntity>>> { it.value.size }
                                .thenByDescending { entry ->
                                    entry.value.maxOfOrNull { it.createdAt } ?: 0L
                                }
                        )
                        .map { it.key }

                val journeyColors = getOrCreateJourneyColors(visibleJourneyIdsInPriorityOrder)

                val chronicleItems = buildChronicleItems(
                    allSessions = sessions,
                    visibleSessionsInWindow = visibleSessions,
                    tags = tags,
                    journeyColors = journeyColors,
                    selectedTagIds = effectiveSelectedTagIds
                )

                val todayAnchor =
                    TimeWindowUtils.startOfPeriodMs(nowMs, currentPeriod)

                val isCurrentPeriod: Boolean =
                    normalizedAnchor == todayAnchor

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
                            .toUiModels(tags, journeyColors)
                    } else {
                        emptyList()
                    }

                val viewJourneysTitle: String =
                    if (viewOpen && viewTagId != null) {
                        tagNameById[viewTagId].orEmpty()
                    } else {
                        ""
                    }

                val totalDurationMs = visibleSessions.sumOf { it.durationMs }
                val totalScore = visibleSessions.sumOf { it.scyraPoints }
                val currentSurgeScore = visibleSessions.sumOf { it.surgePoints }

                FlowListUiState(
                    isLoading = false,
                    sessions = visibleSessions.toUiModels(tags, journeyColors),
                    chronicleItems = chronicleItems,
                    tags = visibleTags.toUiModels(),
                    selectedTagIds = effectiveSelectedTagIds,
                    totalDurationMs = totalDurationMs,
                    errorMessage = null,
                    period = currentPeriod,
                    anchorDayStartMs = normalizedAnchor,
                    currentScore = totalScore,
                    currentSurgeScore = currentSurgeScore,
                    topJourneysLast7d = topJourneysLast7d,
                    firstSessionStartMs = firstSessionStartMs,
                    isCurrentPeriod = isCurrentPeriod,
                    hasAnyRecordedFlows = hasAnyRecordedFlows,
                    sagasInView = sagasInView,
                    isViewJourneysOpen = viewOpen,
                    viewJourneysTitle = viewJourneysTitle,
                    viewJourneysSessions = viewJourneysSessions,
                    showScoreUi = showScoreUi,
                    calmMode = calmMode
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

    private fun buildChronicleItems(
        allSessions: List<SessionEntity>,
        visibleSessionsInWindow: List<SessionEntity>,
        tags: List<TagEntity>,
        journeyColors: Map<Long, Color>,
        selectedTagIds: Set<Long>
    ): List<ChronicleUiModel> {
        val visibleUi = visibleSessionsInWindow.toUiModels(tags, journeyColors)
        val visibleUiBySessionId = visibleUi.associateBy { it.sessionId }

        val allByArcId = allSessions
            .filter { it.arcId != null }
            .groupBy { it.arcId!! }

        val visibleByArcId = visibleSessionsInWindow
            .filter { it.arcId != null }
            .groupBy { it.arcId!! }

        val chronicleItems = mutableListOf<ChronicleUiModel>()
        val emittedArcIds = mutableSetOf<Long>()

        visibleSessionsInWindow.forEach { session ->
            val arcId = session.arcId

            if (arcId == null) {
                val flowUi = visibleUiBySessionId[session.id] ?: return@forEach
                chronicleItems += ChronicleUiModel.StandaloneFlow(flowUi)
                return@forEach
            }

            if (!emittedArcIds.add(arcId)) return@forEach

            val allArcSessions = allByArcId[arcId].orEmpty().sortedByDescending { it.createdAt }
            val visibleArcSessions = visibleByArcId[arcId].orEmpty().sortedByDescending { it.createdAt }

            if (allArcSessions.size < 2) {
                val flowUi = visibleUiBySessionId[session.id] ?: return@forEach
                chronicleItems += ChronicleUiModel.StandaloneFlow(flowUi)
                return@forEach
            }

            val visibleFlows = visibleArcSessions.mapIndexed { index, s ->
                ArcFlowItemUiModel(
                    flow = visibleUiBySessionId[s.id]
                        ?: error("Missing ui model for visible session ${s.id}"),
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

            chronicleItems += ChronicleUiModel.ArcGroup(
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

        return chronicleItems
    }
}
