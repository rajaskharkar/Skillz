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
import com.kingkharnivore.skillz.domain.achievement.CollectionProgress
import com.kingkharnivore.skillz.domain.achievement.MasteryEvidenceCalculator
import com.kingkharnivore.skillz.domain.achievement.MilestoneEngine
import com.kingkharnivore.skillz.domain.achievement.CelebrationLifecycle
import com.kingkharnivore.skillz.domain.achievement.CelebrationStage
import com.kingkharnivore.skillz.domain.achievement.BadgeCountType
import com.kingkharnivore.skillz.domain.achievement.BadgeDefinitionResolver
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import org.json.JSONObject
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
        require(BadgeDefinitionResolver.resolve(badgeId).pinnable) { "This badge cannot be pinned." }
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

    private suspend fun normalizePinOrder() {
        val ordered = achievementDao.getPins()
        ordered.forEachIndexed { index, pin ->
            if (pin.pinOrder != index) achievementDao.updatePinOrder(pin.badgeId, -(index + 10))
        }
        ordered.forEachIndexed { index, pin -> achievementDao.updatePinOrder(pin.badgeId, index) }
    }

    suspend fun trackBadge(badgeId: String): Boolean = db.withTransaction {
        reconcileAchievementLedger(System.currentTimeMillis())
        cleanupTracking()
        val definition = BadgeDefinitionResolver.resolve(badgeId)
        require(definition.trackable) { "This badge cannot be tracked." }
        val currentProgress = definition.collectionId?.let { currentCollectionProgress(it) }
        val ownsRelevantSpecies = definition.speciesId?.let { speciesId ->
            findInstanceDao.getAll().any { it.findId == speciesId && it.creatureStatus == CreatureStatus.ACTIVE }
        } ?: true
        val actionable = when (definition.requirement) {
            BadgeRequirement.COLLECTOR -> currentProgress?.currentRosterCollectorComplete == false && currentProgress.totalParticipatingSpecies > 0
            BadgeRequirement.CURATOR -> currentProgress?.currentRosterCuratorComplete == false && currentProgress.totalParticipatingSpecies > 0
            BadgeRequirement.COMPLETIONIST -> currentProgress?.currentRosterCompletionistComplete == false && currentProgress.totalCompletionistSpecies > 0
            BadgeRequirement.EXACT_COUNT -> ownsRelevantSpecies && if (definition.countType == BadgeCountType.ONE_TIME) {
                (badgeDao.get(badgeId)?.count ?: 0) == 0
            } else definition.milestones.any { it > (badgeDao.get(badgeId)?.count ?: 0) }
        }
        require(actionable) { "This achievement has no current objective to track." }
        val current = achievementDao.getTracking()
        if (current.any { it.badgeId == badgeId }) return@withTransaction false
        require(current.size < 3) { "You can track up to three badges." }
        achievementDao.insertTracking(BadgeTrackingEntity(badgeId, System.currentTimeMillis())) != -1L
    }

    suspend fun untrackBadge(badgeId: String) { achievementDao.deleteTracking(badgeId) }

    private suspend fun masteryEvidence(owned: Map<String, List<Int>> = emptyMap()) =
        MasteryEvidenceCalculator.bySpecies(achievementDao.getMasteries(), achievementDao.getCountFloors(), owned)

    private suspend fun currentCollectionProgress(collectionId: String): CollectionProgress? {
        val definition = CollectionCatalog.byId[collectionId] ?: return null
        val owned = findInstanceDao.getAll().filter { it.creatureStatus == CreatureStatus.ACTIVE }
            .groupBy({ it.findId }, { it.animalLevel })
        return CollectionProgressCalculator.calculate(definition,
            achievementDao.getDiscoveries().map { it.speciesId }.toSet(), owned, masteryEvidence(owned))
    }

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
        val now = System.currentTimeMillis()
        currentInSlot?.let {
            placementDao.removeByInstance(it.instanceId)
            findInstanceDao.updateArchivedState(it.instanceId, true)
            findInstanceDao.updateActivity(it.instanceId, now)
        }
        placementDao.removeByInstance(instanceId)
        findInstanceDao.updateArchivedState(instanceId, false)
        findInstanceDao.updateActivity(instanceId, now)
        placementDao.insert(
            ShellPlacementEntity(
                UUID.randomUUID().toString(),
                roomId.name,
                slotId,
                instanceId,
                now
            )
        )
    }

    suspend fun removePlacement(instanceId: String) = db.withTransaction {
        placementDao.removeByInstance(instanceId)
        findInstanceDao.updateArchivedState(instanceId, true)
        findInstanceDao.updateActivity(instanceId, System.currentTimeMillis())
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
            val payload = runCatching { JSONObject(committed.resultPayload) }.getOrNull()
            val legacy = committed.resultPayload.split('|')
            return@withTransaction AchievementResult(committed.eventId, false, emptyList(),
                pearlCost = payload?.optInt("pearlCost") ?: legacy.getOrNull(0)?.toIntOrNull() ?: 0,
                resultingLevel = payload?.optInt("resultingLevel")?.takeIf { it > 0 }
                    ?: legacy.getOrNull(1)?.toIntOrNull() ?: level)
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
            reconcileAchievementLedger(now)
            val badgesBefore = badgeDao.getAll().associate { it.badgeId to it.count }
            val completionsBefore = achievementDao.getCompletions().map { it.completionId }.toSet()
            val inserted = achievementDao.recordMastery(CreatureMasteryEventEntity(
                eventId = "mastery:$instanceId", creatureInstanceId = instanceId,
                speciesId = instance.findId, achievedAt = now, levelUpTransactionId = transactionId
            )) != -1L
            if (inserted) {
                val evidence = masteryEvidence()
                val speciesCount = evidence.getValue(instance.findId).effectiveLifetimeCount
                changes += AchievementChange(AchievementChangeType.SPECIES_MASTERY_RECORDED,
                    badgeId = "mastery_species_${instance.findId}", speciesId = instance.findId, exactCount = speciesCount)
            }
            persistCurrentCollectionCompletions(now)
            reconcileAchievementLedger(now)
            if (inserted) {
                val definition = CreatureCatalog.require(instance.findId)
                val discoveries = achievementDao.getDiscoveries().map { it.speciesId }.toSet()
                val owned = findInstanceDao.getAll().filter { it.creatureStatus == CreatureStatus.ACTIVE }
                    .groupBy({ it.findId }, { it.animalLevel })
                val evidence = masteryEvidence(owned)
                fun progress(id: String) = CollectionProgressCalculator.calculate(
                    CollectionCatalog.byId.getValue(id), discoveries, owned, evidence
                )
                val region = progress(definition.primaryProgressCollectionId)
                val blue = progress("collection_the_blue")
                val stillwater = progress("collection_stillwater")
                val allWaters = progress("collection_all_waters")
                val completionsAfter = achievementDao.getCompletions()
                val badgesAfter = badgeDao.getAll().associate { it.badgeId to it.count }
                val affectedIds = (badgesBefore.keys + badgesAfter.keys).filter {
                    (badgesBefore[it] ?: 0) != (badgesAfter[it] ?: 0)
                }
                val newlyEarned = affectedIds.filter { (badgesBefore[it] ?: 0) == 0 && (badgesAfter[it] ?: 0) > 0 }
                val newEditionBadges = completionsAfter.filter { it.completionId !in completionsBefore }
                    .map { "${it.collectionId}_${it.completionType.lowercase()}" }.filter { it !in newlyEarned }
                val advanced = affectedIds.filter { it !in newlyEarned } + newEditionBadges
                val milestones = affectedIds.mapNotNull { id ->
                    MilestoneEngine.evaluate(badgesAfter[id] ?: 0, badgesBefore[id] ?: 0).newlyReachedThreshold?.let { "$id:$it" }
                }
                affectedIds.forEach { id ->
                    val before = badgesBefore[id] ?: 0; val after = badgesAfter[id] ?: 0
                    changes += AchievementChange(if (before == 0) AchievementChangeType.BADGE_NEWLY_EARNED else AchievementChangeType.COUNT_INCREASED,
                        badgeId = id, previousCount = before, exactCount = after)
                    MilestoneEngine.evaluate(after, before).newlyReachedThreshold?.let { milestone ->
                        changes += AchievementChange(AchievementChangeType.MILESTONE_REACHED, id, previousCount = before, exactCount = after, milestone = milestone)
                    }
                }
                newEditionBadges.forEach { id ->
                    changes += AchievementChange(AchievementChangeType.CURRENT_ROSTER_COMPLETED,
                        badgeId = id, collectionId = BadgeDefinitionResolver.resolve(id).collectionId,
                        previousCount = badgesBefore[id] ?: 1, exactCount = badgesAfter[id] ?: 1)
                }
                achievementDao.insertCelebration(
                    MasteryCelebrationEventEntity(
                        eventId = "celebration:$transactionId", transactionId = transactionId,
                        creatureInstanceId = instanceId, speciesId = instance.findId,
                        artworkKey = definition.staticIconKey, regionId = definition.primaryProgressCollectionId,
                        sourceId = definition.sourceType.name, previousLevel = currentLevel,
                        newLevel = resultingLevel,
                        speciesMasteryCount = evidence.getValue(instance.findId).effectiveLifetimeCount,
                        totalMasteries = evidence.values.sumOf { it.effectiveLifetimeCount },
                        uniqueMasteredSpecies = evidence.values.count { it.hasEverBeenMastered },
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
        val payload = JSONObject().put("schemaVersion", 1).put("pearlCost", cost)
            .put("previousLevel", currentLevel).put("resultingLevel", resultingLevel)
            .put("speciesId", instance.findId).toString()
        achievementDao.recordEvent(AchievementEventEntity(transactionId, "CREATURE_LEVEL_UP", instanceId, instance.findId, now, payload))
        AchievementResult(transactionId, true, changes, cost, resultingLevel)
    }

    private suspend fun setBadgeExact(id: String, count: Int, now: Long) {
        val current = badgeDao.get(id)
        val floor = achievementDao.getCountFloors().firstOrNull { it.badgeId == id }
        val reconciled = MasteryEvidenceCalculator.effectiveCount(count, floor)
        val next = maxOf(current?.count ?: 0, reconciled)
        if (current?.count == next || next <= 0) return
        badgeDao.upsert(current?.copy(count = next, lastEarnedAt = now, isNew = true, viewedAt = null)
            ?: UserBadgeEntity(id, next, now, now, true))
    }

    /** Reconciles the persistent earned ledger without reducing reliable legacy totals. */
    private suspend fun reconcileAchievementLedger(now: Long) {
        val discoveries = achievementDao.getDiscoveries().map { it.speciesId }.toSet()
        val masteries = achievementDao.getMasteries()
        val floorsList = achievementDao.getCountFloors()
        val floors = floorsList.associateBy { it.badgeId }
        val evidence = MasteryEvidenceCalculator.bySpecies(masteries, floorsList)
        CreatureCatalog.all.forEach { species ->
            val id = "mastery_species_${species.creatureId}"
            val count = evidence.getValue(species.creatureId).effectiveLifetimeCount
            if (count > 0) materializeBadge(id, count, now)
        }
        val uniqueMastered = evidence.values.count { it.hasEverBeenMastered }
        val exactCounts = mapOf(
            "mastery_first" to if (uniqueMastered == 0) 0 else 1,
            "mastery_circle" to maxOf(evidence.values.sumOf { it.effectiveLifetimeCount }, MasteryEvidenceCalculator.effectiveCount(masteries.size, floors["mastery_circle"])),
            "mastery_variety" to uniqueMastered,
            "variety_collector" to discoveries.size,
            "stillwater_first_catch" to if (discoveries.any { CreatureCatalog.get(it)?.sourceType == CreatureSourceType.STILLWATER }) 1 else 0,
            "stillwater_variety" to discoveries.count { CreatureCatalog.get(it)?.sourceType == CreatureSourceType.STILLWATER },
            "stillwater_mastery" to maxOf(
                evidence.values.filter { CreatureCatalog.get(it.speciesId)?.sourceType == CreatureSourceType.STILLWATER }.sumOf { it.effectiveLifetimeCount },
                MasteryEvidenceCalculator.effectiveCount(masteries.count { CreatureCatalog.get(it.speciesId)?.sourceType == CreatureSourceType.STILLWATER }, floors["stillwater_mastery"])
            )
        )
        exactCounts.forEach { (id, verified) ->
            val effective = if (id in setOf("mastery_circle", "stillwater_mastery")) verified else MasteryEvidenceCalculator.effectiveCount(verified, floors[id])
            effective.takeIf { it > 0 }?.let { materializeBadge(id, it, now) }
        }
        val owned = findInstanceDao.getAll().filter { it.creatureStatus == CreatureStatus.ACTIVE }.groupBy({ it.findId }, { it.animalLevel })
        val progress = CollectionCatalog.collections.associate { collection -> collection.collectionId to
            CollectionProgressCalculator.calculate(collection, discoveries, owned, evidence) }
        if (CollectionCatalog.collections.filter { it.collectionId.startsWith("blue_") }.all { progress.getValue(it.collectionId).discoveredSpeciesCount > 0 }) materializeBadge("across_the_depths", 1, now)
        if (CollectionCatalog.collections.filter { it.collectionId.startsWith("blue_") || it.collectionId.startsWith("stillwater_") }.all { collection ->
                collection.eligibleRoster(BadgeRequirement.COMPLETIONIST).any { evidence[it]?.hasEverBeenMastered == true }
            }) materializeBadge("one_from_every_water", 1, now)
        if (CollectionCatalog.collections.filter { it.collectionId.startsWith("blue_") }.all { progress[it.collectionId]?.collectorEarned == true }) materializeBadge("keeper_of_the_blue", 1, now)
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
            val definition = BadgeDefinitionResolver.resolve(tracked.badgeId)
            val invalid = !definition.trackable
            val collection = definition.collectionId?.let { currentCollectionProgress(it) }
            val currentRosterIncomplete = when (definition.requirement) {
                BadgeRequirement.COLLECTOR -> collection?.currentRosterCollectorComplete == false
                BadgeRequirement.CURATOR -> collection?.currentRosterCuratorComplete == false
                BadgeRequirement.COMPLETIONIST -> collection?.currentRosterCompletionistComplete == false
                else -> false
            }
            val completedOneTime = definition.countType == BadgeCountType.ONE_TIME && (earned[tracked.badgeId]?.count ?: 0) > 0 && !currentRosterIncomplete
            val exhaustedRepeatable = definition.countType == BadgeCountType.REPEATABLE &&
                definition.milestones.none { it > (earned[tracked.badgeId]?.count ?: 0) }
            if (invalid || completedOneTime || exhaustedRepeatable) achievementDao.deleteTracking(tracked.badgeId)
        }
    }

    private suspend fun persistCurrentCollectionCompletions(now: Long): Int {
        val discoveries = achievementDao.getDiscoveries().map { it.speciesId }.toSet()
        val owned = findInstanceDao.getAll()
            .filter { it.creatureStatus == CreatureStatus.ACTIVE }
            .groupBy({ it.findId }, { it.animalLevel })
        val evidence = masteryEvidence(owned)
        var inserted = 0
        CollectionCatalog.collections.forEach { collection ->
            val progress = CollectionProgressCalculator.calculate(collection, discoveries, owned, evidence)
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
    suspend fun backfillAchievements(version: Int = 2): BackfillResult = db.withTransaction {
        achievementDao.getBackfill(version)?.let {
            persistCurrentCollectionCompletions(System.currentTimeMillis())
            reconcileAchievementLedger(System.currentTimeMillis())
            return@withTransaction BackfillResult(version, 0, 0, 0, true)
        }
        val now = System.currentTimeMillis()
        val discoveriesBefore = achievementDao.getDiscoveries().size
        val masteriesBefore = achievementDao.getMasteries().size
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
        val recognizedDiscoveries = (discoveryCount - discoveriesBefore).coerceAtLeast(0)
        val recognizedMasteries = (masteryCount - masteriesBefore).coerceAtLeast(0)
        achievementDao.recordBackfill(AchievementBackfillEntity(version, now, recognizedDiscoveries, recognizedMasteries, completionCount))
        BackfillResult(version, recognizedDiscoveries, recognizedMasteries, completionCount, false)
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
            if (transition.lifecycle == CelebrationLifecycle.COMPLETED) {
                achievementDao.getCelebration(eventId)?.let { event ->
                    // The final summary always renders newly-earned badge chips. Advanced and
                    // milestone rows may have been skipped, so they intentionally remain unseen.
                    event.newlyEarnedBadgeIds.split(',').filter { it.isNotBlank() }.distinct()
                        .forEach { badgeDao.markViewed(it, System.currentTimeMillis()) }
                }
            }
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
            findInstanceDao.updateActivity(creature.instanceId, now)
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
