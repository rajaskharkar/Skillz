package com.kingkharnivore.skillz.model

data class ArcMetadata(
    val arcId: Long,
    val title: String? = null,
    val summary: String? = null,
    val outcome: String? = null,
    val highlight: String? = null,
    val nextStep: String? = null
) {
    val isEmpty: Boolean
        get() = title == null && summary == null && outcome == null && highlight == null && nextStep == null

    val hasReflection: Boolean
        get() = outcome != null || highlight != null || nextStep != null

    companion object {
        const val TITLE_LIMIT = 60
        const val SUMMARY_LIMIT = 500
        const val REFLECTION_LIMIT = 250

        fun normalize(
            arcId: Long,
            title: String,
            summary: String,
            outcome: String,
            highlight: String,
            nextStep: String
        ): ArcMetadata = ArcMetadata(
            arcId = arcId,
            title = title.normalized(),
            summary = summary.normalized(),
            outcome = outcome.normalized(),
            highlight = highlight.normalized(),
            nextStep = nextStep.normalized()
        )

        private fun String.normalized(): String? = trim().takeIf(String::isNotEmpty)
    }
}
