import Foundation
import Testing
@testable import Scyra

@MainActor
struct ChronicleRepositoryParityTests {
    @Test func textPayloadOrderEditingAndDeletionMatchAndroid() throws {
        let repository = InMemoryFlowRepository()
        let owner = ChronicleOwner.activeFlow(UUID())
        let exact = "  First line\nकथा 🐢  "

        _ = try repository.setChronicleDraft(owner: owner, text: exact)
        var snapshot = try repository.addChronicleText(owner: owner, text: exact)
        #expect(snapshot.draftText.isEmpty)
        #expect(snapshot.textMoments == [exact])

        _ = try repository.addChronicleText(owner: owner, text: "Second")
        snapshot = try repository.addChronicleText(owner: owner, text: "Third")
        let originalIDs = snapshot.moments.map(\.id)
        snapshot = try repository.reorderChronicleMoments(
            owner: owner,
            orderedIDs: [originalIDs[2], originalIDs[0], originalIDs[1]]
        )
        #expect(snapshot.textMoments == ["Third", exact, "Second"])
        #expect(snapshot.moments.map(\.position) == [0, 1, 2])

        snapshot = try repository.updateChronicleText(
            owner: owner,
            momentID: originalIDs[0],
            text: "Edited"
        )
        #expect(snapshot.textMoments == ["Third", "Edited", "Second"])

        snapshot = try repository.deleteChronicleMoment(owner: owner, momentID: originalIDs[2])
        #expect(snapshot.textMoments == ["Edited", "Second"])
        #expect(snapshot.moments.map(\.position) == [0, 1])
    }

    @Test func addTextClearsOnlyTheMatchingDurableDraft() throws {
        let repository = InMemoryFlowRepository()
        let owner = ChronicleOwner.activeFlow(UUID())
        _ = try repository.setChronicleDraft(owner: owner, text: "Newer draft")

        let snapshot = try repository.addChronicleText(owner: owner, text: "Older callback")

        #expect(snapshot.textMoments == ["Older callback"])
        #expect(snapshot.draftText == "Newer draft")
    }

    @Test func swiftDataFlowCommitAtomicallyPromotesChronicleAndRejectsLateWrites() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let flowID = UUID()
        let owner = ChronicleOwner.activeFlow(flowID)
        let exact = "  Kept exactly  "
        _ = try repository.setChronicleDraft(owner: owner, text: exact)
        _ = try repository.addChronicleText(owner: owner, text: exact)

        let session = makeSession(flowInstanceID: flowID)
        _ = try repository.commit(session: session, activeArc: nil, recentlyEndedArc: nil)

        #expect(try repository.fetchChronicle(owner: owner).moments.isEmpty)
        #expect(try repository.fetchChronicle(owner: .session(session.id)).textMoments == [exact])
        #expect(throws: ChronicleRepositoryError.finalizedOwner) {
            try repository.setChronicleDraft(owner: owner, text: "late")
        }
    }

    private func makeSession(flowInstanceID: UUID) -> FlowSession {
        let now = Date(timeIntervalSince1970: 10_000)
        return FlowSession(
            id: UUID(),
            flowInstanceID: flowInstanceID,
            title: "Chronicle Flow",
            description: "",
            journeyName: "Scyra",
            startTime: now.addingTimeInterval(-60),
            endTime: now,
            durationMs: 60_000,
            surgePlannedMs: nil,
            surgePoints: 0,
            scyraPoints: 1,
            isSoftMode: false,
            arcID: nil,
            arcIndex: nil,
            arcMultiplierUsed: nil,
            arcBonusPoints: 0,
            createdAt: now
        )
    }
}

@MainActor
struct PulseRepositoryParityTests {
    @Test func pulseCreationReceiptAndOwnerPromotionSurviveRepositoryRecreation() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let firstRepository = SwiftDataFlowRepository(container: container)
        let creationKey = UUID()
        let owner = ChronicleOwner.pulseDraft(creationKey)
        _ = try firstRepository.setChronicleDraft(owner: owner, text: "moment")
        _ = try firstRepository.addChronicleText(owner: owner, text: "moment")

        let first = try firstRepository.createPulse(
            creationKey: creationKey,
            title: "",
            journeyName: "Ideas",
            parentFlowInstanceID: nil,
            arcID: nil,
            createdAt: Date(timeIntervalSince1970: 20_000)
        )
        let recreatedRepository = SwiftDataFlowRepository(container: container)
        let retry = try recreatedRepository.createPulse(
            creationKey: creationKey,
            title: "ignored retry",
            journeyName: nil,
            parentFlowInstanceID: nil,
            arcID: nil,
            createdAt: Date(timeIntervalSince1970: 30_000)
        )

        #expect(first.id == retry.id)
        #expect(try recreatedRepository.fetchAllPulses().count == 1)
        #expect(try recreatedRepository.fetchChronicle(owner: .pulse(first.id)).textMoments == ["moment"])
        #expect(throws: ChronicleRepositoryError.finalizedOwner) {
            try recreatedRepository.setChronicleDraft(owner: owner, text: "late")
        }
    }

    @Test func livePulseAttachesToTheCompletedFlowAndInheritsItsArc() throws {
        let repository = InMemoryFlowRepository()
        let flowID = UUID()
        let pulseKey = UUID()
        _ = try repository.addChronicleText(
            owner: .pulseDraft(pulseKey),
            text: "Captured during Flow"
        )
        let pulse = try repository.createPulse(
            creationKey: pulseKey,
            title: "Live insight",
            journeyName: "Scyra",
            parentFlowInstanceID: flowID,
            arcID: nil,
            createdAt: Date(timeIntervalSince1970: 40_000)
        )
        let arcID = UUID()
        let session = makeSession(flowInstanceID: flowID, arcID: arcID)

        _ = repository.commit(session: session, activeArc: nil, recentlyEndedArc: nil)
        let attached = try #require(repository.fetchPulse(id: pulse.id))

        #expect(attached.parentSessionID == session.id)
        #expect(attached.parentFlowInstanceID == nil)
        #expect(attached.arcID == arcID)
    }

    @Test func storyNestsAttachedPulsesAndKeepsStandalonePulsesTopLevel() throws {
        let repository = InMemoryFlowRepository()
        let now = Date(timeIntervalSince1970: 50_000)
        let flowID = UUID()
        let session = makeSession(flowInstanceID: flowID, createdAt: now)

        let attached = try makePulse(
            repository: repository,
            title: "Attached",
            journey: "Scyra",
            parentFlowInstanceID: flowID,
            createdAt: now.addingTimeInterval(10)
        )
        _ = repository.commit(session: session, activeArc: nil, recentlyEndedArc: nil)
        let standalone = try makePulse(
            repository: repository,
            title: "Standalone",
            journey: "Life",
            parentFlowInstanceID: nil,
            createdAt: now.addingTimeInterval(20)
        )

        let viewModel = StoryViewModel(
            repository: repository,
            now: { now.addingTimeInterval(30) },
            calendar: StoryCalendarRules.appCalendar(timeZone: .gmt)
        )

        let flowItem = try #require(viewModel.chronicleItems.first { item in
            if case .flow = item { return true }
            return false
        })
        guard case .flow(_, let children) = flowItem else { return }
        #expect(children.map(\.id) == [attached.id])
        #expect(viewModel.chronicleItems.contains { item in
            if case .pulse(let value) = item { return value.id == standalone.id }
            return false
        })

        viewModel.toggleJourney("Scyra")
        #expect(viewModel.visiblePulses.map(\.id) == [attached.id])
    }

    private func makePulse(
        repository: InMemoryFlowRepository,
        title: String,
        journey: String,
        parentFlowInstanceID: UUID?,
        createdAt: Date
    ) throws -> Pulse {
        let key = UUID()
        _ = try repository.addChronicleText(owner: .pulseDraft(key), text: title)
        return try repository.createPulse(
            creationKey: key,
            title: title,
            journeyName: journey,
            parentFlowInstanceID: parentFlowInstanceID,
            arcID: nil,
            createdAt: createdAt
        )
    }

    private func makeSession(
        flowInstanceID: UUID,
        arcID: UUID? = nil,
        createdAt: Date = Date(timeIntervalSince1970: 41_000)
    ) -> FlowSession {
        FlowSession(
            id: UUID(),
            flowInstanceID: flowInstanceID,
            title: "Flow",
            description: "",
            journeyName: "Scyra",
            startTime: createdAt.addingTimeInterval(-60),
            endTime: createdAt,
            durationMs: 60_000,
            surgePlannedMs: nil,
            surgePoints: 0,
            scyraPoints: 1,
            isSoftMode: false,
            arcID: arcID,
            arcIndex: arcID == nil ? nil : 1,
            arcMultiplierUsed: arcID == nil ? nil : 1,
            arcBonusPoints: 0,
            createdAt: createdAt
        )
    }
}

@MainActor
struct ChronicleFlowIntegrationParityTests {
    @Test func completedFlowReadsItsPromotedChronicleInStory() throws {
        let repository = InMemoryFlowRepository()
        let clock = ChronicleTestClock(Date(timeIntervalSince1970: 60_000))
        let flow = FlowViewModel(repository: repository, now: { clock.value })
        flow.updateTitle("Write and work")
        flow.updateJourneyName("Scyra")
        flow.updateChronicleDraft("Exact Flow moment")
        #expect(flow.addChronicleText())
        flow.enterFlowMode()
        clock.advance(seconds: 60)
        flow.refreshElapsed()
        flow.exitFlowMode()
        flow.complete(.saveFlow)

        let session = try #require(repository.fetchAllSessions().first)
        #expect(try repository.fetchChronicle(owner: .session(session.id)).textMoments == ["Exact Flow moment"])

        let story = StoryViewModel(repository: repository, now: { clock.value })
        #expect(story.chronicleForSession(id: session.id).excerpt == "Exact Flow moment")
    }
}

@MainActor
private final class ChronicleTestClock {
    private(set) var value: Date
    init(_ value: Date) { self.value = value }
    func advance(seconds: TimeInterval) { value = value.addingTimeInterval(seconds) }
}
