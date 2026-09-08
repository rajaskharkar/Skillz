import Foundation
import SwiftData
import Testing
@testable import Scyra

@MainActor
struct ShellRewardPolicyParityTests {
    @Test func regularFlowMilestonesUseAndroidGreedyDurationConversion() {
        #expect(ShellRewardPolicy.milestoneFinds(minutes: 9).isEmpty)
        #expect(ShellRewardPolicy.milestoneFinds(minutes: 10) == [ShellRewardCatalog.focusMinnow])
        #expect(ShellRewardPolicy.milestoneFinds(minutes: 29) == [
            ShellRewardCatalog.focusMinnow,
            ShellRewardCatalog.focusMinnow
        ])
        #expect(ShellRewardPolicy.milestoneFinds(minutes: 30) == [ShellRewardCatalog.focusSeahorse])
        #expect(ShellRewardPolicy.milestoneFinds(minutes: 80) == [
            ShellRewardCatalog.focusManta,
            ShellRewardCatalog.focusMinnow,
            ShellRewardCatalog.focusMinnow
        ])
        #expect(ShellRewardPolicy.milestoneFinds(minutes: 150) == [
            ShellRewardCatalog.focusWhale,
            ShellRewardCatalog.focusSeahorse,
            ShellRewardCatalog.focusWhale
        ])
        #expect(ShellRewardPolicy.milestoneFinds(minutes: 150, isSoftFlow: true).isEmpty)
    }

    @Test func regularAndSoftPoliciesUseSeparateCurrencies() {
        let regular = makeShellSession(durationMs: 600_000, points: 42)
        let soft = makeShellSession(durationMs: 1_321_999, points: 999, isSoftMode: true)

        #expect(ShellRewardPolicy.reward(for: regular) == ShellSessionReward(
            pearlsEarned: 42,
            grantedFindIDs: [ShellRewardCatalog.focusMinnow],
            badgeIDs: [ShellRewardCatalog.badgeFlow10]
        ))
        #expect(ShellRewardPolicy.reward(for: soft) == ShellSessionReward(stillwaterUnits: 1_321))
    }

    @Test func aggregationGroupsArcEventsByTypeAndIdentifier() {
        let arcID = UUID()
        let first = UUID()
        let second = UUID()
        let events = [
            event(sessionID: first, arcID: arcID, type: .animalGranted, rewardID: ShellRewardCatalog.focusMinnow),
            event(sessionID: second, arcID: arcID, type: .animalGranted, rewardID: ShellRewardCatalog.focusMinnow),
            event(sessionID: first, arcID: arcID, type: .badgeUpdated, rewardID: ShellRewardCatalog.badgeFlow10, quantity: 2),
            event(sessionID: first, arcID: arcID, type: .pearlsCarried, quantity: 482),
            event(sessionID: second, arcID: arcID, type: .stillwaterAdded, quantity: 42)
        ]

        let summary = ShellRewardEventAggregator.aggregate(events)
        #expect(summary.animals == [ShellRewardCount(id: ShellRewardCatalog.focusMinnow, count: 2)])
        #expect(summary.badges == [ShellRewardCount(id: ShellRewardCatalog.badgeFlow10, count: 2)])
        #expect(summary.pearlsCarried == 482)
        #expect(summary.stillwaterAdded == 42)
    }

    @Test func revealMappingUnifiesPointsAndPearlsAndSeparatesSoftDrops() throws {
        let regular = FlowReward(
            id: UUID(),
            minutes: 10,
            baseScyraPoints: 20,
            tenMinuteBonuses: 1,
            thirtyMinuteBonuses: 0,
            sixtyMinuteBonuses: 0,
            finalScyraPoints: 25,
            surgePoints: 0,
            movementSteps: nil,
            movementPoints: 0,
            arcIndex: nil,
            arcMultiplierUsed: nil,
            arcBonusPoints: 0,
            arcNextMultiplier: nil,
            arcDidLevelUp: false,
            isSoftSession: false,
            arcSummary: nil,
            shellReward: ShellSessionReward(
                pearlsEarned: 25,
                grantedFindIDs: [ShellRewardCatalog.focusMinnow],
                badgeIDs: [ShellRewardCatalog.badgeFlow10]
            )
        )
        let regularCards = RewardRevealMapper.cards(for: regular)
        #expect(regularCards.first?.title == "25 Scyra Points")
        #expect(regularCards.first?.subtitle == "Carried into The Shell as Pearls.")
        #expect(!regularCards.dropFirst().contains { $0.amountText?.contains("25 Pearls") == true })
        #expect(regularCards.contains { $0.type == .animal && $0.subtitle == "Animal · Sunlit Reef" })

        let soft = FlowReward(
            id: UUID(),
            minutes: 22,
            baseScyraPoints: 0,
            tenMinuteBonuses: 0,
            thirtyMinuteBonuses: 0,
            sixtyMinuteBonuses: 0,
            finalScyraPoints: 0,
            surgePoints: 0,
            movementSteps: nil,
            movementPoints: 0,
            arcIndex: nil,
            arcMultiplierUsed: nil,
            arcBonusPoints: 0,
            arcNextMultiplier: nil,
            arcDidLevelUp: false,
            isSoftSession: true,
            arcSummary: nil,
            shellReward: ShellSessionReward(stillwaterUnits: 220)
        )
        let softCards = RewardRevealMapper.cards(for: soft)
        #expect(softCards.count == 1)
        #expect(softCards[0].type == .stillwaterResult)
        #expect(softCards[0].amountText == "+220 Drops")
        #expect(softCards[0].destinationHint == "View in Stillwater Room.")
    }

    @Test func revealCardsPreserveAndroidRewardPolicyOrder() throws {
        let session = makeShellSession(durationMs: 220 * 60_000, points: 1_000)
        var shell = ShellRewardPolicy.reward(for: session)
        shell.grantedFindIDs.insert("legacy_non_creature", at: 0)
        let reward = FlowReward(
            id: session.id,
            minutes: 220,
            baseScyraPoints: 220,
            tenMinuteBonuses: 22,
            thirtyMinuteBonuses: 7,
            sixtyMinuteBonuses: 3,
            finalScyraPoints: 1_000,
            surgePoints: 0,
            movementSteps: nil,
            movementPoints: 0,
            arcIndex: nil,
            arcMultiplierUsed: nil,
            arcBonusPoints: 0,
            arcNextMultiplier: nil,
            arcDidLevelUp: false,
            isSoftSession: false,
            arcSummary: nil,
            shellReward: shell
        )

        let cards = RewardRevealMapper.sessionCards(for: reward)
        #expect(cards.filter { $0.type == .animal }.map(\.id) == [
            "animal-\(ShellRewardCatalog.focusWhale)-2",
            "animal-\(ShellRewardCatalog.focusManta)-1",
            "animal-\(ShellRewardCatalog.focusSeahorse)-1",
            "animal-\(ShellRewardCatalog.focusMinnow)-1"
        ])
        #expect(!cards.contains { $0.id == "find-legacy_non_creature" })
        let badge = try #require(cards.first { $0.type == .badge })
        let lines = try #require(badge.body).components(separatedBy: "\n")
        #expect(Array(lines.prefix(4)) == [
            "10-minute Flow ×1",
            "30-minute Flow ×1",
            "60-minute Flow ×1",
            "2-hour Flow ×1"
        ])
    }

    private func event(
        sessionID: UUID,
        arcID: UUID?,
        type: ShellRewardEventType,
        rewardID: String? = nil,
        quantity: Int64 = 1
    ) -> ShellRewardEvent {
        ShellRewardEvent(
            id: "\(sessionID)-\(type.rawValue)-\(rewardID ?? "_")",
            sourceSessionID: sessionID,
            arcID: arcID,
            type: type,
            rewardID: rewardID,
            quantity: quantity,
            occurredAt: .now
        )
    }
}

@MainActor
struct ShellRewardTransactionParityTests {
    @Test func swiftDataCommitAtomicallyPersistsRegularRewardAndRetryIsIdempotent() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let flowInstanceID = UUID()
        let session = makeShellSession(
            flowInstanceID: flowInstanceID,
            durationMs: 9_000_000,
            points: 482,
            arcID: UUID()
        )

        _ = try repository.commit(session: session, activeArc: nil, recentlyEndedArc: nil)
        let retry = makeShellSession(
            flowInstanceID: flowInstanceID,
            durationMs: 9_000_000,
            points: 999,
            arcID: session.arcID
        )
        _ = try repository.commit(session: retry, activeArc: nil, recentlyEndedArc: nil)

        #expect(try repository.fetchPearlBalance() == 482)
        #expect(try repository.fetchPearlLedger().count == 1)
        #expect(try repository.fetchStillwaterBalance() == 0)
        #expect(try repository.fetchShellRewardEvents(sessionID: session.id).count == 7)
        let findCounts = Dictionary(grouping: try repository.fetchShellFindGrants(), by: \.findID)
            .mapValues(\.count)
        #expect(findCounts == [
            ShellRewardCatalog.focusWhale: 2,
            ShellRewardCatalog.focusSeahorse: 1
        ])
        #expect(Set(try repository.fetchShellBadges().map(\.badgeID)) == Set([
            ShellRewardCatalog.badgeFlow10,
            ShellRewardCatalog.badgeFlow30,
            ShellRewardCatalog.badgeFlow60,
            ShellRewardCatalog.badgeFlow120,
            "variety_collector"
        ]))

        let recreated = SwiftDataFlowRepository(container: container)
        #expect(try recreated.fetchPearlBalance() == 482)
        let arcID = try #require(session.arcID)
        #expect(try recreated.fetchArcShellRewardSummary(arcID: arcID).pearlsCarried == 482)
    }

    @Test func softCommitWritesDropsAndNoPearlsFindsOrBadges() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let session = makeShellSession(durationMs: 600_999, points: 1_000, isSoftMode: true)

        _ = try repository.commit(session: session, activeArc: nil, recentlyEndedArc: nil)

        #expect(try repository.fetchStillwaterBalance() == 600)
        #expect(try repository.fetchStillwaterLifetimeTotal() == 600)
        #expect(try repository.fetchPearlBalance() == 0)
        #expect(try repository.fetchShellFindGrants().isEmpty)
        #expect(try repository.fetchShellBadges().isEmpty)
        let events = try repository.fetchShellRewardEvents(sessionID: session.id)
        #expect(events == [ShellRewardEvent(
            id: "\(session.id.uuidString):STILLWATER_ADDED:_",
            sourceSessionID: session.id,
            arcID: nil,
            type: .stillwaterAdded,
            rewardID: nil,
            quantity: 600,
            occurredAt: session.createdAt
        )])
    }

    @Test func delayedMovementCreditsOnlyPositivePearlDeltasTransactionally() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let session = makeShellSession(durationMs: 60_000, points: 20)
        let initial = MovementRewardRecalculator.calculate(
            sessionID: session.id,
            nonMovementPreMultiplierPoints: 20,
            movementPoints: 0,
            arcMultiplier: 1,
            pearlEligible: true
        )
        let snapshot = shellMovementSnapshot(session: session, movementPoints: 0)
        _ = try repository.commit(
            session: session,
            activeArc: nil,
            recentlyEndedArc: nil,
            movement: FlowMovementCompletion(snapshot: snapshot, rewardBreakdown: initial)
        )

        let firstIncrease = MovementRewardRecalculator.calculate(
            sessionID: session.id,
            nonMovementPreMultiplierPoints: 20,
            movementPoints: 3,
            arcMultiplier: 1,
            pearlEligible: true
        )
        var refreshed = snapshot
        refreshed.rawMovementPoints = 3
        try repository.applyMovementRefresh(snapshot: refreshed, breakdown: firstIncrease)
        try repository.applyMovementRefresh(snapshot: refreshed, breakdown: firstIncrease)

        #expect(try repository.fetchPearlBalance() == 23)
        #expect(try repository.fetchPearlLedger().map(\.delta).sorted() == [3, 20])
        #expect(try repository.fetchAllSessions().single?.scyraPoints == 23)

        let secondIncrease = MovementRewardRecalculator.calculate(
            sessionID: session.id,
            nonMovementPreMultiplierPoints: 20,
            movementPoints: 5,
            arcMultiplier: 1,
            pearlEligible: true
        )
        refreshed.rawMovementPoints = 5
        try repository.applyMovementRefresh(snapshot: refreshed, breakdown: secondIncrease)
        #expect(try repository.fetchPearlBalance() == 25)
        #expect(try repository.fetchPearlLedger().map(\.delta).sorted() == [2, 3, 20])
    }

    @Test func flowViewModelSurfacesCommittedShellReward() throws {
        let repository = InMemoryFlowRepository()
        let clock = ShellRewardTestClock(Date(timeIntervalSince1970: 100_000))
        let viewModel = FlowViewModel(repository: repository, now: { clock.now })
        viewModel.updateTitle("Reward Flow")
        viewModel.updateJourneyName("Scyra")
        viewModel.enterFlowMode()
        clock.advance(seconds: 600)
        viewModel.exitFlowMode()
        viewModel.complete(.saveFlow)

        let reward = try #require(viewModel.reward)
        #expect(reward.shellReward.pearlsEarned == reward.finalScyraPoints)
        #expect(reward.shellReward.grantedFindIDs == [ShellRewardCatalog.focusMinnow])
        #expect(reward.shellReward.badgeIDs == [ShellRewardCatalog.badgeFlow10])
        #expect(repository.fetchPearlBalance() == reward.finalScyraPoints)
    }

    @Test func completingArcAggregatesCommittedShellEventsIntoReveal() throws {
        let repository = InMemoryFlowRepository()
        let arcID = UUID()
        let first = makeShellSession(durationMs: 600_000, points: 25, arcID: arcID)
        let activeArc = ArcRuntimeState(
            id: arcID,
            isPending: true,
            multiplier: ArcRules.startMultiplier,
            progressMs: 0,
            lastSessionEndTime: first.endTime,
            sessionCount: 1,
            pauseUsedMs: 0,
            pauseStartedAt: nil
        )
        _ = repository.commit(session: first, activeArc: activeArc, recentlyEndedArc: nil)

        let clock = ShellRewardTestClock(first.endTime.addingTimeInterval(60))
        let viewModel = FlowViewModel(repository: repository, now: { clock.now })
        viewModel.updateTitle("Finish Arc")
        viewModel.updateJourneyName("Scyra")
        viewModel.enterFlowMode()
        clock.advance(seconds: 600)
        viewModel.exitFlowMode()
        viewModel.complete(.completeArc)

        let reward = try #require(viewModel.reward)
        let summary = try #require(reward.arcSummary)
        #expect(summary.totalSessions == 2)
        #expect(summary.shellSummary.pearlsCarried == repository.fetchPearlBalance())
        #expect(summary.shellSummary.animals == [
            ShellRewardCount(id: ShellRewardCatalog.focusMinnow, count: 2)
        ])
        let cards = RewardRevealMapper.cards(for: reward)
        #expect(cards.first?.type == .scoreBreakdown)
        #expect(cards.contains { $0.type == .arcScore })
        #expect(cards.contains { $0.type == .arcAnimals })
    }
}

@MainActor
private func makeShellSession(
    id: UUID = UUID(),
    flowInstanceID: UUID = UUID(),
    durationMs: Int64,
    points: Int,
    isSoftMode: Bool = false,
    arcID: UUID? = nil
) -> FlowSession {
    let end = Date(timeIntervalSince1970: 50_000)
    return FlowSession(
        id: id,
        flowInstanceID: flowInstanceID,
        title: "Shell reward",
        description: "",
        journeyName: "Scyra",
        startTime: end.addingTimeInterval(-Double(durationMs) / 1_000),
        endTime: end,
        durationMs: durationMs,
        surgePlannedMs: nil,
        surgePoints: 0,
        scyraPoints: points,
        isSoftMode: isSoftMode,
        arcID: arcID,
        arcIndex: arcID == nil ? nil : 1,
        arcMultiplierUsed: arcID == nil ? nil : 1,
        arcBonusPoints: 0,
        createdAt: end
    )
}

@MainActor
private func shellMovementSnapshot(session: FlowSession, movementPoints: Int64) -> FlowHealthSnapshot {
    FlowHealthSnapshot(
        sessionID: session.id,
        healthEnabledAtStart: true,
        accessRequestedAtStart: true,
        status: .captured,
        steps: movementPoints * MovementBonusCalculator.stepsPerPoint,
        rawMovementPoints: movementPoints,
        finalMovementScyraContribution: movementPoints,
        finalMovementPearlContribution: movementPoints,
        firstCheckedAt: session.endTime,
        lastCheckedAt: session.endTime,
        capturedAt: session.endTime,
        expiresAt: session.endTime.addingTimeInterval(MovementRefreshPolicy.refreshWindow),
        checkCount: 1,
        flowStartTime: session.startTime,
        flowEndTime: session.endTime,
        activeIntervals: [FlowActiveInterval(start: session.startTime, end: session.endTime)],
        sourceLabel: "Apple Health",
        updatedAfterSync: false
    )
}

@MainActor
private final class ShellRewardTestClock {
    var now: Date
    init(_ now: Date) { self.now = now }
    func advance(seconds: TimeInterval) { now = now.addingTimeInterval(seconds) }
}

private extension Array {
    var single: Element? { count == 1 ? first : nil }
}
