package com.kingkharnivore.skillz.ui.screen.flow.reward

import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellDepthTier
import com.kingkharnivore.skillz.data.model.shell.ShellRewardKind
import com.kingkharnivore.skillz.model.state.flow.ArcShellRewardCountUiModel
import com.kingkharnivore.skillz.model.state.flow.ArcSummaryUiModel
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel
import com.kingkharnivore.skillz.model.state.flow.RewardRevealAnimationStyle
import com.kingkharnivore.skillz.model.state.flow.RewardRevealCardType
import com.kingkharnivore.skillz.model.state.flow.RewardRevealCardUiModel

interface RewardRevealTextProvider {
    fun scyraPoints(points: Int): String
    fun pointsDelta(points: Int): String
    fun minutes(minutes: Int): String
    fun quietMinutes(minutes: Int): String
    fun carriedAsPearls(): String
    fun scoreBuiltFrom(): String
    fun baseFlow(points: Int): String
    fun timeBonuses(points: Int): String
    fun surge(points: Int): String
    fun arcBonus(points: Int): String
    fun arcMultiplier(multiplier: Double): String
    fun swipeFlowHint(): String
    fun swipeArcHint(): String
    fun loggedStory(): String
    fun timeLoggedTitle(): String
    fun shellWasShapedTitle(): String
    fun shellWasShapedBody(): String
    fun animalTitle(name: String): String
    fun badgeTitle(name: String): String
    fun animalChip(depth: String): String
    fun badgeChip(): String
    fun reef(): String
    fun deeperReef(): String
    fun openBlue(): String
    fun deepOcean(): String
    fun animalReason(findId: String): String
    fun badgeReason(badgeId: String): String
    fun theBlueHint(): String
    fun stillwaterHint(): String
    fun shellHint(): String
    fun pearlBasinHint(): String
    fun shellChestHint(): String
    fun badgesHint(): String
    fun shellRewardRecordedTitle(): String
    fun shellRewardRecordedBody(): String
    fun stillwaterAddedTitle(): String
    fun softDropsGained(drops: Long): String
    fun softAddedToStillwater(): String
    fun softExplainer(): String
    fun arcCompleteTitle(): String
    fun arcFlows(count: Int): String
    fun totalDuration(duration: String): String
    fun peakMultiplier(multiplier: Double): String
    fun arcBonusLine(points: Int): String
    fun arcStoryPlaceholderTitle(): String
    fun arcStoryPlaceholderBody(): String
    fun groupedBadgesTitle(): String
    fun itemCount(name: String, count: Int): String
    fun recordsUpdatedFromFlow(): String
    fun flowMilestonesAcrossArc(): String
    fun recordsUpdatedAcrossArc(): String
    fun arcAnimalsTitle(): String
    fun arcBadgesTitle(): String
    fun arcShellShapedBody(): String
}

fun buildSessionRewardCards(
    reward: FlowRewardUiModel,
    isAera: Boolean,
    calmMode: Boolean,
    text: RewardRevealTextProvider,
    findTitle: (String) -> String?,
    badgeTitle: (String) -> String?,
    discoveryTitle: (String) -> String?
): List<RewardRevealCardUiModel> {
    val cards = mutableListOf<RewardRevealCardUiModel>()
    val timeFirst = calmMode || isAera
    val scoreBody = buildList {
        if (timeFirst) {
            add(text.minutes(reward.minutes))
            add(text.loggedStory())
        } else {
            add(text.scoreBuiltFrom())
            add(text.baseFlow(reward.baseScyraPoints))
            val totalTimeBonuses = reward.tenMinuteBonuses * 5 + reward.thirtyMinuteBonuses * 15 + reward.sixtyMinuteBonuses * 50
            if (totalTimeBonuses > 0) add(text.timeBonuses(totalTimeBonuses))
            if (reward.surgePoints > 0) add(text.surge(reward.surgePoints))
            if (reward.arcBonusPoints > 0) add(text.arcBonus(reward.arcBonusPoints))
            reward.arcMultiplierUsed?.let { add(text.arcMultiplier(it)) }
            add(text.swipeFlowHint())
        }
    }.joinToString("\n")
    val scoreTitle = if (timeFirst) text.timeLoggedTitle() else text.scyraPoints(reward.finalScyraPoints)
    val scoreSubtitle = if (!timeFirst && reward.shellPearlsEarned > 0) text.carriedAsPearls() else text.loggedStory()
    cards += RewardRevealCardUiModel(
        id = "session-score",
        type = RewardRevealCardType.SCORE_BREAKDOWN,
        title = scoreTitle,
        subtitle = scoreSubtitle,
        body = scoreBody,
        amountText = if (timeFirst) text.minutes(reward.minutes) else text.scyraPoints(reward.finalScyraPoints),
        iconKey = "score",
        contentDescription = listOf(scoreTitle, scoreSubtitle, scoreBody).joinToString(". "),
        animationStyle = if (reward.shellPearlsEarned > 0) RewardRevealAnimationStyle.PEARL_GLOW else RewardRevealAnimationStyle.NONE
    )

    cards += buildShellRewardCards(
        reward = reward,
        text = text,
        findTitle = findTitle,
        badgeTitle = badgeTitle,
        discoveryTitle = discoveryTitle
    )

    if (cards.size == 1) {
        cards += RewardRevealCardUiModel(
            id = "session-shell-quiet",
            type = RewardRevealCardType.EMPTY_SHELL_MEANING,
            title = text.shellWasShapedTitle(),
            body = if (reward.shellPearlsEarned > 0) text.shellWasShapedBody() else text.loggedStory(),
            iconKey = "shell",
            destinationHint = if (reward.shellPearlsEarned > 0) text.pearlBasinHint() else text.shellHint(),
            contentDescription = listOf(
                text.shellWasShapedTitle(),
                if (reward.shellPearlsEarned > 0) text.shellWasShapedBody() else text.loggedStory(),
                if (reward.shellPearlsEarned > 0) text.pearlBasinHint() else text.shellHint()
            ).joinToString(". "),
            animationStyle = RewardRevealAnimationStyle.PEARL_GLOW
        )
    }

    return cards
}

fun buildSoftRewardCards(
    reward: FlowRewardUiModel,
    text: RewardRevealTextProvider
): List<RewardRevealCardUiModel> {
    val dropsText = text.softDropsGained(reward.shellStillwaterUnits)
    return listOf(
        RewardRevealCardUiModel(
            id = "soft-stillwater",
            type = RewardRevealCardType.STILLWATER_RESULT,
            title = text.stillwaterAddedTitle(),
            subtitle = text.softAddedToStillwater(),
            body = text.softExplainer(),
            amountText = dropsText,
            iconKey = "stillwater",
            destinationHint = text.stillwaterHint(),
            contentDescription = listOf(text.stillwaterAddedTitle(), dropsText, text.softAddedToStillwater(), text.softExplainer()).joinToString(". "),
            animationStyle = RewardRevealAnimationStyle.STILLWATER_RIPPLE
        )
    )
}

fun buildArcSummaryRewardCards(
    arc: ArcSummaryUiModel,
    isAera: Boolean,
    calmMode: Boolean,
    text: RewardRevealTextProvider,
    durationText: String,
    findTitle: (String) -> String? = { null },
    badgeTitle: (String) -> String? = { null },
    discoveryTitle: (String) -> String? = { null }
): List<RewardRevealCardUiModel> {
    val showScore = !isAera && !calmMode
    val body = buildList {
        add(text.arcFlows(arc.totalSessions))
        add(text.totalDuration(durationText))
        if (showScore) {
            add(text.scyraPoints(arc.totalFinalPoints))
            add(text.carriedAsPearls())
            add(text.peakMultiplier(arc.peakMultiplier))
            if (arc.totalArcBonusPoints > 0) add(text.arcBonusLine(arc.totalArcBonusPoints))
            add(text.swipeArcHint())
        }
    }.joinToString("\n")
    val cards = mutableListOf(
        RewardRevealCardUiModel(
            id = "arc-score",
            type = RewardRevealCardType.ARC_SCORE,
            title = text.arcCompleteTitle(),
            subtitle = if (showScore) text.scyraPoints(arc.totalFinalPoints) else text.totalDuration(durationText),
            body = body,
            amountText = if (showScore) text.scyraPoints(arc.totalFinalPoints) else text.totalDuration(durationText),
            iconKey = "arc",
            contentDescription = listOf(text.arcCompleteTitle(), body).joinToString(". "),
            animationStyle = if (showScore) RewardRevealAnimationStyle.PEARL_GLOW else RewardRevealAnimationStyle.NONE
        )
    )

    val shell = arc.shellSummary
    cards += aggregateCountCard("arc-animals", RewardRevealCardType.ARC_ANIMALS, text.arcAnimalsTitle(), shell.animals, text.flowMilestonesAcrossArc(), text.theBlueHint(), "animal", text, findTitle)
    cards += aggregateCountCard("arc-badges", RewardRevealCardType.ARC_BADGES, text.arcBadgesTitle(), shell.badges, text.recordsUpdatedAcrossArc(), text.badgesHint(), "badge", text, badgeTitle)

    if (shell.stillwaterAdded > 0L) {
        val bodyText = text.quietMinutes(shell.stillwaterAdded.toInt())
        cards += RewardRevealCardUiModel(
            id = "arc-stillwater",
            type = RewardRevealCardType.ARC_STILLWATER,
            title = text.stillwaterAddedTitle(),
            body = bodyText,
            iconKey = "stillwater",
            destinationHint = text.stillwaterHint(),
            contentDescription = listOf(text.stillwaterAddedTitle(), bodyText, text.stillwaterHint()).joinToString(". "),
            animationStyle = RewardRevealAnimationStyle.STILLWATER_RIPPLE
        )
    }

    cards += shell.unknownRewards.map { unknownCard("arc-unknown-${it.id}", text) }

    if (cards.size == 1) {
        if (shell.pearlsCarried > 0) {
            cards += RewardRevealCardUiModel(
                id = "arc-shell-shaped",
                type = RewardRevealCardType.SHELL_BRIDGE,
                title = text.shellWasShapedTitle(),
                body = text.arcShellShapedBody(),
                iconKey = "pearl",
                destinationHint = text.pearlBasinHint(),
                contentDescription = listOf(text.shellWasShapedTitle(), text.arcShellShapedBody(), text.pearlBasinHint()).joinToString(". "),
                animationStyle = RewardRevealAnimationStyle.PEARL_GLOW
            )
        } else {
            cards += RewardRevealCardUiModel(
                id = "arc-shell-placeholder",
                type = RewardRevealCardType.ARC_STORY_PLACEHOLDER,
                title = text.arcStoryPlaceholderTitle(),
                body = text.arcStoryPlaceholderBody(),
                iconKey = "voyage",
                contentDescription = listOf(text.arcStoryPlaceholderTitle(), text.arcStoryPlaceholderBody()).joinToString(". ")
            )
        }
    }

    return cards
}


private fun aggregateCountCard(
    id: String,
    type: RewardRevealCardType,
    title: String,
    rewards: List<ArcShellRewardCountUiModel>,
    footer: String,
    destination: String,
    iconKey: String,
    text: RewardRevealTextProvider,
    titleForId: (String) -> String?
): List<RewardRevealCardUiModel> {
    if (rewards.isEmpty()) return emptyList()
    val lines = rewards.map { reward ->
        val name = titleForId(reward.id) ?: text.shellRewardRecordedTitle()
        text.itemCount(name, reward.count)
    }
    val body = (lines + footer).joinToString("\n")
    return listOf(
        RewardRevealCardUiModel(
            id = id,
            type = type,
            title = title,
            body = body,
            iconKey = iconKey,
            destinationHint = destination,
            contentDescription = listOf(title, body, destination).joinToString(". ")
        )
    )
}

private fun buildShellRewardCards(
    reward: FlowRewardUiModel,
    text: RewardRevealTextProvider,
    findTitle: (String) -> String?,
    badgeTitle: (String) -> String?,
    discoveryTitle: (String) -> String?
): List<RewardRevealCardUiModel> {
    val cards = mutableListOf<RewardRevealCardUiModel>()
    val findCounts = reward.shellGrantedFindIds.groupingBy { it }.eachCount()
    findCounts.filterKeys { ShellContentCatalog.find(it)?.kind == ShellRewardKind.ANIMAL }.forEach { (findId, count) ->
        val def = ShellContentCatalog.find(findId)
        val name = findTitle(findId) ?: text.shellRewardRecordedTitle()
        when (def?.kind) {
            ShellRewardKind.ANIMAL -> {
                val depth = depthText(def.depthTier, text)
                val title = text.animalTitle(name)
                val body = text.animalReason(findId)
                cards += RewardRevealCardUiModel(
                    id = "animal-$findId-$count",
                    type = RewardRevealCardType.ANIMAL,
                    title = if (count > 1) text.itemCount(title, count) else title,
                    subtitle = text.animalChip(depth),
                    body = body,
                    chip = depth,
                    iconKey = def.iconKey,
                    destinationHint = text.theBlueHint(),
                    contentDescription = listOf(title, text.animalChip(depth), body, text.theBlueHint()).joinToString(". "),
                    animationStyle = animalAnimation(findId)
                )
            }
            else -> cards += unknownCard("find-$findId", text)
        }
    }

    // Legacy non-creature rewards are no longer user-facing Shell rewards. Existing inert data is intentionally hidden.


    // Legacy discovery rewards are no longer user-facing.


    val badgeCounts = reward.shellBadgeIds.groupingBy { it }.eachCount()
    if (badgeCounts.size > 1) {
        val lines = badgeCounts.map { (badgeId, count) -> text.itemCount(badgeTitle(badgeId) ?: text.shellRewardRecordedTitle(), count) }
        cards += RewardRevealCardUiModel(
            id = "badges",
            type = RewardRevealCardType.BADGE,
            title = text.groupedBadgesTitle(),
            subtitle = text.badgeChip(),
            body = lines.joinToString("\n") + "\n" + text.recordsUpdatedFromFlow(),
            chip = text.badgeChip(),
            destinationHint = text.badgesHint(),
            contentDescription = (listOf(text.groupedBadgesTitle()) + lines + listOf(text.recordsUpdatedFromFlow(), text.badgesHint())).joinToString(". "),
            animationStyle = RewardRevealAnimationStyle.BADGE_STAMP
        )
    } else {
        badgeCounts.forEach { (badgeId, count) ->
            val name = badgeTitle(badgeId) ?: text.shellRewardRecordedTitle()
            val title = text.badgeTitle(name)
            val body = text.badgeReason(badgeId)
            cards += RewardRevealCardUiModel(
                id = "badge-$badgeId-$count",
                type = RewardRevealCardType.BADGE,
                title = if (count > 1) text.itemCount(title, count) else title,
                subtitle = text.badgeChip(),
                body = body,
                chip = text.badgeChip(),
                destinationHint = text.badgesHint(),
                contentDescription = listOf(title, text.badgeChip(), body, text.badgesHint()).joinToString(". "),
                animationStyle = RewardRevealAnimationStyle.BADGE_STAMP
            )
        }
    }

    if (reward.shellStillwaterUnits > 0L) {
        val title = text.stillwaterAddedTitle()
        val body = text.quietMinutes(reward.minutes)
        cards += RewardRevealCardUiModel(
            id = "stillwater-${reward.shellStillwaterUnits}",
            type = RewardRevealCardType.STILLWATER_RESULT,
            title = title,
            subtitle = body,
            iconKey = "stillwater",
            destinationHint = text.stillwaterHint(),
            contentDescription = listOf(title, body, text.stillwaterHint()).joinToString(". "),
            animationStyle = RewardRevealAnimationStyle.STILLWATER_RIPPLE
        )
    }

    if (cards.isEmpty() && reward.shellPearlsEarned > 0) {
        cards += RewardRevealCardUiModel(
            id = "shell-bridge",
            type = RewardRevealCardType.SHELL_BRIDGE,
            title = text.shellWasShapedTitle(),
            body = text.shellWasShapedBody(),
            iconKey = "pearl",
            destinationHint = text.pearlBasinHint(),
            contentDescription = listOf(text.shellWasShapedTitle(), text.shellWasShapedBody(), text.pearlBasinHint()).joinToString(". "),
            animationStyle = RewardRevealAnimationStyle.PEARL_GLOW
        )
    }
    return cards
}

private fun unknownCard(id: String, text: RewardRevealTextProvider) = RewardRevealCardUiModel(
    id = id,
    type = RewardRevealCardType.EMPTY_SHELL_MEANING,
    title = text.shellRewardRecordedTitle(),
    body = text.shellRewardRecordedBody(),
    destinationHint = text.shellHint(),
    contentDescription = listOf(text.shellRewardRecordedTitle(), text.shellRewardRecordedBody(), text.shellHint()).joinToString(". ")
)

private fun depthText(depthTier: ShellDepthTier?, text: RewardRevealTextProvider): String = when (depthTier) {
    ShellDepthTier.REEF -> text.reef()
    ShellDepthTier.DEEPER_REEF -> text.deeperReef()
    ShellDepthTier.OPEN_BLUE -> text.openBlue()
    ShellDepthTier.DEEP_OCEAN -> text.deepOcean()
    null -> text.reef()
}

private fun animalAnimation(findId: String): RewardRevealAnimationStyle = when (findId) {
    ShellContentCatalog.FOCUS_MINNOW -> RewardRevealAnimationStyle.ANIMAL_SWIM
    ShellContentCatalog.FOCUS_SEAHORSE -> RewardRevealAnimationStyle.ANIMAL_BOB
    ShellContentCatalog.FOCUS_MANTA -> RewardRevealAnimationStyle.ANIMAL_GLIDE
    ShellContentCatalog.FOCUS_WHALE -> RewardRevealAnimationStyle.ANIMAL_SHADOW
    else -> RewardRevealAnimationStyle.DISCOVERY_REVEAL
}
