package com.kingkharnivore.skillz.domain.shell

import kotlin.random.Random

enum class StillwaterVessel(
    val dropCost: Long,
    val level: Int,
    val zone: CreatureZone
) {
    FISHBOWL(15_000L, 1, CreatureZone.SUNLIT_REEF),
    AQUARIUM(25_000L, 2, CreatureZone.DEEPER_REEF),
    POND(45_000L, 3, CreatureZone.OPEN_BLUE),
    LAKE(75_000L, 4, CreatureZone.GREAT_BLUE)
}

enum class StillwaterRarity { COMMON, UNCOMMON, RARE, MYTHIC }

data class StillwaterCreatureEntry(
    val creatureId: String,
    val displayName: String,
    val vessel: StillwaterVessel,
    val rarity: StillwaterRarity
)

fun calculateDropsForSoftFlow(durationSeconds: Long): Long = durationSeconds.coerceAtLeast(0L)

fun requiresStillwaterConfirmation(vessel: StillwaterVessel): Boolean =
    vessel == StillwaterVessel.POND || vessel == StillwaterVessel.LAKE

fun stillwaterDropsNeeded(drops: Long, vessel: StillwaterVessel): Long =
    (vessel.dropCost - drops).coerceAtLeast(0L)

fun stillwaterVesselProgress(drops: Long, vessel: StillwaterVessel): Float =
    (drops.toFloat() / vessel.dropCost.toFloat()).coerceIn(0f, 1f)


fun validateStillwaterDraw(
    vessel: StillwaterVessel,
    unlockedZones: Set<CreatureZone>,
    claimableDrops: Long
) {
    require(vessel.zone in unlockedZones) { "Reach this depth in The Blue first." }
    require(claimableDrops >= vessel.dropCost) { "Not enough Drops yet." }
}

object StillwaterCatalog {
    val creatures: List<StillwaterCreatureEntry> = listOf(
        entry("stillwater_shrimp", "Shrimp", StillwaterVessel.FISHBOWL, StillwaterRarity.COMMON),
        entry("stillwater_crab", "Crab", StillwaterVessel.FISHBOWL, StillwaterRarity.COMMON),
        entry("stillwater_clam", "Clam", StillwaterVessel.FISHBOWL, StillwaterRarity.COMMON),
        entry("stillwater_snail", "Snail", StillwaterVessel.FISHBOWL, StillwaterRarity.UNCOMMON),
        entry("stillwater_limpet", "Limpet", StillwaterVessel.FISHBOWL, StillwaterRarity.UNCOMMON),
        entry("stillwater_barnacle", "Barnacle", StillwaterVessel.FISHBOWL, StillwaterRarity.UNCOMMON),
        entry("stillwater_cowrie", "Cowrie", StillwaterVessel.FISHBOWL, StillwaterRarity.RARE),
        entry("stillwater_horseshoe", "Horseshoe", StillwaterVessel.FISHBOWL, StillwaterRarity.MYTHIC),
        entry("stillwater_goby", "Goby", StillwaterVessel.AQUARIUM, StillwaterRarity.COMMON),
        entry("stillwater_wrasse", "Wrasse", StillwaterVessel.AQUARIUM, StillwaterRarity.COMMON),
        entry("stillwater_blenny", "Blenny", StillwaterVessel.AQUARIUM, StillwaterRarity.COMMON),
        entry("stillwater_lionfish", "Lionfish", StillwaterVessel.AQUARIUM, StillwaterRarity.UNCOMMON),
        entry("stillwater_anemone", "Anemone", StillwaterVessel.AQUARIUM, StillwaterRarity.UNCOMMON),
        entry("stillwater_cuttlefish", "Cuttlefish", StillwaterVessel.AQUARIUM, StillwaterRarity.UNCOMMON),
        entry("stillwater_moray", "Moray", StillwaterVessel.AQUARIUM, StillwaterRarity.RARE),
        entry("stillwater_nautilus", "Nautilus", StillwaterVessel.AQUARIUM, StillwaterRarity.MYTHIC),
        entry("stillwater_mahi", "Mahi", StillwaterVessel.POND, StillwaterRarity.COMMON),
        entry("stillwater_wahoo", "Wahoo", StillwaterVessel.POND, StillwaterRarity.COMMON),
        entry("stillwater_bonito", "Bonito", StillwaterVessel.POND, StillwaterRarity.COMMON),
        entry("stillwater_barracuda", "Barracuda", StillwaterVessel.POND, StillwaterRarity.UNCOMMON),
        entry("stillwater_amberjack", "Amberjack", StillwaterVessel.POND, StillwaterRarity.UNCOMMON),
        entry("stillwater_grouper", "Grouper", StillwaterVessel.POND, StillwaterRarity.UNCOMMON),
        entry("stillwater_marlin", "Marlin", StillwaterVessel.POND, StillwaterRarity.RARE),
        entry("stillwater_sailfish", "Sailfish", StillwaterVessel.POND, StillwaterRarity.MYTHIC),
        entry("stillwater_fangtooth", "Fangtooth", StillwaterVessel.LAKE, StillwaterRarity.COMMON),
        entry("stillwater_viperfish", "Viperfish", StillwaterVessel.LAKE, StillwaterRarity.COMMON),
        entry("stillwater_hatchetfish", "Hatchetfish", StillwaterVessel.LAKE, StillwaterRarity.COMMON),
        entry("stillwater_gulper", "Gulper", StillwaterVessel.LAKE, StillwaterRarity.UNCOMMON),
        entry("stillwater_grenadier", "Grenadier", StillwaterVessel.LAKE, StillwaterRarity.UNCOMMON),
        entry("stillwater_oarfish", "Oarfish", StillwaterVessel.LAKE, StillwaterRarity.UNCOMMON),
        entry("stillwater_blackdragon", "Blackdragon", StillwaterVessel.LAKE, StillwaterRarity.RARE),
        entry("stillwater_coelacanth", "Coelacanth", StillwaterVessel.LAKE, StillwaterRarity.MYTHIC)
    )

    val byId: Map<String, StillwaterCreatureEntry> = creatures.associateBy { it.creatureId }

    fun creaturesFor(vessel: StillwaterVessel): List<StillwaterCreatureEntry> = creatures.filter { it.vessel == vessel }

    fun roll(vessel: StillwaterVessel, random: Random = Random.Default): StillwaterCreatureEntry {
        val rarity = when (random.nextInt(100)) {
            in 0..59 -> StillwaterRarity.COMMON
            in 60..89 -> StillwaterRarity.UNCOMMON
            in 90..97 -> StillwaterRarity.RARE
            else -> StillwaterRarity.MYTHIC
        }
        val pool = creaturesFor(vessel).filter { it.rarity == rarity }
            .ifEmpty { creaturesFor(vessel) }
        return pool[random.nextInt(pool.size)]
    }

    private fun entry(
        creatureId: String,
        displayName: String,
        vessel: StillwaterVessel,
        rarity: StillwaterRarity
    ) = StillwaterCreatureEntry(creatureId, displayName, vessel, rarity)
}
