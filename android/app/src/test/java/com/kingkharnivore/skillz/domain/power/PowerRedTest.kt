package com.kingkharnivore.skillz.domain.power

import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.model.FlowModeConverter
import com.kingkharnivore.skillz.utils.score.ScoreCalculator
import com.kingkharnivore.skillz.utils.shell.*
import com.kingkharnivore.skillz.utils.shell.voyage.*
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.time.*

class PowerRedTest {
    @Test fun scoringRoundsOnceAndPreservesLegacyFlow() {
        assertEquals(15, ScoreCalculator.timeScore(10*60_000L, FlowMode.POWER))
        assertEquals(72, ScoreCalculator.timeScore(40*60_000L, FlowMode.POWER, 1.2))
        assertEquals(61, ScoreCalculator.timeScore(37*60_000L, FlowMode.POWER, 1.1))
        assertEquals(0, ScoreCalculator.timeScore(40*60_000L, FlowMode.SOFT))
        assertEquals(15, ScoreCalculator.breakdownFromDuration(10*60_000L).totalPoints)
        assertEquals(70, ScoreCalculator.breakdownFromDuration(40*60_000L).totalPoints)
        assertEquals(68, ScoreCalculator.timeScore(37*60_000L, FlowMode.POWER, 1.1) + 7)
        assertEquals(FlowMode.FLOW, FlowModeConverter().decode(0))
        assertEquals(FlowMode.SOFT, FlowModeConverter().decode(1))
        assertEquals(FlowMode.POWER, FlowModeConverter().decode(2))
    }
    @Test fun catalogMatchesEverySpecifiedIdPriceAndOrder() {
        val rows = Regex("\\| `([a-z_]+)` \\| [^|]+ \\| ([0-9,]+) \\|").findAll(File("docs/features/power-red/08_RED_CREATURE_ECONOMY.md").readText()).map { it.groupValues[1] to it.groupValues[2].replace(",", "").toInt() }.toList()
        assertEquals(69, rows.size)
        assertEquals(rows, RedCreatureCatalog.entries.map { it.id to it.pebbleCost })
        assertEquals(listOf(14,21,34), RedEra.entries.map { era -> RedCreatureCatalog.entries.count { it.era == era } })
        assertEquals(69, RedCreatureCatalog.byId.size)
        assertEquals((0..68).toList(), RedCreatureCatalog.entries.map { it.catalogOrder })
        assertTrue(RedCreatureCatalog.entries.all { it.pebbleCost > 0 && it.active && CreatureCatalog.get(it.id) != null })
        assertEquals(3, RedEra.entries.size)
        val art = com.kingkharnivore.skillz.ui.screen.shell.rooms.red.RedDinosaurVisuals
        assertEquals(RedCreatureCatalog.entries.map { it.id.substringAfter('_') }.toSet(),art.byName.keys)
        assertTrue(RedCreatureCatalog.entries.all { art.get(it.id).edgeDp in 50f..160f && it.assetId==it.id })
    }
    private fun sessions(historyPower: Int, historySize: Int = 10, recentPower: Int = 2, recentSize: Int = 8) = List(historySize+recentSize) { i ->
        SessionEntity(id=i.toLong(), title="", description="",tagId=1,startTime=i.toLong(),endTime=i+1L,durationMs=60_000,
            mode=if (if(i < historySize) i < historyPower else i-historySize < recentPower) FlowMode.POWER else FlowMode.FLOW)
    }
    @Test fun adaptationSamplesAndBoundaries() {
        assertFalse(AdaptationPolicy.qualifies(sessions(5)))
        assertTrue(AdaptationPolicy.qualifies(sessions(6)))
        assertTrue(AdaptationPolicy.qualifies(sessions(7)))
        assertFalse(AdaptationPolicy.qualifies(sessions(6,recentPower=3)))
        assertFalse(AdaptationPolicy.qualifies(sessions(6,historySize=9)))
        assertFalse(AdaptationPolicy.qualifies(sessions(6,recentSize=7)))
        assertFalse(AdaptationPolicy.qualifies(sessions(6).mapIndexed { i,s -> s.copy(tagId=i.toLong()) }))
    }
    @Test fun exactEraAndFamilyBadges() {
        val all = RedCreatureCatalog.byId.keys
        val familyText = File("docs/features/power-red/10_RED_BADGES.md").readText()
        val families = Regex("### (\\w+)\\n(.*?)(?=\\n###|\\n##)", RegexOption.DOT_MATCHES_ALL).findAll(familyText).toList()
        families.forEach { section ->
            val ids = Regex("`((?:triassic|jurassic|cretaceous)_\\w+)`").findAll(section.groupValues[2]).map { it.groupValues[1] }.toSet()
            if(ids.isNotEmpty()) assertEquals(ids, RedBadgeCatalog.byId.getValue("red_${section.groupValues[1].lowercase()}").species)
        }
        RedBadgeCatalog.specs.filter { it.species.isNotEmpty() }.forEach { spec ->
            assertEquals(spec.species.size, RedBadgeCatalog.progress(spec, all, all, emptyList()))
            assertEquals(0, RedBadgeCatalog.progress(spec, emptySet(), emptySet(), emptyList()))
            if(spec.target > 1) assertTrue(RedBadgeCatalog.progress(spec, spec.species.drop(1).toSet(), spec.species.drop(1).toSet(), emptyList()) < spec.target)
        }
        assertEquals(26, RedBadgeCatalog.specs.size)
    }
    @Test fun powerBadgeThresholds() {
        val flows = List(10) { i -> sessions(10)[0].copy(id=i.toLong(),tagId=i.toLong(), durationMs=10*3_600_000L) }
        RedBadgeCatalog.specs.filter { it.id.startsWith("power_") && it.id != "power_adaptation" }.forEach {
            assertTrue(it.id, RedBadgeCatalog.progress(it, emptySet(), emptySet(), flows) >= it.target)
            assertEquals(0, RedBadgeCatalog.progress(it, emptySet(), emptySet(), flows.map { s -> s.copy(mode=FlowMode.FLOW) }))
        }
    }
    @Test fun voyageRangesJourneysShareAndZeroDenominator() {
        val now = Instant.parse("2026-10-01T18:00:00Z")
        fun source(id: Long, days: Long, mode: FlowMode, duration: Long) = VoyageSourceFlow(id,"Task","Journey",1,now.minusSeconds(days*86400).toEpochMilli(),duration,1,mode == FlowMode.SOFT,null,null,null,mode=mode,tagId=id%2)
        val stats = PowerVoyageCalculator.calculate(listOf(source(1,0,FlowMode.POWER,60_000),source(2,8,FlowMode.POWER,120_000),source(3,35,FlowMode.FLOW,180_000),source(4,91,FlowMode.POWER,240_000),source(5,0,FlowMode.SOFT,999_999)),now,ZoneId.of("UTC"))
        assertEquals(420_000L,stats.lifetime.durationMs)
        assertEquals(3,stats.lifetime.sessions)
        assertEquals(.7,stats.lifetime.share,.0001)
        assertEquals(1,stats.windows.getValue(7).sessions)
        assertEquals(2,stats.windows.getValue(30).sessions)
        assertEquals(2,stats.windows.getValue(90).sessions)
        assertEquals(2,stats.lifetime.journeys.size)
        assertTrue(stats.lifetime.journeys.all { it.months.isNotEmpty() })
        assertEquals(0.0,PowerVoyageCalculator.calculate(emptyList(),now,ZoneId.of("UTC")).lifetime.share,0.0)
    }

    @Test fun scenesUseOnlyActiveOwnedEraCreaturesWithBoundedStablePopulation() {
        val owned = RedCreatureCatalog.entries.mapIndexed { i, entry ->
            com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity(
                "owned-$i",entry.id,i.toLong(),"red_purchase",null,null,null,false,true)
        }
        RedEra.entries.forEach { era ->
            val population = RedScenePopulation.populate(era,owned)
            assertTrue(population.size <= RedScenePopulation.VISIBLE_CAP)
            assertEquals(population,RedScenePopulation.populate(era,owned.reversed()))
            assertTrue(population.all { RedCreatureCatalog.byId.getValue(it.speciesId).era == era })
            assertEquals(emptyList<RedSceneCreature>(),RedScenePopulation.populate(era,emptyList()))
            assertEquals(emptyList<RedSceneCreature>(),RedScenePopulation.populate(era,owned.map { it.copy(creatureStatus=CreatureStatus.RELEASED) }))
        }
    }
}
