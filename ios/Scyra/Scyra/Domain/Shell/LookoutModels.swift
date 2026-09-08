import Foundation

enum ObjectivePeriod: String, CaseIterable, Codable, Sendable {
    case daily, weekly, monthly

    var title: String { rawValue.capitalized }
    var dayCount: Int {
        switch self {
        case .daily: 1
        case .weekly: 7
        case .monthly: 30
        }
    }
}

enum ObjectiveKind: String, CaseIterable, Codable, Sendable {
    case oneTime = "one_time"
    case recurring

    var title: String { self == .oneTime ? "One-time" : "Recurring" }
}

enum ObjectiveCardState: String, Codable, Sendable {
    case upcoming, inProgress, completed
}

struct LookoutObjective: Identifiable, Equatable, Sendable {
    let id: UUID
    let journeyID: UUID
    let journeyNameSnapshot: String
    let period: ObjectivePeriod
    let kind: ObjectiveKind
    let targetDurationMs: Int64
    let startAt: Date
    let weeklyBoundaryDay: Int?
    var currentStreak: Int
    var maxStreak: Int
    var totalCompletions: Int
    var isArchived: Bool
    let createdAt: Date
    var updatedAt: Date
}

struct LookoutObjectiveRequest: Equatable, Sendable {
    let journeyID: UUID?
    let journeyName: String
    let period: ObjectivePeriod
    let kind: ObjectiveKind
    let targetDurationMs: Int64
    let startAt: Date
}

struct ObjectiveWindow: Equatable, Sendable {
    let start: Date
    let end: Date

    func contains(_ date: Date) -> Bool { date >= start && date < end }
    var key: String { "\(start.timeIntervalSince1970):\(end.timeIntervalSince1970)" }
}

struct ObjectiveSourceFlow: Identifiable, Equatable, Sendable {
    let id: UUID
    let journeyID: UUID?
    let journeyName: String
    let startTime: Date
    let endTime: Date
    let durationMs: Int64
    let isSoftMode: Bool
}

struct ObjectiveCompletionEvidence: Equatable, Sendable {
    let achievedDurationMs: Int64
    let completedAt: Date
}

struct LookoutCompletion: Identifiable, Equatable, Sendable {
    let id: UUID
    let objectiveID: UUID
    let journeyID: UUID
    let journeyNameSnapshot: String
    let period: ObjectivePeriod
    let kind: ObjectiveKind
    let periodStart: Date
    let periodEnd: Date
    let completedAt: Date
    let achievedDurationMs: Int64
    let targetDurationMs: Int64
    let baseRewardPearls: Int
    let streakBeforeCompletion: Int
    let streakMultiplier: Double
    let finalRewardPearls: Int
    let badgeKey: String
    let badgeLabelSnapshot: String
    var pearlsClaimed: Bool
    var pearlsClaimedAt: Date?
}

struct ObjectiveSkippedCycle: Identifiable, Equatable, Sendable {
    let id: String
    let objectiveID: UUID
    let periodStart: Date
    let periodEnd: Date
    let skippedAt: Date
}

struct ObjectiveCardModel: Identifiable, Equatable, Sendable {
    var id: UUID { objective.id }
    let objective: LookoutObjective
    let window: ObjectiveWindow
    let state: ObjectiveCardState
    let progressDurationMs: Int64
    let progressPercent: Int
    let completion: LookoutCompletion?
    let effectiveCurrentStreak: Int
}

struct RecurringObjectiveStats: Equatable, Sendable {
    let currentStreak: Int
    let maxStreak: Int
    let totalCompletions: Int
}

struct LookoutSnapshot: Equatable, Sendable {
    let cards: [ObjectiveCardModel]
    let completions: [LookoutCompletion]
    let unclaimedPearls: Int
    let unclaimedCompletionCount: Int
}

enum LookoutError: Error, Equatable, LocalizedError {
    case journeyRequired
    case targetRequired
    case startDateInPast
    case duplicateObjective
    case objectiveMissing

    var errorDescription: String? {
        switch self {
        case .journeyRequired: "Choose a Journey first."
        case .targetRequired: "Enter at least 1 minute."
        case .startDateInPast: "Start date must be today or later."
        case .duplicateObjective: "An active objective already exists for this Journey and period."
        case .objectiveMissing: "That objective is no longer available."
        }
    }
}

enum ObjectiveBadgeIdentity {
    static func badgeID(journeyID: UUID, period: ObjectivePeriod) -> String {
        "objective_badge_\(journeyID.uuidString.lowercased())_\(period.rawValue)"
    }
}

enum ObjectiveProgressCalculator {
    static let millisPerMinute: Int64 = 60_000

    static func initialWindow(
        startAt: Date,
        period: ObjectivePeriod,
        calendar: Calendar = .current
    ) -> ObjectiveWindow {
        let start = calendar.startOfDay(for: startAt)
        let end = calendar.date(byAdding: .day, value: period.dayCount, to: start)
            ?? start.addingTimeInterval(Double(period.dayCount * 86_400))
        return ObjectiveWindow(start: start, end: end)
    }

    static func window(
        for objective: LookoutObjective,
        at date: Date,
        calendar: Calendar = .current
    ) -> ObjectiveWindow {
        let initial = initialWindow(startAt: objective.startAt, period: objective.period, calendar: calendar)
        guard objective.kind == .recurring, date >= initial.end else { return initial }
        let nowDay = calendar.startOfDay(for: date)
        let elapsedDays = max(0, calendar.dateComponents([.day], from: initial.start, to: nowDay).day ?? 0)
        let cycle = elapsedDays / objective.period.dayCount
        let start = calendar.date(byAdding: .day, value: cycle * objective.period.dayCount, to: initial.start)
            ?? initial.start
        let end = calendar.date(byAdding: .day, value: objective.period.dayCount, to: start)
            ?? start.addingTimeInterval(Double(objective.period.dayCount * 86_400))
        return ObjectiveWindow(start: start, end: end)
    }

    static func eligibleFlows(
        for objective: LookoutObjective,
        flows: [ObjectiveSourceFlow],
        window: ObjectiveWindow
    ) -> [ObjectiveSourceFlow] {
        flows.filter {
            !$0.isSoftMode &&
            ($0.journeyID == objective.journeyID || $0.journeyName.caseInsensitiveCompare(objective.journeyNameSnapshot) == .orderedSame) &&
            window.contains($0.endTime)
        }.sorted {
            if $0.endTime != $1.endTime { return $0.endTime < $1.endTime }
            return $0.id.uuidString < $1.id.uuidString
        }
    }

    static func completionEvidence(
        for objective: LookoutObjective,
        flows: [ObjectiveSourceFlow],
        window: ObjectiveWindow
    ) -> ObjectiveCompletionEvidence? {
        var total: Int64 = 0
        for flow in eligibleFlows(for: objective, flows: flows, window: window) {
            total += max(0, flow.durationMs)
            if total >= objective.targetDurationMs {
                return ObjectiveCompletionEvidence(achievedDurationMs: total, completedAt: flow.endTime)
            }
        }
        return nil
    }

    static func progress(
        for objective: LookoutObjective,
        flows: [ObjectiveSourceFlow],
        window: ObjectiveWindow
    ) -> Int64 {
        eligibleFlows(for: objective, flows: flows, window: window)
            .reduce(0) { $0 + max(0, $1.durationMs) }
    }

    static func percent(progress: Int64, target: Int64) -> Int {
        guard target > 0 else { return 0 }
        return min(100, max(0, Int((Double(progress) / Double(target)) * 100)))
    }

    static func makeCompletion(
        objective: LookoutObjective,
        window: ObjectiveWindow,
        evidence: ObjectiveCompletionEvidence,
        streakBefore: Int
    ) -> LookoutCompletion {
        let base = max(1, Int(floor(Double(evidence.achievedDurationMs) / Double(millisPerMinute))))
        let effectiveStreak = objective.kind == .recurring ? streakBefore : 0
        let multiplier = objective.kind == .recurring ? 1 + Double(effectiveStreak) * 0.1 : 1
        let reward = Int(floor(Double(base) * multiplier))
        return LookoutCompletion(
            id: UUID(), objectiveID: objective.id, journeyID: objective.journeyID,
            journeyNameSnapshot: objective.journeyNameSnapshot, period: objective.period,
            kind: objective.kind, periodStart: window.start, periodEnd: window.end,
            completedAt: evidence.completedAt, achievedDurationMs: evidence.achievedDurationMs,
            targetDurationMs: objective.targetDurationMs, baseRewardPearls: base,
            streakBeforeCompletion: effectiveStreak, streakMultiplier: multiplier,
            finalRewardPearls: reward,
            badgeKey: ObjectiveBadgeIdentity.badgeID(journeyID: objective.journeyID, period: objective.period),
            badgeLabelSnapshot: "\(objective.journeyNameSnapshot) \(objective.period.title) Objective",
            pearlsClaimed: false, pearlsClaimedAt: nil
        )
    }
}

enum RecurringObjectiveStatsCalculator {
    static func derive(
        objective: LookoutObjective,
        completions: [LookoutCompletion],
        skipped: [ObjectiveSkippedCycle],
        asOf: Date,
        calendar: Calendar = .current
    ) -> RecurringObjectiveStats {
        let relevant = completions.filter { $0.objectiveID == objective.id }.sorted { $0.periodStart < $1.periodStart }
        guard objective.kind == .recurring else { return .init(currentStreak: 0, maxStreak: 0, totalCompletions: relevant.count) }
        var streak = 0
        var best = 0
        var previousEnd: Date?
        for completion in relevant {
            streak = previousEnd == completion.periodStart ? streak + 1 : 1
            best = max(best, streak)
            previousEnd = completion.periodEnd
        }
        let current = ObjectiveProgressCalculator.window(for: objective, at: asOf, calendar: calendar)
        let currentSkipped = skipped.contains { $0.objectiveID == objective.id && $0.periodStart == current.start }
        let latest = relevant.last
        let currentStreak = !currentSkipped && (latest?.periodStart == current.start || latest?.periodEnd == current.start) ? streak : 0
        return .init(currentStreak: currentStreak, maxStreak: best, totalCompletions: relevant.count)
    }

    static func streakBefore(
        objective: LookoutObjective,
        completions: [LookoutCompletion],
        skipped: [ObjectiveSkippedCycle],
        periodStart: Date
    ) -> Int {
        let relevant = completions.filter { $0.objectiveID == objective.id && $0.periodStart < periodStart }
            .sorted { $0.periodStart < $1.periodStart }
        var streak = 0
        var previousEnd: Date?
        for completion in relevant {
            streak = previousEnd == completion.periodStart ? streak + 1 : 1
            previousEnd = completion.periodEnd
        }
        let previousWasSkipped = skipped.contains { $0.objectiveID == objective.id && $0.periodEnd == periodStart }
        return !previousWasSkipped && relevant.last?.periodEnd == periodStart ? streak : 0
    }
}
