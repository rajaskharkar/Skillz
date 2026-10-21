package com.kingkharnivore.skillz.ui.screen.shell.rooms.green

import com.kingkharnivore.skillz.data.model.entity.green.PlantSpecimenEntity
import com.kingkharnivore.skillz.domain.green.GreenCatalogue
import com.kingkharnivore.skillz.domain.green.PlantFootprint
import kotlin.math.*
import kotlin.random.Random

internal data class GreenPlantPlacement(
    val plantId: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val artworkHeight: Float,
)

/** An organic planting composition: drifts, open ground, perspective, and overlapping foliage. */
internal fun layoutGreenPlants(
    plants: List<PlantSpecimenEntity>,
    width: Float,
    height: Float,
): List<GreenPlantPlacement> {
    if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) return emptyList()
    val sized = plants.mapNotNull { plant ->
        val species = GreenCatalogue.byId[plant.speciesId] ?: return@mapNotNull null
        val extent = when (species.footprint(plant.level)) {
            PlantFootprint.TINY -> 70f
            PlantFootprint.SMALL -> 88f
            PlantFootprint.MEDIUM -> 110f
            PlantFootprint.LARGE -> 136f
            PlantFootprint.XL -> 155f
            PlantFootprint.LANDMARK -> 180f
        }
        plant to extent
    }.sortedWith(compareBy<Pair<PlantSpecimenEntity, Float>> { it.first.createdOrder }.thenBy { it.first.id })
    if (sized.isEmpty()) return emptyList()

    // This is a ground plane, not a set of cells. Canopies may share space. Only their
    // centers repel one another enough to preserve recognizable individual plants.
    val area = sized.sumOf { (it.second * it.second * .45f).toDouble() }.toFloat()
    val aspect = (width / height).coerceIn(.35f, 3f)
    val groundWidth = sqrt(area * aspect)
    val groundHeight = sqrt(area / aspect)
    val clumps = listOf(-.25f to -.27f, .24f to -.02f, -.12f to .29f)
    val bucketSize = 110f
    val buckets = mutableMapOf<Pair<Int, Int>, MutableList<GreenPlantPlacement>>()
    val world = ArrayList<GreenPlantPlacement>(sized.size)
    sized.forEach { (plant, naturalExtent) ->
        val random = Random(plant.id.hashCode())
        val home = Math.floorMod(plant.speciesId.hashCode(), clumps.size)
        var best: GreenPlantPlacement? = null
        var bestPenalty = Float.POSITIVE_INFINITY
        for (attempt in 0 until 24) {
            // Related plants favor the same drift; occasional outliers soften its edges.
            val clump = clumps[if (attempt % 4 == 3) random.nextInt(clumps.size) else home]
            val angle = random.nextFloat() * (2f * PI.toFloat())
            val radius = sqrt(random.nextFloat())
            val rootX = (clump.first + cos(angle) * radius * .34f) * groundWidth
            val rootY = (clump.second + sin(angle) * radius * .29f) * groundHeight
            val depth = (rootY / groundHeight + .5f).coerceIn(0f, 1f)
            val extent = naturalExtent * (.8f + .3f * depth)
            val candidate = GreenPlantPlacement(plant.id, rootX - extent * .41f,
                rootY - extent * 1.05f, extent * .82f, extent * 1.05f, extent)
            val centerX = candidate.x + candidate.width / 2f
            val centerY = candidate.y + candidate.height / 2f
            val bx = floor(centerX / bucketSize).toInt()
            val by = floor(centerY / bucketSize).toInt()
            var penalty = 0f
            for (dx in -1..1) for (dy in -1..1) {
                buckets[bx + dx to by + dy]?.forEach { other ->
                    val distance = hypot(centerX - (other.x + other.width / 2f), centerY - (other.y + other.height / 2f))
                    val separation = (extent + other.artworkHeight) * .25f
                    penalty += (1f - distance / separation).coerceAtLeast(0f).pow(2)
                }
            }
            // Keep the first clear spot, instead of regularizing the scene into a lattice.
            if (penalty < bestPenalty) {
                best = candidate
                bestPenalty = penalty
            }
            if (penalty == 0f) break
        }
        val placement = best!!
        world += placement
        val bucket = floor((placement.x + placement.width / 2f) / bucketSize).toInt() to
            floor((placement.y + placement.height / 2f) / bucketSize).toInt()
        buckets.getOrPut(bucket) { mutableListOf() }.add(placement)
    }
    val left = world.minOf { it.x }
    val top = world.minOf { it.y }
    val sceneWidth = world.maxOf { it.x + it.width } - left
    val sceneHeight = world.maxOf { it.y + it.height } - top
    // Reserve a little air at the edges and frame the whole habitat with one shared zoom.
    val zoom = minOf(width * .94f / sceneWidth, height * .94f / sceneHeight, 1f)
    val offsetX = (width - sceneWidth * zoom) / 2f
    val offsetY = (height - sceneHeight * zoom) / 2f
    return world.map {
        it.copy(x = offsetX + (it.x - left) * zoom, y = offsetY + (it.y - top) * zoom,
            width = it.width * zoom, height = it.height * zoom, artworkHeight = it.artworkHeight * zoom)
    }.sortedWith(compareBy<GreenPlantPlacement> { it.y + it.height }.thenBy { it.plantId })
}

/** Resolve overlapping art by proximity to its center, rather than rectangular z-order. */
internal fun pickGreenPlant(placements: List<GreenPlantPlacement>, x: Float, y: Float): String? =
    placements.asReversed().filter { x >= it.x && x <= it.x + it.width && y >= it.y && y <= it.y + it.height }
        .minByOrNull { (x - it.x - it.width / 2f).pow(2) + (y - it.y - it.height / 2f).pow(2) }?.plantId
