package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.kingkharnivore.skillz.utils.shell.CreatureDefinition
import com.kingkharnivore.skillz.utils.shell.CreaturePlacementBand
import com.kingkharnivore.skillz.utils.shell.CreatureSceneBehavior
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel

data class TheBlueRenderedCreature(
    val animal: TheBlueAnimalGroupUiModel,
    val definition: CreatureDefinition,
    val center: Offset,
    val visualBounds: Rect,
    val tapBounds: Rect,
    val scale: Float,
    val alpha: Float,
    val zIndex: Float,
    val sceneBehavior: CreatureSceneBehavior,
    val placementBand: CreaturePlacementBand,
    val driftSeed: Float,
    val glowing: Boolean,
    val rendererKey: String,
    val facingRight: Boolean,
    val clickable: Boolean = true
)
