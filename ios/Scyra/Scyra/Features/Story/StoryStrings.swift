import Foundation

/// Android `res/values/strings.xml` is the canonical source for this copy.
enum StoryStrings {
    static let observeJourneys = String(localized: "Observe journeys:")
    static let all = String(localized: "All")
    static let timeInView = String(localized: "Time in view")
    static let totalTime = String(localized: "TOTAL TIME")
    static let selectedJourneyTime = String(localized: "Time in selected journeys")
    static let sagas = String(localized: "Sagas")
    static let chronicles = String(localized: "Chronicles")
    static let scyraScore = String(localized: "Scyra Score")
    static let noFlowsCurrent = String(localized: "No Flows yet")
    static let noFlowsPast = String(localized: "No Flows in this view")
    static let firstFlow = String(localized: "Your Story begins with the first Flow.")
    static let startStory = String(localized: "Start your Story!")
    static let goToday = String(localized: "Go to Today")
    static let showFlows = String(localized: "Show flows")
    static let hideFlows = String(localized: "Hide flows")
    static let arcScore = String(localized: "Arc Score")
    static let peak = String(localized: "Peak")
    static let duration = String(localized: "Duration")
    static let multiplierUsed = String(localized: "Multiplier used")

    static func surgeBonus(_ score: Int) -> String {
        String(localized: "+\(score) Surge")
    }

    static func scorePeriodLabel(for period: StoryPeriod) -> String {
        switch period {
        case .day:
            scyraScore
        case .week:
            String(localized: "This week")
        case .month:
            String(localized: "This month")
        }
    }

    static func scoreAccessibilityLabel(
        score: Int,
        surgeScore: Int,
        period: StoryPeriod
    ) -> String {
        let periodLabel = scorePeriodLabel(for: period)
        guard surgeScore > 0 else {
            return String(localized: "\(score) points, \(periodLabel)")
        }
        return String(localized: "\(score) points, \(surgeBonus(surgeScore)), \(periodLabel)")
    }
}
