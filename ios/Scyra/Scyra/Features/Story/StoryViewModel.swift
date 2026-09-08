import Combine
import Foundation

@MainActor
final class StoryViewModel: ObservableObject {
    @Published private(set) var allSessions: [FlowSession] = []
    @Published private(set) var allPulses: [Pulse] = []
    @Published private(set) var period: StoryPeriod = .week
    @Published private(set) var anchor: Date
    @Published private(set) var selectedJourneys: Set<String> = []
    @Published private(set) var errorMessage: String?
    @Published private(set) var arcMetadataByID: [UUID: ArcMetadata] = [:]

    private let repository: any ScyraRepository
    private let now: () -> Date
    private let calendar: Calendar
    private var lastKnownCurrentAnchor: Date
    private var chronicleSnapshots: [ChronicleOwner: ChronicleSnapshot] = [:]
    private var movementSnapshots: [UUID: FlowHealthSnapshot] = [:]

    init(
        repository: any ScyraRepository,
        now: @escaping () -> Date = Date.init,
        calendar: Calendar? = nil
    ) {
        let resolvedCalendar = calendar ?? StoryCalendarRules.appCalendar()
        self.repository = repository
        self.now = now
        self.calendar = resolvedCalendar
        let currentAnchor = StoryCalendarRules.start(
            of: .week,
            containing: now(),
            calendar: resolvedCalendar
        )
        self.anchor = currentAnchor
        self.lastKnownCurrentAnchor = currentAnchor
        refresh()
    }

    var window: StoryTimeWindow {
        StoryCalendarRules.window(for: anchor, period: period, calendar: calendar)
    }

    var visibleSessions: [FlowSession] {
        allSessions.filter { session in
            window.contains(session.createdAt)
                && (selectedJourneys.isEmpty || selectedJourneys.contains(session.journeyName))
        }
    }

    var visiblePulses: [Pulse] {
        allPulses.filter { pulse in
            window.contains(pulse.createdAt)
                && (selectedJourneys.isEmpty || pulse.journeyName.map(selectedJourneys.contains) == true)
        }
    }

    var journeys: [String] {
        let flowUses = allSessions.map { ($0.journeyName, $0.createdAt) }
        let pulseUses = allPulses.compactMap { pulse in pulse.journeyName.map { ($0, pulse.createdAt) } }
        return Dictionary(grouping: flowUses + pulseUses, by: \.0)
            .map { name, uses in (name, uses.map(\.1).max() ?? .distantPast) }
            .sorted { lhs, rhs in
                if lhs.1 != rhs.1 { return lhs.1 > rhs.1 }
                return lhs.0.localizedCaseInsensitiveCompare(rhs.0) == .orderedAscending
            }
            .map(\.0)
    }

    var totalDurationMs: Int64 { visibleSessions.reduce(0) { $0 + $1.durationMs } }
    var currentScore: Int { visibleSessions.reduce(0) { $0 + $1.scyraPoints } }
    var currentSurgeScore: Int { visibleSessions.reduce(0) { $0 + $1.surgePoints } }
    var firstRecordedAt: Date? {
        (allSessions.map(\.createdAt) + allPulses.map(\.createdAt)).min()
    }

    var isCurrentPeriod: Bool {
        anchor == StoryCalendarRules.start(of: period, containing: now(), calendar: calendar)
    }

    var canGoPrevious: Bool {
        guard let firstRecordedAt else { return false }
        let earliest = StoryCalendarRules.start(of: period, containing: firstRecordedAt, calendar: calendar)
        return StoryCalendarRules.shifted(anchor, period: period, direction: -1, calendar: calendar) >= earliest
    }

    var canGoNext: Bool {
        let current = StoryCalendarRules.start(of: period, containing: now(), calendar: calendar)
        return StoryCalendarRules.shifted(anchor, period: period, direction: 1, calendar: calendar) <= current
    }

    var sagas: [StorySaga] {
        Dictionary(grouping: visibleSessions, by: \FlowSession.journeyName)
            .map { name, sessions in
                StorySaga(
                    journeyName: name,
                    totalScore: sessions.reduce(0) { $0 + $1.scyraPoints },
                    totalDurationMs: sessions.reduce(0) { $0 + $1.durationMs },
                    flowCount: sessions.count
                )
            }
            .sorted {
                if $0.totalScore != $1.totalScore { return $0.totalScore > $1.totalScore }
                if $0.totalDurationMs != $1.totalDurationMs { return $0.totalDurationMs > $1.totalDurationMs }
                return $0.flowCount > $1.flowCount
            }
    }

    var topJourneysLast7Days: [StorySaga] {
        guard isCurrentPeriod else { return [] }
        let start = now().addingTimeInterval(-7 * 24 * 60 * 60)
        return Array(rankJourneys(allSessions.filter { $0.createdAt >= start }).prefix(5))
    }

    var chronicleItems: [StoryChronicleItem] {
        let allByArc = Dictionary(grouping: allSessions.compactMap { flow in
            flow.arcID.map { ($0, flow) }
        }, by: \.0).mapValues { $0.map(\.1) }
        let visible = visibleSessions
        let visiblePulseItems = visiblePulses.map(storyPulseItem)
        let visiblePulsesBySessionID = Dictionary(
            grouping: visiblePulseItems.compactMap { item in
                item.pulse.parentSessionID.map { ($0, item) }
            },
            by: \.0
        ).mapValues { $0.map(\.1) }
        let visibleByArc = Dictionary(grouping: visible.compactMap { flow in
            flow.arcID.map { ($0, flow) }
        }, by: \.0).mapValues { $0.map(\.1) }

        var emittedArcIDs = Set<UUID>()
        var items: [StoryChronicleItem] = []

        for flow in visible {
            guard let arcID = flow.arcID else {
                items.append(.flow(
                    flow,
                    childPulses: visiblePulsesBySessionID[flow.id, default: []]
                        .sorted { $0.pulse.createdAt > $1.pulse.createdAt }
                ))
                continue
            }
            guard emittedArcIDs.insert(arcID).inserted else { continue }

            let allArcFlows = allByArc[arcID, default: []].sorted { $0.createdAt > $1.createdAt }
            let visibleArcFlows = visibleByArc[arcID, default: []].sorted { $0.createdAt > $1.createdAt }
            guard allArcFlows.count >= 2 else {
                items.append(.flow(
                    flow,
                    childPulses: visiblePulsesBySessionID[flow.id, default: []]
                        .sorted { $0.pulse.createdAt > $1.pulse.createdAt }
                ))
                continue
            }

            items.append(.arc(StoryArcGroup(
                id: arcID,
                visibleFlows: visibleArcFlows,
                hiddenFlowCount: max(0, allArcFlows.count - visibleArcFlows.count),
                totalFlowCount: allArcFlows.count,
                totalDurationMs: allArcFlows.reduce(0) { $0 + $1.durationMs },
                totalScore: allArcFlows.reduce(0) { $0 + $1.scyraPoints },
                peakMultiplier: allArcFlows.compactMap(\.arcMultiplierUsed).max(),
                mostRecentAt: visibleArcFlows.map(\.createdAt).max() ?? flow.createdAt,
                childPulsesByFlowID: Dictionary(uniqueKeysWithValues: visibleArcFlows.map { visibleFlow in
                    (
                        visibleFlow.id,
                        visiblePulsesBySessionID[visibleFlow.id, default: []]
                            .sorted { $0.pulse.createdAt > $1.pulse.createdAt }
                    )
                }),
                chroniclesByFlowID: Dictionary(uniqueKeysWithValues: visibleArcFlows.map { visibleFlow in
                    (visibleFlow.id, chronicleForSession(id: visibleFlow.id))
                }),
                movementByFlowID: Dictionary(uniqueKeysWithValues: visibleArcFlows.compactMap { visibleFlow in
                    movementForSession(id: visibleFlow.id).map { (visibleFlow.id, $0) }
                }),
                metadata: arcMetadataByID[arcID]
            )))
        }

        items.append(contentsOf: visiblePulseItems
            .filter { $0.pulse.parentSessionID == nil }
            .map(StoryChronicleItem.pulse))

        return items.sorted { $0.mostRecentAt > $1.mostRecentAt }
    }

    func refresh() {
        let currentAnchor = StoryCalendarRules.start(
            of: period,
            containing: now(),
            calendar: calendar
        )
        if anchor == lastKnownCurrentAnchor { anchor = currentAnchor }
        lastKnownCurrentAnchor = currentAnchor

        do {
            allSessions = try repository.fetchAllSessions()
            allPulses = try repository.fetchAllPulses()
            movementSnapshots = Dictionary(
                uniqueKeysWithValues: try repository.fetchAllMovementSnapshots().map { ($0.sessionID, $0) }
            )
            arcMetadataByID = try repository.fetchAllArcMetadata()
            chronicleSnapshots = [:]
            for session in allSessions {
                let owner = ChronicleOwner.session(session.id)
                chronicleSnapshots[owner] = try repository.fetchChronicle(owner: owner)
            }
            for pulse in allPulses {
                let owner = ChronicleOwner.pulse(pulse.id)
                chronicleSnapshots[owner] = try repository.fetchChronicle(owner: owner)
            }
            let validJourneys = Set(allSessions.map(\.journeyName) + allPulses.compactMap(\.journeyName))
            selectedJourneys.formIntersection(validJourneys)
            clampAnchor()
            errorMessage = nil
        } catch {
            errorMessage = "Scyra couldn't load your Story. Try again."
        }
    }

    func selectPeriod(_ newPeriod: StoryPeriod) {
        period = newPeriod
        anchor = StoryCalendarRules.start(of: newPeriod, containing: now(), calendar: calendar)
        lastKnownCurrentAnchor = anchor
        clampAnchor()
    }

    func goPrevious() {
        guard canGoPrevious else { return }
        anchor = StoryCalendarRules.shifted(anchor, period: period, direction: -1, calendar: calendar)
    }

    func goNext() {
        guard canGoNext else { return }
        anchor = StoryCalendarRules.shifted(anchor, period: period, direction: 1, calendar: calendar)
    }

    func goCurrent() {
        anchor = StoryCalendarRules.start(of: period, containing: now(), calendar: calendar)
        lastKnownCurrentAnchor = anchor
    }

    func toggleJourney(_ journeyName: String) {
        if selectedJourneys.contains(journeyName) {
            selectedJourneys.remove(journeyName)
        } else {
            selectedJourneys.insert(journeyName)
        }
    }

    func clearJourneyFilters() {
        selectedJourneys.removeAll()
    }

    func session(id: String) -> FlowSession? {
        guard let uuid = UUID(uuidString: id) else { return nil }
        return allSessions.first { $0.id == uuid }
    }

    func pulse(id: String) -> Pulse? {
        guard let uuid = UUID(uuidString: id) else { return nil }
        return allPulses.first { $0.id == uuid }
    }

    func chronicleForSession(id: UUID) -> ChronicleSnapshot {
        chronicleSnapshots[.session(id)] ?? .empty(owner: .session(id))
    }

    func chronicleForPulse(id: UUID) -> ChronicleSnapshot {
        chronicleSnapshots[.pulse(id)] ?? .empty(owner: .pulse(id))
    }

    func movementForSession(id: UUID) -> FlowHealthSnapshot? {
        movementSnapshots[id]
    }

    func childPulses(forSessionID id: UUID) -> [StoryPulseItem] {
        allPulses
            .filter { $0.parentSessionID == id }
            .sorted { $0.createdAt > $1.createdAt }
            .map(storyPulseItem)
    }

    func sessions(forJourney journeyName: String) -> [FlowSession] {
        visibleSessions
            .filter { $0.journeyName == journeyName }
            .sorted { $0.createdAt > $1.createdAt }
    }

    @discardableResult
    func createHistoricalPulse(
        parentSessionID: UUID,
        title: String,
        description: String,
        journeyName: String?
    ) -> Bool {
        do {
            _ = try repository.createHistoricalPulse(
                parentSessionID: parentSessionID,
                title: title,
                description: description,
                journeyName: journeyName,
                createdAt: now()
            )
            refresh()
            return true
        } catch {
            errorMessage = "Pulse couldn't be saved"
            return false
        }
    }

    @discardableResult
    func deleteSession(id: UUID) -> Bool {
        do {
            try repository.deleteSession(id: id, detachedAt: now())
            refresh()
            return true
        } catch {
            errorMessage = "Flow couldn't be deleted"
            return false
        }
    }

    @discardableResult
    func saveArcMetadata(_ metadata: ArcMetadata) -> Bool {
        guard (metadata.title?.count ?? 0) <= ArcMetadata.titleLimit,
              (metadata.summary?.count ?? 0) <= ArcMetadata.summaryLimit,
              (metadata.outcome?.count ?? 0) <= ArcMetadata.reflectionLimit,
              (metadata.highlight?.count ?? 0) <= ArcMetadata.reflectionLimit,
              (metadata.nextStep?.count ?? 0) <= ArcMetadata.reflectionLimit else {
            errorMessage = "Arc details are too long"
            return false
        }
        do {
            try repository.saveArcMetadata(metadata, updatedAt: now())
            refresh()
            return true
        } catch {
            errorMessage = "Couldn't save Arc details. Try again."
            return false
        }
    }

    func deletePulse(id: UUID) {
        do {
            try repository.deletePulse(id: id)
            refresh()
        } catch {
            errorMessage = PulseStrings.saveError
        }
    }

    private func clampAnchor() {
        let maximum = StoryCalendarRules.start(of: period, containing: now(), calendar: calendar)
        let minimum = firstRecordedAt.map {
            StoryCalendarRules.start(of: period, containing: $0, calendar: calendar)
        } ?? maximum
        anchor = min(max(anchor, minimum), maximum)
    }

    private func storyPulseItem(_ pulse: Pulse) -> StoryPulseItem {
        StoryPulseItem(pulse: pulse, chronicle: chronicleForPulse(id: pulse.id))
    }

    private func rankJourneys(_ sessions: [FlowSession]) -> [StorySaga] {
        Dictionary(grouping: sessions, by: \FlowSession.journeyName)
            .map { name, values in
                StorySaga(
                    journeyName: name,
                    totalScore: values.reduce(0) { $0 + $1.scyraPoints },
                    totalDurationMs: values.reduce(0) { $0 + $1.durationMs },
                    flowCount: values.count
                )
            }
            .sorted {
                if $0.totalScore != $1.totalScore { return $0.totalScore > $1.totalScore }
                if $0.totalDurationMs != $1.totalDurationMs { return $0.totalDurationMs > $1.totalDurationMs }
                return $0.flowCount > $1.flowCount
            }
    }
}
