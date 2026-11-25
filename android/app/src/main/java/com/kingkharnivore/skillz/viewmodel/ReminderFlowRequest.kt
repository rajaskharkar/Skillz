package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.SavedStateHandle

/** Saved with the Flow destination, including while Chronicle/completion dialogs are open. */
internal class ReminderFlowRequest(private val saved: SavedStateHandle) {
    val activityId = saved.getStateFlow<Long?>("reminder.activityId", null)
    val accepted = saved.getStateFlow("reminder.accepted", false)
    var sourceFlowId: String?
        get() = saved["reminder.sourceFlowId"]
        private set(value) { saved["reminder.sourceFlowId"] = value }

    fun request(id: Long): Boolean {
        // A second tap must not replace a destination whose current flow is being saved.
        if (accepted.value || activityId.value == id) return false
        saved["reminder.activityId"] = id
        return true
    }

    fun accept(source: String) {
        sourceFlowId = source
        saved["reminder.accepted"] = true
    }

    fun clear() {
        saved["reminder.activityId"] = null
        saved["reminder.accepted"] = false
        sourceFlowId = null
    }
}

data class ReminderFlowPrompt(
    val activityId: Long,
    val sourceFlowId: String,
    val title: String,
    val kind: String,
    val currentTitle: String,
    val hasActiveFlow: Boolean
)
