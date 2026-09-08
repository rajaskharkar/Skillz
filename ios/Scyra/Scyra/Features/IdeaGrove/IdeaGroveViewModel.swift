import Combine
import Foundation

enum IdeaGroveFlowRequest: Equatable {
    case launch(PulseLaunchContext)
    case activeFlowConflict
    case unavailable
}

@MainActor
final class IdeaGroveViewModel: ObservableObject {
    @Published private(set) var items: [IdeaGroveItem] = []
    @Published private(set) var isLoading = true
    @Published private(set) var errorMessage: String?
    @Published private(set) var notice: String?
    @Published var selectedCompletedTab = false
    @Published var sort: IdeaGroveSort = .recents
    @Published var expandedPulseID: UUID?
    @Published var pendingDelete: IdeaGroveItem?
    @Published var showsActiveFlowConflict = false

    private let repository: any ScyraRepository
    private let now: () -> Date

    init(repository: any ScyraRepository, now: @escaping () -> Date = Date.init) {
        self.repository = repository
        self.now = now
        refresh(repairing: true)
    }

    var aliveItems: [IdeaGroveItem] {
        Self.sortAlive(items.filter { $0.groveStatus == .alive }, by: sort)
    }

    var completedItems: [IdeaGroveItem] {
        items
            .filter { $0.type == .insight || $0.type == .completedIdea }
            .sorted {
                ($0.groveStatusChangedAt ?? $0.updatedAt) > ($1.groveStatusChangedAt ?? $1.updatedAt)
            }
    }

    var totalPulseFlowDurationMs: Int64 { items.reduce(0) { $0 + $1.totalFlowDurationMs } }
    var totalPulseFlowCount: Int { items.reduce(0) { $0 + $1.flowCount } }
    var completedPulseFlowDurationMs: Int64 { completedItems.reduce(0) { $0 + $1.totalFlowDurationMs } }
    var completedPulseFlowCount: Int { completedItems.reduce(0) { $0 + $1.flowCount } }

    func refresh(repairing: Bool = false) {
        isLoading = true
        do {
            if repairing { try repository.repairCompletedPulsesWithoutFlows(changedAt: now()) }
            if let active = try repository.fetchActiveFlow(), active.isAbandonedPulseOriginDraft {
                try repository.discardChronicle(owner: .activeFlow(active.flowInstanceID))
                try repository.clearActiveFlow()
            }
            items = try repository.fetchIdeaGroveItems()
            errorMessage = nil
        } catch {
            errorMessage = IdeaGroveStrings.loadError
        }
        isLoading = false
    }

    func toggleExpanded(_ id: UUID) {
        expandedPulseID = expandedPulseID == id ? nil : id
    }

    func requestFlow(from id: UUID) -> IdeaGroveFlowRequest {
        do {
            if let active = try repository.fetchActiveFlow(), active.isMeaningfulActiveFlow {
                showsActiveFlowConflict = true
                return .activeFlowConflict
            }
            guard let context = try repository.fetchPulseLaunchContext(id: id) else {
                return .unavailable
            }
            return .launch(context)
        } catch {
            errorMessage = IdeaGroveStrings.loadError
            return .unavailable
        }
    }

    func dismissActiveFlowConflict() {
        showsActiveFlowConflict = false
    }

    func markAsInsight(_ id: UUID) {
        mutate(notice: "Marked as Insight") {
            try repository.markPulseAsInsight(id: id, changedAt: now())
        }
    }

    func markCompleted(_ id: UUID) {
        mutate(notice: "Moved to Completed") {
            try repository.markPulseCompleted(id: id, changedAt: now())
        }
    }

    func revive(_ id: UUID) {
        mutate(notice: "Moved back to Alive") {
            try repository.revivePulse(id: id, changedAt: now())
        }
    }

    func requestDelete(_ id: UUID) {
        pendingDelete = items.first { $0.id == id }
    }

    func cancelDelete() {
        pendingDelete = nil
    }

    func confirmDelete() {
        guard let id = pendingDelete?.id else { return }
        mutate(notice: "Pulse deleted") {
            try repository.deletePulse(id: id)
        }
        pendingDelete = nil
    }

    func clearNotice() {
        notice = nil
    }

    static func sortAlive(_ items: [IdeaGroveItem], by sort: IdeaGroveSort) -> [IdeaGroveItem] {
        switch sort {
        case .recents:
            items.sorted {
                let lhsWorked = $0.flowCount > 0
                let rhsWorked = $1.flowCount > 0
                if lhsWorked != rhsWorked { return lhsWorked && !rhsWorked }
                if $0.lastWorkedAt != $1.lastWorkedAt {
                    return ($0.lastWorkedAt ?? .distantPast) > ($1.lastWorkedAt ?? .distantPast)
                }
                if $0.updatedAt != $1.updatedAt { return $0.updatedAt > $1.updatedAt }
                return $0.createdAt > $1.createdAt
            }
        case .newest:
            items.sorted { $0.createdAt > $1.createdAt }
        case .oldest:
            items.sorted { $0.createdAt < $1.createdAt }
        case .mostTime:
            items.sorted {
                if $0.totalFlowDurationMs != $1.totalFlowDurationMs {
                    return $0.totalFlowDurationMs > $1.totalFlowDurationMs
                }
                if $0.lastWorkedAt != $1.lastWorkedAt {
                    return ($0.lastWorkedAt ?? .distantPast) > ($1.lastWorkedAt ?? .distantPast)
                }
                return $0.createdAt > $1.createdAt
            }
        case .leastTime:
            items.sorted {
                if $0.totalFlowDurationMs != $1.totalFlowDurationMs {
                    return $0.totalFlowDurationMs < $1.totalFlowDurationMs
                }
                return $0.createdAt > $1.createdAt
            }
        }
    }

    private func mutate(notice: String, _ mutation: () throws -> Void) {
        do {
            try mutation()
            expandedPulseID = nil
            self.notice = notice
            refresh()
        } catch {
            errorMessage = IdeaGroveStrings.loadError
        }
    }
}
