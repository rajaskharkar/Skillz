package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.domain.shell.CreatureDefinition
import com.kingkharnivore.skillz.domain.shell.CreatureZone
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneId
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneUiModel

@Composable
fun zoneTitle(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_title)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_title)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_title)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_title)
}

@Composable
fun zoneRailLabel(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_rail)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_rail)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_rail)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_rail)
}

@Composable
fun zoneSubtitle(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_subtitle)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_subtitle)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_subtitle)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_subtitle)
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

fun formatMinutesCompact(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val mins = safe % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

fun TheBlueZoneId.toCreatureZone(): CreatureZone = when (this) {
    TheBlueZoneId.SUNLIT_REEF -> CreatureZone.SUNLIT_REEF
    TheBlueZoneId.DEEPER_REEF -> CreatureZone.DEEPER_REEF
    TheBlueZoneId.OPEN_BLUE -> CreatureZone.OPEN_BLUE
    TheBlueZoneId.GREAT_BLUE -> CreatureZone.GREAT_BLUE
}

fun theBlueZoneFor(zone: CreatureZone): TheBlueZoneId = when (zone) {
    CreatureZone.SUNLIT_REEF -> TheBlueZoneId.SUNLIT_REEF
    CreatureZone.DEEPER_REEF -> TheBlueZoneId.DEEPER_REEF
    CreatureZone.OPEN_BLUE -> TheBlueZoneId.OPEN_BLUE
    CreatureZone.GREAT_BLUE -> TheBlueZoneId.GREAT_BLUE
}
