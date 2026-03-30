package com.kingkharnivore.skillz.ui.navigation

import android.net.Uri

object SkillzDestinations {
    const val SKILLS_LIST = "skills_list"
    const val ADD_SKILL = "add_skill"

    const val ADD_SKILL_ARG_PREFILL_JOURNEY = "prefillJourney"
    const val ADD_SKILL_ARG_PREFILL_TITLE = "prefillTitle"
    const val ADD_SKILL_ARG_PREFILL_SOFT_MODE = "prefillSoftMode"

    const val ADD_SKILL_ROUTE =
        "$ADD_SKILL?" +
                "$ADD_SKILL_ARG_PREFILL_JOURNEY={$ADD_SKILL_ARG_PREFILL_JOURNEY}&" +
                "$ADD_SKILL_ARG_PREFILL_TITLE={$ADD_SKILL_ARG_PREFILL_TITLE}&" +
                "$ADD_SKILL_ARG_PREFILL_SOFT_MODE={$ADD_SKILL_ARG_PREFILL_SOFT_MODE}"

    const val HOME_SCREEN = "home_screen"
    const val SCHEDULE_BEAM = "schedule_beam"
    const val ADD_PULSE_ROUTE = "add_pulse"

    fun addSkillRoute(
        prefillJourney: String? = null,
        prefillTitle: String? = null,
        prefillSoftMode: Boolean? = null
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
        }

        return if (params.isEmpty()) {
            ADD_SKILL
        } else {
            "$ADD_SKILL?${params.joinToString("&")}"
        }
    }
}