package com.kingkharnivore.skillz.ui.screen.shell.rooms.focus

enum class FocusExerciseId {
    THREE_POINT_GROUNDING,
    BOX_BREATHING,
    MINI_BODY_SCAN,
    FOUR_SEVEN_EIGHT_BREATHING,
    FIVE_SENSES_RESET
}

enum class FocusExerciseCategory {
    ORIGINAL_5
}

enum class FocusExerciseStepType {
    INTRO,
    GUIDANCE,
    BREATH_IN,
    HOLD,
    BREATH_OUT,
    GROUNDING,
    BODY_SCAN,
    CLOSING
}

enum class FocusExerciseVisualState {
    STILL,
    EXPAND,
    FULL,
    CONTRACT,
    SMALL,
    GROUNDING,
    BODY
}

data class FocusGuidedExercise(
    val id: FocusExerciseId,
    val title: String,
    val shortTitle: String,
    val category: FocusExerciseCategory = FocusExerciseCategory.ORIGINAL_5,
    val description: String,
    val bestFor: String,
    val steps: List<FocusGuidedExerciseStep>
) {
    val durationSeconds: Int
        get() = steps.sumOf { it.durationSeconds }
}

data class FocusGuidedExerciseStep(
    val spokenText: String,
    val displayText: String = spokenText,
    val durationSeconds: Int,
    val type: FocusExerciseStepType = FocusExerciseStepType.GUIDANCE,
    val visualState: FocusExerciseVisualState = FocusExerciseVisualState.STILL
)

object FocusRoomOriginalExercises {

    private val threePointGrounding = FocusGuidedExercise(
        id = FocusExerciseId.THREE_POINT_GROUNDING,
        title = "Three-Point Grounding",
        shortTitle = "3 Points",
        description = "A quick reset through sight, touch, and sound.",
        bestFor = "When attention feels scattered and you need to return quickly.",
        steps = listOf(
            FocusGuidedExerciseStep(
                spokenText = "Let's begin Three Point Grounding. We'll return slowly to the room around you.",
                displayText = "Return to the room around you.",
                durationSeconds = 6,
                type = FocusExerciseStepType.INTRO,
                visualState = FocusExerciseVisualState.GROUNDING
            ),
            FocusGuidedExerciseStep(
                spokenText = "First, notice one thing you can see. Let your eyes rest on it for a moment.",
                displayText = "Notice one thing you can see.",
                durationSeconds = 12,
                type = FocusExerciseStepType.GROUNDING,
                visualState = FocusExerciseVisualState.GROUNDING
            ),
            FocusGuidedExerciseStep(
                spokenText = "Now, notice one thing you can feel. Your feet, your hands, the chair, or the phone in your hand.",
                displayText = "Notice one thing you can feel.",
                durationSeconds = 12,
                type = FocusExerciseStepType.GROUNDING,
                visualState = FocusExerciseVisualState.GROUNDING
            ),
            FocusGuidedExerciseStep(
                spokenText = "Now, notice one thing you can hear. Let the sound arrive without chasing it.",
                displayText = "Notice one thing you can hear.",
                durationSeconds = 12,
                type = FocusExerciseStepType.GROUNDING,
                visualState = FocusExerciseVisualState.GROUNDING
            ),
            FocusGuidedExerciseStep(
                spokenText = "You are here. Return gently to what matters next.",
                displayText = "You are here.",
                durationSeconds = 6,
                type = FocusExerciseStepType.CLOSING,
                visualState = FocusExerciseVisualState.STILL
            )
        )
    )

    private val boxBreathing = FocusGuidedExercise(
        id = FocusExerciseId.BOX_BREATHING,
        title = "Box Breathing",
        shortTitle = "Box",
        description = "A four-count breathing rhythm to settle your attention.",
        bestFor = "Before a Flow, after distraction, or when the mind feels scattered.",
        steps = listOf(
            FocusGuidedExerciseStep(
                spokenText = "Let's begin Box Breathing. Follow the rhythm on screen. Breathe gently.",
                displayText = "Follow the rhythm on screen.",
                durationSeconds = 6,
                type = FocusExerciseStepType.INTRO,
                visualState = FocusExerciseVisualState.STILL
            ),

            // Cycle 1
            FocusGuidedExerciseStep(
                spokenText = "Inhale slowly.",
                displayText = "Inhale",
                durationSeconds = 4,
                type = FocusExerciseStepType.BREATH_IN,
                visualState = FocusExerciseVisualState.EXPAND
            ),
            FocusGuidedExerciseStep(
                spokenText = "Hold gently.",
                displayText = "Hold",
                durationSeconds = 4,
                type = FocusExerciseStepType.HOLD,
                visualState = FocusExerciseVisualState.FULL
            ),
            FocusGuidedExerciseStep(
                spokenText = "Exhale softly.",
                displayText = "Exhale",
                durationSeconds = 4,
                type = FocusExerciseStepType.BREATH_OUT,
                visualState = FocusExerciseVisualState.CONTRACT
            ),
            FocusGuidedExerciseStep(
                spokenText = "Rest in the stillness.",
                displayText = "Hold",
                durationSeconds = 4,
                type = FocusExerciseStepType.HOLD,
                visualState = FocusExerciseVisualState.SMALL
            ),

            // Cycle 2
            FocusGuidedExerciseStep(
                spokenText = "Inhale slowly.",
                displayText = "Inhale",
                durationSeconds = 4,
                type = FocusExerciseStepType.BREATH_IN,
                visualState = FocusExerciseVisualState.EXPAND
            ),
            FocusGuidedExerciseStep(
                spokenText = "Hold gently.",
                displayText = "Hold",
                durationSeconds = 4,
                type = FocusExerciseStepType.HOLD,
                visualState = FocusExerciseVisualState.FULL
            ),
            FocusGuidedExerciseStep(
                spokenText = "Exhale softly.",
                displayText = "Exhale",
                durationSeconds = 4,
                type = FocusExerciseStepType.BREATH_OUT,
                visualState = FocusExerciseVisualState.CONTRACT
            ),
            FocusGuidedExerciseStep(
                spokenText = "Rest in the stillness.",
                displayText = "Hold",
                durationSeconds = 4,
                type = FocusExerciseStepType.HOLD,
                visualState = FocusExerciseVisualState.SMALL
            ),

            // Cycle 3
            FocusGuidedExerciseStep(
                spokenText = "Inhale slowly.",
                displayText = "Inhale",
                durationSeconds = 4,
                type = FocusExerciseStepType.BREATH_IN,
                visualState = FocusExerciseVisualState.EXPAND
            ),
            FocusGuidedExerciseStep(
                spokenText = "Hold gently.",
                displayText = "Hold",
                durationSeconds = 4,
                type = FocusExerciseStepType.HOLD,
                visualState = FocusExerciseVisualState.FULL
            ),
            FocusGuidedExerciseStep(
                spokenText = "Exhale softly.",
                displayText = "Exhale",
                durationSeconds = 4,
                type = FocusExerciseStepType.BREATH_OUT,
                visualState = FocusExerciseVisualState.CONTRACT
            ),
            FocusGuidedExerciseStep(
                spokenText = "Rest in the stillness.",
                displayText = "Hold",
                durationSeconds = 4,
                type = FocusExerciseStepType.HOLD,
                visualState = FocusExerciseVisualState.SMALL
            ),

            FocusGuidedExerciseStep(
                spokenText = "Let the breath return to normal. Notice the steadiness you created.",
                displayText = "Return gently.",
                durationSeconds = 6,
                type = FocusExerciseStepType.CLOSING,
                visualState = FocusExerciseVisualState.STILL
            )
        )
    )

    private val miniBodyScan = FocusGuidedExercise(
        id = FocusExerciseId.MINI_BODY_SCAN,
        title = "Mini Body Scan",
        shortTitle = "Body Scan",
        description = "A short scan to soften the body before or after effort.",
        bestFor = "Before deep work, after screen time, or when the body feels tight.",
        steps = listOf(
            FocusGuidedExerciseStep(
                spokenText = "Let's begin the Mini Body Scan. Let your attention move gently through the body.",
                displayText = "Let attention move through the body.",
                durationSeconds = 8,
                type = FocusExerciseStepType.INTRO,
                visualState = FocusExerciseVisualState.BODY
            ),
            FocusGuidedExerciseStep(
                spokenText = "Start with your face. Let the forehead soften. Let the jaw loosen.",
                displayText = "Soften the face and jaw.",
                durationSeconds = 12,
                type = FocusExerciseStepType.BODY_SCAN,
                visualState = FocusExerciseVisualState.BODY
            ),
            FocusGuidedExerciseStep(
                spokenText = "Move to the shoulders. Let them drop slightly. You do not have to hold the whole day here.",
                displayText = "Drop the shoulders.",
                durationSeconds = 14,
                type = FocusExerciseStepType.BODY_SCAN,
                visualState = FocusExerciseVisualState.BODY
            ),
            FocusGuidedExerciseStep(
                spokenText = "Notice your hands. Let the fingers soften. Let the grip release.",
                displayText = "Soften the hands.",
                durationSeconds = 12,
                type = FocusExerciseStepType.BODY_SCAN,
                visualState = FocusExerciseVisualState.BODY
            ),
            FocusGuidedExerciseStep(
                spokenText = "Notice your chest and breath. There is nothing to force. Let the breath move naturally.",
                displayText = "Notice the breath.",
                durationSeconds = 14,
                type = FocusExerciseStepType.BODY_SCAN,
                visualState = FocusExerciseVisualState.BODY
            ),
            FocusGuidedExerciseStep(
                spokenText = "Notice your back and spine. Let yourself sit with a little more ease.",
                displayText = "Ease through the back.",
                durationSeconds = 12,
                type = FocusExerciseStepType.BODY_SCAN,
                visualState = FocusExerciseVisualState.BODY
            ),
            FocusGuidedExerciseStep(
                spokenText = "Notice your feet or legs. Feel the support underneath you.",
                displayText = "Feel the support underneath you.",
                durationSeconds = 10,
                type = FocusExerciseStepType.BODY_SCAN,
                visualState = FocusExerciseVisualState.BODY
            ),
            FocusGuidedExerciseStep(
                spokenText = "Let the body be a little softer than before. Return when you are ready.",
                displayText = "Return gently.",
                durationSeconds = 8,
                type = FocusExerciseStepType.CLOSING,
                visualState = FocusExerciseVisualState.STILL
            )
        )
    )

    private val fourSevenEightBreathing = FocusGuidedExercise(
        id = FocusExerciseId.FOUR_SEVEN_EIGHT_BREATHING,
        title = "4-7-8 Breathing",
        shortTitle = "4-7-8",
        description = "A slower breath pattern with a longer exhale.",
        bestFor = "When the body feels tense or the mind feels loud.",
        steps = listOf(
            FocusGuidedExerciseStep(
                spokenText = "Let's begin four seven eight breathing. Stay gentle. Never force the breath. If the hold feels uncomfortable, return to normal breathing.",
                displayText = "Stay gentle. Never force the breath.",
                durationSeconds = 12,
                type = FocusExerciseStepType.INTRO,
                visualState = FocusExerciseVisualState.STILL
            ),

            FocusGuidedExerciseStep("Inhale.", "Inhale", 4, FocusExerciseStepType.BREATH_IN, FocusExerciseVisualState.EXPAND),
            FocusGuidedExerciseStep("Hold.", "Hold", 7, FocusExerciseStepType.HOLD, FocusExerciseVisualState.FULL),
            FocusGuidedExerciseStep("Exhale.", "Exhale", 8, FocusExerciseStepType.BREATH_OUT, FocusExerciseVisualState.CONTRACT),

            FocusGuidedExerciseStep("Inhale.", "Inhale", 4, FocusExerciseStepType.BREATH_IN, FocusExerciseVisualState.EXPAND),
            FocusGuidedExerciseStep("Hold.", "Hold", 7, FocusExerciseStepType.HOLD, FocusExerciseVisualState.FULL),
            FocusGuidedExerciseStep("Exhale.", "Exhale", 8, FocusExerciseStepType.BREATH_OUT, FocusExerciseVisualState.CONTRACT),

            FocusGuidedExerciseStep("Inhale.", "Inhale", 4, FocusExerciseStepType.BREATH_IN, FocusExerciseVisualState.EXPAND),
            FocusGuidedExerciseStep("Hold.", "Hold", 7, FocusExerciseStepType.HOLD, FocusExerciseVisualState.FULL),
            FocusGuidedExerciseStep("Exhale.", "Exhale", 8, FocusExerciseStepType.BREATH_OUT, FocusExerciseVisualState.CONTRACT),

            FocusGuidedExerciseStep(
                spokenText = "Let go of the pattern. Breathe normally. Notice what feels softer now.",
                displayText = "Breathe normally.",
                durationSeconds = 10,
                type = FocusExerciseStepType.CLOSING,
                visualState = FocusExerciseVisualState.STILL
            )
        )
    )

    private val fiveSensesReset = FocusGuidedExercise(
        id = FocusExerciseId.FIVE_SENSES_RESET,
        title = "Five Senses Reset",
        shortTitle = "5 Senses",
        description = "A fuller grounding exercise through all five senses.",
        bestFor = "When the mind feels noisy, overwhelmed, or far from the present.",
        steps = listOf(
            FocusGuidedExerciseStep(
                spokenText = "Let's begin the Five Senses Reset. We'll return gently to the present through the senses.",
                displayText = "Return through the senses.",
                durationSeconds = 8,
                type = FocusExerciseStepType.INTRO,
                visualState = FocusExerciseVisualState.GROUNDING
            ),
            FocusGuidedExerciseStep(
                spokenText = "Notice five things you can see. Move slowly. Let each one be simple.",
                displayText = "Notice 5 things you can see.",
                durationSeconds = 20,
                type = FocusExerciseStepType.GROUNDING,
                visualState = FocusExerciseVisualState.GROUNDING
            ),
            FocusGuidedExerciseStep(
                spokenText = "Notice four things you can feel. Clothing, air, the floor, the chair, or your hands.",
                displayText = "Notice 4 things you can feel.",
                durationSeconds = 18,
                type = FocusExerciseStepType.GROUNDING,
                visualState = FocusExerciseVisualState.GROUNDING
            ),
            FocusGuidedExerciseStep(
                spokenText = "Notice three things you can hear. Near or far. Loud or quiet.",
                displayText = "Notice 3 things you can hear.",
                durationSeconds = 15,
                type = FocusExerciseStepType.GROUNDING,
                visualState = FocusExerciseVisualState.GROUNDING
            ),
            FocusGuidedExerciseStep(
                spokenText = "Notice two things you can smell. If smell is not clear, simply notice the air around you.",
                displayText = "Notice 2 things you can smell.",
                durationSeconds = 12,
                type = FocusExerciseStepType.GROUNDING,
                visualState = FocusExerciseVisualState.GROUNDING
            ),
            FocusGuidedExerciseStep(
                spokenText = "Notice one thing you can taste. Or simply notice the mouth and breath.",
                displayText = "Notice 1 thing you can taste.",
                durationSeconds = 10,
                type = FocusExerciseStepType.GROUNDING,
                visualState = FocusExerciseVisualState.GROUNDING
            ),
            FocusGuidedExerciseStep(
                spokenText = "You are back in the room. Let your attention settle here.",
                displayText = "You are here.",
                durationSeconds = 15,
                type = FocusExerciseStepType.CLOSING,
                visualState = FocusExerciseVisualState.STILL
            )
        )
    )

    /**
     * Final Original 5 order:
     *
     * 1. Three-Point Grounding — Return
     * 2. Box Breathing — Steady
     * 3. Mini Body Scan — Soften
     * 4. 4-7-8 Breathing — Deepen
     * 5. Five Senses Reset — Reset
     */
    val exercises: List<FocusGuidedExercise> = listOf(
        threePointGrounding,
        boxBreathing,
        miniBodyScan,
        fourSevenEightBreathing,
        fiveSensesReset
    )

    fun getById(id: FocusExerciseId): FocusGuidedExercise? {
        return exercises.firstOrNull { it.id == id }
    }
}