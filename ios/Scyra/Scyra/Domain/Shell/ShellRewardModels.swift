import Foundation

enum ShellRewardEventType: String, Codable, CaseIterable, Sendable {
    case pearlsCarried = "PEARLS_CARRIED"
    case stillwaterAdded = "STILLWATER_ADDED"
    case animalGranted = "ANIMAL_GRANTED"
    case objectGranted = "OBJECT_GRANTED"
    case trinketGranted = "TRINKET_GRANTED"
    case discoveryRecorded = "DISCOVERY_RECORDED"
    case badgeUpdated = "BADGE_UPDATED"
    case unknown = "UNKNOWN"
}

struct PearlLedgerEntry: Identifiable, Equatable, Sendable {
    let id: String
    let delta: Int
    let reason: String
    let sourceType: String
    let sourceID: String?
    let createdAt: Date
    let note: String?
}

struct StillwaterLedgerEntry: Identifiable, Equatable, Sendable {
    let id: String
    let units: Int64
    let sourceType: String
    let sourceID: String?
    let createdAt: Date
}

struct ShellRewardEvent: Identifiable, Equatable, Sendable {
    let id: String
    let sourceSessionID: UUID
    let arcID: UUID?
    let type: ShellRewardEventType
    let rewardID: String?
    let quantity: Int64
    let occurredAt: Date
}

struct ShellRewardCount: Identifiable, Equatable, Sendable {
    let id: String
    let count: Int
}

struct ShellRewardSummary: Equatable, Sendable {
    var animals: [ShellRewardCount] = []
    var objects: [ShellRewardCount] = []
    var trinkets: [ShellRewardCount] = []
    var badges: [ShellRewardCount] = []
    var discoveries: [ShellRewardCount] = []
    var unknownRewards: [ShellRewardCount] = []
    var pearlsCarried = 0
    var stillwaterAdded: Int64 = 0

    static let empty = ShellRewardSummary()

    var hasVisibleShellRewards: Bool {
        !animals.isEmpty || !objects.isEmpty || !trinkets.isEmpty
            || !badges.isEmpty || !discoveries.isEmpty || !unknownRewards.isEmpty
            || stillwaterAdded > 0
    }
}

struct ShellSessionReward: Equatable, Sendable {
    var pearlsEarned = 0
    var stillwaterUnits: Int64 = 0
    var grantedFindIDs: [String] = []
    var badgeIDs: [String] = []
    var discoveryIDs: [String] = []

    static let empty = ShellSessionReward()
}

struct ShellFindGrant: Identifiable, Equatable, Sendable {
    let id: String
    let findID: String
    let sourceSessionID: UUID
    let acquiredAt: Date
}

struct ShellBadge: Identifiable, Equatable, Sendable {
    var id: String { badgeID }
    let badgeID: String
    let count: Int
    let firstEarnedAt: Date
    let lastEarnedAt: Date
    let isNew: Bool
}

enum ShellRewardCatalog {
    static let focusMinnow = "focus_minnow"
    static let focusSeahorse = "focus_seahorse"
    static let focusManta = "focus_manta"
    static let focusWhale = "focus_whale"

    static let badgeFlow10 = "badge_flow_10_min"
    static let badgeFlow30 = "badge_flow_30_min"
    static let badgeFlow60 = "badge_flow_60_min"
    static let badgeFlow120 = "badge_flow_120_min"

    struct Animal: Equatable, Sendable {
        let id: String
        let title: String
        let depth: String
        let reason: String
        let systemImage: String
    }

    static let animals: [String: Animal] = [
        focusMinnow: Animal(
            id: focusMinnow,
            title: "Minnow",
            depth: "Sunlit Reef",
            reason: "From a regular Flow lasting 10 minutes or more.",
            systemImage: "fish.fill"
        ),
        focusSeahorse: Animal(
            id: focusSeahorse,
            title: "Seahorse",
            depth: "Deeper Reef",
            reason: "From a regular Flow lasting 30 minutes or more.",
            systemImage: "fish.fill"
        ),
        focusManta: Animal(
            id: focusManta,
            title: "Manta",
            depth: "Open Blue",
            reason: "From a regular Flow lasting 1 hour or more.",
            systemImage: "water.waves"
        ),
        focusWhale: Animal(
            id: focusWhale,
            title: "Whale",
            depth: "Great Blue",
            reason: "From a regular Flow lasting 2 hours or more.",
            systemImage: "water.waves"
        )
    ]

    static let badgeTitles: [String: String] = [
        badgeFlow10: "10-minute Flow",
        badgeFlow30: "30-minute Flow",
        badgeFlow60: "60-minute Flow",
        badgeFlow120: "2-hour Flow"
    ]
}

enum ShellRewardPolicy {
    static func milestoneFinds(minutes: Int, isSoftFlow: Bool = false) -> [String] {
        guard !isSoftFlow, minutes >= 10 else { return [] }
        var remaining = minutes
        var rewards: [String] = []

        func take(_ chunk: Int, _ id: String) {
            let count = remaining / chunk
            if count > 0 { rewards.append(contentsOf: repeatElement(id, count: count)) }
            remaining %= chunk
        }

        take(120, ShellRewardCatalog.focusWhale)
        take(60, ShellRewardCatalog.focusManta)
        take(30, ShellRewardCatalog.focusSeahorse)
        take(10, ShellRewardCatalog.focusMinnow)
        rewards.append(contentsOf: repeatElement(
            ShellRewardCatalog.focusWhale,
            count: minutes / 150
        ))
        return rewards
    }

    static func badgeIDs(minutes: Int, isSoftFlow: Bool = false) -> [String] {
        guard !isSoftFlow else { return [] }
        return [
            (10, ShellRewardCatalog.badgeFlow10),
            (30, ShellRewardCatalog.badgeFlow30),
            (60, ShellRewardCatalog.badgeFlow60),
            (120, ShellRewardCatalog.badgeFlow120)
        ].compactMap { minutes >= $0.0 ? $0.1 : nil }
    }

    static func reward(for session: FlowSession) -> ShellSessionReward {
        if session.isSoftMode {
            return ShellSessionReward(
                stillwaterUnits: max(0, session.durationMs / 1_000)
            )
        }
        let minutes = max(0, Int(session.durationMs / 60_000))
        return ShellSessionReward(
            pearlsEarned: max(0, session.scyraPoints),
            grantedFindIDs: milestoneFinds(minutes: minutes),
            badgeIDs: badgeIDs(minutes: minutes)
        )
    }
}

enum ShellRewardEventRecorder {
    static func events(
        for session: FlowSession,
        reward: ShellSessionReward,
        occurredAt: Date
    ) -> [ShellRewardEvent] {
        var events: [ShellRewardEvent] = []

        func add(_ type: ShellRewardEventType, rewardID: String?, quantity: Int64) {
            guard quantity > 0 else { return }
            let rewardKey = rewardID ?? "_"
            events.append(ShellRewardEvent(
                id: "\(session.id.uuidString):\(type.rawValue):\(rewardKey)",
                sourceSessionID: session.id,
                arcID: session.arcID,
                type: type,
                rewardID: rewardID,
                quantity: quantity,
                occurredAt: occurredAt
            ))
        }

        if session.isSoftMode {
            add(.stillwaterAdded, rewardID: nil, quantity: reward.stillwaterUnits)
            return events
        }

        add(.pearlsCarried, rewardID: nil, quantity: Int64(reward.pearlsEarned))
        Dictionary(grouping: reward.grantedFindIDs, by: { $0 }).forEach { id, values in
            if ShellRewardCatalog.animals[id] != nil {
                add(.animalGranted, rewardID: id, quantity: Int64(values.count))
            }
        }
        Dictionary(grouping: reward.discoveryIDs, by: { $0 }).forEach { id, values in
            add(.discoveryRecorded, rewardID: id, quantity: Int64(values.count))
        }
        Dictionary(grouping: reward.badgeIDs, by: { $0 }).forEach { id, values in
            add(.badgeUpdated, rewardID: id, quantity: Int64(values.count))
        }
        return events
    }
}

enum ShellRewardEventAggregator {
    static func aggregate(_ events: [ShellRewardEvent]) -> ShellRewardSummary {
        func counts(for type: ShellRewardEventType) -> [ShellRewardCount] {
            Dictionary(grouping: events.filter { $0.type == type && $0.rewardID != nil }) {
                $0.rewardID ?? ""
            }
            .map { ShellRewardCount(id: $0.key, count: Int($0.value.reduce(0) { $0 + $1.quantity })) }
            .filter { $0.count > 0 }
            .sorted { $0.id < $1.id }
        }

        let unknown = Dictionary(grouping: events.filter {
            $0.type == .unknown || ($0.rewardID == nil && $0.type != .pearlsCarried && $0.type != .stillwaterAdded)
        }) { $0.rewardID ?? $0.type.rawValue }
            .map { ShellRewardCount(id: $0.key, count: Int($0.value.reduce(0) { $0 + $1.quantity })) }
            .filter { $0.count > 0 }
            .sorted { $0.id < $1.id }

        return ShellRewardSummary(
            animals: counts(for: .animalGranted),
            objects: counts(for: .objectGranted),
            trinkets: counts(for: .trinketGranted),
            badges: counts(for: .badgeUpdated),
            discoveries: counts(for: .discoveryRecorded),
            unknownRewards: unknown,
            pearlsCarried: Int(events.filter { $0.type == .pearlsCarried }.reduce(0) { $0 + $1.quantity }),
            stillwaterAdded: events.filter { $0.type == .stillwaterAdded }.reduce(0) { $0 + $1.quantity }
        )
    }
}

enum MovementPearlDeltaKey {
    static func reason(sessionID: UUID, movementPoints: Int64, finalScyraPoints: Int64) -> String {
        "movement_bonus_delta_session_\(sessionID.uuidString)_movement_\(movementPoints)_final_\(finalScyraPoints)"
    }
}
