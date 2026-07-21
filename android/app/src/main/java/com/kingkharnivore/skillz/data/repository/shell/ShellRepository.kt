package com.kingkharnivore.skillz.data.repository.shell

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.shell.*
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.data.model.shell.*
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureEconomy
import com.kingkharnivore.skillz.utils.shell.CreatureSourceType
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import com.kingkharnivore.skillz.utils.shell.CreatureStatus
import com.kingkharnivore.skillz.utils.shell.StillwaterCatalog
import com.kingkharnivore.skillz.utils.shell.StillwaterVessel
import com.kingkharnivore.skillz.utils.shell.validateStillwaterDraw
import com.kingkharnivore.skillz.domain.achievement.AchievementChange
import com.kingkharnivore.skillz.domain.achievement.AchievementChangeType
import com.kingkharnivore.skillz.domain.achievement.AchievementResult
import com.kingkharnivore.skillz.domain.achievement.BadgeRequirement
import com.kingkharnivore.skillz.domain.achievement.CollectionCatalog
import com.kingkharnivore.skillz.domain.achievement.CollectionProgressCalculator
import com.kingkharnivore.skillz.domain.achievement.MilestoneEngine
import com.kingkharnivore.skillz.domain.achievement.CelebrationLifecycle
import com.kingkharnivore.skillz.domain.achievement.CelebrationStage
import com.kingkharnivore.skillz.domain.achievement.AchievementBadgeCatalog
import com.kingkharnivore.skillz.domain.achievement.BadgeCountType
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


enum class ShellNotificationType { FIND, BADGE }


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
    private val objectiveCompletionDao: ObjectiveCompletionDao,
    private val achievementDao: AchievementDao
) {
    fun observePearlBalance(): Flow<Int> = pearlLedgerDao.observeBalance()
    fun observeStillwaterTotal(): Flow<Long> = stillwaterLedgerDao.observeTotal()
    fun observeStillwaterLifetimeTotal(): Flow<Long> = stillwaterLedgerDao.observeLifetimeTotal()
    fun observeOwnedFinds(): Flow<List<UserShellFindInstanceEntity>> = findInstanceDao.observeAll()
    fun observeStacks(): Flow<List<UserShellFindStackEntity>> = findStackDao.observeAll()
    fun observePlacements(roomId: ShellRoomId): Flow<List<ShellPlacementEntity>> =
        placementDao.observeByRoom(roomId.name)
    fun observeEarnedBadges(): Flow<List<UserBadgeEntity>> = badgeDao.observeEarned()
    fun observeBadgePins(): Flow<List<BadgePinEntity>> = achievementDao.observePins()
    fun observeBadgeTracking(): Flow<List<BadgeTrackingEntity>> = achievementDao.observeTracking()
    fun observeCreatureDiscoveries(): Flow<List<CreatureDiscoveryEntity>> = achievementDao.observeDiscoveries()
    fun observeCreatureMasteries(): Flow<List<CreatureMasteryEventEntity>> = achievementDao.observeMasteries()
    fun observeCollectionCompletions(): Flow<List<CollectionCompletionEntity>> = achievementDao.observeCompletions()
    fun observeLatestAchievementBackfill(): Flow<AchievementBackfillEntity?> = achievementDao.observeLatestBackfill()
    fun observePendingMasteryCelebration(): Flow<MasteryCelebrationEventEntity?> = achievementDao.observePendingCelebration()
    fun observeBadgeCountFloors(): Flow<List<BadgeCountFloorEntity>> = achievementDao.observeCountFloors()
    fun observeDiscoveries(): Flow<List<UserDiscoveryEntity>> = discoveryDao.observeAll()
    fun observeObjectiveCompletions(): Flow<List<ObjectiveCompletionEntity>> =
        objectiveCompletionDao.observeCompletions()
    fun observeStillwaterPreference(): Flow<StillwaterPreferenceEntity?> =
        stillwaterPreferenceDao.observe()



    suspend fun getPearlBalance(): Int = pearlLedgerDao.getBalance()
    suspend fun getStillwaterTotal(): Long = stillwaterLedgerDao.getTotal()

    sealed interface PinResult {
        data object Pinned : PinResult
        data object AlreadyPinned : PinResult
        data class ReplacementRequired(val currentBadgeIds: List<String>) : PinResult
    }

    suspend fun pinBadge(badgeId: String, replaceBadgeId: String? = null): PinResult = db.withTransaction {
        reconcileAchievementLedger(System.currentTimeMillis())
        require(badgeDao.get(badgeId)?.count?.let { it > 0 } == true) { "Only earned badges can be pinned." }
        val current = achievementDao.getPins()
        if (current.any { it.badgeId == badgeId }) return@withTransaction PinResult.AlreadyPinned
        if (current.size >= 3 && replaceBadgeId == null) return@withTransaction PinResult.ReplacementRequired(current.map { it.badgeId })
        val replacement = replaceBadgeId?.let { id -> current.firstOrNull { it.badgeId == id } }
        if (current.size >= 3) require(replacement != null) { "Choose a pinned badge to replace." }
        replacement?.let { achievementDao.deletePin(it.badgeId) }
        val order = replacement?.pinOrder ?: ((current.maxOfOrNull { it.pinOrder } ?: -1) + 1)
        achievementDao.insertPin(BadgePinEntity(badgeId, order, System.currentTimeMillis()))
        normalizePinOrder()
        PinResult.Pinned
    }

    suspend fun unpinBadge(badgeId: String) = db.withTransaction {
        achievementDao.deletePin(badgeId)
        normalizePinOrder()
    }

    suspend fun movePinnedBadge(badgeId: String, direction: Int) = db.withTransaction {
        val pins = achievementDao.getPins()
        val from = pins.indexOfFirst { it.badgeId == badgeId }
        if (from < 0) return@withTransaction
        val to = (from + direction.coerceIn(-1, 1)).coerceIn(0, pins.lastIndex)
        if (from == to) return@withTransaction
        achievementDao.updatePinOrder(pins[from].badgeId, -1)
        achievementDao.updatePinOrder(pins[to].badgeId, from)
        achievementDao.updatePinOrder(pins[from].badgeId, to)
    }

    private suspend fun normalizePinOrder() {
        val ordered = achievementDao.getPins()
        ordered.forEachIndexed { index, pin ->
            if (pin.pinOrder != index) achievementDao.updatePinOrder(pin.badgeId, -(index + 10))
        }
        ordered.forEachIndexed { index, pin -> achievementDao.updatePinOrder(pin.badgeId, index) }
    }

    suspend fun trackBadge(badgeId: String): Boolean = db.withTransaction {
        require(com.kingkharnivore.skillz.domain.achievement.AchievementBadgeCatalog.byId[badgeId]?.trackable == true) { "This badge cannot be tracked." }
        val current = achievementDao.getTracking()
        if (current.any { it.badgeId == badgeId }) return@withTransaction false
        require(current.size < 3) { "You can track up to three badges." }
        achievementDao.insertTracking(BadgeTrackingEntity(badgeId, System.currentTimeMillis())) != -1L
    }

    suspend fun untrackBadge(badgeId: String) { achievementDao.deleteTracking(badgeId) }

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

    suspend fun addPearls(
        delta: Int, reason: String, sourceType: String, sourceId: String?, note: String? = null
    ): Boolean = db.withTransaction {
        if (delta == 0 || pearlLedgerDao.sourceRewardCount(sourceType, sourceId, reason) > 0)
            return@withTransaction false
        pearlLedgerDao.insert(
            PearlLedgerEntity(
                UUID.randomUUID().toString(),
                delta,
                reason,
                sourceType,
                sourceId,
                System.currentTimeMillis(),
                note
            )
        )
        true
    }

    suspend fun addStillwater(units: Long, sourceType: String, sourceId: String?): Boolean =
        db.withTransaction {
        if (units <= 0 || stillwaterLedgerDao.sourceCount(sourceType, sourceId) > 0)
            return@withTransaction false
        stillwaterLedgerDao.insert(
            StillwaterLedgerEntity(
                UUID.randomUUID().toString(),
                units,
                sourceType,
                sourceId,System.currentTimeMillis()
            )
        )
        true
    }

    suspend fun drawFromStillwater(
        vessel: StillwaterVessel,
        unlockedZones: Set<CreatureZone>
    ): UserShellFindInstanceEntity = db.withTransaction {
        val balance = stillwaterLedgerDao.getTotal()
        validateStillwaterDraw(vessel, unlockedZones, balance)
        val entry = StillwaterCatalog.roll(vessel)
        val definition = CreatureCatalog.require(entry.creatureId)
        require(definition.sourceType == CreatureSourceType.STILLWATER) {
            "Stillwater can only draw Stillwater creatures."
        }
        require(definition.zone == vessel.zone) { "Stillwater vessel depth mismatch." }
        val now = System.currentTimeMillis()
        val instance = grantFindCopy(
            entry.creatureId, "stillwater", vessel.name.lowercase()
        )
        stillwaterLedgerDao.insert(
            StillwaterLedgerEntity(
                id = UUID.randomUUID().toString(),
                units = -vessel.dropCost,
                sourceType = "stillwater_draw",
                sourceId = instance.instanceId,
                createdAt = now
            )
        )
        instance
    }

    suspend fun incrementBadge(badgeId: String, by: Int = 1): Int {
        val now = System.currentTimeMillis()
        val current = badgeDao.get(badgeId)
        val newCount = (current?.count ?: 0) + by
        badgeDao.upsert(
            current?.copy(count = newCount, lastEarnedAt = now, isNew = true, viewedAt = null)
                ?: UserBadgeEntity(
                    badgeId, by, now, now, true
                )
        )
        return newCount
    }

    suspend fun grantFindCopy(
        findId: String, sourceType: String, sourceId: String?
    ): UserShellFindInstanceEntity = db.withTransaction {
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
        CreatureCatalog.get(findId)?.let {
            achievementDao.recordDiscovery(
                CreatureDiscoveryEntity(findId, now, sourceType, entity.instanceId, now)
            )
            persistCurrentCollectionCompletions(now)
            reconcileAchievementLedger(now)
        }
        entity
    }

    suspend fun grantFindOnce(
        findId: String, sourceType: String, sourceId: String?
    ): UserShellFindInstanceEntity? {
        if (findInstanceDao.countByFindId(findId) > 0) return null
        return grantFindCopy(findId, sourceType, sourceId)
    }

    suspend fun addStack(findId: String, quantity: Int = 1) {
        val now = System.currentTimeMillis()
        val current = findStackDao.get(findId)
        findStackDao.upsert(
            current?.copy(
                quantity = current.quantity + quantity,
                lastAcquiredAt = now,
                isNew = true,
                viewedAt = null
            )
                ?: UserShellFindStackEntity(
                    findId, quantity, now, now, true
                )
        )
    }

    suspend fun grantDiscoveryOnce(
        discoveryId: String, sourceType: String, sourceId: String?
    ): UserDiscoveryEntity? = db.withTransaction {
        val definition = ShellContentCatalog.discovery(discoveryId)
            ?: return@withTransaction null
        if (definition.oncePerUser && discoveryDao.getFirst(discoveryId) != null)
            return@withTransaction null
        val find = definition.grantsFindId?.let { findId ->
            val def = ShellContentCatalog.find(findId)
            when {
                def?.stackable == true -> { addStack(findId); null }
                def != null -> grantFindOnce(findId, sourceType, sourceId)
                else -> null
            }
        }
        val entity = UserDiscoveryEntity(
            UUID.randomUUID().toString(),
            discoveryId,
            System.currentTimeMillis(),
            sourceType,
            sourceId,
            find?.instanceId,
            true
        )
        discoveryDao.insert(entity)
        incrementBadge("badge_discovery")
        entity
    }

    suspend fun placeInstance(instanceId: String, roomId: ShellRoomId, slotId: String) =
        db.withTransaction {
        val instance = findInstanceDao.getById(instanceId) ?: error("Shell reward not found")
        val find = ShellContentCatalog.find(instance.findId)
            ?: error("Shell reward definition missing")
        val slot = ShellContentCatalog.focusSlots.firstOrNull {
            it.roomId == roomId && it.slotId == slotId
        } ?: error("Invalid slot.")
        require(find.placeable) { "This reward is not displayable." }
        require(find.kind != ShellRewardKind.ANIMAL
                || instance.creatureStatus == CreatureStatus.ACTIVE
        ) { "This creature is no longer swimming in The Blue." }
        require(
            ShellContentCatalog.isCompatibleWithSlot(slot, find)
        ) { "Invalid nook for this reward." }
        val currentInSlot = placementDao.getBySlot(roomId.name, slotId)
        if (currentInSlot?.instanceId == instanceId) return@withTransaction
        currentInSlot?.let {
            placementDao.removeByInstance(it.instanceId)
            findInstanceDao.updateArchivedState(it.instanceId, true)
        }
        placementDao.removeByInstance(instanceId)
        findInstanceDao.updateArchivedState(instanceId, false)
        placementDao.insert(
            ShellPlacementEntity(
                UUID.randomUUID().toString(),
                roomId.name,
                slotId,
                instanceId,
                System.currentTimeMillis()
            )
        )
    }

    suspend fun removePlacement(instanceId: String) = db.withTransaction {
        placementDao.removeByInstance(instanceId)
        findInstanceDao.updateArchivedState(instanceId, true)
    }

    suspend fun markNotificationViewed(notificationId: String) = db.withTransaction {
        val now = System.currentTimeMillis()
        when (notificationId.substringBefore(':')) {
            ShellNotificationType.FIND.name -> findInstanceDao.markViewed(notificationId.substringAfter(':'), now)
            ShellNotificationType.BADGE.name -> badgeDao.markViewed(notificationId.substringAfter(':'), now)
        }
    }

    suspend fun markAllNotificationsViewed() = db.withTransaction {
        val now = System.currentTimeMillis()
        findInstanceDao.markFindIdsViewed(ShellContentCatalog.allAnimalFindIds.toList(), now)
        badgeDao.markAllViewed(now)
    }

    suspend fun markTheBlueAnimalsSeen() = db.withTransaction {
        val animalFindIds = ShellContentCatalog.regularFlowAnimalFindIds.toList()
        if (animalFindIds.isNotEmpty()) {
            findInstanceDao.markFindIdsSeen(animalFindIds)
        }
    }

    suspend fun invitePearlObject(findId: String, roomId: ShellRoomId, slotId: String) =
        db.withTransaction {
        val def = ShellContentCatalog.find(findId) ?: error("Shell object definition missing")
        val cost = def.pearlCost ?: error("This object cannot be shaped with Pearls.")
        val slot = ShellContentCatalog.focusSlots.firstOrNull {
            it.roomId == roomId && it.slotId == slotId
        } ?: error("Invalid slot.")
        require(def.isPearlObject) { "This object cannot be invited with Pearls." }
        require(def.placeable) { "This object cannot rest here." }
        require(ShellContentCatalog.isCompatibleWithSlot(slot, def)) {
            "Invalid nook for this object."
        }
        require(placementDao.getBySlot(roomId.name, slotId) == null) {
            "Choose something to swap."
        }
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
        pearlLedgerDao.insert(
            PearlLedgerEntity(
                UUID.randomUUID().toString(),
                -cost,
                "invite_object",
                "shell_reward",
                instance.instanceId,
                now, null
            )
        )
        findInstanceDao.insert(instance)
        placementDao.insert(
            ShellPlacementEntity(
                UUID.randomUUID().toString(),
                roomId.name,
                slotId,
                instance.instanceId,
                now
            )
        )
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
        pearlLedgerDao.insert(
            PearlLedgerEntity(
                UUID.randomUUID().toString(),
                -cost,
                "invite_object",
                "shell_reward",
                instance.instanceId,
                now, null
            )
        )
        findInstanceDao.insert(instance)
    }

    suspend fun upgradeInstance(instanceId: String) = db.withTransaction {
        val instance = findInstanceDao.getById(instanceId) ?: error("Shell reward not found")
        val find = ShellContentCatalog.find(instance.findId)
            ?: error("Shell reward definition missing")
        if (find.kind == ShellRewardKind.ANIMAL) {
            val currentLevel = instance.animalLevel.coerceAtLeast(1)
            growCreature(instanceId, "level_up:$instanceId:${currentLevel + 1}")
            return@withTransaction
        }
        require(find.upgradeable) { "This object is resting in its current form." }
        val next = ShellContentCatalog.nextUpgrade(
            find.findId, instance.currentUpgradeStageId
        ) ?: error("This object is resting in its current form.")
        val balance = pearlLedgerDao.getBalance()
        require(balance >= next.pearlCost) { "Insufficient Pearls." }
        pearlLedgerDao.insert(
            PearlLedgerEntity(
                UUID.randomUUID().toString(),
                -next.pearlCost,
                "shape_find",
                "shell_reward",
                instanceId,
                System.currentTimeMillis(),
                null
            )
        )
        upgradeDao.insert(
            ShellFindUpgradeEntity(
                UUID.randomUUID().toString(),
                instanceId,
                instance.currentUpgradeStageId,
                next.upgradeStageId,
                next.pearlCost,
                System.currentTimeMillis()
            )
        )
        findInstanceDao.updateUpgradeStage(instanceId, next.upgradeStageId)
    }


    suspend fun growCreature(
        instanceId: String,
        transactionId: String,
        originDestination: String = "CHEST"
    ): AchievementResult = db.withTransaction {
        achievementDao.getEvent(transactionId)?.let { committed ->
            val level = findInstanceDao.getById(instanceId)?.animalLevel
            return@withTransaction AchievementResult(committed.eventId, false, emptyList(), resultingLevel = level)
        }
        val instance = findInstanceDao.getById(instanceId) ?: error("Creature not found")
        require(ShellContentCatalog.find(instance.findId)?.kind == ShellRewardKind.ANIMAL) { "Only animals can grow with Pearls." }
        require(instance.creatureStatus == CreatureStatus.ACTIVE) {
            "Only active creatures can grow."
        }
        val currentLevel = instance.animalLevel.coerceAtLeast(1)
        require(currentLevel < CreatureEconomy.MAX_CREATURE_LEVEL) {
            "Mastered at Level 99."
        }
        val cost = CreatureEconomy.growthCostPearls(instance.findId, currentLevel)
        val balance = pearlLedgerDao.getBalance()
        require(balance >= cost) { insufficientGrowthPearlsMessage(cost, balance) }
        val now = System.currentTimeMillis()
        pearlLedgerDao.insert(
            PearlLedgerEntity(
                "pearl:$transactionId",
                -cost,
                "grow_creature",
                "shell_reward",
                instanceId,
                now,
                null
            )
        )
        val resultingLevel = currentLevel + 1
        findInstanceDao.updateAnimalLevel(instanceId, resultingLevel)
        findInstanceDao.updateActivity(instanceId, now)
        achievementDao.recordDiscovery(CreatureDiscoveryEntity(instance.findId, instance.acquiredAt, instance.sourceType, instanceId, now))
        val changes = mutableListOf<AchievementChange>()
        if (resultingLevel == CreatureEconomy.MAX_CREATURE_LEVEL) {
            val badgesBefore = listOf(
                "mastery_species_${instance.findId}", "mastery_first", "mastery_circle", "mastery_variety"
            ).associateWith { badgeDao.get(it)?.count ?: 0 }
            val completionsBefore = achievementDao.getCompletions().map { it.completionId }.toSet()
            val inserted = achievementDao.recordMastery(CreatureMasteryEventEntity(
                eventId = "mastery:$instanceId", creatureInstanceId = instanceId,
                speciesId = instance.findId, achievedAt = now, levelUpTransactionId = transactionId
            )) != -1L
            if (inserted) {
                val masteries = achievementDao.getMasteries()
                val speciesCount = masteries.count { it.speciesId == instance.findId }
                setBadgeExact("mastery_species_${instance.findId}", speciesCount, now)
                setBadgeExact("mastery_first", 1, now)
                setBadgeExact("mastery_circle", masteries.size, now)
                setBadgeExact("mastery_variety", masteries.map { it.speciesId }.distinct().size, now)
                changes += AchievementChange(AchievementChangeType.SPECIES_MASTERY_RECORDED,
                    badgeId = "mastery_species_${instance.findId}", speciesId = instance.findId, exactCount = speciesCount)
            }
            persistCurrentCollectionCompletions(now)
            reconcileAchievementLedger(now)
            if (inserted) {
                val masteries = achievementDao.getMasteries()
                val definition = CreatureCatalog.require(instance.findId)
                val discoveries = achievementDao.getDiscoveries().map { it.speciesId }.toSet()
                val owned = findInstanceDao.getAll().filter { it.creatureStatus == CreatureStatus.ACTIVE }
                    .groupBy({ it.findId }, { it.animalLevel })
                val mastered = masteries.map { it.speciesId }.toSet()
                fun progress(id: String) = CollectionProgressCalculator.calculate(
                    CollectionCatalog.byId.getValue(id), discoveries, owned, mastered
                )
                val region = progress(definition.collectionId)
                val blue = progress("collection_the_blue")
                val stillwater = progress("collection_stillwater")
                val allWaters = progress("collection_all_waters")
                val completionsAfter = achievementDao.getCompletions()
                val newCompletionBadges = completionsAfter.filter { it.completionId !in completionsBefore }
                    .map { "${it.collectionId}_${it.completionType.lowercase()}" }
                val standardIds = badgesBefore.keys.toList()
                val badgesAfter = standardIds.associateWith { badgeDao.get(it)?.count ?: 0 }
                val newlyEarned = standardIds.filter { badgesBefore.getValue(it) == 0 && badgesAfter.getValue(it) > 0 } + newCompletionBadges
                val advanced = standardIds.filter { badgesAfter.getValue(it) > badgesBefore.getValue(it) && it !in newlyEarned }
                val milestones = standardIds.mapNotNull { id ->
                    MilestoneEngine.evaluate(badgesAfter.getValue(id), badgesBefore.getValue(id)).newlyReachedThreshold?.let { "$id:$it" }
                }
                achievementDao.insertCelebration(
                    MasteryCelebrationEventEntity(
                        eventId = "celebration:$transactionId", transactionId = transactionId,
                        creatureInstanceId = instanceId, speciesId = instance.findId,
                        artworkKey = definition.staticIconKey, regionId = definition.collectionId,
                        sourceId = definition.sourceType.name, previousLevel = currentLevel,
                        newLevel = resultingLevel,
                        speciesMasteryCount = badgeDao.get("mastery_species_${instance.findId}")?.count ?: masteries.count { it.speciesId == instance.findId },
                        totalMasteries = badgeDao.get("mastery_circle")?.count ?: masteries.size,
                        uniqueMasteredSpecies = badgeDao.get("mastery_variety")?.count ?: mastered.size,
                        regionalDiscovered = region.discoveredSpeciesCount, regionalTotal = region.totalCompletionistSpecies,
                        regionalMastered = region.masteredSpeciesCount, regionalCollectorEarned = region.collectorEarned,
                        regionalCompletionistEarned = region.completionistEarned,
                        blueMastered = blue.masteredSpeciesCount, blueTotal = blue.totalCompletionistSpecies,
                        stillwaterMastered = stillwater.masteredSpeciesCount, stillwaterTotal = stillwater.totalCompletionistSpecies,
                        allWatersMastered = allWaters.masteredSpeciesCount, allWatersTotal = allWaters.totalCompletionistSpecies,
                        newlyEarnedBadgeIds = newlyEarned.distinct().joinToString(","),
                        advancedBadgeIds = advanced.joinToString(","), milestonesReached = milestones.joinToString(","),
                        originDestination = originDestination, createdAt = now,
                        lifecycleState = CelebrationLifecycle.PENDING.name,
                        presentationStage = CelebrationStage.LEVEL_TRANSITION.name
                    )
                )
            }
        }
        achievementDao.recordEvent(AchievementEventEntity(transactionId, "CREATURE_LEVEL_UP", instanceId, instance.findId, now, "$cost|$resultingLevel"))
        AchievementResult(transactionId, true, changes, cost, resultingLevel)
    }

    private suspend fun setBadgeExact(id: String, count: Int, now: Long) {
        val current = badgeDao.get(id)
        val floor = achievementDao.getCountFloors().firstOrNull { it.badgeId == id }
        val reconciled = floor?.let {
            it.minimumCount + (count - it.verifiedCountAtReconciliation).coerceAtLeast(0)
        } ?: count
        if (current?.count == reconciled) return
        badgeDao.upsert(current?.copy(count = maxOf(current.count, reconciled), lastEarnedAt = now, isNew = true, viewedAt = null)
            ?: UserBadgeEntity(id, reconciled, now, now, true))
    }

    /** Reconciles the persistent earned ledger without reducing reliable legacy totals. */
    private suspend fun reconcileAchievementLedger(now: Long) {
        val discoveries = achievementDao.getDiscoveries().map { it.speciesId }.toSet()
        val masteries = achievementDao.getMasteries()
        val floors = achievementDao.getCountFloors().associateBy { it.badgeId }
        fun withFloor(id: String, verified: Int): Int = floors[id]?.let {
            it.minimumCount + (verified - it.verifiedCountAtReconciliation).coerceAtLeast(0)
        } ?: verified
        CreatureCatalog.all.forEach { species ->
            val id = "mastery_species_${species.creatureId}"
            val count = withFloor(id, masteries.count { it.speciesId == species.creatureId })
            if (count > 0) materializeBadge(id, count, now)
        }
        val uniqueMastered = masteries.map { it.speciesId }.toSet().size
        val exactCounts = mapOf(
            "mastery_first" to if (masteries.isEmpty()) 0 else 1,
            "mastery_circle" to masteries.size,
            "mastery_variety" to uniqueMastered,
            "variety_collector" to discoveries.size,
            "stillwater_first_catch" to if (discoveries.any { CreatureCatalog.get(it)?.sourceType == CreatureSourceType.STILLWATER }) 1 else 0,
            "stillwater_variety" to discoveries.count { CreatureCatalog.get(it)?.sourceType == CreatureSourceType.STILLWATER },
            "stillwater_mastery" to masteries.count { CreatureCatalog.get(it.speciesId)?.sourceType == CreatureSourceType.STILLWATER }
        )
        exactCounts.forEach { (id, verified) -> withFloor(id, verified).takeIf { it > 0 }?.let { materializeBadge(id, it, now) } }
        val owned = findInstanceDao.getAll().filter { it.creatureStatus == CreatureStatus.ACTIVE }.groupBy({ it.findId }, { it.animalLevel })
        val masteredIds = masteries.map { it.speciesId }.toSet()
        val progress = CollectionCatalog.collections.associate { collection -> collection.collectionId to
            CollectionProgressCalculator.calculate(collection, discoveries, owned, masteredIds) }
        if (CollectionCatalog.collections.filter { it.collectionId.startsWith("blue_") }.all { progress.getValue(it.collectionId).discoveredSpeciesCount > 0 }) materializeBadge("across_the_depths", 1, now)
        if (CollectionCatalog.collections.filter { it.collectionId.startsWith("blue_") || it.collectionId.startsWith("stillwater_") }.all { progress.getValue(it.collectionId).discoveredSpeciesCount > 0 }) materializeBadge("one_from_every_water", 1, now)
        if (progress["collection_the_blue"]?.curatorEarned == true) materializeBadge("keeper_of_the_blue", 1, now)
        achievementDao.getCompletions().forEach { completion ->
            materializeBadge("${completion.collectionId}_${completion.completionType.lowercase()}", 1, completion.completedAt)
        }
        cleanupTracking()
    }

    private suspend fun materializeBadge(id: String, count: Int, timestamp: Long) {
        val current = badgeDao.get(id)
        if (current == null) badgeDao.upsert(UserBadgeEntity(id, count, timestamp, timestamp, true))
        else if (count > current.count) badgeDao.upsert(current.copy(count = count, lastEarnedAt = timestamp, isNew = true, viewedAt = null))
    }

    private suspend fun cleanupTracking() {
        val earned = badgeDao.getAll().associateBy { it.badgeId }
        achievementDao.getTracking().forEach { tracked ->
            val definition = AchievementBadgeCatalog.byId[tracked.badgeId]
            val invalid = definition == null
            val completedOneTime = definition?.countType == BadgeCountType.ONE_TIME && (earned[tracked.badgeId]?.count ?: 0) > 0
            val exhaustedRepeatable = definition?.countType == BadgeCountType.REPEATABLE &&
                definition.milestones.none { it > (earned[tracked.badgeId]?.count ?: 0) }
            if (invalid || completedOneTime || exhaustedRepeatable) achievementDao.deleteTracking(tracked.badgeId)
        }
    }

    private suspend fun persistCurrentCollectionCompletions(now: Long): Int {
        val discoveries = achievementDao.getDiscoveries().map { it.speciesId }.toSet()
        val masteries = achievementDao.getMasteries().map { it.speciesId }.toSet()
        val owned = findInstanceDao.getAll()
            .filter { it.creatureStatus == CreatureStatus.ACTIVE }
            .groupBy({ it.findId }, { it.animalLevel })
        var inserted = 0
        CollectionCatalog.collections.forEach { collection ->
            val progress = CollectionProgressCalculator.calculate(collection, discoveries, owned, masteries)
            listOf(
                BadgeRequirement.COLLECTOR to progress.currentRosterCollectorComplete,
                BadgeRequirement.CURATOR to progress.currentRosterCuratorComplete,
                BadgeRequirement.COMPLETIONIST to progress.currentRosterCompletionistComplete
            ).filter { it.second }.forEach { (type, _) ->
                val eligibleRoster = collection.eligibleRoster(type)
                val rosterHash = collection.rosterHash(type)
                val entity = CollectionCompletionEntity(
                    "${collection.collectionId}:${type.name}:$rosterHash", collection.collectionId,
                    type.name, now, collection.rosterVersion, rosterHash,
                    eligibleRoster.joinToString(",")
                )
                if (achievementDao.recordCompletion(entity) != -1L) inserted++
                setBadgeExact("${collection.collectionId}_${type.name.lowercase()}", 1, now)
            }
        }
        return inserted
    }

    data class BackfillResult(val version: Int, val discoveredCount: Int, val masteryCount: Int, val completionCount: Int, val alreadyCompleted: Boolean)

    /** Safe to call from WorkManager or an IO coroutine; every evidence write is idempotent. */
    suspend fun backfillAchievements(version: Int = 1): BackfillResult = db.withTransaction {
        achievementDao.getBackfill(version)?.let {
            reconcileAchievementLedger(System.currentTimeMillis())
            return@withTransaction BackfillResult(version, it.discoveredCount, it.masteryCount, it.completionCount, true)
        }
        val now = System.currentTimeMillis()
        findInstanceDao.getAll().filter { CreatureCatalog.get(it.findId) != null }.forEach { instance ->
            achievementDao.recordDiscovery(CreatureDiscoveryEntity(instance.findId, instance.acquiredAt,
                instance.sourceType, instance.instanceId, now))
            if (instance.animalLevel >= CreatureEconomy.MAX_CREATURE_LEVEL) {
                achievementDao.recordMastery(CreatureMasteryEventEntity("backfill_mastery_${instance.instanceId}",
                    instance.instanceId, instance.findId, now, "backfill_${instance.instanceId}"))
            }
        }
        val completionCount = persistCurrentCollectionCompletions(now)
        reconcileAchievementLedger(now)
        val discoveryCount = achievementDao.getDiscoveries().size
        val masteryCount = achievementDao.getMasteries().size
        achievementDao.recordBackfill(AchievementBackfillEntity(version, now, discoveryCount, masteryCount, completionCount))
        BackfillResult(version, discoveryCount, masteryCount, completionCount, false)
    }


    suspend fun growCreatureByLevel(findId: String, level: Int, originDestination: String = "CHEST"): AchievementResult = db.withTransaction {
        require(ShellContentCatalog.find(findId)?.kind == ShellRewardKind.ANIMAL) {
            "Only animals can grow with Pearls."
        }
        val currentLevel = level.coerceAtLeast(1)
        require(currentLevel < CreatureEconomy.MAX_CREATURE_LEVEL) { "Mastered at Level 99." }
        val activeAtLevel = findInstanceDao
            .getActiveByFindIdAndLevel(findId, currentLevel, CreatureStatus.ACTIVE)
        require(activeAtLevel.isNotEmpty()) {
            "No active Level $currentLevel creature to grow."
        }
        val instance = activeAtLevel.first()
        growCreature(instance.instanceId, "level_up:${instance.instanceId}:${currentLevel + 1}", originDestination)
    }

    suspend fun updateCelebration(eventId: String, transition: com.kingkharnivore.skillz.domain.achievement.CelebrationTransition) {
        db.withTransaction {
            achievementDao.updateCelebrationState(eventId, transition.lifecycle.name, transition.stage.name,
                if (transition.lifecycle == CelebrationLifecycle.COMPLETED) System.currentTimeMillis() else null)
        }
    }

    private fun insufficientGrowthPearlsMessage(requiredPearls: Int, currentPearls: Int): String {
        val shortfall = (requiredPearls - currentPearls).coerceAtLeast(0)
        return "Level up requires $requiredPearls Pearls. You need $shortfall more."
    }

    suspend fun releaseCreature(instanceId: String): Int = db.withTransaction {
        val instance = findInstanceDao.getById(instanceId) ?: error("Creature not found")
        require(ShellContentCatalog.find(instance.findId)?.kind == ShellRewardKind.ANIMAL) {
            "Only animals can be released."
        }
        require(instance.creatureStatus == CreatureStatus.ACTIVE) {
            "Only active creatures can be released."
        }
        releaseActiveCreatures(listOf(instance), System.currentTimeMillis())
    }

    suspend fun releaseCreaturesByLevel(findId: String, selectionsByLevel: Map<Int, Int>): Int =
        db.withTransaction {
        val requestedSelections = selectionsByLevel.entries
            .groupBy { it.key.coerceAtLeast(1) }
            .mapValues { (_, entries) -> entries.sumOf {
                it.value.coerceAtLeast(0)
            } }
            .filterValues { it > 0 }
        require(requestedSelections.isNotEmpty()) {
            "Select at least one creature to release."
        }
        require(ShellContentCatalog.find(findId)?.kind == ShellRewardKind.ANIMAL) {
            "Only animals can be released."
        }
        val selectedInstances = mutableListOf<UserShellFindInstanceEntity>()
        requestedSelections.toSortedMap(
            compareByDescending { it }
        ).forEach { (level, quantity) ->
            val activeAtLevel = findInstanceDao
                .getActiveByFindIdAndLevel(findId, level, CreatureStatus.ACTIVE)
            require(activeAtLevel.size >= quantity) {
                "Not enough active Level $level creatures to release."
            }
            val placedInstanceIds = mutableSetOf<String>()
            for (instance in activeAtLevel) {
                if (placementDao.getByInstance(instance.instanceId) != null) {
                    placedInstanceIds += instance.instanceId
                }
            }
            selectedInstances += activeAtLevel
                .sortedWith(
                    compareBy<UserShellFindInstanceEntity> {
                        it.instanceId in placedInstanceIds
                    }
                        .thenBy { it.acquiredAt }
                        .thenBy { it.instanceId }
                )
                .take(quantity)
        }
        releaseActiveCreatures(selectedInstances, System.currentTimeMillis())
    }

    private suspend fun releaseActiveCreatures(
        instances: List<UserShellFindInstanceEntity>, now: Long
    ): Int {
        var totalPayout = 0
        instances.forEach { instance ->
            val payout = CreatureEconomy.releaseValuePearls(
                instance.findId,
                instance.animalLevel
            )
            placementDao.removeByInstance(instance.instanceId)
            findInstanceDao.updateCreatureStatus(
                instance.instanceId,
                CreatureStatus.RELEASED
            )
            findInstanceDao.updateActivity(instance.instanceId, now)
            findInstanceDao.updateSpeciesActivity(instance.findId, now)
            pearlLedgerDao.insert(
                PearlLedgerEntity(
                    UUID.randomUUID().toString(),
                    payout,
                    "release_creature",
                    "shell_reward",
                    instance.instanceId,
                    now, "Creature release")
            )
            totalPayout += payout
        }
        return totalPayout
    }

    suspend fun encounterBeyondBlue(
        targetCreatureId: String, selectedInstanceIds: List<String>
    ): UserShellFindInstanceEntity = db.withTransaction {
        val target = CreatureCatalog.require(targetCreatureId)
        require(target.sourceType == CreatureSourceType.BEYOND_BLUE) {
            "Only Beyond Blue creatures can be encountered here."
        }
        val selected = if (
            selectedInstanceIds.isEmpty()
            ) emptyList() else
                findInstanceDao.getByIds(selectedInstanceIds)
        require(selected.size == selectedInstanceIds.toSet().size) {
            "Selected creature missing."
        }
        require(selected.all {
            it.creatureStatus == CreatureStatus.ACTIVE
                    && ShellContentCatalog.find(it.findId)?.kind == ShellRewardKind.ANIMAL
        }) { "Selected creatures must be active animals." }
        val selectedMinutes = selected.sumOf {
            CreatureEconomy.beyondBlueTradeContributionMinutes(
                it.findId,
                it.animalLevel
            )
        }
        val quote = CreatureEconomy.quoteBeyondBluePayment(
            targetCreatureId,
            selectedMinutes,
            pearlLedgerDao.getBalance()
        )
        require(quote.canEncounter) { "Insufficient Pearls." }
        val now = System.currentTimeMillis()
        if (quote.pearlCostForRemaining > 0) {
            pearlLedgerDao.insert(
                PearlLedgerEntity(
                    UUID.randomUUID().toString(),
                    -quote.pearlCostForRemaining,
                    "beyond_blue_encounter",
                    "shell_reward",
                    targetCreatureId,
                    now,
                    null
                )
            )
        }
        if (quote.pearlReturnForOverpay > 0) {
            pearlLedgerDao.insert(
                PearlLedgerEntity(
                    UUID.randomUUID().toString(),
                    quote.pearlReturnForOverpay,
                    "beyond_blue_overpay_return",
                    "shell_reward",
                    UUID.randomUUID().toString(),
                    now,
                    null
                )
            )
        }
        selected.forEach { creature ->
            placementDao.removeByInstance(creature.instanceId)
            findInstanceDao.updateCreatureStatus(
                creature.instanceId, CreatureStatus.USED_BEYOND_BLUE
            )
        }
        val encountered = grantFindCopy(
            targetCreatureId, "beyond_blue", targetCreatureId
        )
        encountered
    }

    suspend fun regularFlowCount(): Int = sessionDao.getRegularSessionCount()
    suspend fun lastRegularFlowBefore(endTime: Long): Long? =
        sessionDao.getLastRegularSessionEndBefore(endTime)
}


const val SHELL_CHEST_ROUTE: String = "shell/chest"
const val SHELL_BADGES_ROUTE: String = "shell/badges"

fun notificationId(type: ShellNotificationType, sourceId: String): String = "${type.name}:$sourceId"
