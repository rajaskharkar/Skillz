package com.kingkharnivore.skillz.viewmodel.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ShellPlacementEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserDiscoveryEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindStackEntity
import com.kingkharnivore.skillz.data.model.shell.ShellRoomId
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureDefinition
import com.kingkharnivore.skillz.utils.shell.CreatureSourceType
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import com.kingkharnivore.skillz.utils.shell.StillwaterVessel
import com.kingkharnivore.skillz.utils.shell.requiresStillwaterConfirmation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShellUiState(
    val pearlBalance: Int = 0,
    val stillwaterClaimableDrops: Long = 0,
    val stillwaterLifetimeDrops: Long = 0,
    val stillwaterRevealCreature: CreatureDefinition? = null,
    val pendingStillwaterDrawVessel: StillwaterVessel? = null,
    val unlockedBlueZones: Set<CreatureZone> = setOf(CreatureZone.SUNLIT_REEF),
    val finds: List<UserShellFindInstanceEntity> = emptyList(),
    val stacks: List<UserShellFindStackEntity> = emptyList(),
    val focusPlacements: List<ShellPlacementEntity> = emptyList(),
    val badges: List<UserBadgeEntity> = emptyList(),
    val discoveries: List<UserDiscoveryEntity> = emptyList(),
    val objectiveCompletions: List<ObjectiveCompletionEntity> = emptyList()
)

private data class ShellEconomyState(
    val pearlBalance: Int,
    val stillwaterClaimableDrops: Long,
    val stillwaterLifetimeDrops: Long
)

private data class ShellOwnershipState(
    val finds: List<UserShellFindInstanceEntity>,
    val stacks: List<UserShellFindStackEntity>,
    val focusPlacements: List<ShellPlacementEntity>
)

private data class ShellMemoryState(
    val badges: List<UserBadgeEntity>,
    val discoveries: List<UserDiscoveryEntity>,
    val objectiveCompletions: List<ObjectiveCompletionEntity>
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val repository: ShellRepository
) : ViewModel() {
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events

    private val stillwaterRevealCreature = MutableStateFlow<CreatureDefinition?>(null)
    private val pendingStillwaterDrawVessel = MutableStateFlow<StillwaterVessel?>(null)

    private val economy = combine(
        repository.observePearlBalance(),
        repository.observeStillwaterTotal(),
        repository.observeStillwaterLifetimeTotal()
    ) { pearls, claimableDrops, lifetimeDrops -> ShellEconomyState(pearls, claimableDrops, lifetimeDrops) }

    private val ownership = combine(
        repository.observeOwnedFinds(),
        repository.observeStacks(),
        repository.observePlacements(ShellRoomId.FOCUS)
    ) { finds, stacks, placements -> ShellOwnershipState(finds, stacks, placements) }

    private val memory = combine(
        repository.observeEarnedBadges(),
        repository.observeDiscoveries(),
        repository.observeObjectiveCompletions()
    ) { badges, discoveries, objectiveCompletions -> ShellMemoryState(badges, discoveries, objectiveCompletions) }

    val uiState: StateFlow<ShellUiState> = combine(
        economy,
        ownership,
        memory,
        stillwaterRevealCreature,
        pendingStillwaterDrawVessel
    ) { economy, ownership, memory, revealCreature, pendingVessel ->
        ShellUiState(
            pearlBalance = economy.pearlBalance,
            stillwaterClaimableDrops = economy.stillwaterClaimableDrops,
            stillwaterLifetimeDrops = economy.stillwaterLifetimeDrops,
            stillwaterRevealCreature = revealCreature,
            pendingStillwaterDrawVessel = pendingVessel,
            unlockedBlueZones = deriveUnlockedBlueZonesFromHistoricalFinds(ownership.finds),
            finds = ownership.finds,
            stacks = ownership.stacks,
            focusPlacements = ownership.focusPlacements,
            badges = memory.badges,
            discoveries = memory.discoveries,
            objectiveCompletions = memory.objectiveCompletions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShellUiState())

    fun place(instanceId: String, slotId: String) = viewModelScope.launch {
        runCatching { repository.placeInstance(instanceId, ShellRoomId.FOCUS, slotId) }
            .onSuccess { _events.emit("Placed in the Focus Room.") }
            .onFailure { _events.emit(it.message ?: "Could not display that reward.") }
    }

    fun returnToChest(instanceId: String) = viewModelScope.launch {
        runCatching { repository.removePlacement(instanceId) }
            .onSuccess { _events.emit("Returned to The Chest.") }
            .onFailure { _events.emit(it.message ?: "Could not return that reward.") }
    }

    fun invitePearlObject(findId: String, slotId: String) = viewModelScope.launch {
        runCatching { repository.invitePearlObject(findId, ShellRoomId.FOCUS, slotId) }
            .onSuccess { _events.emit("Your Pearls shaped the Focus Room.") }
            .onFailure { _events.emit(it.message ?: "Could not shape that space.") }
    }

    fun invitePearlObjectToChest(findId: String) = viewModelScope.launch {
        runCatching { repository.invitePearlObjectToChest(findId) }
            .onSuccess { _events.emit("A creature is in The Chest.") }
            .onFailure { _events.emit(it.message ?: "Could not invite that object.") }
    }

    fun upgrade(instanceId: String) = viewModelScope.launch {
        runCatching { repository.upgradeInstance(instanceId) }
            .onSuccess { _events.emit("Your Pearls shaped growth in The Shell.") }
            .onFailure { _events.emit(it.message ?: "Could not shape that reward.") }
    }

    fun growCreature(instanceId: String) = viewModelScope.launch {
        runCatching { repository.growCreature(instanceId) }
            .onSuccess { _events.emit("Your creature grew inside The Blue.") }
            .onFailure { _events.emit(it.message ?: "Could not grow that creature.") }
    }

    fun growCreatureByLevel(findId: String, level: Int) = viewModelScope.launch {
        runCatching { repository.growCreatureByLevel(findId, level) }
            .onSuccess { _events.emit("Your creature grew inside The Chest.") }
            .onFailure { _events.emit(it.message ?: "Could not grow that creature.") }
    }

    fun releaseCreature(instanceId: String) = viewModelScope.launch {
        runCatching { repository.releaseCreature(instanceId) }
            .onSuccess { pearls -> _events.emit("Released back into The Blue. $pearls Pearls returned. Your lifetime record remains.") }
            .onFailure { _events.emit(it.message ?: "Could not release that creature.") }
    }

    fun releaseCreaturesByLevel(findId: String, selectionsByLevel: Map<Int, Int>) = viewModelScope.launch {
        runCatching { repository.releaseCreaturesByLevel(findId, selectionsByLevel) }
            .onSuccess { pearls -> _events.emit("Released back into The Blue. $pearls Pearls returned. Your lifetime record remains.") }
            .onFailure { _events.emit(it.message ?: "Could not release those creatures.") }
    }

    fun encounterBeyondBlue(targetCreatureId: String, selectedInstanceIds: List<String>) = viewModelScope.launch {
        runCatching { repository.encounterBeyondBlue(targetCreatureId, selectedInstanceIds) }
            .onSuccess { creature ->
                val definition = CreatureCatalog.get(creature.findId)
                val name = definition?.displayName ?: "A new creature"
                val zone = definition?.zone?.displayName ?: "The Blue"
                _events.emit("$name entered the $zone.")
            }
            .onFailure { _events.emit(it.message ?: "Could not encounter that creature.") }
    }

    fun onDrawFromStillwater(vessel: StillwaterVessel) = viewModelScope.launch {
        val state = uiState.value
        if (vessel.zone !in state.unlockedBlueZones) {
            _events.emit("Progress farther in The Blue to unlock this vessel.")
            return@launch
        }
        if (state.stillwaterClaimableDrops < vessel.dropCost) {
            _events.emit("Not enough Drops yet.")
            return@launch
        }
        if (requiresStillwaterConfirmation(vessel)) {
            pendingStillwaterDrawVessel.value = vessel
        } else {
            drawFromStillwater(vessel)
        }
    }

    fun onConfirmStillwaterDraw(vessel: StillwaterVessel) = viewModelScope.launch {
        pendingStillwaterDrawVessel.value = null
        drawFromStillwater(vessel)
    }

    fun onDismissStillwaterReveal() {
        stillwaterRevealCreature.value = null
    }

    fun onDismissStillwaterDrawConfirmation() {
        pendingStillwaterDrawVessel.value = null
    }

    private suspend fun drawFromStillwater(vessel: StillwaterVessel) {
        if (vessel.zone !in deriveUnlockedBlueZonesFromHistoricalFinds(uiState.value.finds)) {
            _events.emit("Progress farther in The Blue to unlock this vessel.")
            return
        }
        runCatching { repository.drawFromStillwater(vessel, uiState.value.unlockedBlueZones) }
            .onSuccess { instance ->
                stillwaterRevealCreature.value = CreatureCatalog.get(instance.findId)
            }
            .onFailure { _events.emit(it.message ?: "Could not draw from Stillwater.") }
    }

    fun markNotificationViewed(notificationId: String) = viewModelScope.launch {
        repository.markNotificationViewed(notificationId)
    }

    fun markAllNotificationsViewed() = viewModelScope.launch {
        repository.markAllNotificationsViewed()
    }

    fun markTheBlueAnimalsSeen() = viewModelScope.launch {
        repository.markTheBlueAnimalsSeen()
    }

    fun markRoomOpened(roomId: ShellRoomId) = viewModelScope.launch { repository.markRoomOpened(roomId) }
}


internal fun deriveUnlockedBlueZonesFromHistoricalFinds(finds: List<UserShellFindInstanceEntity>): Set<CreatureZone> {
    val deepestReachedOrder = finds
        .asSequence()
        .mapNotNull { instance -> CreatureCatalog.get(instance.findId) }
        .filter { definition -> definition.sourceType != CreatureSourceType.STILLWATER }
        .map { definition -> definition.zone.progressionOrder() }
        .maxOrNull() ?: CreatureZone.SUNLIT_REEF.progressionOrder()

    return CreatureZone.values()
        .filter { zone -> zone.progressionOrder() <= deepestReachedOrder }
        .toSet()
}

private fun CreatureZone.progressionOrder(): Int = when (this) {
    CreatureZone.SUNLIT_REEF -> 0
    CreatureZone.DEEPER_REEF -> 1
    CreatureZone.OPEN_BLUE -> 2
    CreatureZone.GREAT_BLUE -> 3
}

fun ShellUiState.isBlueZoneUnlocked(zone: CreatureZone): Boolean = zone in unlockedBlueZones
