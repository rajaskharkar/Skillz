package com.kingkharnivore.skillz.ui.screen.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellDepthTier
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.data.model.shell.ShellRewardKind
import com.kingkharnivore.skillz.domain.shell.CreatureStatus
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState

internal fun displayedInstanceIds(uiState: ShellUiState): Set<String> =
    uiState.focusPlacements.map { it.instanceId }.toSet()

internal fun isUserVisibleShellFind(def: ShellFindDefinition?): Boolean =
    def != null && def.kind != ShellRewardKind.TRINKET

internal fun canDisplayInstance(instance: UserShellFindInstanceEntity, def: ShellFindDefinition?): Boolean =
    isUserVisibleShellFind(def) && (def?.kind != ShellRewardKind.ANIMAL || instance.creatureStatus == CreatureStatus.ACTIVE)

internal fun restingFinds(uiState: ShellUiState): List<UserShellFindInstanceEntity> {
    val displayed = displayedInstanceIds(uiState)
    return uiState.finds.filter { item ->
        item.instanceId !in displayed && canDisplayInstance(item, ShellContentCatalog.find(item.findId))
    }
}



internal fun hasRestingPlaceableFinds(uiState: ShellUiState): Boolean = restingFinds(uiState).any { item ->
    val def = ShellContentCatalog.find(item.findId)
    def?.placeable == true
}

internal fun hasAffordablePearlShape(uiState: ShellUiState): Boolean =
    uiState.finds.any { item ->
        val def = ShellContentCatalog.find(item.findId) ?: return@any false
        if (def.kind != ShellRewardKind.OBJECT) return@any false
        val next = ShellContentCatalog.nextUpgrade(def.findId, item.currentUpgradeStageId) ?: return@any false
        next.pearlCost <= uiState.pearlBalance
    } || ShellContentCatalog.focusPearlObjects.any { (it.pearlCost ?: Int.MAX_VALUE) <= uiState.pearlBalance }

internal fun currentFormOrder(instance: UserShellFindInstanceEntity): Int =
    ShellContentCatalog.upgradesFor(instance.findId)
        .firstOrNull { it.upgradeStageId == instance.currentUpgradeStageId }
        ?.orderIndex ?: 0

internal fun unseenNotificationCount(uiState: ShellUiState): Int =
    uiState.finds.count { it.isNew && isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) } +
            uiState.stacks.count { it.isNew && isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) } +
            uiState.badges.count { it.isNew } +
            uiState.discoveries.count { it.isNew }

internal fun hasAffordableFocusPearlAction(uiState: ShellUiState): Boolean {
    val displayed = displayedInstanceIds(uiState)
    val hasAffordableDisplayedUpgrade = uiState.finds.any { item ->
        if (item.instanceId !in displayed) return@any false
        val def = ShellContentCatalog.find(item.findId) ?: return@any false
        if (def.kind != ShellRewardKind.OBJECT) return@any false
        val next = ShellContentCatalog.nextUpgrade(findId = def.findId, currentStageId = item.currentUpgradeStageId) ?: return@any false
        next.pearlCost <= uiState.pearlBalance
    }
    if (hasAffordableDisplayedUpgrade) return true
    val occupiedSlots = uiState.focusPlacements.map { it.slotId }.toSet()
    val emptySlots = ShellContentCatalog.focusSlots.filter { slot -> slot.slotId !in occupiedSlots }
    if (emptySlots.isEmpty()) return false
    return ShellContentCatalog.focusPearlObjects.any { def ->
        val cost = def.pearlCost ?: return@any false
        cost <= uiState.pearlBalance && emptySlots.any { slot -> ShellContentCatalog.isCompatibleWithSlot(slot, def) }
    }
}

internal fun hasEmptyNookForNewRestingObject(uiState: ShellUiState): Boolean {
    val occupied = uiState.focusPlacements.map { it.slotId }.toSet()
    val emptySlots = ShellContentCatalog.focusSlots.filter { it.slotId !in occupied }
    if (emptySlots.isEmpty()) return false
    return restingFinds(uiState).any { item ->
        val def = ShellContentCatalog.find(item.findId) ?: return@any false
        item.isNew && canDisplayInstance(item, def) && def.placeable && emptySlots.any { slot -> ShellContentCatalog.isCompatibleWithSlot(slot, def) }
    }
}

@Composable
internal fun kindLabel(kind: ShellRewardKind): String = when (kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_kind_animal)
    ShellRewardKind.OBJECT -> stringResource(R.string.shell_kind_object)
    ShellRewardKind.TRINKET -> stringResource(R.string.shell_kind_trinket)
    ShellRewardKind.DISCOVERY -> stringResource(R.string.shell_kind_discovery)
}

@Composable
internal fun depthLabel(depth: ShellDepthTier?): String? = when (depth) {
    ShellDepthTier.REEF -> stringResource(R.string.shell_depth_reef)
    ShellDepthTier.DEEPER_REEF -> stringResource(R.string.shell_depth_deeper_reef)
    ShellDepthTier.OPEN_BLUE -> stringResource(R.string.shell_depth_open_blue)
    ShellDepthTier.DEEP_OCEAN -> stringResource(R.string.shell_depth_deep_ocean)
    null -> null
}
