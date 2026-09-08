import Combine
import Foundation

@MainActor
final class HorizonViewModel: ObservableObject {
    @Published private(set) var activePlans: [FlowPlan] = []
    @Published private(set) var dreamPlans: [FlowPlan] = []
    @Published private(set) var journeySuggestions: [String] = []
    @Published private(set) var arcPlans: [ArcPlan] = []
    @Published private(set) var activePlannedArcRun: ActivePlannedArcRun?
    @Published private(set) var isLoading = true
    @Published private(set) var isSaving = false
    @Published private(set) var errorMessage: String?
    @Published var selectedTimeLens: HorizonTimeLens = .day
    @Published var selectedPrimaryTab: HorizonPrimaryTab = .flows
    @Published var showsActiveFlowConflict = false
    @Published private(set) var pendingNewArcRequestID: UUID?

    private let repository: any ScyraRepository
    private let now: () -> Date

    init(repository: any ScyraRepository, now: @escaping () -> Date = Date.init) {
        self.repository = repository
        self.now = now
        refresh()
    }

    func refresh() {
        do {
            activePlans = try repository.fetchActiveFlowPlans()
            dreamPlans = try repository.fetchArchivedFlowPlans()
            arcPlans = try repository.fetchActiveArcPlans()
            activePlannedArcRun = try repository.fetchActivePlannedArcRun()
            let existingJourneys = try repository.fetchJourneys().map(\.name)
            let plannedJourneys = (activePlans + dreamPlans).compactMap(\.journeyName)
            journeySuggestions = uniqueNames(existingJourneys + plannedJourneys)
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    @discardableResult
    func save(_ draft: FlowPlanDraft, editing planID: UUID? = nil) -> Bool {
        guard !isSaving else { return false }
        isSaving = true
        defer { isSaving = false }
        do {
            let input = try draft.validated()
            if let planID {
                try repository.updateFlowPlan(id: planID, input: input, at: now())
            } else {
                _ = try repository.createFlowPlan(input: input, at: now())
            }
            refresh()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func setPinned(_ plan: FlowPlan, pinned: Bool) {
        perform { try repository.setFlowPlanPinned(id: plan.id, pinned: pinned, at: now()) }
    }

    func moveToDreams(_ plan: FlowPlan) {
        perform { try repository.setFlowPlanArchived(id: plan.id, archived: true, at: now()) }
    }

    func restore(_ plan: FlowPlan) {
        perform { try repository.setFlowPlanArchived(id: plan.id, archived: false, at: now()) }
    }

    func delete(_ plan: FlowPlan) {
        perform { try repository.deleteFlowPlan(id: plan.id) }
    }

    func recordSuccessfulLaunch(_ plan: FlowPlan) {
        perform { try repository.markFlowPlanLaunched(id: plan.id, at: now()) }
    }

    func reportActiveFlowConflict() {
        showsActiveFlowConflict = true
    }

    func requestNewArcPlan() {
        selectedPrimaryTab = .arcs
        pendingNewArcRequestID = UUID()
    }

    @discardableResult
    func consumeNewArcPlanRequest() -> Bool {
        guard pendingNewArcRequestID != nil else { return false }
        pendingNewArcRequestID = nil
        return true
    }

    @discardableResult
    func saveArc(_ draft: ArcPlanDraft, editing planID: UUID? = nil) -> ArcPlan? {
        guard !isSaving else { return nil }
        isSaving = true
        defer { isSaving = false }
        do {
            let input = try draft.validatedInput()
            let saved: ArcPlan
            if let planID {
                try repository.updateArcPlan(id: planID, input: input, at: now())
                guard let refreshed = try repository.fetchArcPlan(id: planID) else {
                    throw ArcPlanValidationError.notFound
                }
                saved = refreshed
            } else {
                saved = try repository.createArcPlan(input: input, at: now())
            }
            refresh()
            return saved
        } catch {
            errorMessage = error.localizedDescription
            return nil
        }
    }

    func setInStudio(_ plan: ArcPlan, isInStudio: Bool) {
        perform { try repository.setArcPlanInStudio(id: plan.id, isInStudio: isInStudio, at: now()) }
    }

    func delete(_ plan: ArcPlan) {
        perform { try repository.deleteArcPlan(id: plan.id) }
    }

    func saveSuggestedRoute(_ route: SuggestedRoute, addToStudio: Bool) -> ArcPlan? {
        saveArc(ArcPlanDraft(route: route, isInStudio: addToStudio))
    }

    func arcPlan(id: UUID) -> ArcPlan? { arcPlans.first { $0.id == id } }

    func clearError() {
        errorMessage = nil
    }

    private func perform(_ operation: () throws -> Void) {
        do {
            try operation()
            refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func uniqueNames(_ values: [String]) -> [String] {
        var seen: Set<String> = []
        return values.filter { value in
            let normalized = value.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !normalized.isEmpty else { return false }
            return seen.insert(normalized.folding(options: [.caseInsensitive], locale: .current)).inserted
        }
    }
}
