package com.kingkharnivore.skillz.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.ui.screen.flow.FlowTimer
import com.kingkharnivore.skillz.ui.screen.flow.SessionModeSelector
import com.kingkharnivore.skillz.ui.screen.flow.reward.SessionRewardContent
import com.kingkharnivore.skillz.ui.screen.shell.inventory.*
import com.kingkharnivore.skillz.ui.screen.shell.rooms.red.*
import com.kingkharnivore.skillz.ui.screen.shell.rooms.voyage.PowerVoyageContent
import com.kingkharnivore.skillz.ui.screen.story.chronicle.FlowCard
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.utils.shell.*
import com.kingkharnivore.skillz.utils.shell.voyage.*
import com.kingkharnivore.skillz.viewmodel.shell.RedUiState
import java.time.YearMonth

/** Deterministic, isolated fixtures. No production wallet, timer, or repository mutations. */
class PowerRedVisualTestActivity : ComponentActivity() {
    // Dialog windows create their own Android composition locals from the Activity.
    // Override only this debug Activity's resources so sheets share the fixture locale.
    private var fixtureResources: android.content.res.Resources? = null
    override fun getResources(): android.content.res.Resources = fixtureResources ?: super.getResources()

    override fun onCreate(savedInstanceState: Bundle?) {
        val localeTag = intent.getStringExtra("locale")
        val configuration = android.content.res.Configuration(super.getResources().configuration).apply {
            if (localeTag != null) setLocales(android.os.LocaleList.forLanguageTags(localeTag))
            fontScale = intent.getFloatExtra("fontScale", fontScale)
        }
        fixtureResources = baseContext.createConfigurationContext(configuration).resources
        super.onCreate(savedInstanceState)
        val scenario = intent.getStringExtra("scenario") ?: "overview"
        val dark = intent.getBooleanExtra("dark",false)
        val animated = intent.getBooleanExtra("animate",false)
        val era = when(scenario) { "triassic", "era-transition" -> RedEra.TRIASSIC; "jurassic" -> RedEra.JURASSIC; else -> RedEra.CRETACEOUS }
        val owned = when(scenario) { "triassic", "era-transition" -> 3; "jurassic" -> 10; "cretaceous", "render-animals", "render-earth" -> 30; "owned", "mastered" -> 1; else -> 0 }
        val species = RedCreatureCatalog.entries.first { it.era == era }
        val creatureEntries = if (intent.getBooleanExtra("allOwned",false)) RedCreatureCatalog.entries
            else RedCreatureCatalog.entries.filter { it.era == era }.take(owned)
        val creatures = creatureEntries.mapIndexed { index, entry -> fixture(entry.id,index,if(scenario == "mastered") 99 else 12) }
        val state = RedUiState(if(scenario == "insufficient") 20 else 12_450, 20_000, creatures, if(scenario == "mastered") mapOf(species.id to 1) else emptyMap())
        setContent {
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides this,
                androidx.compose.ui.platform.LocalConfiguration provides configuration,
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(resources.displayMetrics.density, configuration.fontScale)
            ) {
            SkillzTheme(darkTheme=dark,dynamicColor=false) {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                        when(scenario) {
                            "chest", "chest-land", "chest-red", "chest-detail", "chest-empty" -> {
                                val ids = listOf("focus_minnow", "creature_chicken", "creature_duck", "jurassic_stegosaurus", "triassic_saturnalia", "cretaceous_triceratops") +
                                    LandCreatureCatalog.restorative.take(3).map { it.creatureId } + StillwaterCatalog.byId.keys.take(2)
                                var chest by remember { mutableStateOf(com.kingkharnivore.skillz.viewmodel.shell.ShellUiState(
                                    finds = if(scenario == "chest-empty") emptyList() else ids.mapIndexed { i,id -> fixture(id,i,if(i%3 == 0)98 else 12) }, pearlBalance = 200_000,
                                    chestEnvironment = when(scenario) { "chest-land" -> ChestFilterOption.Land; "chest-red" -> ChestFilterOption.Red; else -> ChestFilterOption.All })) }
                                ShellChestScreen(chest,{_,_->},{_,_->},{}, { chest=chest.copy(chestSortOption=it) }, { chest=chest.copy(chestFilter=it) },
                                    focusSpeciesId=if(scenario == "chest-detail") "jurassic_stegosaurus" else null,
                                    onEnvironmentSelected={chest=chest.copy(chestEnvironment=it)},
                                    onClearFilters={chest=chest.copy(chestEnvironment=ChestFilterOption.All,chestFilter=ChestFilterOption.All)})
                            }
                            "shell" -> com.kingkharnivore.skillz.ui.screen.shell.HeartRoomScreen(com.kingkharnivore.skillz.viewmodel.shell.ShellUiState()) {}
                            "selector" -> Column(Modifier.padding(24.dp)) { SessionModeSelector(FlowMode.POWER,{},false,{},{}) }
                            "active" -> Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(32.dp)) {
                                SessionModeSelector(FlowMode.POWER,{},true,{},{})
                                Text(stringResource(R.string.power_fixture_task),style=MaterialTheme.typography.headlineMedium)
                                FlowTimer("42:00",false,FlowMode.POWER)
                                Text(stringResource(R.string.power_fixture_arc),color=MaterialTheme.colorScheme.tertiary)
                            }
                            "completion", "completion-movement" -> Column(Modifier.padding(24.dp)) { SessionRewardContent(r = FlowRewardUiModel(42,mode=FlowMode.POWER,baseScyraPoints=63,tenMinuteBonuses=0,thirtyMinuteBonuses=0,sixtyMinuteBonuses=0,finalScyraPoints=if(scenario=="completion-movement")70 else 63,surgePoints=0,movementPoints=if(scenario=="completion-movement")7 else 0,shellPearlsEarned=if(scenario=="completion-movement")70 else 63,shellPebblesEarned=if(scenario=="completion-movement")70 else 63), calmMode=false) }
                            "arc-pebbles" -> Column(Modifier.padding(24.dp)) {
                                val text=com.kingkharnivore.skillz.ui.screen.flow.reward.rememberRewardRevealTextProvider()
                                val arc=com.kingkharnivore.skillz.model.state.flow.ArcSummaryUiModel(3,60*60_000L,100,12,1.2,
                                    com.kingkharnivore.skillz.model.state.flow.ArcShellRewardSummaryUiModel(pearlsCarried=100,pebblesCarried=72))
                                val cards=com.kingkharnivore.skillz.ui.screen.flow.reward.buildArcSummaryRewardCards(arc,false,text,stringResource(R.string.session_reward_minutes_value,60))
                                com.kingkharnivore.skillz.ui.screen.flow.reward.RewardRevealDeck(cards.filter { it.id=="arc-pebbles" })
                            }
                            "chronicle" -> Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                                FlowMode.entries.forEachIndexed { index, mode ->
                                    FlowCard(FlowListItemUiModel(index.toLong(),stringResource(R.string.power_fixture_task),"",tagId=1,tagName=stringResource(R.string.power_fixture_journey),journeyColor=MaterialTheme.colorScheme.primary,durationMs=42*60_000L,createdAt=1790874000000,score=if(mode==FlowMode.SOFT)0 else if(mode==FlowMode.POWER)63 else 72,isSoftMode=mode==FlowMode.SOFT,mode=mode,isSurge=false,surgePoints=0),false,true,false,{},{},{},{})
                                }
                            }
                            "badge-locked", "badge-partial", "badge-earned" -> {
                                val count = when(scenario) { "badge-earned" -> 14; "badge-partial" -> 7; else -> 0 }
                                val instances = RedCreatureCatalog.entries.filter { it.era == RedEra.TRIASSIC }.take(count).mapIndexed { i,e -> fixture(e.id,i,1) }
                                val dashboard = BadgeDashboardCalculator.calculate(emptyList(),instances,instances.map { CreatureDiscoveryEntity(it.findId,1,"test",it.instanceId,1) },emptyList(),emptyList(),emptyList(),emptyList())
                                val badge = dashboard.badges.first { it.badgeId == "red_dawn" }
                                Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                                    Text(stringResource(R.string.red_badges),style=MaterialTheme.typography.headlineMedium)
                                    BadgeMedallion(badge,BadgeMedallionSize.Large)
                                    Text(resolveBadgePresentation(badge.badgeId).title,style=MaterialTheme.typography.titleLarge)
                                    Text(resolveBadgePresentation(badge.badgeId).description)
                                    Text(stringResource(R.string.red_progress,count,14))
                                }
                            }
                            "render-animals" -> {
                                val clock = rememberRedSceneClock(null,true)
                                RedEraPage(era,state,{clock.value},{},{},{})
                            }
                            "render-earth" -> {
                                val clock = rememberRedSceneClock(null,true)
                                androidx.compose.foundation.Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }) { drawRedEarth(era,0f) }
                                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                    drawCircle(Color(0xFFA43B3E),30f,androidx.compose.ui.geometry.Offset(size.width*.5f+kotlin.math.sin(clock.value)*100f,size.height*.5f))
                                }
                            }
                            "render-baseline" -> {
                                val clock = rememberRedSceneClock(null,true)
                                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                    drawRect(Color(0xFFCADDD1))
                                    drawCircle(Color(0xFFA43B3E),30f,androidx.compose.ui.geometry.Offset(size.width*.5f+kotlin.math.sin(clock.value)*100f,size.height*.5f))
                                }
                            }
                            "book-empty", "book-partial", "book-complete", "book-detail" -> {
                                val group=BadgeBookCollections.collections.first()
                                val ids=when(scenario) { "book-empty" -> emptyList(); "book-complete" -> group.memberIds; else -> group.memberIds.take(2) }
                                val dashboard=remember { BadgeDashboardCalculator.calculate(ids.map { UserBadgeEntity(it,1,1790874000000,1790874000000,false) },emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList()) }
                                if(scenario=="book-detail") BadgeBookCollectionSheet(group,dashboard.badges,{},{})
                                else BadgesScreen(com.kingkharnivore.skillz.viewmodel.shell.ShellUiState(badgeDashboard=dashboard),
                                    {_,_->},{},{},{},{},{},{},{},{},{},{},{},initialTab=BadgesTab.BADGE_BOOK)
                            }
                            "badge-types" -> {
                                val dashboard=remember { BadgeDashboardCalculator.calculate(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList()) }
                                androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                                    badgeGalleryRows(listOf("red_first_footprint","mastery_first","badge_flow_10_min","mastery_species_triassic_saturnalia").map { id ->
                                        dashboard.badges.first { it.badgeId==id }.copy(everEarned=true,lifetimeCount=if(id=="badge_flow_10_min")12 else 1)
                                    },2,"types",true,{})
                                }
                            }
                            "red-badge-detail" -> {
                                val mastered = RedBadgeCatalog.byId.getValue("red_apex").species.take(2).map { id ->
                                    CreatureMasteryEventEntity("master-$id",id,id,1790874000000,"growth-$id")
                                }
                                val badge = BadgeDashboardCalculator.calculate(emptyList(),emptyList(),emptyList(),mastered,emptyList(),emptyList(),emptyList())
                                    .badges.first { it.badgeId == "red_apex" }
                                BadgeDetailsSheet(badge,{},{},{},{})
                            }
                            "power-badge-progress" -> {
                                val sessions = listOf(com.kingkharnivore.skillz.data.model.entity.SessionEntity(
                                    id=1,title=stringResource(R.string.power_fixture_task),description="",tagId=1,
                                    startTime=1,endTime=116_040_001,durationMs=116_040_000,mode=FlowMode.POWER))
                                val badge = BadgeDashboardCalculator.calculate(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),sessions=sessions)
                                    .badges.first { it.badgeId=="power_bedrock" }
                                BadgeDetailsSheet(badge,{},{},{},{})
                            }
                            "voyage" -> Box(Modifier.padding(20.dp)) {
                                val range = PowerRangeStats(116_040_000,47,.34,listOf(PowerJourneyTrend(1,stringResource(R.string.power_fixture_journey),86_400_000,.42,mapOf(YearMonth.of(2026,7) to .62,YearMonth.of(2026,8) to .48,YearMonth.of(2026,9) to .31))))
                                PowerVoyageContent(PowerVoyageStats(PowerVoyageCalculator.windows.associateWith { range }))
                            }
                            else -> if (scenario.startsWith("badge-atlas-")) RedBadgeAtlas(scenario.removePrefix("badge-atlas-").toInt()) else if (scenario.startsWith("art-")) DinosaurAtlas(scenario.removePrefix("art-").toInt()) else RedScreen(state,initialPageOffsetFraction=if(scenario=="era-transition") .45f else 0f,simulatedSceneTime=if(animated) null else 12f,initialEra=if(scenario=="overview")null else era,initialCatalog=scenario=="catalog",initialCreature=if(scenario in setOf("affordable","insufficient","owned","mastered"))species.id else null)
                        }
                    }
                }
            }
        }
    }
    }
    private fun fixture(id: String,index: Int,level: Int) = UserShellFindInstanceEntity("fixture-$index",id,index.toLong(),"red_purchase",null,null,null,false,true,animalLevel=level)
}

@Composable
private fun DinosaurAtlas(page: Int) {
    val entries=RedCreatureCatalog.entries.drop(page*12).take(12)
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.red_title),style=MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.red_progress,(page*12+entries.size),69))
        entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                row.forEach { entry ->
                    Column(Modifier.weight(1f),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                        RedDinosaurIcon(entry.id,Modifier.fillMaxWidth().height(100.dp))
                        Text(stringResource(entry.nameRes),style=MaterialTheme.typography.labelSmall,maxLines=2)
                    }
                }
                repeat(3-row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RedBadgeAtlas(page: Int) {
    val dashboard = remember { BadgeDashboardCalculator.calculate(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList()) }
    val specs = RedBadgeCatalog.specs.drop(page * 8).take(8)
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.red_badges),style=MaterialTheme.typography.headlineSmall)
        specs.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                row.forEach { spec ->
                    val badge=dashboard.badges.first { it.badgeId==spec.id }
                    Column(Modifier.weight(1f), horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(stringResource(spec.titleRes),style=MaterialTheme.typography.titleSmall)
                        Row {
                            Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                                BadgeMedallion(badge,BadgeMedallionSize.Small)
                                Text(stringResource(R.string.badge_locked),style=MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                                BadgeMedallion(badge.copy(everEarned=true,lifetimeCount=1,currentProgress=spec.target),BadgeMedallionSize.Small)
                                Text(stringResource(R.string.badge_earned),style=MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                repeat(2-row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
