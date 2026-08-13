package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object ChronicleOwnerType {
    const val ACTIVE_FLOW = "ACTIVE_FLOW"
    const val SESSION = "SESSION"
    const val PULSE_DRAFT = "PULSE_DRAFT"
    const val PULSE = "PULSE"
}

object ChronicleMomentType {
    const val TEXT = "TEXT"
    const val MEDIA = "MEDIA"
    const val VOICE = "VOICE"
    const val AUDIO = "AUDIO"
}

@Entity(
    tableName = "chronicles",
    indices = [Index(value = ["ownerType", "ownerKey"], unique = true)]
)
data class ChronicleEntity(
    @PrimaryKey val id: String,
    val ownerType: String,
    val ownerKey: String,
    val draftText: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "chronicle_moments",
    foreignKeys = [ForeignKey(
        entity = ChronicleEntity::class,
        parentColumns = ["id"],
        childColumns = ["chronicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("chronicleId"),
        Index(value = ["chronicleId", "position"], unique = true)
    ]
)
data class ChronicleMomentEntity(
    @PrimaryKey val id: String,
    val chronicleId: String,
    val type: String,
    val position: Int,
    val text: String? = null,
    val audioPath: String? = null,
    val displayName: String? = null,
    val mimeType: String? = null,
    val durationMs: Long? = null,
    val transcript: String? = null,
    val transcriptEdited: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
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
    val localPath: String,
    val mimeType: String,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val thumbnailPath: String? = null,
    val createdAt: Long
)
