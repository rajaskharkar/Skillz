package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures

import com.kingkharnivore.skillz.domain.shell.CreaturePlacementBand

data class LifeAgent(
    val key: String,
    val findId: String,
    val representativeIndex: Int,
    val level: Int,
    val sourceType: String?,
    val laneId: String,
    val motionMode: LifeMotionMode,
    val clickable: Boolean = true
)

enum class LifeMotionMode { VISIBLE_LANE, ANCHORED, DRIFT_BOUNDED, PASS_THROUGH, AMBIENT }

data class LifePresencePlan(
    val directIndividuals: List<LifeAgent>,
    val cohorts: List<LifeCohort>,
    val habitatMarks: List<HabitatPresence>,
    val overflowCount: Int
)

data class LifeCohort(
    val key: String,
    val findId: String,
    val count: Int,
    val laneId: String,
    val motionMode: LifeMotionMode,
    val clickable: Boolean = true
)

data class HabitatPresence(
    val key: String,
    val findId: String,
    val countRepresented: Int,
    val kind: HabitatPresenceKind,
    val placementBand: CreaturePlacementBand,
    val alpha: Float,
    val clickable: Boolean = false
)

enum class HabitatPresenceKind { SCHOOL_SHIMMER, POD_SHADOW, BLOOM_GLOW, REEF_CLUSTER, DISTANT_SILHOUETTE, CURRENT_TRAIL, BUBBLE_CLUSTER }