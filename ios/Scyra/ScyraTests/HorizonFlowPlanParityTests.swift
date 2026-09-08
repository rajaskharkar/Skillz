import Foundation
import SwiftData
import Testing
@testable import Scyra

@MainActor
struct HorizonFlowPlanDomainParityTests {
    @Test func validationNormalizesTitleJourneySoftAndSurgeLikeAndroid() throws {
        let regular = try FlowPlanDraft(
            title: "  Deep Work  ",
            journeyName: "  Writing  ",
            isSoftMode: false,
            targetMinutesText: "25",
            launchWithSurge: true
        ).validated()
        #expect(regular.title == "Deep Work")
        #expect(regular.journeyName == "Writing")
        #expect(regular.targetMinutes == 25)
        #expect(regular.launchWithSurge)

        let soft = try FlowPlanDraft(
            title: "Read",
            journeyName: "",
            isSoftMode: true,
            targetMinutesText: "30",
            launchWithSurge: true
        ).validated()
        #expect(soft.journeyName == nil)
        #expect(soft.targetMinutes == nil)
        #expect(!soft.launchWithSurge)

        #expect(throws: FlowPlanValidationError.titleRequired) {
            try FlowPlanDraft(title: "   ").validated()
        }
        #expect(throws: FlowPlanValidationError.targetMinutesMustBePositive) {
            try FlowPlanDraft(title: "Work", targetMinutesText: "0").validated()
        }
    }

    @Test func allCanonicalTimeLensStatesRemainAvailableWithoutInventedFiltering() {
        #expect(HorizonTimeLens.allCases == [.day, .week, .month])
    }
}

@MainActor
struct HorizonFlowPlanRepositoryParityTests {
    @Test func inMemorySortPinDreamLaunchRestoreAndDeleteMatchAndroidQueries() throws {
        let repository = InMemoryFlowRepository()
        let base = Date(timeIntervalSince1970: 100_000)
        let first = repository.createFlowPlan(
            input: input(title: "Alpha"),
            at: base
        )
        let second = repository.createFlowPlan(
            input: input(title: "Beta"),
            at: base.addingTimeInterval(1)
        )
        #expect(repository.fetchActiveFlowPlans().map(\.id) == [second.id, first.id])

        try repository.setFlowPlanPinned(id: first.id, pinned: true, at: base.addingTimeInterval(2))
        #expect(repository.fetchActiveFlowPlans().map(\.id) == [first.id, second.id])

        try repository.markFlowPlanLaunched(id: first.id, at: base.addingTimeInterval(3))
        let launched = try #require(repository.fetchFlowPlan(id: first.id))
        #expect(launched.launchCount == 1)
        #expect(launched.lastLaunchedAt == base.addingTimeInterval(3))

        try repository.setFlowPlanArchived(id: first.id, archived: true, at: base.addingTimeInterval(4))
        #expect(repository.fetchActiveFlowPlans().map(\.id) == [second.id])
        #expect(repository.fetchArchivedFlowPlans().map(\.id) == [first.id])

        try repository.setFlowPlanArchived(id: first.id, archived: false, at: base.addingTimeInterval(5))
        #expect(repository.fetchActiveFlowPlans().map(\.id) == [first.id, second.id])
        repository.deleteFlowPlan(id: first.id)
        #expect(repository.fetchFlowPlan(id: first.id) == nil)
    }

    @Test func swiftDataRoundTripsEveryFlowPlanFieldAndMutatesDurably() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let base = Date(timeIntervalSince1970: 110_000)
        let created = try repository.createFlowPlan(
            input: input(
                title: "Focused writing",
                journey: "Novel",
                target: 45,
                surge: true
            ),
            at: base
        )
        try repository.setFlowPlanPinned(id: created.id, pinned: true, at: base.addingTimeInterval(1))
        try repository.markFlowPlanLaunched(id: created.id, at: base.addingTimeInterval(2))
        try repository.setFlowPlanArchived(id: created.id, archived: true, at: base.addingTimeInterval(3))

        let recreated = SwiftDataFlowRepository(container: container)
        let fetched = try recreated.fetchFlowPlan(id: created.id)
        let stored = try #require(fetched)
        #expect(stored.title == "Focused writing")
        #expect(stored.journeyName == "Novel")
        #expect(stored.targetMinutes == 45)
        #expect(stored.launchWithSurge)
        #expect(stored.pinned)
        #expect(stored.archived)
        #expect(stored.launchCount == 1)
        #expect(stored.lastLaunchedAt == base.addingTimeInterval(2))
        #expect(try recreated.fetchArchivedFlowPlans() == [stored])
    }

    @Test func versionOneStoreLightweightMigratesToFlowPlanSchema() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("scyra-flow-plan-migration-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let storeURL = directory.appendingPathComponent("Scyra.store")

        do {
            // The shipped store was created from an unversioned Schema. Its
            // default 1.0.0 identity must be recognized as migration V1.
            let schema = Schema(ScyraSchemaV1.models)
            let configuration = ModelConfiguration("Scyra", schema: schema, url: storeURL)
            let container = try ModelContainer(for: schema, configurations: [configuration])
            container.mainContext.insert(PearlLedgerModel(entry: PearlLedgerEntry(
                id: "legacy-entry",
                delta: 7,
                reason: "legacy",
                sourceType: "test",
                sourceID: nil,
                createdAt: .now,
                note: nil
            )))
            try container.mainContext.save()
        }

        let schema = Schema(versionedSchema: ScyraSchemaV2.self)
        let configuration = ModelConfiguration("Scyra", schema: schema, url: storeURL)
        let upgraded = try ModelContainer(
            for: schema,
            migrationPlan: ScyraMigrationPlan.self,
            configurations: [configuration]
        )
        let repository = SwiftDataFlowRepository(container: upgraded)
        #expect(try repository.fetchPearlBalance() == 7)
        let created = try repository.createFlowPlan(input: input(title: "After upgrade"), at: .now)
        #expect(try repository.fetchFlowPlan(id: created.id) == created)
    }

    private func input(
        title: String,
        journey: String? = nil,
        soft: Bool = false,
        target: Int? = nil,
        surge: Bool = false
    ) -> FlowPlanInput {
        FlowPlanInput(
            title: title,
            journeyName: journey,
            isSoftMode: soft,
            targetMinutes: target,
            launchWithSurge: surge
        )
    }
}

@MainActor
struct HorizonFlowPlanViewModelParityTests {
    @Test func plannedPrefillReplacesIdleDraftPersistsPresetAndBlocksActiveConflict() throws {
        let repository = InMemoryFlowRepository()
        let clock = HorizonTestClock(Date(timeIntervalSince1970: 120_000))
        let flow = FlowViewModel(repository: repository, now: { clock.value })
        flow.updateTitle("Abandoned draft")
        let plan = makePlan(
            title: "Deep work",
            journey: "Craft",
            target: 30,
            surge: true,
            at: clock.value
        )

        #expect(flow.prepareFromPlan(plan))
        #expect(flow.title == "Deep work")
        #expect(flow.journeyName == "Craft")
        #expect(flow.mode == .flow)
        #expect(flow.surgePlannedMinutes == 30)
        #expect(repository.fetchActiveFlow()?.title == "Deep work")

        flow.enterFlowMode()
        let other = makePlan(title: "Should not replace", soft: true, at: clock.value)
        #expect(!flow.prepareFromPlan(other))
        #expect(flow.title == "Deep work")
        #expect(flow.mode == .flow)
    }

    @Test func softPlanClearsSurgeAndAbandonedPulseOrigin() {
        let repository = InMemoryFlowRepository()
        let clock = HorizonTestClock(Date(timeIntervalSince1970: 130_000))
        let flow = FlowViewModel(repository: repository, now: { clock.value })
        let pulseContext = PulseLaunchContext(
            pulseID: UUID(),
            title: "Old idea",
            description: "",
            journeyName: "Ideas"
        )
        #expect(flow.prepareFromPulse(pulseContext))

        let soft = makePlan(title: "Gentle reading", journey: "Books", soft: true, at: clock.value)
        #expect(flow.prepareFromPlan(soft))
        #expect(flow.mode == .soft)
        #expect(flow.surgePlannedMs == nil)
        #expect(flow.originPulseID == nil)
        #expect(repository.fetchActiveFlow()?.originPulseID == nil)
    }

    @Test func horizonViewModelCreatesEditsAndRecordsOnlyExplicitSuccessfulLaunch() throws {
        let repository = InMemoryFlowRepository()
        let clock = HorizonTestClock(Date(timeIntervalSince1970: 140_000))
        let viewModel = HorizonViewModel(repository: repository, now: { clock.value })
        let draft = FlowPlanDraft(
            title: "Plan",
            journeyName: "Work",
            isSoftMode: false,
            targetMinutesText: "20",
            launchWithSurge: true
        )
        #expect(viewModel.save(draft))
        let plan = try #require(viewModel.activePlans.first)
        #expect(plan.launchCount == 0)

        clock.advance(seconds: 1)
        viewModel.recordSuccessfulLaunch(plan)
        #expect(viewModel.activePlans.first?.launchCount == 1)
        #expect(viewModel.activePlans.first?.lastLaunchedAt == clock.value)

        var edited = draft
        edited.title = "Updated Plan"
        clock.advance(seconds: 1)
        #expect(viewModel.save(edited, editing: plan.id))
        #expect(viewModel.activePlans.first?.title == "Updated Plan")
        #expect(viewModel.journeySuggestions == ["Work"])

        viewModel.selectedTimeLens = .month
        viewModel.refresh()
        #expect(viewModel.selectedTimeLens == .month)
    }

    private func makePlan(
        title: String,
        journey: String? = nil,
        soft: Bool = false,
        target: Int? = nil,
        surge: Bool = false,
        at date: Date
    ) -> FlowPlan {
        FlowPlan(
            id: UUID(),
            title: title,
            journeyName: journey,
            isSoftMode: soft,
            targetMinutes: target,
            launchWithSurge: surge,
            pinned: false,
            archived: false,
            launchCount: 0,
            lastLaunchedAt: nil,
            createdAt: date,
            updatedAt: date
        )
    }
}

@MainActor
private final class HorizonTestClock {
    private(set) var value: Date
    init(_ value: Date) { self.value = value }
    func advance(seconds: TimeInterval) { value = value.addingTimeInterval(seconds) }
}
