package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.land.*
import com.kingkharnivore.skillz.utils.shell.LandCreatureCatalog
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class LandAnimalMotionTest {
    @Test fun everyLandSpeciesHasAnExplicitScale() {
        assertEquals(LandCreatureCatalog.all.map { it.creatureId }.toSet(), LandAnimalScale.speciesIds)
    }
    @Test fun relativeScaleSurvivesLevelGrowthAndCommonViewportFit() {
        fun edge(name:String)=LandAnimalScale.edgeDp("creature_$name")
        assertTrue(edge("chimpanzee") > edge("squirrel") * 2f)
        assertTrue(edge("gorilla") > edge("chimpanzee") * 1.12f)
        assertTrue(edge("horse") > edge("goat") * 1.12f)
        assertTrue(edge("elephant") > edge("horse") * 1.12f)
        assertTrue(edge("camel") > edge("meerkat") * 2f)
    }
    @Test fun longRunningMotionIsBoundedAndContinuousAcrossRestsAndTurns() {
        listOf(38f,86f,140f).forEach { edge ->
            repeat(6) { index ->
                var previous=landAnimalPose(0f,index,edge)
                repeat(60000) { frame ->
                    val pose=landAnimalPose(frame/60f,index,edge)
                    assertTrue(pose.progress in 0f..1f)
                    assertTrue(pose.activity in 0f..1f)
                    assertTrue(pose.facing in -1f..1f)
                    assertTrue(abs(pose.progress-previous.progress)<.005f)
                    assertTrue(abs(pose.facing-previous.facing)<.06f)
                    previous=pose
                }
            }
        }
    }
    @Test fun walkingDirectionMatchesTravelAndIncludesStationaryRests() {
        var rests=0
        var walks=0
        repeat(3000) { frame ->
            val pose=landAnimalPose(frame/30f,0,86f)
            val next=landAnimalPose((frame+1)/30f,0,86f)
            if(pose.activity>.01f && next.activity>.01f) {
                assertTrue((next.progress-pose.progress)*pose.facing>=0f)
                walks++
            }
            if(pose.activity==0f) { assertEquals(pose.progress,next.progress,.001f);rests++ }
        }
        assertTrue(rests>300)
        assertTrue(walks>300)
    }
}
