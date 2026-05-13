package com.kingkharnivore.skillz.viewmodel.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.shell.ShellPlacementEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserDiscoveryEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindStackEntity
import com.kingkharnivore.skillz.data.model.shell.ShellRoomId
import com.kingkharnivore.skillz.data.model.shell.StillwaterPerspective
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShellUiState(
    val pearlBalance: Int = 0,
    val stillwaterTotal: Long = 0,
    val perspective: StillwaterPerspective = StillwaterPerspective.BOWLS,
    val finds: List<UserShellFindInstanceEntity> = emptyList(),
    val stacks: List<UserShellFindStackEntity> = emptyList(),
    val focusPlacements: List<ShellPlacementEntity> = emptyList(),
    val badges: List<UserBadgeEntity> = emptyList(),
    val discoveries: List<UserDiscoveryEntity> = emptyList()
)

private data class ShellEconomyState(
    val pearlBalance: Int,
    val stillwaterTotal: Long,
    val perspective: StillwaterPerspective
)

private data class ShellOwnershipState(
    val finds: List<UserShellFindInstanceEntity>,
    val stacks: List<UserShellFindStackEntity>,
    val focusPlacements: List<ShellPlacementEntity>
)

private data class ShellMemoryState(
    val badges: List<UserBadgeEntity>,
    val discoveries: List<UserDiscoveryEntity>
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val repository: ShellRepository
) : ViewModel() {
    private val economy = combine(
        repository.observePearlBalance(),
        repository.observeStillwaterTotal(),
        repository.observeStillwaterPreference().map { pref ->
            pref?.perspective?.let { runCatching { StillwaterPerspective.valueOf(it) }.getOrNull() }
                ?: StillwaterPerspective.BOWLS
        }
    ) { pearls, stillwater, perspective -> ShellEconomyState(pearls, stillwater, perspective) }

    private val ownership = combine(
        repository.observeOwnedFinds(),
        repository.observeStacks(),
        repository.observePlacements(ShellRoomId.FOCUS)
    ) { finds, stacks, placements -> ShellOwnershipState(finds, stacks, placements) }

    private val memory = combine(
        repository.observeEarnedBadges(),
        repository.observeDiscoveries()
    ) { badges, discoveries -> ShellMemoryState(badges, discoveries) }

    val uiState: StateFlow<ShellUiState> = combine(economy, ownership, memory) { economy, ownership, memory ->
        ShellUiState(
            pearlBalance = economy.pearlBalance,
            stillwaterTotal = economy.stillwaterTotal,
            perspective = economy.perspective,
            finds = ownership.finds,
            stacks = ownership.stacks,
            focusPlacements = ownership.focusPlacements,
            badges = memory.badges,
            discoveries = memory.discoveries
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShellUiState())

    fun place(instanceId: String, slotId: String) = viewModelScope.launch {
        runCatching { repository.placeInstance(instanceId, ShellRoomId.FOCUS, slotId) }
    }

    fun returnToChest(instanceId: String) = viewModelScope.launch { repository.removePlacement(instanceId) }

    fun upgrade(instanceId: String) = viewModelScope.launch { runCatching { repository.upgradeInstance(instanceId) } }

    fun setPerspective(perspective: StillwaterPerspective) = viewModelScope.launch {
        repository.updateStillwaterPerspective(perspective)
    }

    fun markRoomOpened(roomId: ShellRoomId) = viewModelScope.launch { repository.markRoomOpened(roomId) }
}
