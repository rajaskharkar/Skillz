package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.land

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Visual scale is deliberately compressed, so tiny species remain legible beside megafauna.
 * These are drawing dimensions, never acquisition, growth, or economy rules. */
object LandAnimalScale {
    private val sizes = buildMap {
        fun group(edge: Float, names: String) = names.split(" ").forEach { put("creature_$it", edge) }
        group(38f, "squirrel jerboa pika scorpion quail partridge hedgehog echidna")
        group(46f, "rabbit marmot meerkat mongoose roadrunner chameleon desert_tortoise chicken duck pheasant")
        group(55f, "raccoon porcupine badger red_panda armadillo fennec_fox arctic_fox snowy_owl goose platypus kinkajou lemur koala")
        group(65f, "fox jackal coyote bobcat caracal serval lynx wolverine pangolin sloth peacock turkey vulture eagle iguana rattlesnake")
        group(76f, "goat sheep mountain_goat bighorn_sheep pig warthog wild_boar capybara wombat wallaby gibbon macaque baboon mandrill monitor_lizard")
        group(86f, "chimpanzee gray_wolf spotted_hyena gazelle chamois tahr vicuna gerenuk alpaca anteater")
        group(100f, "deer reindeer llama donkey ibex oryx addax kangaroo ostrich emu orangutan cheetah leopard cougar snow_leopard takin tapir komodo_dragon")
        group(112f, "gorilla black_bear panda tiger jaguar lion cow horse zebra okapi musk_ox buffalo wildebeest crocodile")
        group(124f, "grizzly_bear polar_bear yak bison moose camel hippopotamus rhinoceros")
        group(140f, "elephant giraffe")
    }
    val speciesIds: Set<String> get() = sizes.keys
    fun edgeDp(id: String): Float = requireNotNull(sizes[id]) { "Missing Land visual scale: $id" }
}

data class LandAnimalPose(val progress: Float, val facing: Float, val walkPhase: Float, val activity: Float)

/** A bounded out-and-back stroll, with a rest at both ends. No accumulated frame state;
 * reopening a page or dropping frames cannot move an animal out of its lane. */
fun landAnimalPose(seconds: Float, index: Int, edgeDp: Float): LandAnimalPose {
    val walk = 7f + edgeDp / 22f
    val rest = 3f + edgeDp / 20f
    val half = walk + rest
    val elapsed = (seconds.coerceAtLeast(0f) + index * 4.37f) % (half * 2f)
    val returning = elapsed >= half
    val local = if (returning) elapsed - half else elapsed
    val fraction = (local / walk).coerceIn(0f, 1f)
    val travel = (1f - cos(fraction * PI.toFloat())) * .5f
    // Turn while stationary: narrow the silhouette briefly rather than snapping direction.
    val turn = ((local - walk - rest + 1.2f) / 1.2f).coerceIn(0f, 1f)
    val facing = (if (returning) -1f else 1f) * cos(turn * PI.toFloat())
    return LandAnimalPose(
        progress = if (returning) 1f - travel else travel,
        facing = facing,
        walkPhase = travel * 4f * 2f * PI.toFloat(),
        activity = if (fraction < 1f) sin(fraction * PI.toFloat()) else 0f
    )
}
