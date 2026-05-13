package com.kingkharnivore.skillz.ui.navigation

import android.net.Uri

object SkillzDestinations {
    const val SKILLS_LIST = "skills_list"
    const val ADD_SKILL = "add_skill"

    const val ADD_SKILL_ARG_PREFILL_JOURNEY = "prefillJourney"
    const val ADD_SKILL_ARG_PREFILL_TITLE = "prefillTitle"
    const val ADD_SKILL_ARG_PREFILL_SOFT_MODE = "prefillSoftMode"

    const val ADD_SKILL_ARG_PLANNED_ARC_TITLE = "plannedArcTitle"
    const val ADD_SKILL_ARG_PLANNED_ARC_STEP_INDEX = "plannedArcStepIndex"
    const val ADD_SKILL_ARG_PLANNED_ARC_TOTAL_STEPS = "plannedArcTotalSteps"

    const val ADD_SKILL_ROUTE =
        "$ADD_SKILL?" +
                "$ADD_SKILL_ARG_PREFILL_JOURNEY={$ADD_SKILL_ARG_PREFILL_JOURNEY}&" +
                "$ADD_SKILL_ARG_PREFILL_TITLE={$ADD_SKILL_ARG_PREFILL_TITLE}&" +
                "$ADD_SKILL_ARG_PREFILL_SOFT_MODE={$ADD_SKILL_ARG_PREFILL_SOFT_MODE}&" +
                "$ADD_SKILL_ARG_PLANNED_ARC_TITLE={$ADD_SKILL_ARG_PLANNED_ARC_TITLE}&" +
                "$ADD_SKILL_ARG_PLANNED_ARC_STEP_INDEX={$ADD_SKILL_ARG_PLANNED_ARC_STEP_INDEX}&" +
                "$ADD_SKILL_ARG_PLANNED_ARC_TOTAL_STEPS={$ADD_SKILL_ARG_PLANNED_ARC_TOTAL_STEPS}"

    const val HOME_SCREEN = "home_screen"
    const val SHELL = "shell"
    const val ADD_PULSE_ROUTE = "add_pulse"

    const val PLAN_ARC_ROUTE_BASE = "plan_arc"
    const val PLAN_ARC_ARG_EDIT_ID = "editArcPlanId"
    const val PLAN_ARC_ROUTE =
        "$PLAN_ARC_ROUTE_BASE?$PLAN_ARC_ARG_EDIT_ID={$PLAN_ARC_ARG_EDIT_ID}"

    const val SUGGESTED_ROUTE_DETAIL = "suggested_route_detail"
    const val SUGGESTED_ROUTE_ID_ARG = "suggestedRouteId"
    const val SUGGESTED_ROUTE_DETAIL_ROUTE =
        "$SUGGESTED_ROUTE_DETAIL/{$SUGGESTED_ROUTE_ID_ARG}"

    const val ARC_DETAIL_ROUTE_BASE = "arc_detail"
    const val ARC_DETAIL_ARG_ID = "arcPlanId"
    const val ARC_DETAIL_ROUTE =
        "$ARC_DETAIL_ROUTE_BASE/{$ARC_DETAIL_ARG_ID}"

    fun addSkillRoute(
        prefillJourney: String? = null,
        prefillTitle: String? = null,
        prefillSoftMode: Boolean? = null,
        plannedArcTitle: String? = null,
        plannedArcStepIndex: Int? = null,
        plannedArcTotalSteps: Int? = null
    ): String {
        val params = buildList {
            prefillJourney
                ?.takeIf { it.isNotBlank() }
                ?.let { add("$ADD_SKILL_ARG_PREFILL_JOURNEY=${Uri.encode(it)}") }

            prefillTitle
                ?.takeIf { it.isNotBlank() }
                ?.let { add("$ADD_SKILL_ARG_PREFILL_TITLE=${Uri.encode(it)}") }

            prefillSoftMode?.let {
                add("$ADD_SKILL_ARG_PREFILL_SOFT_MODE=$it")
            }

            plannedArcTitle
                ?.takeIf { it.isNotBlank() }
                ?.let { add("$ADD_SKILL_ARG_PLANNED_ARC_TITLE=${Uri.encode(it)}") }

            plannedArcStepIndex?.let {
                add("$ADD_SKILL_ARG_PLANNED_ARC_STEP_INDEX=$it")
            }

            plannedArcTotalSteps?.let {
                add("$ADD_SKILL_ARG_PLANNED_ARC_TOTAL_STEPS=$it")
            }
        }

        return if (params.isEmpty()) ADD_SKILL else "$ADD_SKILL?${params.joinToString("&")}"
    }

    fun suggestedRouteDetailRoute(routeId: String): String =
        "$SUGGESTED_ROUTE_DETAIL/${Uri.encode(routeId)}"

    fun arcDetailRoute(arcPlanId: Long): String =
        "$ARC_DETAIL_ROUTE_BASE/$arcPlanId"

    fun planArcRoute(editArcPlanId: Long? = null): String {
        return if (editArcPlanId == null) {
            PLAN_ARC_ROUTE_BASE
        } else {
            "$PLAN_ARC_ROUTE_BASE?$PLAN_ARC_ARG_EDIT_ID=$editArcPlanId"
        }
    }
}