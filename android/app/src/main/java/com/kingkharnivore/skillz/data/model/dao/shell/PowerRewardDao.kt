package com.kingkharnivore.skillz.data.model.dao.shell

import androidx.room.*
import com.kingkharnivore.skillz.data.model.entity.shell.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PowerRewardDao {
    @Query("SELECT COALESCE(SUM(delta), 0) FROM pebble_ledger") fun observeBalance(): Flow<Int>
    @Query("SELECT COALESCE(SUM(delta), 0) FROM pebble_ledger") suspend fun balance(): Int
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(entry: PebbleLedgerEntity)
    @Query("SELECT * FROM pebble_ledger WHERE id = :id") suspend fun entry(id: String): PebbleLedgerEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun enqueue(entry: SessionShellRewardEntity)
    @Update suspend fun update(entry: SessionShellRewardEntity)
    @Query("SELECT * FROM session_shell_reward WHERE sessionId = :id") suspend fun reward(id: Long): SessionShellRewardEntity?
    @Query("SELECT sessionId FROM session_shell_reward WHERE completed = 0 AND queuedAt < :before ORDER BY queuedAt LIMIT 32") suspend fun pending(before: Long): List<Long>
    @Query("UPDATE session_shell_reward SET queuedAt = :retryAt WHERE sessionId = :id AND completed = 0") suspend fun defer(id: Long, retryAt: Long)
    @Query("DELETE FROM session_shell_reward WHERE sessionId = :id AND completed = 0") suspend fun discardPending(id: Long)
}
