package com.kingkharnivore.skillz.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class SessionListItemUiModel(
    val sessionId: Long,
    val title: String,
    val description: String,
    val tagName: String,
    val durationMinutes: Long,
    val createdAt: Long
)

data class SkillListUiState(
    val isLoading: Boolean = true,
    val sessions: List<SessionListItemUiModel> = emptyList(),
    val tags: List<TagEntity> = emptyList(),   // available Skills (tags)
    val selectedTagId: Long? = null,          // null = "All"
    val errorMessage: String? = null
)

