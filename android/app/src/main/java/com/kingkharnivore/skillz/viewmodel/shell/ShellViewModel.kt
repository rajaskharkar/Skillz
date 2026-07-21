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
import com.kingkharnivore.skillz.data.repository.shell.ShellNotificationType
import com.kingkharnivore.skillz.utils.shell.ChestSortOption
import com.kingkharnivore.skillz.utils.shell.ChestFilterOption
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureDefinition
import com.kingkharnivore.skillz.utils.shell.CreatureSourceType
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import com.kingkharnivore.skillz.utils.shell.StillwaterVessel
import com.kingkharnivore.skillz.utils.user.UserPrefs
import com.kingkharnivore.skillz.utils.shell.requiresStillwaterConfirmation
import com.kingkharnivore.skillz.domain.achievement.BadgeDashboard
import com.kingkharnivore.skillz.domain.achievement.BadgeDashboardCalculator
import com.kingkharnivore.skillz.domain.achievement.BadgeSort
import com.kingkharnivore.skillz.domain.achievement.BadgeUiCategory
import com.kingkharnivore.skillz.data.model.entity.shell.BadgePinEntity
import com.kingkharnivore.skillz.data.model.entity.shell.BadgeTrackingEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CreatureDiscoveryEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CreatureMasteryEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CollectionCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.AchievementBackfillEntity
import com.kingkharnivore.skillz.data.model.entity.shell.MasteryCelebrationEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.BadgeCountFloorEntity
import com.kingkharnivore.skillz.domain.achievement.CelebrationStage
import com.kingkharnivore.skillz.domain.achievement.MasteryCelebrationStateMachine
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
    val objectiveCompletions: List<ObjectiveCompletionEntity> = emptyList(),
    val chestSortOption: ChestSortOption = ChestSortOption.Level,
    val badgeDashboard: BadgeDashboard? = null,
    val badgeCategory: BadgeUiCategory = BadgeUiCategory.ALL,
    val badgeSort: BadgeSort = BadgeSort.RECOMMENDED,
    val backfillSummary: AchievementBackfillEntity? = null,
    val masteryCelebration: MasteryCelebrationEventEntity? = null,
    val calmMode: Boolean = false,
    val chestFilter: ChestFilterOption = ChestFilterOption.All
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

private data class ShellMemoryAndPreferenceState(
    val memory: ShellMemoryState,
    val chestSortOption: ChestSortOption,
    val badgeCategory: BadgeUiCategory,
    val badgeSort: BadgeSort,
    val acknowledgedBackfillVersion: Int,
    val calmMode: Boolean,
    val chestFilter: ChestFilterOption,
    val achievements: ShellAchievementState
)

private data class ShellAchievementState(
    val pins: List<BadgePinEntity>, val tracking: List<BadgeTrackingEntity>,
    val discoveries: List<CreatureDiscoveryEntity>, val masteries: List<CreatureMasteryEventEntity>,
    val completions: List<CollectionCompletionEntity>,
    val backfill: AchievementBackfillEntity?,
    val celebration: MasteryCelebrationEventEntity?,
    val countFloors: List<BadgeCountFloorEntity>
)
private data class ShellAchievementCore(
    val pins: List<BadgePinEntity>, val tracking: List<BadgeTrackingEntity>,
    val discoveries: List<CreatureDiscoveryEntity>, val masteries: List<CreatureMasteryEventEntity>
)
private data class ShellAchievementPersistence(
    val completions: List<CollectionCompletionEntity>, val backfill: AchievementBackfillEntity?,
    val celebration: MasteryCelebrationEventEntity?, val floors: List<BadgeCountFloorEntity>
)

private data class ShellPreferenceState(
    val chestSort: ChestSortOption,
    val badgeCategory: BadgeUiCategory,
    val badgeSort: BadgeSort,
    val acknowledgedBackfillVersion: Int,
    val calmMode: Boolean,
    val chestFilter: ChestFilterOption
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val repository: ShellRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {
    init { viewModelScope.launch { runCatching { repository.backfillAchievements() } } }
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

    private val memoryAndPreferences = combine(
        memory,
        combine(
            combine(userPrefs.chestSortOption, userPrefs.badgeCategory, userPrefs.badgeSort,
                userPrefs.acknowledgedBackfillVersion, userPrefs.calmMode) { chest, category, sort, acknowledged, calm ->
                ShellPreferenceState(chest, category, sort, acknowledged, calm, ChestFilterOption.All)
            }, userPrefs.chestFilter
        ) { preferences, filter -> preferences.copy(chestFilter = filter) },
        combine(
            combine(repository.observeBadgePins(), repository.observeBadgeTracking(),
                repository.observeCreatureDiscoveries(), repository.observeCreatureMasteries()) { pins, tracking, discoveries, masteries ->
                ShellAchievementCore(pins, tracking, discoveries, masteries)
            },
            combine(
                combine(repository.observeCollectionCompletions(), repository.observeLatestAchievementBackfill(),
                    repository.observePendingMasteryCelebration()) { completions, backfill, celebration -> Triple(completions, backfill, celebration) },
                repository.observeBadgeCountFloors()
            ) { aggregate, floors -> ShellAchievementPersistence(aggregate.first, aggregate.second, aggregate.third, floors) }
        ) { core, persistence -> ShellAchievementState(core.pins, core.tracking, core.discoveries, core.masteries,
            persistence.completions, persistence.backfill, persistence.celebration, persistence.floors) }
    ) { memory, preferences, achievements -> ShellMemoryAndPreferenceState(
        memory, preferences.chestSort, preferences.badgeCategory, preferences.badgeSort,
        preferences.acknowledgedBackfillVersion, preferences.calmMode, preferences.chestFilter, achievements
    ) }

    val uiState: StateFlow<ShellUiState> = combine(
        economy,
        ownership,
        memoryAndPreferences,
        stillwaterRevealCreature,
        pendingStillwaterDrawVessel
    ) { economy, ownership, memoryAndPreferences, revealCreature, pendingVessel ->
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
            badges = memoryAndPreferences.memory.badges,
            discoveries = memoryAndPreferences.memory.discoveries,
            objectiveCompletions = memoryAndPreferences.memory.objectiveCompletions,
            chestSortOption = memoryAndPreferences.chestSortOption,
            badgeDashboard = BadgeDashboardCalculator.calculate(
                memoryAndPreferences.memory.badges, ownership.finds,
                memoryAndPreferences.achievements.discoveries, memoryAndPreferences.achievements.masteries,
                memoryAndPreferences.achievements.completions,
                memoryAndPreferences.achievements.pins, memoryAndPreferences.achievements.tracking,
                memoryAndPreferences.achievements.countFloors
            ),
            badgeCategory = memoryAndPreferences.badgeCategory,
            badgeSort = memoryAndPreferences.badgeSort,
            backfillSummary = memoryAndPreferences.achievements.backfill?.takeIf { it.version > memoryAndPreferences.acknowledgedBackfillVersion && (it.discoveredCount > 0 || it.masteryCount > 0 || it.completionCount > 0) },
            masteryCelebration = memoryAndPreferences.achievements.celebration,
            calmMode = memoryAndPreferences.calmMode,
            chestFilter = memoryAndPreferences.chestFilter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShellUiState())


    fun setChestSortOption(option: ChestSortOption) = viewModelScope.launch {
        if (uiState.value.chestSortOption != option) {
            userPrefs.setChestSortOption(option)
        }
    }
    fun setChestFilter(option: ChestFilterOption) = viewModelScope.launch { userPrefs.setChestFilter(option) }
    fun setBadgeCategory(value: BadgeUiCategory) = viewModelScope.launch { userPrefs.setBadgeCategory(value) }
    fun setBadgeSort(value: BadgeSort) = viewModelScope.launch { userPrefs.setBadgeSort(value) }
    fun acknowledgeBackfill(version: Int) = viewModelScope.launch { userPrefs.acknowledgeBackfill(version) }

    fun pinBadge(badgeId: String, replaceBadgeId: String? = null) = viewModelScope.launch {
        runCatching { repository.pinBadge(badgeId, replaceBadgeId) }
            .onFailure { _events.emit(it.message ?: "Could not update Your Showcase.") }
    }
    fun unpinBadge(badgeId: String) = viewModelScope.launch { repository.unpinBadge(badgeId) }
    fun movePinnedBadge(badgeId: String, direction: Int) = viewModelScope.launch { repository.movePinnedBadge(badgeId, direction) }
    fun trackBadge(badgeId: String) = viewModelScope.launch {
        runCatching { repository.trackBadge(badgeId) }.onFailure { _events.emit(it.message ?: "Could not track this badge.") }
    }
    fun untrackBadge(badgeId: String) = viewModelScope.launch { repository.untrackBadge(badgeId) }
    fun markBadgeViewed(badgeId: String) = viewModelScope.launch {
        repository.markNotificationViewed("${ShellNotificationType.BADGE.name}:$badgeId")
    }

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

    fun growCreature(instanceId: String, origin: String = "BLUE") = viewModelScope.launch {
        val currentLevel = uiState.value.finds
            .firstOrNull { it.instanceId == instanceId }?.animalLevel ?: 1
        runCatching { repository.growCreature(instanceId, "level_up:$instanceId:${currentLevel + 1}", origin) }
            .onSuccess { _events.emit("Your creature grew inside The Blue.") }
            .onFailure { _events.emit(it.message ?: "Could not grow that creature.") }
    }

    fun growCreatureByLevel(findId: String, level: Int, origin: String = "CHEST") = viewModelScope.launch {
        runCatching { repository.growCreatureByLevel(findId, level, origin) }
            .onSuccess { _events.emit("Your creature grew inside The Chest.") }
            .onFailure { _events.emit(it.message ?: "Could not grow that creature.") }
    }

    fun beginCelebration() = transitionCelebration { stage -> MasteryCelebrationStateMachine.begin(stage) }
    fun advanceCelebration(reducedMotion: Boolean = false) = transitionCelebration { stage ->
        MasteryCelebrationStateMachine.advance(stage, reducedMotion)
    }
    fun skipCelebration() = transitionCelebration { MasteryCelebrationStateMachine.skip() }
    fun completeCelebration(onCompleted: () -> Unit = {}) = viewModelScope.launch {
        val event = uiState.value.masteryCelebration ?: return@launch
        runCatching { repository.updateCelebration(event.eventId, MasteryCelebrationStateMachine.complete()) }
            .onSuccess { onCompleted() }
            .onFailure { _events.emit(it.message ?: "Could not close the Mastery summary.") }
    }

    private fun transitionCelebration(
        transition: (CelebrationStage) -> com.kingkharnivore.skillz.domain.achievement.CelebrationTransition
    ) = viewModelScope.launch {
        val event = uiState.value.masteryCelebration ?: return@launch
        val stage = runCatching { CelebrationStage.valueOf(event.presentationStage) }
            .getOrDefault(CelebrationStage.FINAL_SUMMARY)
        repository.updateCelebration(event.eventId, transition(stage))
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
