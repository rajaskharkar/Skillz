package com.kingkharnivore.skillz.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.SessionEntity
import com.kingkharnivore.skillz.data.model.SessionListItemUiModel
import com.kingkharnivore.skillz.data.model.SkillListUiState
import com.kingkharnivore.skillz.data.model.TagEntity
import com.kingkharnivore.skillz.data.repository.SessionRepository
import com.kingkharnivore.skillz.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val tagRepository: TagRepository
) : ViewModel() {
    // null = "All skills", non-null = filter by that tag/skill
    private val selectedTagId = MutableStateFlow<Long?>(null)

    // stream of sessions depending on selected tag
    @OptIn(ExperimentalCoroutinesApi::class)
    private val sessionsFlow: Flow<List<SessionEntity>> =
        selectedTagId.flatMapLatest { tagId ->
            if (tagId == null) {
                sessionRepository.getAllSessions()
            } else {
                sessionRepository.getSessionsForTag(tagId)
            }
        }

    // all tags = all skills
    private val tagsFlow: Flow<List<TagEntity>> = tagRepository.getAllTags()

    // public UI state
    val uiState: StateFlow<SkillListUiState> =
        combine(sessionsFlow, tagsFlow, selectedTagId) { sessions, tags, currentTagId ->
            val totalDurationMs = sessions.sumOf { it.durationMs }
            SkillListUiState(
                isLoading = false,
                sessions = sessions.toUiModels(tags),
                tags = tags,
                selectedTagId = currentTagId,
                totalDurationMs = totalDurationMs,
                errorMessage = null
            )
        }
            .catch { e ->
                emit(
                    SkillListUiState(
                        isLoading = false,
                        errorMessage = e.message ?: "Something went wrong"
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SkillListUiState()
            )

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

    private fun List<SessionEntity>.toUiModels(tags: List<TagEntity>): List<SessionListItemUiModel> {
        val tagsById = tags.associateBy { it.id }

        return this.map { session ->
            val tagName = tagsById[session.tagId]?.name ?: "Unknown skill"
            SessionListItemUiModel(
                sessionId = session.id,
                title = session.title.ifBlank { "(Untitled session)" },
                description = session.description,
                tagName = tagName,
                durationMs = session.durationMs,
                createdAt = session.createdAt
            )
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