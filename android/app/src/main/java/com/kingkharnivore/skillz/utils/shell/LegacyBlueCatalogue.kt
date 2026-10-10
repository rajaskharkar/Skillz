package com.kingkharnivore.skillz.utils.shell


sealed interface StillwaterContainer {
    val name: String
    val dropCost: Long
    val level: Int
    val zone: CreatureZone
}

enum class StillwaterVessel(
    override val dropCost: Long,
    override val level: Int,
    override val zone: CreatureZone
) : StillwaterContainer {
    FISHBOWL(15_000L, 1, CreatureZone.SUNLIT_REEF),
    AQUARIUM(25_000L, 2, CreatureZone.DEEPER_REEF),
    POND(45_000L, 3, CreatureZone.OPEN_BLUE),
    LAKE(75_000L, 4, CreatureZone.GREAT_BLUE)
}


data class StillwaterCreatureEntry(
    val creatureId: String,
    val displayName: String,
    val vessel: StillwaterVessel
)

fun calculateDropsForSoftFlow(durationSeconds: Long): Long = durationSeconds.coerceAtLeast(0L)

/** Frozen historical roster metadata; acquisition is now deterministic in The Blue. */
object StillwaterCatalog {
    val creatures: List<StillwaterCreatureEntry> = listOf(
        entry("stillwater_shrimp", "Shrimp", StillwaterVessel.FISHBOWL),
        entry("stillwater_crab", "Crab", StillwaterVessel.FISHBOWL),
        entry("stillwater_clam", "Clam", StillwaterVessel.FISHBOWL),
        entry("stillwater_snail", "Snail", StillwaterVessel.FISHBOWL),
        entry("stillwater_limpet", "Limpet", StillwaterVessel.FISHBOWL),
        entry("stillwater_barnacle", "Barnacle", StillwaterVessel.FISHBOWL),
        entry("stillwater_cowrie", "Cowrie", StillwaterVessel.FISHBOWL),
        entry("stillwater_horseshoe", "Horseshoe", StillwaterVessel.FISHBOWL),
        entry("stillwater_goby", "Goby", StillwaterVessel.AQUARIUM),
        entry("stillwater_wrasse", "Wrasse", StillwaterVessel.AQUARIUM),
        entry("stillwater_blenny", "Blenny", StillwaterVessel.AQUARIUM),
        entry("stillwater_lionfish", "Lionfish", StillwaterVessel.AQUARIUM),
        entry("stillwater_anemone", "Anemone", StillwaterVessel.AQUARIUM),
        entry("stillwater_cuttlefish", "Cuttlefish", StillwaterVessel.AQUARIUM),
        entry("stillwater_moray", "Moray", StillwaterVessel.AQUARIUM),
        entry("stillwater_nautilus", "Nautilus", StillwaterVessel.AQUARIUM),
        entry("stillwater_mahi", "Mahi", StillwaterVessel.POND),
        entry("stillwater_wahoo", "Wahoo", StillwaterVessel.POND),
        entry("stillwater_bonito", "Bonito", StillwaterVessel.POND),
        entry("stillwater_barracuda", "Barracuda", StillwaterVessel.POND),
        entry("stillwater_amberjack", "Amberjack", StillwaterVessel.POND),
        entry("stillwater_grouper", "Grouper", StillwaterVessel.POND),
        entry("stillwater_marlin", "Marlin", StillwaterVessel.POND),
        entry("stillwater_sailfish", "Sailfish", StillwaterVessel.POND),
        entry("stillwater_fangtooth", "Fangtooth", StillwaterVessel.LAKE),
        entry("stillwater_viperfish", "Viperfish", StillwaterVessel.LAKE),
        entry("stillwater_hatchetfish", "Hatchetfish", StillwaterVessel.LAKE),
        entry("stillwater_gulper", "Gulper", StillwaterVessel.LAKE),
        entry("stillwater_grenadier", "Grenadier", StillwaterVessel.LAKE),
        entry("stillwater_oarfish", "Oarfish", StillwaterVessel.LAKE),
        entry("stillwater_blackdragon", "Blackdragon", StillwaterVessel.LAKE),
        entry("stillwater_coelacanth", "Coelacanth", StillwaterVessel.LAKE)
    )

    val byId: Map<String, StillwaterCreatureEntry> = creatures.associateBy { it.creatureId }

    fun creaturesFor(vessel: StillwaterVessel): List<StillwaterCreatureEntry> = creatures.filter { it.vessel == vessel }

    private fun entry(
        creatureId: String,
        displayName: String,
        vessel: StillwaterVessel
    ) = StillwaterCreatureEntry(creatureId, displayName, vessel)
}
