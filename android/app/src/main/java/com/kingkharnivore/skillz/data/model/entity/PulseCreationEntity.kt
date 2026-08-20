package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Durable idempotency receipt for one logical Pulse creation screen. */
@Entity(
    tableName = "pulse_creations",
    foreignKeys = [ForeignKey(
        entity = PulseEntity::class,
        parentColumns = ["id"],
        childColumns = ["pulseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["pulseId"], unique = true)]
)
data class PulseCreationEntity(
    @PrimaryKey val creationKey: String,
    val pulseId: Long,
    val createdAt: Long
)
