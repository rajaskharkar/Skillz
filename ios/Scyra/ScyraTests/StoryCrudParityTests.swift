import Foundation
import SwiftData
import Testing
@testable import Scyra

@MainActor
struct StoryCrudRepositoryParityTests {
    @Test func historicalPulseInheritsArcAndPersistsItsChronicleAtomically() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let createdAt = Date(timeIntervalSince1970: 100_000)
        let arcID = UUID()
        let session = makeSession(journey: "Work", createdAt: createdAt, arcID: arcID, arcIndex: 1)
        _ = try repository.commit(session: session, activeArc: nil, recentlyEndedArc: nil)

        let pulse = try repository.createHistoricalPulse(
            parentSessionID: session.id,
            title: "  Decision  ",
            description: "  Preserve this moment  ",
            journeyName: " Ideas ",
            createdAt: createdAt.addingTimeInterval(10)
        )

        #expect(pulse.title == "Decision")
        #expect(pulse.journeyName == "Ideas")
        #expect(pulse.parentSessionID == session.id)
        #expect(pulse.parentFlowInstanceID == nil)
        #expect(pulse.arcID == arcID)
        #expect(try repository.fetchChronicle(owner: .pulse(pulse.id)).textMoments == ["Preserve this moment"])
    }

    @Test func deletingFlowDetachesPulsesAndCleansDependentRowsAndUnusedJourney() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let createdAt = Date(timeIntervalSince1970: 110_000)
        let arcID = UUID()
        let session = makeSession(journey: "Work", createdAt: createdAt, arcID: arcID, arcIndex: 1)
        _ = try repository.addChronicleText(owner: .activeFlow(session.flowInstanceID), text: "Flow note")
        _ = try repository.commit(
            session: session,
            activeArc: nil,
            recentlyEndedArc: nil,
            movement: movementCompletion(session: session)
        )
        let pulse = try repository.createHistoricalPulse(
            parentSessionID: session.id,
            title: "Keep me",
            description: "Detached, not deleted",
            journeyName: "Ideas",
            createdAt: createdAt.addingTimeInterval(1)
        )
        try repository.linkCompletedFlow(pulseID: pulse.id, sessionID: session.id, linkedAt: createdAt)
        try repository.saveArcMetadata(
            ArcMetadata(arcID: arcID, title: "One Flow Arc"),
            updatedAt: createdAt
        )

        try repository.deleteSession(id: session.id, detachedAt: createdAt.addingTimeInterval(20))

        #expect(try repository.fetchAllSessions().isEmpty)
        let fetchedPulse = try repository.fetchPulse(id: pulse.id)
        let detached = try #require(fetchedPulse)
        #expect(detached.parentSessionID == nil)
        #expect(detached.arcID == arcID)
        #expect(detached.updatedAt == createdAt.addingTimeInterval(20))
        #expect(try repository.fetchChronicle(owner: .pulse(pulse.id)).textMoments == ["Detached, not deleted"])
        #expect(try repository.fetchChronicle(owner: .session(session.id)).moments.isEmpty)
        #expect(try repository.fetchPulseFlowLinks(pulseID: pulse.id).isEmpty)
        #expect(try repository.fetchMovementSnapshot(sessionID: session.id) == nil)
        #expect(try repository.fetchMovementRewardBreakdown(sessionID: session.id) == nil)
        #expect(try repository.fetchAllArcMetadata()[arcID] == nil)

        let journeyNames = try container.mainContext.fetch(FetchDescriptor<JourneyModel>()).map(\.name)
        #expect(!journeyNames.contains("Work"))
        #expect(journeyNames.contains("Ideas"))
    }

    @Test func arcMetadataSurvivesUntilTheFinalArcFlowIsDeleted() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let createdAt = Date(timeIntervalSince1970: 120_000)
        let arcID = UUID()
        let first = makeSession(journey: "Arc", createdAt: createdAt, arcID: arcID, arcIndex: 1)
        let second = makeSession(journey: "Arc", createdAt: createdAt.addingTimeInterval(60), arcID: arcID, arcIndex: 2)
        _ = try repository.commit(session: first, activeArc: nil, recentlyEndedArc: nil)
        _ = try repository.commit(session: second, activeArc: nil, recentlyEndedArc: nil)
        let metadata = ArcMetadata(
            arcID: arcID,
            title: "Morning Flow",
            summary: "A linked sequence",
            outcome: "Finished",
            highlight: nil,
            nextStep: "Continue"
        )
        try repository.saveArcMetadata(metadata, updatedAt: createdAt)

        try repository.deleteSession(id: first.id, detachedAt: createdAt.addingTimeInterval(120))
        #expect(try repository.fetchAllArcMetadata()[arcID] == metadata)

        try repository.deleteSession(id: second.id, detachedAt: createdAt.addingTimeInterval(180))
        #expect(try repository.fetchAllArcMetadata()[arcID] == nil)
    }

    @Test func pulseUpdateAndDeleteRemoveOnlyUnusedJourneys() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let createdAt = Date(timeIntervalSince1970: 130_000)
        let first = try makeStandalonePulse(repository: repository, title: "First", journey: "Old", createdAt: createdAt)
        let second = try makeStandalonePulse(repository: repository, title: "Second", journey: "Old", createdAt: createdAt.addingTimeInterval(1))

        _ = try repository.updatePulse(id: first.id, title: first.title, journeyName: "New", updatedAt: createdAt.addingTimeInterval(2))
        var names = try container.mainContext.fetch(FetchDescriptor<JourneyModel>()).map(\.name)
        #expect(Set(names) == Set(["Old", "New"]))

        try repository.deletePulse(id: second.id)
        names = try container.mainContext.fetch(FetchDescriptor<JourneyModel>()).map(\.name)
        #expect(names == ["New"])
    }

    private func makeStandalonePulse(
        repository: SwiftDataFlowRepository,
        title: String,
        journey: String,
        createdAt: Date
    ) throws -> Pulse {
        let key = UUID()
        _ = try repository.addChronicleText(owner: .pulseDraft(key), text: title)
        return try repository.createPulse(
            creationKey: key,
            title: title,
            journeyName: journey,
            parentFlowInstanceID: nil,
            arcID: nil,
            createdAt: createdAt
        )
    }

    private func movementCompletion(session: FlowSession) -> FlowMovementCompletion {
        let snapshot = FlowHealthSnapshot(
            sessionID: session.id,
            healthEnabledAtStart: true,
            accessRequestedAtStart: true,
            status: .captured,
            steps: 300,
            rawMovementPoints: 3,
            finalMovementScyraContribution: 3,
            finalMovementPearlContribution: 3,
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
        return FlowMovementCompletion(
            snapshot: snapshot,
            rewardBreakdown: MovementRewardRecalculator.calculate(
                sessionID: session.id,
                nonMovementPreMultiplierPoints: 10,
                movementPoints: 3,
                arcMultiplier: 1,
                pearlEligible: true
            )
        )
    }

    private func makeSession(
        journey: String,
        createdAt: Date,
        arcID: UUID?,
        arcIndex: Int?
    ) -> FlowSession {
        FlowSession(
            id: UUID(),
            flowInstanceID: UUID(),
            title: "Flow",
            description: "",
            journeyName: journey,
            startTime: createdAt.addingTimeInterval(-600),
            endTime: createdAt,
            durationMs: 600_000,
            surgePlannedMs: nil,
            surgePoints: 0,
            scyraPoints: 10,
            isSoftMode: false,
            arcID: arcID,
            arcIndex: arcIndex,
            arcMultiplierUsed: arcID == nil ? nil : 1,
            arcBonusPoints: 0,
            createdAt: createdAt
        )
    }
}

@MainActor
struct StoryCrudViewModelParityTests {
    @Test func metadataNormalizationMatchesAndroidAndPreservesInternalContent() {
        let id = UUID()
        let result = ArcMetadata.normalized(
            arcID: id,
            title: "  Morning  Flow  ",
            summary: "\nline one\nline two\n",
            outcome: " ",
            highlight: "hi",
            nextStep: "later"
        )

        #expect(result.title == "Morning  Flow")
        #expect(result.summary == "line one\nline two")
        #expect(result.outcome == nil)
        #expect(result.hasReflection)
        #expect(!result.isEmpty)
    }

    @Test func topJourneysUseAndroidSevenDayRankingAndFiveItemLimit() {
        let now = Date(timeIntervalSince1970: 1_000_000)
        let repository = InMemoryFlowRepository()
        for index in 0..<6 {
            let session = FlowSession(
                id: UUID(),
                flowInstanceID: UUID(),
                title: "Flow \(index)",
                description: "",
                journeyName: "Journey \(index)",
                startTime: now.addingTimeInterval(-3_600),
                endTime: now,
                durationMs: Int64((index + 1) * 60_000),
                surgePlannedMs: nil,
                surgePoints: 0,
                scyraPoints: index * 10,
                isSoftMode: false,
                arcID: nil,
                arcIndex: nil,
                arcMultiplierUsed: nil,
                arcBonusPoints: 0,
                createdAt: now.addingTimeInterval(TimeInterval(-index * 60))
            )
            _ = repository.commit(session: session, activeArc: nil, recentlyEndedArc: nil)
        }
        let old = FlowSession(
            id: UUID(), flowInstanceID: UUID(), title: "Old", description: "", journeyName: "Old winner",
            startTime: now.addingTimeInterval(-900_000), endTime: now.addingTimeInterval(-900_000),
            durationMs: 9_000_000, surgePlannedMs: nil, surgePoints: 0, scyraPoints: 9_999,
            isSoftMode: false, arcID: nil, arcIndex: nil, arcMultiplierUsed: nil, arcBonusPoints: 0,
            createdAt: now.addingTimeInterval(-900_000)
        )
        _ = repository.commit(session: old, activeArc: nil, recentlyEndedArc: nil)

        let viewModel = StoryViewModel(
            repository: repository,
            now: { now },
            calendar: StoryCalendarRules.appCalendar(timeZone: .gmt)
        )

        #expect(viewModel.topJourneysLast7Days.count == 5)
        #expect(viewModel.topJourneysLast7Days.map(\.journeyName) == [
            "Journey 5", "Journey 4", "Journey 3", "Journey 2", "Journey 1"
        ])
        #expect(!viewModel.topJourneysLast7Days.map(\.journeyName).contains("Old winner"))
    }

    @Test func storyProjectsSavedArcMetadataAndJourneyDrillDownUsesTheActiveWindow() {
        let now = Date(timeIntervalSince1970: 1_100_000)
        let repository = InMemoryFlowRepository()
        let arcID = UUID()
        let first = session(journey: "Build", createdAt: now.addingTimeInterval(-120), arcID: arcID, arcIndex: 1)
        let second = session(journey: "Build", createdAt: now.addingTimeInterval(-60), arcID: arcID, arcIndex: 2)
        let other = session(journey: "Rest", createdAt: now.addingTimeInterval(-30), arcID: nil, arcIndex: nil)
        _ = repository.commit(session: first, activeArc: nil, recentlyEndedArc: nil)
        _ = repository.commit(session: second, activeArc: nil, recentlyEndedArc: nil)
        _ = repository.commit(session: other, activeArc: nil, recentlyEndedArc: nil)
        let metadata = ArcMetadata(arcID: arcID, title: "Launch Arc", outcome: "Shipped")
        repository.saveArcMetadata(metadata, updatedAt: now)

        let viewModel = StoryViewModel(
            repository: repository,
            now: { now },
            calendar: StoryCalendarRules.appCalendar(timeZone: .gmt)
        )
        let arcItem = viewModel.chronicleItems.first { item in
            if case .arc = item { return true }
            return false
        }
        guard let arcItem, case .arc(let group) = arcItem else {
            Issue.record("Expected an Arc group")
            return
        }

        #expect(group.metadata == metadata)
        #expect(viewModel.sessions(forJourney: "Build").map(\.id) == [second.id, first.id])
        #expect(viewModel.sessions(forJourney: "Rest").map(\.id) == [other.id])
    }

    private func session(
        journey: String,
        createdAt: Date,
        arcID: UUID?,
        arcIndex: Int?
    ) -> FlowSession {
        FlowSession(
            id: UUID(), flowInstanceID: UUID(), title: "Flow", description: "", journeyName: journey,
            startTime: createdAt.addingTimeInterval(-60), endTime: createdAt, durationMs: 60_000,
            surgePlannedMs: nil, surgePoints: 0, scyraPoints: 10, isSoftMode: false,
            arcID: arcID, arcIndex: arcIndex, arcMultiplierUsed: arcID == nil ? nil : 1,
            arcBonusPoints: 0, createdAt: createdAt
        )
    }
}
