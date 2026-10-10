package com.kingkharnivore.skillz.data.repository.green

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.green.*
import com.kingkharnivore.skillz.data.model.entity.shell.StillwaterLedgerEntity
import com.kingkharnivore.skillz.domain.green.*
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PlantActionResult {
    data class Success(val action: GreenActionEntity) : PlantActionResult
    data object InsufficientDrops : PlantActionResult
    data object Mastered : PlantActionResult
    data object StaleLevel : PlantActionResult
    data object NotFound : PlantActionResult
}

/** All wallet, plant, milestone, badge and action writes commit together. */
@Singleton
class GreenRepository @Inject constructor(private val db: SkillzDatabase) {
    private val dao get() = db.greenDao()
    private val wallet get() = db.stillwaterLedgerDao()
    fun observeDropBalance() = wallet.observeTotal()
    fun observeSpecimens() = dao.observeSpecimens()
    fun observeAwards() = dao.observeAwards()
    fun observeEnvironment(environment: GreenEnvironment) = observeSpecimens().map { plants ->
        plants.filter { GreenCatalogue.byId[it.speciesId]?.environment == environment }
    }

    suspend fun plantSeed(speciesId: String, actionId: String, now: Long = System.currentTimeMillis()): PlantActionResult = db.withTransaction {
        require(actionId.isNotBlank())
        dao.action(actionId)?.let {
            require(it.kind == "plant_seed_planted" && it.targetId == speciesId)
            return@withTransaction PlantActionResult.Success(it)
        }
        val species = GreenCatalogue.byId[speciesId] ?: return@withTransaction PlantActionResult.NotFound
        val cost = GreenEconomy.seedCost(species.tier)
        val balance = wallet.getTotal()
        if (balance < cost) return@withTransaction PlantActionResult.InsufficientDrops
        val order = dao.nextOrder()
        val time = maxOf(now, dao.specimens().maxOfOrNull { it.plantedAt } ?: now)
        val plant = PlantSpecimenEntity(UUID.randomUUID().toString(), species.id, 1, time,
            positionKey = "${species.environment.id}:$order", createdOrder = order, investedDrops = cost)
        dao.insert(plant)
        commit(actionId, "plant_seed_planted", speciesId, species, plant, 0, cost, balance, time)
    }

    suspend fun water(specimenId: String, expectedLevel: Int, actionId: String, now: Long = System.currentTimeMillis()): PlantActionResult = db.withTransaction {
        require(actionId.isNotBlank())
        dao.action(actionId)?.let {
            require(it.kind == "plant_watered" && it.targetId == specimenId && it.oldLevel == expectedLevel)
            return@withTransaction PlantActionResult.Success(it)
        }
        val plant = dao.specimen(specimenId) ?: return@withTransaction PlantActionResult.NotFound
        if (plant.level == 99) return@withTransaction PlantActionResult.Mastered
        if (plant.level != expectedLevel) return@withTransaction PlantActionResult.StaleLevel
        val species = GreenCatalogue.byId[plant.speciesId] ?: return@withTransaction PlantActionResult.NotFound
        val cost = GreenEconomy.waterCost(species.tier, plant.level)
        val balance = wallet.getTotal()
        if (balance < cost) return@withTransaction PlantActionResult.InsufficientDrops
        val time = maxOf(now, plant.lastWateredAt ?: plant.plantedAt, plant.fullyGrownAt ?: 0)
        val level = plant.level + 1
        val grown = plant.copy(level = level, lastWateredAt = time,
            fullyGrownAt = plant.fullyGrownAt ?: time.takeIf { level >= 90 },
            masteredAt = plant.masteredAt ?: time.takeIf { level == 99 }, investedDrops = plant.investedDrops + cost)
        dao.update(grown)
        commit(actionId, "plant_watered", specimenId, species, grown, plant.level, cost, balance, time)
    }

    private suspend fun commit(id: String, kind: String, target: String, species: PlantSpecies, plant: PlantSpecimenEntity,
        oldLevel: Int, cost: Long, balance: Long, time: Long): PlantActionResult.Success {
        val action = GreenActionEntity(id, kind, target, plant.id, oldLevel, plant.level, cost, balance - cost, time)
        wallet.insert(StillwaterLedgerEntity("green:$id", -cost, kind, plant.id, time))
        dao.record(action)
        val event = GreenEventEntity("$id:$kind", kind, time, species.id, species.environment.id, species.tier,
            oldLevel, plant.level, PlantGrowthStageResolver.resolve(plant.level).name, cost, balance - cost)
        dao.event(event)
        if (oldLevel > 0 && PlantGrowthStageResolver.resolve(oldLevel) != PlantGrowthStageResolver.resolve(plant.level)) {
            dao.event(event.copy(id = "$id:stage", name = "plant_growth_stage_reached"))
        }
        if (plant.level == 90 || plant.level == 99) {
            val milestone = if (plant.level == 90) "plant_fully_grown" else "plant_mastered"
            dao.event(event.copy(id = "$id:$milestone", name = milestone))
        }
        reconcileBadges(time, historical = false)
        return PlantActionResult.Success(action)
    }

    /** Additive reconciliation: keep every original Green award and shared badge record. */
    suspend fun reconcileBadges(now: Long = System.currentTimeMillis(), historical: Boolean = true) = db.withTransaction {
        val plants = dao.specimens()
        val progress = plants.map { PlantProgress(it.speciesId, it.level) }
        val existing = dao.awards().associateBy { it.id }
        val eligible = GreenBadgeEvaluator.eligible(progress)
        val shared = db.userBadgeDao().getAll().associateBy { it.badgeId }
        GreenBadgeEvaluator.definitions.filter { it.id in eligible || it.id in existing || (shared[it.id]?.count ?: 0) > 0 }.forEach { badge ->
            val previous = shared[badge.id]
            val firstTime = existing[badge.id]?.awardedAt ?: previous?.firstEarnedAt ?: now
            val inserted = dao.award(GreenBadgeAwardEntity(badge.id, firstTime, GreenCatalogue.VERSION)) != -1L
            val count = maxOf(previous?.count ?: 0, if (badge.speciesId != null) plants.count { it.speciesId == badge.speciesId && it.level == 99 }.coerceAtLeast(1) else 1)
            if (previous == null) {
                db.userBadgeDao().upsert(com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity(
                    badge.id, count, firstTime, firstTime, !historical,
                    viewedAt = firstTime.takeIf { historical },
                    timestampConfidence = if (historical && badge.id !in existing) com.kingkharnivore.skillz.domain.achievement.AchievementTimestampConfidence.UNKNOWN else com.kingkharnivore.skillz.domain.achievement.AchievementTimestampConfidence.EXACT))
            } else if (count > previous.count) {
                db.userBadgeDao().upsert(previous.copy(count = count, lastEarnedAt = maxOf(previous.lastEarnedAt, now),
                    isNew = if (historical) previous.isNew else true, viewedAt = if (historical) previous.viewedAt else null))
            }
            // Match the shared badge hub: finished goals leave active tracking; pins remain.
            if (badge.isTerminal(count)) db.achievementDao().deleteTracking(badge.id)
            if (inserted && previous == null && !historical) {
                val family = if (badge.id.startsWith("green_v1_") && badge.speciesId == null) {
                    val scope = if (badge.environment == null) "green" else "environment"
                    "${scope}_${when (badge.requiredLevel) { 1 -> "catalogue"; 90 -> "flourish"; else -> "mastery" }}_completed"
                } else "green_badge_earned"
                dao.event(GreenEventEntity("award:${badge.id}", family, now, badge.speciesId, badge.environment?.id))
            }
        }
        // Plant actions can complete a badge-book collection without visiting another Shell room.
        val ledger = db.userBadgeDao().getAll().associateBy { it.badgeId }
        com.kingkharnivore.skillz.domain.achievement.BadgeBookCollections.newAwards
            .filter { collection -> collection.memberIds.all { it in GreenBadgeEvaluator.byId } }
            .forEach { collection ->
                val members = collection.memberIds.mapNotNull(ledger::get)
                if (members.size == collection.memberIds.size && members.all { it.count > 0 } && collection.completionBadgeId !in ledger) {
                    val awardedAt = members.maxOf { it.firstEarnedAt }
                    val confidence = com.kingkharnivore.skillz.domain.achievement.AchievementTimestampCalculator.combineConfidence(
                        members.map { com.kingkharnivore.skillz.domain.achievement.EvidenceTimestamp(it.firstEarnedAt, it.timestampConfidence) })
                    db.userBadgeDao().upsert(com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity(
                        collection.completionBadgeId, 1, awardedAt, awardedAt, !historical,
                        viewedAt = awardedAt.takeIf { historical }, timestampConfidence = confidence))
                    db.achievementDao().deleteTracking(collection.completionBadgeId)
                }
            }
    }

    /** Local, first-party usage events; no network or personal content. */
    suspend fun recordView(name: String, environment: GreenEnvironment? = null, speciesId: String? = null) {
        require(name in setOf("green_opened", "green_environment_opened", "green_catalogue_opened", "plant_seed_viewed"))
        val species = speciesId?.let { GreenCatalogue.byId[it] }
        dao.event(GreenEventEntity(UUID.randomUUID().toString(), name, System.currentTimeMillis(), speciesId,
            environment?.id ?: species?.environment?.id, species?.tier))
    }
}
