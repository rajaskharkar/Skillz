package com.kingkharnivore.skillz.model.state


data class ScheduleBeamUiState(
    val tagName: String = "",
    val selectedDateEpochMs: Long? = null,
    val selectedHour: Int? = null,
    val selectedMinute: Int? = null,
    val durationMinutes: Int? = null,
    val customMinutesText: String = "",
    val datePreset: String? = null, // "today" | "tomorrow" | null
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)