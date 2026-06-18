package com.kingkharnivore.skillz.data.model.entity.health

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kingkharnivore.skillz.data.model.entity.SessionEntity

@Entity(
    tableName = "flow_health_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("status"), Index("expiresAtMs")]
)
data class FlowHealthSnapshotEntity(
    @PrimaryKey val sessionId: Long,
    val healthEnabledAtStart: Boolean,
    val permissionGrantedAtStart: Boolean,
    val status: FlowHealthSyncStatus,
    val steps: Long?,
    val rawMovementPoints: Long,
    val finalMovementScyraContribution: Long,
    val finalMovementPearlContribution: Long,
    val firstCheckedAtMs: Long?,
    val lastCheckedAtMs: Long?,
    val capturedAtMs: Long?,
    val expiresAtMs: Long?,
    val checkCount: Int,
    val flowStartTimeMs: Long,
    val flowEndTimeMs: Long,
    val activeIntervalJson: String?,
    val sourceLabel: String? = "Health Connect",
    @ColumnInfo(defaultValue = "0") val updatedAfterSync: Boolean = false
)
