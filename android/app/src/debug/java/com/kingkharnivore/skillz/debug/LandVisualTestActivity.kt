package com.kingkharnivore.skillz.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.model.state.flow.*
import com.kingkharnivore.skillz.ui.screen.flow.reward.ArcSummaryContent
import com.kingkharnivore.skillz.ui.screen.shell.*
import com.kingkharnivore.skillz.ui.screen.shell.inventory.*
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.*
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.utils.shell.*
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState

/** Isolated synthetic state. No production database or acquisition paths are changed by previews. */
class LandVisualTestActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scenario = intent.getStringExtra("scenario") ?: "selector"
        val zoneName = intent.getStringExtra("zone") ?: "GOLDEN_FIELDS"
        val habitat = LandStillwaterHabitat.entries.firstOrNull { it.name == zoneName }
        val zone = habitat?.zone ?: CreatureZone.valueOf(zoneName)
        val speciesId = intent.getStringExtra("species") ?: "creature_tiger"
        val badgeId = intent.getStringExtra("badge")
        val count = intent.getIntExtra("flows",18)
        setContent {
            SkillzTheme(darkTheme=false,dynamicColor=false) {
                var realm by remember { mutableStateOf(if(scenario=="selector") null else if(scenario in setOf("sea","sea-empty")) CreatureRealm.SEA else CreatureRealm.LAND) }
                var state by remember { mutableStateOf(ShellUiState(pearlBalance=if(scenario=="unaffordable") 20 else 25_000,
                    finds=when (scenario) {
                        "sea-empty" -> emptyList()
                        "copies" -> List(intent.getIntExtra("copies",13).coerceIn(0,10_000)) {
                            instance(speciesId, suffix=it.toString())
                        }
                        "dense" -> CreatureCatalog.all.filter { it.realm==CreatureRealm.LAND && it.zone==zone }
                            .flatMap { creature -> List(13) { instance(creature.creatureId,suffix=it.toString()) } }
                        "animals" -> animalFixtures(zone)
                        else -> if(scenario.startsWith("badge")) badgeFixtures() else fixtures()
                    },
                    stillwaterClaimableDrops=intent.getLongExtra("drops",100_000L), stillwaterLifetimeDrops=150_000L,
                    unlockedBlueZones=CreatureZone.entries.toSet())) }
                var request by remember { mutableStateOf<PendingShellNavigation?>(when(scenario) {
                    "selector", "sea", "sea-empty" -> null
                    "badges", "badge-art" -> badgeId?.let { PendingShellNavigation.OpenBadge(it) }
                    "detail", "level99" -> PendingShellNavigation.OpenBlueSpecies(CreatureCatalog.require(speciesId).collectionId,speciesId)
                    else -> PendingShellNavigation.OpenBlueSpecies("blue_${zone.name.lowercase()}",null)
                }) }
                var encounterOpen by remember { mutableStateOf(true) }
                Surface(Modifier.fillMaxSize()) {
                    Scaffold(topBar={ ShellTopBar(
                        destination=when(scenario) { "habitat" -> ShellDestination.TheGreen; "chest" -> ShellDestination.ShellChest; "badges", "badge-art" -> ShellDestination.Badges; else -> ShellDestination.TheBluePreview },
                        pearlBalance=state.pearlBalance,pearlBasinHasIndicator=false,notificationCount=0,
                        onBack={realm=null},onNotifications={},blueRealm=realm) }) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            when(scenario) {
                                "animal-art" -> LandArtworkPreview(zone,intent.getBooleanExtra("restorative",false),intent.getBooleanExtra("animate",false))
                                "encounter", "affordable", "unaffordable" -> {
                                    LandZonePage(buildTheBlueUiState(state.finds,emptyList(),CreatureRealm.LAND).zones.first { it.zoneId.creatureZone==zone },{0f},{},{},{})
                                    if (encounterOpen) BeyondBlueEncounterSheet(pearlBalance=state.pearlBalance,initialZone=theBlueZoneFor(zone),
                                        activeAnimalInstances=state.finds,
                                        initialTargetSpeciesId=if(scenario=="encounter") null else "creature_duck",onDismiss={encounterOpen=false},
                                        onEncounter={id,_ -> state=state.copy(pearlBalance=state.pearlBalance-(CreatureCatalog.require(id).pearlPrice ?: 0),finds=state.finds+instance(id,1,"purchase")); encounterOpen=false})
                                }
                                "badges", "badge-art" -> {
                                    var previewPins by remember { mutableStateOf(listOf(
                                        "mastery_species_creature_tiger", "land_first", "land_arc_15", "land_pasture_first", "living_earth_first")) }
                                    var previewTracking by remember { mutableStateOf(listOf("mastery_species_creature_tiger", "land_two_tigers")) }
                                    val dashboard = remember(state.finds, previewPins, previewTracking) { badgeDashboard(state.finds, previewPins, previewTracking) }
                                    if(scenario=="badge-art") BadgeArtworkPreview(dashboard, badgeId ?: "land")
                                    else BadgesScreen(uiState=state.copy(badgeDashboard=dashboard,achievementInitializationState=com.kingkharnivore.skillz.viewmodel.shell.AchievementInitializationState.Complete),
                                        onPin={id,_-> if(id !in previewPins) previewPins=previewPins+id},onDismissPinReplacement={},
                                        onUnpin={id->previewPins=previewPins-id},onTrack={id->if(id !in previewTracking) previewTracking=previewTracking+id},
                                        onUntrack={id->previewTracking=previewTracking-id},
                                        onCategory={state=state.copy(badgeCategory=it)},onSort={state=state.copy(badgeSort=it)},
                                        onBadgeViewed={},onAcknowledgeBackfill={},onNavigate={},onOpenFlow={},onOpenArc={},
                                        pendingNavigation=request,onNavigationResult={_,_->request=null})
                                }
                                "chest" -> ShellChestScreen(state,{_,_->},{_,_->},{},{state=state.copy(chestSortOption=it)},{state=state.copy(chestFilter=it)})
                                "reward" -> Column(Modifier.fillMaxWidth().padding(20.dp)) {
                                    ArcSummaryContent(ArcSummaryUiModel(count,count*600_000L,100,0,1.3,
                                        shellSummary=ArcShellRewardSummaryUiModel(animals=LandArcRewards.forFlowCount(count).map { ArcShellRewardCountUiModel(it.creatureId,it.quantity) })),false)
                                }
                                "mastery" -> MasteryCelebrationScreen(event=mastery(),uiState=state,onBegin={},onAdvance={},onPrevious={},onComplete={},onPin={_,_->},onDismissPinReplacement={},onUnpin={},onTrack={},onUntrack={},onNavigate={})
                                else -> TheBlueRoomScreen(state,{_,_->},{},{_,_->},{_,_->},{},request,{_,_->request=null},realm,{realm=it})
                            }
                        }
                    }
                }
            }
        }
    }

    private fun animalFixtures(zone: CreatureZone): List<UserShellFindInstanceEntity> {
        val names=when(zone) {
            CreatureZone.GOLDEN_FIELDS -> "horse goat sheep chicken cow donkey"
            CreatureZone.ANCIENT_WOODS -> "chimpanzee squirrel orangutan deer rabbit black_bear"
            CreatureZone.OPEN_SANDS -> "camel fennec_fox ostrich meerkat monitor_lizard scorpion"
            CreatureZone.HIGH_PEAKS -> "moose mountain_goat bighorn_sheep arctic_fox snow_leopard polar_bear"
            else -> "gorilla baboon tiger lion crocodile grizzly_bear"
        }
        return names.split(" ").flatMap { name ->
            List(intent.getIntExtra("copies",1).coerceIn(1,10_000)) { index ->
                instance("creature_$name", suffix=index.toString())
            }
        }
    }
    private fun badgeFixtures() = CreatureCatalog.all.mapIndexed { i, c ->
        instance(c.creatureId, if(i%9==0 || c.creatureId=="creature_tiger") 99 else 25).copy(
            sourceType=when(c.sourceType) { CreatureSourceType.ARC_EARNED -> "arc"; CreatureSourceType.STILLWATER, CreatureSourceType.RESTORATIVE_LAND -> "stillwater"; else -> "beyond_blue" },
            sourceId=if(c.sourceType==CreatureSourceType.ARC_EARNED) "arc_${c.arcFlowRequirement}" else "preview")
    }
    private fun badgeDashboard(finds:List<UserShellFindInstanceEntity>, pins:List<String>, tracked:List<String>) = BadgeDashboardCalculator.calculate(
        emptyList(),finds,finds.map { CreatureDiscoveryEntity(it.findId,1,it.sourceType,it.instanceId,1) },
        finds.filter { it.animalLevel==99 }.map { CreatureMasteryEventEntity("m:${it.instanceId}",it.instanceId,it.findId,1,"preview") },
        emptyList(),pins.mapIndexed { index,id -> BadgePinEntity(id,index,1) },tracked.map { BadgeTrackingEntity(it,1) })

    private fun instance(id:String,level:Int=1,suffix:String="0") = UserShellFindInstanceEntity(
        instanceId="$id:$suffix",findId=id,acquiredAt=1,sourceType="debug_preview",sourceId=null,
        currentUpgradeStageId=null,customName=null,isNew=false,isArchivedInChest=true,animalLevel=level)
    private fun fixtures() = (LandCreatureCatalog.main.filter { it.sourceType == CreatureSourceType.ARC_EARNED || it.creatureId in setOf("creature_duck","creature_fox","creature_coyote","creature_eagle","creature_lion") }.map { instance(it.creatureId,if(it.creatureId=="creature_tiger") 99 else 1) }
        + listOf(instance("focus_minnow"),instance("focus_seahorse"),instance("focus_manta"),instance("focus_whale"),instance("creature_tiger",1,"second")))
    private fun mastery(): MasteryCelebrationEventEntity {
        val d=CreatureCatalog.require("creature_tiger")
        return MasteryCelebrationEventEntity(eventId="preview",transactionId="preview",creatureInstanceId="preview",speciesId=d.creatureId,
            artworkKey=d.staticIconKey,regionId=d.collectionId,sourceId=d.sourceType.name,previousLevel=98,newLevel=99,
            speciesMasteryCount=1,totalMasteries=1,uniqueMasteredSpecies=1,regionalDiscovered=13,regionalTotal=13,regionalMastered=1,
            regionalCollectorEarned=true,regionalCompletionistEarned=false,blueMastered=1,blueTotal=103,stillwaterMastered=0,stillwaterTotal=32,
            allWatersMastered=1,allWatersTotal=135,newlyEarnedBadgeIds="mastery_species_creature_tiger",advancedBadgeIds="",milestonesReached="",
            originDestination="BLUE",createdAt=1,lifecycleState=CelebrationLifecycle.PRESENTING.name,presentationStage=CelebrationStage.MASTERY_REVEAL.name)
    }
}
