package com.kingkharnivore.skillz.ui.viewmodel

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
                initialValue = ""
            )

    fun onTextChanged(newText: String) {
        // simple version: save on every change
        viewModelScope.launch {
            repository.saveNotepadText(newText)
        }
    }
}
