package com.kingkharnivore.skillz.data.repository.shell

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.shell.*
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.data.model.shell.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShellRepository @Inject constructor(
    private val db: SkillzDatabase,
    private val sessionDao: SessionDao,
    private val pearlLedgerDao: PearlLedgerDao,
    private val findInstanceDao: ShellFindInstanceDao,
    private val findStackDao: ShellFindStackDao,
    private val placementDao: ShellPlacementDao,
    private val upgradeDao: ShellFindUpgradeDao,
    private val badgeDao: UserBadgeDao,
    private val discoveryDao: UserDiscoveryDao,
    private val stillwaterLedgerDao: StillwaterLedgerDao,
    private val stillwaterPreferenceDao: StillwaterPreferenceDao,
    private val roomStateDao: UserShellRoomStateDao
) {
    fun observePearlBalance(): Flow<Int> = pearlLedgerDao.observeBalance()
    fun observeStillwaterTotal(): Flow<Long> = stillwaterLedgerDao.observeTotal()
    fun observeOwnedFinds(): Flow<List<UserShellFindInstanceEntity>> = findInstanceDao.observeAll()
    fun observeStacks(): Flow<List<UserShellFindStackEntity>> = findStackDao.observeAll()
    fun observePlacements(roomId: ShellRoomId): Flow<List<ShellPlacementEntity>> = placementDao.observeByRoom(roomId.name)
    fun observeEarnedBadges(): Flow<List<UserBadgeEntity>> = badgeDao.observeEarned()
    fun observeDiscoveries(): Flow<List<UserDiscoveryEntity>> = discoveryDao.observeAll()
    fun observeStillwaterPreference(): Flow<StillwaterPreferenceEntity?> = stillwaterPreferenceDao.observe()

    suspend fun getPearlBalance(): Int = pearlLedgerDao.getBalance()
    suspend fun getStillwaterTotal(): Long = stillwaterLedgerDao.getTotal()

    suspend fun markRoomOpened(roomId: ShellRoomId) = db.withTransaction {
        val now = System.currentTimeMillis()
        val current = roomStateDao.get(roomId.name)
        roomStateDao.upsert(
            current?.copy(lastOpenedAt = now) ?: UserShellRoomStateEntity(
                roomId = roomId.name,
                firstOpenedAt = now,
                lastOpenedAt = now,
                visualMaturityScore = 0,
                ambientLifeScore = 0,
                lastChangedAt = null
            )
        )
    }

    suspend fun addPearls(delta: Int, reason: String, sourceType: String, sourceId: String?, note: String? = null): Boolean = db.withTransaction {
        if (delta == 0 || pearlLedgerDao.sourceRewardCount(sourceType, sourceId, reason) > 0) return@withTransaction false
        pearlLedgerDao.insert(PearlLedgerEntity(UUID.randomUUID().toString(), delta, reason, sourceType, sourceId, System.currentTimeMillis(), note))
        true
    }

    suspend fun addStillwater(units: Long, sourceType: String, sourceId: String?): Boolean = db.withTransaction {
        if (units <= 0 || stillwaterLedgerDao.sourceCount(sourceType, sourceId) > 0) return@withTransaction false
        stillwaterLedgerDao.insert(StillwaterLedgerEntity(UUID.randomUUID().toString(), units, sourceType, sourceId, System.currentTimeMillis()))
        true
    }

    suspend fun incrementBadge(badgeId: String, by: Int = 1) {
        val now = System.currentTimeMillis()
        val current = badgeDao.get(badgeId)
        badgeDao.upsert(
            current?.copy(count = current.count + by, lastEarnedAt = now, isNew = true)
                ?: UserBadgeEntity(badgeId, by, now, now, true)
        )
    }

    suspend fun grantFindOnce(findId: String, sourceType: String, sourceId: String?): UserShellFindInstanceEntity? {
        if (findInstanceDao.countByFindId(findId) > 0) return null
        val now = System.currentTimeMillis()
        val firstStage = ShellContentCatalog.upgradesFor(findId).firstOrNull()?.upgradeStageId
        val entity = UserShellFindInstanceEntity(
            instanceId = UUID.randomUUID().toString(),
            findId = findId,
            acquiredAt = now,
            sourceType = sourceType,
            sourceId = sourceId,
            currentUpgradeStageId = firstStage,
            customName = null,
            isNew = true,
            isArchivedInChest = true
        )
        findInstanceDao.insert(entity)
        return entity
    }

    suspend fun addStack(findId: String, quantity: Int = 1) {
        val now = System.currentTimeMillis()
        val current = findStackDao.get(findId)
        findStackDao.upsert(
            current?.copy(quantity = current.quantity + quantity, lastAcquiredAt = now, isNew = true)
                ?: UserShellFindStackEntity(findId, quantity, now, now, true)
        )
    }

    suspend fun grantDiscoveryOnce(discoveryId: String, sourceType: String, sourceId: String?): UserDiscoveryEntity? = db.withTransaction {
        val definition = ShellContentCatalog.discovery(discoveryId) ?: return@withTransaction null
        if (definition.oncePerUser && discoveryDao.getFirst(discoveryId) != null) return@withTransaction null
        val find = definition.grantsFindId?.let { findId ->
            val def = ShellContentCatalog.find(findId)
            when {
                def?.stackable == true -> { addStack(findId); null }
                def != null -> grantFindOnce(findId, sourceType, sourceId)
                else -> null
            }
        }
        val entity = UserDiscoveryEntity(UUID.randomUUID().toString(), discoveryId, System.currentTimeMillis(), sourceType, sourceId, find?.instanceId, true)
        discoveryDao.insert(entity)
        incrementBadge("badge_discovery")
        entity
    }

    suspend fun placeInstance(instanceId: String, roomId: ShellRoomId, slotId: String) = db.withTransaction {
        val instance = findInstanceDao.getById(instanceId) ?: error("Shell Find not found")
        val find = ShellContentCatalog.find(instance.findId) ?: error("Shell Find definition missing")
        val slot = ShellContentCatalog.focusSlots.firstOrNull { it.roomId == roomId && it.slotId == slotId } ?: error("Invalid slot.")
        require(find.placeable) { "This Shell Find rests in the Shell Chest." }
        require(slot.slotType in find.acceptedSlotTypes && find.category in slot.acceptsCategories) { "Invalid slot for this Shell Find." }
        require(placementDao.getBySlot(roomId.name, slotId) == null) { "This space already holds something." }
        placementDao.removeByInstance(instanceId)
        placementDao.insert(ShellPlacementEntity(UUID.randomUUID().toString(), roomId.name, slotId, instanceId, System.currentTimeMillis()))
    }

    suspend fun removePlacement(instanceId: String) = placementDao.removeByInstance(instanceId)

    suspend fun upgradeInstance(instanceId: String) = db.withTransaction {
        val instance = findInstanceDao.getById(instanceId) ?: error("Shell Find not found")
        val find = ShellContentCatalog.find(instance.findId) ?: error("Shell Find definition missing")
        require(find.upgradeable) { "Object already complete." }
        val next = ShellContentCatalog.nextUpgrade(find.findId, instance.currentUpgradeStageId) ?: error("Object already complete.")
        val balance = pearlLedgerDao.getBalance()
        require(balance >= next.pearlCost) { "Insufficient Pearls." }
        pearlLedgerDao.insert(PearlLedgerEntity(UUID.randomUUID().toString(), -next.pearlCost, "shape_find", "shell_find", instanceId, System.currentTimeMillis(), null))
        upgradeDao.insert(ShellFindUpgradeEntity(UUID.randomUUID().toString(), instanceId, instance.currentUpgradeStageId, next.upgradeStageId, next.pearlCost, System.currentTimeMillis()))
        findInstanceDao.updateUpgradeStage(instanceId, next.upgradeStageId)
    }

    suspend fun updateStillwaterPerspective(perspective: StillwaterPerspective) {
        stillwaterPreferenceDao.upsert(StillwaterPreferenceEntity(perspective = perspective.name, updatedAt = System.currentTimeMillis()))
    }

    suspend fun regularFlowCount(): Int = sessionDao.getRegularSessionCount()
    suspend fun lastRegularFlowBefore(endTime: Long): Long? = sessionDao.getLastRegularSessionEndBefore(endTime)
}
