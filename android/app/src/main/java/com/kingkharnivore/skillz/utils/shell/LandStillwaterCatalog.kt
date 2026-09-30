package com.kingkharnivore.skillz.utils.shell

import kotlin.random.Random

/** Explicit species tiers; draw odds and repeat ownership are shared with Sea Stillwater. */
object LandStillwaterCatalog {
    val rarityById: Map<String, StillwaterRarity> = buildMap {
        fun tier(rarity: StillwaterRarity, vararg species: String) {
            species.forEach { put("creature_$it", rarity) }
        }
        tier(StillwaterRarity.COMMON,
            "goose", "quail", "partridge", "wombat",
            "hedgehog", "sloth", "koala", "chameleon", "macaque",
            "jerboa", "gazelle", "mongoose", "iguana", "pika", "chamois", "tahr",
            "bison", "zebra", "wildebeest", "buffalo")
        tier(StillwaterRarity.UNCOMMON,
            "pheasant", "emu", "capybara", "wallaby", "echidna",
            "kinkajou", "lemur", "tapir", "mandrill", "gibbon", "pangolin", "platypus",
            "oryx", "caracal", "gerenuk", "ibex", "vicuna",
            "giraffe", "hippopotamus", "anteater", "lynx")
        tier(StillwaterRarity.RARE, "kangaroo", "okapi", "serval", "wolverine", "rhinoceros")
        tier(StillwaterRarity.MYTHIC, "peacock", "panda", "addax", "takin", "elephant")
    }

    fun creaturesFor(habitat: LandStillwaterHabitat): List<CreatureDefinition> =
        LandCreatureCatalog.restorative.filter { it.restorativeHabitat == habitat }

    fun roll(habitat: LandStillwaterHabitat, random: Random = Random.Default): CreatureDefinition {
        val rarity = rollStillwaterRarity(random)
        val creatures = creaturesFor(habitat)
        val pool = creatures.filter { rarityById[it.creatureId] == rarity }.ifEmpty { creatures }
        return pool[random.nextInt(pool.size)]
    }
}
