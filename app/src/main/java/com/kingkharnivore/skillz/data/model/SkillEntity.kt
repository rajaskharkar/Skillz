package com.kingkharnivore.skillz.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val totalTimeMs: Long = 0L
)

data class SkillListUiState(
    val isLoading: Boolean = false,
    val skills: List<SkillEntity> = emptyList(),
    val errorMessage: String? = null
)
