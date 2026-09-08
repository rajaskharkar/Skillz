import Foundation
import SwiftData

extension SwiftDataFlowRepository {
    func fetchIdeaGroveItems() throws -> [IdeaGroveItem] {
        let pulses = try context.fetch(FetchDescriptor<PulseModel>())
        let links = try context.fetch(FetchDescriptor<PulseFlowLinkModel>())
        let sessions = try context.fetch(FetchDescriptor<FlowSessionModel>())
        let chronicles = try context.fetch(FetchDescriptor<ChronicleModel>())
        let sessionsByID = Dictionary(uniqueKeysWithValues: sessions.map { ($0.id, $0) })
        let linksByPulse = Dictionary(grouping: links, by: \PulseFlowLinkModel.pulseID)
        let excerpts = Dictionary(uniqueKeysWithValues: chronicles.map { ($0.ownerIdentity, Self.excerpt($0)) })

        return pulses.map { pulse in
            let flows = linksByPulse[pulse.id, default: []]
                .compactMap { link -> IdeaGroveFlow? in
                    guard let session = sessionsByID[link.sessionID] else { return nil }
                    return IdeaGroveFlow(
                        sessionID: session.id,
                        title: session.title,
                        description: excerpts[ChronicleOwner.session(session.id).identity] ?? "",
                        journeyName: session.journey?.name ?? session.journeyNameSnapshot,
                        durationMs: session.durationMs,
                        startTime: session.startTime,
                        endTime: session.endTime
                    )
                }
                .sorted { $0.endTime > $1.endTime }
            let storedStatus = PulseGroveStatus(rawValue: pulse.groveStatusRawValue) ?? .alive
            let effectiveStatus: PulseGroveStatus = storedStatus == .completed && flows.isEmpty
                ? .alive
                : storedStatus
            let type: IdeaGroveItemType = switch (effectiveStatus, flows.isEmpty) {
            case (.insight, _): .insight
            case (.completed, _): .completedIdea
            case (_, false): .idea
            case (_, true): .rawPulse
            }

            return IdeaGroveItem(
                pulseID: pulse.id,
                type: type,
                title: pulse.title,
                description: excerpts[ChronicleOwner.pulse(pulse.id).identity] ?? "",
                journeyName: pulse.journey?.name ?? pulse.journeyNameSnapshot,
                createdAt: pulse.createdAt,
                updatedAt: pulse.updatedAt,
                groveStatus: effectiveStatus,
                groveStatusChangedAt: pulse.groveStatusChangedAt,
                flowCount: flows.count,
                totalFlowDurationMs: flows.reduce(0) { $0 + $1.durationMs },
                lastWorkedAt: flows.map(\.endTime).max(),
                flows: flows,
                wasCapturedDuringFlow: pulse.parentSessionID != nil || pulse.parentFlowInstanceID != nil
            )
        }
    }

    func fetchPulseFlowLinks(pulseID: UUID?) throws -> [PulseFlowLink] {
        try context.fetch(FetchDescriptor<PulseFlowLinkModel>())
            .filter { pulseID == nil || $0.pulseID == pulseID }
            .sorted { $0.linkedAt > $1.linkedAt }
            .map { PulseFlowLink(id: $0.id, pulseID: $0.pulseID, sessionID: $0.sessionID, linkedAt: $0.linkedAt) }
    }

    func markPulseAsInsight(id: UUID, changedAt: Date) throws {
        guard try fetchPulseFlowLinks(pulseID: id).isEmpty,
              let pulse = try grovePulseModel(id: id) else { return }
        updateGroveStatus(pulse, to: .insight, changedAt: changedAt)
        try context.save()
    }

    func markPulseCompleted(id: UUID, changedAt: Date) throws {
        guard try !fetchPulseFlowLinks(pulseID: id).isEmpty,
              let pulse = try grovePulseModel(id: id) else { return }
        updateGroveStatus(pulse, to: .completed, changedAt: changedAt)
        try context.save()
    }

    func revivePulse(id: UUID, changedAt: Date) throws {
        guard let pulse = try grovePulseModel(id: id) else { return }
        updateGroveStatus(pulse, to: .alive, changedAt: changedAt)
        try context.save()
    }

    func repairCompletedPulsesWithoutFlows(changedAt: Date) throws {
        let linkedPulseIDs = Set(try context.fetch(FetchDescriptor<PulseFlowLinkModel>()).map(\.pulseID))
        var repaired = false
        for pulse in try context.fetch(FetchDescriptor<PulseModel>())
        where pulse.groveStatusRawValue == PulseGroveStatus.completed.rawValue
            && !linkedPulseIDs.contains(pulse.id) {
            updateGroveStatus(pulse, to: .alive, changedAt: changedAt)
            repaired = true
        }
        if repaired { try context.save() }
    }

    func fetchPulseLaunchContext(id: UUID) throws -> PulseLaunchContext? {
        guard let pulse = try grovePulseModel(id: id) else { return nil }
        let chronicle = try groveChronicleModel(owner: .pulse(id))
        return PulseLaunchContext(
            pulseID: id,
            title: pulse.title,
            description: chronicle.map(Self.excerpt).flatMap { $0.isEmpty ? nil : $0 }
                ?? pulse.pulseDescription,
            journeyName: pulse.journey?.name ?? pulse.journeyNameSnapshot
        )
    }

    func linkCompletedFlow(pulseID: UUID, sessionID: UUID, linkedAt: Date) throws {
        do {
            guard let pulse = try grovePulseModel(id: pulseID),
                  try groveSessionModel(id: sessionID) != nil else { return }
            let existing = try context.fetch(FetchDescriptor<PulseFlowLinkModel>())
            guard !existing.contains(where: { $0.sessionID == sessionID }) else { return }
            context.insert(PulseFlowLinkModel(link: PulseFlowLink(
                id: UUID(),
                pulseID: pulseID,
                sessionID: sessionID,
                linkedAt: linkedAt
            )))
            if pulse.groveStatusRawValue == PulseGroveStatus.insight.rawValue {
                updateGroveStatus(pulse, to: .alive, changedAt: linkedAt)
            }
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func insertPulseFlowLinkForCommit(pulseID: UUID, sessionID: UUID, linkedAt: Date) throws {
        guard let pulse = try grovePulseModel(id: pulseID) else { return }
        let existing = try context.fetch(FetchDescriptor<PulseFlowLinkModel>())
        guard !existing.contains(where: { $0.sessionID == sessionID }) else { return }
        context.insert(PulseFlowLinkModel(link: PulseFlowLink(
            id: UUID(),
            pulseID: pulseID,
            sessionID: sessionID,
            linkedAt: linkedAt
        )))
        if pulse.groveStatusRawValue == PulseGroveStatus.insight.rawValue {
            updateGroveStatus(pulse, to: .alive, changedAt: linkedAt)
        }
    }

    private func grovePulseModel(id: UUID) throws -> PulseModel? {
        var descriptor = FetchDescriptor<PulseModel>(predicate: #Predicate { $0.id == id })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func groveSessionModel(id: UUID) throws -> FlowSessionModel? {
        var descriptor = FetchDescriptor<FlowSessionModel>(predicate: #Predicate { $0.id == id })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func groveChronicleModel(owner: ChronicleOwner) throws -> ChronicleModel? {
        let identity = owner.identity
        var descriptor = FetchDescriptor<ChronicleModel>(predicate: #Predicate { $0.ownerIdentity == identity })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func updateGroveStatus(_ pulse: PulseModel, to status: PulseGroveStatus, changedAt: Date) {
        pulse.groveStatusRawValue = status.rawValue
        pulse.groveStatusChangedAt = changedAt
        pulse.updatedAt = changedAt
    }

    private static func excerpt(_ chronicle: ChronicleModel) -> String {
        chronicle.moments
            .filter { $0.typeRawValue == ChronicleMomentType.text.rawValue }
            .sorted { $0.position < $1.position }
            .compactMap(\.text)
            .first { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .map { String($0.prefix(240)) } ?? ""
    }
}

extension InMemoryFlowRepository {
    func fetchIdeaGroveItems() -> [IdeaGroveItem] {
        let sessionsByID = Dictionary(uniqueKeysWithValues: sessions.map { ($0.id, $0) })
        let linksByPulse = Dictionary(grouping: pulseFlowLinks, by: \PulseFlowLink.pulseID)
        return pulses.map { pulse in
            let flows = linksByPulse[pulse.id, default: []]
                .compactMap { link -> IdeaGroveFlow? in
                    guard let session = sessionsByID[link.sessionID] else { return nil }
                    return IdeaGroveFlow(
                        sessionID: session.id,
                        title: session.title,
                        description: fetchChronicle(owner: .session(session.id)).excerpt ?? "",
                        journeyName: session.journeyName,
                        durationMs: session.durationMs,
                        startTime: session.startTime,
                        endTime: session.endTime
                    )
                }
                .sorted { $0.endTime > $1.endTime }
            let effectiveStatus: PulseGroveStatus = pulse.groveStatus == .completed && flows.isEmpty
                ? .alive
                : pulse.groveStatus
            let type: IdeaGroveItemType = switch (effectiveStatus, flows.isEmpty) {
            case (.insight, _): .insight
            case (.completed, _): .completedIdea
            case (_, false): .idea
            case (_, true): .rawPulse
            }
            return IdeaGroveItem(
                pulseID: pulse.id,
                type: type,
                title: pulse.title,
                description: fetchChronicle(owner: .pulse(pulse.id)).excerpt ?? "",
                journeyName: pulse.journeyName,
                createdAt: pulse.createdAt,
                updatedAt: pulse.updatedAt,
                groveStatus: effectiveStatus,
                groveStatusChangedAt: pulse.groveStatusChangedAt,
                flowCount: flows.count,
                totalFlowDurationMs: flows.reduce(0) { $0 + $1.durationMs },
                lastWorkedAt: flows.map(\.endTime).max(),
                flows: flows,
                wasCapturedDuringFlow: pulse.parentSessionID != nil || pulse.parentFlowInstanceID != nil
            )
        }
    }

    func fetchPulseFlowLinks(pulseID: UUID?) -> [PulseFlowLink] {
        pulseFlowLinks
            .filter { pulseID == nil || $0.pulseID == pulseID }
            .sorted { $0.linkedAt > $1.linkedAt }
    }

    func markPulseAsInsight(id: UUID, changedAt: Date) {
        guard fetchPulseFlowLinks(pulseID: id).isEmpty,
              let index = pulses.firstIndex(where: { $0.id == id }) else { return }
        updateGroveStatus(at: index, to: .insight, changedAt: changedAt)
    }

    func markPulseCompleted(id: UUID, changedAt: Date) {
        guard !fetchPulseFlowLinks(pulseID: id).isEmpty,
              let index = pulses.firstIndex(where: { $0.id == id }) else { return }
        updateGroveStatus(at: index, to: .completed, changedAt: changedAt)
    }

    func revivePulse(id: UUID, changedAt: Date) {
        guard let index = pulses.firstIndex(where: { $0.id == id }) else { return }
        updateGroveStatus(at: index, to: .alive, changedAt: changedAt)
    }

    func repairCompletedPulsesWithoutFlows(changedAt: Date) {
        let linked = Set(pulseFlowLinks.map(\.pulseID))
        for index in pulses.indices where pulses[index].groveStatus == .completed && !linked.contains(pulses[index].id) {
            updateGroveStatus(at: index, to: .alive, changedAt: changedAt)
        }
    }

    func fetchPulseLaunchContext(id: UUID) -> PulseLaunchContext? {
        guard let pulse = pulses.first(where: { $0.id == id }) else { return nil }
        return PulseLaunchContext(
            pulseID: id,
            title: pulse.title,
            description: fetchChronicle(owner: .pulse(id)).excerpt ?? pulse.description,
            journeyName: pulse.journeyName
        )
    }

    func linkCompletedFlow(pulseID: UUID, sessionID: UUID, linkedAt: Date) {
        guard pulses.contains(where: { $0.id == pulseID }),
              sessions.contains(where: { $0.id == sessionID }),
              !pulseFlowLinks.contains(where: { $0.sessionID == sessionID }) else { return }
        pulseFlowLinks.append(PulseFlowLink(id: UUID(), pulseID: pulseID, sessionID: sessionID, linkedAt: linkedAt))
        if let index = pulses.firstIndex(where: { $0.id == pulseID }), pulses[index].groveStatus == .insight {
            updateGroveStatus(at: index, to: .alive, changedAt: linkedAt)
        }
    }

    private func updateGroveStatus(at index: Int, to status: PulseGroveStatus, changedAt: Date) {
        pulses[index].groveStatus = status
        pulses[index].groveStatusChangedAt = changedAt
        pulses[index].updatedAt = changedAt
    }
}
