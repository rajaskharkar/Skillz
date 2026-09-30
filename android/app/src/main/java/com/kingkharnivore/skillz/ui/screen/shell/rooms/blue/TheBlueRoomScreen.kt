package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import com.kingkharnivore.skillz.utils.shell.CreatureRealm
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellRewardKind
import com.kingkharnivore.skillz.utils.shell.CreatureStatus
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneId
import com.kingkharnivore.skillz.ui.screen.shell.buildTheBlueUiState
import com.kingkharnivore.skillz.ui.screen.shell.depthOrder
import com.kingkharnivore.skillz.ui.screen.shell.theBlueSequentialNavigationPath
import com.kingkharnivore.skillz.ui.screen.shell.theBlueZoneForPage
import com.kingkharnivore.skillz.ui.screen.shell.ux.canDisplayInstance
import com.kingkharnivore.skillz.utils.shell.shellBackground
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import com.kingkharnivore.skillz.ui.screen.shell.NavigationConsumptionResult
import com.kingkharnivore.skillz.ui.screen.shell.NavigationFailureReason
import com.kingkharnivore.skillz.ui.screen.shell.PendingShellNavigation
import com.kingkharnivore.skillz.ui.screen.shell.validateBlueSpeciesFocus
import com.kingkharnivore.skillz.ui.screen.shell.validateBeyondBlueFocus
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun TheBlueRoomScreen(
    uiState: ShellUiState,
    onDisplayInFocus: (String, String) -> Unit,
    onGrowCreature: (String) -> Unit,
    onReleaseCreaturesByLevel: (String, Map<Int, Int>) -> Unit,
    onEncounterBeyondBlue: (String, List<String>) -> Unit,
    onOpenChest: () -> Unit,
    focusRequest: PendingShellNavigation? = null,
    onFocusResult: (String, NavigationConsumptionResult) -> Unit = { _, _ -> },
    realm: CreatureRealm? = null,
    onSelectRealm: (CreatureRealm?) -> Unit = {}
) {
    LaunchedEffect(focusRequest) {
        val collectionId = when(val request = focusRequest) {
            is PendingShellNavigation.OpenBlueSpecies -> request.collectionId
            is PendingShellNavigation.OpenBeyondBlue -> request.collectionId
            else -> null
        }
        collectionId?.removePrefix("blue_")?.let { name ->
            com.kingkharnivore.skillz.utils.shell.CreatureZone.entries.firstOrNull { it.name.lowercase() == name }
                ?.let { onSelectRealm(it.realm) }
        }
    }
    if (!com.kingkharnivore.skillz.BuildConfig.LAND_ENABLED) {
        BlueRealmRoomScreen(uiState,onDisplayInFocus,onGrowCreature,onReleaseCreaturesByLevel,
            onEncounterBeyondBlue,onOpenChest,focusRequest,onFocusResult,CreatureRealm.SEA) {}
    } else if (realm == null) {
        BlueRealmSelector(onSelectRealm)
    } else {
        BackHandler { onSelectRealm(null) }
        androidx.compose.runtime.key(realm) {
            BlueRealmRoomScreen(uiState, onDisplayInFocus, onGrowCreature, onReleaseCreaturesByLevel,
                onEncounterBeyondBlue, onOpenChest, focusRequest, onFocusResult, realm) { onSelectRealm(null) }
        }
    }
}

@Composable
private fun BlueRealmRoomScreen(
    uiState: ShellUiState,
    onDisplayInFocus: (String, String) -> Unit,
    onGrowCreature: (String) -> Unit,
    onReleaseCreaturesByLevel: (String, Map<Int, Int>) -> Unit,
    onEncounterBeyondBlue: (String, List<String>) -> Unit,
    onOpenChest: () -> Unit,
    focusRequest: PendingShellNavigation? = null,
    onFocusResult: (String, NavigationConsumptionResult) -> Unit,
    realm: CreatureRealm,
    onReturn: () -> Unit
) {
    val theBlueState = remember(uiState.finds, uiState.focusPlacements) {
        buildTheBlueUiState(uiState.finds, uiState.focusPlacements, realm)
    }
    val shownZones = theBlueState.zones
    var selectedAnimal by remember { mutableStateOf<TheBlueAnimalGroupUiModel?>(null) }
    var releaseCandidate by remember { mutableStateOf<TheBlueAnimalGroupUiModel?>(null) }
    var showBeyondBlue by remember { mutableStateOf(false) }
    var beyondBlueInitialZone by remember { mutableStateOf(if(realm == CreatureRealm.SEA) TheBlueZoneId.SUNLIT_REEF else TheBlueZoneId.GOLDEN_FIELDS) }
    var beyondBlueTargetSpeciesId by remember { mutableStateOf<String?>(null) }
    var entryNewAnimalFindIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var railNavigationJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(theBlueState.newAnimalCount, theBlueState.zones) {
        if (entryNewAnimalFindIds.isEmpty() && theBlueState.newAnimalCount > 0) {
            entryNewAnimalFindIds = theBlueState.zones
                .flatMap { zone -> zone.animals.filter { it.isNew }.map { it.findId } }
                .toSet()
        }
    }
    val pageCount = if (theBlueState.isEmpty && realm == CreatureRealm.SEA) 1 else shownZones.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    LaunchedEffect(focusRequest, theBlueState.zones) {
        val request = focusRequest
        val collectionId = when (request) {
            is PendingShellNavigation.OpenBlueSpecies -> request.collectionId
            is PendingShellNavigation.OpenBeyondBlue -> request.collectionId
            else -> return@LaunchedEffect
        }
        val speciesId = when (request) {
            is PendingShellNavigation.OpenBlueSpecies -> request.speciesId
            is PendingShellNavigation.OpenBeyondBlue -> request.speciesId
            else -> null
        }
        val species = speciesId?.let(CreatureCatalog::get)
        if (request is PendingShellNavigation.OpenBeyondBlue) {
            val validation = validateBeyondBlueFocus(
                speciesExists = species != null,
                isBeyondBlueSpecies = species != null && species in CreatureCatalog.beyondBlue,
                belongsToCollection = species?.collectionId == collectionId
            )
            if (validation is NavigationConsumptionResult.Failed) {
                onFocusResult(request.requestId, validation)
                return@LaunchedEffect
            }
        }
        val zoneName = collectionId.removePrefix("blue_")
        val page = shownZones.indexOfFirst { it.zoneId.name.lowercase() == zoneName }
        if (page < 0) {
            onFocusResult(request.requestId, NavigationConsumptionResult.Failed(NavigationFailureReason.DESTINATION_UNAVAILABLE))
            return@LaunchedEffect
        }
        pagerState.scrollToPage(page)
        if (request is PendingShellNavigation.OpenBeyondBlue) {
            beyondBlueInitialZone = shownZones[page].zoneId
            beyondBlueTargetSpeciesId = request.speciesId
            showBeyondBlue = true
            withFrameNanos { }
            onFocusResult(request.requestId, NavigationConsumptionResult.Consumed)
        } else if (speciesId == null) {
            withFrameNanos { }
            onFocusResult(request.requestId, NavigationConsumptionResult.Consumed)
        } else {
            val targetAnimal = shownZones[page].animals.firstOrNull { it.findId == speciesId }
            val validation = validateBlueSpeciesFocus(
                speciesId,
                catalogSpeciesExists = species != null,
                renderedSpeciesIds = shownZones[page].animals.mapTo(mutableSetOf()) { it.findId }
            )
            if (validation is NavigationConsumptionResult.Failed) {
                onFocusResult(request.requestId, validation)
                return@LaunchedEffect
            }
            selectedAnimal = targetAnimal ?: return@LaunchedEffect
            withFrameNanos { }
            onFocusResult(request.requestId, NavigationConsumptionResult.Consumed)
        }
    }
    val scope = rememberCoroutineScope()
    var sceneTimeSeconds by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            sceneTimeSeconds = (withFrameNanos { it } - startNanos) / 1_000_000_000f
        }
    }
    val activeZone by remember(pagerState, shownZones) {
        derivedStateOf { shownZones.getOrNull(pagerState.currentPage)?.zoneId ?: TheBlueZoneId.SUNLIT_REEF }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(shellBackground())
    ) {
        val pageHeight = maxHeight
        Column(Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (theBlueState.isEmpty && realm == CreatureRealm.SEA) {
                TheBlueEmptyOceanPage(pageHeight = pageHeight, onEncounter = {
                    beyondBlueInitialZone = TheBlueZoneId.SUNLIT_REEF
                    beyondBlueTargetSpeciesId = null
                    showBeyondBlue = true
                })
            } else {
                val zone = shownZones[page]
                if (realm == CreatureRealm.LAND) {
                    LandZonePage(zone, { sceneTimeSeconds }, { selectedAnimal = it }, { target ->
                        beyondBlueInitialZone = zone.zoneId
                        beyondBlueTargetSpeciesId = target
                        showBeyondBlue = true
                    }, onReturn)
                } else TheBlueZonePage(
                    zone = zone,
                    state = theBlueState,
                    pageHeight = pageHeight,
                    showRoomHeader = zone.zoneId == TheBlueZoneId.SUNLIT_REEF,
                    entryNewAnimalFindIds = entryNewAnimalFindIds,
                    sceneTimeSeconds = sceneTimeSeconds,
                    onZoneBeyondBlue = {
                        beyondBlueInitialZone = zone.zoneId
                        beyondBlueTargetSpeciesId = null
                        showBeyondBlue = true
                    },
                    onAnimalClick = { selectedAnimal = it },
                    collectionProgress = uiState.badgeDashboard?.collections?.firstOrNull {
                        it.collectionId == "blue_${zone.zoneId.name.lowercase()}"
                    }
                )
            }
        }

        }
        if (!theBlueState.isEmpty || realm == CreatureRealm.LAND) {
            TheBlueDepthRail(
                zones = theBlueState.zones.map { it.zoneId },
                activeZone = activeZone,
                onZoneClick = { target ->
                    railNavigationJob?.cancel()
                    railNavigationJob = scope.launch {
                        if (realm == CreatureRealm.LAND) {
                            pagerState.animateScrollToPage(shownZones.indexOfFirst { it.zoneId == target })
                        } else for (zone in theBlueSequentialNavigationPath(theBlueZoneForPage(pagerState.currentPage), target)) {
                            pagerState.animateScrollToPage(zone.depthOrder())
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }
    }

    selectedAnimal?.let { animal ->
        TheBlueAnimalDetailSheet(
            animal = animal,
            focusSlotId = remember(uiState.focusPlacements, animal.findId) {
                firstOpenFocusSlotFor(animal.findId, uiState)
            },
            pearlBalance = uiState.pearlBalance,
            onDismiss = { selectedAnimal = null },
            onGrow = { instanceId ->
                onGrowCreature(instanceId)
                selectedAnimal = null
            },
            onRelease = {
                releaseCandidate = animal
                selectedAnimal = null
            },
            onBeyondBlue = {
                beyondBlueInitialZone = animal.zoneId
                beyondBlueTargetSpeciesId = null
                showBeyondBlue = true
                selectedAnimal = null
            },
            onDisplayInFocus = { instanceId, slotId ->
                onDisplayInFocus(instanceId, slotId)
                selectedAnimal = null
            },
            onOpenChest = {
                selectedAnimal = null
                onOpenChest()
            },
            firstRestingInstanceId = remember(uiState.finds, uiState.focusPlacements, animal.findId) {
                firstRestingInstanceId(animal.findId, uiState)
            },
            level99Preview = uiState.badgeDashboard?.level99Previews?.get(animal.findId)
                ?.takeIf { animal.highestLevel == 98 }
        )
    }

    releaseCandidate?.let { animal ->
        ReleaseCreatureConfirmationSheet(
            animal = animal,
            onDismiss = { releaseCandidate = null },
            onConfirm = { findId, selectionsByLevel ->
                onReleaseCreaturesByLevel(findId, selectionsByLevel)
                releaseCandidate = null
            }
        )
    }

    if (showBeyondBlue) {
        BeyondBlueEncounterSheet(
            pearlBalance = uiState.pearlBalance,
            initialZone = beyondBlueInitialZone,
            activeAnimalInstances = uiState.finds.filter {
                it.creatureStatus == CreatureStatus.ACTIVE && ShellContentCatalog.find(it.findId)?.kind == ShellRewardKind.ANIMAL
            },
            initialTargetSpeciesId = beyondBlueTargetSpeciesId,
            onDismiss = { showBeyondBlue = false },
            onEncounter = { targetCreatureId, selectedIds ->
                onEncounterBeyondBlue(targetCreatureId, selectedIds)
                showBeyondBlue = false
            }
        )
    }
}

private fun firstOpenFocusSlotFor(findId: String, uiState: ShellUiState): String? {
    val definition = ShellContentCatalog.find(findId) ?: return null
    val occupied = uiState.focusPlacements.map { it.slotId }.toSet()
    return ShellContentCatalog.focusSlots.firstOrNull { slot ->
        slot.slotId !in occupied && ShellContentCatalog.isCompatibleWithSlot(slot, definition)
    }?.slotId
}

private fun firstRestingInstanceId(findId: String, uiState: ShellUiState): String? {
    val displayed = uiState.focusPlacements.map { it.instanceId }.toSet()
    return uiState.finds.firstOrNull { item ->
        item.findId == findId && item.instanceId !in displayed && canDisplayInstance(item, ShellContentCatalog.find(item.findId))
    }?.instanceId
}
