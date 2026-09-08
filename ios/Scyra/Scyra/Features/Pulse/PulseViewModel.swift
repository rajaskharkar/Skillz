import Combine
import Foundation

@MainActor
final class PulseViewModel: ObservableObject {
    @Published private(set) var title = ""
    @Published private(set) var journeyName = ""
    @Published private(set) var attachToCurrentFlow = false
    @Published private(set) var isFlowActive = false
    @Published private(set) var chronicle: ChronicleSnapshot
    @Published private(set) var journeys: [Journey] = []
    @Published private(set) var errorMessage: String?
    @Published private(set) var isSaving = false
    @Published private(set) var isImportingChronicleMedia = false

    private let repository: any ScyraRepository
    private let now: () -> Date
    private var creationKey: UUID
    private var draftCreatedAt: Date
    private var restoredDraft = false

    init(repository: any ScyraRepository, now: @escaping () -> Date = Date.init) {
        let key = UUID()
        let createdAt = now()
        self.repository = repository
        self.now = now
        self.creationKey = key
        self.draftCreatedAt = createdAt
        self.chronicle = .empty(owner: .pulseDraft(key))
        restore()
    }

    var canSave: Bool {
        !isSaving && (
            !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                || !chronicle.moments.isEmpty
                || !chronicle.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        )
    }

    var owner: ChronicleOwner { .pulseDraft(creationKey) }

    func prepareForPresentation(isFlowActive: Bool) {
        self.isFlowActive = isFlowActive
        if !restoredDraft { attachToCurrentFlow = isFlowActive }
        if !isFlowActive { attachToCurrentFlow = false }
        persistDraftMetadata()
    }

    func updateTitle(_ value: String) {
        title = String(value.prefix(60))
        persistDraftMetadata()
    }

    func updateJourneyName(_ value: String) {
        journeyName = value
        persistDraftMetadata()
    }

    func selectJourney(_ journey: Journey) {
        updateJourneyName(journey.name)
    }

    func setAttachToCurrentFlow(_ enabled: Bool) {
        attachToCurrentFlow = isFlowActive && enabled
        persistDraftMetadata()
    }

    func save(activeFlowContext: (flowInstanceID: UUID, arcID: UUID?)?) -> Pulse? {
        guard !isSaving else { return nil }
        guard chronicle.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            errorMessage = ChronicleStrings.unfinishedMoment
            return nil
        }
        guard !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || !chronicle.moments.isEmpty else {
            errorMessage = PulseStrings.validation
            return nil
        }

        isSaving = true
        defer { isSaving = false }
        do {
            let context = attachToCurrentFlow ? activeFlowContext : nil
            let pulse = try repository.createPulse(
                creationKey: creationKey,
                title: title,
                journeyName: journeyName,
                parentFlowInstanceID: context?.flowInstanceID,
                arcID: context?.arcID,
                createdAt: now()
            )
            errorMessage = nil
            return pulse
        } catch {
            errorMessage = PulseStrings.saveError
            #if DEBUG
            print("Scyra Pulse persistence error: \(error)")
            #endif
            return nil
        }
    }

    func finishSuccessfulPresentation() {
        startFreshDraft()
    }

    func cancel() {
        do {
            try repository.discardPulseDraft(creationKey: creationKey)
            startFreshDraft()
        } catch {
            errorMessage = PulseStrings.saveError
        }
    }

    func updateChronicleDraft(_ text: String) {
        performChronicleMutation {
            try repository.setChronicleDraft(owner: owner, text: text)
        }
    }

    @discardableResult
    func addChronicleText() -> Bool {
        guard !chronicle.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return false
        }
        return performChronicleMutation {
            try repository.addChronicleText(owner: owner, text: chronicle.draftText)
        }
    }

    func discardChronicleDraftText() {
        performChronicleMutation {
            try repository.setChronicleDraft(owner: owner, text: "")
        }
    }

    func updateChronicleMoment(id: UUID, text: String) {
        performChronicleMutation {
            try repository.updateChronicleText(owner: owner, momentID: id, text: text)
        }
    }

    func importChronicleMedia(_ sources: [ChronicleMediaSource]) async -> ChronicleMediaImportResult? {
        guard !isImportingChronicleMedia else { return nil }
        isImportingChronicleMedia = true
        defer { isImportingChronicleMedia = false }
        do {
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
            let result = try await repository.stageChronicleMedia(owner: owner, sources: sources)
            errorMessage = nil
            return result
        } catch {
            errorMessage = ChronicleStrings.saveError
            return nil
        }
    }

    func replaceChronicleMedia(momentID: UUID, items: [ChronicleMediaItem]) -> Bool {
        performChronicleMutation {
            try repository.replaceChronicleMedia(owner: owner, momentID: momentID, items: items)
        }
    }

    func addChronicleVoice(_ staged: ChronicleStagedVoice) async -> Bool {
        do {
            chronicle = try await repository.addChronicleVoice(owner: owner, staged: staged)
            errorMessage = nil
            return true
        } catch {
            errorMessage = ChronicleStrings.saveError
            return false
        }
    }

    func updateChronicleTranscript(id: UUID, transcript: String?, manuallyEdited: Bool) -> Bool {
        performChronicleMutation {
            try repository.updateChronicleTranscript(
                owner: owner,
                momentID: id,
                transcript: transcript,
                manuallyEdited: manuallyEdited
            )
        }
    }

    func deleteChronicleMoment(id: UUID) {
        performChronicleMutation {
            try repository.deleteChronicleMoment(owner: owner, momentID: id)
        }
    }

    func moveChronicleMoment(id: UUID, offset: Int) {
        var ids = chronicle.moments.map(\.id)
        guard let source = ids.firstIndex(of: id) else { return }
        let destination = source + offset
        guard ids.indices.contains(destination) else { return }
        ids.swapAt(source, destination)
        performChronicleMutation {
            try repository.reorderChronicleMoments(owner: owner, orderedIDs: ids)
        }
    }

    private func restore() {
        do {
            if let draft = try repository.fetchPulseDraft() {
                creationKey = draft.creationKey
                draftCreatedAt = draft.createdAt
                title = draft.title
                journeyName = draft.journeyName
                attachToCurrentFlow = draft.attachToCurrentFlow
                restoredDraft = true
            }
            chronicle = try repository.fetchChronicle(owner: owner)
            journeys = try repository.fetchJourneys()
        } catch {
            errorMessage = PulseStrings.saveError
        }
    }

    private func persistDraftMetadata() {
        do {
            try repository.savePulseDraft(PulseDraft(
                creationKey: creationKey,
                title: title,
                journeyName: journeyName,
                attachToCurrentFlow: attachToCurrentFlow,
                createdAt: draftCreatedAt
            ))
        } catch {
            errorMessage = PulseStrings.saveError
        }
    }

    private func startFreshDraft() {
        creationKey = UUID()
        draftCreatedAt = now()
        title = ""
        journeyName = ""
        attachToCurrentFlow = false
        isFlowActive = false
        restoredDraft = false
        chronicle = .empty(owner: owner)
        errorMessage = nil
        do { journeys = try repository.fetchJourneys() }
        catch { errorMessage = PulseStrings.saveError }
    }

    @discardableResult
    private func performChronicleMutation(
        _ mutation: () throws -> ChronicleSnapshot
    ) -> Bool {
        do {
            chronicle = try mutation()
            errorMessage = nil
            return true
        } catch {
            errorMessage = ChronicleStrings.saveError
            return false
        }
    }
}
