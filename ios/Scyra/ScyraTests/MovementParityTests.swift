import Foundation
import Testing
@testable import Scyra

@MainActor
struct MovementDomainParityTests {
    @Test(arguments: [
        (Int64(-1), Int64(0)),
        (Int64(0), Int64(0)),
        (Int64(99), Int64(0)),
        (Int64(100), Int64(1)),
        (Int64(342), Int64(3)),
        (Int64(1_000), Int64(10))
    ])
    func movementUsesTheCanonicalHundredStepsPerPoint(steps: Int64, expected: Int64) {
        #expect(MovementBonusCalculator.points(for: steps) == expected)
    }

    @Test func eligibilityRequiresEveryStartTimeCondition() {
        let eligible = MovementBonusEligibilityInput(
            movementBonusEnabled: true,
            healthDataAvailable: true,
            healthAccessRequested: true,
            isRegularPointEligibleFlow: true,
            isSoftFlow: false
        )
        #expect(MovementBonusEligibilityPolicy.isEligible(eligible))

        let ineligible = [
            MovementBonusEligibilityInput(movementBonusEnabled: false, healthDataAvailable: true, healthAccessRequested: true, isRegularPointEligibleFlow: true, isSoftFlow: false),
            MovementBonusEligibilityInput(movementBonusEnabled: true, healthDataAvailable: false, healthAccessRequested: true, isRegularPointEligibleFlow: true, isSoftFlow: false),
            MovementBonusEligibilityInput(movementBonusEnabled: true, healthDataAvailable: true, healthAccessRequested: false, isRegularPointEligibleFlow: true, isSoftFlow: false),
            MovementBonusEligibilityInput(movementBonusEnabled: true, healthDataAvailable: true, healthAccessRequested: true, isRegularPointEligibleFlow: false, isSoftFlow: false),
            MovementBonusEligibilityInput(movementBonusEnabled: true, healthDataAvailable: true, healthAccessRequested: true, isRegularPointEligibleFlow: true, isSoftFlow: true)
        ]
        #expect(ineligible.allSatisfy { !MovementBonusEligibilityPolicy.isEligible($0) })
    }

    @Test func activeIntervalsDiscardInvalidRangesAndMergeOverlapOrAdjacency() {
        let origin = Date(timeIntervalSince1970: 1_000)
        let normalized = FlowActiveIntervalNormalizer.normalize([
            interval(origin, 30, 40),
            interval(origin, 0, 10),
            interval(origin, 10, 20),
            interval(origin, 15, 35),
            interval(origin, 50, 50),
            interval(origin, 80, 70)
        ])

        #expect(normalized == [interval(origin, 0, 40)])
    }

    @Test func aggregationSumsSuccessesButPropagatesTerminalFailures() async {
        let origin = Date(timeIntervalSince1970: 2_000)
        let intervals = [interval(origin, 0, 10), interval(origin, 20, 30)]
        var results: [MovementReadResult] = [.success(steps: 120), .success(steps: 222)]
        let total = await MovementStepAggregator.read(intervals: intervals) { _ in results.removeFirst() }
        #expect(total == .success(steps: 342))

        results = [.success(steps: 120), .permissionMissing]
        let denied = await MovementStepAggregator.read(intervals: intervals) { _ in results.removeFirst() }
        #expect(denied == .permissionMissing)
    }

    @Test func movementIsAddedBeforeTheArcMultiplier() {
        let result = MovementRewardRecalculator.calculate(
            sessionID: UUID(),
            nonMovementPreMultiplierPoints: 42,
            movementPoints: 13,
            arcMultiplier: 1.2,
            pearlEligible: true
        )

        #expect(result.preMultiplierTotal == 55)
        #expect(result.arcBonusPoints == 11)
        #expect(result.finalScyraPoints == 66)
        #expect(result.pearlsEarned == 66)
    }

    @Test func delayedSyncCanIncreaseButNeverSubtractRewards() {
        let id = UUID()
        let context = StoredMovementRewardContext(
            sessionID: id,
            nonMovementPreMultiplierPoints: 20,
            pulseBonusPoints: 0,
            surgeBonusPoints: 0,
            otherPreMultiplierBonusPoints: 0,
            existingMovementPoints: 3,
            oldFinalScyraPoints: 28,
            arcMultiplier: 1.2,
            streakMultiplier: 1,
            otherMultiplier: 1,
            pearlEligible: true
        )

        let lower = DelayedMovementRewardPolicy.calculate(steps: 100, context: context)
        #expect(lower.newRawMovementPoints == 3)
        #expect(lower.newFinalScyraPoints == 28)
        #expect(lower.deltaScyraPoints == 0)

        let higher = DelayedMovementRewardPolicy.calculate(steps: 1_000, context: context)
        #expect(higher.newRawMovementPoints == 10)
        #expect(higher.newFinalScyraPoints == 36)
        #expect(higher.deltaScyraPoints == 8)
        #expect(higher.pearlDelta == 8)
    }

    private func interval(_ origin: Date, _ start: TimeInterval, _ end: TimeInterval) -> FlowActiveInterval {
        FlowActiveInterval(
            start: origin.addingTimeInterval(start),
            end: origin.addingTimeInterval(end)
        )
    }
}

@MainActor
struct MovementFlowParityTests {
    @Test func completionFreezesEditableFlowStateWhileHealthReadIsPending() async throws {
        let repository = InMemoryFlowRepository()
        let clock = MovementTestClock(Date(timeIntervalSince1970: 9_000))
        let dataSource = DeferredMovementTestDataSource()
        let controller = MovementController(
            settings: InMemoryMovementSettingsStore(isEnabled: true, accessWasRequested: true),
            dataSource: dataSource,
            repository: repository,
            now: { clock.value }
        )
        let viewModel = FlowViewModel(
            repository: repository,
            movementController: controller,
            now: { clock.value }
        )
        viewModel.updateTitle("Original title")
        viewModel.updateJourneyName("Original journey")
        viewModel.enterFlowMode()
        clock.advance(seconds: 60)
        viewModel.exitFlowMode()

        viewModel.complete(.saveFlow)
        while !dataSource.isWaiting { await Task.yield() }

        viewModel.updateTitle("Late title")
        viewModel.updateJourneyName("Late journey")
        viewModel.updateChronicleDraft("Late Chronicle mutation")
        viewModel.enterFlowMode()

        #expect(viewModel.title == "Original title")
        #expect(viewModel.journeyName == "Original journey")
        #expect(viewModel.chronicle.draftText.isEmpty)
        #expect(!viewModel.isInFlowMode)

        dataSource.resolve(.success(steps: 100))
        while viewModel.isSaving { await Task.yield() }

        let session = try #require(repository.fetchAllSessions().first)
        #expect(session.title == "Original title")
        #expect(session.journeyName == "Original journey")
    }

    @Test func completionReadsOnlyActiveFlowIntervalsAndPersistsReward() async throws {
        let repository = InMemoryFlowRepository()
        let clock = MovementTestClock(Date(timeIntervalSince1970: 10_000))
        let dataSource = MovementTestDataSource(results: [.success(steps: 150), .success(steps: 150)])
        let controller = MovementController(
            settings: InMemoryMovementSettingsStore(isEnabled: true, accessWasRequested: true),
            dataSource: dataSource,
            repository: repository,
            now: { clock.value }
        )
        let viewModel = FlowViewModel(
            repository: repository,
            movementController: controller,
            now: { clock.value }
        )
        viewModel.updateTitle("Movement parity")
        viewModel.updateJourneyName("Scyra")

        viewModel.enterFlowMode()
        #expect(viewModel.movementBonusEligibleAtStart)
        clock.advance(seconds: 60)
        viewModel.exitFlowMode()
        clock.advance(seconds: 60)
        viewModel.enterFlowMode()
        clock.advance(seconds: 60)
        viewModel.exitFlowMode()
        viewModel.complete(.saveFlow)
        while viewModel.isSaving { await Task.yield() }

        let reward = try #require(viewModel.reward)
        #expect(reward.movementSteps == 300)
        #expect(reward.movementPoints == 3)
        #expect(reward.finalScyraPoints == 5)
        let session = try #require(repository.fetchAllSessions().first)
        #expect(session.durationMs == 120_000)
        #expect(session.scyraPoints == 5)
        let snapshot = try #require(repository.fetchMovementSnapshot(sessionID: session.id))
        #expect(snapshot.status == .captured)
        #expect(snapshot.activeIntervals.count == 2)
        #expect(dataSource.readRanges == snapshot.activeIntervals)
    }

    @Test func foregroundRefreshUpdatesSessionAndStoryWithoutRewardRegression() async throws {
        let repository = InMemoryFlowRepository()
        let completedAt = Date(timeIntervalSince1970: 20_000)
        let session = makeSession(id: UUID(), endTime: completedAt, points: 20)
        let snapshot = makeSnapshot(session: session, status: .pending, steps: nil, movementPoints: 0)
        let breakdown = MovementRewardRecalculator.calculate(
            sessionID: session.id,
            nonMovementPreMultiplierPoints: 20,
            movementPoints: 0,
            arcMultiplier: 1,
            pearlEligible: true
        )
        _ = repository.commit(
            session: session,
            activeArc: nil,
            recentlyEndedArc: nil,
            movement: FlowMovementCompletion(snapshot: snapshot, rewardBreakdown: breakdown)
        )

        let dataSource = MovementTestDataSource(results: [.success(steps: 342)])
        let controller = MovementController(
            settings: InMemoryMovementSettingsStore(isEnabled: true, accessWasRequested: true),
            dataSource: dataSource,
            repository: repository,
            now: { completedAt.addingTimeInterval(3_600) }
        )
        await controller.refreshForeground()

        let refreshedSession = try #require(repository.fetchAllSessions().first)
        let refreshed = try #require(repository.fetchMovementSnapshot(sessionID: session.id))
        #expect(refreshedSession.scyraPoints == 23)
        #expect(refreshed.rawMovementPoints == 3)
        #expect(refreshed.updatedAfterSync)
        #expect(refreshed.status == .captured)

        let story = StoryViewModel(repository: repository, now: { completedAt.addingTimeInterval(3_600) })
        #expect(story.movementForSession(id: session.id) == refreshed)
    }

    @Test func swiftDataCommitRoundTripsMovementSnapshotAndBreakdownAtomically() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let completedAt = Date(timeIntervalSince1970: 30_000)
        let session = makeSession(id: UUID(), endTime: completedAt, points: 23)
        let snapshot = makeSnapshot(
            session: session,
            status: .captured,
            steps: 342,
            movementPoints: 3
        )
        let breakdown = MovementRewardRecalculator.calculate(
            sessionID: session.id,
            nonMovementPreMultiplierPoints: 20,
            movementPoints: 3,
            arcMultiplier: 1,
            pearlEligible: true
        )

        _ = try repository.commit(
            session: session,
            activeArc: nil,
            recentlyEndedArc: nil,
            movement: FlowMovementCompletion(snapshot: snapshot, rewardBreakdown: breakdown)
        )
        let recreated = SwiftDataFlowRepository(container: container)

        #expect(try recreated.fetchAllSessions() == [session])
        #expect(try recreated.fetchMovementSnapshot(sessionID: session.id) == snapshot)
        #expect(try recreated.fetchMovementRewardBreakdown(sessionID: session.id) == breakdown)
    }

    @Test func idempotentCommitKeepsMovementAndArcStateOnTheCanonicalSession() throws {
        let completedAt = Date(timeIntervalSince1970: 40_000)

        let inMemory = InMemoryFlowRepository()
        try assertIdempotentCommit(repository: inMemory, completedAt: completedAt)

        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let swiftData = SwiftDataFlowRepository(container: container)
        try assertIdempotentCommit(repository: swiftData, completedAt: completedAt)
    }

    private func assertIdempotentCommit(
        repository: any FlowRepository,
        completedAt: Date
    ) throws {
        let flowInstanceID = UUID()
        let original = makeSession(
            id: UUID(),
            flowInstanceID: flowInstanceID,
            endTime: completedAt,
            points: 20
        )
        let retry = makeSession(
            id: UUID(),
            flowInstanceID: flowInstanceID,
            endTime: completedAt,
            points: 23
        )
        let firstArc = ArcRuntimeState(
            id: UUID(),
            isPending: false,
            multiplier: 1.2,
            progressMs: 0,
            lastSessionEndTime: completedAt,
            sessionCount: 2,
            pauseUsedMs: 0,
            pauseStartedAt: nil
        )
        let incorrectlyAdvancedArc = ArcRuntimeState(
            id: firstArc.id,
            isPending: false,
            multiplier: 1.3,
            progressMs: 0,
            lastSessionEndTime: completedAt,
            sessionCount: 3,
            pauseUsedMs: 0,
            pauseStartedAt: nil
        )

        _ = try repository.commit(
            session: original,
            activeArc: firstArc,
            recentlyEndedArc: nil,
            movement: movementCompletion(session: original, points: 0)
        )
        let committed = try repository.commit(
            session: retry,
            activeArc: incorrectlyAdvancedArc,
            recentlyEndedArc: nil,
            movement: movementCompletion(session: retry, points: 3)
        )

        #expect(committed.id == original.id)
        #expect(try repository.fetchAllSessions() == [original])
        #expect(try repository.fetchActiveArc() == firstArc)
        #expect(try repository.fetchMovementSnapshot(sessionID: retry.id) == nil)
        #expect(try repository.fetchMovementRewardBreakdown(sessionID: retry.id) == nil)
        #expect(try repository.fetchMovementSnapshot(sessionID: original.id)?.rawMovementPoints == 0)
        #expect(try repository.fetchMovementRewardBreakdown(sessionID: original.id)?.movementPoints == 0)
    }

    private func movementCompletion(session: FlowSession, points: Int64) -> FlowMovementCompletion {
        let snapshot = makeSnapshot(
            session: session,
            status: .captured,
            steps: points * MovementBonusCalculator.stepsPerPoint,
            movementPoints: points
        )
        let breakdown = MovementRewardRecalculator.calculate(
            sessionID: session.id,
            nonMovementPreMultiplierPoints: 20,
            movementPoints: points,
            arcMultiplier: 1,
            pearlEligible: true
        )
        return FlowMovementCompletion(snapshot: snapshot, rewardBreakdown: breakdown)
    }

    private func makeSession(
        id: UUID,
        flowInstanceID: UUID = UUID(),
        endTime: Date,
        points: Int
    ) -> FlowSession {
        FlowSession(
            id: id,
            flowInstanceID: flowInstanceID,
            title: "Walk and focus",
            description: "",
            journeyName: "Scyra",
            startTime: endTime.addingTimeInterval(-60),
            endTime: endTime,
            durationMs: 60_000,
            surgePlannedMs: nil,
            surgePoints: 0,
            scyraPoints: points,
            isSoftMode: false,
            arcID: nil,
            arcIndex: nil,
            arcMultiplierUsed: nil,
            arcBonusPoints: 0,
            createdAt: endTime
        )
    }

    private func makeSnapshot(
        session: FlowSession,
        status: FlowHealthSyncStatus,
        steps: Int64?,
        movementPoints: Int64
    ) -> FlowHealthSnapshot {
        FlowHealthSnapshot(
            sessionID: session.id,
            healthEnabledAtStart: true,
            accessRequestedAtStart: true,
            status: status,
            steps: steps,
            rawMovementPoints: movementPoints,
            finalMovementScyraContribution: movementPoints,
            finalMovementPearlContribution: movementPoints,
            firstCheckedAt: status == .pending ? nil : session.endTime,
            lastCheckedAt: status == .pending ? nil : session.endTime,
            capturedAt: status == .captured ? session.endTime : nil,
            expiresAt: session.endTime.addingTimeInterval(MovementRefreshPolicy.refreshWindow),
            checkCount: status == .pending ? 0 : 1,
            flowStartTime: session.startTime,
            flowEndTime: session.endTime,
            activeIntervals: [FlowActiveInterval(start: session.startTime, end: session.endTime)],
            sourceLabel: "Apple Health",
            updatedAfterSync: false
        )
    }
}

@MainActor
private final class MovementTestDataSource: MovementDataSource {
    var isHealthDataAvailable = true
    private var results: [MovementReadResult]
    private(set) var readRanges: [FlowActiveInterval] = []

    init(results: [MovementReadResult]) {
        self.results = results
    }

    func requestAccess() async throws {}

    func readSteps(between start: Date, and end: Date) async -> MovementReadResult {
        readRanges.append(FlowActiveInterval(start: start, end: end))
        return results.isEmpty ? .noData : results.removeFirst()
    }
}

@MainActor
private final class DeferredMovementTestDataSource: MovementDataSource {
    let isHealthDataAvailable = true
    private var continuation: CheckedContinuation<MovementReadResult, Never>?

    var isWaiting: Bool { continuation != nil }

    func requestAccess() async throws {}

    func readSteps(between start: Date, and end: Date) async -> MovementReadResult {
        await withCheckedContinuation { continuation = $0 }
    }

    func resolve(_ result: MovementReadResult) {
        continuation?.resume(returning: result)
        continuation = nil
    }
}

@MainActor
private final class MovementTestClock {
    private(set) var value: Date

    init(_ value: Date) {
        self.value = value
    }

    func advance(seconds: TimeInterval) {
        value = value.addingTimeInterval(seconds)
    }
}
