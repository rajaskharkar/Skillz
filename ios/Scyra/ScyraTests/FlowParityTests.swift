import Foundation
import Testing
@testable import Scyra

@MainActor
struct ScoreCalculatorParityTests {
    @Test(arguments: [
        (9, 9),
        (10, 15),
        (29, 39),
        (30, 55),
        (59, 94),
        (60, 145),
        (90, 200)
    ])
    func scoreBreakpointsMatchAndroid(minutes: Int, expected: Int) {
        let result = ScoreCalculator.breakdown(
            durationMs: Int64(minutes) * ScoreCalculator.millisPerMinute
        )
        #expect(result.totalPoints == expected)
    }

    @Test func scoreUsesCompletedMinutesOnly() {
        let result = ScoreCalculator.breakdown(durationMs: 10 * 60_000 - 1)
        #expect(result.minutes == 9)
        #expect(result.totalPoints == 9)
    }

    @Test func exactSurgeTargetGetsMaximumCurveBonus() {
        let points = ScoreCalculator.surgePoints(
            plannedMs: 30 * 60_000,
            actualDurationMs: 30 * 60_000
        )
        #expect(points == 41)
    }

    @Test func overtimeSurgeIsCappedAtPlannedMinutes() {
        let points = ScoreCalculator.surgePoints(
            plannedMs: 30 * 60_000,
            actualDurationMs: 31 * 60_000
        )
        #expect(points == 30)
    }

    @Test func arcMathMatchesAndroidTiersAndProgression() {
        let result = ScoreCalculator.arcMath(
            beforeArcPoints: 100,
            chainBase: 1.3,
            durationMs: 30 * 60_000
        )
        #expect(abs(result.arcMultiplierUsed - 1.4) < 0.000_001)
        #expect(result.arcBonusPoints == 40)
        #expect(result.finalPoints == 140)
        #expect(abs(result.nextChainBase - 1.4) < 0.000_001)
        #expect(result.didLevelUp)
    }
}

@MainActor
struct ArcContinuationParityTests {
    private let end = Date(timeIntervalSince1970: 1_000)

    @Test func continuationWindowIncludesExactlyFiveMinutes() {
        let arc = makeArc()
        #expect(ArcContinuationResolver.resolve(
            activeArc: nil,
            recentlyEndedArc: arc,
            flowStartTime: end.addingTimeInterval(5 * 60)
        ) == arc)
    }

    @Test func continuationWindowRejectsTheNextMillisecond() {
        #expect(ArcContinuationResolver.resolve(
            activeArc: nil,
            recentlyEndedArc: makeArc(),
            flowStartTime: end.addingTimeInterval(5 * 60 + 0.001)
        ) == nil)
    }

    @Test func futureArcEndIsRejected() {
        #expect(ArcContinuationResolver.resolve(
            activeArc: nil,
            recentlyEndedArc: makeArc(),
            flowStartTime: end.addingTimeInterval(-0.001)
        ) == nil)
    }

    @Test func softFlowResetsOnlyMultiplierAndKeepsContinuity() {
        let original = makeArc()
        let completed = original
            .resettingMultiplierForSoftFlow()
            .afterCompletedSoftFlow(at: end.addingTimeInterval(60))

        #expect(completed.id == original.id)
        #expect(completed.multiplier == 1)
        #expect(completed.sessionCount == original.sessionCount + 1)
        #expect(completed.pauseUsedMs == original.pauseUsedMs)
    }

    @Test func arcConclusionDoesNotPersistUntilAllEvidenceIsReadyAndCanRetry() throws {
        let arcID = UUID()
        let session = FlowSession(
            id: UUID(), flowInstanceID: UUID(), title: "One", description: "",
            journeyName: "Arc", startTime: end, endTime: end.addingTimeInterval(60),
            durationMs: 60_000, surgePlannedMs: nil, surgePoints: 0,
            scyraPoints: 12, isSoftMode: false, arcID: arcID, arcIndex: 1,
            arcMultiplierUsed: 1.2, arcBonusPoints: 2, createdAt: end
        )
        var persistAttempts = 0

        #expect(throws: ArcConclusionProbeError.persistence) {
            try ArcConclusionPolicy.conclude(
                arcID: arcID,
                fetchSessions: { [session] },
                fetchShellSummary: { ShellRewardSummary(pearlsCarried: 12) },
                persistConclusion: {
                    persistAttempts += 1
                    throw ArcConclusionProbeError.persistence
                }
            )
        }
        #expect(persistAttempts == 1)

        let retried = try ArcConclusionPolicy.conclude(
            arcID: arcID,
            fetchSessions: { [session] },
            fetchShellSummary: { ShellRewardSummary(pearlsCarried: 12) },
            persistConclusion: { persistAttempts += 1 }
        )
        #expect(persistAttempts == 2)
        #expect(retried?.totalSessions == 1)
        #expect(retried?.shellSummary.pearlsCarried == 12)
    }

    private func makeArc() -> ArcRuntimeState {
        ArcRuntimeState(
            id: UUID(uuidString: "00000000-0000-0000-0000-000000000042")!,
            isPending: false,
            multiplier: 1.7,
            progressMs: 123_000,
            lastSessionEndTime: end,
            sessionCount: 6,
            pauseUsedMs: 1_000,
            pauseStartedAt: nil
        )
    }
}

@MainActor
struct FlowLifecycleParityTests {
    @Test func swiftDataRepositoryRoundTripsDraftArcAndCompletedSession() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let createdAt = Date(timeIntervalSince1970: 5_000)
        let snapshot = ActiveFlowSnapshot(
            flowInstanceID: UUID(),
            title: "Durable work",
            journeyName: "Scyra",
            mode: .flow,
            isInFlowMode: true,
            isRunning: true,
            accumulatedDurationMs: 30_000,
            segmentStartedAt: createdAt,
            firstStartedAt: createdAt,
            surgePlannedMs: 10 * 60_000,
            createdAt: createdAt
        )
        let arc = ArcRuntimeState(
            id: UUID(),
            isPending: false,
            multiplier: 1.4,
            progressMs: 0,
            lastSessionEndTime: createdAt,
            sessionCount: 2,
            pauseUsedMs: 0,
            pauseStartedAt: nil
        )

        try repository.saveActiveFlow(snapshot)
        try repository.saveArcState(active: arc, recentlyEnded: nil)
        #expect(try repository.fetchActiveFlow() == snapshot)
        #expect(try repository.fetchActiveArc() == arc)

        let session = FlowSession(
            id: UUID(),
            flowInstanceID: snapshot.flowInstanceID,
            title: snapshot.title,
            description: "",
            journeyName: snapshot.journeyName,
            startTime: createdAt,
            endTime: createdAt.addingTimeInterval(600),
            durationMs: 10 * 60_000,
            surgePlannedMs: snapshot.surgePlannedMs,
            surgePoints: 14,
            scyraPoints: 21,
            isSoftMode: false,
            arcID: arc.id,
            arcIndex: 3,
            arcMultiplierUsed: 1.4,
            arcBonusPoints: 6,
            createdAt: createdAt
        )

        _ = try repository.commit(session: session, activeArc: arc, recentlyEndedArc: nil)
        #expect(try repository.fetchActiveFlow() == nil)
        #expect(try repository.fetchJourneys().map(\.name) == ["Scyra"])
        #expect(try repository.fetchSessions(arcID: arc.id) == [session])
        #expect(try repository.fetchAllSessions() == [session])
    }

    @Test func runningFlowRestoresFromTimestampsAfterRelaunch() {
        let repository = InMemoryFlowRepository()
        let clock = TestClock(Date(timeIntervalSince1970: 10_000))
        let first = FlowViewModel(repository: repository, now: { clock.value })
        first.updateTitle("Deep work")
        first.updateJourneyName("Build Scyra")
        first.enterFlowMode()

        clock.advance(seconds: 125)
        let restored = FlowViewModel(repository: repository, now: { clock.value })

        #expect(restored.isRunning)
        #expect(restored.isInFlowMode)
        #expect(restored.elapsedMs == 125_000)
        #expect(restored.title == "Deep work")
    }

    @Test func completedFlowPersistsCanonicalScoreAndJourney() throws {
        let repository = InMemoryFlowRepository()
        let clock = TestClock(Date(timeIntervalSince1970: 20_000))
        let viewModel = FlowViewModel(repository: repository, now: { clock.value })
        viewModel.updateTitle("Parity slice")
        viewModel.updateJourneyName("Scyra")
        viewModel.enterFlowMode()
        clock.advance(seconds: 10 * 60)
        viewModel.refreshElapsed()
        viewModel.exitFlowMode()
        viewModel.complete(.saveFlow)

        #expect(viewModel.reward?.finalScyraPoints == 15)
        #expect(viewModel.reward?.minutes == 10)
        #expect(repository.fetchJourneys().map(\.name) == ["Scyra"])
        #expect(repository.fetchActiveFlow() == nil)
    }

    @Test func continueArcMayCommitZeroDurationLikeAndroid() throws {
        let repository = InMemoryFlowRepository()
        let clock = TestClock(Date(timeIntervalSince1970: 30_000))
        let viewModel = FlowViewModel(repository: repository, now: { clock.value })
        viewModel.updateTitle("First link")
        viewModel.updateJourneyName("Arc")
        viewModel.complete(.continueArc)

        #expect(viewModel.reward?.arcIndex == 1)
        #expect(viewModel.activeArc?.sessionCount == 1)
        #expect(viewModel.activeArc?.multiplier == ArcRules.startMultiplier)
        #expect(viewModel.awaitingNextFlow)
        #expect(viewModel.finishReward() == false)
        #expect(viewModel.title.isEmpty)
        #expect(viewModel.journeyName == "Arc")
        #expect(repository.fetchActiveFlow() != nil)
    }

    @Test func standaloneSoftFlowRequiresElapsedTime() {
        let viewModel = FlowViewModel(repository: InMemoryFlowRepository())
        viewModel.updateTitle("Show up")
        viewModel.updateJourneyName("Recovery")
        viewModel.setMode(.soft)
        viewModel.complete(.saveFlow)

        #expect(viewModel.reward == nil)
        #expect(viewModel.errorMessage == FlowStrings.startTimerBeforeSaving)
    }

    @Test func terminalRewardLeavesAReusableCleanFlowScreen() {
        let repository = InMemoryFlowRepository()
        let clock = TestClock(Date(timeIntervalSince1970: 40_000))
        let viewModel = FlowViewModel(repository: repository, now: { clock.value })
        viewModel.updateTitle("Finished work")
        viewModel.updateJourneyName("Scyra")
        viewModel.enterFlowMode()
        clock.advance(seconds: 60)
        viewModel.exitFlowMode()
        viewModel.complete(.saveFlow)

        #expect(viewModel.finishReward())
        #expect(viewModel.title.isEmpty)
        #expect(viewModel.journeyName.isEmpty)
        #expect(viewModel.mode == .flow)
        #expect(viewModel.elapsedMs == 0)
        #expect(repository.fetchActiveFlow() == nil)
    }

    @Test func rewardShellEntryConsumesTerminalFlowAndPreparesArcContinuation() {
        let terminalRepository = InMemoryFlowRepository()
        let terminalClock = TestClock(Date(timeIntervalSince1970: 45_000))
        let terminal = FlowViewModel(repository: terminalRepository, now: { terminalClock.value })
        terminal.updateTitle("Finished work")
        terminal.updateJourneyName("Scyra")
        terminal.enterFlowMode()
        terminalClock.advance(seconds: 10 * 60)
        terminal.exitFlowMode()
        terminal.complete(.saveFlow)

        #expect(terminal.reward?.hasShellReward == true)
        #expect(terminal.enterShellFromReward())
        #expect(terminal.reward == nil)
        #expect(terminal.title.isEmpty)
        #expect(terminal.journeyName.isEmpty)
        #expect(terminalRepository.fetchActiveFlow() == nil)

        let continuationRepository = InMemoryFlowRepository()
        let continuationClock = TestClock(Date(timeIntervalSince1970: 46_000))
        let continuation = FlowViewModel(
            repository: continuationRepository,
            now: { continuationClock.value }
        )
        continuation.updateTitle("First link")
        continuation.updateJourneyName("Arc")
        continuation.enterFlowMode()
        continuationClock.advance(seconds: 10 * 60)
        continuation.exitFlowMode()
        continuation.complete(.continueArc)

        #expect(continuation.reward?.hasShellReward == true)
        #expect(continuation.enterShellFromReward())
        #expect(!continuation.awaitingNextFlow)
        #expect(continuation.reward == nil)
        #expect(continuation.activeArc?.sessionCount == 1)
        #expect(continuation.title.isEmpty)
        #expect(continuation.journeyName == "Arc")
        #expect(continuationRepository.fetchActiveFlow() != nil)
    }

    @Test func arcOnlySummaryDoesNotOfferSessionShellEntry() {
        let reward = FlowReward.arcOnly(
            arcID: UUID(),
            summary: ArcSummary(
                totalSessions: 2,
                totalDurationMs: 120_000,
                totalFinalPoints: 20,
                totalArcBonusPoints: 2,
                peakMultiplier: 1.2,
                shellSummary: ShellRewardSummary(pearlsCarried: 20)
            )
        )
        #expect(!reward.hasShellReward)
        #expect(reward.isArcOnlySummary)
    }

    @Test func flowCompletionControlsMatchAndroid() {
        #expect(FlowCompletionControls.resolve(isSoftMode: true, isArcLinked: false) == .standaloneSoft)
        #expect(FlowCompletionControls.resolve(isSoftMode: true, isArcLinked: true) == .arcActions)
        #expect(FlowCompletionControls.resolve(isSoftMode: false, isArcLinked: true) == .arcActions)
        #expect(FlowCompletionControls.resolve(isSoftMode: false, isArcLinked: false) == .regularActions)
    }

    @Test func expiredArcShowsArcOnlyRewardWithoutCommittingAThrowawayFlow() {
        let repository = InMemoryFlowRepository()
        let clock = TestClock(Date(timeIntervalSince1970: 50_000))
        let arcID = UUID()
        let expiredArc = ArcRuntimeState(
            id: arcID,
            isPending: false,
            multiplier: 1.5,
            progressMs: 0,
            lastSessionEndTime: clock.value.addingTimeInterval(-5 * 60 - 0.001),
            sessionCount: 2,
            pauseUsedMs: 0,
            pauseStartedAt: nil
        )
        let draftID = UUID()
        repository.activeArc = expiredArc
        repository.activeFlow = ActiveFlowSnapshot(
            flowInstanceID: draftID,
            title: "Next focus",
            journeyName: "Arc",
            mode: .flow,
            isInFlowMode: false,
            isRunning: false,
            accumulatedDurationMs: 0,
            segmentStartedAt: nil,
            firstStartedAt: nil,
            surgePlannedMs: nil,
            createdAt: clock.value
        )
        repository.sessions = [
            Self.arcSession(arcID: arcID, index: 1, points: 10, bonus: 0, durationMs: 60_000),
            Self.arcSession(arcID: arcID, index: 2, points: 25, bonus: 5, durationMs: 120_000)
        ]

        let viewModel = FlowViewModel(repository: repository, now: { clock.value })

        #expect(viewModel.activeArc == nil)
        #expect(repository.activeArc == nil)
        #expect(repository.sessions.count == 2)
        #expect(viewModel.reward?.isArcOnlySummary == true)
        #expect(viewModel.reward?.arcSummary?.totalSessions == 2)
        #expect(viewModel.reward?.arcSummary?.totalDurationMs == 180_000)
        #expect(viewModel.reward?.arcSummary?.totalFinalPoints == 35)

        let rewardID = viewModel.reward?.id
        viewModel.refreshElapsed(at: clock.value.addingTimeInterval(1))
        #expect(viewModel.reward?.id == rewardID)
        #expect(repository.sessions.count == 2)
        #expect(viewModel.finishReward() == false)
        #expect(viewModel.title == "Next focus")
        #expect(viewModel.journeyName == "Arc")
    }

    @Test func exhaustedArcPauseShowsSummaryAndPreservesPausedFlow() {
        let repository = InMemoryFlowRepository()
        let clock = TestClock(Date(timeIntervalSince1970: 60_000))
        let arcID = UUID()
        repository.activeArc = ArcRuntimeState(
            id: arcID,
            isPending: false,
            multiplier: 1.4,
            progressMs: 0,
            lastSessionEndTime: clock.value.addingTimeInterval(-30),
            sessionCount: 2,
            pauseUsedMs: 0,
            pauseStartedAt: clock.value.addingTimeInterval(-2 * 60)
        )
        repository.sessions = [Self.arcSession(
            arcID: arcID, index: 2, points: 20, bonus: 4, durationMs: 60_000
        )]
        repository.activeFlow = ActiveFlowSnapshot(
            flowInstanceID: UUID(),
            title: "Paused focus",
            journeyName: "Arc",
            mode: .flow,
            isInFlowMode: false,
            isRunning: false,
            accumulatedDurationMs: 60_000,
            segmentStartedAt: nil,
            firstStartedAt: clock.value.addingTimeInterval(-3 * 60),
            surgePlannedMs: nil,
            createdAt: clock.value.addingTimeInterval(-3 * 60)
        )

        let viewModel = FlowViewModel(repository: repository, now: { clock.value })

        #expect(viewModel.reward?.isArcOnlySummary == true)
        #expect(viewModel.activeArc == nil)
        #expect(viewModel.elapsedMs == 60_000)
        #expect(viewModel.finishReward() == false)
        #expect(viewModel.title == "Paused focus")
        #expect(viewModel.elapsedMs == 60_000)
    }

    private static func arcSession(
        arcID: UUID,
        index: Int,
        points: Int,
        bonus: Int,
        durationMs: Int64
    ) -> FlowSession {
        let end = Date(timeIntervalSince1970: TimeInterval(40_000 + index * 1_000))
        return FlowSession(
            id: UUID(),
            flowInstanceID: UUID(),
            title: "Arc Flow \(index)",
            description: "",
            journeyName: "Arc",
            startTime: end.addingTimeInterval(-TimeInterval(durationMs) / 1_000),
            endTime: end,
            durationMs: durationMs,
            surgePlannedMs: nil,
            surgePoints: 0,
            scyraPoints: points,
            isSoftMode: false,
            arcID: arcID,
            arcIndex: index,
            arcMultiplierUsed: 1 + Double(index) / 10,
            arcBonusPoints: bonus,
            createdAt: end
        )
    }
}

@MainActor
private final class TestClock {
    private(set) var value: Date

    init(_ value: Date) {
        self.value = value
    }

    func advance(seconds: TimeInterval) {
        value = value.addingTimeInterval(seconds)
    }
}

private enum ArcConclusionProbeError: Error {
    case persistence
}
