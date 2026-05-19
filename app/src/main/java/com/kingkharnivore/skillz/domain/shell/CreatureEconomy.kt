package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

const val PEARLS_PER_REQUIRED_FLOW_MINUTE = 2
const val PEARLS_PER_EXTRA_FLOW_MINUTE = 1

object CreatureStatus {
    const val ACTIVE = "ACTIVE"
    const val RELEASED = "RELEASED"
    const val USED_BEYOND_BLUE = "USED_BEYOND_BLUE"
}

enum class CreatureZone(val displayName: String) {
    SUNLIT_REEF("Sunlit Reef"),
    DEEPER_REEF("Deeper Reef"),
    OPEN_BLUE("Open Blue"),
    GREAT_BLUE("Great Blue")
}

enum class CreatureSourceType { FLOW_EARNED, BEYOND_BLUE }

enum class CreatureRenderFamily(val key: String, val visualCap: Float) {
    SMALL_FISH("small_fish", 1.65f),
    REEF_FISH("reef_fish", 1.55f),
    JELLYFISH("jellyfish", 1.55f),
    TURTLE("turtle", 1.45f),
    SURFACE_MAMMAL("surface_mammal", 1.45f),
    SEAHORSE("seahorse", 1.55f),
    STARFISH("starfish", 1.45f),
    SPIKY_URCHIN("spiky_urchin", 1.45f),
    PUFFERFISH("pufferfish", 1.55f),
    EEL("eel", 1.45f),
    RAY("ray", 1.45f),
    OCTOPUS("octopus", 1.45f),
    SNAKE("snake", 1.45f),
    SQUID("squid", 1.45f),
    LARGE_FISH("large_fish", 1.45f),
    DOLPHIN("dolphin", 1.45f),
    SUNFISH("sunfish", 1.35f),
    ORCA("orca", 1.35f),
    WHALE("whale", 1.30f),
    ANGLERFISH("anglerfish", 1.45f),
    SHARK("shark", 1.35f),
    GIANT_TENTACLE("giant_tentacle", 1.30f),
    LEGENDARY("legendary", 1.25f)
}

data class CreatureDefinition(
    val creatureId: String,
    val displayName: String,
    val zone: CreatureZone,
    val sourceType: CreatureSourceType,
    val flowTimeValueMinutes: Int? = null,
    val requirementMinutes: Int? = null,
    val staticIconKey: String,
    val animatedRendererKey: String,
    val renderFamily: CreatureRenderFamily
) {
    val pearlPrice: Int? get() = requirementMinutes?.times(PEARLS_PER_REQUIRED_FLOW_MINUTE)
}

data class CreatureReward(val creatureId: String, val quantity: Int)

data class CreaturePaymentQuote(
    val targetRequirementMinutes: Int,
    val selectedCreatureMinutes: Int,
    val remainingMinutes: Int,
    val pearlCostForRemaining: Int,
    val pearlReturnForOverpay: Int,
    val canEncounter: Boolean
)

data class CreatureCounts(
    val activeCount: Int,
    val lifetimeCount: Int,
    val releasedCount: Int,
    val usedBeyondBlueCount: Int,
    val highestLevel: Int,
    val levelDistribution: Map<Int, Int>
)

data class CreatureLedgerEntry(
    val creatureId: String,
    val status: String = CreatureStatus.ACTIVE,
    val level: Int = 1
)

object CreatureCatalog {
    val all: List<CreatureDefinition> = listOf(
        flow(ShellContentCatalog.FOCUS_MINNOW, "Minnow", CreatureZone.SUNLIT_REEF, 10, CreatureRenderFamily.SMALL_FISH),
        beyond("creature_clownfish", "Clownfish", CreatureZone.SUNLIT_REEF, 30, CreatureRenderFamily.REEF_FISH),
        beyond("creature_blue_tang", "Blue Tang", CreatureZone.SUNLIT_REEF, 45, CreatureRenderFamily.REEF_FISH),
        beyond("creature_butterflyfish", "Butterflyfish", CreatureZone.SUNLIT_REEF, 60, CreatureRenderFamily.REEF_FISH),
        beyond("creature_angelfish", "Angelfish", CreatureZone.SUNLIT_REEF, 90, CreatureRenderFamily.REEF_FISH),
        beyond("creature_parrotfish", "Parrotfish", CreatureZone.SUNLIT_REEF, 120, CreatureRenderFamily.REEF_FISH),
        beyond("creature_jellyfish", "Jellyfish", CreatureZone.SUNLIT_REEF, 180, CreatureRenderFamily.JELLYFISH),
        beyond("creature_sea_turtle", "Sea Turtle", CreatureZone.SUNLIT_REEF, 300, CreatureRenderFamily.TURTLE),
        beyond("creature_sea_otter", "Sea Otter", CreatureZone.SUNLIT_REEF, 420, CreatureRenderFamily.SURFACE_MAMMAL),
        beyond("creature_seal", "Seal", CreatureZone.SUNLIT_REEF, 480, CreatureRenderFamily.SURFACE_MAMMAL),

        flow(ShellContentCatalog.FOCUS_SEAHORSE, "Seahorse", CreatureZone.DEEPER_REEF, 30, CreatureRenderFamily.SEAHORSE),
        beyond("creature_starfish", "Starfish", CreatureZone.DEEPER_REEF, 90, CreatureRenderFamily.STARFISH),
        beyond("creature_sea_urchin", "Sea Urchin", CreatureZone.DEEPER_REEF, 120, CreatureRenderFamily.SPIKY_URCHIN),
        beyond("creature_pufferfish", "Pufferfish", CreatureZone.DEEPER_REEF, 180, CreatureRenderFamily.PUFFERFISH),
        beyond("creature_lionfish", "Lionfish", CreatureZone.DEEPER_REEF, 240, CreatureRenderFamily.REEF_FISH),
        beyond("creature_moray_eel", "Moray Eel", CreatureZone.DEEPER_REEF, 300, CreatureRenderFamily.EEL),
        beyond("creature_stingray", "Stingray", CreatureZone.DEEPER_REEF, 360, CreatureRenderFamily.RAY),
        beyond(ShellContentCatalog.FOCUS_OCTOPUS, "Octopus", CreatureZone.DEEPER_REEF, 480, CreatureRenderFamily.OCTOPUS),
        beyond("creature_sea_snake", "Sea Snake", CreatureZone.DEEPER_REEF, 540, CreatureRenderFamily.SNAKE),
        beyond("creature_squid", "Squid", CreatureZone.DEEPER_REEF, 600, CreatureRenderFamily.SQUID),

        flow(ShellContentCatalog.FOCUS_MANTA, "Manta", CreatureZone.OPEN_BLUE, 60, CreatureRenderFamily.RAY),
        beyond("creature_flying_fish", "Flying Fish", CreatureZone.OPEN_BLUE, 120, CreatureRenderFamily.LARGE_FISH),
        beyond("creature_barracuda", "Barracuda", CreatureZone.OPEN_BLUE, 180, CreatureRenderFamily.LARGE_FISH),
        beyond("creature_swordfish", "Swordfish", CreatureZone.OPEN_BLUE, 240, CreatureRenderFamily.LARGE_FISH),
        beyond("creature_dolphin", "Dolphin", CreatureZone.OPEN_BLUE, 360, CreatureRenderFamily.DOLPHIN),
        beyond("creature_ocean_sunfish", "Ocean Sunfish", CreatureZone.OPEN_BLUE, 480, CreatureRenderFamily.SUNFISH),
        beyond("creature_penguin", "Penguin", CreatureZone.OPEN_BLUE, 540, CreatureRenderFamily.SURFACE_MAMMAL),
        beyond("creature_sea_lion", "Sea Lion", CreatureZone.OPEN_BLUE, 600, CreatureRenderFamily.SURFACE_MAMMAL),
        beyond("creature_orca", "Orca", CreatureZone.OPEN_BLUE, 720, CreatureRenderFamily.ORCA),
        beyond("creature_great_white_shark", "Great White Shark", CreatureZone.OPEN_BLUE, 960, CreatureRenderFamily.SHARK),

        flow(ShellContentCatalog.FOCUS_WHALE, "Whale", CreatureZone.GREAT_BLUE, 120, CreatureRenderFamily.WHALE),
        beyond("creature_anglerfish", "Anglerfish", CreatureZone.GREAT_BLUE, 300, CreatureRenderFamily.ANGLERFISH),
        beyond("creature_leatherback_turtle", "Leatherback Turtle", CreatureZone.GREAT_BLUE, 360, CreatureRenderFamily.TURTLE),
        beyond("creature_giant_squid", "Giant Squid", CreatureZone.GREAT_BLUE, 600, CreatureRenderFamily.GIANT_TENTACLE),
        beyond("creature_humpback_whale", "Humpback Whale", CreatureZone.GREAT_BLUE, 720, CreatureRenderFamily.WHALE),
        beyond("creature_blue_whale", "Blue Whale", CreatureZone.GREAT_BLUE, 900, CreatureRenderFamily.WHALE),
        beyond("creature_megalodon", "Megalodon", CreatureZone.GREAT_BLUE, 1200, CreatureRenderFamily.SHARK),
        beyond("creature_kraken", "Kraken", CreatureZone.GREAT_BLUE, 1500, CreatureRenderFamily.GIANT_TENTACLE),
        beyond("creature_leviathan", "Leviathan", CreatureZone.GREAT_BLUE, 1800, CreatureRenderFamily.LEGENDARY)
    )

    val byId: Map<String, CreatureDefinition> = all.associateBy { it.creatureId }
    val flowEarned: List<CreatureDefinition> = all.filter { it.sourceType == CreatureSourceType.FLOW_EARNED }
    val beyondBlue: List<CreatureDefinition> = all.filter { it.sourceType == CreatureSourceType.BEYOND_BLUE }

    fun get(creatureId: String): CreatureDefinition? = byId[creatureId]
    fun require(creatureId: String): CreatureDefinition = get(creatureId) ?: error("Unknown creature: $creatureId")

    private fun flow(id: String, name: String, zone: CreatureZone, minutes: Int, family: CreatureRenderFamily) =
        CreatureDefinition(id, name, zone, CreatureSourceType.FLOW_EARNED, flowTimeValueMinutes = minutes, staticIconKey = "creature_icon_$id", animatedRendererKey = "creature_renderer_${family.key}", renderFamily = family)

    private fun beyond(id: String, name: String, zone: CreatureZone, requirementMinutes: Int, family: CreatureRenderFamily) =
        CreatureDefinition(id, name, zone, CreatureSourceType.BEYOND_BLUE, requirementMinutes = requirementMinutes, staticIconKey = "creature_icon_$id", animatedRendererKey = "creature_renderer_${family.key}", renderFamily = family)
}

object CreatureEconomy {
    fun creaturesForRegularFlowMinutes(minutes: Int, isSoftFlow: Boolean = false): List<CreatureReward> {
        if (isSoftFlow || minutes < 10) return emptyList()
        var remaining = minutes
        val counts = linkedMapOf<String, Int>()
        fun take(chunk: Int, id: String) {
            val count = remaining / chunk
            if (count > 0) counts[id] = (counts[id] ?: 0) + count
            remaining %= chunk
        }
        take(120, ShellContentCatalog.FOCUS_WHALE)
        take(60, ShellContentCatalog.FOCUS_MANTA)
        take(30, ShellContentCatalog.FOCUS_SEAHORSE)
        take(10, ShellContentCatalog.FOCUS_MINNOW)
        val enduranceWhales = minutes / 150
        if (enduranceWhales > 0) counts[ShellContentCatalog.FOCUS_WHALE] = (counts[ShellContentCatalog.FOCUS_WHALE] ?: 0) + enduranceWhales
        return counts.map { CreatureReward(it.key, it.value) }
    }

    fun flowTimeValueMinutes(creatureId: String, level: Int = 1): Int =
        CreatureCatalog.require(creatureId).flowTimeValueMinutes
            ?: CreatureCatalog.require(creatureId).requirementMinutes
            ?: 0

    fun beyondBlueTradeContributionMinutes(creatureId: String, level: Int = 1): Int = flowTimeValueMinutes(creatureId, level)
    fun releaseValuePearls(creatureId: String, level: Int = 1): Int = flowTimeValueMinutes(creatureId, level)
    fun pearlPriceForRequirement(requirementMinutes: Int): Int = requirementMinutes * PEARLS_PER_REQUIRED_FLOW_MINUTE

    fun quoteBeyondBluePayment(targetCreatureId: String, selectedCreatureMinutes: Int, availablePearls: Int): CreaturePaymentQuote {
        val requirement = CreatureCatalog.require(targetCreatureId).requirementMinutes ?: error("Only Beyond Blue creatures have requirements.")
        val remaining = max(0, requirement - selectedCreatureMinutes)
        val cost = pearlPriceForRequirement(remaining)
        val overpay = max(0, selectedCreatureMinutes - requirement) * PEARLS_PER_EXTRA_FLOW_MINUTE
        return CreaturePaymentQuote(requirement, selectedCreatureMinutes, remaining, cost, overpay, availablePearls >= cost)
    }

    fun growthCostPearls(creatureId: String, currentLevel: Int): Int {
        val definition = CreatureCatalog.require(creatureId)
        val level = currentLevel.coerceAtLeast(1)
        return when (creatureId) {
            ShellContentCatalog.FOCUS_MINNOW -> 25 * level
            ShellContentCatalog.FOCUS_SEAHORSE -> 75 * level
            ShellContentCatalog.FOCUS_MANTA -> 200 * level
            ShellContentCatalog.FOCUS_WHALE -> 600 * level
            else -> max(25, (definition.requirementMinutes ?: definition.flowTimeValueMinutes ?: 25) * level)
        }
    }

    fun animalVisualScale(creatureId: String, level: Int): Float {
        val definition = CreatureCatalog.require(creatureId)
        val cap = when (creatureId) {
            ShellContentCatalog.FOCUS_MINNOW -> 1.65f
            ShellContentCatalog.FOCUS_SEAHORSE -> 1.55f
            ShellContentCatalog.FOCUS_MANTA -> 1.45f
            ShellContentCatalog.FOCUS_WHALE -> 1.30f
            else -> definition.renderFamily.visualCap
        }
        val normalizedLevel = level.coerceAtLeast(1)
        val curve = 1f + (ln(normalizedLevel.toDouble()) / ln(101.0)).toFloat() * 0.58f
        return min(cap, curve.coerceAtLeast(1f))
    }

    fun counts(entries: List<CreatureLedgerEntry>, creatureId: String): CreatureCounts {
        val matching = entries.filter { it.creatureId == creatureId }
        val active = matching.filter { it.status == CreatureStatus.ACTIVE }
        return CreatureCounts(
            activeCount = active.size,
            lifetimeCount = matching.size,
            releasedCount = matching.count { it.status == CreatureStatus.RELEASED },
            usedBeyondBlueCount = matching.count { it.status == CreatureStatus.USED_BEYOND_BLUE },
            highestLevel = active.maxOfOrNull { it.level.coerceAtLeast(1) } ?: 0,
            levelDistribution = active.groupingBy { it.level.coerceAtLeast(1) }.eachCount().toSortedMap()
        )
    }
}
