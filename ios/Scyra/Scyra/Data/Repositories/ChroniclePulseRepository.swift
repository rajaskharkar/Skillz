import Foundation
import SwiftData

enum ChronicleRepositoryError: Error, Equatable {
    case finalizedOwner
    case ownerUnavailable
    case momentUnavailable
    case invalidMoment
    case invalidOrder
    case ownerAlreadyExists
}

@MainActor
protocol ChronicleRepository: AnyObject {
    var chronicleFileStore: ChronicleFileStore { get }
    func fetchChronicle(owner: ChronicleOwner) throws -> ChronicleSnapshot
    func setChronicleDraft(owner: ChronicleOwner, text: String) throws -> ChronicleSnapshot
    func addChronicleText(owner: ChronicleOwner, text: String) throws -> ChronicleSnapshot
    func importChronicleMedia(
        owner: ChronicleOwner,
        sources: [ChronicleMediaSource]
    ) async throws -> ChronicleMediaImportResult
    func stageChronicleMedia(
        owner: ChronicleOwner,
        sources: [ChronicleMediaSource]
    ) async throws -> ChronicleMediaStageResult
    func replaceChronicleMedia(
        owner: ChronicleOwner,
        momentID: UUID,
        items: [ChronicleMediaItem]
    ) throws -> ChronicleSnapshot
    func addChronicleVoice(
        owner: ChronicleOwner,
        staged: ChronicleStagedVoice
    ) async throws -> ChronicleSnapshot
    func updateChronicleText(owner: ChronicleOwner, momentID: UUID, text: String) throws -> ChronicleSnapshot
    func updateChronicleTranscript(
        owner: ChronicleOwner,
        momentID: UUID,
        transcript: String?,
        manuallyEdited: Bool
    ) throws -> ChronicleSnapshot
    func deleteChronicleMoment(owner: ChronicleOwner, momentID: UUID) throws -> ChronicleSnapshot
    func reorderChronicleMoments(owner: ChronicleOwner, orderedIDs: [UUID]) throws -> ChronicleSnapshot
    func discardChronicle(owner: ChronicleOwner) throws
    func reconcileChronicleStorage() async throws
}

@MainActor
protocol PulseRepository: AnyObject {
    func fetchPulseDraft() throws -> PulseDraft?
    func savePulseDraft(_ draft: PulseDraft) throws
    func discardPulseDraft(creationKey: UUID) throws
    func findCreatedPulse(creationKey: UUID) throws -> Pulse?
    func createPulse(
        creationKey: UUID,
        title: String,
        journeyName: String?,
        parentFlowInstanceID: UUID?,
        arcID: UUID?,
        createdAt: Date
    ) throws -> Pulse
    func createHistoricalPulse(
        parentSessionID: UUID,
        title: String,
        description: String,
        journeyName: String?,
        createdAt: Date
    ) throws -> Pulse
    func fetchAllPulses() throws -> [Pulse]
    func fetchPulse(id: UUID) throws -> Pulse?
    func updatePulse(id: UUID, title: String, journeyName: String?, updatedAt: Date) throws -> Pulse?
    func deletePulse(id: UUID) throws
}

@MainActor
protocol IdeaGroveRepository: AnyObject {
    func fetchIdeaGroveItems() throws -> [IdeaGroveItem]
    func fetchPulseFlowLinks(pulseID: UUID?) throws -> [PulseFlowLink]
    func markPulseAsInsight(id: UUID, changedAt: Date) throws
    func markPulseCompleted(id: UUID, changedAt: Date) throws
    func revivePulse(id: UUID, changedAt: Date) throws
    func repairCompletedPulsesWithoutFlows(changedAt: Date) throws
    func fetchPulseLaunchContext(id: UUID) throws -> PulseLaunchContext?
    func linkCompletedFlow(pulseID: UUID, sessionID: UUID, linkedAt: Date) throws
}

@MainActor
protocol ScyraRepository: FlowRepository, ChronicleRepository, PulseRepository, IdeaGroveRepository, ShellRewardRepository, ShellCollectionRepository, CreatureShellRepository, LookoutRepository, FlowPlanRepository, ArcPlanRepository {}

extension SwiftDataFlowRepository {
    func fetchChronicle(owner: ChronicleOwner) throws -> ChronicleSnapshot {
        guard let model = try chronicleModel(owner: owner) else {
            return .empty(owner: owner)
        }
        return chronicleSnapshot(from: model)
    }

    func setChronicleDraft(owner: ChronicleOwner, text: String) throws -> ChronicleSnapshot {
        try requireMutableChronicle(owner)
        let now = Date()
        let model = try getOrCreateChronicle(owner: owner, now: now)
        model.draftText = text
        model.updatedAt = now
        try context.save()
        return chronicleSnapshot(from: model)
    }

    func addChronicleText(owner: ChronicleOwner, text: String) throws -> ChronicleSnapshot {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return try fetchChronicle(owner: owner)
        }
        try requireMutableChronicle(owner)
        let now = Date()
        let chronicle = try getOrCreateChronicle(owner: owner, now: now)
        let nextPosition = chronicle.moments.count
        let moment = ChronicleMoment.text(
            chronicleID: chronicle.id,
            position: nextPosition,
            text: text,
            now: now
        )
        context.insert(ChronicleMomentModel(moment: moment, chronicle: chronicle))
        if chronicle.draftText == text { chronicle.draftText = "" }
        chronicle.updatedAt = now
        try context.save()
        return chronicleSnapshot(from: chronicle)
    }

    func importChronicleMedia(
        owner: ChronicleOwner,
        sources: [ChronicleMediaSource]
    ) async throws -> ChronicleMediaImportResult {
        guard !sources.isEmpty else {
            return ChronicleMediaImportResult(momentID: nil, importedCount: 0, failedCount: 0)
        }
        try requireMutableChronicle(owner)
        let chronicle = try getOrCreateChronicle(owner: owner, now: Date())
        try context.save()

        var stored: [ChronicleFileStore.StoredMedia] = []
        var failedCount = 0
        for source in sources {
            do {
                stored.append(try await chronicleFileStore.importMedia(
                    chronicleID: chronicle.id,
                    source: source
                ))
            } catch {
                failedCount += 1
            }
        }
        guard !stored.isEmpty else {
            return ChronicleMediaImportResult(
                momentID: nil,
                importedCount: 0,
                failedCount: failedCount
            )
        }

        do {
            try requireMutableChronicle(owner)
            guard let current = try chronicleModel(owner: owner), current.id == chronicle.id else {
                throw ChronicleRepositoryError.ownerUnavailable
            }
            let now = Date()
            let items = stored.enumerated().map { $1.item(position: $0) }
            let moment = ChronicleMoment.media(
                chronicleID: current.id,
                position: current.moments.count,
                items: items,
                now: now
            )
            let momentModel = ChronicleMomentModel(moment: moment, chronicle: current)
            context.insert(momentModel)
            items.forEach { context.insert(ChronicleMediaItemModel(item: $0, moment: momentModel)) }
            current.updatedAt = now
            try context.save()
            return ChronicleMediaImportResult(
                momentID: moment.id,
                importedCount: items.count,
                failedCount: failedCount
            )
        } catch {
            context.rollback()
            stored.forEach { chronicleFileStore.deleteIfOwned($0.localPath) }
            throw error
        }
    }

    func createHistoricalPulse(
        parentSessionID: UUID,
        title: String,
        description: String,
        journeyName: String?,
        createdAt: Date
    ) throws -> Pulse {
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedDescription = description.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty || !normalizedDescription.isEmpty else {
            throw ChronicleRepositoryError.invalidMoment
        }
        guard let session = try context.fetch(FetchDescriptor<FlowSessionModel>())
            .first(where: { $0.id == parentSessionID }) else {
            throw ChronicleRepositoryError.ownerUnavailable
        }
        let normalizedJourney = journeyName?.trimmingCharacters(in: .whitespacesAndNewlines)
        let finalJourney = normalizedJourney?.isEmpty == false ? normalizedJourney : nil
        let pulse = Pulse(
            id: UUID(),
            title: normalizedTitle,
            description: "",
            journeyName: finalJourney,
            parentSessionID: parentSessionID,
            parentFlowInstanceID: nil,
            arcID: session.arcID,
            createdAt: createdAt,
            updatedAt: createdAt,
            groveStatus: .alive,
            groveStatusChangedAt: nil
        )

        do {
            let journey = try finalJourney.map { try self.journey(named: $0, createdAt: createdAt) }
            context.insert(PulseModel(pulse: pulse, journey: journey))
            if !normalizedDescription.isEmpty {
                let owner = ChronicleOwner.pulse(pulse.id)
                let chronicle = ChronicleModel(owner: owner, now: createdAt)
                context.insert(chronicle)
                context.insert(ChronicleMomentModel(
                    moment: .text(
                        chronicleID: chronicle.id,
                        position: 0,
                        text: normalizedDescription,
                        now: createdAt
                    ),
                    chronicle: chronicle
                ))
            }
            try context.save()
            return pulse
        } catch {
            context.rollback()
            throw error
        }
    }

    func stageChronicleMedia(
        owner: ChronicleOwner,
        sources: [ChronicleMediaSource]
    ) async throws -> ChronicleMediaStageResult {
        guard !sources.isEmpty else {
            return ChronicleMediaStageResult(items: [], failedCount: 0)
        }
        try requireMutableChronicle(owner)
        guard let chronicle = try chronicleModel(owner: owner) else {
            throw ChronicleRepositoryError.ownerUnavailable
        }

        var stored: [ChronicleFileStore.StoredMedia] = []
        var failedCount = 0
        for source in sources {
            do {
                stored.append(try await chronicleFileStore.importMedia(
                    chronicleID: chronicle.id,
                    source: source
                ))
            } catch {
                failedCount += 1
            }
        }

        do {
            try requireMutableChronicle(owner)
            guard try chronicleModel(owner: owner)?.id == chronicle.id else {
                throw ChronicleRepositoryError.ownerUnavailable
            }
            return ChronicleMediaStageResult(
                items: stored.enumerated().map { $1.item(position: $0) },
                failedCount: failedCount
            )
        } catch {
            stored.forEach { chronicleFileStore.deleteIfOwned($0.localPath) }
            throw error
        }
    }

    func replaceChronicleMedia(
        owner: ChronicleOwner,
        momentID: UUID,
        items: [ChronicleMediaItem]
    ) throws -> ChronicleSnapshot {
        guard !items.isEmpty, Set(items.map(\.id)).count == items.count else {
            throw ChronicleRepositoryError.invalidMoment
        }
        try requireMutableChronicle(owner)
        guard let chronicle = try chronicleModel(owner: owner) else {
            throw ChronicleRepositoryError.ownerUnavailable
        }
        guard let moment = chronicle.moments.first(where: { $0.id == momentID }),
              moment.typeRawValue == ChronicleMomentType.media.rawValue else {
            throw ChronicleRepositoryError.momentUnavailable
        }

        let normalized = items.enumerated().map { $1.repositioned(to: $0) }
        let previousPaths = Set(moment.mediaItems.flatMap { item in
            [item.localPath, item.thumbnailPath].compactMap { $0 }
        })
        let finalPaths = Set(normalized.flatMap { item in
            [item.localPath, item.thumbnailPath].compactMap { $0 }
        })
        let existingByID = Dictionary(uniqueKeysWithValues: moment.mediaItems.map { ($0.id, $0) })
        let finalIDs = Set(normalized.map(\.id))

        for item in normalized {
            if let model = existingByID[item.id] {
                model.position = item.position
                model.localPath = item.localPath
                model.mimeType = item.mimeType
                model.durationMs = item.durationMs
                model.width = item.width
                model.height = item.height
                model.thumbnailPath = item.thumbnailPath
                model.createdAt = item.createdAt
            } else {
                context.insert(ChronicleMediaItemModel(item: item, moment: moment))
            }
        }
        moment.mediaItems
            .filter { !finalIDs.contains($0.id) }
            .forEach(context.delete)
        let now = Date()
        moment.updatedAt = now
        chronicle.updatedAt = now
        try context.save()

        let allMedia = try context.fetch(FetchDescriptor<ChronicleMediaItemModel>())
        let referencedPaths = Set(allMedia.flatMap { item in
            [item.localPath, item.thumbnailPath].compactMap { $0 }
        })
        previousPaths
            .subtracting(finalPaths)
            .subtracting(referencedPaths)
            .forEach(chronicleFileStore.deleteIfOwned)
        return chronicleSnapshot(from: chronicle)
    }

    func updateChronicleText(
        owner: ChronicleOwner,
        momentID: UUID,
        text: String
    ) throws -> ChronicleSnapshot {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw ChronicleRepositoryError.invalidMoment
        }
        try requireMutableChronicle(owner)
        guard let chronicle = try chronicleModel(owner: owner) else {
            throw ChronicleRepositoryError.ownerUnavailable
        }
        guard let moment = chronicle.moments.first(where: { $0.id == momentID }),
              moment.typeRawValue == ChronicleMomentType.text.rawValue else {
            throw ChronicleRepositoryError.momentUnavailable
        }
        let now = Date()
        moment.text = text
        moment.updatedAt = now
        chronicle.updatedAt = now
        try context.save()
        return chronicleSnapshot(from: chronicle)
    }

    func addChronicleVoice(
        owner: ChronicleOwner,
        staged: ChronicleStagedVoice
    ) async throws -> ChronicleSnapshot {
        try requireMutableChronicle(owner)
        let chronicle = try getOrCreateChronicle(owner: owner, now: Date())
        let stored: ChronicleFileStore.StoredVoice
        do {
            stored = try await chronicleFileStore.finalizeVoice(chronicleID: chronicle.id, staged: staged)
        } catch {
            await chronicleFileStore.discardVoiceStaging(staged)
            throw error
        }

        do {
            try requireMutableChronicle(owner)
            guard let current = try chronicleModel(owner: owner), current.id == chronicle.id else {
                throw ChronicleRepositoryError.ownerUnavailable
            }
            let now = Date()
            let moment = ChronicleMoment.voice(
                chronicleID: current.id,
                position: current.moments.count,
                localPath: stored.localPath,
                mimeType: stored.mimeType,
                durationMs: stored.durationMs,
                now: now
            )
            context.insert(ChronicleMomentModel(moment: moment, chronicle: current))
            current.updatedAt = now
            try context.save()
            return chronicleSnapshot(from: current)
        } catch {
            context.rollback()
            chronicleFileStore.deleteIfOwned(stored.localPath)
            throw error
        }
    }

    func updateChronicleTranscript(
        owner: ChronicleOwner,
        momentID: UUID,
        transcript: String?,
        manuallyEdited: Bool
    ) throws -> ChronicleSnapshot {
        try requireMutableChronicle(owner)
        guard let chronicle = try chronicleModel(owner: owner) else {
            throw ChronicleRepositoryError.ownerUnavailable
        }
        guard let moment = chronicle.moments.first(where: { $0.id == momentID }),
              moment.typeRawValue == ChronicleMomentType.voice.rawValue else {
            throw ChronicleRepositoryError.momentUnavailable
        }
        let now = Date()
        if manuallyEdited {
            moment.transcript = transcript
            moment.transcriptEdited = true
        } else {
            moment.originalTranscript = transcript
            if !moment.transcriptEdited { moment.transcript = transcript }
        }
        moment.updatedAt = now
        chronicle.updatedAt = now
        try context.save()
        return chronicleSnapshot(from: chronicle)
    }

    func deleteChronicleMoment(owner: ChronicleOwner, momentID: UUID) throws -> ChronicleSnapshot {
        try requireMutableChronicle(owner)
        guard let chronicle = try chronicleModel(owner: owner) else {
            throw ChronicleRepositoryError.ownerUnavailable
        }
        guard let moment = chronicle.moments.first(where: { $0.id == momentID }) else {
            throw ChronicleRepositoryError.momentUnavailable
        }
        let ownedPaths = moment.mediaItems.flatMap { item in
            [item.localPath, item.thumbnailPath].compactMap { $0 }
        } + [moment.audioPath].compactMap { $0 }
        context.delete(moment)
        let remaining = chronicle.moments
            .filter { $0.id != momentID }
            .sorted { $0.position < $1.position }
        let now = Date()
        for (position, item) in remaining.enumerated() {
            item.position = position
            item.updatedAt = now
        }
        chronicle.updatedAt = now
        try context.save()
        ownedPaths.forEach(chronicleFileStore.deleteIfOwned)
        return chronicleSnapshot(from: chronicle)
    }

    func reorderChronicleMoments(
        owner: ChronicleOwner,
        orderedIDs: [UUID]
    ) throws -> ChronicleSnapshot {
        try requireMutableChronicle(owner)
        guard let chronicle = try chronicleModel(owner: owner) else {
            throw ChronicleRepositoryError.ownerUnavailable
        }
        let currentIDs = Set(chronicle.moments.map(\.id))
        guard orderedIDs.count == chronicle.moments.count,
              Set(orderedIDs) == currentIDs else {
            throw ChronicleRepositoryError.invalidOrder
        }
        let byID = Dictionary(uniqueKeysWithValues: chronicle.moments.map { ($0.id, $0) })
        let now = Date()
        for (position, id) in orderedIDs.enumerated() {
            byID[id]?.position = position
            byID[id]?.updatedAt = now
        }
        chronicle.updatedAt = now
        try context.save()
        return chronicleSnapshot(from: chronicle)
    }

    func discardChronicle(owner: ChronicleOwner) throws {
        if let chronicle = try chronicleModel(owner: owner) {
            let chronicleID = chronicle.id
            context.delete(chronicle)
            try context.save()
            chronicleFileStore.deleteChronicle(chronicleID)
        }
    }

    func reconcileChronicleStorage() async throws {
        let media = try context.fetch(FetchDescriptor<ChronicleMediaItemModel>())
        let moments = try context.fetch(FetchDescriptor<ChronicleMomentModel>())
        let referenced = Set(
            media.flatMap { [$0.localPath, $0.thumbnailPath].compactMap { $0 } }
                + moments.compactMap(\.audioPath)
        )
        await chronicleFileStore.reconcile(referencedPaths: referenced)
    }

    func fetchPulseDraft() throws -> PulseDraft? {
        try pulseDraftModel().map { model in
            PulseDraft(
                creationKey: model.creationKey,
                title: model.title,
                journeyName: model.journeyName,
                attachToCurrentFlow: model.attachToCurrentFlow,
                createdAt: model.createdAt
            )
        }
    }

    func savePulseDraft(_ draft: PulseDraft) throws {
        if let model = try pulseDraftModel() {
            model.update(from: draft)
        } else {
            context.insert(PulseDraftModel(draft: draft))
        }
        try context.save()
    }

    func discardPulseDraft(creationKey: UUID) throws {
        try discardChronicle(owner: .pulseDraft(creationKey))
        if let model = try pulseDraftModel(), model.creationKey == creationKey {
            context.delete(model)
            try context.save()
        }
    }

    func findCreatedPulse(creationKey: UUID) throws -> Pulse? {
        guard let receipt = try pulseCreationModel(creationKey: creationKey) else { return nil }
        return try pulseModel(id: receipt.pulseID).map(pulse(from:))
    }

    func createPulse(
        creationKey: UUID,
        title: String,
        journeyName: String?,
        parentFlowInstanceID: UUID?,
        arcID: UUID?,
        createdAt: Date
    ) throws -> Pulse {
        if let existing = try findCreatedPulse(creationKey: creationKey) { return existing }

        let draftOwner = ChronicleOwner.pulseDraft(creationKey)
        let draftChronicle = try fetchChronicle(owner: draftOwner)
        guard !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                || !draftChronicle.moments.isEmpty else {
            throw ChronicleRepositoryError.invalidMoment
        }

        let normalizedJourney = journeyName?.trimmingCharacters(in: .whitespacesAndNewlines)
        let finalJourney = normalizedJourney?.isEmpty == false ? normalizedJourney : nil
        let pulse = Pulse(
            id: UUID(),
            title: title.trimmingCharacters(in: .whitespacesAndNewlines),
            description: "",
            journeyName: finalJourney,
            parentSessionID: nil,
            parentFlowInstanceID: parentFlowInstanceID,
            arcID: arcID,
            createdAt: createdAt,
            updatedAt: createdAt,
            groveStatus: .alive,
            groveStatusChangedAt: nil
        )

        do {
            let journey = try finalJourney.map { try self.journey(named: $0, createdAt: createdAt) }
            context.insert(PulseModel(pulse: pulse, journey: journey))
            try promoteChronicle(from: draftOwner, to: .pulse(pulse.id), now: createdAt)
            context.insert(PulseCreationModel(
                creationKey: creationKey,
                pulseID: pulse.id,
                createdAt: createdAt
            ))
            if let draft = try pulseDraftModel(), draft.creationKey == creationKey {
                context.delete(draft)
            }
            try context.save()
            return pulse
        } catch {
            context.rollback()
            throw error
        }
    }

    func fetchAllPulses() throws -> [Pulse] {
        var descriptor = FetchDescriptor<PulseModel>()
        descriptor.sortBy = [SortDescriptor(\PulseModel.createdAt, order: .reverse)]
        return try context.fetch(descriptor).map(pulse(from:))
    }

    func fetchPulse(id: UUID) throws -> Pulse? {
        try pulseModel(id: id).map(pulse(from:))
    }

    func updatePulse(
        id: UUID,
        title: String,
        journeyName: String?,
        updatedAt: Date
    ) throws -> Pulse? {
        guard let model = try pulseModel(id: id) else { return nil }
        do {
            let previousJourney = model.journey
            let normalizedJourney = journeyName?.trimmingCharacters(in: .whitespacesAndNewlines)
            let finalJourney = normalizedJourney?.isEmpty == false ? normalizedJourney : nil
            model.title = title.trimmingCharacters(in: .whitespacesAndNewlines)
            model.journeyNameSnapshot = finalJourney
            model.journey = try finalJourney.map { try self.journey(named: $0, createdAt: updatedAt) }
            model.updatedAt = updatedAt
            if let previousJourney,
               previousJourney.id != model.journey?.id,
               previousJourney.flows.isEmpty,
               !previousJourney.pulses.contains(where: { $0.id != id }) {
                context.delete(previousJourney)
            }
            try context.save()
            return pulse(from: model)
        } catch {
            context.rollback()
            throw error
        }
    }

    func deletePulse(id: UUID) throws {
        let chronicleID = try chronicleModel(owner: .pulse(id))?.id
        let journey = try pulseModel(id: id)?.journey
        do {
            if let chronicle = try chronicleModel(owner: .pulse(id)) { context.delete(chronicle) }
            if let pulse = try pulseModel(id: id) { context.delete(pulse) }
            let receipts = try context.fetch(FetchDescriptor<PulseCreationModel>())
            receipts.filter { $0.pulseID == id }.forEach(context.delete)
            let links = try context.fetch(FetchDescriptor<PulseFlowLinkModel>())
            links.filter { $0.pulseID == id }.forEach(context.delete)
            if let journey,
               journey.flows.isEmpty,
               !journey.pulses.contains(where: { $0.id != id }) {
                context.delete(journey)
            }
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
        if let chronicleID { chronicleFileStore.deleteChronicle(chronicleID) }
    }

    func promoteChronicle(from source: ChronicleOwner, to destination: ChronicleOwner, now: Date) throws {
        guard let chronicle = try chronicleModel(owner: source) else { return }
        if try chronicleModel(owner: destination) != nil {
            throw ChronicleRepositoryError.ownerAlreadyExists
        }
        chronicle.promote(to: destination, now: now)
    }

    func attachLivePulses(to session: FlowSession) throws {
        let flowInstanceID = session.flowInstanceID
        let descriptor = FetchDescriptor<PulseModel>(
            predicate: #Predicate { $0.parentFlowInstanceID == flowInstanceID }
        )
        for pulse in try context.fetch(descriptor) {
            pulse.parentSessionID = session.id
            pulse.parentFlowInstanceID = nil
            if let arcID = session.arcID { pulse.arcID = arcID }
            pulse.updatedAt = session.createdAt
        }
    }

    private func chronicleModel(owner: ChronicleOwner) throws -> ChronicleModel? {
        let identity = owner.identity
        var descriptor = FetchDescriptor<ChronicleModel>(
            predicate: #Predicate { $0.ownerIdentity == identity }
        )
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func getOrCreateChronicle(owner: ChronicleOwner, now: Date) throws -> ChronicleModel {
        if let existing = try chronicleModel(owner: owner) { return existing }
        let model = ChronicleModel(owner: owner, now: now)
        context.insert(model)
        return model
    }

    private func requireMutableChronicle(_ owner: ChronicleOwner) throws {
        switch owner.type {
        case .activeFlow:
            if let id = UUID(uuidString: owner.key) {
                let descriptor = FetchDescriptor<FlowSessionModel>(
                    predicate: #Predicate { $0.flowInstanceID == id }
                )
                if try !context.fetch(descriptor).isEmpty {
                    throw ChronicleRepositoryError.finalizedOwner
                }
            }
        case .pulseDraft:
            if let id = UUID(uuidString: owner.key), try pulseCreationModel(creationKey: id) != nil {
                throw ChronicleRepositoryError.finalizedOwner
            }
        case .session, .pulse:
            break
        }
    }

    private func pulseDraftModel() throws -> PulseDraftModel? {
        let id = PulseDraft.singletonID
        var descriptor = FetchDescriptor<PulseDraftModel>(predicate: #Predicate { $0.id == id })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func pulseCreationModel(creationKey: UUID) throws -> PulseCreationModel? {
        var descriptor = FetchDescriptor<PulseCreationModel>(
            predicate: #Predicate { $0.creationKey == creationKey }
        )
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func pulseModel(id: UUID) throws -> PulseModel? {
        var descriptor = FetchDescriptor<PulseModel>(predicate: #Predicate { $0.id == id })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func pulse(from model: PulseModel) -> Pulse {
        Pulse(
            id: model.id,
            title: model.title,
            description: model.pulseDescription,
            journeyName: model.journey?.name ?? model.journeyNameSnapshot,
            parentSessionID: model.parentSessionID,
            parentFlowInstanceID: model.parentFlowInstanceID,
            arcID: model.arcID,
            createdAt: model.createdAt,
            updatedAt: model.updatedAt,
            groveStatus: PulseGroveStatus(rawValue: model.groveStatusRawValue) ?? .alive,
            groveStatusChangedAt: model.groveStatusChangedAt
        )
    }

    private func chronicleSnapshot(from model: ChronicleModel) -> ChronicleSnapshot {
        let owner = ChronicleOwner(
            type: ChronicleOwnerType(rawValue: model.ownerTypeRawValue) ?? .session,
            key: model.ownerKey
        )
        return ChronicleSnapshot(
            chronicleID: model.id,
            owner: owner,
            draftText: model.draftText,
            moments: model.moments
                .sorted { $0.position < $1.position }
                .map(chronicleMoment(from:)),
            createdAt: model.createdAt,
            updatedAt: model.updatedAt
        )
    }

    private func chronicleMoment(from model: ChronicleMomentModel) -> ChronicleMoment {
        ChronicleMoment(
            id: model.id,
            chronicleID: model.chronicle?.id ?? UUID(),
            type: ChronicleMomentType(rawValue: model.typeRawValue) ?? .text,
            position: model.position,
            text: model.text,
            audioPath: model.audioPath,
            displayName: model.displayName,
            mimeType: model.mimeType,
            durationMs: model.durationMs,
            originalTranscript: model.originalTranscript,
            transcript: model.transcript,
            transcriptEdited: model.transcriptEdited,
            mediaItems: model.mediaItems.sorted { $0.position < $1.position }.map { item in
                ChronicleMediaItem(
                    id: item.id,
                    position: item.position,
                    localPath: item.localPath,
                    mimeType: item.mimeType,
                    durationMs: item.durationMs,
                    width: item.width,
                    height: item.height,
                    thumbnailPath: item.thumbnailPath,
                    createdAt: item.createdAt
                )
            },
            createdAt: model.createdAt,
            updatedAt: model.updatedAt
        )
    }
}

extension InMemoryFlowRepository {
    func fetchChronicle(owner: ChronicleOwner) -> ChronicleSnapshot {
        chronicles[owner] ?? .empty(owner: owner)
    }

    func setChronicleDraft(owner: ChronicleOwner, text: String) throws -> ChronicleSnapshot {
        try requireMutableChronicleInMemory(owner)
        let current = chronicleInMemory(owner)
        let updated = ChronicleSnapshot(
            chronicleID: current.chronicleID,
            owner: owner,
            draftText: text,
            moments: current.moments,
            createdAt: current.createdAt,
            updatedAt: Date()
        )
        chronicles[owner] = updated
        return updated
    }

    func importChronicleMedia(
        owner: ChronicleOwner,
        sources: [ChronicleMediaSource]
    ) async throws -> ChronicleMediaImportResult {
        guard !sources.isEmpty else {
            return ChronicleMediaImportResult(momentID: nil, importedCount: 0, failedCount: 0)
        }
        try requireMutableChronicleInMemory(owner)
        let initial = chronicleInMemory(owner)
        let chronicleID = initial.chronicleID ?? UUID()
        var stored: [ChronicleFileStore.StoredMedia] = []
        var failedCount = 0
        for source in sources {
            do {
                stored.append(try await chronicleFileStore.importMedia(
                    chronicleID: chronicleID,
                    source: source
                ))
            } catch {
                failedCount += 1
            }
        }
        guard !stored.isEmpty else {
            return ChronicleMediaImportResult(
                momentID: nil,
                importedCount: 0,
                failedCount: failedCount
            )
        }
        do {
            try requireMutableChronicleInMemory(owner)
            let current = chronicleInMemory(owner)
            guard current.chronicleID == chronicleID else {
                throw ChronicleRepositoryError.ownerUnavailable
            }
            let now = Date()
            let items = stored.enumerated().map { $1.item(position: $0) }
            let moment = ChronicleMoment.media(
                chronicleID: chronicleID,
                position: current.moments.count,
                items: items,
                now: now
            )
            chronicles[owner] = ChronicleSnapshot(
                chronicleID: chronicleID,
                owner: owner,
                draftText: current.draftText,
                moments: current.moments + [moment],
                createdAt: current.createdAt,
                updatedAt: now
            )
            return ChronicleMediaImportResult(
                momentID: moment.id,
                importedCount: items.count,
                failedCount: failedCount
            )
        } catch {
            stored.forEach { chronicleFileStore.deleteIfOwned($0.localPath) }
            throw error
        }
    }

    func stageChronicleMedia(
        owner: ChronicleOwner,
        sources: [ChronicleMediaSource]
    ) async throws -> ChronicleMediaStageResult {
        guard !sources.isEmpty else {
            return ChronicleMediaStageResult(items: [], failedCount: 0)
        }
        try requireMutableChronicleInMemory(owner)
        guard let chronicleID = chronicleInMemory(owner).chronicleID else {
            throw ChronicleRepositoryError.ownerUnavailable
        }
        var stored: [ChronicleFileStore.StoredMedia] = []
        var failedCount = 0
        for source in sources {
            do {
                stored.append(try await chronicleFileStore.importMedia(
                    chronicleID: chronicleID,
                    source: source
                ))
            } catch {
                failedCount += 1
            }
        }
        do {
            try requireMutableChronicleInMemory(owner)
            guard chronicleInMemory(owner).chronicleID == chronicleID else {
                throw ChronicleRepositoryError.ownerUnavailable
            }
            return ChronicleMediaStageResult(
                items: stored.enumerated().map { $1.item(position: $0) },
                failedCount: failedCount
            )
        } catch {
            stored.forEach { chronicleFileStore.deleteIfOwned($0.localPath) }
            throw error
        }
    }

    func replaceChronicleMedia(
        owner: ChronicleOwner,
        momentID: UUID,
        items: [ChronicleMediaItem]
    ) throws -> ChronicleSnapshot {
        guard !items.isEmpty, Set(items.map(\.id)).count == items.count else {
            throw ChronicleRepositoryError.invalidMoment
        }
        try requireMutableChronicleInMemory(owner)
        let current = fetchChronicle(owner: owner)
        guard let previous = current.moments.first(where: { $0.id == momentID }),
              previous.type == .media else {
            throw ChronicleRepositoryError.momentUnavailable
        }
        let normalized = items.enumerated().map { $1.repositioned(to: $0) }
        let now = Date()
        let moments = current.moments.map { moment in
            guard moment.id == momentID else { return moment }
            return ChronicleMoment(
                id: moment.id,
                chronicleID: moment.chronicleID,
                type: moment.type,
                position: moment.position,
                text: moment.text,
                audioPath: moment.audioPath,
                displayName: moment.displayName,
                mimeType: moment.mimeType,
                durationMs: moment.durationMs,
                originalTranscript: moment.originalTranscript,
                transcript: moment.transcript,
                transcriptEdited: moment.transcriptEdited,
                mediaItems: normalized,
                createdAt: moment.createdAt,
                updatedAt: now
            )
        }
        let updated = ChronicleSnapshot(
            chronicleID: current.chronicleID,
            owner: owner,
            draftText: current.draftText,
            moments: moments,
            createdAt: current.createdAt,
            updatedAt: now
        )
        chronicles[owner] = updated

        let previousPaths = Set(previous.mediaItems.flatMap { item in
            [item.localPath, item.thumbnailPath].compactMap { $0 }
        })
        let referencedPaths = Set(chronicles.values.flatMap { snapshot in
            snapshot.mediaItems.flatMap { item in
                [item.localPath, item.thumbnailPath].compactMap { $0 }
            }
        })
        previousPaths
            .subtracting(referencedPaths)
            .forEach(chronicleFileStore.deleteIfOwned)
        return updated
    }

    func addChronicleText(owner: ChronicleOwner, text: String) throws -> ChronicleSnapshot {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return fetchChronicle(owner: owner)
        }
        try requireMutableChronicleInMemory(owner)
        let current = chronicleInMemory(owner)
        let now = Date()
        let moment = ChronicleMoment.text(
            chronicleID: current.chronicleID ?? UUID(),
            position: current.moments.count,
            text: text,
            now: now
        )
        let updated = ChronicleSnapshot(
            chronicleID: current.chronicleID,
            owner: owner,
            draftText: current.draftText == text ? "" : current.draftText,
            moments: current.moments + [moment],
            createdAt: current.createdAt,
            updatedAt: now
        )
        chronicles[owner] = updated
        return updated
    }

    func updateChronicleText(
        owner: ChronicleOwner,
        momentID: UUID,
        text: String
    ) throws -> ChronicleSnapshot {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw ChronicleRepositoryError.invalidMoment
        }
        try requireMutableChronicleInMemory(owner)
        let current = fetchChronicle(owner: owner)
        guard current.moments.contains(where: { $0.id == momentID && $0.type == .text }) else {
            throw ChronicleRepositoryError.momentUnavailable
        }
        let now = Date()
        let moments = current.moments.map { moment in
            guard moment.id == momentID else { return moment }
            return ChronicleMoment(
                id: moment.id,
                chronicleID: moment.chronicleID,
                type: moment.type,
                position: moment.position,
                text: text,
                audioPath: moment.audioPath,
                displayName: moment.displayName,
                mimeType: moment.mimeType,
                durationMs: moment.durationMs,
                originalTranscript: moment.originalTranscript,
                transcript: moment.transcript,
                transcriptEdited: moment.transcriptEdited,
                mediaItems: moment.mediaItems,
                createdAt: moment.createdAt,
                updatedAt: now
            )
        }
        let updated = ChronicleSnapshot(
            chronicleID: current.chronicleID,
            owner: owner,
            draftText: current.draftText,
            moments: moments,
            createdAt: current.createdAt,
            updatedAt: now
        )
        chronicles[owner] = updated
        return updated
    }

    func addChronicleVoice(
        owner: ChronicleOwner,
        staged: ChronicleStagedVoice
    ) async throws -> ChronicleSnapshot {
        try requireMutableChronicleInMemory(owner)
        let initial = chronicleInMemory(owner)
        guard let chronicleID = initial.chronicleID else {
            await chronicleFileStore.discardVoiceStaging(staged)
            throw ChronicleRepositoryError.ownerUnavailable
        }
        let stored: ChronicleFileStore.StoredVoice
        do {
            stored = try await chronicleFileStore.finalizeVoice(chronicleID: chronicleID, staged: staged)
        } catch {
            await chronicleFileStore.discardVoiceStaging(staged)
            throw error
        }
        do {
            try requireMutableChronicleInMemory(owner)
            let current = chronicleInMemory(owner)
            guard current.chronicleID == chronicleID else {
                throw ChronicleRepositoryError.ownerUnavailable
            }
            let now = Date()
            let moment = ChronicleMoment.voice(
                chronicleID: chronicleID,
                position: current.moments.count,
                localPath: stored.localPath,
                mimeType: stored.mimeType,
                durationMs: stored.durationMs,
                now: now
            )
            let updated = ChronicleSnapshot(
                chronicleID: chronicleID,
                owner: owner,
                draftText: current.draftText,
                moments: current.moments + [moment],
                createdAt: current.createdAt,
                updatedAt: now
            )
            chronicles[owner] = updated
            return updated
        } catch {
            chronicleFileStore.deleteIfOwned(stored.localPath)
            throw error
        }
    }

    func updateChronicleTranscript(
        owner: ChronicleOwner,
        momentID: UUID,
        transcript: String?,
        manuallyEdited: Bool
    ) throws -> ChronicleSnapshot {
        try requireMutableChronicleInMemory(owner)
        let current = fetchChronicle(owner: owner)
        guard current.moments.contains(where: { $0.id == momentID && $0.type == .voice }) else {
            throw ChronicleRepositoryError.momentUnavailable
        }
        let now = Date()
        let moments = current.moments.map { moment -> ChronicleMoment in
            guard moment.id == momentID else { return moment }
            return ChronicleMoment(
                id: moment.id,
                chronicleID: moment.chronicleID,
                type: moment.type,
                position: moment.position,
                text: moment.text,
                audioPath: moment.audioPath,
                displayName: moment.displayName,
                mimeType: moment.mimeType,
                durationMs: moment.durationMs,
                originalTranscript: manuallyEdited ? moment.originalTranscript : transcript,
                transcript: manuallyEdited || !moment.transcriptEdited ? transcript : moment.transcript,
                transcriptEdited: manuallyEdited || moment.transcriptEdited,
                mediaItems: moment.mediaItems,
                createdAt: moment.createdAt,
                updatedAt: now
            )
        }
        let updated = ChronicleSnapshot(
            chronicleID: current.chronicleID,
            owner: owner,
            draftText: current.draftText,
            moments: moments,
            createdAt: current.createdAt,
            updatedAt: now
        )
        chronicles[owner] = updated
        return updated
    }

    func deleteChronicleMoment(owner: ChronicleOwner, momentID: UUID) throws -> ChronicleSnapshot {
        try requireMutableChronicleInMemory(owner)
        let current = fetchChronicle(owner: owner)
        guard current.moments.contains(where: { $0.id == momentID }) else {
            throw ChronicleRepositoryError.momentUnavailable
        }
        let removed = current.moments.first { $0.id == momentID }
        let now = Date()
        let moments = current.moments
            .filter { $0.id != momentID }
            .enumerated()
            .map { position, moment in moment.repositioned(to: position, now: now) }
        let updated = ChronicleSnapshot(
            chronicleID: current.chronicleID,
            owner: owner,
            draftText: current.draftText,
            moments: moments,
            createdAt: current.createdAt,
            updatedAt: now
        )
        chronicles[owner] = updated
        removed?.mediaItems.forEach { item in
            chronicleFileStore.deleteIfOwned(item.localPath)
            item.thumbnailPath.map(chronicleFileStore.deleteIfOwned)
        }
        removed?.audioPath.map(chronicleFileStore.deleteIfOwned)
        return updated
    }

    func reorderChronicleMoments(
        owner: ChronicleOwner,
        orderedIDs: [UUID]
    ) throws -> ChronicleSnapshot {
        try requireMutableChronicleInMemory(owner)
        let current = fetchChronicle(owner: owner)
        guard orderedIDs.count == current.moments.count,
              Set(orderedIDs) == Set(current.moments.map(\.id)) else {
            throw ChronicleRepositoryError.invalidOrder
        }
        let byID = Dictionary(uniqueKeysWithValues: current.moments.map { ($0.id, $0) })
        let now = Date()
        let moments = orderedIDs.enumerated().compactMap { position, id in
            byID[id]?.repositioned(to: position, now: now)
        }
        let updated = ChronicleSnapshot(
            chronicleID: current.chronicleID,
            owner: owner,
            draftText: current.draftText,
            moments: moments,
            createdAt: current.createdAt,
            updatedAt: now
        )
        chronicles[owner] = updated
        return updated
    }

    func discardChronicle(owner: ChronicleOwner) {
        let chronicleID = chronicles.removeValue(forKey: owner)?.chronicleID
        if let chronicleID { chronicleFileStore.deleteChronicle(chronicleID) }
        finalizedChronicleOwners.insert(owner)
    }

    func reconcileChronicleStorage() async {
        let referenced = Set(chronicles.values.flatMap { snapshot in
            snapshot.moments.flatMap { moment in
                moment.mediaItems.flatMap { [$0.localPath, $0.thumbnailPath].compactMap { $0 } }
                    + [moment.audioPath].compactMap { $0 }
            }
        })
        await chronicleFileStore.reconcile(referencedPaths: referenced)
    }

    func fetchPulseDraft() -> PulseDraft? { pulseDraft }
    func savePulseDraft(_ draft: PulseDraft) { pulseDraft = draft }

    func discardPulseDraft(creationKey: UUID) {
        discardChronicle(owner: .pulseDraft(creationKey))
        if pulseDraft?.creationKey == creationKey { pulseDraft = nil }
    }

    func findCreatedPulse(creationKey: UUID) -> Pulse? {
        guard let pulseID = pulseCreationReceipts[creationKey] else { return nil }
        return pulses.first { $0.id == pulseID }
    }

    func createPulse(
        creationKey: UUID,
        title: String,
        journeyName: String?,
        parentFlowInstanceID: UUID?,
        arcID: UUID?,
        createdAt: Date
    ) throws -> Pulse {
        if let existing = findCreatedPulse(creationKey: creationKey) { return existing }
        let owner = ChronicleOwner.pulseDraft(creationKey)
        guard !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                || !fetchChronicle(owner: owner).moments.isEmpty else {
            throw ChronicleRepositoryError.invalidMoment
        }
        let normalizedJourney = journeyName?.trimmingCharacters(in: .whitespacesAndNewlines)
        let pulse = Pulse(
            id: UUID(),
            title: title.trimmingCharacters(in: .whitespacesAndNewlines),
            description: "",
            journeyName: normalizedJourney?.isEmpty == false ? normalizedJourney : nil,
            parentSessionID: nil,
            parentFlowInstanceID: parentFlowInstanceID,
            arcID: arcID,
            createdAt: createdAt,
            updatedAt: createdAt,
            groveStatus: .alive,
            groveStatusChangedAt: nil
        )
        pulses.append(pulse)
        promoteChronicleInMemory(from: owner, to: .pulse(pulse.id))
        pulseCreationReceipts[creationKey] = pulse.id
        if pulseDraft?.creationKey == creationKey { pulseDraft = nil }
        return pulse
    }

    func createHistoricalPulse(
        parentSessionID: UUID,
        title: String,
        description: String,
        journeyName: String?,
        createdAt: Date
    ) throws -> Pulse {
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedDescription = description.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty || !normalizedDescription.isEmpty else {
            throw ChronicleRepositoryError.invalidMoment
        }
        guard let session = sessions.first(where: { $0.id == parentSessionID }) else {
            throw ChronicleRepositoryError.ownerUnavailable
        }
        let normalizedJourney = journeyName?.trimmingCharacters(in: .whitespacesAndNewlines)
        let pulse = Pulse(
            id: UUID(),
            title: normalizedTitle,
            description: "",
            journeyName: normalizedJourney?.isEmpty == false ? normalizedJourney : nil,
            parentSessionID: parentSessionID,
            parentFlowInstanceID: nil,
            arcID: session.arcID,
            createdAt: createdAt,
            updatedAt: createdAt,
            groveStatus: .alive,
            groveStatusChangedAt: nil
        )
        pulses.append(pulse)
        if !normalizedDescription.isEmpty {
            let owner = ChronicleOwner.pulse(pulse.id)
            let chronicleID = UUID()
            chronicles[owner] = ChronicleSnapshot(
                chronicleID: chronicleID,
                owner: owner,
                draftText: "",
                moments: [
                    .text(
                        chronicleID: chronicleID,
                        position: 0,
                        text: normalizedDescription,
                        now: createdAt
                    )
                ],
                createdAt: createdAt,
                updatedAt: createdAt
            )
        }
        return pulse
    }

    func fetchAllPulses() -> [Pulse] { pulses.sorted { $0.createdAt > $1.createdAt } }
    func fetchPulse(id: UUID) -> Pulse? { pulses.first { $0.id == id } }

    func updatePulse(
        id: UUID,
        title: String,
        journeyName: String?,
        updatedAt: Date
    ) -> Pulse? {
        guard let index = pulses.firstIndex(where: { $0.id == id }) else { return nil }
        let normalizedJourney = journeyName?.trimmingCharacters(in: .whitespacesAndNewlines)
        pulses[index].title = title.trimmingCharacters(in: .whitespacesAndNewlines)
        pulses[index].journeyName = normalizedJourney?.isEmpty == false ? normalizedJourney : nil
        pulses[index].updatedAt = updatedAt
        return pulses[index]
    }

    func deletePulse(id: UUID) {
        pulses.removeAll { $0.id == id }
        pulseFlowLinks.removeAll { $0.pulseID == id }
        if let chronicleID = chronicles.removeValue(forKey: .pulse(id))?.chronicleID {
            chronicleFileStore.deleteChronicle(chronicleID)
        }
        pulseCreationReceipts = pulseCreationReceipts.filter { $0.value != id }
    }

    func promoteChronicleInMemory(from source: ChronicleOwner, to destination: ChronicleOwner) {
        guard let current = chronicles.removeValue(forKey: source) else {
            finalizedChronicleOwners.insert(source)
            return
        }
        chronicles[destination] = ChronicleSnapshot(
            chronicleID: current.chronicleID,
            owner: destination,
            draftText: current.draftText,
            moments: current.moments,
            createdAt: current.createdAt,
            updatedAt: Date()
        )
        finalizedChronicleOwners.insert(source)
    }

    private func chronicleInMemory(_ owner: ChronicleOwner) -> ChronicleSnapshot {
        if let current = chronicles[owner] { return current }
        let now = Date()
        let snapshot = ChronicleSnapshot(
            chronicleID: UUID(),
            owner: owner,
            draftText: "",
            moments: [],
            createdAt: now,
            updatedAt: now
        )
        chronicles[owner] = snapshot
        return snapshot
    }

    private func requireMutableChronicleInMemory(_ owner: ChronicleOwner) throws {
        if finalizedChronicleOwners.contains(owner) {
            throw ChronicleRepositoryError.finalizedOwner
        }
        switch owner.type {
        case .activeFlow:
            if let id = UUID(uuidString: owner.key), sessions.contains(where: { $0.flowInstanceID == id }) {
                throw ChronicleRepositoryError.finalizedOwner
            }
        case .pulseDraft:
            if let id = UUID(uuidString: owner.key), pulseCreationReceipts[id] != nil {
                throw ChronicleRepositoryError.finalizedOwner
            }
        case .session, .pulse:
            break
        }
    }
}

private extension ChronicleMoment {
    func repositioned(to position: Int, now: Date) -> Self {
        Self(
            id: id,
            chronicleID: chronicleID,
            type: type,
            position: position,
            text: text,
            audioPath: audioPath,
            displayName: displayName,
            mimeType: mimeType,
            durationMs: durationMs,
            originalTranscript: originalTranscript,
            transcript: transcript,
            transcriptEdited: transcriptEdited,
            mediaItems: mediaItems,
            createdAt: createdAt,
            updatedAt: now
        )
    }
}
