package com.kingkharnivore.skillz.model.ui

data class PulseListItemUiModel(
    val pulseId: Long,
    val title: String,
    val description: String,
    val chronicleTexts: List<String> = emptyList(),
    val tagId: Long?,
    val tagName: String,
    val createdAt: Long,
    val parentSessionId: Long?,
    val arcId: Long?
)
