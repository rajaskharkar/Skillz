package com.kingkharnivore.skillz.data.model.entity.anchor

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anchored_apps")
data class AnchoredAppEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val iconCacheKey: String? = null,
    val addedAt: Long,
    val lastSeenAt: Long? = null
)
