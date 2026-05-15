package com.kingkharnivore.skillz.data.model.shell

import androidx.annotation.StringRes
import com.kingkharnivore.skillz.R

enum class ShellRoomId { HEART, FOCUS, STILLWATER, VOYAGE, THE_BLUE, IDEA_GROVE, LOOKOUT }

data class ShellRoomDefinition(
    val roomId: ShellRoomId,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val iconKey: String,
    val backgroundAssetKey: String?,
    val slotTemplateId: String?
)

enum class ShellFindCategory { CREATURES, SHELLS, CORAL, PLANTS, TROPHIES, TRINKETS, DISCOVERIES }
enum class ShellRewardKind { ANIMAL, OBJECT, TRINKET, DISCOVERY }
enum class ShellDepthTier { REEF, DEEPER_REEF, OPEN_BLUE, DEEP_OCEAN }

data class ShellFindDefinition(
    val findId: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val category: ShellFindCategory,
    val primaryRoomId: ShellRoomId?,
    val iconKey: String,
    val assetKey: String,
    val placeable: Boolean,
    val upgradeable: Boolean,
    val stackable: Boolean,
    val acceptedSlotTypes: Set<ShellSlotType>,
    val pearlCost: Int? = null,
    val isPearlObject: Boolean = false,
    val kind: ShellRewardKind = ShellRewardKind.OBJECT,
    val depthTier: ShellDepthTier? = null
)

enum class ShellSlotType { REEF_SHELF, SHELL_WALL, CREATURE_PERCH, CORAL_BED, TIDEPOOL_EDGE, CURRENT_PATH, SURGE_CURRENT, CENTERPIECE, MEMORY_NOOK }

data class ShellSlotDefinition(
    val slotId: String,
    val roomId: ShellRoomId,
    val slotType: ShellSlotType,
    @StringRes val titleRes: Int,
    val anchorX: Float,
    val anchorY: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val zIndex: Int,
    val acceptsCategories: Set<ShellFindCategory>
)

data class ShellFindUpgradeDefinition(
    val upgradeStageId: String,
    val findId: String,
    val orderIndex: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val iconKey: String,
    val assetKey: String,
    val pearlCost: Int,
    @StringRes val upgradeVerbRes: Int
)

enum class BadgeCategory { FLOW, SOFT_FLOW, ARC, SURGE, PULSE, DISCOVERY }

data class BadgeDefinition(
    val badgeId: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val iconKey: String,
    val category: BadgeCategory
)

data class DiscoveryDefinition(
    val discoveryId: String,
    @StringRes val titleRes: Int,
    @StringRes val revealCopyRes: Int,
    @StringRes val explanationRes: Int,
    val roomId: ShellRoomId?,
    val grantsFindId: String?,
    val iconKey: String,
    val oncePerUser: Boolean
)

enum class StillwaterPerspective { CUPS, BOWLS, TANK, POOL, LAKE, LAKE_TAHOE_PERCENT, WORLD_OCEAN_PERCENT, STREAM_TIME }

object ShellContentCatalog {
    const val FOCUS_MINNOW = "focus_minnow"
    const val FOCUS_SEAHORSE = "focus_seahorse"
    const val FOCUS_MANTA = "focus_manta"
    const val FOCUS_WHALE = "focus_whale"
    const val FOCUS_OCTOPUS = "focus_octopus"
    const val FOCUS_PEBBLE = "focus_pebble"
    const val TRINKET_SEA_GLASS_SHARD = "trinket_sea_glass_shard"
    const val TRINKET_GLIMMER = "trinket_glimmer"
    const val FOCUS_LAMP = "focus_lamp"
    const val FOCUS_PERCH = "focus_perch"
    const val FOCUS_PEBBLES = "focus_pebbles"
    const val FOCUS_CURTAIN = "focus_curtain"
    const val FOCUS_BUBBLES = "focus_bubbles"

    val rooms = listOf(
        ShellRoomDefinition(ShellRoomId.HEART, R.string.shell_room_heart_title, R.string.shell_room_heart_description, "shell_heart", null, null),
        ShellRoomDefinition(ShellRoomId.FOCUS, R.string.shell_room_focus_title, R.string.shell_room_focus_description, "shell_focus", null, "focus_v1"),
        ShellRoomDefinition(ShellRoomId.STILLWATER, R.string.shell_room_stillwater_title, R.string.shell_room_stillwater_description, "shell_stillwater", null, null),
        ShellRoomDefinition(ShellRoomId.VOYAGE, R.string.shell_room_voyage_title, R.string.shell_room_voyage_description, "shell_voyage", null, null),
        ShellRoomDefinition(ShellRoomId.THE_BLUE, R.string.shell_room_the_blue_title, R.string.shell_room_the_blue_description, "shell_the_blue", null, null),
        ShellRoomDefinition(ShellRoomId.IDEA_GROVE, R.string.shell_room_idea_title, R.string.shell_room_idea_description, "shell_idea", null, null),
        ShellRoomDefinition(ShellRoomId.LOOKOUT, R.string.shell_room_lookout_title, R.string.shell_room_lookout_description, "shell_lookout", null, null)
    )

    val finds = listOf(
        ShellFindDefinition(FOCUS_MINNOW, R.string.shell_find_minnow_title, R.string.shell_find_minnow_description, ShellFindCategory.CREATURES, ShellRoomId.FOCUS, "minnow", "minnow", true, true, false, setOf(ShellSlotType.CREATURE_PERCH, ShellSlotType.TIDEPOOL_EDGE), kind = ShellRewardKind.ANIMAL, depthTier = ShellDepthTier.REEF),
        ShellFindDefinition(FOCUS_SEAHORSE, R.string.shell_find_seahorse_title, R.string.shell_find_seahorse_description, ShellFindCategory.CREATURES, ShellRoomId.FOCUS, "seahorse", "seahorse", true, true, false, setOf(ShellSlotType.CREATURE_PERCH, ShellSlotType.TIDEPOOL_EDGE), kind = ShellRewardKind.ANIMAL, depthTier = ShellDepthTier.DEEPER_REEF),
        ShellFindDefinition(FOCUS_MANTA, R.string.shell_find_manta_title, R.string.shell_find_manta_description, ShellFindCategory.CREATURES, ShellRoomId.FOCUS, "manta", "manta", true, true, false, setOf(ShellSlotType.CURRENT_PATH, ShellSlotType.CENTERPIECE, ShellSlotType.TIDEPOOL_EDGE), kind = ShellRewardKind.ANIMAL, depthTier = ShellDepthTier.OPEN_BLUE),
        ShellFindDefinition(FOCUS_WHALE, R.string.shell_find_whale_title, R.string.shell_find_whale_description, ShellFindCategory.CREATURES, ShellRoomId.FOCUS, "whale", "whale", true, true, false, setOf(ShellSlotType.CURRENT_PATH, ShellSlotType.CENTERPIECE), kind = ShellRewardKind.ANIMAL, depthTier = ShellDepthTier.DEEP_OCEAN),
        ShellFindDefinition(FOCUS_OCTOPUS, R.string.shell_find_octopus_title, R.string.shell_find_octopus_description, ShellFindCategory.CREATURES, ShellRoomId.FOCUS, "octopus", "octopus", true, true, false, setOf(ShellSlotType.CREATURE_PERCH, ShellSlotType.MEMORY_NOOK), kind = ShellRewardKind.ANIMAL),
        ShellFindDefinition(FOCUS_PEBBLE, R.string.shell_find_pebble_title, R.string.shell_find_pebble_description, ShellFindCategory.TRINKETS, ShellRoomId.FOCUS, "pebble", "pebble", true, true, false, setOf(ShellSlotType.MEMORY_NOOK, ShellSlotType.REEF_SHELF), kind = ShellRewardKind.OBJECT),
        ShellFindDefinition(TRINKET_SEA_GLASS_SHARD, R.string.shell_find_sea_glass_title, R.string.shell_find_sea_glass_description, ShellFindCategory.TRINKETS, null, "sea_glass", "sea_glass", false, false, true, emptySet(), kind = ShellRewardKind.TRINKET),
        ShellFindDefinition(TRINKET_GLIMMER, R.string.shell_find_glimmer_title, R.string.shell_find_glimmer_description, ShellFindCategory.TRINKETS, null, "glimmer", "glimmer", false, false, true, emptySet(), kind = ShellRewardKind.TRINKET),
        ShellFindDefinition(FOCUS_LAMP, R.string.shell_object_lamp_title, R.string.shell_object_lamp_description, ShellFindCategory.CORAL, ShellRoomId.FOCUS, "lamp", "lamp", true, true, false, setOf(ShellSlotType.SHELL_WALL, ShellSlotType.CORAL_BED, ShellSlotType.CENTERPIECE), 80, true, ShellRewardKind.OBJECT),
        ShellFindDefinition(FOCUS_PERCH, R.string.shell_object_perch_title, R.string.shell_object_perch_description, ShellFindCategory.CREATURES, ShellRoomId.FOCUS, "perch", "perch", true, false, false, setOf(ShellSlotType.CREATURE_PERCH, ShellSlotType.MEMORY_NOOK), 120, true, ShellRewardKind.OBJECT),
        ShellFindDefinition(FOCUS_PEBBLES, R.string.shell_object_pebbles_title, R.string.shell_object_pebbles_description, ShellFindCategory.TRINKETS, ShellRoomId.FOCUS, "pebbles", "pebbles", true, false, false, setOf(ShellSlotType.REEF_SHELF, ShellSlotType.MEMORY_NOOK), 60, true, ShellRewardKind.OBJECT),
        ShellFindDefinition(FOCUS_CURTAIN, R.string.shell_object_curtain_title, R.string.shell_object_curtain_description, ShellFindCategory.PLANTS, ShellRoomId.FOCUS, "curtain", "curtain", true, false, false, setOf(ShellSlotType.CORAL_BED, ShellSlotType.SHELL_WALL), 140, true, ShellRewardKind.OBJECT),
        ShellFindDefinition(FOCUS_BUBBLES, R.string.shell_object_bubbles_title, R.string.shell_object_bubbles_description, ShellFindCategory.TRINKETS, ShellRoomId.FOCUS, "bubbles", "bubbles", true, false, false, setOf(ShellSlotType.CURRENT_PATH, ShellSlotType.CENTERPIECE, ShellSlotType.MEMORY_NOOK), 100, true, ShellRewardKind.OBJECT)
    )

    val focusSlots = listOf(
        ShellSlotDefinition("left_reef_shelf", ShellRoomId.FOCUS, ShellSlotType.REEF_SHELF, R.string.shell_slot_left_reef_shelf, .18f, .34f, .28f, .14f, 2, setOf(ShellFindCategory.SHELLS, ShellFindCategory.TRINKETS, ShellFindCategory.CORAL)),
        ShellSlotDefinition("right_reef_shelf", ShellRoomId.FOCUS, ShellSlotType.REEF_SHELF, R.string.shell_slot_right_reef_shelf, .82f, .36f, .28f, .14f, 2, setOf(ShellFindCategory.SHELLS, ShellFindCategory.TRINKETS, ShellFindCategory.CORAL)),
        ShellSlotDefinition("shell_wall_nook", ShellRoomId.FOCUS, ShellSlotType.SHELL_WALL, R.string.shell_slot_shell_wall_nook, .50f, .20f, .26f, .14f, 1, setOf(ShellFindCategory.SHELLS, ShellFindCategory.CORAL, ShellFindCategory.PLANTS)),
        ShellSlotDefinition("coral_bed", ShellRoomId.FOCUS, ShellSlotType.CORAL_BED, R.string.shell_slot_coral_bed, .26f, .72f, .30f, .16f, 4, setOf(ShellFindCategory.CORAL, ShellFindCategory.PLANTS)),
        ShellSlotDefinition("creature_perch_left", ShellRoomId.FOCUS, ShellSlotType.CREATURE_PERCH, R.string.shell_slot_creature_perch_left, .24f, .52f, .24f, .14f, 3, setOf(ShellFindCategory.CREATURES)),
        ShellSlotDefinition("creature_perch_right", ShellRoomId.FOCUS, ShellSlotType.CREATURE_PERCH, R.string.shell_slot_creature_perch_right, .76f, .54f, .24f, .14f, 3, setOf(ShellFindCategory.CREATURES)),
        ShellSlotDefinition("center_focus_nook", ShellRoomId.FOCUS, ShellSlotType.CENTERPIECE, R.string.shell_slot_center_focus_nook, .50f, .58f, .32f, .18f, 5, setOf(ShellFindCategory.SHELLS, ShellFindCategory.CORAL, ShellFindCategory.CREATURES, ShellFindCategory.TROPHIES, ShellFindCategory.TRINKETS)),
        ShellSlotDefinition("surge_current_nook", ShellRoomId.FOCUS, ShellSlotType.SURGE_CURRENT, R.string.shell_slot_surge_current_nook, .74f, .72f, .30f, .16f, 6, setOf(ShellFindCategory.TROPHIES)),
        ShellSlotDefinition("memory_nook", ShellRoomId.FOCUS, ShellSlotType.MEMORY_NOOK, R.string.shell_slot_memory_nook, .50f, .82f, .30f, .14f, 7, setOf(ShellFindCategory.SHELLS, ShellFindCategory.TRINKETS, ShellFindCategory.DISCOVERIES, ShellFindCategory.CREATURES))
    )

    val upgrades = listOf(
        ShellFindUpgradeDefinition("focus_minnow_base", FOCUS_MINNOW, 0, R.string.shell_form_base, R.string.shell_upgrade_minnow_base_description, "minnow", "minnow", 0, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_minnow_bright", FOCUS_MINNOW, 1, R.string.shell_form_bright, R.string.shell_upgrade_minnow_bright_description, "minnow_bright", "minnow_bright", 80, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_minnow_glimmer", FOCUS_MINNOW, 2, R.string.shell_form_glimmer, R.string.shell_upgrade_minnow_glimmer_description, "minnow_glimmer", "minnow_glimmer", 160, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_minnow_luminous", FOCUS_MINNOW, 3, R.string.shell_form_luminous, R.string.shell_upgrade_minnow_luminous_description, "minnow_luminous", "minnow_luminous", 260, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_seahorse_base", FOCUS_SEAHORSE, 0, R.string.shell_form_base, R.string.shell_upgrade_seahorse_base_description, "seahorse", "seahorse", 0, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_seahorse_bright", FOCUS_SEAHORSE, 1, R.string.shell_form_bright, R.string.shell_upgrade_seahorse_bright_description, "seahorse_bright", "seahorse_bright", 100, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_seahorse_glimmer", FOCUS_SEAHORSE, 2, R.string.shell_form_glimmer, R.string.shell_upgrade_seahorse_glimmer_description, "seahorse_glimmer", "seahorse_glimmer", 200, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_seahorse_crowned", FOCUS_SEAHORSE, 3, R.string.shell_form_crowned, R.string.shell_upgrade_seahorse_crowned_description, "seahorse_crowned", "seahorse_crowned", 320, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_manta_base", FOCUS_MANTA, 0, R.string.shell_form_base, R.string.shell_upgrade_manta_base_description, "manta", "manta", 0, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_manta_bright", FOCUS_MANTA, 1, R.string.shell_form_bright, R.string.shell_upgrade_manta_bright_description, "manta_bright", "manta_bright", 120, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_manta_moonlit", FOCUS_MANTA, 2, R.string.shell_form_moonlit, R.string.shell_upgrade_manta_moonlit_description, "manta_moonlit", "manta_moonlit", 240, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_manta_radiant", FOCUS_MANTA, 3, R.string.shell_form_radiant, R.string.shell_upgrade_manta_radiant_description, "manta_radiant", "manta_radiant", 380, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_whale_base", FOCUS_WHALE, 0, R.string.shell_form_base, R.string.shell_upgrade_whale_base_description, "whale", "whale", 0, R.string.shell_action_deepen),
        ShellFindUpgradeDefinition("focus_whale_deep", FOCUS_WHALE, 1, R.string.shell_form_deep, R.string.shell_upgrade_whale_deep_description, "whale_deep", "whale_deep", 180, R.string.shell_action_deepen),
        ShellFindUpgradeDefinition("focus_whale_elder", FOCUS_WHALE, 2, R.string.shell_form_elder, R.string.shell_upgrade_whale_elder_description, "whale_elder", "whale_elder", 320, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_whale_ancient", FOCUS_WHALE, 3, R.string.shell_form_ancient, R.string.shell_upgrade_whale_ancient_description, "whale_ancient", "whale_ancient", 520, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_octopus_base", FOCUS_OCTOPUS, 0, R.string.shell_form_base, R.string.shell_upgrade_octopus_base_description, "octopus", "octopus", 0, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_octopus_glimmer", FOCUS_OCTOPUS, 1, R.string.shell_form_glimmer, R.string.shell_upgrade_octopus_glimmer_description, "octopus_glimmer", "octopus_glimmer", 120, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_octopus_crowned", FOCUS_OCTOPUS, 2, R.string.shell_form_crowned, R.string.shell_upgrade_octopus_crowned_description, "octopus_crowned", "octopus_crowned", 260, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_octopus_oracle", FOCUS_OCTOPUS, 3, R.string.shell_form_oracle, R.string.shell_upgrade_octopus_oracle_description, "octopus_oracle", "octopus_oracle", 420, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_pebble_base", FOCUS_PEBBLE, 0, R.string.shell_form_base, R.string.shell_upgrade_pebble_base_description, "pebble", "pebble", 0, R.string.shell_action_polish),
        ShellFindUpgradeDefinition("focus_pebble_polished", FOCUS_PEBBLE, 1, R.string.shell_form_polished, R.string.shell_upgrade_pebble_polished_description, "pebble_polished", "pebble_polished", 60, R.string.shell_action_polish),
        ShellFindUpgradeDefinition("focus_pebble_tidemarked", FOCUS_PEBBLE, 2, R.string.shell_form_tidemarked, R.string.shell_upgrade_pebble_tidemarked_description, "pebble_tidemarked", "pebble_tidemarked", 140, R.string.shell_action_polish),
        ShellFindUpgradeDefinition("focus_pebble_moonlit", FOCUS_PEBBLE, 3, R.string.shell_form_moonlit, R.string.shell_upgrade_pebble_moonlit_description, "pebble_moonlit", "pebble_moonlit", 260, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_lamp_base", FOCUS_LAMP, 0, R.string.shell_form_base, R.string.shell_upgrade_lamp_base_description, "lamp", "lamp", 0, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_lamp_bright", FOCUS_LAMP, 1, R.string.shell_form_bright, R.string.shell_upgrade_lamp_bright_description, "lamp_bright", "lamp_bright", 80, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_lamp_moonlit", FOCUS_LAMP, 2, R.string.shell_form_moonlit, R.string.shell_upgrade_lamp_moonlit_description, "lamp_moonlit", "lamp_moonlit", 160, R.string.shell_action_awaken)
    )

    val badges = listOf(
        BadgeDefinition("badge_flow_10_min", R.string.shell_badge_flow_10_title, R.string.shell_badge_flow_10_description, "badge_current_10", BadgeCategory.FLOW),
        BadgeDefinition("badge_flow_30_min", R.string.shell_badge_flow_30_title, R.string.shell_badge_flow_30_description, "badge_current_30", BadgeCategory.FLOW),
        BadgeDefinition("badge_flow_60_min", R.string.shell_badge_flow_60_title, R.string.shell_badge_flow_60_description, "badge_current_60", BadgeCategory.FLOW),
        BadgeDefinition("badge_flow_120_min", R.string.shell_badge_flow_120_title, R.string.shell_badge_flow_120_description, "badge_current_120", BadgeCategory.FLOW),
        BadgeDefinition("badge_discovery", R.string.shell_badge_discovery_title, R.string.shell_badge_discovery_description, "badge_discovery", BadgeCategory.DISCOVERY)
    )

    val discoveries = listOf(
        DiscoveryDefinition("discovery_sea_glass_shard", R.string.shell_find_sea_glass_title, R.string.shell_discovery_sea_glass_reveal, R.string.shell_discovery_sea_glass_explanation, null, TRINKET_SEA_GLASS_SHARD, "sea_glass", false),
        DiscoveryDefinition("discovery_glimmer", R.string.shell_find_glimmer_title, R.string.shell_discovery_glimmer_reveal, R.string.shell_discovery_glimmer_explanation, null, TRINKET_GLIMMER, "glimmer", false),
        DiscoveryDefinition("discovery_octopus", R.string.shell_find_octopus_title, R.string.shell_discovery_octopus_reveal, R.string.shell_discovery_octopus_explanation, ShellRoomId.FOCUS, FOCUS_OCTOPUS, "octopus", true),
        DiscoveryDefinition("discovery_pebble", R.string.shell_find_pebble_title, R.string.shell_discovery_pebble_reveal, R.string.shell_discovery_pebble_explanation, ShellRoomId.FOCUS, FOCUS_PEBBLE, "pebble", true)
    )

    private val surgeRewardIds = emptySet<String>()

    fun isCompatibleWithSlot(slot: ShellSlotDefinition, definition: ShellFindDefinition): Boolean {
        if (slot.slotType == ShellSlotType.SURGE_CURRENT) return definition.findId in surgeRewardIds
        if (slot.slotType !in definition.acceptedSlotTypes || definition.category !in slot.acceptsCategories) return false
        return when (slot.slotType) {
            ShellSlotType.CREATURE_PERCH -> definition.kind == ShellRewardKind.ANIMAL || definition.findId == FOCUS_PERCH
            ShellSlotType.CURRENT_PATH -> definition.kind == ShellRewardKind.ANIMAL || definition.findId == FOCUS_BUBBLES
            ShellSlotType.SURGE_CURRENT -> definition.findId in surgeRewardIds
            ShellSlotType.TIDEPOOL_EDGE -> definition.kind == ShellRewardKind.ANIMAL
            ShellSlotType.MEMORY_NOOK -> definition.kind in setOf(ShellRewardKind.OBJECT, ShellRewardKind.TRINKET, ShellRewardKind.DISCOVERY) || definition.findId == FOCUS_OCTOPUS
            ShellSlotType.REEF_SHELF -> definition.kind in setOf(ShellRewardKind.OBJECT, ShellRewardKind.TRINKET, ShellRewardKind.DISCOVERY)
            ShellSlotType.SHELL_WALL -> definition.kind == ShellRewardKind.OBJECT
            ShellSlotType.CORAL_BED -> definition.kind == ShellRewardKind.OBJECT
            ShellSlotType.CENTERPIECE -> definition.kind in setOf(ShellRewardKind.ANIMAL, ShellRewardKind.OBJECT)
        }
    }

    val focusPearlObjects = finds.filter { it.isPearlObject && it.primaryRoomId == ShellRoomId.FOCUS }
    fun find(findId: String) = finds.firstOrNull { it.findId == findId }
    fun badge(badgeId: String) = badges.firstOrNull { it.badgeId == badgeId }
    fun discovery(discoveryId: String) = discoveries.firstOrNull { it.discoveryId == discoveryId }
    fun upgradesFor(findId: String) = upgrades.filter { it.findId == findId }.sortedBy { it.orderIndex }
    fun nextUpgrade(findId: String, currentStageId: String?) = upgradesFor(findId).let { stages ->
        val currentIndex = stages.indexOfFirst { it.upgradeStageId == currentStageId }.takeIf { it >= 0 } ?: 0
        stages.getOrNull(currentIndex + 1)
    }
}
