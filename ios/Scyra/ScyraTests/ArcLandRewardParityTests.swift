import Foundation
import SwiftData
import Testing
@testable import Scyra

@MainActor
struct ArcLandRewardParityTests {
    @Test func canonicalPackagesAndUnboundedStacking() throws {
        let cases: [(Int, String)] = [
            (0,""),(1,""),(2,""),(3,"chicken"),(4,"chicken"),(5,"chicken"),
            (6,"deer"),(7,"deer"),(8,"deer"),(9,"camel"),(10,"camel"),(11,"camel"),
            (12,"moose"),(13,"moose"),(14,"moose"),(15,"tiger"),(16,"tiger"),(17,"tiger"),
            (18,"tiger,chicken"),(21,"tiger,deer"),(24,"tiger,camel"),(27,"tiger,moose"),
            (30,"tiger,tiger"),(33,"tiger,tiger,chicken"),(38,"tiger,tiger,deer"),
            (45,"tiger,tiger,tiger"),(63,"tiger,tiger,tiger,tiger,chicken")
        ]
        for (depth, expected) in cases {
            let package = try ArcLandRewardPolicy.rewards(flowCount: depth)
                .flatMap { Array(repeating: $0.id.replacingOccurrences(of: "creature_", with: ""), count: $0.count) }
            #expect(package.joined(separator: ",") == expected)
        }
        let huge = try ArcLandRewardPolicy.rewards(flowCount: Int.max)
        #expect(huge.first?.count == Int.max / 15)
        #expect(huge.count <= 2)
        #expect(throws: ArcLandRewardError.self) { try ArcLandRewardPolicy.rewards(flowCount: -1) }
    }

    @Test(arguments: [false, true])
    func finalizationIsStrictlyAfterGraceAtomicIdempotentAndRejectsLateMembership(swiftData: Bool) throws {
        let repository = try makeRepository(swiftData: swiftData)
        let arcID = UUID()
        let end = Date(timeIntervalSince1970: 50_000)
        let members = try seedArc(repository, arcID: arcID, count: 18, end: end)
        #expect(try repository.fetchArcLandReward(arcID: arcID)?.flowCount == 18)
        #expect(try repository.finalizeExpiredArcLandRewards(at: end.addingTimeInterval(300)).isEmpty)
        #expect(try repository.fetchShellFindInstances().allSatisfy { $0.sourceType != "arc" })
        #expect(try repository.finalizeExpiredArcLandRewards(at: end.addingTimeInterval(301)) == [arcID])
        let finds = try repository.fetchShellFindInstances().filter { $0.sourceType == "arc" }
        #expect(Set(finds.map(\.findID)) == ["creature_tiger", "creature_chicken"])
        #expect(finds.allSatisfy { $0.sourceID == arcID.uuidString && $0.isNew && $0.animalLevel == 1 })
        #expect(try repository.fetchCreatureDiscoveries().count == 2)
        #expect(try repository.fetchShellRewardEvents(arcID: arcID).filter { $0.type == .animalGranted }.count == 2)
        #expect(try repository.finalizeExpiredArcLandRewards(at: end.addingTimeInterval(900)).isEmpty)

        // A completion retry keeps the first commit and must not reopen the journal.
        let original = try #require(members.first)
        _ = try repository.commit(session: original, activeArc: nil, recentlyEndedArc: nil)
        #expect(try repository.fetchArcLandReward(arcID: arcID)?.finalizedAt != nil)
        #expect(throws: ArcLandRewardError.self) {
            _ = try repository.commit(session: session(arcID: arcID, index: 19, end: end), activeArc: nil, recentlyEndedArc: nil)
        }
        #expect(try repository.fetchSessions(arcID: arcID).count == 18)
        #expect(try repository.fetchShellFindInstances().filter { $0.sourceType == "arc" }.count == 2)

        let shell = ShellViewModel(repository: repository)
        shell.chestFilter = .land
        #expect(shell.chestStacks.count == 2)
        let chicken = try #require(finds.first { $0.findID == "creature_chicken" })
        #expect(try repository.releaseCreature(instanceID: chicken.id, at: end.addingTimeInterval(901)) == 30)
        #expect(try repository.fetchCreatureDiscoveries().contains { $0.speciesID == chicken.findID })
    }

    @Test(arguments: [false, true])
    func reservationProtectsPausedFlowAndRuntimeGraceCanExtendFinalization(swiftData: Bool) throws {
        let repository = try makeRepository(swiftData: swiftData)
        let arcID = UUID(), flowID = UUID()
        let end = Date(timeIntervalSince1970: 60_000)
        _ = try seedArc(repository, arcID: arcID, count: 3, end: end)
        try repository.reserveArcFlow(arcID: arcID, flowInstanceID: flowID)
        #expect(try repository.finalizeExpiredArcLandRewards(at: end.addingTimeInterval(10_000)).isEmpty)
        try repository.clearActiveFlow()
        let extended = ArcRuntimeState(id: arcID, isPending: false, multiplier: 1.3, progressMs: 0,
                                      lastSessionEndTime: end.addingTimeInterval(250), sessionCount: 3,
                                      pauseUsedMs: 0, pauseStartedAt: nil)
        try repository.saveArcState(active: nil, recentlyEnded: extended)
        #expect(try repository.finalizeExpiredArcLandRewards(at: end.addingTimeInterval(550)).isEmpty)
        #expect(try repository.finalizeExpiredArcLandRewards(at: end.addingTimeInterval(551)) == [arcID])
        #expect(throws: ArcLandRewardError.self) { try repository.reserveArcFlow(arcID: arcID, flowInstanceID: UUID()) }
    }

    @Test(arguments: [false, true])
    func deletedFlowsAreRecountedAtFinalCloseAndAllDeletedArcsCloseWithoutRewards(swiftData: Bool) throws {
        let repository = try makeRepository(swiftData: swiftData)
        let arcID = UUID(), otherArc = UUID()
        let end = Date(timeIntervalSince1970: 70_000)
        let members = try seedArc(repository, arcID: arcID, count: 6, end: end)
        try repository.deleteSession(id: #require(members.first).id, detachedAt: end)
        let deleted = try seedArc(repository, arcID: otherArc, count: 3, end: end)
        for member in deleted { try repository.deleteSession(id: member.id, detachedAt: end) }
        #expect(try repository.finalizeExpiredArcLandRewards(at: end.addingTimeInterval(301)).count == 2)
        #expect(try repository.fetchArcLandReward(arcID: arcID)?.flowCount == 5)
        #expect(try repository.fetchArcLandReward(arcID: otherArc)?.flowCount == 0)
        #expect(try repository.fetchShellFindInstances().filter { $0.sourceType == "arc" }.map(\.findID) == ["creature_chicken"])
    }

    @Test func completionPreviewBecomesFinalRewardAndSurvivesRepositoryRecreation() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let clock = LandTestClock()
        let flow = FlowViewModel(repository: repository, now: { clock.date })
        for index in 1...3 {
            flow.updateTitle("Land \(index)")
            flow.updateJourneyName("Practice")
            flow.enterFlowMode()
            clock.date = clock.date.addingTimeInterval(60)
            flow.exitFlowMode()
            flow.complete(index == 3 ? .completeArc : .continueArc)
            if index < 3 { _ = flow.finishReward() }
        }
        let arcID = try #require(flow.reward?.arcSummary?.arcID)
        #expect(flow.reward?.arcSummary?.landRewardsPending == true)
        #expect(try repository.fetchShellFindInstances().isEmpty)
        clock.date = clock.date.addingTimeInterval(301)
        #expect(flow.refreshArcLandRewards())
        let summary = try #require(flow.reward?.arcSummary)
        #expect(!summary.landRewardsPending)
        #expect(summary.shellSummary.animals == [.init(id: "creature_chicken", count: 1)])
        #expect(RewardRevealMapper.arcCards(for: summary).contains { $0.body?.contains("completed Arc depth") == true })
        let restored = SwiftDataFlowRepository(container: container)
        #expect(try restored.fetchArcLandReward(arcID: arcID)?.finalizedAt != nil)
        #expect(try restored.finalizeExpiredArcLandRewards(at: clock.date).isEmpty)
        #expect(try restored.fetchShellFindInstances().count == 1)
    }

    @Test func v6MigrationPreservesHistoryWithoutBackfillAndReservationsSurviveRelaunch() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("scyra-land-v7-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let url = directory.appendingPathComponent("Scyra.store")
        let oldArc = UUID(), newArc = UUID()
        let end = Date(timeIntervalSince1970: 90_000)
        do {
            let schema = Schema(versionedSchema: ScyraSchemaV6.self)
            let container = try ModelContainer(for: schema, configurations: [ModelConfiguration("Scyra", schema: schema, url: url)])
            let journey = JourneyModel(name: "Land", createdAt: end)
            container.mainContext.insert(journey)
            for index in 1...3 {
                container.mainContext.insert(FlowSessionModel(session: session(arcID: oldArc, index: index, end: end), journey: journey))
            }
            try container.mainContext.save()
        }
        func reopen() throws -> ModelContainer {
            let schema = Schema(versionedSchema: ScyraSchemaV7.self)
            return try ModelContainer(for: schema, migrationPlan: ScyraMigrationPlan.self,
                                      configurations: [ModelConfiguration("Scyra", schema: schema, url: url)])
        }
        do {
            let repository = SwiftDataFlowRepository(container: try reopen())
            #expect(try repository.fetchSessions(arcID: oldArc).count == 3)
            #expect(try repository.fetchArcLandReward(arcID: oldArc) == nil)
            #expect(try repository.finalizeExpiredArcLandRewards(at: end.addingTimeInterval(301)).isEmpty)
            _ = try seedArc(repository, arcID: newArc, count: 3, end: end)
            try repository.reserveArcFlow(arcID: newArc, flowInstanceID: UUID())
        }
        do {
            let repository = SwiftDataFlowRepository(container: try reopen())
            #expect(try repository.finalizeExpiredArcLandRewards(at: end.addingTimeInterval(301)).isEmpty)
            try repository.clearActiveFlow()
            #expect(try repository.finalizeExpiredArcLandRewards(at: end.addingTimeInterval(301)) == [newArc])
        }
        let repository = SwiftDataFlowRepository(container: try reopen())
        #expect(try repository.finalizeExpiredArcLandRewards(at: end.addingTimeInterval(999)).isEmpty)
        #expect(try repository.fetchShellFindInstances().filter { $0.sourceType == "arc" }.count == 1)
        #expect(try repository.fetchArcLandReward(arcID: oldArc) == nil)
        #expect(try repository.fetchArcLandReward(arcID: newArc)?.finalizedAt != nil)
    }

    private func makeRepository(swiftData: Bool) throws -> any ScyraRepository {
        swiftData ? SwiftDataFlowRepository(container: try ScyraPersistenceFactory.makeContainer(inMemory: true)) : InMemoryFlowRepository()
    }

    @discardableResult
    private func seedArc(_ repository: any ScyraRepository, arcID: UUID, count: Int, end: Date) throws -> [FlowSession] {
        try (1...count).map { index in
            let value = session(arcID: arcID, index: index, end: end.addingTimeInterval(Double(index - count)))
            return try repository.commit(session: value, activeArc: nil, recentlyEndedArc: nil)
        }
    }

    private func session(arcID: UUID, index: Int, end: Date) -> FlowSession {
        .init(id: UUID(), flowInstanceID: UUID(), title: "Arc member", description: "", journeyName: "Land",
              startTime: end.addingTimeInterval(-60), endTime: end, durationMs: 60_000,
              surgePlannedMs: nil, surgePoints: 0, scyraPoints: 1, isSoftMode: index.isMultiple(of: 2),
              arcID: arcID, arcIndex: index, arcMultiplierUsed: 1, arcBonusPoints: 0, createdAt: end)
    }
}

@MainActor private final class LandTestClock {
    var date = Date(timeIntervalSince1970: 80_000)
}
