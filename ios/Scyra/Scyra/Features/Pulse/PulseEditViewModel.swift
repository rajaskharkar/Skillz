import Combine
import Foundation

@MainActor
final class PulseEditViewModel: ObservableObject {
    @Published private(set) var pulse: Pulse?
    @Published private(set) var title = ""
    @Published private(set) var journeyName = ""
    @Published private(set) var chronicle: ChronicleSnapshot
    @Published private(set) var journeys: [Journey] = []
    @Published private(set) var errorMessage: String?
    @Published private(set) var isSaving = false
    @Published private(set) var isImportingChronicleMedia = false

    private let pulseID: UUID
    private let repository: any ScyraRepository
    private let now: () -> Date

    init(pulseID: UUID, repository: any ScyraRepository, now: @escaping () -> Date = Date.init) {
        self.pulseID = pulseID
        self.repository = repository
        self.now = now
        self.chronicle = .empty(owner: .pulse(pulseID))
        reload()
    }

    var canSave: Bool {
        !isSaving && (
            !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                || !chronicle.moments.isEmpty
                || !chronicle.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        )
    }

    func updateTitle(_ value: String) { title = String(value.prefix(60)) }
    func updateJourneyName(_ value: String) { journeyName = value }
    func selectJourney(_ journey: Journey) { journeyName = journey.name }

    func save() -> Bool {
        guard canSave else { return false }
        guard chronicle.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            errorMessage = ChronicleStrings.unfinishedMoment
            return false
        }
        isSaving = true
        defer { isSaving = false }
        do {
            pulse = try repository.updatePulse(
                id: pulseID,
                title: title,
                journeyName: journeyName,
                updatedAt: now()
            )
            errorMessage = pulse == nil ? PulseStrings.saveError : nil
            return pulse != nil
        } catch {
            errorMessage = PulseStrings.saveError
            return false
        }
    }

    func updateChronicleDraft(_ text: String) {
        mutate { try repository.setChronicleDraft(owner: .pulse(pulseID), text: text) }
    }

    @discardableResult
    func addChronicleText() -> Bool {
        guard !chronicle.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return false
        }
        return mutate { try repository.addChronicleText(owner: .pulse(pulseID), text: chronicle.draftText) }
    }

    func discardChronicleDraftText() {
        mutate { try repository.setChronicleDraft(owner: .pulse(pulseID), text: "") }
    }

    func updateChronicleMoment(id: UUID, text: String) {
        mutate { try repository.updateChronicleText(owner: .pulse(pulseID), momentID: id, text: text) }
    }

    func importChronicleMedia(_ sources: [ChronicleMediaSource]) async -> ChronicleMediaImportResult? {
        guard !isImportingChronicleMedia else { return nil }
        isImportingChronicleMedia = true
        defer { isImportingChronicleMedia = false }
        do {
            let owner = ChronicleOwner.pulse(pulseID)
            let result = try await repository.importChronicleMedia(owner: owner, sources: sources)
            chronicle = try repository.fetchChronicle(owner: owner)
            errorMessage = nil
            return result
        } catch {
            errorMessage = ChronicleStrings.saveError
            return nil
        }
    }

    func stageChronicleMedia(_ sources: [ChronicleMediaSource]) async -> ChronicleMediaStageResult? {
        guard !isImportingChronicleMedia else { return nil }
        isImportingChronicleMedia = true
        defer { isImportingChronicleMedia = false }
        do {
            let owner = ChronicleOwner.pulse(pulseID)
            let result = try await repository.stageChronicleMedia(owner: owner, sources: sources)
            errorMessage = nil
            return result
        } catch {
            errorMessage = ChronicleStrings.saveError
            return nil
        }
    }

    func replaceChronicleMedia(momentID: UUID, items: [ChronicleMediaItem]) -> Bool {
        mutate {
            try repository.replaceChronicleMedia(
                owner: .pulse(pulseID),
                momentID: momentID,
                items: items
            )
        }
    }

    func addChronicleVoice(_ staged: ChronicleStagedVoice) async -> Bool {
        do {
            let owner = ChronicleOwner.pulse(pulseID)
            chronicle = try await repository.addChronicleVoice(owner: owner, staged: staged)
            errorMessage = nil
            return true
        } catch {
            errorMessage = ChronicleStrings.saveError
            return false
        }
    }

    func updateChronicleTranscript(id: UUID, transcript: String?, manuallyEdited: Bool) -> Bool {
        mutate {
            try repository.updateChronicleTranscript(
                owner: .pulse(pulseID),
                momentID: id,
                transcript: transcript,
                manuallyEdited: manuallyEdited
            )
        }
    }

    func deleteChronicleMoment(id: UUID) {
        mutate { try repository.deleteChronicleMoment(owner: .pulse(pulseID), momentID: id) }
    }

    func moveChronicleMoment(id: UUID, offset: Int) {
        var ids = chronicle.moments.map(\.id)
        guard let source = ids.firstIndex(of: id) else { return }
        let destination = source + offset
        guard ids.indices.contains(destination) else { return }
        ids.swapAt(source, destination)
        mutate { try repository.reorderChronicleMoments(owner: .pulse(pulseID), orderedIDs: ids) }
    }

    private func reload() {
        do {
            pulse = try repository.fetchPulse(id: pulseID)
            title = pulse?.title ?? ""
            journeyName = pulse?.journeyName ?? ""
            chronicle = try repository.fetchChronicle(owner: .pulse(pulseID))
            journeys = try repository.fetchJourneys()
        } catch {
            errorMessage = PulseStrings.saveError
        }
    }

    @discardableResult
    private func mutate(_ operation: () throws -> ChronicleSnapshot) -> Bool {
        do {
            chronicle = try operation()
            errorMessage = nil
            return true
        } catch {
            errorMessage = ChronicleStrings.saveError
            return false
        }
    }
}
