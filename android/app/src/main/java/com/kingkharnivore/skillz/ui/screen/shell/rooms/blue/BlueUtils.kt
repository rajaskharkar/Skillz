package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureDefinition
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneId
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneUiModel

@Composable
fun zoneTitle(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_title)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_title)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_title)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_title)
    TheBlueZoneId.GOLDEN_FIELDS -> stringResource(R.string.land_zone_golden_fields)
    TheBlueZoneId.ANCIENT_WOODS -> stringResource(R.string.land_zone_ancient_woods)
    TheBlueZoneId.OPEN_SANDS -> stringResource(R.string.land_zone_open_sands)
    TheBlueZoneId.HIGH_PEAKS -> stringResource(R.string.land_zone_high_peaks)
    TheBlueZoneId.GREAT_WILD -> stringResource(R.string.land_zone_great_wild)
}

@Composable
fun zoneRailLabel(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_rail)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_rail)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_rail)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_rail)
    TheBlueZoneId.GOLDEN_FIELDS -> stringResource(R.string.land_rail_golden_fields)
    TheBlueZoneId.ANCIENT_WOODS -> stringResource(R.string.land_rail_ancient_woods)
    TheBlueZoneId.OPEN_SANDS -> stringResource(R.string.land_rail_open_sands)
    TheBlueZoneId.HIGH_PEAKS -> stringResource(R.string.land_rail_high_peaks)
    TheBlueZoneId.GREAT_WILD -> stringResource(R.string.land_rail_great_wild)
}

@Composable
fun zoneSubtitle(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_subtitle)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_subtitle)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_subtitle)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_subtitle)
    TheBlueZoneId.GOLDEN_FIELDS -> stringResource(R.string.land_subtitle_golden_fields)
    TheBlueZoneId.ANCIENT_WOODS -> stringResource(R.string.land_subtitle_ancient_woods)
    TheBlueZoneId.OPEN_SANDS -> stringResource(R.string.land_subtitle_open_sands)
    TheBlueZoneId.HIGH_PEAKS -> stringResource(R.string.land_subtitle_high_peaks)
    TheBlueZoneId.GREAT_WILD -> stringResource(R.string.land_subtitle_great_wild)
}

@Composable
fun zoneAnimalSummary(zone: TheBlueZoneUiModel): String {
    if (zone.animals.isEmpty()) return stringResource(R.string.the_blue_zone_waiting)
    val labels = mutableListOf<String>()
    for (animal in zone.animals) {
        labels += stringResource(R.string.the_blue_animal_count, findName(animal.findId), animal.totalCount)
    }
    return labels.joinToString()
}

@Composable
fun findName(findId: String): String = ShellContentCatalog.find(findId)?.let { stringResource(it.titleRes) } ?: stringResource(R.string.reward_card_shell_recorded_title)

fun isUniqueLegendaryCreature(definition: CreatureDefinition): Boolean {
    val id = definition.creatureId.lowercase()
    return id.contains("leviathan") || id.contains("kraken") || id.contains("megalodon")
}

@Composable
fun formatMinutesCompact(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val mins = safe % 60
    return when {
        hours > 0 && mins > 0 -> stringResource(R.string.land_duration_hours_minutes, hours, mins)
        hours > 0 -> stringResource(R.string.land_duration_hours, hours)
        else -> stringResource(R.string.land_duration_minutes, mins)
    }
}

fun TheBlueZoneId.toCreatureZone(): CreatureZone = creatureZone

fun theBlueZoneFor(zone: CreatureZone): TheBlueZoneId = TheBlueZoneId.valueOf(zone.name)
