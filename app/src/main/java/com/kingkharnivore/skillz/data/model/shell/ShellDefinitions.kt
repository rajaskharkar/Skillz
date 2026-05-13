package com.kingkharnivore.skillz.data.model.shell

import androidx.annotation.StringRes
import com.kingkharnivore.skillz.R

enum class ShellRoomId { HEART, FOCUS, STILLWATER, VOYAGE, CORAL_REEF, IDEA_GROVE, LOOKOUT }

data class ShellRoomDefinition(
    val roomId: ShellRoomId,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val iconKey: String,
    val backgroundAssetKey: String?,
    val slotTemplateId: String?
)

enum class ShellFindCategory { CREATURES, SHELLS, CORAL, PLANTS, TROPHIES, TRINKETS, DISCOVERIES }

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
    val acceptedSlotTypes: Set<ShellSlotType>
)

enum class ShellSlotType { REEF_SHELF, SHELL_WALL, CREATURE_PERCH, CORAL_BED, TIDEPOOL_EDGE, CURRENT_PATH, CENTERPIECE, MEMORY_NOOK }

data class ShellSlotDefinition(
    val slotId: String,
    val roomId: ShellRoomId,
    val slotType: ShellSlotType,
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
    const val FOCUS_GLOW_SHELL = "focus_glow_shell"
    const val FOCUS_CURRENT_CONCH = "focus_current_conch"
    const val FOCUS_ANCHOR_CORAL = "focus_anchor_coral"
    const val FOCUS_ABYSS_LANTERNFISH = "focus_abyss_lanternfish"
    const val FOCUS_THRESHOLD_SEAHORSE = "focus_threshold_seahorse"
    const val FOCUS_RETURN_TURTLE_STONE = "focus_return_turtle_stone"
    const val TRINKET_SEA_GLASS_SHARD = "trinket_sea_glass_shard"
    const val TRINKET_PEARL_CLUSTER = "trinket_pearl_cluster"

    val rooms = listOf(
        ShellRoomDefinition(ShellRoomId.HEART, R.string.shell_room_heart_title, R.string.shell_room_heart_description, "shell_heart", null, null),
        ShellRoomDefinition(ShellRoomId.FOCUS, R.string.shell_room_focus_title, R.string.shell_room_focus_description, "shell_focus", null, "focus_v1"),
        ShellRoomDefinition(ShellRoomId.STILLWATER, R.string.shell_room_stillwater_title, R.string.shell_room_stillwater_description, "shell_stillwater", null, null),
        ShellRoomDefinition(ShellRoomId.VOYAGE, R.string.shell_room_voyage_title, R.string.shell_room_voyage_description, "shell_voyage", null, null),
        ShellRoomDefinition(ShellRoomId.CORAL_REEF, R.string.shell_room_coral_title, R.string.shell_room_coral_description, "shell_coral", null, null),
        ShellRoomDefinition(ShellRoomId.IDEA_GROVE, R.string.shell_room_idea_title, R.string.shell_room_idea_description, "shell_idea", null, null),
        ShellRoomDefinition(ShellRoomId.LOOKOUT, R.string.shell_room_lookout_title, R.string.shell_room_lookout_description, "shell_lookout", null, null)
    )

    val finds = listOf(
        ShellFindDefinition(FOCUS_GLOW_SHELL, R.string.shell_find_glow_shell_title, R.string.shell_find_glow_shell_description, ShellFindCategory.SHELLS, ShellRoomId.FOCUS, "glow_shell", "glow_shell", true, true, false, setOf(ShellSlotType.REEF_SHELF, ShellSlotType.SHELL_WALL, ShellSlotType.CENTERPIECE)),
        ShellFindDefinition(FOCUS_CURRENT_CONCH, R.string.shell_find_current_conch_title, R.string.shell_find_current_conch_description, ShellFindCategory.SHELLS, ShellRoomId.FOCUS, "current_conch", "current_conch", true, true, false, setOf(ShellSlotType.REEF_SHELF, ShellSlotType.SHELL_WALL, ShellSlotType.MEMORY_NOOK)),
        ShellFindDefinition(FOCUS_ANCHOR_CORAL, R.string.shell_find_anchor_coral_title, R.string.shell_find_anchor_coral_description, ShellFindCategory.CORAL, ShellRoomId.FOCUS, "anchor_coral", "anchor_coral", true, true, false, setOf(ShellSlotType.CORAL_BED, ShellSlotType.CENTERPIECE)),
        ShellFindDefinition(FOCUS_ABYSS_LANTERNFISH, R.string.shell_find_abyss_lanternfish_title, R.string.shell_find_abyss_lanternfish_description, ShellFindCategory.CREATURES, ShellRoomId.FOCUS, "abyss_lanternfish", "abyss_lanternfish", true, true, false, setOf(ShellSlotType.CREATURE_PERCH, ShellSlotType.CENTERPIECE)),
        ShellFindDefinition(FOCUS_THRESHOLD_SEAHORSE, R.string.shell_find_threshold_seahorse_title, R.string.shell_find_threshold_seahorse_description, ShellFindCategory.CREATURES, ShellRoomId.FOCUS, "threshold_seahorse", "threshold_seahorse", true, false, false, setOf(ShellSlotType.CREATURE_PERCH, ShellSlotType.MEMORY_NOOK)),
        ShellFindDefinition(FOCUS_RETURN_TURTLE_STONE, R.string.shell_find_return_turtle_stone_title, R.string.shell_find_return_turtle_stone_description, ShellFindCategory.TRINKETS, ShellRoomId.FOCUS, "return_turtle_stone", "return_turtle_stone", true, false, false, setOf(ShellSlotType.MEMORY_NOOK, ShellSlotType.REEF_SHELF)),
        ShellFindDefinition(TRINKET_SEA_GLASS_SHARD, R.string.shell_find_sea_glass_title, R.string.shell_find_sea_glass_description, ShellFindCategory.TRINKETS, null, "sea_glass", "sea_glass", false, false, true, emptySet()),
        ShellFindDefinition(TRINKET_PEARL_CLUSTER, R.string.shell_find_pearl_cluster_title, R.string.shell_find_pearl_cluster_description, ShellFindCategory.TRINKETS, null, "pearl_cluster", "pearl_cluster", false, false, true, emptySet())
    )

    val focusSlots = listOf(
        ShellSlotDefinition("left_reef_shelf", ShellRoomId.FOCUS, ShellSlotType.REEF_SHELF, .18f, .34f, .28f, .14f, 2, setOf(ShellFindCategory.SHELLS, ShellFindCategory.TRINKETS)),
        ShellSlotDefinition("right_reef_shelf", ShellRoomId.FOCUS, ShellSlotType.REEF_SHELF, .82f, .36f, .28f, .14f, 2, setOf(ShellFindCategory.SHELLS, ShellFindCategory.TRINKETS)),
        ShellSlotDefinition("shell_wall_nook", ShellRoomId.FOCUS, ShellSlotType.SHELL_WALL, .50f, .20f, .26f, .14f, 1, setOf(ShellFindCategory.SHELLS)),
        ShellSlotDefinition("coral_bed", ShellRoomId.FOCUS, ShellSlotType.CORAL_BED, .26f, .72f, .30f, .16f, 4, setOf(ShellFindCategory.CORAL, ShellFindCategory.PLANTS)),
        ShellSlotDefinition("creature_perch_left", ShellRoomId.FOCUS, ShellSlotType.CREATURE_PERCH, .24f, .52f, .24f, .14f, 3, setOf(ShellFindCategory.CREATURES)),
        ShellSlotDefinition("creature_perch_right", ShellRoomId.FOCUS, ShellSlotType.CREATURE_PERCH, .76f, .54f, .24f, .14f, 3, setOf(ShellFindCategory.CREATURES)),
        ShellSlotDefinition("center_focus_nook", ShellRoomId.FOCUS, ShellSlotType.CENTERPIECE, .50f, .58f, .32f, .18f, 5, setOf(ShellFindCategory.SHELLS, ShellFindCategory.CORAL, ShellFindCategory.CREATURES, ShellFindCategory.TROPHIES)),
        ShellSlotDefinition("memory_nook", ShellRoomId.FOCUS, ShellSlotType.MEMORY_NOOK, .50f, .80f, .30f, .14f, 6, setOf(ShellFindCategory.SHELLS, ShellFindCategory.TRINKETS, ShellFindCategory.DISCOVERIES, ShellFindCategory.CREATURES))
    )

    val upgrades = listOf(
        ShellFindUpgradeDefinition("focus_glow_shell_form_1", FOCUS_GLOW_SHELL, 0, R.string.shell_find_glow_shell_title, R.string.shell_upgrade_glow_1_description, "glow_shell", "glow_shell", 0, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_glow_shell_form_2", FOCUS_GLOW_SHELL, 1, R.string.shell_upgrade_glow_2_title, R.string.shell_upgrade_glow_2_description, "bright_glow_shell", "bright_glow_shell", 80, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_glow_shell_form_3", FOCUS_GLOW_SHELL, 2, R.string.shell_upgrade_glow_3_title, R.string.shell_upgrade_glow_3_description, "living_glow_shell", "living_glow_shell", 160, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_current_conch_form_1", FOCUS_CURRENT_CONCH, 0, R.string.shell_find_current_conch_title, R.string.shell_upgrade_conch_1_description, "current_conch", "current_conch", 0, R.string.shell_action_enrich),
        ShellFindUpgradeDefinition("focus_current_conch_form_2", FOCUS_CURRENT_CONCH, 1, R.string.shell_upgrade_conch_2_title, R.string.shell_upgrade_conch_2_description, "singing_current_conch", "singing_current_conch", 100, R.string.shell_action_enrich),
        ShellFindUpgradeDefinition("focus_current_conch_form_3", FOCUS_CURRENT_CONCH, 2, R.string.shell_upgrade_conch_3_title, R.string.shell_upgrade_conch_3_description, "echoing_current_conch", "echoing_current_conch", 200, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_anchor_coral_form_1", FOCUS_ANCHOR_CORAL, 0, R.string.shell_find_anchor_coral_title, R.string.shell_upgrade_coral_1_description, "anchor_coral", "anchor_coral", 0, R.string.shell_action_grow),
        ShellFindUpgradeDefinition("focus_anchor_coral_form_2", FOCUS_ANCHOR_CORAL, 1, R.string.shell_upgrade_coral_2_title, R.string.shell_upgrade_coral_2_description, "rooted_anchor_coral", "rooted_anchor_coral", 120, R.string.shell_action_grow),
        ShellFindUpgradeDefinition("focus_anchor_coral_form_3", FOCUS_ANCHOR_CORAL, 2, R.string.shell_upgrade_coral_3_title, R.string.shell_upgrade_coral_3_description, "guardian_anchor_coral", "guardian_anchor_coral", 240, R.string.shell_action_awaken),
        ShellFindUpgradeDefinition("focus_abyss_lanternfish_form_1", FOCUS_ABYSS_LANTERNFISH, 0, R.string.shell_find_abyss_lanternfish_title, R.string.shell_upgrade_lantern_1_description, "abyss_lanternfish", "abyss_lanternfish", 0, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_abyss_lanternfish_form_2", FOCUS_ABYSS_LANTERNFISH, 1, R.string.shell_upgrade_lantern_2_title, R.string.shell_upgrade_lantern_2_description, "radiant_abyss_lanternfish", "radiant_abyss_lanternfish", 180, R.string.shell_action_brighten),
        ShellFindUpgradeDefinition("focus_abyss_lanternfish_form_3", FOCUS_ABYSS_LANTERNFISH, 2, R.string.shell_upgrade_lantern_3_title, R.string.shell_upgrade_lantern_3_description, "elder_abyss_lanternfish", "elder_abyss_lanternfish", 320, R.string.shell_action_awaken)
    )

    val badges = listOf(
        BadgeDefinition("badge_flow_10_min", R.string.shell_badge_flow_10_title, R.string.shell_badge_flow_10_description, "badge_current_10", BadgeCategory.FLOW),
        BadgeDefinition("badge_flow_30_min", R.string.shell_badge_flow_30_title, R.string.shell_badge_flow_30_description, "badge_current_30", BadgeCategory.FLOW),
        BadgeDefinition("badge_flow_60_min", R.string.shell_badge_flow_60_title, R.string.shell_badge_flow_60_description, "badge_current_60", BadgeCategory.FLOW),
        BadgeDefinition("badge_flow_120_min", R.string.shell_badge_flow_120_title, R.string.shell_badge_flow_120_description, "badge_current_120", BadgeCategory.FLOW),
        BadgeDefinition("badge_soft_flow", R.string.shell_badge_soft_title, R.string.shell_badge_soft_description, "badge_soft", BadgeCategory.SOFT_FLOW),
        BadgeDefinition("badge_discovery", R.string.shell_badge_discovery_title, R.string.shell_badge_discovery_description, "badge_discovery", BadgeCategory.DISCOVERY)
    )

    val discoveries = listOf(
        DiscoveryDefinition("discovery_sea_glass_shard", R.string.shell_find_sea_glass_title, R.string.shell_discovery_sea_glass_reveal, R.string.shell_discovery_sea_glass_explanation, null, TRINKET_SEA_GLASS_SHARD, "sea_glass", false),
        DiscoveryDefinition("discovery_pearl_cluster", R.string.shell_find_pearl_cluster_title, R.string.shell_discovery_pearl_cluster_reveal, R.string.shell_discovery_pearl_cluster_explanation, null, TRINKET_PEARL_CLUSTER, "pearl_cluster", false),
        DiscoveryDefinition("discovery_threshold_seahorse", R.string.shell_find_threshold_seahorse_title, R.string.shell_discovery_threshold_reveal, R.string.shell_discovery_threshold_explanation, ShellRoomId.FOCUS, FOCUS_THRESHOLD_SEAHORSE, "threshold_seahorse", true),
        DiscoveryDefinition("discovery_return_turtle_stone", R.string.shell_find_return_turtle_stone_title, R.string.shell_discovery_return_reveal, R.string.shell_discovery_return_explanation, ShellRoomId.FOCUS, FOCUS_RETURN_TURTLE_STONE, "return_turtle_stone", true)
    )

    fun find(findId: String) = finds.firstOrNull { it.findId == findId }
    fun badge(badgeId: String) = badges.firstOrNull { it.badgeId == badgeId }
    fun discovery(discoveryId: String) = discoveries.firstOrNull { it.discoveryId == discoveryId }
    fun upgradesFor(findId: String) = upgrades.filter { it.findId == findId }.sortedBy { it.orderIndex }
    fun nextUpgrade(findId: String, currentStageId: String?) = upgradesFor(findId).let { stages ->
        val currentIndex = stages.indexOfFirst { it.upgradeStageId == currentStageId }.takeIf { it >= 0 } ?: 0
        stages.getOrNull(currentIndex + 1)
    }
}
