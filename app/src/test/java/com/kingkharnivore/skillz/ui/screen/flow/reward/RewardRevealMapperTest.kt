package com.kingkharnivore.skillz.ui.screen.flow.reward

import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.model.state.flow.ArcShellRewardCountUiModel
import com.kingkharnivore.skillz.model.state.flow.ArcShellRewardSummaryUiModel
import com.kingkharnivore.skillz.model.state.flow.ArcSummaryUiModel
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel
import com.kingkharnivore.skillz.model.state.flow.RewardRevealAnimationStyle
import com.kingkharnivore.skillz.model.state.flow.RewardRevealCardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardRevealMapperTest {
    private val text = FakeRewardRevealTextProvider()

    @Test
    fun regularFlowUnifiesScyraPointsAndPearls() {
        val cards = buildSessionRewardCards(
            reward = reward(finalScyraPoints = 482, shellPearlsEarned = 482),
            isAera = false,
            calmMode = false,
            text = text,
            findTitle = ::findTitle,
            badgeTitle = ::badgeTitle,
            discoveryTitle = ::discoveryTitle
        )

        assertEquals("482 Scyra Points", cards.first().title)
        assertEquals("Carried into The Shell as Pearls.", cards.first().subtitle)
        assertEquals("Shape The Shell with Pearls.", cards.first { it.type == RewardRevealCardType.SHELL_BRIDGE }.destinationHint)
        assertFalse(cards.drop(1).any { it.amountText?.contains("482") == true && it.title.contains("Pearl") })
    }

    @Test
    fun nonAnimalFallbackCardsDoNotUseTheBlueDestination() {
        val shellBridgeCards = buildSessionRewardCards(
            reward = reward(shellPearlsEarned = 482),
            isAera = false,
            calmMode = false,
            text = text,
            findTitle = ::findTitle,
            badgeTitle = ::badgeTitle,
            discoveryTitle = ::discoveryTitle
        )
        val unknownCards = buildSessionRewardCards(
            reward = reward(shellPearlsEarned = 0, shellGrantedFindIds = listOf("missing")),
            isAera = false,
            calmMode = false,
            text = text,
            findTitle = ::findTitle,
            badgeTitle = ::badgeTitle,
            discoveryTitle = ::discoveryTitle
        )

        assertEquals("Shape The Shell with Pearls.", shellBridgeCards.first { it.type == RewardRevealCardType.SHELL_BRIDGE }.destinationHint)
        assertEquals("View inside The Shell.", unknownCards.first { it.type == RewardRevealCardType.EMPTY_SHELL_MEANING }.destinationHint)
        assertFalse((shellBridgeCards + unknownCards).filter { it.type != RewardRevealCardType.ANIMAL }.any { it.destinationHint == "View later in The Blue." })
    }

    @Test
    fun animalCardsMapMinnowAndWhaleDepths() {
        val cards = buildSessionRewardCards(
            reward = reward(shellGrantedFindIds = listOf(ShellContentCatalog.FOCUS_MINNOW, ShellContentCatalog.FOCUS_WHALE)),
            isAera = false,
            calmMode = false,
            text = text,
            findTitle = ::findTitle,
            badgeTitle = ::badgeTitle,
            discoveryTitle = ::discoveryTitle
        )

        val minnow = cards.first { it.id.startsWith("animal-${ShellContentCatalog.FOCUS_MINNOW}") }
        val whale = cards.first { it.id.startsWith("animal-${ShellContentCatalog.FOCUS_WHALE}") }
        assertEquals(RewardRevealCardType.ANIMAL, minnow.type)
        assertEquals("Animal · Reef", minnow.subtitle)
        assertEquals("View later in The Blue.", minnow.destinationHint)
        assertEquals("Animal · Deep Ocean", whale.subtitle)
    }

    @Test
    fun objectCardUsesFoundLanguage() {
        val cards = buildSessionRewardCards(
            reward = reward(shellGrantedFindIds = listOf(ShellContentCatalog.FOCUS_PEBBLE)),
            isAera = false,
            calmMode = false,
            text = text,
            findTitle = ::findTitle,
            badgeTitle = ::badgeTitle,
            discoveryTitle = ::discoveryTitle
        )

        val objectCard = cards.first { it.type == RewardRevealCardType.OBJECT }
        assertEquals("Pebble found", objectCard.title)
        assertEquals("Object", objectCard.subtitle)
        assertEquals("Resting in the Shell Chest.", objectCard.destinationHint)
    }

    @Test
    fun softFlowEmitsStillwaterWithoutScoreOrPearls() {
        val cards = buildSoftRewardCards(reward(minutes = 22, shellStillwaterUnits = 220), text)

        assertEquals(RewardRevealCardType.STILLWATER_RESULT, cards[0].type)
        assertEquals("Stillwater added", cards[0].title)
        assertEquals("22 quiet minutes carried into the stream.", cards[0].subtitle)
        assertTrue(cards.any { it.type == RewardRevealCardType.SOFT_RULE })
        assertEquals("View in Stillwater Room.", cards[0].destinationHint)
        assertEquals("View in Stillwater Room.", cards[2].destinationHint)
        assertFalse(cards.any { it.destinationHint == "View later in The Blue." })
        assertFalse(cards.any { it.title.contains("Scyra") || it.title.contains("Pearl") })
    }

    @Test
    fun duplicateTrinketsAndBadgesAreGrouped() {
        val cards = buildSessionRewardCards(
            reward = reward(
                shellGrantedFindIds = listOf(
                    ShellContentCatalog.TRINKET_SEA_GLASS_SHARD,
                    ShellContentCatalog.TRINKET_SEA_GLASS_SHARD,
                    ShellContentCatalog.TRINKET_GLIMMER
                ),
                shellBadgeIds = listOf("badge_flow_10_min", "badge_flow_30_min")
            ),
            isAera = false,
            calmMode = false,
            text = text,
            findTitle = ::findTitle,
            badgeTitle = ::badgeTitle,
            discoveryTitle = ::discoveryTitle
        )

        assertTrue(cards.any { it.type == RewardRevealCardType.TRINKET && it.body?.contains("Seaglass ×2") == true })
        assertTrue(cards.any { it.type == RewardRevealCardType.BADGE && it.title == "Badges updated" })
    }

    @Test
    fun unknownIdsDoNotCrash() {
        val cards = buildSessionRewardCards(
            reward = reward(shellGrantedFindIds = listOf("missing"), shellDiscoveryIds = listOf("missing"), shellBadgeIds = listOf("missing")),
            isAera = false,
            calmMode = false,
            text = text,
            findTitle = ::findTitle,
            badgeTitle = ::badgeTitle,
            discoveryTitle = ::discoveryTitle
        )

        assertTrue(cards.size >= 2)
        assertTrue(cards.any { it.title.contains("Shell reward recorded") || it.body?.contains("Recorded inside The Shell.") == true })
        assertTrue(cards.filter { it.type != RewardRevealCardType.ANIMAL }.all { it.destinationHint != "View later in The Blue." })
    }

    @Test
    fun arcSummaryUsesDeckWithUserFriendlyPlaceholder() {
        val cards = buildArcSummaryRewardCards(
            arc = ArcSummaryUiModel(
                totalSessions = 3,
                totalDurationMs = 5_100_000L,
                totalFinalPoints = 1240,
                totalArcBonusPoints = 220,
                peakMultiplier = 1.4
            ),
            isAera = false,
            calmMode = false,
            text = text,
            durationText = "1h 25m"
        )

        assertEquals(RewardRevealCardType.ARC_SCORE, cards.first().type)
        assertTrue(cards.first().body.orEmpty().contains("1240 Scyra Points"))
        assertTrue(cards.first().body.orEmpty().contains("Carried into The Shell as Pearls."))
        assertEquals(RewardRevealCardType.ARC_STORY_PLACEHOLDER, cards[1].type)
    }


    @Test
    fun arcSummaryGroupsAnimalsAndKeepsAnimalDestinationAtTheBlue() {
        val cards = buildArcSummaryRewardCards(
            arc = arcSummary(
                shellSummary = ArcShellRewardSummaryUiModel(
                    animals = listOf(
                        ArcShellRewardCountUiModel(ShellContentCatalog.FOCUS_MINNOW, 2),
                        ArcShellRewardCountUiModel(ShellContentCatalog.FOCUS_SEAHORSE, 1)
                    )
                )
            ),
            isAera = false,
            calmMode = false,
            text = text,
            durationText = "1h 25m",
            findTitle = ::findTitle,
            badgeTitle = ::badgeTitle,
            discoveryTitle = ::discoveryTitle
        )

        val animalCard = cards.first { it.type == RewardRevealCardType.ARC_ANIMALS }
        assertEquals("View later in The Blue.", animalCard.destinationHint)
        assertTrue(animalCard.body.orEmpty().contains("Minnow ×2"))
        assertTrue(animalCard.body.orEmpty().contains("Seahorse ×1"))
    }

    @Test
    fun arcSummaryUsesCorrectDestinationsForNonAnimalAggregates() {
        val cards = buildArcSummaryRewardCards(
            arc = arcSummary(
                shellSummary = ArcShellRewardSummaryUiModel(
                    objects = listOf(ArcShellRewardCountUiModel(ShellContentCatalog.FOCUS_PEBBLE, 1)),
                    trinkets = listOf(
                        ArcShellRewardCountUiModel(ShellContentCatalog.TRINKET_SEA_GLASS_SHARD, 2),
                        ArcShellRewardCountUiModel(ShellContentCatalog.TRINKET_GLIMMER, 1)
                    ),
                    badges = listOf(ArcShellRewardCountUiModel("badge_flow_10_min", 3)),
                    discoveries = listOf(ArcShellRewardCountUiModel("discovery_octopus", 1)),
                    stillwaterAdded = 42L
                )
            ),
            isAera = false,
            calmMode = false,
            text = text,
            durationText = "1h 25m",
            findTitle = ::findTitle,
            badgeTitle = ::badgeTitle,
            discoveryTitle = ::discoveryTitle
        )

        assertEquals("Resting in the Shell Chest.", cards.first { it.type == RewardRevealCardType.ARC_OBJECTS }.destinationHint)
        assertEquals("Resting in the Shell Chest.", cards.first { it.type == RewardRevealCardType.ARC_TRINKETS }.destinationHint)
        assertEquals("Recorded in Badges.", cards.first { it.type == RewardRevealCardType.ARC_BADGES }.destinationHint)
        assertEquals("Recorded in the Discovery Journal.", cards.first { it.type == RewardRevealCardType.ARC_DISCOVERIES }.destinationHint)
        assertEquals("View in Stillwater Room.", cards.first { it.type == RewardRevealCardType.ARC_STILLWATER }.destinationHint)
        assertTrue(cards.filter { it.type != RewardRevealCardType.ARC_ANIMALS }.all { it.destinationHint != "View later in The Blue." })
        assertTrue(cards.first { it.type == RewardRevealCardType.ARC_BADGES }.body.orEmpty().contains("10-minute Flow ×3"))
        assertTrue(cards.first { it.type == RewardRevealCardType.ARC_TRINKETS }.body.orEmpty().contains("Seaglass ×2"))
        assertTrue(cards.first { it.type == RewardRevealCardType.ARC_DISCOVERIES }.body.orEmpty().contains("Octopus ×1"))
    }

    @Test
    fun arcSummaryWithOnlyPearlsShowsShellShapedCardWithoutPearlDuplicate() {
        val cards = buildArcSummaryRewardCards(
            arc = arcSummary(shellSummary = ArcShellRewardSummaryUiModel(pearlsCarried = 482)),
            isAera = false,
            calmMode = false,
            text = text,
            durationText = "30m",
            findTitle = ::findTitle,
            badgeTitle = ::badgeTitle,
            discoveryTitle = ::discoveryTitle
        )

        assertEquals(2, cards.size)
        assertEquals(RewardRevealCardType.SHELL_BRIDGE, cards[1].type)
        assertEquals("Shape The Shell with Pearls.", cards[1].destinationHint)
        assertEquals(1, cards.count { it.animationStyle == RewardRevealAnimationStyle.PEARL_GLOW && it.type != RewardRevealCardType.ARC_SCORE })
    }

    @Test
    fun arcSummaryUnknownRewardUsesShellFallbackWithoutCrashing() {
        val cards = buildArcSummaryRewardCards(
            arc = arcSummary(shellSummary = ArcShellRewardSummaryUiModel(unknownRewards = listOf(ArcShellRewardCountUiModel("missing", 1)))),
            isAera = false,
            calmMode = false,
            text = text,
            durationText = "30m"
        )

        assertTrue(cards.any { it.title == "Shell reward recorded" && it.destinationHint == "View inside The Shell." })
    }

    private fun reward(
        minutes: Int = 30,
        finalScyraPoints: Int = 482,
        shellPearlsEarned: Int = 482,
        shellStillwaterUnits: Long = 0L,
        shellGrantedFindIds: List<String> = emptyList(),
        shellDiscoveryIds: List<String> = emptyList(),
        shellBadgeIds: List<String> = emptyList()
    ) = FlowRewardUiModel(
        minutes = minutes,
        baseScyraPoints = 300,
        tenMinuteBonuses = 1,
        thirtyMinuteBonuses = 1,
        sixtyMinuteBonuses = 0,
        finalScyraPoints = finalScyraPoints,
        surgePoints = 40,
        arcBonusPoints = 67,
        shellPearlsEarned = shellPearlsEarned,
        shellStillwaterUnits = shellStillwaterUnits,
        shellGrantedFindIds = shellGrantedFindIds,
        shellDiscoveryIds = shellDiscoveryIds,
        shellBadgeIds = shellBadgeIds
    )

    private fun arcSummary(shellSummary: ArcShellRewardSummaryUiModel = ArcShellRewardSummaryUiModel()) = ArcSummaryUiModel(
        totalSessions = 3,
        totalDurationMs = 5_100_000L,
        totalFinalPoints = 1240,
        totalArcBonusPoints = 220,
        peakMultiplier = 1.4,
        shellSummary = shellSummary
    )

    private fun findTitle(id: String): String? = when (id) {
        ShellContentCatalog.FOCUS_MINNOW -> "Minnow"
        ShellContentCatalog.FOCUS_SEAHORSE -> "Seahorse"
        ShellContentCatalog.FOCUS_WHALE -> "Whale"
        ShellContentCatalog.FOCUS_PEBBLE -> "Pebble"
        ShellContentCatalog.TRINKET_SEA_GLASS_SHARD -> "Seaglass"
        ShellContentCatalog.TRINKET_GLIMMER -> "Glimmers"
        else -> null
    }

    private fun badgeTitle(id: String): String? = when (id) {
        "badge_flow_10_min" -> "10-minute Flow"
        "badge_flow_30_min" -> "30-minute Flow"
        else -> null
    }

    private fun discoveryTitle(id: String): String? = when (id) {
        "discovery_octopus" -> "Octopus"
        else -> null
    }
}

private class FakeRewardRevealTextProvider : RewardRevealTextProvider {
    override fun scyraPoints(points: Int) = "$points Scyra Points"
    override fun pointsDelta(points: Int) = "+$points"
    override fun minutes(minutes: Int) = "$minutes min"
    override fun quietMinutes(minutes: Int) = "$minutes quiet minutes carried into the stream."
    override fun carriedAsPearls() = "Carried into The Shell as Pearls."
    override fun scoreBuiltFrom() = "Built from:"
    override fun baseFlow(points: Int) = "Base Flow $points"
    override fun timeBonuses(points: Int) = "Time bonuses +$points"
    override fun surge(points: Int) = "Surge +$points"
    override fun arcBonus(points: Int) = "Arc bonus +$points"
    override fun arcMultiplier(multiplier: Double) = "Arc multiplier: ${multiplier}×"
    override fun swipeFlowHint() = "Swipe to see what Scyra brought back."
    override fun swipeArcHint() = "Swipe to see what this Arc brought back."
    override fun loggedStory() = "Logged into your story."
    override fun timeLoggedTitle() = "Time logged"
    override fun shellWasShapedTitle() = "The Shell was shaped"
    override fun shellWasShapedBody() = "Your Scyra Points were carried into The Shell as Pearls."
    override fun animalTitle(name: String) = "$name encountered"
    override fun objectTitle(name: String) = "$name found"
    override fun trinketTitle(name: String) = "$name gathered"
    override fun badgeTitle(name: String) = "$name badge updated"
    override fun discoveryTitle(name: String) = "$name discovered"
    override fun animalChip(depth: String) = "Animal · $depth"
    override fun objectChip() = "Object"
    override fun trinketChip() = "Trinket"
    override fun discoveryChip() = "Discovery"
    override fun badgeChip() = "Badge"
    override fun reef() = "Reef"
    override fun deeperReef() = "Deeper Reef"
    override fun openBlue() = "Open Blue"
    override fun deepOcean() = "Deep Ocean"
    override fun animalReason(findId: String) = "From a regular Flow lasting 10 minutes or more."
    override fun objectReason(findId: String) = "Found after you returned to regular Flow."
    override fun trinketReason() = "Found through regular Flow activity."
    override fun discoveryReason(discoveryId: String) = "Discovered after 3 regular Flows lasting 30 minutes or more."
    override fun badgeReason(badgeId: String) = "Earned each time a regular Flow lasts 30 minutes or more."
    override fun theBlueHint() = "View later in The Blue."
    override fun stillwaterHint() = "View in Stillwater Room."
    override fun shellHint() = "View inside The Shell."
    override fun pearlBasinHint() = "Shape The Shell with Pearls."
    override fun shellChestHint() = "Resting in the Shell Chest."
    override fun discoveryJournalHint() = "Recorded in the Discovery Journal."
    override fun badgesHint() = "Recorded in Badges."
    override fun shellRewardRecordedTitle() = "Shell reward recorded"
    override fun shellRewardRecordedBody() = "Recorded inside The Shell."
    override fun stillwaterAddedTitle() = "Stillwater added"
    override fun softRuleTitle() = "Soft Flow stayed soft"
    override fun softRuleBody() = "No score. No Pearls. Only Stillwater was added."
    override fun stillwaterPerspectiveTitle() = "Same Stillwater, different view"
    override fun stillwaterPerspectiveBody() = "Inside The Shell, you can view Stillwater as cups, bowls, streams, lakes, or oceans."
    override fun arcCompleteTitle() = "Arc complete"
    override fun arcFlows(count: Int) = "$count Flows"
    override fun totalDuration(duration: String) = "$duration total"
    override fun peakMultiplier(multiplier: Double) = "Peak multiplier: ${multiplier}×"
    override fun arcBonusLine(points: Int) = "Arc bonus: +$points"
    override fun arcStoryPlaceholderTitle() = "This Arc became part of your story"
    override fun arcStoryPlaceholderBody() = "Voyage Hall will gather Arc journeys in a future Shell update."
    override fun groupedTrinketsTitle() = "Trinkets gathered"
    override fun groupedBadgesTitle() = "Badges updated"
    override fun itemCount(name: String, count: Int) = "$name ×$count"
    override fun recordsUpdatedFromFlow() = "Records updated from this Flow."
    override fun flowMilestonesAcrossArc() = "From Flow milestones across this Arc."
    override fun recordsUpdatedAcrossArc() = "Records updated across this Arc."
    override fun arcAnimalsTitle() = "Animals encountered"
    override fun arcObjectsTitle() = "Objects found"
    override fun arcTrinketsTitle() = "Trinkets gathered"
    override fun arcBadgesTitle() = "Badges updated"
    override fun arcDiscoveriesTitle() = "Discoveries recorded"
    override fun foundAcrossArc() = "Found across this Arc."
    override fun recordedInJournal() = "Recorded in the Discovery Journal."
    override fun arcShellShapedBody() = "Scyra Points from this Arc were carried into The Shell as Pearls."
}
