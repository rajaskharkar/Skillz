package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.land

import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures.PresenceAccounting
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.lifePresencePlan
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureDefinition
import com.kingkharnivore.skillz.utils.shell.CreatureScaleClass

internal data class LandAnimalPresence(
    val key: String,
    val animal: TheBlueAnimalGroupUiModel,
    val definition: CreatureDefinition,
    val representativeIndex: Int,
    val countRepresented: Int,
    val isCohort: Boolean
) {
    // Match Sea's distant cohort silhouette treatment, keeping Land's species proportions.
    val scale: Float get() = if (!isCohort) 1f else when (definition.scaleClass) {
        CreatureScaleClass.GIANT, CreatureScaleClass.LEGENDARY -> .72f
        CreatureScaleClass.LARGE -> .76f
        else -> .82f
    }
}

/**
 * Sea's presence plan controls per-species individual limits and overflow. Land only adapts
 * placement to walking lanes: reserve a place for each owned species, then share spare lanes
 * among copies. Individuals that cannot fit join the cohort, as with Sea's collision fallback.
 * Work and animation count depend on species and available lanes, never total ownership.
 */
internal fun landAnimalPresence(
    animals: List<TheBlueAnimalGroupUiModel>,
    laneCapacity: Int
): List<LandAnimalPresence> {
    val groups = animals.filter { it.totalCount > 0 }.sortedBy { it.findId }
    val definitions = groups.map { CreatureCatalog.require(it.findId) }
    val plans = groups.mapIndexed { index, animal -> lifePresencePlan(animal, definitions[index]) }
    val slots = IntArray(groups.size) { 1 }
    var remainingSlots = (laneCapacity - groups.size).coerceAtLeast(0)
    while (remainingSlots > 0) {
        var allocated = false
        groups.indices.forEach { index ->
            val desired = plans[index].directIndividuals.size + plans[index].cohorts.size
            if (remainingSlots > 0 && slots[index] < desired) {
                slots[index]++
                remainingSlots--
                allocated = true
            }
        }
        if (!allocated) break
    }
    return buildList {
        groups.forEachIndexed { index, animal ->
            val plan = plans[index]
            val accounting = PresenceAccounting(owned = animal.totalCount)
            val directCount = minOf(plan.directIndividuals.size,
                if (animal.totalCount > slots[index]) slots[index] - 1 else slots[index])
            plan.directIndividuals.take(directCount).forEach { agent ->
                add(LandAnimalPresence(agent.key, animal, definitions[index],
                    agent.representativeIndex, 1, isCohort = false))
                accounting.representedDirect++
            }
            if (accounting.remaining > 0) {
                add(LandAnimalPresence("${animal.findId}:cohort", animal, definitions[index],
                    plan.directIndividuals.size, accounting.remaining, isCohort = true))
            }
        }
    }
}
