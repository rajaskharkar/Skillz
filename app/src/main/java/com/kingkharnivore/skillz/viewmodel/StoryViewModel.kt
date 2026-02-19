package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.FlowListItemUiModel
import com.kingkharnivore.skillz.data.model.entity.FlowListUiState
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.model.entity.isInScoreWindow
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.utils.score.ScoreFilter
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

    private val scoreFilter = MutableStateFlow(ScoreFilter.LAST_7_DAYS)

    // stream of sessions depending on selected tag
    // Source flows
    private val sessionsFlow: Flow<List<SessionEntity>> =
        sessionRepository.getAllSessions()         // Flow<List<SessionEntity>>

    private val tagsFlow: Flow<List<TagEntity>> =
        tagRepository.getAllTags()                 // Flow<List<TagEntity>> (skills)

    val uiState = MutableStateFlow(FlowListUiState())

    init {
        observeSessions()
    }

    fun onTagSelected(tagId: Long?) {
        selectedTagId.value = tagId
    }

    // edit description fn you already have:
    fun updateSessionDescription(sessionId: Long, description: String) {
        viewModelScope.launch {
            sessionRepository.updateSessionDescription(sessionId, description)
            // sessionsFlow emits updated list, observeSessions() will update uiState
        }
    }

    fun onScoreFilterSelected(filter: ScoreFilter) {
        scoreFilter.value = filter
    }

    /** User chose a tag/skill chip – null means "All". */
    fun selectTag(tagId: Long?) {
        selectedTagId.value = tagId
    }

    /** If you still want to surface errors (optional). */
    fun clearError() {
        viewModelScope.launch {
            // just emit a copy with errorMessage = null
            val current = uiState.value
            selectedTagId.value = current.selectedTagId // no-op for sessionsFlow, but keeps pattern
        }
    }

    // --- Private mapping helpers ---

    private fun List<SessionEntity>.toUiModels(
        tags: List<TagEntity>
    ): List<FlowListItemUiModel> {
        val tagNameById: Map<Long, String> = tags.associate { tag ->
            tag.id to tag.name  // adjust field names if needed
        }

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

    private fun List<TagEntity>.toUiModels(): List<TagUiModel> {
        return map { tag ->
            TagUiModel(
                id = tag.id,
                name = tag.name
            )
        }
    }

    private fun observeSessions() {
        viewModelScope.launch {
            // show loading till first emission
            uiState.value = uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            combine(
                sessionsFlow,
                tagsFlow,
                selectedTagId,
                scoreFilter
            ) { sessions, tags, currentTagId, currentScoreFilter ->
                val nowMs = System.currentTimeMillis()

                val tagUsageCount: Map<Long, Int> = sessions
                    .groupingBy { it.tagId }
                    .eachCount()
                val visibleTags: List<TagEntity> = tags.filter { tag ->
                    (tagUsageCount[tag.id] ?: 0) > 0
                }
                val effectiveTagId: Long? = currentTagId?.takeIf { tagId ->
                    (tagUsageCount[tagId] ?: 0) > 0
                }

                // 1) Filter by selected tag (using effectiveTagId)
                val sessionsForTag: List<SessionEntity> = effectiveTagId?.let { tagId ->
                    sessions.filter { it.tagId == tagId }
                } ?: sessions
                val availableFilters: Set<ScoreFilter> =
                    ScoreFilter.entries.filterTo(mutableSetOf()) { filter ->
                        when (filter) {
                            ScoreFilter.ALL_TIME -> sessionsForTag.isNotEmpty()
                            else -> sessionsForTag.any { it.isInScoreWindow(nowMs = nowMs, filter = filter) }
                        }
                    }
                val effectiveScoreFilter =
                    if (availableFilters.contains(currentScoreFilter)) currentScoreFilter
                    else availableFilters.firstOrNull() ?: ScoreFilter.ALL_TIME

                // 2) Apply score window filter ON TOP of tag filter
                val visibleSessions: List<SessionEntity> = when (effectiveScoreFilter) {
                    ScoreFilter.ALL_TIME -> sessionsForTag
                    else -> sessionsForTag.filter { it.isInScoreWindow(nowMs = nowMs, filter = effectiveScoreFilter) }
                }

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
                    scoreFilter = effectiveScoreFilter,
                    currentScore = totalScore,
                    availableScoreFilters = availableFilters,
                    currentSurgeScore = currentSurgeScore,
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
                // Flows from Room will take care of updating sessions + tags
            } catch (e: Exception) {
                // optionally set an error in uiState
            }
        }
    }
}