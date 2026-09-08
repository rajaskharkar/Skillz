import Foundation

enum IdeaGroveItemType: String, Sendable {
    case rawPulse
    case idea
    case insight
    case completedIdea
}

enum IdeaGroveSort: String, CaseIterable, Identifiable, Sendable {
    case recents = "Recents"
    case newest = "Newest"
    case oldest = "Oldest"
    case mostTime = "Most time"
    case leastTime = "Least time"

    var id: Self { self }
}

struct IdeaGroveFlow: Identifiable, Equatable, Sendable {
    var id: UUID { sessionID }

    let sessionID: UUID
    let title: String
    let description: String
    let journeyName: String?
    let durationMs: Int64
    let startTime: Date
    let endTime: Date
}

struct IdeaGroveItem: Identifiable, Equatable, Sendable {
    var id: UUID { pulseID }

    let pulseID: UUID
    let type: IdeaGroveItemType
    let title: String
    let description: String
    let journeyName: String?
    let createdAt: Date
    let updatedAt: Date
    let groveStatus: PulseGroveStatus
    let groveStatusChangedAt: Date?
    let flowCount: Int
    let totalFlowDurationMs: Int64
    let lastWorkedAt: Date?
    let flows: [IdeaGroveFlow]
    let wasCapturedDuringFlow: Bool
}

struct PulseLaunchContext: Equatable, Sendable {
    let pulseID: UUID
    let title: String
    let description: String
    let journeyName: String?
}

struct PendingIdeaContinuation: Identifiable, Equatable, Sendable {
    var id: UUID { pulseID }

    let pulseID: UUID
    let title: String
    let journeyName: String?
}

enum IdeaGroveDurationFormatter {
    static func compact(_ durationMs: Int64) -> String {
        let totalSeconds = max(0, durationMs / 1_000)
        let hours = totalSeconds / 3_600
        let minutes = (totalSeconds % 3_600) / 60
        let seconds = totalSeconds % 60
        var parts: [String] = []
        if hours > 0 { parts.append("\(hours)h") }
        if minutes > 0 || hours > 0 { parts.append("\(minutes)m") }
        if seconds > 0 || parts.isEmpty { parts.append("\(seconds)s") }
        return parts.joined(separator: " ")
    }

    static func spoken(_ durationMs: Int64) -> String {
        let totalSeconds = max(0, durationMs / 1_000)
        let hours = totalSeconds / 3_600
        let minutes = (totalSeconds % 3_600) / 60
        let seconds = totalSeconds % 60
        var parts: [String] = []
        if hours > 0 { parts.append("\(hours) \(hours == 1 ? "hour" : "hours")") }
        if minutes > 0 { parts.append("\(minutes) \(minutes == 1 ? "minute" : "minutes")") }
        if seconds > 0 || parts.isEmpty {
            parts.append("\(seconds) \(seconds == 1 ? "second" : "seconds")")
        }
        return parts.joined(separator: " ")
    }
}

extension ActiveFlowSnapshot {
    var isMeaningfulActiveFlow: Bool {
        isRunning
            || isInFlowMode
            || accumulatedDurationMs > 0
            || segmentStartedAt != nil
            || firstStartedAt != nil
    }

    var isAbandonedPulseOriginDraft: Bool {
        originPulseID != nil && !isMeaningfulActiveFlow
    }
}
