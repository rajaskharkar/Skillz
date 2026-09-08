import Foundation

enum FlowMode: String, Codable, CaseIterable, Sendable {
    case flow
    case soft
}

enum FlowEndAction: Sendable {
    case saveFlow
    case continueArc
    case completeArc
}

enum FlowCompletionControls: Equatable, Sendable {
    case standaloneSoft
    case arcActions
    case regularActions

    static func resolve(isSoftMode: Bool, isArcLinked: Bool) -> Self {
        if isSoftMode && !isArcLinked { return .standaloneSoft }
        if isArcLinked { return .arcActions }
        return .regularActions
    }
}

struct Journey: Identifiable, Equatable, Sendable {
    let id: UUID
    let name: String
    let createdAt: Date
    let lastUsedAt: Date?
}

struct ActiveFlowSnapshot: Equatable, Sendable {
    static let singletonID = "active-flow"

    let flowInstanceID: UUID
    var title: String
    var journeyName: String
    var mode: FlowMode
    var isInFlowMode: Bool
    var isRunning: Bool
    var accumulatedDurationMs: Int64
    var segmentStartedAt: Date?
    var firstStartedAt: Date?
    var surgePlannedMs: Int64?
    var healthEnabledAtStart: Bool = false
    var healthAccessRequestedAtStart: Bool = false
    var movementBonusEligibleAtStart: Bool = false
    var originPulseID: UUID? = nil
    var originPulseTitle: String? = nil
    var originPulseJourneyName: String? = nil
    var activeIntervals: [FlowActiveInterval] = []
    let createdAt: Date

    func elapsedMs(at now: Date) -> Int64 {
        guard isRunning, let segmentStartedAt else {
            return max(0, accumulatedDurationMs)
        }
        let currentSegment = max(0, Int64(now.timeIntervalSince(segmentStartedAt) * 1_000))
        return max(0, accumulatedDurationMs + currentSegment)
    }

    var isIdleDraft: Bool {
        !isInFlowMode && !isRunning && accumulatedDurationMs == 0 && segmentStartedAt == nil
    }
}

struct ArcRuntimeState: Equatable, Sendable {
    static let baseMultiplier = 1.0

    let id: UUID
    var isPending: Bool
    var multiplier: Double
    var progressMs: Int64
    var lastSessionEndTime: Date
    var sessionCount: Int
    var pauseUsedMs: Int64
    var pauseStartedAt: Date?

    func resettingMultiplierForSoftFlow() -> Self {
        var copy = self
        copy.multiplier = Self.baseMultiplier
        return copy
    }

    func afterCompletedSoftFlow(at endTime: Date) -> Self {
        var copy = self
        copy.isPending = sessionCount + 1 < 2
        copy.multiplier = Self.baseMultiplier
        copy.progressMs = 0
        copy.lastSessionEndTime = endTime
        copy.sessionCount += 1
        copy.pauseStartedAt = nil
        return copy
    }
}

enum ArcRules {
    static let graceWindowMs: Int64 = 5 * 60_000
    static let progressStepMs: Int64 = 10 * 60_000
    static let multiplierStep = 0.1
    static let startMultiplier = 1.3
    static let earlyPauseBudgetMs: Int64 = 2 * 60_000
    static let latePauseBudgetMs: Int64 = 5 * 60_000
    static let ultraPauseBudgetMs: Int64 = 10 * 60_000

    static func pauseBudget(for arc: ArcRuntimeState) -> Int64 {
        let nextIndex = arc.sessionCount + 1
        if nextIndex <= 3 { return earlyPauseBudgetMs }
        if nextIndex >= 10 { return ultraPauseBudgetMs }
        return latePauseBudgetMs
    }
}

enum ArcContinuationResolver {
    static func resolve(
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?,
        flowStartTime: Date
    ) -> ArcRuntimeState? {
        if let activeArc,
           activeArc.sessionCount > 0,
           isWithinContinuationWindow(activeArc, flowStartTime: flowStartTime) {
            return activeArc
        }

        if let recentlyEndedArc,
           isWithinContinuationWindow(recentlyEndedArc, flowStartTime: flowStartTime) {
            return recentlyEndedArc
        }

        return activeArc?.sessionCount == 0 ? activeArc : nil
    }

    static func isWithinContinuationWindow(
        _ arc: ArcRuntimeState,
        flowStartTime: Date
    ) -> Bool {
        let gapMs = Int64(flowStartTime.timeIntervalSince(arc.lastSessionEndTime) * 1_000)
        return gapMs >= 0 && gapMs <= ArcRules.graceWindowMs
    }
}

struct FlowSession: Identifiable, Equatable, Sendable {
    let id: UUID
    let flowInstanceID: UUID
    let title: String
    let description: String
    let journeyName: String
    let startTime: Date
    let endTime: Date
    let durationMs: Int64
    let surgePlannedMs: Int64?
    let surgePoints: Int
    let scyraPoints: Int
    let isSoftMode: Bool
    let arcID: UUID?
    let arcIndex: Int?
    let arcMultiplierUsed: Double?
    let arcBonusPoints: Int
    let createdAt: Date

    func updatingRewards(scyraPoints: Int, arcBonusPoints: Int) -> FlowSession {
        FlowSession(
            id: id,
            flowInstanceID: flowInstanceID,
            title: title,
            description: description,
            journeyName: journeyName,
            startTime: startTime,
            endTime: endTime,
            durationMs: durationMs,
            surgePlannedMs: surgePlannedMs,
            surgePoints: surgePoints,
            scyraPoints: scyraPoints,
            isSoftMode: isSoftMode,
            arcID: arcID,
            arcIndex: arcIndex,
            arcMultiplierUsed: arcMultiplierUsed,
            arcBonusPoints: arcBonusPoints,
            createdAt: createdAt
        )
    }
}

struct ArcSummary: Equatable, Sendable {
    let totalSessions: Int
    let totalDurationMs: Int64
    let totalFinalPoints: Int
    let totalArcBonusPoints: Int
    let peakMultiplier: Double
    var shellSummary: ShellRewardSummary = .empty
}

/// Builds an Arc-only completion from durable history before clearing runtime
/// state. Keeping the persistence step last means a failed read or write can be
/// retried without losing the Arc that still needs to be concluded.
enum ArcConclusionPolicy {
    static func conclude(
        arcID: UUID,
        fetchSessions: () throws -> [FlowSession],
        fetchShellSummary: () throws -> ShellRewardSummary,
        persistConclusion: () throws -> Void
    ) throws -> ArcSummary? {
        let sessions = try fetchSessions()
        let summary: ArcSummary? = if sessions.isEmpty {
            nil
        } else {
            ArcSummary(
                totalSessions: sessions.count,
                totalDurationMs: sessions.reduce(0) { $0 + $1.durationMs },
                totalFinalPoints: sessions.reduce(0) { $0 + $1.scyraPoints },
                totalArcBonusPoints: sessions.reduce(0) { $0 + $1.arcBonusPoints },
                peakMultiplier: sessions.compactMap(\.arcMultiplierUsed).max() ?? 1,
                shellSummary: try fetchShellSummary()
            )
        }
        try persistConclusion()
        return summary
    }
}

struct FlowReward: Identifiable, Equatable, Sendable {
    let id: UUID
    let minutes: Int
    let baseScyraPoints: Int
    let tenMinuteBonuses: Int
    let thirtyMinuteBonuses: Int
    let sixtyMinuteBonuses: Int
    let finalScyraPoints: Int
    let surgePoints: Int
    let movementSteps: Int64?
    let movementPoints: Int64
    let arcIndex: Int?
    let arcMultiplierUsed: Double?
    let arcBonusPoints: Int
    let arcNextMultiplier: Double?
    let arcDidLevelUp: Bool
    let isSoftSession: Bool
    let arcSummary: ArcSummary?
    var isArcOnlySummary: Bool = false
    var shellReward: ShellSessionReward = .empty

    var hasShellReward: Bool {
        shellReward.pearlsEarned > 0
            || shellReward.stillwaterUnits > 0
            || !shellReward.grantedFindIDs.isEmpty
            || !shellReward.discoveryIDs.isEmpty
            || !shellReward.badgeIDs.isEmpty
    }
}

extension FlowReward {
    static func arcOnly(arcID: UUID, summary: ArcSummary) -> Self {
        FlowReward(
            id: arcID,
            minutes: 0,
            baseScyraPoints: 0,
            tenMinuteBonuses: 0,
            thirtyMinuteBonuses: 0,
            sixtyMinuteBonuses: 0,
            finalScyraPoints: 0,
            surgePoints: 0,
            movementSteps: nil,
            movementPoints: 0,
            arcIndex: nil,
            arcMultiplierUsed: nil,
            arcBonusPoints: 0,
            arcNextMultiplier: nil,
            arcDidLevelUp: false,
            isSoftSession: false,
            arcSummary: summary,
            isArcOnlySummary: true
        )
    }
}
