package com.kingkharnivore.skillz.domain.green

import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.GreenPreviewFixtures
import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.layoutGreenPlants
import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.pickGreenPlant
import org.junit.Assert.*
import org.junit.Test

class GreenSceneLayoutTest {
    @Test fun everySpecimenFitsAcrossPopulationsAndViewportShapes() {
        GreenEnvironment.entries.forEach { environment ->
            val species = GreenCatalogue.inEnvironment(environment)
            val plants = (1..250).map { index ->
                GreenPreviewFixtures.specimen(species[(index - 1) % species.size],
                    GreenPreviewFixtures.levels[index % 8], index.toLong())
            }
            listOf(1, 2, 3, 6, 7, 10, 25, 60, 250).forEach { count ->
                listOf(220f to 400f, 600f to 160f, 120f to 90f).forEach { (width, height) ->
                    val layout = layoutGreenPlants(plants.take(count), width, height)
                    assertEquals(plants.take(count).map { it.id }.toSet(), layout.map { it.plantId }.toSet())
                    layout.forEach { slot ->
                        assertTrue(slot.width > 0f && slot.height > 0f && slot.artworkHeight > 0f)
                        assertTrue(slot.x >= -.01f && slot.y >= -.01f)
                        assertTrue(slot.x + slot.width <= width + .01f)
                        assertTrue(slot.y + slot.height <= height + .01f)
                        assertTrue(slot.artworkHeight <= slot.height)
                        assertTrue(slot.artworkHeight * .82f <= slot.width + .01f)

                    }
                }
            }
        }
    }

    @Test fun layoutIsStableAcrossInputOrderAndRefitsOnGrowthAndPlanting() {
        val species = GreenCatalogue.byId.getValue("magnolia")
        val plants = (1..12).map { GreenPreviewFixtures.specimen(species, 1, it.toLong()) }
        val initial = layoutGreenPlants(plants, 220f, 300f)
        assertEquals(initial, layoutGreenPlants(plants.reversed(), 220f, 300f))
        val grown = layoutGreenPlants(plants.map { it.copy(level = 90) }, 220f, 300f)
        assertNotEquals(initial.map { it.width to it.height }, grown.map { it.width to it.height })
        val added = layoutGreenPlants(plants + GreenPreviewFixtures.specimen(species, 1, 13), 220f, 300f)
        assertEquals(13, added.size)
        assertNotEquals(initial.first(), added.first())
    }

    @Test fun emptyAndUnmeasuredScenesAreSafeAndOnePlantIsCentered() {
        val plant = GreenPreviewFixtures.specimen(GreenCatalogue.species.first(), 90, 1)
        assertTrue(layoutGreenPlants(emptyList(), 220f, 300f).isEmpty())
        assertTrue(layoutGreenPlants(listOf(plant), 0f, 300f).isEmpty())
        assertTrue(layoutGreenPlants(listOf(plant), 220f, 0f).isEmpty())
        val slot = layoutGreenPlants(listOf(plant), 220f, 300f).single()
        assertEquals(110f, slot.x + slot.width / 2f, .01f)
        assertEquals(150f, slot.y + slot.height / 2f, .01f)
    }
    @Test(timeout = 5000) fun tenThousandSpecimensFitWithoutQuadraticLayoutWork() {
        val plants = (1..10_000).map { index ->
            GreenPreviewFixtures.specimen(GreenCatalogue.species[index % 60],
                GreenPreviewFixtures.levels[index % 8], index.toLong())
        }
        val layout = layoutGreenPlants(plants, 240f, 400f)
        assertEquals(plants.size, layout.size)
        layout.forEach {
            assertTrue(it.x >= -.01f && it.y >= -.01f)
            assertTrue(it.x + it.width <= 240.01f && it.y + it.height <= 400.01f)
        }
    }

    @Test fun nonGeometryChangesDoNotTriggerAReflowAndInvalidViewportsAreIgnored() {
        val plant = GreenPreviewFixtures.specimen(GreenCatalogue.species.first(), 1, 1)
        val initial = layoutGreenPlants(listOf(plant), 240f, 400f)
        assertEquals(initial, layoutGreenPlants(listOf(plant.copy(level = 2, lastWateredAt = (plant.lastWateredAt ?: plant.plantedAt) + 1)), 240f, 400f))
        listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f).forEach {
            assertTrue(layoutGreenPlants(listOf(plant), it, 400f).isEmpty())
            assertTrue(layoutGreenPlants(listOf(plant), 240f, it).isEmpty())
        }
    }

    @Test fun foliageOverlapsWithoutRowsAndEveryCenterStillSelectsItsOwnPlant() {
        val plants = (1..60).map { index ->
            GreenPreviewFixtures.specimen(GreenCatalogue.inEnvironment(GreenEnvironment.WOODLANDS)[index % 10],
                90, index.toLong())
        }
        val layout = layoutGreenPlants(plants, 240f, 400f)
        val overlaps = layout.indices.sumOf { index ->
            val a = layout[index]
            layout.drop(index + 1).count { b ->
                a.x < b.x + b.width && b.x < a.x + a.width && a.y < b.y + b.height && b.y < a.y + a.height
            }
        }
        assertTrue("Foliage should layer into a habitat", overlaps > plants.size / 2)
        assertTrue("Ground positions should not form rows", layout.map { it.y + it.height }.distinct().size > plants.size * .9)
        assertTrue("Draw from the back of the habitat toward the foreground",
            layout.zipWithNext().all { (a, b) -> a.y + a.height <= b.y + b.height })
        layout.forEach {
            assertEquals(it.plantId, pickGreenPlant(layout, it.x + it.width / 2f, it.y + it.height / 2f))
        }
        assertNull(pickGreenPlant(layout, -1f, -1f))
        assertNull(pickGreenPlant(layout, 241f, 401f))
    }
}
