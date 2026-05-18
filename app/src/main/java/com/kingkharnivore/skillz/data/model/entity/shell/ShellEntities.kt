package com.kingkharnivore.skillz.data.model.entity.shell

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pearl_ledger")
data class PearlLedgerEntity(
    @PrimaryKey val id: String,
    val delta: Int,
    val reason: String,
    val sourceType: String,
    val sourceId: String?,
    val createdAt: Long,
    val note: String?
)

@Entity(
    tableName = "user_shell_find_instance",
    indices = [Index("findId"), Index("sourceType"), Index("sourceId")]
)
data class UserShellFindInstanceEntity(
    @PrimaryKey val instanceId: String,
    val findId: String,
    val acquiredAt: Long,
    val sourceType: String,
    val sourceId: String?,
    val currentUpgradeStageId: String?,
    val customName: String?,
    val isNew: Boolean,
    val isArchivedInChest: Boolean,
    val animalLevel: Int = 1,
    val creatureStatus: String = "ACTIVE",
    val creatureSource: String? = null,
    val flowTimeValueMinutes: Int? = null
)

@Entity(tableName = "user_shell_find_stack")
data class UserShellFindStackEntity(
    @PrimaryKey val findId: String,
    val quantity: Int,
    val firstAcquiredAt: Long,
    val lastAcquiredAt: Long,
    val isNew: Boolean
)

@Entity(
    tableName = "shell_placement",
    indices = [
        Index("roomId"),
        Index("slotId"),
        Index(value = ["roomId", "slotId"], unique = true),
        Index(value = ["instanceId"], unique = true)
    ]
)
data class ShellPlacementEntity(
    @PrimaryKey val placementId: String,
    val roomId: String,
    val slotId: String,
    val instanceId: String,
    val placedAt: Long
)

@Entity(tableName = "shell_find_upgrade", indices = [Index("instanceId")])
data class ShellFindUpgradeEntity(
    @PrimaryKey val upgradeEventId: String,
    val instanceId: String,
    val fromStageId: String?,
    val toStageId: String,
    val pearlCost: Int,
    val upgradedAt: Long
)

@Entity(tableName = "user_badge")
data class UserBadgeEntity(
    @PrimaryKey val badgeId: String,
    val count: Int,
    val firstEarnedAt: Long,
    val lastEarnedAt: Long,
    val isNew: Boolean
)

@Entity(
    tableName = "user_discovery",
    indices = [Index("discoveryId"), Index("sourceType"), Index("sourceId")]
)
data class UserDiscoveryEntity(
    @PrimaryKey val userDiscoveryId: String,
    val discoveryId: String,
    val discoveredAt: Long,
    val sourceType: String,
    val sourceId: String?,
    val grantedFindInstanceId: String?,
    val isNew: Boolean
)

@Entity(
    tableName = "stillwater_ledger",
    indices = [Index("sourceType"), Index("sourceId")]
)
data class StillwaterLedgerEntity(
    @PrimaryKey val id: String,
    val units: Long,
    val sourceType: String,
    val sourceId: String?,
    val createdAt: Long
)

@Entity(tableName = "stillwater_preference")
data class StillwaterPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val perspective: String,
    val updatedAt: Long
)

@Entity(tableName = "user_shell_room_state")
data class UserShellRoomStateEntity(
    @PrimaryKey val roomId: String,
    val firstOpenedAt: Long?,
    val lastOpenedAt: Long?,
    val visualMaturityScore: Int,
    val ambientLifeScore: Int,
    val lastChangedAt: Long?
)
