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
    private val tagRepository: JourneyRepository
) : ViewModel() {

    // null = "All skills", non-null = filter by that tag/skill
    private val selectedTagId = MutableStateFlow<Long?>(null)

    private val period = MutableStateFlow(StoryPeriod.DAY)
    private val anchorDayStartMs = MutableStateFlow(TimeWindowUtils.startOfTodayMs())

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
        period.value = newPeriod

        val snapped = TimeWindowUtils.normalizeAnchor(anchorDayStartMs.value, newPeriod)
        setClampedAnchor(period = newPeriod, anchorStartMs = snapped)
    }

    fun goPrev() = navigatePeriod(dir = -1)

    fun goNext() = navigatePeriod(dir = +1)

    fun goToday() {
        val p = period.value
        val todayStart = TimeWindowUtils.startOfTodayMs()
        val snappedToday = TimeWindowUtils.normalizeAnchor(todayStart, p)

        setClampedAnchor(period = p, anchorStartMs = snappedToday)
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
                beamBonusPoints = session.beamBonusPoints
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
                anchorDayStartMs
            ) { sessions, tags, currentTagId, currentPeriod, anchorStartMs ->

                val nowMs = System.currentTimeMillis()

                // Map tag names
                val tagNameById: Map<Long, String> = tags.associate { it.id to it.name }

                // ✅ FIRST SESSION should be based on time the session actually happened
                // Use startTime (not createdAt)
                val firstSessionStartMs: Long? = sessions.minOfOrNull { it.startTime }

                // ✅ Normalize & clamp anchor to valid range (first session .. today) for this period
                val normalizedAnchor = TimeWindowUtils.normalizeAnchor(anchorStartMs, currentPeriod)
                val clampedAnchor = TimeWindowUtils.clampToFirstAndToday(
                    anchorStartMs = normalizedAnchor,
                    period = currentPeriod,
                    firstSessionStartMs = firstSessionStartMs,
                    nowMs = nowMs
                )

                // Visible tags are ones used at least once
                val tagUsageCount: Map<Long, Int> = sessions.groupingBy { it.tagId }.eachCount()
                val visibleTags: List<TagEntity> = tags.filter { (tagUsageCount[it.id] ?: 0) > 0 }

                val effectiveTagId: Long? = currentTagId?.takeIf { (tagUsageCount[it] ?: 0) > 0 }

                // 1) Apply tag filter (or all)
                val sessionsForTag: List<SessionEntity> = effectiveTagId?.let { tagId ->
                    sessions.filter { it.tagId == tagId }
                } ?: sessions

                // 2) ✅ Apply period window filter using OVERLAP (startTime/endTime), NOT createdAt
                val window = TimeWindowUtils.windowFor(clampedAnchor, currentPeriod)

                // session overlaps [window.start, window.end) if:
                // endTime > start AND startTime < end
                val visibleSessions: List<SessionEntity> = sessionsForTag.filter { s ->
                    s.endTime > window.startMs && s.startTime < window.endMs
                }

                // 3) ✅ Empty-day motivator analytics (ALWAYS computed from ALL sessions, last 7 days)
                val sevenDaysMs = 7L * 24L * 60L * 60L * 1000L
                val last7dStart = nowMs - sevenDaysMs

                val topJourneysLast7d = sessions
                    .asSequence()
                    // count sessions that ended in the last 7 days (or you can use startTime)
                    .filter { it.endTime >= last7dStart }
                    .groupBy { it.tagId }
                    .map { (tagId, ss) ->
                        val score = ss.sumOf { it.scyraPoints }
                        val dur = ss.sumOf { it.durationMs }
                        val name = tagNameById[tagId].orEmpty()
                        Journey7dStatUiModel(
                            tagId = tagId,
                            tagName = name,
                            totalScore = score,
                            totalDurationMs = dur,
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

                val currentSurgeScore = visibleSessions.sumOf { it.surgePoints }
                val totalDurationMs = visibleSessions.sumOf { it.durationMs }
                val totalScore = visibleSessions.sumOf { it.scyraPoints }

                FlowListUiState(
                    isLoading = false,
                    sessions = visibleSessions.toUiModels(tags),
                    tags = visibleTags.toUiModels(),
                    selectedTagId = effectiveTagId,
                    totalDurationMs = totalDurationMs,
                    errorMessage = null,
                    period = currentPeriod,
                    anchorDayStartMs = clampedAnchor, // ✅ IMPORTANT: use clamped anchor
                    currentScore = totalScore,
                    currentSurgeScore = currentSurgeScore,
                    topJourneysLast7d = topJourneysLast7d,
                    firstSessionStartMs = firstSessionStartMs
                )
            }.catch { e ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Something went wrong"
                )
            }.collect { newState ->
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
