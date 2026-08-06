package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arc_metadata")
data class ArcMetadataEntity(
    @PrimaryKey val arcId: Long,
    val title: String?,
    val summary: String?,
    val outcome: String?,
    val highlight: String?,
    val nextStep: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
