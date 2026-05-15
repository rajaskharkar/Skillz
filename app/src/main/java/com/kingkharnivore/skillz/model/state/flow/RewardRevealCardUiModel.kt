package com.kingkharnivore.skillz.model.state.flow

data class RewardRevealCardUiModel(
    val id: String,
    val type: RewardRevealCardType,
    val title: String,
    val subtitle: String? = null,
    val body: String? = null,
    val chip: String? = null,
    val amountText: String? = null,
    val iconKey: String? = null,
    val destinationHint: String? = null,
    val contentDescription: String,
    val animationStyle: RewardRevealAnimationStyle = RewardRevealAnimationStyle.NONE
)

enum class RewardRevealCardType {
    SCORE_BREAKDOWN,
    STILLWATER_RESULT,
    SHELL_BRIDGE,
    ANIMAL,
    OBJECT,
    TRINKET,
    DISCOVERY,
    BADGE,
    SOFT_RULE,
    STILLWATER_PERSPECTIVE,
    ARC_SCORE,
    ARC_ANIMALS,
    ARC_OBJECTS,
    ARC_TRINKETS,
    ARC_DISCOVERIES,
    ARC_BADGES,
    EMPTY_SHELL_MEANING,
    ARC_STORY_PLACEHOLDER
}

enum class RewardRevealAnimationStyle {
    NONE,
    PEARL_GLOW,
    STILLWATER_RIPPLE,
    ANIMAL_SWIM,
    ANIMAL_BOB,
    ANIMAL_GLIDE,
    ANIMAL_SHADOW,
    OBJECT_PLACE,
    DISCOVERY_REVEAL,
    BADGE_STAMP
}
