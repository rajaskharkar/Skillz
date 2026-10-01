import Foundation

struct ArcLandRewardJournal: Equatable, Sendable {
    let arcID: UUID
    var flowCount: Int
    var lastFlowEndTime: Date
    var finalSessionID: UUID
    var finalizedAt: Date?
}

enum ArcLandRewardError: LocalizedError {
    case finalizedArc, invalidDepth
    var errorDescription: String? {
        switch self {
        case .finalizedArc: "This Arc has already closed. Start a new Arc to continue."
        case .invalidDepth: "Arc Flow count cannot be negative."
        }
    }
}

enum ArcLandRewardPolicy {
    /// One flagship for the remainder, not every lower milestone. No per-Tiger
    /// allocation: even very large Arc depths yield at most two reward entries.
    static func rewards(flowCount: Int) throws -> [ShellRewardCount] {
        guard flowCount >= 0 else { throw ArcLandRewardError.invalidDepth }
        var rewards: [ShellRewardCount] = []
        if flowCount >= 15 { rewards.append(.init(id: "creature_tiger", count: flowCount / 15)) }
        let remainder = [1: "creature_chicken", 2: "creature_deer", 3: "creature_camel", 4: "creature_moose"]
        if let id = remainder[(flowCount % 15) / 3] { rewards.append(.init(id: id, count: 1)) }
        return rewards
    }

    static func canFinalize(_ journal: ArcLandRewardJournal, now: Date, protectedArcID: UUID?, runtimes: [ArcRuntimeState]) -> Bool {
        journal.finalizedAt == nil && journal.arcID != protectedArcID &&
            now.timeIntervalSince(journal.lastFlowEndTime) * 1_000 > Double(ArcRules.graceWindowMs) &&
            !runtimes.contains { $0.id == journal.arcID && ArcContinuationResolver.isWithinContinuationWindow($0, flowStartTime: now) }
    }
}
