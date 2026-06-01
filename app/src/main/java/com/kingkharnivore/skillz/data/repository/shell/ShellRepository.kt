package com.kingkharnivore.skillz.data.repository.shell

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.shell.*
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.data.model.shell.*
import com.kingkharnivore.skillz.domain.shell.CreatureCatalog
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.domain.shell.CreatureSourceType
import com.kingkharnivore.skillz.domain.shell.CreatureStatus
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
    private val roomStateDao: UserShellRoomStateDao,
    private val objectiveCompletionDao: ObjectiveCompletionDao
) {
    fun observePearlBalance(): Flow<Int> = pearlLedgerDao.observeBalance()
    fun observeStillwaterTotal(): Flow<Long> = stillwaterLedgerDao.observeTotal()
    fun observeOwnedFinds(): Flow<List<UserShellFindInstanceEntity>> = findInstanceDao.observeAll()
    fun observeStacks(): Flow<List<UserShellFindStackEntity>> = findStackDao.observeAll()
    fun observePlacements(roomId: ShellRoomId): Flow<List<ShellPlacementEntity>> = placementDao.observeByRoom(roomId.name)
    fun observeEarnedBadges(): Flow<List<UserBadgeEntity>> = badgeDao.observeEarned()
    fun observeDiscoveries(): Flow<List<UserDiscoveryEntity>> = discoveryDao.observeAll()
    fun observeObjectiveCompletions(): Flow<List<ObjectiveCompletionEntity>> = objectiveCompletionDao.observeCompletions()
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

    suspend fun incrementBadge(badgeId: String, by: Int = 1): Int {
        val now = System.currentTimeMillis()
        val current = badgeDao.get(badgeId)
        val newCount = (current?.count ?: 0) + by
        badgeDao.upsert(
            current?.copy(count = newCount, lastEarnedAt = now, isNew = true)
                ?: UserBadgeEntity(badgeId, by, now, now, true)
        )
        return newCount
    }

    suspend fun grantFindCopy(findId: String, sourceType: String, sourceId: String?): UserShellFindInstanceEntity {
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
            isArchivedInChest = true,
            animalLevel = 1,
            creatureStatus = CreatureStatus.ACTIVE,
            creatureSource = CreatureCatalog.get(findId)?.sourceType?.name,
            flowTimeValueMinutes = CreatureCatalog.get(findId)?.flowTimeValueMinutes
                ?: CreatureCatalog.get(findId)?.requirementMinutes
        )
        findInstanceDao.insert(entity)
        return entity
    }

    suspend fun grantFindOnce(findId: String, sourceType: String, sourceId: String?): UserShellFindInstanceEntity? {
        if (findInstanceDao.countByFindId(findId) > 0) return null
        return grantFindCopy(findId, sourceType, sourceId)
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
        val instance = findInstanceDao.getById(instanceId) ?: error("Shell reward not found")
        val find = ShellContentCatalog.find(instance.findId) ?: error("Shell reward definition missing")
        val slot = ShellContentCatalog.focusSlots.firstOrNull { it.roomId == roomId && it.slotId == slotId } ?: error("Invalid slot.")
        require(find.placeable) { "This reward rests in the Shell Chest." }
        require(find.kind != ShellRewardKind.ANIMAL || instance.creatureStatus == CreatureStatus.ACTIVE) { "This creature is no longer swimming in The Blue." }
        require(ShellContentCatalog.isCompatibleWithSlot(slot, find)) { "Invalid nook for this reward." }
        val currentInSlot = placementDao.getBySlot(roomId.name, slotId)
        if (currentInSlot?.instanceId == instanceId) return@withTransaction
        currentInSlot?.let {
            placementDao.removeByInstance(it.instanceId)
            findInstanceDao.updateArchivedState(it.instanceId, true)
        }
        placementDao.removeByInstance(instanceId)
        findInstanceDao.updateArchivedState(instanceId, false)
        placementDao.insert(ShellPlacementEntity(UUID.randomUUID().toString(), roomId.name, slotId, instanceId, System.currentTimeMillis()))
    }

    suspend fun removePlacement(instanceId: String) = db.withTransaction {
        placementDao.removeByInstance(instanceId)
        findInstanceDao.updateArchivedState(instanceId, true)
    }

    suspend fun markAllNotificationsSeen() = db.withTransaction {
        findInstanceDao.markAllSeen()
        findStackDao.markAllSeen()
        badgeDao.markAllSeen()
        discoveryDao.markAllSeen()
    }

    suspend fun markTheBlueAnimalsSeen() = db.withTransaction {
        val animalFindIds = ShellContentCatalog.animalFindIds.toList()
        if (animalFindIds.isNotEmpty()) {
            findInstanceDao.markFindIdsSeen(animalFindIds)
        }
    }

    suspend fun invitePearlObject(findId: String, roomId: ShellRoomId, slotId: String) = db.withTransaction {
        val def = ShellContentCatalog.find(findId) ?: error("Shell object definition missing")
        val cost = def.pearlCost ?: error("This object cannot be shaped with Pearls.")
        val slot = ShellContentCatalog.focusSlots.firstOrNull { it.roomId == roomId && it.slotId == slotId } ?: error("Invalid slot.")
        require(def.isPearlObject) { "This object cannot be invited with Pearls." }
        require(def.placeable) { "This object cannot rest here." }
        require(ShellContentCatalog.isCompatibleWithSlot(slot, def)) { "Invalid nook for this object." }
        require(placementDao.getBySlot(roomId.name, slotId) == null) { "Choose something to swap." }
        val balance = pearlLedgerDao.getBalance()
        require(balance >= cost) { "Insufficient Pearls." }
        val now = System.currentTimeMillis()
        val firstStage = ShellContentCatalog.upgradesFor(findId).firstOrNull()?.upgradeStageId
        val instance = UserShellFindInstanceEntity(
            instanceId = UUID.randomUUID().toString(),
            findId = findId,
            acquiredAt = now,
            sourceType = "pearl_basin",
            sourceId = slotId,
            currentUpgradeStageId = firstStage,
            customName = null,
            isNew = true,
            isArchivedInChest = false,
            animalLevel = 1,
            creatureStatus = CreatureStatus.ACTIVE,
            creatureSource = CreatureCatalog.get(findId)?.sourceType?.name,
            flowTimeValueMinutes = CreatureCatalog.get(findId)?.flowTimeValueMinutes
                ?: CreatureCatalog.get(findId)?.requirementMinutes
        )
        pearlLedgerDao.insert(PearlLedgerEntity(UUID.randomUUID().toString(), -cost, "invite_object", "shell_reward", instance.instanceId, now, null))
        findInstanceDao.insert(instance)
        placementDao.insert(ShellPlacementEntity(UUID.randomUUID().toString(), roomId.name, slotId, instance.instanceId, now))
    }


    suspend fun invitePearlObjectToChest(findId: String) = db.withTransaction {
        val def = ShellContentCatalog.find(findId) ?: error("Shell object definition missing")
        val cost = def.pearlCost ?: error("This object cannot be shaped with Pearls.")
        require(def.isPearlObject) { "This object cannot be invited with Pearls." }
        val balance = pearlLedgerDao.getBalance()
        require(balance >= cost) { "Insufficient Pearls." }
        val now = System.currentTimeMillis()
        val firstStage = ShellContentCatalog.upgradesFor(findId).firstOrNull()?.upgradeStageId
        val instance = UserShellFindInstanceEntity(
            instanceId = UUID.randomUUID().toString(),
            findId = findId,
            acquiredAt = now,
            sourceType = "pearl_basin",
            sourceId = null,
            currentUpgradeStageId = firstStage,
            customName = null,
            isNew = true,
            isArchivedInChest = true,
            animalLevel = 1,
            creatureStatus = CreatureStatus.ACTIVE,
            creatureSource = CreatureCatalog.get(findId)?.sourceType?.name,
            flowTimeValueMinutes = CreatureCatalog.get(findId)?.flowTimeValueMinutes
                ?: CreatureCatalog.get(findId)?.requirementMinutes
        )
        pearlLedgerDao.insert(PearlLedgerEntity(UUID.randomUUID().toString(), -cost, "invite_object", "shell_reward", instance.instanceId, now, null))
        findInstanceDao.insert(instance)
    }

    suspend fun upgradeInstance(instanceId: String) = db.withTransaction {
        val instance = findInstanceDao.getById(instanceId) ?: error("Shell reward not found")
        val find = ShellContentCatalog.find(instance.findId) ?: error("Shell reward definition missing")
        if (find.kind == ShellRewardKind.ANIMAL) {
            require(instance.creatureStatus == CreatureStatus.ACTIVE) { "Only active creatures can grow." }
            val currentLevel = instance.animalLevel.coerceAtLeast(1)
            val cost = CreatureEconomy.growthCostPearls(instance.findId, currentLevel)
            require(pearlLedgerDao.getBalance() >= cost) { "Insufficient Pearls." }
            pearlLedgerDao.insert(PearlLedgerEntity(UUID.randomUUID().toString(), -cost, "grow_creature", "shell_reward", instanceId, System.currentTimeMillis(), null))
            findInstanceDao.updateAnimalLevel(instanceId, currentLevel + 1)
            return@withTransaction
        }
        require(find.upgradeable) { "This object is resting in its current form." }
        val next = ShellContentCatalog.nextUpgrade(find.findId, instance.currentUpgradeStageId) ?: error("This object is resting in its current form.")
        val balance = pearlLedgerDao.getBalance()
        require(balance >= next.pearlCost) { "Insufficient Pearls." }
        pearlLedgerDao.insert(PearlLedgerEntity(UUID.randomUUID().toString(), -next.pearlCost, "shape_find", "shell_reward", instanceId, System.currentTimeMillis(), null))
        upgradeDao.insert(ShellFindUpgradeEntity(UUID.randomUUID().toString(), instanceId, instance.currentUpgradeStageId, next.upgradeStageId, next.pearlCost, System.currentTimeMillis()))
        findInstanceDao.updateUpgradeStage(instanceId, next.upgradeStageId)
    }


    suspend fun growCreature(instanceId: String) = db.withTransaction {
        val instance = findInstanceDao.getById(instanceId) ?: error("Creature not found")
        require(ShellContentCatalog.find(instance.findId)?.kind == ShellRewardKind.ANIMAL) { "Only animals can grow with Pearls." }
        require(instance.creatureStatus == CreatureStatus.ACTIVE) { "Only active creatures can grow." }
        val currentLevel = instance.animalLevel.coerceAtLeast(1)
        val cost = CreatureEconomy.growthCostPearls(instance.findId, currentLevel)
        require(pearlLedgerDao.getBalance() >= cost) { "Insufficient Pearls." }
        val now = System.currentTimeMillis()
        pearlLedgerDao.insert(PearlLedgerEntity(UUID.randomUUID().toString(), -cost, "grow_creature", "shell_reward", instanceId, now, null))
        findInstanceDao.updateAnimalLevel(instanceId, currentLevel + 1)
    }

    suspend fun releaseCreature(instanceId: String): Int = db.withTransaction {
        val instance = findInstanceDao.getById(instanceId) ?: error("Creature not found")
        require(ShellContentCatalog.find(instance.findId)?.kind == ShellRewardKind.ANIMAL) { "Only animals can be released." }
        require(instance.creatureStatus == CreatureStatus.ACTIVE) { "Only active creatures can be released." }
        val payout = CreatureEconomy.releaseValuePearls(instance.findId, instance.animalLevel)
        val now = System.currentTimeMillis()
        placementDao.removeByInstance(instanceId)
        findInstanceDao.updateCreatureStatus(instanceId, CreatureStatus.RELEASED)
        pearlLedgerDao.insert(PearlLedgerEntity(UUID.randomUUID().toString(), payout, "release_creature", "shell_reward", instanceId, now, "Release for Pearls"))
        payout
    }

    suspend fun encounterBeyondBlue(targetCreatureId: String, selectedInstanceIds: List<String>): UserShellFindInstanceEntity = db.withTransaction {
        val target = CreatureCatalog.require(targetCreatureId)
        require(target.sourceType == CreatureSourceType.BEYOND_BLUE) { "Only Beyond Blue creatures can be encountered here." }
        val selected = if (selectedInstanceIds.isEmpty()) emptyList() else findInstanceDao.getByIds(selectedInstanceIds)
        require(selected.size == selectedInstanceIds.toSet().size) { "Selected creature missing." }
        require(selected.all { it.creatureStatus == CreatureStatus.ACTIVE && ShellContentCatalog.find(it.findId)?.kind == ShellRewardKind.ANIMAL }) { "Selected creatures must be active animals." }
        val selectedMinutes = selected.sumOf { CreatureEconomy.beyondBlueTradeContributionMinutes(it.findId, it.animalLevel) }
        val quote = CreatureEconomy.quoteBeyondBluePayment(targetCreatureId, selectedMinutes, pearlLedgerDao.getBalance())
        require(quote.canEncounter) { "Insufficient Pearls." }
        val now = System.currentTimeMillis()
        if (quote.pearlCostForRemaining > 0) {
            pearlLedgerDao.insert(PearlLedgerEntity(UUID.randomUUID().toString(), -quote.pearlCostForRemaining, "beyond_blue_encounter", "shell_reward", targetCreatureId, now, null))
        }
        if (quote.pearlReturnForOverpay > 0) {
            pearlLedgerDao.insert(PearlLedgerEntity(UUID.randomUUID().toString(), quote.pearlReturnForOverpay, "beyond_blue_overpay_return", "shell_reward", UUID.randomUUID().toString(), now, null))
        }
        selected.forEach { creature ->
            placementDao.removeByInstance(creature.instanceId)
            findInstanceDao.updateCreatureStatus(creature.instanceId, CreatureStatus.USED_BEYOND_BLUE)
        }
        val encountered = grantFindCopy(targetCreatureId, "beyond_blue", targetCreatureId)
        encountered
    }

    suspend fun updateStillwaterPerspective(perspective: StillwaterPerspective) {
        stillwaterPreferenceDao.upsert(StillwaterPreferenceEntity(perspective = perspective.name, updatedAt = System.currentTimeMillis()))
    }

    suspend fun regularFlowCount(): Int = sessionDao.getRegularSessionCount()
    suspend fun lastRegularFlowBefore(endTime: Long): Long? = sessionDao.getLastRegularSessionEndBefore(endTime)
}
