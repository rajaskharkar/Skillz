import Foundation

/// Android `res/values/strings.xml` is the canonical source for this copy.
enum FlowStrings {
    static let title = String(localized: "Flow")
    static let focus = String(localized: "Focus")
    static let titlePlaceholder = String(localized: "What are you flowing through?")
    static let journeys = String(localized: "Journeys")
    static let journeyPlaceholder = String(localized: "Choose or type a Journey")
    static let mode = String(localized: "Mode")
    static let flowMode = String(localized: "Flow")
    static let flowModeSubtitle = String(localized: "Scored")
    static let softMode = String(localized: "Soft")
    static let softModeSubtitle = String(localized: "Gentle")
    static let modeLocked = String(localized: "Mode locks once time starts.")
    static let inFlow = String(localized: "In Flow")
    static let reset = String(localized: "Reset")
    static let resetTitle = String(localized: "Reset session?")
    static let resetConfirmation = String(localized: "Yes, reset")
    static let cancel = String(localized: "Cancel")
    static let enterFlow = String(localized: "Enter Flow")
    static let exitFlow = String(localized: "Exit Flow")
    static let beginSoftFlow = String(localized: "Begin Soft Flow")
    static let exitSoftFlow = String(localized: "Exit Soft Flow")
    static let softFlowLabel = String(localized: "Soft Flow")
    static let softFlowBody = String(localized: "This session will be recorded without score or Surge.")
    static let inFlowHelper = String(localized: "You're in Flow. You may use other parts of this app.\nYou may turn off the screen — the timer continues.")
    static let surge = String(localized: "Surge")
    static let surgeBody = String(localized: "Set a planned time limit. Finish early to earn Surge Points.")
    static let set = String(localized: "Set")
    static let turnOff = String(localized: "Turn Off")
    static let saveSoftFlow = String(localized: "Save Soft Flow")
    static let continueArc = String(localized: "Continue Arc")
    static let completeArc = String(localized: "Complete Arc")
    static let completeFlow = String(localized: "Complete Flow")
    static let saving = String(localized: "Saving...")
    static let done = String(localized: "Done")
    static let next = String(localized: "Next")
    static let enterShell = String(localized: "Enter The Shell")
    static let youDidIt = String(localized: "You did it!")
    static let arcReward = String(localized: "Arc Reward")
    static let softRecorded = String(localized: "Soft Flow recorded")
    static let flowCompleted = String(localized: "Flow completed.")
    static let surgeCompleted = String(localized: "Surge completed.")
    static let loggedStory = String(localized: "Logged into your story.")
    static let totalScore = String(localized: "Total Scyra Score")
    static let duration = String(localized: "Duration")
    static let baseScore = String(localized: "Base Scyra score")
    static let arcBonus = String(localized: "Arc bonus")
    static let softRewardBody = String(localized: "You showed up without turning this session into a score chase.")
    static let softRewardFooter = String(localized: "This session is part of your Story, but it does not affect score or Surge.")
    static let titleAndJourneyRequired = String(localized: "Title and Skill are required")
    static let startTimerBeforeSaving = String(localized: "Start the timer before saving.")
    static let persistenceFailure = String(localized: "Scyra couldn't save this Flow. Try again.")
    static let arcResumed = String(localized: "Arc resumed. Momentum preserved.")

    static func plannedSurge(minutes: Int) -> String {
        String(localized: "Planned: \(minutes) min")
    }

    static func arcMultiplier(_ multiplier: Double) -> String {
        String(localized: "Arc ×\(multiplier.formatted(.number.precision(.fractionLength(1))))")
    }

    static func arcFlowIndex(_ index: Int) -> String {
        String(localized: "Flow \(index)")
    }
}
