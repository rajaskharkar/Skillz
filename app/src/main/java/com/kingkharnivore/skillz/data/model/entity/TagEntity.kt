package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kingkharnivore.skillz.ui.viewmodel.TagUiModel
import com.kingkharnivore.skillz.utils.score.ScoreFilter

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
    val durationMs: Long,
    val createdAt: Long
)

data class SkillListUiState(
    val isLoading: Boolean = true,
    val sessions: List<SessionListItemUiModel> = emptyList(),
    val tags: List<TagUiModel> = emptyList(),   // available Skills (tags)
    val selectedTagId: Long? = null,          // null = "All"
    val totalDurationMs: Long = 0L,
    val errorMessage: String? = null,
    val scoreFilter: ScoreFilter = ScoreFilter.LAST_7_DAYS,
    val currentScore: Int = 0
)

