package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.FlowListItemUiModel
import com.kingkharnivore.skillz.data.model.entity.FlowListUiState
import com.kingkharnivore.skillz.data.model.entity.Journey7dStatUiModel
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
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

    // null = "All skills", non-null = filter by that tag/skill
    private val selectedTagId = MutableStateFlow<Long?>(null)
    private val showScoreUiFlow = userPrefs.showScoreUi
    private val calmModeFlow = userPrefs.calmMode

    private val period = MutableStateFlow(StoryPeriod.WEEK)
    private val anchorDayStartMs = MutableStateFlow(
        TimeWindowUtils.startOfPeriodMs(System.currentTimeMillis(), StoryPeriod.WEEK)
    )

    private val viewJourneysTagId = MutableStateFlow<Long?>(null)
    private val isViewJourneysOpen = MutableStateFlow(false)

    fun openViewJourneys(tagId: Long) {
        viewJourneysTagId.value = tagId
        isViewJourneysOpen.value = true
    }

    fun closeViewJourneys() {
        isViewJourneysOpen.value = false
        viewJourneysTagId.value = null
    }

    private fun setAnchorClamped(anchorCandidateMs: Long, periodValue: StoryPeriod, nowMs: Long = System.currentTimeMillis()) {
        anchorDayStartMs.value = TimeWindowUtils.clampToFirstAndToday(
            anchorStartMs = TimeWindowUtils.normalizeAnchor(anchorCandidateMs, periodValue),
            period = periodValue,
            firstSessionStartMs = uiState.value.firstSessionStartMs,
            nowMs = nowMs
        )
    }

    // Source flows
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

    /** User chose a tag/skill chip – null means "All". */
    fun selectTag(tagId: Long?) {
        selectedTagId.value = tagId
    }

    // Keep if you still call it somewhere
    fun onTagSelected(tagId: Long?) {
        selectedTagId.value = tagId
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

        // 1) update period
        period.value = newPeriod

        // 2) default anchor to "today" for the selected period:
        // DAY -> today start
        // WEEK -> Monday of this week
        // MONTH -> 1st of this month
        val todayAnchor = TimeWindowUtils.startOfPeriodMs(nowMs, newPeriod)

        // 3) clamp to [first session .. today] (keeps arrows consistent)
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
            // sessionsFlow emits updated list
        }
    }

    /** Optional: clear visible error (simple + deterministic). */
    fun clearError() {
        uiState.value = uiState.value.copy(errorMessage = null)
    }

    // --- Private mapping helpers ---

    private fun List<SessionEntity>.toUiModels(
        tags: List<TagEntity>
    ): List<FlowListItemUiModel> {
        val tagNameById: Map<Long, String> = tags.associate { it.id to it.name }

        return map { session ->
            FlowListItemUiModel(
                sessionId = session.id,
                title = session.title,
                description = session.description,
                tagName = tagNameById[session.tagId].orEmpty(),
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
                selectedTagId,
                period,
                anchorDayStartMs,
                viewJourneysTagId,
                isViewJourneysOpen,
                showScoreUiFlow,
                calmModeFlow
            ) { arr: Array<Any?> ->

                // ✅ strongly-typed unpack (this is the key fix)
                val sessions = arr[0] as List<SessionEntity>
                val tags = arr[1] as List<TagEntity>
                val currentTagId = arr[2] as Long?
                val currentPeriod = arr[3] as StoryPeriod
                val anchorStartMs = arr[4] as Long
                val viewTagId = arr[5] as Long?
                val viewOpen = arr[6] as Boolean
                val showScoreUi = arr[7] as Boolean
                val calmMode = arr[8] as Boolean

                val nowMs = System.currentTimeMillis()

                // --- Tag map ---
                val tagNameById: Map<Long, String> = tags.associate { it.id to it.name }

                // --- First session boundary (used for clamping + nav disable) ---
                val firstSessionStartMs: Long? = sessions.minOfOrNull { it.createdAt }

                // --- Visible tags ---
                val tagUsageCount: Map<Long, Int> = sessions.groupingBy { it.tagId }.eachCount()
                val visibleTags: List<TagEntity> = tags.filter { (tagUsageCount[it.id] ?: 0) > 0 }

                val effectiveTagId: Long? =
                    currentTagId?.takeIf { (tagUsageCount[it] ?: 0) > 0 }

                // 1) Apply tag filter
                val sessionsForTag: List<SessionEntity> =
                    effectiveTagId?.let { tagId -> sessions.filter { it.tagId == tagId } } ?: sessions

                // 2) Normalize anchor + compute window
                val normalizedAnchor =
                    TimeWindowUtils.normalizeAnchor(anchorStartMs, currentPeriod)

                val window =
                    TimeWindowUtils.windowFor(normalizedAnchor, currentPeriod)

                // 3) Apply time window (your “view”)
                val visibleSessions: List<SessionEntity> =
                    sessionsForTag.filter { it.createdAt in window.startMs until window.endMs }

                // --- Is current period? ---
                val todayAnchor =
                    TimeWindowUtils.startOfPeriodMs(nowMs, currentPeriod)

                val isCurrentPeriod: Boolean =
                    normalizedAnchor == todayAnchor

                // ============================================================
                // 7-day summary (ONLY for current period)
                // ============================================================
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
                    } else emptyList()

                // ============================================================
                // Sagas (ONLY for past periods)
                // ============================================================
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

                // ============================================================
                // View Journeys sheet (session list for a chosen journey inside current window)
                // ============================================================
                val viewJourneysSessions: List<FlowListItemUiModel> =
                    if (viewOpen && viewTagId != null) {
                        sessions.asSequence()
                            .filter { it.tagId == viewTagId }
                            .filter { it.createdAt in window.startMs until window.endMs }
                            .sortedByDescending { it.createdAt }
                            .toList()
                            .toUiModels(tags)
                    } else emptyList()

                val viewJourneysTitle: String =
                    if (viewOpen && viewTagId != null) tagNameById[viewTagId].orEmpty() else ""

                // --- Totals from visible sessions ---
                val totalDurationMs = visibleSessions.sumOf { it.durationMs }
                val totalScore = visibleSessions.sumOf { it.scyraPoints }
                val currentSurgeScore = visibleSessions.sumOf { it.surgePoints }

                // Build state
                FlowListUiState(
                    isLoading = false,
                    sessions = visibleSessions.toUiModels(tags),
                    tags = visibleTags.toUiModels(),
                    selectedTagId = effectiveTagId,
                    totalDurationMs = totalDurationMs,
                    errorMessage = null,
                    period = currentPeriod,
                    anchorDayStartMs = normalizedAnchor,
                    currentScore = totalScore,
                    currentSurgeScore = currentSurgeScore,
                    topJourneysLast7d = topJourneysLast7d,
                    firstSessionStartMs = firstSessionStartMs,
                    isCurrentPeriod = isCurrentPeriod,
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
                if (removedTagId != null && selectedTagId.value == removedTagId) {
                    selectedTagId.value = null
                }
            } catch (_: Exception) {
                // optional: set uiState error
            }
        }
    }
}
