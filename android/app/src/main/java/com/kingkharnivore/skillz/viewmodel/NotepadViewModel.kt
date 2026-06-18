package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.repository.NotepadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotepadViewModel @Inject constructor(
    private val repository: NotepadRepository
) : ViewModel() {

    val notepadText: StateFlow<String> =
        repository.notepadTextFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = repository.DEFAULT_WELCOME_HTML
            )

    // 0 default, 1 cursive, 2 mono
    val notepadDocFont: StateFlow<Int> =
        repository.notepadDocFontFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0
            )

    fun onTextChanged(newText: String) {
        viewModelScope.launch {
            repository.saveNotepadText(newText)
        }
    }

    fun onDocFontChanged(font: Int) {
        viewModelScope.launch {
            repository.saveNotepadDocFont(font)
        }
    }
}