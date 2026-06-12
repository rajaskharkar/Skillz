package com.kingkharnivore.skillz.ui.screen.flow.reward

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog

@Composable
fun rememberRewardRevealTextProvider(): RewardRevealTextProvider {
    val scyraPointsTemplate = stringResource(R.string.reward_card_scyra_points_template)
    val pointsDeltaTemplate = stringResource(R.string.session_reward_points_value)
    val minutesTemplate = stringResource(R.string.session_reward_minutes_value)
    val quietMinutesTemplate = stringResource(R.string.reward_card_stillwater_quiet_minutes)
    val carriedAsPearls = stringResource(R.string.reward_card_points_carried_as_pearls)
    val scoreBuiltFrom = stringResource(R.string.reward_card_score_built_from)
    val baseFlowTemplate = stringResource(R.string.reward_card_base_flow)
    val timeBonusesTemplate = stringResource(R.string.reward_card_time_bonuses)
    val surgeTemplate = stringResource(R.string.reward_card_surge)
    val arcBonusTemplate = stringResource(R.string.reward_card_arc_bonus)
    val arcMultiplierTemplate = stringResource(R.string.reward_card_arc_multiplier)
    val swipeFlowHint = stringResource(R.string.reward_card_swipe_flow_hint)
    val swipeArcHint = stringResource(R.string.reward_card_swipe_arc_hint)
    val loggedStory = stringResource(R.string.session_reward_subtitle_logged_story)
    val timeLoggedTitle = stringResource(R.string.reward_card_time_logged_title)
    val shellWasShapedTitle = stringResource(R.string.reward_card_shell_shaped_title)
    val shellWasShapedBody = stringResource(R.string.reward_card_shell_shaped_body)
    val animalTitleTemplate = stringResource(R.string.reward_card_animal_encountered_title)
    val badgeTitleTemplate = stringResource(R.string.reward_card_badge_updated_title)
    val animalChipTemplate = stringResource(R.string.reward_card_animal_chip)
    val badgeChip = stringResource(R.string.reward_card_badge_chip)
    val reef = stringResource(R.string.reward_card_depth_reef)
    val deeperReef = stringResource(R.string.reward_card_depth_deeper_reef)
    val openBlue = stringResource(R.string.reward_card_depth_open_blue)
    val deepOcean = stringResource(R.string.reward_card_depth_deep_ocean)
    val animalReasons = mapOf(
        ShellContentCatalog.FOCUS_MINNOW to stringResource(R.string.reward_card_animal_reason_minnow),
        ShellContentCatalog.FOCUS_SEAHORSE to stringResource(R.string.reward_card_animal_reason_seahorse),
        ShellContentCatalog.FOCUS_MANTA to stringResource(R.string.reward_card_animal_reason_manta),
        ShellContentCatalog.FOCUS_WHALE to stringResource(R.string.reward_card_animal_reason_whale),
        ShellContentCatalog.FOCUS_OCTOPUS to stringResource(R.string.reward_card_animal_reason_octopus)
    )
    val badgeReasons = mapOf(
        "badge_flow_10_min" to stringResource(R.string.reward_card_badge_reason_10),
        "badge_flow_30_min" to stringResource(R.string.reward_card_badge_reason_30),
        "badge_flow_60_min" to stringResource(R.string.reward_card_badge_reason_60),
        "badge_flow_120_min" to stringResource(R.string.reward_card_badge_reason_120),
        "badge_discovery" to stringResource(R.string.reward_card_badge_reason_discovery)
    )
    val theBlueHint = stringResource(R.string.reward_card_the_blue_hint)
    val stillwaterHint = stringResource(R.string.reward_card_stillwater_hint)
    val shellHint = stringResource(R.string.reward_card_shell_hint)
    val pearlBasinHint = stringResource(R.string.reward_card_pearl_basin_hint)
    val shellChestHint = stringResource(R.string.reward_card_shell_chest_hint)
    val badgesHint = stringResource(R.string.reward_card_badges_hint)
    val shellRewardRecordedTitle = stringResource(R.string.reward_card_shell_recorded_title)
    val shellRewardRecordedBody = stringResource(R.string.reward_card_shell_recorded_body)
    val stillwaterAddedTitle = stringResource(R.string.reward_card_stillwater_added_title)
    val softDropsGainedTemplate = stringResource(R.string.flow_complete_soft_drops_gained)
    val softAddedToStillwater = stringResource(R.string.flow_complete_soft_added_to_stillwater)
    val softExplainer = stringResource(R.string.flow_complete_soft_explainer)
    val arcCompleteTitle = stringResource(R.string.reward_card_arc_complete_title)
    val arcFlowsTemplate = stringResource(R.string.reward_card_arc_flows)
    val arcTotalDurationTemplate = stringResource(R.string.reward_card_arc_total_duration)
    val arcPeakMultiplierTemplate = stringResource(R.string.reward_card_arc_peak_multiplier)
    val arcBonusLineTemplate = stringResource(R.string.reward_card_arc_bonus_line)
    val arcStoryPlaceholderTitle = stringResource(R.string.reward_card_arc_placeholder_title)
    val arcStoryPlaceholderBody = stringResource(R.string.reward_card_arc_placeholder_body)
    val groupedBadgesTitle = stringResource(R.string.reward_card_badges_grouped_title)
    val itemCountTemplate = stringResource(R.string.reward_card_item_count)
    val recordsUpdatedFromFlow = stringResource(R.string.reward_card_records_updated_from_flow)
    val flowMilestonesAcrossArc = stringResource(R.string.reward_card_flow_milestones_across_arc)
    val recordsUpdatedAcrossArc = stringResource(R.string.reward_card_records_updated_across_arc)
    val arcAnimalsTitle = stringResource(R.string.reward_card_arc_animals_title)
    val arcBadgesTitle = stringResource(R.string.reward_card_arc_badges_title)
    val arcShellShapedBody = stringResource(R.string.reward_card_arc_shell_shaped_body)

        return object : RewardRevealTextProvider {
        override fun scyraPoints(points: Int) = scyraPointsTemplate.format(points)
        override fun pointsDelta(points: Int) = pointsDeltaTemplate.format(points)
        override fun minutes(minutes: Int) = minutesTemplate.format(minutes)
        override fun quietMinutes(minutes: Int) = quietMinutesTemplate.format(minutes)
        override fun carriedAsPearls() = carriedAsPearls
        override fun scoreBuiltFrom() = scoreBuiltFrom
        override fun baseFlow(points: Int) = baseFlowTemplate.format(points)
        override fun timeBonuses(points: Int) = timeBonusesTemplate.format(points)
        override fun surge(points: Int) = surgeTemplate.format(points)
        override fun arcBonus(points: Int) = arcBonusTemplate.format(points)
        override fun arcMultiplier(multiplier: Double) = arcMultiplierTemplate.format(multiplier)
        override fun swipeFlowHint() = swipeFlowHint
        override fun swipeArcHint() = swipeArcHint
        override fun loggedStory() = loggedStory
        override fun timeLoggedTitle() = timeLoggedTitle
        override fun shellWasShapedTitle() = shellWasShapedTitle
        override fun shellWasShapedBody() = shellWasShapedBody
        override fun animalTitle(name: String) = animalTitleTemplate.format(name)
        override fun badgeTitle(name: String) = badgeTitleTemplate.format(name)
        override fun animalChip(depth: String) = animalChipTemplate.format(depth)
        override fun badgeChip() = badgeChip
        override fun reef() = reef
        override fun deeperReef() = deeperReef
        override fun openBlue() = openBlue
        override fun deepOcean() = deepOcean
        override fun animalReason(findId: String) = animalReasons[findId] ?: shellRewardRecordedBody
        override fun badgeReason(badgeId: String) = badgeReasons[badgeId] ?: shellRewardRecordedBody
        override fun theBlueHint() = theBlueHint
        override fun stillwaterHint() = stillwaterHint
        override fun shellHint() = shellHint
        override fun pearlBasinHint() = pearlBasinHint
        override fun shellChestHint() = shellChestHint
        override fun badgesHint() = badgesHint
        override fun shellRewardRecordedTitle() = shellRewardRecordedTitle
        override fun shellRewardRecordedBody() = shellRewardRecordedBody
        override fun stillwaterAddedTitle() = stillwaterAddedTitle
        override fun softDropsGained(drops: Long) = softDropsGainedTemplate.format(drops)
        override fun softAddedToStillwater() = softAddedToStillwater
        override fun softExplainer() = softExplainer
        override fun arcCompleteTitle() = arcCompleteTitle
        override fun arcFlows(count: Int) = arcFlowsTemplate.format(count)
        override fun totalDuration(duration: String) = arcTotalDurationTemplate.format(duration)
        override fun peakMultiplier(multiplier: Double) = arcPeakMultiplierTemplate.format(multiplier)
        override fun arcBonusLine(points: Int) = arcBonusLineTemplate.format(points)
        override fun arcStoryPlaceholderTitle() = arcStoryPlaceholderTitle
        override fun arcStoryPlaceholderBody() = arcStoryPlaceholderBody
        override fun groupedBadgesTitle() = groupedBadgesTitle
        override fun itemCount(name: String, count: Int) = itemCountTemplate.format(name, count)
        override fun recordsUpdatedFromFlow() = recordsUpdatedFromFlow
        override fun flowMilestonesAcrossArc() = flowMilestonesAcrossArc
        override fun recordsUpdatedAcrossArc() = recordsUpdatedAcrossArc
        override fun arcAnimalsTitle() = arcAnimalsTitle
        override fun arcBadgesTitle() = arcBadgesTitle
        override fun arcShellShapedBody() = arcShellShapedBody
    }
}
