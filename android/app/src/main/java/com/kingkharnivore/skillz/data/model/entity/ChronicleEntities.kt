package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A stable owner key permits capture before a Flow or Pulse receives its database id. */
@Entity(
    tableName = "chronicles",
    indices = [Index(value = ["ownerType", "ownerKey"], unique = true)]
)
data class ChronicleEntity(
    @PrimaryKey val id: String,
    val ownerType: String,
    val ownerKey: String,
    val draft: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chronicle_moments",
    foreignKeys = [ForeignKey(
        entity = ChronicleEntity::class,
        parentColumns = ["id"],
        childColumns = ["chronicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("chronicleId"), Index(value = ["chronicleId", "position"], unique = true)]
)
data class ChronicleMomentEntity(
    @PrimaryKey val id: String,
    val chronicleId: String,
    val type: String,
    val position: Int,
    val text: String? = null,
    val fileName: String? = null,
    val localFileName: String? = null,
    val mimeType: String? = null,
    val durationMs: Long? = null,
    val transcript: String? = null,
    val transcriptEdited: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chronicle_media_items",
    foreignKeys = [ForeignKey(
        entity = ChronicleMomentEntity::class,
        parentColumns = ["id"],
        childColumns = ["momentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("momentId"), Index(value = ["momentId", "position"], unique = true)]
)
data class ChronicleMediaItemEntity(
    @PrimaryKey val id: String,
    val momentId: String,
    val position: Int,
    /** Relative name below files/chronicle; raw device paths are never persisted. */
    val localFileName: String,
    val mimeType: String,
    val durationMs: Long? = null,
    val thumbnailFileName: String? = null
)
