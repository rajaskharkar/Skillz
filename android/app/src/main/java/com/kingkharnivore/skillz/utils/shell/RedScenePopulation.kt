package com.kingkharnivore.skillz.utils.shell

import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity

enum class RedPopulationBand { SPARSE, GROWING, ESTABLISHED }
data class RedSceneCreature(val speciesId: String, val x: Float, val y: Float, val predator: Boolean)
object RedScenePopulation {
    const val VISIBLE_CAP = 12
    const val GROWING_COUNT = 6
    const val ESTABLISHED_COUNT = 18
    fun band(count: Int) = when { count >= ESTABLISHED_COUNT -> RedPopulationBand.ESTABLISHED; count >= GROWING_COUNT -> RedPopulationBand.GROWING; else -> RedPopulationBand.SPARSE }
    /** Pure presentation: no reward callbacks, timers, persisted coordinates, or hidden assets. */
    fun populate(era: RedEra, owned: List<UserShellFindInstanceEntity>): List<RedSceneCreature> {
        val eligible = owned.filter { it.creatureStatus == CreatureStatus.ACTIVE && RedCreatureCatalog.byId[it.findId]?.era == era }
            .sortedWith(compareByDescending<UserShellFindInstanceEntity> { it.acquiredAt }.thenBy { it.instanceId })
        val varied = eligible.distinctBy { it.findId }
        val cap = when(band(eligible.size)) { RedPopulationBand.SPARSE -> 5; RedPopulationBand.GROWING -> 8; RedPopulationBand.ESTABLISHED -> VISIBLE_CAP }
        val selected = (varied + eligible.filter { copy -> varied.none { it.instanceId == copy.instanceId } }).take(cap)
        return selected.mapIndexed { index, creature ->
            val predator = creature.findId in RedCreatureCatalog.predators
            RedSceneCreature(creature.findId, .10f + (index % 6) * .155f, if (predator) .39f + (index / 6) * .09f else .66f + (index / 6) * .18f, predator)
        }
    }
}
