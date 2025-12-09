package com.kingkharnivore.skillz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.SessionListItemUiModel
import com.kingkharnivore.skillz.data.model.entity.SessionListUiState
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.model.entity.isInScoreWindow
import com.kingkharnivore.skillz.data.repository.SessionRepository
import com.kingkharnivore.skillz.data.repository.TagRepository
import com.kingkharnivore.skillz.utils.score.ScoreCalculator
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
class SkillListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val tagRepository: TagRepository
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

    val uiState = MutableStateFlow(SessionListUiState())

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
    ): List<SessionListItemUiModel> {
        val tagNameById: Map<Long, String> = tags.associate { tag ->
            tag.id to tag.name  // adjust field names if needed
        }

        return map { session ->
            SessionListItemUiModel(
                sessionId = session.id,
                title = session.title,
                description = session.description,
                tagName = session.tagId?.let { tagNameById[it] }.orEmpty(),
                durationMs = session.durationMs,
                createdAt = session.createdAt
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
                sessionsFlow,   // Flow<List<SessionEntity>>
                tagsFlow,       // Flow<List<TagEntity>>
                selectedTagId,  // StateFlow<Long?>
                scoreFilter     // StateFlow<ScoreFilter>
            ) { sessions, tags, currentTagId, currentScoreFilter ->

                val nowMs = System.currentTimeMillis()

                // 1) Filter sessions by selected tag (for the LIST)
                val visibleSessions: List<SessionEntity> = currentTagId?.let { tagId ->
                    sessions.filter { it.tagId == tagId }
                } ?: sessions

                // 2) Total duration for *visible* sessions
                val totalDurationMs = visibleSessions.sumOf { it.durationMs }

                // 3) Score uses ALL sessions within the score window
                val sessionsForScore = sessions.filter { session ->
                    session.isInScoreWindow(
                        nowMs = nowMs,
                        filter = currentScoreFilter
                    )
                }
                val totalScore = ScoreCalculator.totalScoreForSessions(sessionsForScore)

                SessionListUiState(
                    isLoading = false,
                    sessions = visibleSessions.toUiModels(tags),
                    tags = tags.toUiModels(),
                    selectedTagId = currentTagId,
                    totalDurationMs = totalDurationMs,
                    errorMessage = null,
                    scoreFilter = currentScoreFilter,
                    currentScore = totalScore
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

                // If the tag that just got emptied/deleted is currently selected,
                // reset to "All" (null)
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