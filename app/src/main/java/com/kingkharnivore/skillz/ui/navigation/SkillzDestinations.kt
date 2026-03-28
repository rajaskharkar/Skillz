package com.kingkharnivore.skillz.ui.navigation

import android.net.Uri

object SkillzDestinations {
    const val SKILLS_LIST = "skills_list"
    const val ADD_SKILL = "add_skill"
    const val ADD_SKILL_ARG_PREFILL_JOURNEY = "prefillJourney"
    const val ADD_SKILL_ROUTE =
        "$ADD_SKILL?$ADD_SKILL_ARG_PREFILL_JOURNEY={$ADD_SKILL_ARG_PREFILL_JOURNEY}"

    const val HOME_SCREEN = "home_screen"
    const val SCHEDULE_BEAM = "schedule_beam"

    fun addSkillRoute(prefillJourney: String? = null): String {
        val encodedJourney = prefillJourney
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::encode)

        return if (encodedJourney == null) {
            ADD_SKILL
        } else {
            "$ADD_SKILL?$ADD_SKILL_ARG_PREFILL_JOURNEY=$encodedJourney"
        }
    }
}