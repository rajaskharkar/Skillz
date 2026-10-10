package com.kingkharnivore.skillz.data.model.entity.shell

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pebble_ledger")
data class PebbleLedgerEntity(@PrimaryKey val id: String, val delta: Int, val sourceId: String, val createdAt: Long)

/** Outbox created with the completed session, never populated for historical sessions. */
@Entity(tableName = "session_shell_reward")
data class SessionShellRewardEntity(@PrimaryKey val sessionId: Long, val completed: Boolean = false, val pearls: Int = 0, val pebbles: Int = 0, val queuedAt: Long)
