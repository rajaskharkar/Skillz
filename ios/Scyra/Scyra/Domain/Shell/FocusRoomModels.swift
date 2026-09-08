import Foundation

enum FocusExerciseID: String, CaseIterable, Sendable {
    case threePointGrounding
    case boxBreathing
    case miniBodyScan
    case fourSevenEightBreathing
    case fiveSensesReset
}

enum FocusExerciseStepType: Sendable {
    case intro
    case guidance
    case breathIn
    case hold
    case breathOut
    case grounding
    case bodyScan
    case closing

    var isBreathPhase: Bool {
        self == .breathIn || self == .hold || self == .breathOut
    }
}

enum FocusExerciseVisualState: Sendable {
    case still
    case expand
    case full
    case contract
    case small
    case grounding
    case body
}

struct FocusGuidedExerciseStep: Equatable, Sendable {
    let spokenText: String
    let displayText: String
    let durationSeconds: Int
    let type: FocusExerciseStepType
    let visualState: FocusExerciseVisualState

    init(
        spokenText: String,
        displayText: String? = nil,
        durationSeconds: Int,
        type: FocusExerciseStepType = .guidance,
        visualState: FocusExerciseVisualState = .still
    ) {
        self.spokenText = spokenText
        self.displayText = displayText ?? spokenText
        self.durationSeconds = durationSeconds
        self.type = type
        self.visualState = visualState
    }
}

struct FocusGuidedExercise: Identifiable, Equatable, Sendable {
    let id: FocusExerciseID
    let title: String
    let shortTitle: String
    let description: String
    let bestFor: String
    let purpose: String
    let steps: [FocusGuidedExerciseStep]

    var durationSeconds: Int { steps.reduce(0) { $0 + $1.durationSeconds } }
    var isBreathingExercise: Bool { id == .boxBreathing || id == .fourSevenEightBreathing }
}

enum FocusRoomCatalog {
    static let exercises: [FocusGuidedExercise] = [
        .init(
            id: .threePointGrounding,
            title: "Three-Point Grounding",
            shortTitle: "3 Points",
            description: "A quick reset through sight, touch, and sound.",
            bestFor: "When attention feels scattered and you need to return quickly.",
            purpose: "Return",
            steps: [
                .init(spokenText: "Let's begin Three Point Grounding. We'll return slowly to the room around you.", displayText: "Return to the room around you.", durationSeconds: 6, type: .intro, visualState: .grounding),
                .init(spokenText: "First, notice one thing you can see. Let your eyes rest on it for a moment.", displayText: "Notice one thing you can see.", durationSeconds: 12, type: .grounding, visualState: .grounding),
                .init(spokenText: "Now, notice one thing you can feel. Your feet, your hands, the chair, or the phone in your hand.", displayText: "Notice one thing you can feel.", durationSeconds: 12, type: .grounding, visualState: .grounding),
                .init(spokenText: "Now, notice one thing you can hear. Let the sound arrive without chasing it.", displayText: "Notice one thing you can hear.", durationSeconds: 12, type: .grounding, visualState: .grounding),
                .init(spokenText: "You are here. Return gently to what matters next.", displayText: "You are here.", durationSeconds: 6, type: .closing)
            ]
        ),
        .init(
            id: .boxBreathing,
            title: "Box Breathing",
            shortTitle: "Box",
            description: "A four-count breathing rhythm to settle your attention.",
            bestFor: "Before a Flow, after distraction, or when the mind feels scattered.",
            purpose: "Steady",
            steps: [
                .init(spokenText: "Let's begin Box Breathing. Follow the rhythm on screen. Breathe gently.", displayText: "Follow the rhythm on screen.", durationSeconds: 6, type: .intro),
                breath("Inhale slowly.", "Inhale", 4, .breathIn, .expand),
                breath("Hold gently.", "Hold", 4, .hold, .full),
                breath("Exhale softly.", "Exhale", 4, .breathOut, .contract),
                breath("Rest in the stillness.", "Hold", 4, .hold, .small),
                breath("Inhale slowly.", "Inhale", 4, .breathIn, .expand),
                breath("Hold gently.", "Hold", 4, .hold, .full),
                breath("Exhale softly.", "Exhale", 4, .breathOut, .contract),
                breath("Rest in the stillness.", "Hold", 4, .hold, .small),
                breath("Inhale slowly.", "Inhale", 4, .breathIn, .expand),
                breath("Hold gently.", "Hold", 4, .hold, .full),
                breath("Exhale softly.", "Exhale", 4, .breathOut, .contract),
                breath("Rest in the stillness.", "Hold", 4, .hold, .small),
                .init(spokenText: "Let the breath return to normal. Notice the steadiness you created.", displayText: "Return gently.", durationSeconds: 6, type: .closing)
            ]
        ),
        .init(
            id: .miniBodyScan,
            title: "Mini Body Scan",
            shortTitle: "Body Scan",
            description: "A short scan to soften the body before or after effort.",
            bestFor: "Before deep work, after screen time, or when the body feels tight.",
            purpose: "Soften",
            steps: [
                .init(spokenText: "Let's begin the Mini Body Scan. Let your attention move gently through the body.", displayText: "Let attention move through the body.", durationSeconds: 8, type: .intro, visualState: .body),
                .init(spokenText: "Start with your face. Let the forehead soften. Let the jaw loosen.", displayText: "Soften the face and jaw.", durationSeconds: 12, type: .bodyScan, visualState: .body),
                .init(spokenText: "Move to the shoulders. Let them drop slightly. You do not have to hold the whole day here.", displayText: "Drop the shoulders.", durationSeconds: 14, type: .bodyScan, visualState: .body),
                .init(spokenText: "Notice your hands. Let the fingers soften. Let the grip release.", displayText: "Soften the hands.", durationSeconds: 12, type: .bodyScan, visualState: .body),
                .init(spokenText: "Notice your chest and breath. There is nothing to force. Let the breath move naturally.", displayText: "Notice the breath.", durationSeconds: 14, type: .bodyScan, visualState: .body),
                .init(spokenText: "Notice your back and spine. Let yourself sit with a little more ease.", displayText: "Ease through the back.", durationSeconds: 12, type: .bodyScan, visualState: .body),
                .init(spokenText: "Notice your feet or legs. Feel the support underneath you.", displayText: "Feel the support underneath you.", durationSeconds: 10, type: .bodyScan, visualState: .body),
                .init(spokenText: "Let the body be a little softer than before. Return when you are ready.", displayText: "Return gently.", durationSeconds: 8, type: .closing)
            ]
        ),
        .init(
            id: .fourSevenEightBreathing,
            title: "4-7-8 Breathing",
            shortTitle: "4-7-8",
            description: "A slower breath pattern with a longer exhale.",
            bestFor: "When the body feels tense or the mind feels loud.",
            purpose: "Deepen",
            steps: [
                .init(spokenText: "Let's begin four seven eight breathing. Stay gentle. Never force the breath. If the hold feels uncomfortable, return to normal breathing.", displayText: "Stay gentle. Never force the breath.", durationSeconds: 12, type: .intro),
                breath("Inhale.", "Inhale", 4, .breathIn, .expand),
                breath("Hold.", "Hold", 7, .hold, .full),
                breath("Exhale.", "Exhale", 8, .breathOut, .contract),
                breath("Inhale.", "Inhale", 4, .breathIn, .expand),
                breath("Hold.", "Hold", 7, .hold, .full),
                breath("Exhale.", "Exhale", 8, .breathOut, .contract),
                breath("Inhale.", "Inhale", 4, .breathIn, .expand),
                breath("Hold.", "Hold", 7, .hold, .full),
                breath("Exhale.", "Exhale", 8, .breathOut, .contract),
                .init(spokenText: "Let go of the pattern. Breathe normally. Notice what feels softer now.", displayText: "Breathe normally.", durationSeconds: 10, type: .closing)
            ]
        ),
        .init(
            id: .fiveSensesReset,
            title: "Five Senses Reset",
            shortTitle: "5 Senses",
            description: "A fuller grounding exercise through all five senses.",
            bestFor: "When the mind feels noisy, overwhelmed, or far from the present.",
            purpose: "Reset",
            steps: [
                .init(spokenText: "Let's begin the Five Senses Reset. We'll return gently to the present through the senses.", displayText: "Return through the senses.", durationSeconds: 8, type: .intro, visualState: .grounding),
                .init(spokenText: "Notice five things you can see. Move slowly. Let each one be simple.", displayText: "Notice 5 things you can see.", durationSeconds: 20, type: .grounding, visualState: .grounding),
                .init(spokenText: "Notice four things you can feel. Clothing, air, the floor, the chair, or your hands.", displayText: "Notice 4 things you can feel.", durationSeconds: 18, type: .grounding, visualState: .grounding),
                .init(spokenText: "Notice three things you can hear. Near or far. Loud or quiet.", displayText: "Notice 3 things you can hear.", durationSeconds: 15, type: .grounding, visualState: .grounding),
                .init(spokenText: "Notice two things you can smell. If smell is not clear, simply notice the air around you.", displayText: "Notice 2 things you can smell.", durationSeconds: 12, type: .grounding, visualState: .grounding),
                .init(spokenText: "Notice one thing you can taste. Or simply notice the mouth and breath.", displayText: "Notice 1 thing you can taste.", durationSeconds: 10, type: .grounding, visualState: .grounding),
                .init(spokenText: "You are back in the room. Let your attention settle here.", displayText: "You are here.", durationSeconds: 15, type: .closing)
            ]
        )
    ]

    static func exercise(id: FocusExerciseID) -> FocusGuidedExercise? {
        exercises.first { $0.id == id }
    }

    private static func breath(
        _ spoken: String,
        _ display: String,
        _ seconds: Int,
        _ type: FocusExerciseStepType,
        _ visual: FocusExerciseVisualState
    ) -> FocusGuidedExerciseStep {
        .init(spokenText: spoken, displayText: display, durationSeconds: seconds, type: type, visualState: visual)
    }
}
