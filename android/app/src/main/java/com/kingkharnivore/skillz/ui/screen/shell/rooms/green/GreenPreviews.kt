package com.kingkharnivore.skillz.ui.screen.shell.rooms.green

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.data.model.entity.green.*
import com.kingkharnivore.skillz.domain.green.*
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.viewmodel.green.GreenUiState

/** Fixed fixtures shared by previews, screenshots and Compose tests; never seeded into a wallet. */
object GreenPreviewFixtures {
    const val DATE = 1_780_000_000_000L
    val levels = listOf(1,15,30,45,60,75,90,99)
    fun specimen(species: PlantSpecies, level: Int, order: Long) = PlantSpecimenEntity(
        id = "preview-${species.id}-$order", speciesId = species.id, level = level, plantedAt = DATE,
        fullyGrownAt = (DATE + 86_400_000).takeIf { level >= 90 }, masteredAt = (DATE + 172_800_000).takeIf { level == 99 },
        lastWateredAt = DATE + 172_800_000, positionKey = "${species.environment.id}:$order", createdOrder = order,
        investedDrops = GreenEconomy.totalCost(species.tier,level))
    fun mixed(environment: GreenEnvironment): GreenUiState {
        val species=GreenCatalogue.inEnvironment(environment)
        val plants=levels.mapIndexed { index,level -> specimen(species[index],level,index.toLong()+1) } +
            specimen(species.first(),90,9) + specimen(species.last(),90,10)
        val awards=GreenBadgeEvaluator.eligible(plants.map { PlantProgress(it.speciesId,it.level) }).map { GreenBadgeAwardEntity(it,DATE+172_800_000,1) }
        return GreenUiState(250_000,plants,awards)
    }
}
@Preview(name="Empty Green",showBackground=true,widthDp=400,heightDp=850)
@Composable private fun EmptyGreenPreview() { SkillzTheme { Surface { GreenScreen(GreenUiState()) } } }
@Preview(name="Green World light",showBackground=true,widthDp=400,heightDp=850)
@Composable private fun WorldGreenPreview() { SkillzTheme(darkTheme=false) { Surface { GreenScreen(GreenPreviewFixtures.mixed(GreenEnvironment.GARDEN)) } } }
@Preview(name="Green World dark",showBackground=true,widthDp=400,heightDp=850)
@Composable private fun DarkGreenPreview() { SkillzTheme(darkTheme=true) { Surface { GreenScreen(GreenPreviewFixtures.mixed(GreenEnvironment.WOODLANDS),initialEnvironment=GreenEnvironment.WOODLANDS) } } }
@Preview(name="Catalogue without Drops",showBackground=true,widthDp=400,heightDp=850)
@Composable private fun CatalogueGreenPreview() { SkillzTheme { Surface { GreenScreen(GreenUiState(),initialSection=GreenSection.CATALOGUE) } } }
@Preview(name="Green Collection",showBackground=true,widthDp=400,heightDp=850)
@Composable private fun CollectionGreenPreview() { SkillzTheme { Surface { GreenChestPreviewContent(GreenPreviewFixtures.mixed(GreenEnvironment.WETLANDS)) } } }
@Preview(name="Green Badges",showBackground=true,widthDp=400,heightDp=850)
@Composable private fun BadgesGreenPreview() { SkillzTheme { Surface { GreenBadgePreviewContent(GreenPreviewFixtures.mixed(GreenEnvironment.GARDEN)) } } }
@Preview(name="All biological stages and Mastery",showBackground=true,widthDp=600,heightDp=1000)
@Composable fun GreenStagesPreview() {
    SkillzTheme { Surface {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
            listOf("daisy","hydrangea","giant_sequoia","wisteria","saguaro","victoria_lily","moss","rafflesia").forEach { id ->
                val species=GreenCatalogue.byId.getValue(id)
                Text(stringResource(species.nameRes))
                Row { GreenPreviewFixtures.levels.forEach { level ->
                    Column(Modifier.weight(1f)) {
                        PlantArtwork(species,level,stringResource(species.nameRes),Modifier.fillMaxWidth().height(90.dp))
                        Text(stringResource(PlantGrowthStageResolver.resolve(level).nameRes),style=MaterialTheme.typography.labelSmall)
                    }
                } }
            }
        }
    } }
}

/** The same shared screens used by production navigation, with deterministic botanical evidence. */
@Composable
fun GreenChestPreviewContent(state: GreenUiState, onWater: (String, Int, String) -> Unit = { _, _, _ -> }) {
    com.kingkharnivore.skillz.ui.screen.shell.inventory.ShellChestScreen(
        com.kingkharnivore.skillz.viewmodel.shell.ShellUiState(), { _, _ -> }, { _, _ -> }, {}, {}, {},
        greenState = state, showPlantsInitially = true, onWaterPlant = onWater)
}
@Composable
fun GreenBadgePreviewContent(state: GreenUiState, onNavigate: (com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination) -> Unit = {}) {
    val dashboard = com.kingkharnivore.skillz.domain.achievement.BadgeDashboardCalculator.calculate(
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
        greenPlants = state.specimens, greenAwards = state.awards)
    com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgesScreen(
        uiState = com.kingkharnivore.skillz.viewmodel.shell.ShellUiState(badgeDashboard = dashboard,
            badgeCategory = com.kingkharnivore.skillz.domain.achievement.BadgeUiCategory.GREEN,
            badgeSort = com.kingkharnivore.skillz.domain.achievement.BadgeSort.ALPHABETICAL),
        onPin = { _, _ -> }, onDismissPinReplacement = {}, onUnpin = {}, onTrack = {}, onUntrack = {},
        onCategory = {}, onSort = {}, onBadgeViewed = {}, onAcknowledgeBackfill = {}, onNavigate = onNavigate,
        onOpenFlow = {}, onOpenArc = {}, initialTab = com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgesTab.BADGE_BOOK, initialBrowseCollections = false)
}

/** Ten adult forms per environment for botanical review, using the production artwork. */
@Composable
fun GreenBotanicalPortraits(environment: GreenEnvironment) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp)) {
        val cellHeight=(maxHeight-48.dp)/4
        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(stringResource(environment.nameRes),style=MaterialTheme.typography.titleLarge)
            GreenCatalogue.inEnvironment(environment).chunked(3).forEach { row ->
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    row.forEach { species ->
                        Column(Modifier.weight(1f).height(cellHeight),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                            PlantArtwork(species,90,stringResource(species.nameRes),Modifier.fillMaxWidth().weight(1f))
                            Text(stringResource(species.nameRes),style=MaterialTheme.typography.labelSmall,
                                textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    repeat(3-row.size) {Spacer(Modifier.weight(1f))}
                }
            }
        }
    }
}
@Preview(name="Garden botanical portraits",widthDp=400,heightDp=850)
@Composable private fun BotanicalGardenPreview() {SkillzTheme {Surface {GreenBotanicalPortraits(GreenEnvironment.GARDEN)}}}

@Preview(name="Green World large text", showBackground=true, widthDp=400, heightDp=850, fontScale=1.3f)
@Composable private fun LargeTextGreenPreview() { SkillzTheme { Surface { GreenScreen(GreenPreviewFixtures.mixed(GreenEnvironment.GARDEN)) } } }

/** Cross-room early growth evidence; deliberately no Level 90 or 99 specimens. */
@Composable
fun GrowthBadgePreviewContent(collections: Boolean = false) {
    val creatures = com.kingkharnivore.skillz.utils.shell.CreatureRealm.entries.flatMap { realm ->
        com.kingkharnivore.skillz.utils.shell.CreatureCatalog.all.filter { it.isAvailable && it.realm == realm }
            .take(6).mapIndexed { index, species ->
                com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity(
                    "preview-${species.creatureId}", species.creatureId, GreenPreviewFixtures.DATE, "preview", null, null, null, false, true,
                    animalLevel = listOf(5,15,30,45,60,75)[index])
            }
    }
    val plants = GreenCatalogue.inEnvironment(GreenEnvironment.GARDEN).take(6).mapIndexed { index, species ->
        GreenPreviewFixtures.specimen(species, listOf(5,15,30,45,60,75)[index], index.toLong())
    }
    val dashboard = com.kingkharnivore.skillz.domain.achievement.BadgeDashboardCalculator.calculate(
        emptyList(), creatures, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), greenPlants = plants)
    com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgesScreen(
        uiState = com.kingkharnivore.skillz.viewmodel.shell.ShellUiState(badgeDashboard = dashboard,
            badgeCategory = com.kingkharnivore.skillz.domain.achievement.BadgeUiCategory.ALL,
            badgeSort = com.kingkharnivore.skillz.domain.achievement.BadgeSort.ALPHABETICAL),
        onPin = { _, _ -> }, onDismissPinReplacement = {}, onUnpin = {}, onTrack = {}, onUntrack = {},
        onCategory = {}, onSort = {}, onBadgeViewed = {}, onAcknowledgeBackfill = {}, onNavigate = {},
        onOpenFlow = {}, onOpenArc = {}, initialTab = com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgesTab.BADGE_BOOK,
        initialBrowseCollections = collections)
}
@Preview(name="Growth collections before Level 90", widthDp=400, heightDp=850)
@Composable private fun GrowthCollectionsPreview() { SkillzTheme { Surface { GrowthBadgePreviewContent(true) } } }
