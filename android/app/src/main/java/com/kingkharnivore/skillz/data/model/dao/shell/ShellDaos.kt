package com.kingkharnivore.skillz.data.model.dao.shell

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kingkharnivore.skillz.data.model.entity.shell.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PearlLedgerDao {
    @Query("SELECT COALESCE(SUM(delta), 0) FROM pearl_ledger")
    fun observeBalance(): Flow<Int>

    @Query("SELECT COALESCE(SUM(delta), 0) FROM pearl_ledger")
    suspend fun getBalance(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: PearlLedgerEntity)

    @Query("SELECT COUNT(*) FROM pearl_ledger WHERE sourceType = :sourceType AND sourceId = :sourceId AND reason = :reason")
    suspend fun sourceRewardCount(sourceType: String, sourceId: String?, reason: String): Int

    @Query("SELECT * FROM pearl_ledger ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<PearlLedgerEntity>>
}

@Dao
interface ShellFindInstanceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: UserShellFindInstanceEntity)

    @Update
    suspend fun update(entity: UserShellFindInstanceEntity)

    @Query("SELECT * FROM user_shell_find_instance ORDER BY acquiredAt DESC")
    fun observeAll(): Flow<List<UserShellFindInstanceEntity>>

    @Query("SELECT * FROM user_shell_find_instance ORDER BY acquiredAt DESC")
    suspend fun getAll(): List<UserShellFindInstanceEntity>

    @Query("SELECT * FROM user_shell_find_instance WHERE instanceId = :instanceId LIMIT 1")
    suspend fun getById(instanceId: String): UserShellFindInstanceEntity?

    @Query("SELECT * FROM user_shell_find_instance WHERE findId = :findId LIMIT 1")
    suspend fun getFirstByFindId(findId: String): UserShellFindInstanceEntity?

    @Query("SELECT COUNT(*) FROM user_shell_find_instance WHERE findId = :findId")
    suspend fun countByFindId(findId: String): Int

    @Query("SELECT * FROM user_shell_find_instance WHERE instanceId NOT IN (SELECT instanceId FROM shell_placement) ORDER BY acquiredAt DESC")
    fun observeUnplaced(): Flow<List<UserShellFindInstanceEntity>>

    @Query("UPDATE user_shell_find_instance SET currentUpgradeStageId = :stageId WHERE instanceId = :instanceId")
    suspend fun updateUpgradeStage(instanceId: String, stageId: String)

    @Query("UPDATE user_shell_find_instance SET isArchivedInChest = :archived WHERE instanceId = :instanceId")
    suspend fun updateArchivedState(instanceId: String, archived: Boolean)

    @Query("UPDATE user_shell_find_instance SET isNew = 0")
    suspend fun markAllSeen()

    @Query("UPDATE user_shell_find_instance SET isNew = 0, viewedAt = COALESCE(viewedAt, :viewedAt) WHERE viewedAt IS NULL")
    suspend fun markAllViewed(viewedAt: Long)

    @Query("UPDATE user_shell_find_instance SET isNew = 0, viewedAt = COALESCE(viewedAt, :viewedAt) WHERE viewedAt IS NULL AND findId IN (:findIds)")
    suspend fun markFindIdsViewed(findIds: List<String>, viewedAt: Long)

    @Query("UPDATE user_shell_find_instance SET isNew = 0, viewedAt = COALESCE(viewedAt, :viewedAt) WHERE instanceId = :instanceId")
    suspend fun markViewed(instanceId: String, viewedAt: Long)

    @Query("UPDATE user_shell_find_instance SET isNew = 0 WHERE findId IN (:findIds)")
    suspend fun markFindIdsSeen(findIds: List<String>)

    @Query("UPDATE user_shell_find_instance SET animalLevel = :level WHERE instanceId = :instanceId")
    suspend fun updateAnimalLevel(instanceId: String, level: Int)

    @Query("UPDATE user_shell_find_instance SET creatureStatus = :status, isArchivedInChest = 1 WHERE instanceId = :instanceId")
    suspend fun updateCreatureStatus(instanceId: String, status: String)

    @Query("SELECT * FROM user_shell_find_instance WHERE instanceId IN (:instanceIds)")
    suspend fun getByIds(instanceIds: List<String>): List<UserShellFindInstanceEntity>

    @Query("SELECT * FROM user_shell_find_instance WHERE findId = :findId AND animalLevel = :level AND creatureStatus = :status ORDER BY acquiredAt ASC, instanceId ASC")
    suspend fun getActiveByFindIdAndLevel(findId: String, level: Int, status: String): List<UserShellFindInstanceEntity>
}

@Dao
interface ShellFindStackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserShellFindStackEntity)

    @Query("SELECT * FROM user_shell_find_stack WHERE findId = :findId LIMIT 1")
    suspend fun get(findId: String): UserShellFindStackEntity?

    @Query("SELECT * FROM user_shell_find_stack ORDER BY lastAcquiredAt DESC")
    fun observeAll(): Flow<List<UserShellFindStackEntity>>

    @Query("UPDATE user_shell_find_stack SET isNew = 0")
    suspend fun markAllSeen()

    @Query("UPDATE user_shell_find_stack SET isNew = 0, viewedAt = COALESCE(viewedAt, :viewedAt) WHERE viewedAt IS NULL")
    suspend fun markAllViewed(viewedAt: Long)

    @Query("UPDATE user_shell_find_stack SET isNew = 0, viewedAt = COALESCE(viewedAt, :viewedAt) WHERE findId = :findId")
    suspend fun markViewed(findId: String, viewedAt: Long)
}

@Dao
interface ShellPlacementDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ShellPlacementEntity)

    @Query("DELETE FROM shell_placement WHERE instanceId = :instanceId")
    suspend fun removeByInstance(instanceId: String)

    @Query("DELETE FROM shell_placement WHERE placementId = :placementId")
    suspend fun remove(placementId: String)

    @Query("SELECT * FROM shell_placement WHERE roomId = :roomId ORDER BY placedAt DESC")
    fun observeByRoom(roomId: String): Flow<List<ShellPlacementEntity>>

    @Query("SELECT * FROM shell_placement WHERE roomId = :roomId")
    suspend fun getByRoom(roomId: String): List<ShellPlacementEntity>

    @Query("SELECT * FROM shell_placement WHERE slotId = :slotId AND roomId = :roomId LIMIT 1")
    suspend fun getBySlot(roomId: String, slotId: String): ShellPlacementEntity?

    @Query("SELECT * FROM shell_placement WHERE instanceId = :instanceId LIMIT 1")
    suspend fun getByInstance(instanceId: String): ShellPlacementEntity?
}

@Dao
interface ShellFindUpgradeDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ShellFindUpgradeEntity)

    @Query("SELECT * FROM shell_find_upgrade WHERE instanceId = :instanceId ORDER BY upgradedAt DESC")
    suspend fun getForInstance(instanceId: String): List<ShellFindUpgradeEntity>
}

@Dao
interface UserBadgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserBadgeEntity)

    @Query("SELECT * FROM user_badge WHERE badgeId = :badgeId LIMIT 1")
    suspend fun get(badgeId: String): UserBadgeEntity?

    @Query("SELECT * FROM user_badge ORDER BY firstEarnedAt DESC")
    fun observeEarned(): Flow<List<UserBadgeEntity>>

    @Query("UPDATE user_badge SET isNew = 0")
    suspend fun markAllSeen()

    @Query("UPDATE user_badge SET isNew = 0, viewedAt = COALESCE(viewedAt, :viewedAt) WHERE viewedAt IS NULL")
    suspend fun markAllViewed(viewedAt: Long)

    @Query("UPDATE user_badge SET isNew = 0, viewedAt = COALESCE(viewedAt, :viewedAt) WHERE badgeId = :badgeId")
    suspend fun markViewed(badgeId: String, viewedAt: Long)
}

@Dao
interface UserDiscoveryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: UserDiscoveryEntity)

    @Query("SELECT * FROM user_discovery ORDER BY discoveredAt DESC")
    fun observeAll(): Flow<List<UserDiscoveryEntity>>

    @Query("SELECT * FROM user_discovery WHERE discoveryId = :discoveryId LIMIT 1")
    suspend fun getFirst(discoveryId: String): UserDiscoveryEntity?

    @Query("SELECT COUNT(*) FROM user_discovery")
    suspend fun countAll(): Int

    @Query("UPDATE user_discovery SET isNew = 0")
    suspend fun markAllSeen()

    @Query("UPDATE user_discovery SET isNew = 0, viewedAt = COALESCE(viewedAt, :viewedAt) WHERE viewedAt IS NULL")
    suspend fun markAllViewed(viewedAt: Long)

    @Query("UPDATE user_discovery SET isNew = 0, viewedAt = COALESCE(viewedAt, :viewedAt) WHERE userDiscoveryId = :userDiscoveryId")
    suspend fun markViewed(userDiscoveryId: String, viewedAt: Long)
}

@Dao
interface StillwaterLedgerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: StillwaterLedgerEntity)

    @Query("SELECT COALESCE(SUM(units), 0) FROM stillwater_ledger")
    fun observeTotal(): Flow<Long>

    @Query("SELECT COALESCE(SUM(units), 0) FROM stillwater_ledger WHERE units > 0")
    fun observeLifetimeTotal(): Flow<Long>

    @Query("SELECT COALESCE(SUM(units), 0) FROM stillwater_ledger")
    suspend fun getTotal(): Long

    @Query("SELECT COUNT(*) FROM stillwater_ledger WHERE sourceType = :sourceType AND sourceId = :sourceId")
    suspend fun sourceCount(sourceType: String, sourceId: String?): Int

    @Query("SELECT * FROM stillwater_ledger ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<StillwaterLedgerEntity>>
}

@Dao
interface StillwaterPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StillwaterPreferenceEntity)

    @Query("SELECT * FROM stillwater_preference WHERE id = 1 LIMIT 1")
    fun observe(): Flow<StillwaterPreferenceEntity?>

    @Query("SELECT * FROM stillwater_preference WHERE id = 1 LIMIT 1")
    suspend fun get(): StillwaterPreferenceEntity?
}

@Dao
interface ShellRewardEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<ShellRewardEventEntity>)

    @Query("SELECT * FROM shell_reward_event WHERE arcId = :arcId ORDER BY occurredAt ASC")
    suspend fun getEventsForArc(arcId: Long): List<ShellRewardEventEntity>

    @Query("SELECT * FROM shell_reward_event WHERE sourceSessionId = :sourceSessionId ORDER BY occurredAt ASC")
    suspend fun getEventsForSession(sourceSessionId: Long): List<ShellRewardEventEntity>
}

@Dao
interface UserShellRoomStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserShellRoomStateEntity)

    @Query("SELECT * FROM user_shell_room_state WHERE roomId = :roomId LIMIT 1")
    suspend fun get(roomId: String): UserShellRoomStateEntity?

    @Query("SELECT * FROM user_shell_room_state")
    fun observeAll(): Flow<List<UserShellRoomStateEntity>>
}
