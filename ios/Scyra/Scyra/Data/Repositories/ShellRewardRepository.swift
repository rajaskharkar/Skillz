import Foundation
import SwiftData

@MainActor
protocol ShellRewardRepository: AnyObject {
    func fetchPearlBalance() throws -> Int
    func fetchStillwaterBalance() throws -> Int64
    func fetchStillwaterLifetimeTotal() throws -> Int64
    func fetchPearlLedger() throws -> [PearlLedgerEntry]
    func fetchStillwaterLedger() throws -> [StillwaterLedgerEntry]
    func fetchShellRewardEvents(sessionID: UUID) throws -> [ShellRewardEvent]
    func fetchShellRewardEvents(arcID: UUID) throws -> [ShellRewardEvent]
    func fetchSessionShellReward(sessionID: UUID) throws -> ShellSessionReward
    func fetchArcShellRewardSummary(arcID: UUID) throws -> ShellRewardSummary
    func fetchShellFindGrants() throws -> [ShellFindGrant]
    func fetchShellBadges() throws -> [ShellBadge]
}

extension SwiftDataFlowRepository {
    func fetchPearlBalance() throws -> Int {
        try context.fetch(FetchDescriptor<PearlLedgerModel>()).reduce(0) { $0 + $1.delta }
    }

    func fetchStillwaterBalance() throws -> Int64 {
        try context.fetch(FetchDescriptor<StillwaterLedgerModel>()).reduce(0) { $0 + $1.units }
    }

    func fetchStillwaterLifetimeTotal() throws -> Int64 {
        try context.fetch(FetchDescriptor<StillwaterLedgerModel>())
            .filter { $0.units > 0 }
            .reduce(0) { $0 + $1.units }
    }

    func fetchPearlLedger() throws -> [PearlLedgerEntry] {
        try context.fetch(FetchDescriptor<PearlLedgerModel>())
            .map(Self.pearlEntry(from:))
            .sorted { $0.createdAt > $1.createdAt }
    }

    func fetchStillwaterLedger() throws -> [StillwaterLedgerEntry] {
        try context.fetch(FetchDescriptor<StillwaterLedgerModel>())
            .map(Self.stillwaterEntry(from:))
            .sorted { $0.createdAt > $1.createdAt }
    }

    func fetchShellRewardEvents(sessionID: UUID) throws -> [ShellRewardEvent] {
        try allShellRewardEvents()
            .filter { $0.sourceSessionID == sessionID }
            .sorted { $0.occurredAt < $1.occurredAt }
    }

    func fetchShellRewardEvents(arcID: UUID) throws -> [ShellRewardEvent] {
        try allShellRewardEvents()
            .filter { $0.arcID == arcID }
            .sorted { $0.occurredAt < $1.occurredAt }
    }

    func fetchSessionShellReward(sessionID: UUID) throws -> ShellSessionReward {
        let events = try fetchShellRewardEvents(sessionID: sessionID)
        let summary = ShellRewardEventAggregator.aggregate(events)
        return ShellSessionReward(
            pearlsEarned: summary.pearlsCarried,
            stillwaterUnits: summary.stillwaterAdded,
            grantedFindIDs: expanded(summary.animals),
            badgeIDs: expanded(summary.badges),
            discoveryIDs: expanded(summary.discoveries)
        )
    }

    func fetchArcShellRewardSummary(arcID: UUID) throws -> ShellRewardSummary {
        ShellRewardEventAggregator.aggregate(try fetchShellRewardEvents(arcID: arcID))
    }

    func fetchShellFindGrants() throws -> [ShellFindGrant] {
        try context.fetch(FetchDescriptor<ShellFindGrantModel>())
            .map { ShellFindGrant(id: $0.id, findID: $0.findID, sourceSessionID: $0.sourceSessionID, acquiredAt: $0.acquiredAt) }
            .sorted { $0.acquiredAt > $1.acquiredAt }
    }

    func fetchShellBadges() throws -> [ShellBadge] {
        try context.fetch(FetchDescriptor<ShellBadgeModel>())
            .map {
                ShellBadge(
                    badgeID: $0.badgeID,
                    count: $0.count,
                    firstEarnedAt: $0.firstEarnedAt,
                    lastEarnedAt: $0.lastEarnedAt,
                    isNew: $0.isNew
                )
            }
            .sorted { $0.lastEarnedAt > $1.lastEarnedAt }
    }

    /// Adds the session's complete Shell reward set without saving. The caller owns
    /// the surrounding SwiftData transaction so Flow, Arc, Chronicle, Movement,
    /// Idea Grove, and Shell rewards either all commit or all roll back together.
    @discardableResult
    func insertShellRewardsForCommit(session: FlowSession) throws -> ShellSessionReward {
        let reward = ShellRewardPolicy.reward(for: session)
        let sourceID = session.id.uuidString
        let existingEvents = try fetchShellRewardEvents(sessionID: session.id)
        let existingEventIDs = Set(existingEvents.map(\.id))

        if reward.pearlsEarned > 0 {
            let reason = "flow_reward"
            let id = ledgerID(sourceType: "session", sourceID: sourceID, reason: reason)
            if try !hasPearlEntry(id: id) {
                context.insert(PearlLedgerModel(entry: PearlLedgerEntry(
                    id: id,
                    delta: reward.pearlsEarned,
                    reason: reason,
                    sourceType: "session",
                    sourceID: sourceID,
                    createdAt: session.createdAt,
                    note: nil
                )))
            }
        }

        if reward.stillwaterUnits > 0 {
            let id = stillwaterID(sourceType: "session", sourceID: sourceID)
            if try !hasStillwaterEntry(id: id) {
                context.insert(StillwaterLedgerModel(entry: StillwaterLedgerEntry(
                    id: id,
                    units: reward.stillwaterUnits,
                    sourceType: "session",
                    sourceID: sourceID,
                    createdAt: session.createdAt
                )))
            }
        }

        for (index, findID) in reward.grantedFindIDs.enumerated() {
            let id = "\(sourceID):\(findID):\(index)"
            if try !hasFindGrant(id: id) {
                context.insert(ShellFindGrantModel(
                    id: id,
                    findID: findID,
                    sourceSessionID: session.id,
                    acquiredAt: session.createdAt
                ))
                try materializeShellFindReward(
                    id: id,
                    findID: findID,
                    sourceSessionID: session.id,
                    acquiredAt: session.createdAt
                )
            }
        }

        let events = ShellRewardEventRecorder.events(for: session, reward: reward, occurredAt: session.createdAt)
        for event in events where !existingEventIDs.contains(event.id) {
            if event.type == .badgeUpdated, let badgeID = event.rewardID {
                try incrementBadge(badgeID, by: Int(event.quantity), earnedAt: event.occurredAt)
            }
            context.insert(ShellRewardEventModel(event: event))
        }
        return reward
    }

    func insertMovementPearlDeltaForRefresh(
        sessionID: UUID,
        oldBreakdown: FlowRewardBreakdown,
        newBreakdown: FlowRewardBreakdown,
        createdAt: Date = Date()
    ) throws {
        guard oldBreakdown.pearlEligible, newBreakdown.pearlEligible else { return }
        let delta = max(0, newBreakdown.pearlsEarned - oldBreakdown.pearlsEarned)
        guard delta > 0 else { return }
        let reason = MovementPearlDeltaKey.reason(
            sessionID: sessionID,
            movementPoints: newBreakdown.movementPoints,
            finalScyraPoints: newBreakdown.finalScyraPoints
        )
        let sourceID = sessionID.uuidString
        let id = ledgerID(sourceType: "session", sourceID: sourceID, reason: reason)
        guard try !hasPearlEntry(id: id) else { return }
        context.insert(PearlLedgerModel(entry: PearlLedgerEntry(
            id: id,
            delta: Int(delta),
            reason: reason,
            sourceType: "session",
            sourceID: sourceID,
            createdAt: createdAt,
            note: "Movement Bonus delayed sync"
        )))
    }

    private func allShellRewardEvents() throws -> [ShellRewardEvent] {
        try context.fetch(FetchDescriptor<ShellRewardEventModel>()).map { model in
            ShellRewardEvent(
                id: model.id,
                sourceSessionID: model.sourceSessionID,
                arcID: model.arcID,
                type: ShellRewardEventType(rawValue: model.typeRawValue) ?? .unknown,
                rewardID: model.rewardID,
                quantity: model.quantity,
                occurredAt: model.occurredAt
            )
        }
    }

    private func incrementBadge(_ badgeID: String, by count: Int, earnedAt: Date) throws {
        let models = try context.fetch(FetchDescriptor<ShellBadgeModel>())
        if let model = models.first(where: { $0.badgeID == badgeID }) {
            model.count += count
            model.lastEarnedAt = earnedAt
            model.isNew = true
        } else {
            context.insert(ShellBadgeModel(badgeID: badgeID, count: count, earnedAt: earnedAt))
        }
    }

    private func hasPearlEntry(id: String) throws -> Bool {
        try context.fetch(FetchDescriptor<PearlLedgerModel>()).contains { $0.id == id }
    }

    private func hasStillwaterEntry(id: String) throws -> Bool {
        try context.fetch(FetchDescriptor<StillwaterLedgerModel>()).contains { $0.id == id }
    }

    private func hasFindGrant(id: String) throws -> Bool {
        try context.fetch(FetchDescriptor<ShellFindGrantModel>()).contains { $0.id == id }
    }

    private func ledgerID(sourceType: String, sourceID: String?, reason: String) -> String {
        "\(sourceType):\(sourceID ?? "_"):\(reason)"
    }

    private func stillwaterID(sourceType: String, sourceID: String?) -> String {
        "\(sourceType):\(sourceID ?? "_")"
    }

    private static func pearlEntry(from model: PearlLedgerModel) -> PearlLedgerEntry {
        PearlLedgerEntry(
            id: model.id,
            delta: model.delta,
            reason: model.reason,
            sourceType: model.sourceType,
            sourceID: model.sourceID,
            createdAt: model.createdAt,
            note: model.note
        )
    }

    private static func stillwaterEntry(from model: StillwaterLedgerModel) -> StillwaterLedgerEntry {
        StillwaterLedgerEntry(
            id: model.id,
            units: model.units,
            sourceType: model.sourceType,
            sourceID: model.sourceID,
            createdAt: model.createdAt
        )
    }

    private func expanded(_ counts: [ShellRewardCount]) -> [String] {
        counts.flatMap { repeatElement($0.id, count: $0.count) }
    }
}

extension InMemoryFlowRepository {
    func fetchPearlBalance() -> Int { pearlLedger.reduce(0) { $0 + $1.delta } }
    func fetchStillwaterBalance() -> Int64 { stillwaterLedger.reduce(0) { $0 + $1.units } }
    func fetchStillwaterLifetimeTotal() -> Int64 {
        stillwaterLedger.filter { $0.units > 0 }.reduce(0) { $0 + $1.units }
    }
    func fetchPearlLedger() -> [PearlLedgerEntry] { pearlLedger.sorted { $0.createdAt > $1.createdAt } }
    func fetchStillwaterLedger() -> [StillwaterLedgerEntry] { stillwaterLedger.sorted { $0.createdAt > $1.createdAt } }
    func fetchShellRewardEvents(sessionID: UUID) -> [ShellRewardEvent] {
        shellRewardEvents.filter { $0.sourceSessionID == sessionID }.sorted { $0.occurredAt < $1.occurredAt }
    }
    func fetchShellRewardEvents(arcID: UUID) -> [ShellRewardEvent] {
        shellRewardEvents.filter { $0.arcID == arcID }.sorted { $0.occurredAt < $1.occurredAt }
    }
    func fetchSessionShellReward(sessionID: UUID) -> ShellSessionReward {
        let summary = ShellRewardEventAggregator.aggregate(fetchShellRewardEvents(sessionID: sessionID))
        return ShellSessionReward(
            pearlsEarned: summary.pearlsCarried,
            stillwaterUnits: summary.stillwaterAdded,
            grantedFindIDs: expandedShellCounts(summary.animals),
            badgeIDs: expandedShellCounts(summary.badges),
            discoveryIDs: expandedShellCounts(summary.discoveries)
        )
    }
    func fetchArcShellRewardSummary(arcID: UUID) -> ShellRewardSummary {
        ShellRewardEventAggregator.aggregate(fetchShellRewardEvents(arcID: arcID))
    }
    func fetchShellFindGrants() -> [ShellFindGrant] { shellFindGrants.sorted { $0.acquiredAt > $1.acquiredAt } }
    func fetchShellBadges() -> [ShellBadge] { shellBadges.values.sorted { $0.lastEarnedAt > $1.lastEarnedAt } }

    func insertShellRewardsForCommitInMemory(session: FlowSession) {
        let reward = ShellRewardPolicy.reward(for: session)
        let sourceID = session.id.uuidString
        let existingEventIDs = Set(shellRewardEvents.map(\.id))

        if reward.pearlsEarned > 0 {
            let id = "session:\(sourceID):flow_reward"
            if !pearlLedger.contains(where: { $0.id == id }) {
                pearlLedger.append(PearlLedgerEntry(
                    id: id,
                    delta: reward.pearlsEarned,
                    reason: "flow_reward",
                    sourceType: "session",
                    sourceID: sourceID,
                    createdAt: session.createdAt,
                    note: nil
                ))
            }
        }
        if reward.stillwaterUnits > 0 {
            let id = "session:\(sourceID)"
            if !stillwaterLedger.contains(where: { $0.id == id }) {
                stillwaterLedger.append(StillwaterLedgerEntry(
                    id: id,
                    units: reward.stillwaterUnits,
                    sourceType: "session",
                    sourceID: sourceID,
                    createdAt: session.createdAt
                ))
            }
        }
        for (index, findID) in reward.grantedFindIDs.enumerated() {
            let id = "\(sourceID):\(findID):\(index)"
            if !shellFindGrants.contains(where: { $0.id == id }) {
                shellFindGrants.append(ShellFindGrant(
                    id: id,
                    findID: findID,
                    sourceSessionID: session.id,
                    acquiredAt: session.createdAt
                ))
                materializeShellFindRewardInMemory(
                    id: id,
                    findID: findID,
                    sourceSessionID: session.id,
                    acquiredAt: session.createdAt
                )
            }
        }
        let newEvents = ShellRewardEventRecorder.events(for: session, reward: reward, occurredAt: session.createdAt)
            .filter { !existingEventIDs.contains($0.id) }
        for event in newEvents where event.type == .badgeUpdated {
            guard let badgeID = event.rewardID else { continue }
            if let badge = shellBadges[badgeID] {
                shellBadges[badgeID] = ShellBadge(
                    badgeID: badgeID,
                    count: badge.count + Int(event.quantity),
                    firstEarnedAt: badge.firstEarnedAt,
                    lastEarnedAt: event.occurredAt,
                    isNew: true
                )
            } else {
                shellBadges[badgeID] = ShellBadge(
                    badgeID: badgeID,
                    count: Int(event.quantity),
                    firstEarnedAt: event.occurredAt,
                    lastEarnedAt: event.occurredAt,
                    isNew: true
                )
            }
        }
        shellRewardEvents.append(contentsOf: newEvents)
    }

    func insertMovementPearlDeltaInMemory(
        sessionID: UUID,
        oldBreakdown: FlowRewardBreakdown,
        newBreakdown: FlowRewardBreakdown,
        createdAt: Date = Date()
    ) {
        guard oldBreakdown.pearlEligible, newBreakdown.pearlEligible else { return }
        let delta = max(0, newBreakdown.pearlsEarned - oldBreakdown.pearlsEarned)
        guard delta > 0 else { return }
        let reason = MovementPearlDeltaKey.reason(
            sessionID: sessionID,
            movementPoints: newBreakdown.movementPoints,
            finalScyraPoints: newBreakdown.finalScyraPoints
        )
        let id = "session:\(sessionID.uuidString):\(reason)"
        guard !pearlLedger.contains(where: { $0.id == id }) else { return }
        pearlLedger.append(PearlLedgerEntry(
            id: id,
            delta: Int(delta),
            reason: reason,
            sourceType: "session",
            sourceID: sessionID.uuidString,
            createdAt: createdAt,
            note: "Movement Bonus delayed sync"
        ))
    }

    private func expandedShellCounts(_ counts: [ShellRewardCount]) -> [String] {
        counts.flatMap { repeatElement($0.id, count: $0.count) }
    }
}
