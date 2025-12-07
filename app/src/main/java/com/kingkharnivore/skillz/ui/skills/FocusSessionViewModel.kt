package com.kingkharnivore.skillz.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.OngoingSessionEntity
import com.kingkharnivore.skillz.data.repository.FocusSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FocusSessionViewModel @Inject constructor(
    private val focusRepo: FocusSessionRepository
) : ViewModel() {

    val ongoingSession: StateFlow<OngoingSessionEntity?> =
        focusRepo.getOngoingSession()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null
            )
}