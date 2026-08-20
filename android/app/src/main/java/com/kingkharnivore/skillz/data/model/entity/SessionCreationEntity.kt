package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Durable receipt preventing a restored Flow completion from inserting twice. */
@Entity(
    tableName = "session_creations",
    foreignKeys = [ForeignKey(entity = SessionEntity::class, parentColumns = ["id"],
        childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["sessionId"], unique = true)]
)
data class SessionCreationEntity(
    @PrimaryKey val flowInstanceId: String,
    val sessionId: Long,
    val createdAt: Long
)
