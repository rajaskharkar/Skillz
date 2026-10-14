package com.kingkharnivore.skillz.viewmodel.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.domain.lookout.ObjectiveReminderScheduler
import com.kingkharnivore.skillz.utils.user.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ObjectiveReminderSettingsViewModel @Inject constructor(
    private val prefs: UserPrefs, private val scheduler: ObjectiveReminderScheduler
) : ViewModel() {
    val minutes = prefs.objectiveReminderMinutes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30)
    suspend fun save(minutes: Int) {
        prefs.setObjectiveReminderMinutes(minutes)
        scheduler.rescheduleAll()
    }
}
