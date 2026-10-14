package com.kingkharnivore.skillz.utils.shell

enum class LandStillwaterHabitat(
    val displayName: String,
    override val zone: CreatureZone,
    override val dropCost: Long,
    override val level: Int
) : StillwaterContainer {
    PASTURE("Pasture", CreatureZone.GOLDEN_FIELDS, 15_000L, 1),
    GLADE("Glade", CreatureZone.ANCIENT_WOODS, 25_000L, 2),
    OASIS("Oasis", CreatureZone.OPEN_SANDS, 45_000L, 3),
    RAVINE("Ravine", CreatureZone.HIGH_PEAKS, 60_000L, 4),
    SANCTUARY("Sanctuary", CreatureZone.GREAT_WILD, 75_000L, 5)
}

/** Land roster, including preserved heritage species now available in The Blue. */
object LandCreatureCatalog {
    val mainZones = listOf(CreatureZone.GOLDEN_FIELDS, CreatureZone.ANCIENT_WOODS, CreatureZone.OPEN_SANDS, CreatureZone.HIGH_PEAKS, CreatureZone.GREAT_WILD)
    // Arc flagships share the premium growth tier of their zone: Horse, Black Bear,
    // Monitor Lizard, Polar Bear and Lion. These are growth bases, never purchase prices.
    val main: List<CreatureDefinition> = listOf(
        creature("creature_chicken", "Chicken", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.ARC_EARNED, CreatureRenderFamily.GROUND_BIRD, flows = 3, growthBasePearls = 720),
        creature("creature_duck", "Duck", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.GROUND_BIRD, minutes = 120),
        creature("creature_turkey", "Turkey", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.GROUND_BIRD, minutes = 150),
        creature("creature_goat", "Goat", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 180),
        creature("creature_sheep", "Sheep", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 240),
        creature("creature_pig", "Pig", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 300),
        creature("creature_alpaca", "Alpaca", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 420),
        creature("creature_llama", "Llama", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 480),
        creature("creature_donkey", "Donkey", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 540),
        creature("creature_cow", "Cow", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 600),
        creature("creature_horse", "Horse", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 720),
        creature("creature_deer", "Deer", CreatureZone.ANCIENT_WOODS, CreatureSourceType.ARC_EARNED, CreatureRenderFamily.HOOFED, flows = 6, growthBasePearls = 1_200),
        creature("creature_squirrel", "Squirrel", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 240),
        creature("creature_rabbit", "Rabbit", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 300),
        creature("creature_raccoon", "Raccoon", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 420),
        creature("creature_fox", "Fox", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.CANID, minutes = 540),
        creature("creature_porcupine", "Porcupine", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 600),
        creature("creature_wild_boar", "Wild Boar", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 660),
        creature("creature_badger", "Badger", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 780),
        creature("creature_red_panda", "Red Panda", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 900),
        creature("creature_chimpanzee", "Chimpanzee", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.PRIMATE, minutes = 960),
        creature("creature_orangutan", "Orangutan", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.PRIMATE, minutes = 1080),
        creature("creature_bobcat", "Bobcat", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.BIG_CAT, minutes = 1140),
        creature("creature_black_bear", "Black Bear", CreatureZone.ANCIENT_WOODS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.BEAR, minutes = 1200),
        creature("creature_camel", "Camel", CreatureZone.OPEN_SANDS, CreatureSourceType.ARC_EARNED, CreatureRenderFamily.HOOFED, flows = 9, growthBasePearls = 1_500),
        creature("creature_meerkat", "Meerkat", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 420),
        creature("creature_desert_tortoise", "Desert Tortoise", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.TORTOISE, minutes = 540),
        creature("creature_roadrunner", "Roadrunner", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.GROUND_BIRD, minutes = 600),
        creature("creature_fennec_fox", "Fennec Fox", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.CANID, minutes = 720),
        creature("creature_armadillo", "Armadillo", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 840),
        creature("creature_scorpion", "Scorpion", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SCORPION, minutes = 960),
        creature("creature_warthog", "Warthog", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 1020),
        creature("creature_rattlesnake", "Rattlesnake", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.LAND_SNAKE, minutes = 1080),
        creature("creature_jackal", "Jackal", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.CANID, minutes = 1140),
        creature("creature_coyote", "Coyote", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.CANID, minutes = 1200),
        creature("creature_ostrich", "Ostrich", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.GROUND_BIRD, minutes = 1260),
        creature("creature_vulture", "Vulture", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.RAPTOR, minutes = 1380),
        creature("creature_monitor_lizard", "Monitor Lizard", CreatureZone.OPEN_SANDS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.REPTILE, minutes = 1500),
        creature("creature_moose", "Moose", CreatureZone.HIGH_PEAKS, CreatureSourceType.ARC_EARNED, CreatureRenderFamily.HOOFED, flows = 12, growthBasePearls = 2_400),
        creature("creature_marmot", "Marmot", CreatureZone.HIGH_PEAKS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.SMALL_MAMMAL, minutes = 600),
        creature("creature_mountain_goat", "Mountain Goat", CreatureZone.HIGH_PEAKS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 720),
        creature("creature_bighorn_sheep", "Bighorn Sheep", CreatureZone.HIGH_PEAKS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 840),
        creature("creature_reindeer", "Reindeer", CreatureZone.HIGH_PEAKS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 960),
        creature("creature_arctic_fox", "Arctic Fox", CreatureZone.HIGH_PEAKS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.CANID, minutes = 1080),
        creature("creature_yak", "Yak", CreatureZone.HIGH_PEAKS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 1200),
        creature("creature_musk_ox", "Musk Ox", CreatureZone.HIGH_PEAKS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.HOOFED, minutes = 1500),
        creature("creature_snowy_owl", "Snowy Owl", CreatureZone.HIGH_PEAKS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.RAPTOR, minutes = 1800),
        creature("creature_eagle", "Eagle", CreatureZone.HIGH_PEAKS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.RAPTOR, minutes = 1950),
        creature("creature_snow_leopard", "Snow Leopard", CreatureZone.HIGH_PEAKS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.BIG_CAT, minutes = 2100),
        creature("creature_polar_bear", "Polar Bear", CreatureZone.HIGH_PEAKS, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.BEAR, minutes = 2400),
        creature("creature_tiger", "Tiger", CreatureZone.GREAT_WILD, CreatureSourceType.ARC_EARNED, CreatureRenderFamily.BIG_CAT, flows = 15, growthBasePearls = 3_000),
        creature("creature_spotted_hyena", "Spotted Hyena", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.CANID, minutes = 900),
        creature("creature_gray_wolf", "Gray Wolf", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.CANID, minutes = 1080),
        creature("creature_cheetah", "Cheetah", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.BIG_CAT, minutes = 1200),
        creature("creature_leopard", "Leopard", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.BIG_CAT, minutes = 1500),
        creature("creature_cougar", "Cougar", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.BIG_CAT, minutes = 1650),
        creature("creature_jaguar", "Jaguar", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.BIG_CAT, minutes = 1800),
        creature("creature_crocodile", "Crocodile", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.REPTILE, minutes = 2100),
        creature("creature_komodo_dragon", "Komodo Dragon", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.REPTILE, minutes = 2250),
        creature("creature_baboon", "Baboon", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.PRIMATE, minutes = 2325),
        creature("creature_gorilla", "Gorilla", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.PRIMATE, minutes = 2400),
        creature("creature_grizzly_bear", "Grizzly Bear", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.BEAR, minutes = 2700),
        creature("creature_lion", "Lion", CreatureZone.GREAT_WILD, CreatureSourceType.BEYOND_BLUE, CreatureRenderFamily.BIG_CAT, minutes = 3000),
    )
    val restorative: List<CreatureDefinition> = listOf(
        creature("creature_peacock", "Peacock", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.GROUND_BIRD, habitat = LandStillwaterHabitat.PASTURE),
        creature("creature_goose", "Goose", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.GROUND_BIRD, habitat = LandStillwaterHabitat.PASTURE),
        creature("creature_pheasant", "Pheasant", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.GROUND_BIRD, habitat = LandStillwaterHabitat.PASTURE),
        creature("creature_emu", "Emu", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.GROUND_BIRD, habitat = LandStillwaterHabitat.PASTURE),
        creature("creature_capybara", "Capybara", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.PASTURE),
        creature("creature_quail", "Quail", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.GROUND_BIRD, habitat = LandStillwaterHabitat.PASTURE),
        creature("creature_partridge", "Partridge", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.GROUND_BIRD, habitat = LandStillwaterHabitat.PASTURE),
        creature("creature_wallaby", "Wallaby", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.MARSUPIAL, habitat = LandStillwaterHabitat.PASTURE),
        creature("creature_kangaroo", "Kangaroo", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.MARSUPIAL, habitat = LandStillwaterHabitat.PASTURE),
        creature("creature_wombat", "Wombat", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.MARSUPIAL, habitat = LandStillwaterHabitat.PASTURE),
        creature("creature_echidna", "Echidna", CreatureZone.GOLDEN_FIELDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.PASTURE),
        creature("creature_hedgehog", "Hedgehog", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_sloth", "Sloth", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_pangolin", "Pangolin", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_kinkajou", "Kinkajou", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_lemur", "Lemur", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.PRIMATE, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_tapir", "Tapir", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_okapi", "Okapi", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_panda", "Panda", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.BEAR, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_koala", "Koala", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_mandrill", "Mandrill", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.PRIMATE, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_gibbon", "Gibbon", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.PRIMATE, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_platypus", "Platypus", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_chameleon", "Chameleon", CreatureZone.ANCIENT_WOODS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.REPTILE, habitat = LandStillwaterHabitat.GLADE),
        creature("creature_jerboa", "Jerboa", CreatureZone.OPEN_SANDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.OASIS),
        creature("creature_oryx", "Oryx", CreatureZone.OPEN_SANDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.OASIS),
        creature("creature_gazelle", "Gazelle", CreatureZone.OPEN_SANDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.OASIS),
        creature("creature_caracal", "Caracal", CreatureZone.OPEN_SANDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.BIG_CAT, habitat = LandStillwaterHabitat.OASIS),
        creature("creature_gerenuk", "Gerenuk", CreatureZone.OPEN_SANDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.OASIS),
        creature("creature_addax", "Addax", CreatureZone.OPEN_SANDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.OASIS),
        creature("creature_serval", "Serval", CreatureZone.OPEN_SANDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.BIG_CAT, habitat = LandStillwaterHabitat.OASIS),
        creature("creature_mongoose", "Mongoose", CreatureZone.OPEN_SANDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.OASIS),
        creature("creature_iguana", "Iguana", CreatureZone.OPEN_SANDS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.REPTILE, habitat = LandStillwaterHabitat.OASIS),
        creature("creature_pika", "Pika", CreatureZone.HIGH_PEAKS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.RAVINE),
        creature("creature_ibex", "Ibex", CreatureZone.HIGH_PEAKS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.RAVINE),
        creature("creature_chamois", "Chamois", CreatureZone.HIGH_PEAKS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.RAVINE),
        creature("creature_vicuna", "Vicuna", CreatureZone.HIGH_PEAKS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.RAVINE),
        creature("creature_takin", "Takin", CreatureZone.HIGH_PEAKS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.RAVINE),
        creature("creature_tahr", "Tahr", CreatureZone.HIGH_PEAKS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.RAVINE),
        creature("creature_macaque", "Macaque", CreatureZone.HIGH_PEAKS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.PRIMATE, habitat = LandStillwaterHabitat.RAVINE),
        creature("creature_wolverine", "Wolverine", CreatureZone.HIGH_PEAKS, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.RAVINE),
        creature("creature_elephant", "Elephant", CreatureZone.GREAT_WILD, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.MEGAFAUNA, habitat = LandStillwaterHabitat.SANCTUARY),
        creature("creature_rhinoceros", "Rhinoceros", CreatureZone.GREAT_WILD, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.MEGAFAUNA, habitat = LandStillwaterHabitat.SANCTUARY),
        creature("creature_giraffe", "Giraffe", CreatureZone.GREAT_WILD, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.MEGAFAUNA, habitat = LandStillwaterHabitat.SANCTUARY),
        creature("creature_bison", "Bison", CreatureZone.GREAT_WILD, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.SANCTUARY),
        creature("creature_hippopotamus", "Hippopotamus", CreatureZone.GREAT_WILD, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.MEGAFAUNA, habitat = LandStillwaterHabitat.SANCTUARY),
        creature("creature_anteater", "Anteater", CreatureZone.GREAT_WILD, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.SMALL_MAMMAL, habitat = LandStillwaterHabitat.SANCTUARY),
        creature("creature_zebra", "Zebra", CreatureZone.GREAT_WILD, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.SANCTUARY),
        creature("creature_lynx", "Lynx", CreatureZone.GREAT_WILD, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.BIG_CAT, habitat = LandStillwaterHabitat.SANCTUARY),
        creature("creature_wildebeest", "Wildebeest", CreatureZone.GREAT_WILD, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.SANCTUARY),
        creature("creature_buffalo", "Buffalo", CreatureZone.GREAT_WILD, CreatureSourceType.RESTORATIVE_LAND, CreatureRenderFamily.HOOFED, habitat = LandStillwaterHabitat.SANCTUARY),
    )
    val all = main + restorative
    val byId = all.associateBy { it.creatureId }

    private fun creature(id: String, name: String, zone: CreatureZone, source: CreatureSourceType,
        family: CreatureRenderFamily, minutes: Int? = null, flows: Int? = null,
        available: Boolean = true, habitat: LandStillwaterHabitat? = null,
        growthBasePearls: Int? = null) = CreatureDefinition(
        creatureId = id, displayName = name, zone = zone, sourceType = if (habitat != null) CreatureSourceType.BEYOND_BLUE else source,
        requirementMinutes = minutes ?: habitat?.let { (it.dropCost / 60L).toInt() }, arcFlowRequirement = flows, restorativeHabitat = habitat,
        baseGrowthCostPearls = growthBasePearls,
        // Economic value is separate from acquisition requirements: Arc depth is never minutes.
        economicValuePearls = when (id) {
            "creature_chicken" -> 30
            "creature_deer" -> 90
            "creature_camel" -> 180
            "creature_moose" -> 360
            "creature_tiger" -> 720 // Continue the Deer → Camel → Moose doubling progression.
            else -> habitat?.let { (it.dropCost / 60L).toInt() * PEARLS_PER_REQUIRED_FLOW_MINUTE }
        },
        staticIconKey = "creature_icon_$id", animatedRendererKey = "creature_renderer_$id",
        renderFamily = family,
        sceneBehavior = when (family) {
            CreatureRenderFamily.HOOFED -> CreatureSceneBehavior.GRAZE
            CreatureRenderFamily.BIG_CAT -> CreatureSceneBehavior.PROWL
            CreatureRenderFamily.RAPTOR -> CreatureSceneBehavior.PERCH
            CreatureRenderFamily.REPTILE, CreatureRenderFamily.LAND_SNAKE,
            CreatureRenderFamily.TORTOISE, CreatureRenderFamily.SCORPION -> CreatureSceneBehavior.CRAWL
            else -> CreatureSceneBehavior.WALK
        },
        placementBand = if (family == CreatureRenderFamily.RAPTOR) CreaturePlacementBand.CANOPY else CreaturePlacementBand.GROUND,
        scaleClass = when (family) {
            CreatureRenderFamily.MEGAFAUNA -> CreatureScaleClass.GIANT
            CreatureRenderFamily.BEAR, CreatureRenderFamily.BIG_CAT, CreatureRenderFamily.HOOFED -> CreatureScaleClass.LARGE
            CreatureRenderFamily.SMALL_MAMMAL, CreatureRenderFamily.SCORPION -> CreatureScaleClass.SMALL
            else -> CreatureScaleClass.MEDIUM
        },
        isAvailable = available && com.kingkharnivore.skillz.BuildConfig.LAND_ENABLED, participatesInCollector = available, participatesInCompletionist = available
    )
}

/** Input is the Arc engine's canonical completed Flow count, never minutes or lifetime Flows. */
object LandArcRewards {
    fun forFlowCount(flowCount: Int): List<CreatureReward> {
        require(flowCount >= 0) { "Arc Flow count cannot be negative." }
        return buildList {
            val tigers = flowCount / 15
            if (tigers > 0) add(CreatureReward("creature_tiger", tigers))
            val remainderId = when ((flowCount % 15) / 3) {
                1 -> "creature_chicken"
                2 -> "creature_deer"
                3 -> "creature_camel"
                4 -> "creature_moose"
                else -> null
            }
            if (remainderId != null) add(CreatureReward(remainderId, 1))
        }
    }
}
