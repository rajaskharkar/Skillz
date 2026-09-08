import Foundation

enum FlowHealthSyncStatus: String, Codable, CaseIterable, Sendable {
    case notEnabled = "NOT_ENABLED"
    case notEligible = "NOT_ELIGIBLE"
    case pending = "PENDING"
    case noReward = "NO_REWARD"
    case captured = "CAPTURED"
    case expired = "EXPIRED"
    case permissionRevoked = "PERMISSION_REVOKED"
    case disabledBeforeCapture = "DISABLED_BEFORE_CAPTURE"
    case errorRetryable = "ERROR_RETRYABLE"
    case errorFinal = "ERROR_FINAL"

    var isRefreshable: Bool {
        switch self {
        case .pending, .noReward, .captured, .errorRetryable:
            true
        default:
            false
        }
    }
}

struct FlowActiveInterval: Codable, Equatable, Sendable {
    let start: Date
    let end: Date
}

enum FlowActiveIntervalNormalizer {
    static func normalize(_ intervals: [FlowActiveInterval]) -> [FlowActiveInterval] {
        let sorted = intervals
            .filter { $0.end > $0.start }
            .sorted { $0.start < $1.start }
        guard !sorted.isEmpty else { return [] }

        var merged: [FlowActiveInterval] = []
        for interval in sorted {
            guard let last = merged.last else {
                merged.append(interval)
                continue
            }
            if interval.start > last.end {
                merged.append(interval)
            } else {
                merged[merged.count - 1] = FlowActiveInterval(
                    start: last.start,
                    end: max(last.end, interval.end)
                )
            }
        }
        return merged
    }
}

enum MovementReadResult: Equatable, Sendable {
    case success(steps: Int64)
    case permissionMissing
    case unavailable
    case noData
    case failure
}

enum MovementStepAggregator {
    static func read(
        intervals: [FlowActiveInterval],
        readInterval: (FlowActiveInterval) async -> MovementReadResult
    ) async -> MovementReadResult {
        let normalized = FlowActiveIntervalNormalizer.normalize(intervals)
        guard !normalized.isEmpty else { return .noData }

        var total: Int64 = 0
        var sawSuccess = false
        for interval in normalized {
            switch await readInterval(interval) {
            case .success(let steps):
                sawSuccess = true
                total += max(0, steps)
            case .noData:
                continue
            case .permissionMissing:
                return .permissionMissing
            case .unavailable:
                return .unavailable
            case .failure:
                return .failure
            }
        }
        return sawSuccess ? .success(steps: total) : .noData
    }
}

enum MovementBonusCalculator {
    static let stepsPerPoint: Int64 = 100

    static func points(for steps: Int64) -> Int64 {
        max(0, steps) / stepsPerPoint
    }
}

struct MovementBonusEligibilityInput: Equatable, Sendable {
    let movementBonusEnabled: Bool
    let healthDataAvailable: Bool
    let healthAccessRequested: Bool
    let isRegularPointEligibleFlow: Bool
    let isSoftFlow: Bool
}

enum MovementBonusEligibilityPolicy {
    static func isEligible(_ input: MovementBonusEligibilityInput) -> Bool {
        input.movementBonusEnabled
            && input.healthDataAvailable
            && input.healthAccessRequested
            && input.isRegularPointEligibleFlow
            && !input.isSoftFlow
    }
}

struct FlowRewardBreakdown: Equatable, Sendable {
    let sessionID: UUID
    let nonMovementPreMultiplierPoints: Int64
    let pulseBonusPoints: Int64
    let surgeBonusPoints: Int64
    let otherPreMultiplierBonusPoints: Int64
    let movementPoints: Int64
    let preMultiplierTotal: Int64
    let arcMultiplier: Double
    let streakMultiplier: Double
    let otherMultiplier: Double
    let arcBonusPoints: Int64
    let finalScyraPoints: Int64
    let pearlsEarned: Int64
    let pearlEligible: Bool

    func rebased(to sessionID: UUID) -> FlowRewardBreakdown {
        FlowRewardBreakdown(
            sessionID: sessionID,
            nonMovementPreMultiplierPoints: nonMovementPreMultiplierPoints,
            pulseBonusPoints: pulseBonusPoints,
            surgeBonusPoints: surgeBonusPoints,
            otherPreMultiplierBonusPoints: otherPreMultiplierBonusPoints,
            movementPoints: movementPoints,
            preMultiplierTotal: preMultiplierTotal,
            arcMultiplier: arcMultiplier,
            streakMultiplier: streakMultiplier,
            otherMultiplier: otherMultiplier,
            arcBonusPoints: arcBonusPoints,
            finalScyraPoints: finalScyraPoints,
            pearlsEarned: pearlsEarned,
            pearlEligible: pearlEligible
        )
    }
}

enum MovementRewardRecalculator {
    static func calculate(
        sessionID: UUID,
        nonMovementPreMultiplierPoints: Int64,
        pulseBonusPoints: Int64 = 0,
        surgeBonusPoints: Int64 = 0,
        otherPreMultiplierBonusPoints: Int64 = 0,
        movementPoints: Int64,
        arcMultiplier: Double = 1,
        streakMultiplier: Double = 1,
        otherMultiplier: Double = 1,
        pearlEligible: Bool
    ) -> FlowRewardBreakdown {
        let preMultiplierTotal = nonMovementPreMultiplierPoints
            + pulseBonusPoints
            + surgeBonusPoints
            + otherPreMultiplierBonusPoints
            + movementPoints
        let multiplier = arcMultiplier * streakMultiplier * otherMultiplier
        let finalPoints = Int64((Double(preMultiplierTotal) * multiplier).rounded())
        return FlowRewardBreakdown(
            sessionID: sessionID,
            nonMovementPreMultiplierPoints: nonMovementPreMultiplierPoints,
            pulseBonusPoints: pulseBonusPoints,
            surgeBonusPoints: surgeBonusPoints,
            otherPreMultiplierBonusPoints: otherPreMultiplierBonusPoints,
            movementPoints: movementPoints,
            preMultiplierTotal: preMultiplierTotal,
            arcMultiplier: arcMultiplier,
            streakMultiplier: streakMultiplier,
            otherMultiplier: otherMultiplier,
            arcBonusPoints: max(0, finalPoints - preMultiplierTotal),
            finalScyraPoints: finalPoints,
            pearlsEarned: pearlEligible ? finalPoints : 0,
            pearlEligible: pearlEligible
        )
    }
}

struct StoredMovementRewardContext: Equatable, Sendable {
    let sessionID: UUID
    let nonMovementPreMultiplierPoints: Int64
    let pulseBonusPoints: Int64
    let surgeBonusPoints: Int64
    let otherPreMultiplierBonusPoints: Int64
    let existingMovementPoints: Int64
    let oldFinalScyraPoints: Int64
    let arcMultiplier: Double
    let streakMultiplier: Double
    let otherMultiplier: Double
    let pearlEligible: Bool
}

struct DelayedMovementRewardResult: Equatable, Sendable {
    let newRawMovementPoints: Int64
    let newFinalScyraPoints: Int64
    let newPreMultiplierTotal: Int64
    let newArcBonusPoints: Int64
    let deltaScyraPoints: Int64
    let pearlDelta: Int64
    let pearlsEarned: Int64
}

enum DelayedMovementRewardPolicy {
    static func calculate(steps: Int64, context: StoredMovementRewardContext) -> DelayedMovementRewardResult {
        let movementPoints = max(context.existingMovementPoints, MovementBonusCalculator.points(for: steps))
        let recalculated = MovementRewardRecalculator.calculate(
            sessionID: context.sessionID,
            nonMovementPreMultiplierPoints: context.nonMovementPreMultiplierPoints,
            pulseBonusPoints: context.pulseBonusPoints,
            surgeBonusPoints: context.surgeBonusPoints,
            otherPreMultiplierBonusPoints: context.otherPreMultiplierBonusPoints,
            movementPoints: movementPoints,
            arcMultiplier: context.arcMultiplier,
            streakMultiplier: context.streakMultiplier,
            otherMultiplier: context.otherMultiplier,
            pearlEligible: context.pearlEligible
        )
        let finalPoints = max(context.oldFinalScyraPoints, recalculated.finalScyraPoints)
        let delta = max(0, recalculated.finalScyraPoints - context.oldFinalScyraPoints)
        return DelayedMovementRewardResult(
            newRawMovementPoints: movementPoints,
            newFinalScyraPoints: finalPoints,
            newPreMultiplierTotal: recalculated.preMultiplierTotal,
            newArcBonusPoints: recalculated.arcBonusPoints,
            deltaScyraPoints: delta,
            pearlDelta: context.pearlEligible ? delta : 0,
            pearlsEarned: context.pearlEligible ? max(context.oldFinalScyraPoints, recalculated.pearlsEarned) : 0
        )
    }
}

struct FlowHealthSnapshot: Equatable, Sendable {
    let sessionID: UUID
    let healthEnabledAtStart: Bool
    let accessRequestedAtStart: Bool
    var status: FlowHealthSyncStatus
    var steps: Int64?
    var rawMovementPoints: Int64
    var finalMovementScyraContribution: Int64
    var finalMovementPearlContribution: Int64
    var firstCheckedAt: Date?
    var lastCheckedAt: Date?
    var capturedAt: Date?
    let expiresAt: Date?
    var checkCount: Int
    let flowStartTime: Date
    let flowEndTime: Date
    let activeIntervals: [FlowActiveInterval]
    let sourceLabel: String
    var updatedAfterSync: Bool

    func rebased(to sessionID: UUID) -> FlowHealthSnapshot {
        FlowHealthSnapshot(
            sessionID: sessionID,
            healthEnabledAtStart: healthEnabledAtStart,
            accessRequestedAtStart: accessRequestedAtStart,
            status: status,
            steps: steps,
            rawMovementPoints: rawMovementPoints,
            finalMovementScyraContribution: finalMovementScyraContribution,
            finalMovementPearlContribution: finalMovementPearlContribution,
            firstCheckedAt: firstCheckedAt,
            lastCheckedAt: lastCheckedAt,
            capturedAt: capturedAt,
            expiresAt: expiresAt,
            checkCount: checkCount,
            flowStartTime: flowStartTime,
            flowEndTime: flowEndTime,
            activeIntervals: activeIntervals,
            sourceLabel: sourceLabel,
            updatedAfterSync: updatedAfterSync
        )
    }
}

struct FlowMovementCompletion: Equatable, Sendable {
    let snapshot: FlowHealthSnapshot
    let rewardBreakdown: FlowRewardBreakdown

    func rebased(to sessionID: UUID) -> FlowMovementCompletion {
        FlowMovementCompletion(
            snapshot: snapshot.rebased(to: sessionID),
            rewardBreakdown: rewardBreakdown.rebased(to: sessionID)
        )
    }
}

struct CompletionMovementRead: Equatable, Sendable {
    let steps: Int64?
    let movementPoints: Int64
    let status: FlowHealthSyncStatus
    let checkedAt: Date?

    static let notEligible = CompletionMovementRead(
        steps: nil,
        movementPoints: 0,
        status: .notEligible,
        checkedAt: nil
    )
}
