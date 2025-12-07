package com.kingkharnivore.skillz.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.TagEntity
import com.kingkharnivore.skillz.data.repository.SessionRepository
import com.kingkharnivore.skillz.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddSessionViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val tags: StateFlow<List<TagEntity>> =
        tagRepository.getAllTags()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun saveSession(
        title: String,
        description: String,
        tagName: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isSaving.value = true
                _error.value = null

                val tagId = tagRepository.getOrCreateTagId(tagName)
                sessionRepository.addSession(
                    title = title,
                    description = description,
                    tagId = tagId
                    // stopwatch values later
                )

                onDone()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save session"
            } finally {
                _isSaving.value = false
            }
        }
    }
}