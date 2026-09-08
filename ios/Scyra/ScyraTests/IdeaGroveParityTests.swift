import Foundation
import Testing
@testable import Scyra

@MainActor
struct IdeaGroveDomainParityTests {
    @Test func durationFormattingMatchesAndroidCompactAndSpeechRules() {
        #expect(IdeaGroveDurationFormatter.compact(24 * 60_000 + 12_000) == "24m 12s")
        #expect(IdeaGroveDurationFormatter.compact(3_820_000) == "1h 3m 40s")
        #expect(IdeaGroveDurationFormatter.compact(19_080_000) == "5h 18m")
        #expect(IdeaGroveDurationFormatter.spoken(3_820_000) == "1 hour 3 minutes 40 seconds")
    }

    @Test func meaningfulAndAbandonedActiveFlowRulesMatchAndroid() {
        let origin = UUID()
        let idle = snapshot(originPulseID: nil)
        let pulseDraft = snapshot(originPulseID: origin)

        #expect(!idle.isMeaningfulActiveFlow)
        #expect(!idle.isAbandonedPulseOriginDraft)
        #expect(!pulseDraft.isMeaningfulActiveFlow)
        #expect(pulseDraft.isAbandonedPulseOriginDraft)
        #expect(snapshot(isRunning: true).isMeaningfulActiveFlow)
        #expect(snapshot(isInFlowMode: true).isMeaningfulActiveFlow)
        #expect(snapshot(accumulated: 1).isMeaningfulActiveFlow)
        #expect(snapshot(segmentStartedAt: .now).isMeaningfulActiveFlow)
        #expect(snapshot(firstStartedAt: .now).isMeaningfulActiveFlow)
    }

    @Test func recentsAndTimeSortsMatchAndroidTieBreakers() {
        let base = Date(timeIntervalSince1970: 10_000)
        let raw = item(title: "Raw", createdAt: base.addingTimeInterval(30), duration: 0, lastWorkedAt: nil)
        let olderWorked = item(title: "Older", createdAt: base, duration: 120_000, lastWorkedAt: base.addingTimeInterval(10))
        let recentWorked = item(title: "Recent", createdAt: base, duration: 60_000, lastWorkedAt: base.addingTimeInterval(20))

        #expect(IdeaGroveViewModel.sortAlive([raw, olderWorked, recentWorked], by: .recents).map(\.title) == ["Recent", "Older", "Raw"])
        #expect(IdeaGroveViewModel.sortAlive([raw, olderWorked, recentWorked], by: .mostTime).map(\.title) == ["Older", "Recent", "Raw"])
        #expect(IdeaGroveViewModel.sortAlive([raw, olderWorked, recentWorked], by: .leastTime).map(\.title) == ["Raw", "Recent", "Older"])
    }

    private func snapshot(
        isRunning: Bool = false,
        isInFlowMode: Bool = false,
        accumulated: Int64 = 0,
        segmentStartedAt: Date? = nil,
        firstStartedAt: Date? = nil,
        originPulseID: UUID? = nil
    ) -> ActiveFlowSnapshot {
        ActiveFlowSnapshot(
            flowInstanceID: UUID(),
            title: "",
            journeyName: "",
            mode: .flow,
            isInFlowMode: isInFlowMode,
            isRunning: isRunning,
            accumulatedDurationMs: accumulated,
            segmentStartedAt: segmentStartedAt,
            firstStartedAt: firstStartedAt,
            surgePlannedMs: nil,
            originPulseID: originPulseID,
            createdAt: .now
        )
    }

    private func item(
        title: String,
        createdAt: Date,
        duration: Int64,
        lastWorkedAt: Date?
    ) -> IdeaGroveItem {
        IdeaGroveItem(
            pulseID: UUID(),
            type: duration > 0 ? .idea : .rawPulse,
            title: title,
            description: "",
            journeyName: nil,
            createdAt: createdAt,
            updatedAt: createdAt,
            groveStatus: .alive,
            groveStatusChangedAt: nil,
            flowCount: duration > 0 ? 1 : 0,
            totalFlowDurationMs: duration,
            lastWorkedAt: lastWorkedAt,
            flows: [],
            wasCapturedDuringFlow: false
        )
    }
}

@MainActor
struct IdeaGroveRepositoryParityTests {
    @Test func statusGuardsAutoRevivalAndDeleteCleanupMatchAndroid() throws {
        let repository = InMemoryFlowRepository()
        let createdAt = Date(timeIntervalSince1970: 20_000)
        let pulse = try makePulse(repository: repository, title: "Build this", createdAt: createdAt)

        repository.markPulseCompleted(id: pulse.id, changedAt: createdAt.addingTimeInterval(1))
        #expect(repository.fetchPulse(id: pulse.id)?.groveStatus == .alive)

        repository.markPulseAsInsight(id: pulse.id, changedAt: createdAt.addingTimeInterval(2))
        #expect(repository.fetchPulse(id: pulse.id)?.groveStatus == .insight)

        let session = makeSession(createdAt: createdAt.addingTimeInterval(60))
        _ = repository.commit(
            session: session,
            activeArc: nil,
            recentlyEndedArc: nil,
            movement: nil,
            originPulseID: pulse.id
        )
        #expect(repository.fetchPulseFlowLinks(pulseID: pulse.id).map(\.sessionID) == [session.id])
        #expect(repository.fetchPulse(id: pulse.id)?.groveStatus == .alive)

        repository.markPulseAsInsight(id: pulse.id, changedAt: createdAt.addingTimeInterval(61))
        #expect(repository.fetchPulse(id: pulse.id)?.groveStatus == .alive)
        repository.markPulseCompleted(id: pulse.id, changedAt: createdAt.addingTimeInterval(62))
        #expect(repository.fetchPulse(id: pulse.id)?.groveStatus == .completed)

        repository.deletePulse(id: pulse.id)
        #expect(repository.fetchPulse(id: pulse.id) == nil)
        #expect(repository.fetchPulseFlowLinks(pulseID: pulse.id).isEmpty)
        #expect(repository.fetchAllSessions() == [session])
    }

    @Test func swiftDataCommitAtomicallyLinksFlowRevivesInsightAndSurvivesRecreation() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let createdAt = Date(timeIntervalSince1970: 30_000)
        let pulse = try makePulse(repository: repository, title: "Durable idea", createdAt: createdAt)
        try repository.markPulseAsInsight(id: pulse.id, changedAt: createdAt.addingTimeInterval(1))

        let session = makeSession(createdAt: createdAt.addingTimeInterval(120))
        _ = try repository.addChronicleText(owner: .activeFlow(session.flowInstanceID), text: "Flow result")
        let committed = try repository.commit(
            session: session,
            activeArc: nil,
            recentlyEndedArc: nil,
            movement: nil,
            originPulseID: pulse.id
        )

        let recreated = SwiftDataFlowRepository(container: container)
        #expect(try recreated.fetchPulseFlowLinks(pulseID: pulse.id).map(\.sessionID) == [committed.id])
        #expect(try recreated.fetchPulse(id: pulse.id)?.groveStatus == .alive)
        let item = try #require(recreated.fetchIdeaGroveItems().first { $0.id == pulse.id })
        #expect(item.type == .idea)
        #expect(item.flowCount == 1)
        #expect(item.totalFlowDurationMs == session.durationMs)
        #expect(item.description == "Durable idea")
        #expect(item.flows.first?.description == "Flow result")
    }

    @Test func duplicateCommitCannotRelinkRetryToAnotherPulse() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let createdAt = Date(timeIntervalSince1970: 40_000)
        let first = try makePulse(repository: repository, title: "First", createdAt: createdAt)
        let second = try makePulse(repository: repository, title: "Second", createdAt: createdAt.addingTimeInterval(1))
        let session = makeSession(createdAt: createdAt.addingTimeInterval(60))

        _ = try repository.commit(
            session: session,
            activeArc: nil,
            recentlyEndedArc: nil,
            movement: nil,
            originPulseID: first.id
        )
        let retry = makeSession(
            id: UUID(),
            flowInstanceID: session.flowInstanceID,
            createdAt: createdAt.addingTimeInterval(61)
        )
        _ = try repository.commit(
            session: retry,
            activeArc: nil,
            recentlyEndedArc: nil,
            movement: nil,
            originPulseID: second.id
        )

        #expect(try repository.fetchPulseFlowLinks(pulseID: first.id).count == 1)
        #expect(try repository.fetchPulseFlowLinks(pulseID: second.id).isEmpty)
        #expect(try repository.fetchAllSessions().count == 1)
    }

    @Test func swiftDataRoundTripsPulseOriginDraftFields() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let pulseID = UUID()
        let snapshot = ActiveFlowSnapshot(
            flowInstanceID: UUID(),
            title: "Origin title",
            journeyName: "Ideas",
            mode: .flow,
            isInFlowMode: true,
            isRunning: true,
            accumulatedDurationMs: 12_000,
            segmentStartedAt: Date(timeIntervalSince1970: 49_990),
            firstStartedAt: Date(timeIntervalSince1970: 49_900),
            surgePlannedMs: nil,
            originPulseID: pulseID,
            originPulseTitle: "Origin title",
            originPulseJourneyName: "Ideas",
            createdAt: Date(timeIntervalSince1970: 49_900)
        )

        try repository.saveActiveFlow(snapshot)
        let recreated = SwiftDataFlowRepository(container: container)
        #expect(try recreated.fetchActiveFlow() == snapshot)
    }

    @Test func viewModelClearsAbandonedOriginButBlocksMeaningfulFlow() throws {
        let repository = InMemoryFlowRepository()
        let pulse = try makePulse(repository: repository, title: "Launch", createdAt: .now)
        repository.saveActiveFlow(ActiveFlowSnapshot(
            flowInstanceID: UUID(),
            title: pulse.title,
            journeyName: "Ideas",
            mode: .flow,
            isInFlowMode: false,
            isRunning: false,
            accumulatedDurationMs: 0,
            segmentStartedAt: nil,
            firstStartedAt: nil,
            surgePlannedMs: nil,
            originPulseID: pulse.id,
            originPulseTitle: pulse.title,
            originPulseJourneyName: "Ideas",
            createdAt: .now
        ))

        let viewModel = IdeaGroveViewModel(repository: repository)
        #expect(repository.fetchActiveFlow() == nil)
        if case .launch(let context) = viewModel.requestFlow(from: pulse.id) {
            #expect(context.pulseID == pulse.id)
        } else {
            Issue.record("Expected Pulse launch context")
        }

        repository.saveActiveFlow(ActiveFlowSnapshot(
            flowInstanceID: UUID(),
            title: "Running",
            journeyName: "Ideas",
            mode: .flow,
            isInFlowMode: true,
            isRunning: true,
            accumulatedDurationMs: 10,
            segmentStartedAt: .now,
            firstStartedAt: .now,
            surgePlannedMs: nil,
            createdAt: .now
        ))
        #expect(viewModel.requestFlow(from: pulse.id) == .activeFlowConflict)
        #expect(viewModel.showsActiveFlowConflict)
    }

    private func makePulse(
        repository: any ScyraRepository,
        title: String,
        createdAt: Date
    ) throws -> Pulse {
        let creationKey = UUID()
        _ = try repository.addChronicleText(owner: .pulseDraft(creationKey), text: title)
        return try repository.createPulse(
            creationKey: creationKey,
            title: title,
            journeyName: "Ideas",
            parentFlowInstanceID: nil,
            arcID: nil,
            createdAt: createdAt
        )
    }

    private func makeSession(
        id: UUID = UUID(),
        flowInstanceID: UUID = UUID(),
        createdAt: Date
    ) -> FlowSession {
        FlowSession(
            id: id,
            flowInstanceID: flowInstanceID,
            title: "Work the idea",
            description: "",
            journeyName: "Ideas",
            startTime: createdAt.addingTimeInterval(-60),
            endTime: createdAt,
            durationMs: 60_000,
            surgePlannedMs: nil,
            surgePoints: 0,
            scyraPoints: 1,
            isSoftMode: false,
            arcID: nil,
            arcIndex: nil,
            arcMultiplierUsed: nil,
            arcBonusPoints: 0,
            createdAt: createdAt
        )
    }
}

@MainActor
struct IdeaGroveFlowIntegrationParityTests {
    @Test func pulseOriginPersistsAndCompletionCreatesLink() throws {
        let repository = InMemoryFlowRepository()
        let clock = IdeaGroveTestClock(Date(timeIntervalSince1970: 50_000))
        let key = UUID()
        _ = try repository.addChronicleText(owner: .pulseDraft(key), text: "Grow this")
        let pulse = try repository.createPulse(
            creationKey: key,
            title: "Grow this",
            journeyName: "Ideas",
            parentFlowInstanceID: nil,
            arcID: nil,
            createdAt: clock.value
        )
        let context = try #require(repository.fetchPulseLaunchContext(id: pulse.id))
        let flow = FlowViewModel(repository: repository, now: { clock.value })

        #expect(flow.prepareFromPulse(context))
        #expect(repository.fetchActiveFlow()?.originPulseID == pulse.id)
        #expect(flow.originPulseTitle == "Grow this")
        flow.enterFlowMode()
        clock.advance(seconds: 60)
        flow.refreshElapsed()
        flow.exitFlowMode()
        flow.complete(.saveFlow)

        let session = try #require(repository.fetchAllSessions().first)
        #expect(repository.fetchPulseFlowLinks(pulseID: pulse.id).map(\.sessionID) == [session.id])
    }

    @Test func continuingArcOffersCanonicalIdeaContinuationChoice() throws {
        let repository = InMemoryFlowRepository()
        let clock = IdeaGroveTestClock(Date(timeIntervalSince1970: 60_000))
        let key = UUID()
        _ = try repository.addChronicleText(owner: .pulseDraft(key), text: "Arc idea")
        let pulse = try repository.createPulse(
            creationKey: key,
            title: "Arc idea",
            journeyName: "Ideas",
            parentFlowInstanceID: nil,
            arcID: nil,
            createdAt: clock.value
        )
        let context = try #require(repository.fetchPulseLaunchContext(id: pulse.id))
        let flow = FlowViewModel(repository: repository, now: { clock.value })
        #expect(flow.prepareFromPulse(context))
        flow.enterFlowMode()
        clock.advance(seconds: 60)
        flow.refreshElapsed()
        flow.exitFlowMode()
        flow.complete(.continueArc)

        #expect(!flow.finishReward())
        #expect(flow.pendingIdeaContinuation?.pulseID == pulse.id)
        #expect(repository.fetchActiveFlow()?.originPulseID == nil)
        flow.resolveIdeaContinuation(includeIdea: true)
        #expect(flow.pendingIdeaContinuation == nil)
        #expect(repository.fetchActiveFlow()?.originPulseID == pulse.id)
    }
}

@MainActor
private final class IdeaGroveTestClock {
    private(set) var value: Date
    init(_ value: Date) { self.value = value }
    func advance(seconds: TimeInterval) { value = value.addingTimeInterval(seconds) }
}
