import Combine
import Foundation

@MainActor
final class LookoutViewModel: ObservableObject {
    @Published private(set) var snapshot = LookoutSnapshot(
        cards: [], completions: [], unclaimedPearls: 0, unclaimedCompletionCount: 0
    )
    @Published private(set) var journeys: [Journey] = []
    @Published var selectedPeriod: ObjectivePeriod = .daily
    @Published var showsAchievements = false
    @Published var showsEditor = false
    @Published var editorJourney = ""
    @Published var editorKind: ObjectiveKind = .oneTime
    @Published var editorHours = "0"
    @Published var editorMinutes = "30"
    @Published var editorStartDate = Date()
    @Published private(set) var objectivePendingRemoval: ObjectiveCardModel?
    @Published private(set) var message: String?
    @Published private(set) var errorMessage: String?

    private let repository: any ScyraRepository
    private let now: () -> Date
    private let calendar: Calendar

    init(
        repository: any ScyraRepository,
        now: @escaping () -> Date = Date.init,
        calendar: Calendar = .current
    ) {
        self.repository = repository
        self.now = now
        self.calendar = calendar
    }

    var visibleCards: [ObjectiveCardModel] {
        snapshot.cards.filter { $0.objective.period == selectedPeriod }
    }

    var completionGroups: [LookoutCompletionGroup] {
        Dictionary(grouping: snapshot.completions, by: { $0.journeyID }).map { journeyID, values in
            LookoutCompletionGroup(
                id: journeyID,
                journeyName: values.first?.journeyNameSnapshot ?? "Journey",
                rows: Dictionary(grouping: values, by: { $0.badgeKey }).map { badgeID, completions in
                    LookoutCompletionRow(
                        id: badgeID,
                        period: completions.first?.period ?? .daily,
                        completionCount: completions.count,
                        earnedPearls: completions.reduce(0) { $0 + $1.finalRewardPearls },
                        unclaimedPearls: completions.filter { !$0.pearlsClaimed }
                            .reduce(0) { $0 + $1.finalRewardPearls },
                        lastCompletedAt: completions.map(\.completedAt).max() ?? .distantPast
                    )
                }.sorted { $0.period.order < $1.period.order },
                lastCompletedAt: values.map(\.completedAt).max() ?? .distantPast
            )
        }.sorted { $0.lastCompletedAt > $1.lastCompletedAt }
    }

    func refresh() {
        do {
            _ = try repository.reconcileLookout(at: now(), calendar: calendar)
            snapshot = try repository.fetchLookoutSnapshot(at: now(), calendar: calendar)
            journeys = try repository.fetchJourneys()
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func openEditor(period: ObjectivePeriod? = nil) {
        if let period { selectedPeriod = period }
        editorJourney = ""
        editorKind = .oneTime
        editorStartDate = calendar.startOfDay(for: now())
        switch selectedPeriod {
        case .daily: editorHours = "0"; editorMinutes = "30"
        case .weekly: editorHours = "2"; editorMinutes = "0"
        case .monthly: editorHours = "8"; editorMinutes = "0"
        }
        message = nil
        showsEditor = true
    }

    func saveObjective() {
        let hours = Int64(editorHours) ?? -1
        let minutes = Int64(editorMinutes) ?? -1
        guard hours >= 0, minutes >= 0 else {
            errorMessage = LookoutError.targetRequired.localizedDescription
            return
        }
        guard minutes <= 59 else {
            errorMessage = "Minutes must be between 0 and 59."
            return
        }
        let totalMinutes = hours * 60 + minutes
        let matchedJourney = journeys.first {
            $0.name.caseInsensitiveCompare(editorJourney.trimmingCharacters(in: .whitespacesAndNewlines)) == .orderedSame
        }
        do {
            _ = try repository.createLookoutObjective(
                .init(
                    journeyID: matchedJourney?.id,
                    journeyName: matchedJourney?.name ?? editorJourney,
                    period: selectedPeriod,
                    kind: editorKind,
                    targetDurationMs: totalMinutes * ObjectiveProgressCalculator.millisPerMinute,
                    startAt: editorStartDate
                ),
                at: now(),
                calendar: calendar
            )
            showsEditor = false
            message = "Objective set for \(matchedJourney?.name ?? editorJourney)."
            refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func claim(completionID: UUID) {
        performClaim { try repository.claimLookoutCompletion(id: completionID, at: now()) }
    }

    func claimAchievement(_ badgeID: String) {
        performClaim { try repository.claimLookoutAchievement(badgeID: badgeID, at: now()) }
    }

    func claimAll() {
        performClaim { try repository.claimAllLookoutRewards(at: now()) }
    }

    func requestRemoval(_ card: ObjectiveCardModel) {
        objectivePendingRemoval = card
    }

    func cancelRemoval() { objectivePendingRemoval = nil }

    func archivePendingObjective() {
        guard let objectivePendingRemoval else { return }
        do {
            try repository.archiveLookoutObjective(
                id: objectivePendingRemoval.objective.id, at: now(), calendar: calendar
            )
            self.objectivePendingRemoval = nil
            refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func skipPendingCycle() {
        guard let objectivePendingRemoval else { return }
        do {
            try repository.skipLookoutCycle(
                objectiveID: objectivePendingRemoval.objective.id, at: now(), calendar: calendar
            )
            self.objectivePendingRemoval = nil
            refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func dismissError() { errorMessage = nil }
    func dismissMessage() { message = nil }

    func duration(_ milliseconds: Int64) -> String {
        let minutes = max(0, milliseconds / ObjectiveProgressCalculator.millisPerMinute)
        let hours = minutes / 60
        let remainder = minutes % 60
        if hours > 0 { return remainder > 0 ? "\(hours)h \(remainder)m" : "\(hours)h" }
        return "\(minutes)m"
    }

    func remaining(until date: Date) -> String {
        let minutes = max(0, Int(date.timeIntervalSince(now()) / 60))
        let days = minutes / (24 * 60)
        let hours = (minutes % (24 * 60)) / 60
        let remainder = minutes % 60
        if days > 0 { return hours > 0 ? "\(days)d \(hours)h" : "\(days)d" }
        if hours > 0 { return remainder > 0 ? "\(hours)h \(remainder)m" : "\(hours)h" }
        return "\(remainder)m"
    }

    private func performClaim(_ operation: () throws -> Int) {
        do {
            let pearls = try operation()
            if pearls > 0 { message = "\(pearls) Pearls claimed." }
            refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

struct LookoutCompletionGroup: Identifiable, Equatable {
    let id: UUID
    let journeyName: String
    let rows: [LookoutCompletionRow]
    let lastCompletedAt: Date
}

struct LookoutCompletionRow: Identifiable, Equatable {
    let id: String
    let period: ObjectivePeriod
    let completionCount: Int
    let earnedPearls: Int
    let unclaimedPearls: Int
    let lastCompletedAt: Date
}

private extension ObjectivePeriod {
    var order: Int { Self.allCases.firstIndex(of: self) ?? 0 }
}
