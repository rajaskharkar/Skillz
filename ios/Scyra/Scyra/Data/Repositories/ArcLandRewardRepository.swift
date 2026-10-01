import Foundation
import SwiftData

extension SwiftDataFlowRepository {
    func fetchArcLandReward(arcID: UUID) throws -> ArcLandRewardJournal? {
        try arcLandRewardModel(arcID: arcID)?.journal
    }

    func reserveArcFlow(arcID: UUID?, flowInstanceID: UUID) throws {
        do {
            if let arcID, try fetchArcLandReward(arcID: arcID)?.finalizedAt != nil {
                throw ArcLandRewardError.finalizedArc
            }
            try clearArcLandReservationForCommit()
            if let arcID { context.insert(ArcLandFlowReservationModel(arcID: arcID, flowInstanceID: flowInstanceID)) }
            try context.save()
        } catch { context.rollback(); throw error }
    }

    func clearArcLandReservationForCommit() throws {
        try context.fetch(FetchDescriptor<ArcLandFlowReservationModel>()).forEach(context.delete)
    }

    /// Prospective only: called inside the Flow membership transaction, never
    /// by reward backfill or startup. Deleted history is re-counted at final close.
    func recordArcLandRewardForCommit(session: FlowSession) throws {
        guard let arcID = session.arcID else { return }
        let existing = try arcLandRewardModel(arcID: arcID)
        guard existing?.finalizedAt == nil else { throw ArcLandRewardError.finalizedArc }
        let sessions = try fetchSessions(arcID: arcID)
        guard let last = sessions.max(by: { $0.endTime < $1.endTime }) else { return }
        if let existing {
            existing.flowCount = sessions.count
            existing.lastFlowEndTime = last.endTime
            existing.finalSessionID = last.id
        } else {
            context.insert(ArcLandRewardModel(journal: .init(
                arcID: arcID, flowCount: sessions.count, lastFlowEndTime: last.endTime,
                finalSessionID: last.id, finalizedAt: nil
            )))
        }
    }

    @discardableResult
    func finalizeExpiredArcLandRewards(at date: Date) throws -> [UUID] {
        do {
            let reservation = try context.fetch(FetchDescriptor<ArcLandFlowReservationModel>()).first
            let runtime = try [fetchActiveArc(), fetchRecentlyEndedArc()].compactMap { $0 }
            // Also protect pre-V7 restored active Flows, which have no reservation yet.
            let activeFlow = try fetchActiveFlow()
            let legacyProtected = activeFlow.map { !$0.isIdleDraft || $0.firstStartedAt != nil } == true
                ? runtime.first?.id : nil
            let protectedID = reservation?.arcID ?? legacyProtected
            var finalized: [UUID] = []
            for row in try context.fetch(FetchDescriptor<ArcLandRewardModel>()) {
                guard ArcLandRewardPolicy.canFinalize(row.journal, now: date, protectedArcID: protectedID, runtimes: runtime) else { continue }
                let sessions = try fetchSessions(arcID: row.arcID)
                let last = sessions.max(by: { $0.endTime < $1.endTime })
                if let last, date.timeIntervalSince(last.endTime) * 1_000 <= Double(ArcRules.graceWindowMs) { continue }
                for reward in try ArcLandRewardPolicy.rewards(flowCount: sessions.count) {
                    let eventID = "arc_land:\(row.arcID.uuidString):\(reward.id)"
                    for index in 0..<reward.count {
                        let instanceID = "\(eventID):\(index)"
                        context.insert(ShellFindInstanceModel(instance: arcLandInstance(
                            id: instanceID, speciesID: reward.id, arcID: row.arcID, at: date
                        )))
                        _ = try recordCreatureDiscoveryIfNeeded(
                            speciesID: reward.id, sourceType: .arcEarned, instanceID: instanceID,
                            discoveredAt: date, timestampConfidence: "EXACT"
                        )
                    }
                    context.insert(ShellRewardEventModel(event: .init(
                        id: eventID, sourceSessionID: last?.id ?? row.finalSessionID, arcID: row.arcID,
                        type: .animalGranted, rewardID: reward.id, quantity: Int64(reward.count), occurredAt: date
                    )))
                }
                row.flowCount = sessions.count
                row.finalizedAt = date
                finalized.append(row.arcID)
            }
            if !finalized.isEmpty {
                try reconcileCreatureAchievements(at: date)
                try context.save()
            }
            return finalized
        } catch { context.rollback(); throw error }
    }

    private func arcLandRewardModel(arcID: UUID) throws -> ArcLandRewardModel? {
        try context.fetch(FetchDescriptor<ArcLandRewardModel>(predicate: #Predicate { $0.arcID == arcID })).first
    }
}

extension InMemoryFlowRepository {
    func fetchArcLandReward(arcID: UUID) -> ArcLandRewardJournal? { arcLandRewards[arcID] }

    func reserveArcFlow(arcID: UUID?, flowInstanceID: UUID) throws {
        if let arcID, arcLandRewards[arcID]?.finalizedAt != nil { throw ArcLandRewardError.finalizedArc }
        arcLandReservedArcID = arcID
    }

    func recordArcLandRewardForCommitInMemory(session: FlowSession) {
        guard let arcID = session.arcID else { return }
        let members = fetchSessions(arcID: arcID)
        guard let last = members.max(by: { $0.endTime < $1.endTime }) else { return }
        arcLandRewards[arcID] = .init(arcID: arcID, flowCount: members.count, lastFlowEndTime: last.endTime, finalSessionID: last.id, finalizedAt: nil)
    }

    @discardableResult
    func finalizeExpiredArcLandRewards(at date: Date) throws -> [UUID] {
        let runtime = [activeArc, recentlyEndedArc].compactMap { $0 }
        let legacyProtected = activeFlow.map { !$0.isIdleDraft || $0.firstStartedAt != nil } == true ? runtime.first?.id : nil
        var finalized: [UUID] = []
        for (arcID, var row) in arcLandRewards {
            guard ArcLandRewardPolicy.canFinalize(row, now: date, protectedArcID: arcLandReservedArcID ?? legacyProtected, runtimes: runtime) else { continue }
            let members = fetchSessions(arcID: arcID)
            let last = members.max(by: { $0.endTime < $1.endTime })
            if let last, date.timeIntervalSince(last.endTime) * 1_000 <= Double(ArcRules.graceWindowMs) { continue }
            for reward in try ArcLandRewardPolicy.rewards(flowCount: members.count) {
                let eventID = "arc_land:\(arcID.uuidString):\(reward.id)"
                for index in 0..<reward.count {
                    let id = "\(eventID):\(index)"
                    shellFindInstances.append(arcLandInstance(id: id, speciesID: reward.id, arcID: arcID, at: date))
                    shellCreatureStatuses[id] = .active
                    recordCreatureDiscoveryInMemoryIfNeeded(speciesID: reward.id, instanceID: id, discoveredAt: date)
                }
                shellRewardEvents.append(.init(id: eventID, sourceSessionID: last?.id ?? row.finalSessionID, arcID: arcID, type: .animalGranted, rewardID: reward.id, quantity: Int64(reward.count), occurredAt: date))
            }
            row.flowCount = members.count
            row.finalizedAt = date
            arcLandRewards[arcID] = row
            finalized.append(arcID)
        }
        return finalized
    }
}

private func arcLandInstance(id: String, speciesID: String, arcID: UUID, at date: Date) -> ShellFindInstance {
    .init(id: id, findID: speciesID, acquiredAt: date, sourceType: "arc", sourceID: arcID.uuidString,
          currentUpgradeStageID: nil, isNew: true, isArchivedInChest: true, viewedAt: nil,
          animalLevel: 1, lastActivityAt: date)
}
