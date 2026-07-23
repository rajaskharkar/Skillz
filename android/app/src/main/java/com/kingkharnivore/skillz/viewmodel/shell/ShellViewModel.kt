package com.kingkharnivore.skillz.viewmodel.shell

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.annotation.StringRes
import com.kingkharnivore.skillz.R
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
import com.kingkharnivore.skillz.domain.achievement.AchievementAccessState
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
import dagger.hilt.android.qualifiers.ApplicationContext
import com.kingkharnivore.skillz.data.repository.shell.AchievementBackfillWorker
import com.kingkharnivore.skillz.data.repository.shell.AchievementBackfillFailureClassifier
import kotlinx.coroutines.delay

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
    val chestFilter: ChestFilterOption = ChestFilterOption.All,
    val pinReplacement: PinReplacementUiState? = null,
    val achievementInitializationState: AchievementInitializationState = AchievementInitializationState.NotStarted
)

data class PinReplacementUiState(val requestedBadgeId: String, val pinnedBadgeIds: List<String>)
sealed interface UiText {
    data class Resource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText
}

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

private data class ShellTransientState(
    val reveal: CreatureDefinition?,
    val vessel: StillwaterVessel?,
    val replacement: PinReplacementUiState?,
    val achievementInitialization: AchievementInitializationState
)

private data class ShellPreferenceState(
    val chestSort: ChestSortOption,
    val badgeCategory: BadgeUiCategory,
    val badgeSort: BadgeSort,
    val acknowledgedBackfillVersion: Int,
    val calmMode: Boolean,
    val chestFilter: ChestFilterOption
)

sealed interface AchievementInitializationState {
    data object NotStarted : AchievementInitializationState
    data object Running : AchievementInitializationState
    data object Complete : AchievementInitializationState
    data class Failed(val retryable: Boolean, val errorCategory: String) : AchievementInitializationState
}

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val repository: ShellRepository,
    private val userPrefs: UserPrefs,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {
    private val _events = MutableSharedFlow<UiText>()
    val events: SharedFlow<UiText> = _events
    private val _achievementInitialization = MutableStateFlow<AchievementInitializationState>(AchievementInitializationState.NotStarted)
    val achievementInitialization: StateFlow<AchievementInitializationState> = _achievementInitialization
    private var automaticBackfillRetries = 0

    init {
        viewModelScope.launch {
            repository.observeLatestAchievementBackfill().collect { backfill ->
                if (backfill?.version == AchievementBackfillWorker.BACKFILL_VERSION) {
                    _achievementInitialization.value = AchievementInitializationState.Complete
                }
            }
        }
        initializeAchievements()
    }

    fun initializeAchievements() {
        if (_achievementInitialization.value in setOf(
                AchievementInitializationState.Running,
                AchievementInitializationState.Complete
            )) return
        viewModelScope.launch {
            _achievementInitialization.value = AchievementInitializationState.Running
            runCatching { repository.backfillAchievements() }
                .onSuccess { _achievementInitialization.value = AchievementInitializationState.Complete }
                .onFailure { failure ->
                    val failureType = AchievementBackfillFailureClassifier.classify(failure)
                    val category = failureType.name
                    val retryScheduled = failureType.retryable && runCatching {
                        AchievementBackfillWorker.enqueue(applicationContext)
                    }.isSuccess
                    Log.w(
                        "AchievementBackfill",
                        "version=${AchievementBackfillWorker.BACKFILL_VERSION} category=$category " +
                            "transient=${failureType.retryable} retryScheduled=$retryScheduled timestamp=${System.currentTimeMillis()}"
                    )
                    _achievementInitialization.value = AchievementInitializationState.Failed(
                        retryable = failureType.retryable,
                        errorCategory = category
                    )
                    if (failureType.retryable && automaticBackfillRetries < 2) {
                        val delayMillis = 1_000L shl automaticBackfillRetries
                        automaticBackfillRetries++
                        delay(delayMillis)
                        initializeAchievements()
                    }
                }
        }
    }

    private val stillwaterRevealCreature = MutableStateFlow<CreatureDefinition?>(null)
    private val pendingStillwaterDrawVessel = MutableStateFlow<StillwaterVessel?>(null)
    private val pinReplacement = MutableStateFlow<PinReplacementUiState?>(null)

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

    private val transientState = combine(
        stillwaterRevealCreature,
        pendingStillwaterDrawVessel,
        pinReplacement,
        _achievementInitialization
    ) { reveal, vessel, replacement, initialization ->
        ShellTransientState(reveal, vessel, replacement, initialization)
    }

    val uiState: StateFlow<ShellUiState> = combine(
        economy,
        ownership,
        memoryAndPreferences,
        transientState
    ) { economy, ownership, memoryAndPreferences, transient ->
        ShellUiState(
            pearlBalance = economy.pearlBalance,
            stillwaterClaimableDrops = economy.stillwaterClaimableDrops,
            stillwaterLifetimeDrops = economy.stillwaterLifetimeDrops,
            stillwaterRevealCreature = transient.reveal,
            pendingStillwaterDrawVessel = transient.vessel,
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
                memoryAndPreferences.achievements.countFloors,
                AchievementAccessState(
                    unlockedBlueZones = deriveUnlockedBlueZonesFromHistoricalFinds(ownership.finds),
                    unlockedStillwaterVessels = StillwaterVessel.entries.filterTo(mutableSetOf()) {
                        it.zone in deriveUnlockedBlueZonesFromHistoricalFinds(ownership.finds)
                    }
                )
            ),
            badgeCategory = memoryAndPreferences.badgeCategory,
            badgeSort = memoryAndPreferences.badgeSort,
            backfillSummary = memoryAndPreferences.achievements.backfill?.takeIf { it.version > memoryAndPreferences.acknowledgedBackfillVersion && (it.discoveredCount > 0 || it.masteryCount > 0 || it.completionCount > 0) },
            masteryCelebration = memoryAndPreferences.achievements.celebration,
            calmMode = memoryAndPreferences.calmMode,
            chestFilter = memoryAndPreferences.chestFilter,
            pinReplacement = transient.replacement,
            achievementInitializationState = transient.achievementInitialization
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
            .onSuccess { result ->
                pinReplacement.value = when (result) {
                    is ShellRepository.PinResult.ReplacementRequired -> PinReplacementUiState(badgeId, result.currentBadgeIds)
                    ShellRepository.PinResult.Pinned, ShellRepository.PinResult.AlreadyPinned -> null
                }
            }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_showcase_failed)) }
    }
    fun dismissPinReplacement() { pinReplacement.value = null }
    fun unpinBadge(badgeId: String) = viewModelScope.launch { repository.unpinBadge(badgeId) }
    fun trackBadge(badgeId: String) = viewModelScope.launch {
        val unlockedZones = deriveUnlockedBlueZonesFromHistoricalFinds(uiState.value.finds)
        runCatching {
            repository.trackBadge(
                badgeId,
                AchievementAccessState(
                    unlockedBlueZones = unlockedZones,
                    unlockedStillwaterVessels = StillwaterVessel.entries.filterTo(mutableSetOf()) { it.zone in unlockedZones }
                )
            )
        }.onFailure { _events.emit(UiText.Resource(R.string.shell_message_track_failed)) }
    }
    fun untrackBadge(badgeId: String) = viewModelScope.launch { repository.untrackBadge(badgeId) }
    fun markBadgeViewed(badgeId: String) = viewModelScope.launch {
        repository.markNotificationViewed("${ShellNotificationType.BADGE.name}:$badgeId")
    }

    fun place(instanceId: String, slotId: String) = viewModelScope.launch {
        runCatching { repository.placeInstance(instanceId, ShellRoomId.FOCUS, slotId) }
            .onSuccess { _events.emit(UiText.Resource(R.string.shell_message_placed)) }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_display_failed)) }
    }

    fun returnToChest(instanceId: String) = viewModelScope.launch {
        runCatching { repository.removePlacement(instanceId) }
            .onSuccess { _events.emit(UiText.Resource(R.string.shell_message_returned)) }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_return_failed)) }
    }

    fun invitePearlObject(findId: String, slotId: String) = viewModelScope.launch {
        runCatching { repository.invitePearlObject(findId, ShellRoomId.FOCUS, slotId) }
            .onSuccess { _events.emit(UiText.Resource(R.string.shell_message_focus_shaped)) }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_shape_failed)) }
    }

    fun invitePearlObjectToChest(findId: String) = viewModelScope.launch {
        runCatching { repository.invitePearlObjectToChest(findId) }
            .onSuccess { _events.emit(UiText.Resource(R.string.shell_message_creature_chest)) }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_invite_failed)) }
    }

    fun upgrade(instanceId: String) = viewModelScope.launch {
        runCatching { repository.upgradeInstance(instanceId) }
            .onSuccess { _events.emit(UiText.Resource(R.string.shell_message_growth_shaped)) }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_shape_reward_failed)) }
    }

    fun growCreature(instanceId: String, origin: String = "BLUE") = viewModelScope.launch {
        val currentLevel = uiState.value.finds
            .firstOrNull { it.instanceId == instanceId }?.animalLevel ?: 1
        runCatching { repository.growCreature(instanceId, "level_up:$instanceId:${currentLevel + 1}", origin) }
            .onSuccess { _events.emit(UiText.Resource(R.string.shell_message_grew_blue)) }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_grow_failed)) }
    }

    fun growCreatureByLevel(findId: String, level: Int, origin: String = "CHEST") = viewModelScope.launch {
        runCatching { repository.growCreatureByLevel(findId, level, origin) }
            .onSuccess { _events.emit(UiText.Resource(R.string.shell_message_grew_chest)) }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_grow_failed)) }
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
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_celebration_failed)) }
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
            .onSuccess { pearls -> _events.emit(UiText.Resource(R.string.shell_message_released, listOf(pearls))) }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_release_failed)) }
    }

    fun releaseCreaturesByLevel(findId: String, selectionsByLevel: Map<Int, Int>) = viewModelScope.launch {
        runCatching { repository.releaseCreaturesByLevel(findId, selectionsByLevel) }
            .onSuccess { pearls -> _events.emit(UiText.Resource(R.string.shell_message_released, listOf(pearls))) }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_release_failed)) }
    }

    fun encounterBeyondBlue(targetCreatureId: String, selectedInstanceIds: List<String>) = viewModelScope.launch {
        runCatching { repository.encounterBeyondBlue(targetCreatureId, selectedInstanceIds) }
            .onSuccess { _events.emit(UiText.Resource(R.string.shell_message_encounter_succeeded)) }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_encounter_failed)) }
    }

    fun onDrawFromStillwater(vessel: StillwaterVessel) = viewModelScope.launch {
        val state = uiState.value
        if (vessel.zone !in state.unlockedBlueZones) {
            _events.emit(UiText.Resource(R.string.shell_message_vessel_locked))
            return@launch
        }
        if (state.stillwaterClaimableDrops < vessel.dropCost) {
            _events.emit(UiText.Resource(R.string.shell_message_drops_insufficient))
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
            _events.emit(UiText.Resource(R.string.shell_message_vessel_locked))
            return
        }
        runCatching { repository.drawFromStillwater(vessel, uiState.value.unlockedBlueZones) }
            .onSuccess { instance ->
                stillwaterRevealCreature.value = CreatureCatalog.get(instance.findId)
            }
            .onFailure { _events.emit(UiText.Resource(R.string.shell_message_stillwater_failed)) }
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
