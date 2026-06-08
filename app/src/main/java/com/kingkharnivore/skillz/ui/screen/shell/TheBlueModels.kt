package com.kingkharnivore.skillz.ui.screen.shell

import com.kingkharnivore.skillz.data.model.entity.shell.ShellPlacementEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellRewardKind
import com.kingkharnivore.skillz.data.model.shell.ShellRoomId
import com.kingkharnivore.skillz.domain.shell.CreatureCatalog
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.domain.shell.CreatureSourceType
import com.kingkharnivore.skillz.domain.shell.CreatureStatus

enum class TheBlueZoneId { SUNLIT_REEF, DEEPER_REEF, OPEN_BLUE, GREAT_BLUE }

data class FormCountUiModel(
    val formStageId: String?,
    val count: Int
)

data class TheBlueAnimalGroupUiModel(
    val findId: String,
    val zoneId: TheBlueZoneId,
    val totalCount: Int,
    val displayedInFocusCount: Int,
    val restingCount: Int,
    val bestFormStageId: String?,
    val formCounts: List<FormCountUiModel>,
    val iconKey: String?,
    val isNew: Boolean,
    val lifetimeEncounteredCount: Int = totalCount,
    val releasedCount: Int = 0,
    val usedBeyondBlueCount: Int = 0,
    val highestLevel: Int = 1,
    val levelCounts: List<FormCountUiModel> = formCounts,
    val flowTimeValueMinutes: Int? = null,
    val releaseValuePearls: Int? = null,
    val releaseValueVariesByLevel: Boolean = false,
    val firstActiveInstanceId: String? = null,
    val firstRestingInstanceId: String? = null,
    val highestLevelActiveInstanceId: String? = null
)

data class TheBlueZoneUiModel(
    val zoneId: TheBlueZoneId,
    val animals: List<TheBlueAnimalGroupUiModel>
)

enum class TheBlueDisplayDisabledReason { NO_FOCUS_SLOT, NO_RESTING_COPY }

internal fun theBlueDisplayDisabledReason(
    focusSlotId: String?,
    firstRestingInstanceId: String?
): TheBlueDisplayDisabledReason? = when {
    focusSlotId == null -> TheBlueDisplayDisabledReason.NO_FOCUS_SLOT
    firstRestingInstanceId == null -> TheBlueDisplayDisabledReason.NO_RESTING_COPY
    else -> null
}

data class TheBlueUiState(
    val totalAnimals: Int,
    val speciesCount: Int,
    val deepestZoneId: TheBlueZoneId?,
    val newAnimalCount: Int,
    val zones: List<TheBlueZoneUiModel>
) {
    val isEmpty: Boolean get() = totalAnimals == 0
}

internal fun buildTheBlueUiState(
    finds: List<UserShellFindInstanceEntity>,
    focusPlacements: List<ShellPlacementEntity>
): TheBlueUiState {
    val displayedIds = focusPlacements
        .filter { it.roomId == ShellRoomId.FOCUS.name }
        .map { it.instanceId }
        .toSet()

    val allAnimalFinds = finds.filter { instance ->
        ShellContentCatalog.find(instance.findId)?.kind == ShellRewardKind.ANIMAL &&
            CreatureCatalog.get(instance.findId)?.sourceType != CreatureSourceType.STILLWATER
    }
    val animalFinds = allAnimalFinds.filter { it.creatureStatus == CreatureStatus.ACTIVE }

    val historicalByFindId = allAnimalFinds.groupBy { it.findId }
    val groups = animalFinds
        .groupBy { it.findId }
        .mapNotNull { (findId, instances) ->
            val definition = ShellContentCatalog.find(findId) ?: return@mapNotNull null
            val zoneId = zoneForFind(findId) ?: return@mapNotNull null
            val historicalInstances = historicalByFindId[findId].orEmpty()
            val displayedCount = instances.count { it.instanceId in displayedIds }
            val restingInstances = instances.filter { it.instanceId !in displayedIds }
            val highestLevelInstance = instances.maxWithOrNull(
                compareBy<UserShellFindInstanceEntity> { it.animalLevel.coerceAtLeast(1) }.thenBy { it.acquiredAt }
            )
            val formCounts = instances
                .groupingBy { it.currentUpgradeStageId }
                .eachCount()
                .map { (stageId, count) -> FormCountUiModel(stageId, count) }
                .sortedByDescending { formOrder(findId, it.formStageId) }
            val activeLevels = instances.map { it.animalLevel.coerceAtLeast(1) }.toSet()
            val singleActiveLevel = activeLevels.singleOrNull()
            TheBlueAnimalGroupUiModel(
                findId = findId,
                zoneId = zoneId,
                totalCount = instances.size,
                displayedInFocusCount = displayedCount,
                restingCount = instances.size - displayedCount,
                bestFormStageId = instances.maxByOrNull { formOrder(findId, it.currentUpgradeStageId) }?.currentUpgradeStageId,
                formCounts = formCounts,
                iconKey = definition.iconKey,
                isNew = instances.any { it.isNew },
                lifetimeEncounteredCount = historicalInstances.size,
                releasedCount = historicalInstances.count { it.creatureStatus == CreatureStatus.RELEASED },
                usedBeyondBlueCount = historicalInstances.count { it.creatureStatus == CreatureStatus.USED_BEYOND_BLUE },
                highestLevel = instances.maxOfOrNull { it.animalLevel.coerceAtLeast(1) } ?: 1,
                levelCounts = instances.groupingBy { it.animalLevel.coerceAtLeast(1) }.eachCount()
                    .map { (level, count) -> FormCountUiModel("Level $level", count) }
                    .sortedByDescending { it.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: 0 },
                flowTimeValueMinutes = CreatureCatalog.get(findId)?.flowTimeValueMinutes ?: CreatureCatalog.get(findId)?.requirementMinutes,
                releaseValuePearls = CreatureCatalog.get(findId)?.let {
                    singleActiveLevel?.let { level -> CreatureEconomy.releaseValuePearls(findId, level) }
                },
                releaseValueVariesByLevel = activeLevels.size > 1,
                firstActiveInstanceId = instances.firstOrNull()?.instanceId,
                firstRestingInstanceId = restingInstances.firstOrNull()?.instanceId,
                highestLevelActiveInstanceId = highestLevelInstance?.instanceId
            )
        }
        .sortedWith(compareBy<TheBlueAnimalGroupUiModel> { it.zoneId.depthOrder() }.thenBy { it.findId })

    val zones = TheBlueZoneId.values().map { zoneId ->
        TheBlueZoneUiModel(
            zoneId = zoneId,
            animals = groups.filter { it.zoneId == zoneId }
        )
    }

    val inhabitedZones = groups.map { it.zoneId }
    return TheBlueUiState(
        totalAnimals = animalFinds.size,
        speciesCount = groups.size,
        deepestZoneId = inhabitedZones.maxByOrNull { it.depthOrder() },
        newAnimalCount = animalFinds.count { it.isNew },
        zones = zones
    )
}

internal fun zoneForFind(findId: String): TheBlueZoneId? = when (findId) {
    ShellContentCatalog.FOCUS_MINNOW -> TheBlueZoneId.SUNLIT_REEF
    ShellContentCatalog.FOCUS_SEAHORSE -> TheBlueZoneId.DEEPER_REEF
    ShellContentCatalog.FOCUS_OCTOPUS -> TheBlueZoneId.DEEPER_REEF
    ShellContentCatalog.FOCUS_MANTA -> TheBlueZoneId.OPEN_BLUE
    ShellContentCatalog.FOCUS_WHALE -> TheBlueZoneId.GREAT_BLUE
    else -> ShellContentCatalog.find(findId)?.depthTier?.let { depth ->
        when (depth) {
            com.kingkharnivore.skillz.data.model.shell.ShellDepthTier.REEF -> TheBlueZoneId.SUNLIT_REEF
            com.kingkharnivore.skillz.data.model.shell.ShellDepthTier.DEEPER_REEF -> TheBlueZoneId.DEEPER_REEF
            com.kingkharnivore.skillz.data.model.shell.ShellDepthTier.OPEN_BLUE -> TheBlueZoneId.OPEN_BLUE
            com.kingkharnivore.skillz.data.model.shell.ShellDepthTier.DEEP_OCEAN -> TheBlueZoneId.GREAT_BLUE
        }
    }
}


internal fun theBlueSequentialNavigationPath(
    current: TheBlueZoneId,
    target: TheBlueZoneId
): List<TheBlueZoneId> {
    val currentOrder = current.depthOrder()
    val targetOrder = target.depthOrder()
    if (currentOrder == targetOrder) return emptyList()
    val step = if (targetOrder > currentOrder) 1 else -1
    return generateSequence(currentOrder + step) { order ->
        val next = order + step
        if ((step > 0 && next <= targetOrder) || (step < 0 && next >= targetOrder)) next else null
    }.map(::theBlueZoneForPage).toList()
}

internal fun theBlueZoneForPage(page: Int): TheBlueZoneId = when (page.coerceIn(0, TheBlueZoneId.values().lastIndex)) {
    0 -> TheBlueZoneId.SUNLIT_REEF
    1 -> TheBlueZoneId.DEEPER_REEF
    2 -> TheBlueZoneId.OPEN_BLUE
    else -> TheBlueZoneId.GREAT_BLUE
}

internal fun TheBlueZoneId.depthOrder(): Int = when (this) {
    TheBlueZoneId.SUNLIT_REEF -> 0
    TheBlueZoneId.DEEPER_REEF -> 1
    TheBlueZoneId.OPEN_BLUE -> 2
    TheBlueZoneId.GREAT_BLUE -> 3
}

private fun formOrder(findId: String, stageId: String?): Int =
    ShellContentCatalog.upgradesFor(findId)
        .firstOrNull { it.upgradeStageId == stageId }
        ?.orderIndex ?: 0
