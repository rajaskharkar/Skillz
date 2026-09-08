import Foundation
import SwiftData
import Testing
@testable import Scyra

@MainActor
struct HorizonArcPlanDomainParityTests {
    @Test func validationRequiresTitleTwoRegularFlowsAndCustomDay() throws {
        let regular = ArcPlanStepInput(
            sourceFlowPlanID: UUID(),
            titleSnapshot: "  Deep Work  ",
            journeyNameSnapshot: "  Work  ",
            isSoftModeSnapshot: false,
            targetMinutesSnapshot: 25,
            launchWithSurgeSnapshot: true,
            linkState: .linked
        )
        let validated = try ArcPlanInput(
            title: "  Launch Sequence  ",
            steps: [regular, regular],
            isInStudio: true,
            recurrence: .custom,
            recurrenceDays: [1, 3, 8]
        ).validated()
        #expect(validated.title == "Launch Sequence")
        #expect(validated.recurrenceDays == [1, 3])
        #expect(validated.steps.allSatisfy { $0.titleSnapshot == "Deep Work" })

        #expect(throws: ArcPlanValidationError.titleRequired) {
            _ = try ArcPlanInput(
                title: " ", steps: [regular, regular], isInStudio: false,
                recurrence: .oneTime, recurrenceDays: []
            ).validated()
        }
        #expect(throws: ArcPlanValidationError.needsTwoFlows) {
            _ = try ArcPlanInput(
                title: "Arc", steps: [regular], isInStudio: false,
                recurrence: .oneTime, recurrenceDays: []
            ).validated()
        }
        #expect(throws: ArcPlanValidationError.softFlowNotAllowed) {
            let soft = ArcPlanStepInput(
                sourceFlowPlanID: UUID(), titleSnapshot: "Soft", journeyNameSnapshot: "Rest",
                isSoftModeSnapshot: true, targetMinutesSnapshot: nil,
                launchWithSurgeSnapshot: false, linkState: .linked
            )
            _ = try ArcPlanInput(
                title: "Arc", steps: [regular, soft], isInStudio: false,
                recurrence: .oneTime, recurrenceDays: []
            ).validated()
        }
        #expect(throws: ArcPlanValidationError.customDayRequired) {
            _ = try ArcPlanInput(
                title: "Arc", steps: [regular, regular], isInStudio: false,
                recurrence: .custom, recurrenceDays: []
            ).validated()
        }
    }

    @Test func allAndroidRecurrenceAndSuggestedSceneContractsArePresent() {
        #expect(ArcPlanRecurrence.allCases == [.oneTime, .daily, .weekdays, .weekly, .custom])
        #expect(SuggestedRoutesCatalog.routes.count == 7)
        #expect(SuggestedRoutesCatalog.routes.first?.title == "Deep Work Launch")
        #expect(SuggestedRoutesCatalog.routes.last?.title == "Body Before Battle")
        #expect(SuggestedRoutesCatalog.routes.allSatisfy { $0.steps.count == 4 })
    }

    @Test func externalArcActionSelectsStudioAndIsConsumedOnce() {
        let viewModel = HorizonViewModel(repository: InMemoryFlowRepository())
        #expect(viewModel.selectedPrimaryTab == .flows)
        #expect(viewModel.pendingNewArcRequestID == nil)

        viewModel.requestNewArcPlan()

        #expect(viewModel.selectedPrimaryTab == .arcs)
        #expect(viewModel.pendingNewArcRequestID != nil)
        #expect(viewModel.consumeNewArcPlanRequest())
        #expect(viewModel.pendingNewArcRequestID == nil)
        #expect(!viewModel.consumeNewArcPlanRequest())
    }
}

@MainActor
struct HorizonArcPlanRepositoryParityTests {
    @Test func swiftDataRoundTripsOrderedSnapshotsRecurrenceStudioAndLaunchState() throws {
        let repository = SwiftDataFlowRepository(
            container: try ScyraPersistenceFactory.makeContainer(inMemory: true)
        )
        let base = Date(timeIntervalSince1970: 200_000)
        let input = makeArcInput(title: "Morning Arc", recurrence: .custom, days: [1, 4])
        let created = try repository.createArcPlan(input: input, at: base)

        #expect(created.steps.map(\.orderIndex) == [0, 1, 2])
        #expect(created.steps.map(\.titleSnapshot) == ["Clear", "Build", "Review"])
        #expect(created.recurrence == .custom)
        #expect(created.recurrenceDays == [1, 4])
        #expect(created.totalTargetMinutes == 60)
        #expect(created.hasSurge)

        try repository.setArcPlanInStudio(id: created.id, isInStudio: true, at: base.addingTimeInterval(1))
        let launch = try repository.beginPlannedArc(id: created.id, restart: false, at: base.addingTimeInterval(2))
        #expect(launch.step.titleSnapshot == "Clear")
        #expect(launch.run.currentStepIndex == 0)
        #expect(launch.runtime.sessionCount == 0)
        #expect(launch.runtime.multiplier == ArcRuntimeState.baseMultiplier)

        let storedPlan = try repository.fetchArcPlan(id: created.id)
        let fetched = try #require(storedPlan)
        #expect(fetched.isInStudio)
        #expect(fetched.launchCount == 1)
        #expect(fetched.lastLaunchedAt == base.addingTimeInterval(2))
        #expect(try repository.fetchActivePlannedArcRun()?.arcPlanID == created.id)
    }

    @Test func advanceIsDurableAndFinalStepClearsInsteadOfRepeating() throws {
        let repository = InMemoryFlowRepository()
        let base = Date(timeIntervalSince1970: 210_000)
        let plan = try repository.createArcPlan(input: makeArcInput(), at: base)
        _ = try repository.beginPlannedArc(id: plan.id, restart: false, at: base)

        let first = repository.advanceActivePlannedArc(at: base.addingTimeInterval(1))
        guard case .advanced(let firstRun) = first else {
            Issue.record("Expected first step advancement")
            return
        }
        #expect(firstRun.currentStepIndex == 1)
        #expect(firstRun.currentStepTitle == "Build")

        let second = repository.advanceActivePlannedArc(at: base.addingTimeInterval(2))
        guard case .advanced(let secondRun) = second else {
            Issue.record("Expected second step advancement")
            return
        }
        #expect(secondRun.currentStepIndex == 2)
        #expect(repository.advanceActivePlannedArc(at: base.addingTimeInterval(3)) == .completed)
        #expect(repository.fetchActivePlannedArcRun() == nil)
    }

    @Test func swiftDataFlowCommitAdvancesPlannedRunAtomicallyAndRetryIsIdempotent() throws {
        let repository = SwiftDataFlowRepository(
            container: try ScyraPersistenceFactory.makeContainer(inMemory: true)
        )
        let base = Date(timeIntervalSince1970: 215_000)
        let plan = try repository.createArcPlan(input: makeArcInput(), at: base)
        let launch = try repository.beginPlannedArc(id: plan.id, restart: false, at: base)
        let session = FlowSession(
            id: UUID(), flowInstanceID: UUID(), title: launch.step.titleSnapshot,
            description: "", journeyName: launch.step.journeyNameSnapshot ?? "Focus",
            startTime: base, endTime: base, durationMs: 0, surgePlannedMs: nil,
            surgePoints: 0, scyraPoints: 0, isSoftMode: false,
            arcID: launch.runtime.id, arcIndex: 1, arcMultiplierUsed: 1,
            arcBonusPoints: 0, createdAt: base
        )
        _ = try repository.commit(
            session: session,
            activeArc: launch.runtime,
            recentlyEndedArc: nil,
            movement: nil,
            originPulseID: nil,
            plannedArcAction: .advance
        )
        #expect(try repository.fetchActivePlannedArcRun()?.currentStepIndex == 1)
        #expect(try repository.fetchAllSessions().count == 1)

        _ = try repository.commit(
            session: session,
            activeArc: launch.runtime,
            recentlyEndedArc: nil,
            movement: nil,
            originPulseID: nil,
            plannedArcAction: .advance
        )
        #expect(try repository.fetchActivePlannedArcRun()?.currentStepIndex == 1)
        #expect(try repository.fetchAllSessions().count == 1)
    }

    @Test func restartIncrementsLaunchButResumeDoesNotAndFlowDeletionDetachesSnapshot() throws {
        let repository = InMemoryFlowRepository()
        let base = Date(timeIntervalSince1970: 220_000)
        let source = repository.createFlowPlan(
            input: FlowPlanInput(
                title: "Linked", journeyName: "Work", isSoftMode: false,
                targetMinutes: 20, launchWithSurge: false
            ),
            at: base
        )
        let linked = ArcPlanStepInput(
            sourceFlowPlanID: source.id, titleSnapshot: source.title,
            journeyNameSnapshot: source.journeyName, isSoftModeSnapshot: false,
            targetMinutesSnapshot: source.targetMinutes, launchWithSurgeSnapshot: false,
            linkState: .linked
        )
        let plan = try repository.createArcPlan(
            input: ArcPlanInput(
                title: "Linked Arc", steps: [linked, linked], isInStudio: false,
                recurrence: .oneTime, recurrenceDays: []
            ),
            at: base
        )
        _ = try repository.beginPlannedArc(id: plan.id, restart: false, at: base)
        _ = try repository.beginPlannedArc(id: plan.id, restart: false, at: base.addingTimeInterval(1))
        #expect(repository.fetchArcPlan(id: plan.id)?.launchCount == 1)
        _ = try repository.beginPlannedArc(id: plan.id, restart: true, at: base.addingTimeInterval(2))
        #expect(repository.fetchArcPlan(id: plan.id)?.launchCount == 2)

        repository.deleteFlowPlan(id: source.id)
        let detached = try #require(repository.fetchArcPlan(id: plan.id))
        #expect(detached.steps.allSatisfy { $0.sourceFlowPlanID == nil && $0.linkState == .detached })
        #expect(detached.steps.allSatisfy { $0.titleSnapshot == "Linked" })
    }

    @Test func versionTwoStoreLightweightMigratesToArcPlanSchema() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("scyra-arc-plan-migration-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let storeURL = directory.appendingPathComponent("Scyra.store")

        do {
            let schema = Schema(versionedSchema: ScyraSchemaV2.self)
            let configuration = ModelConfiguration("Scyra", schema: schema, url: storeURL)
            let container = try ModelContainer(for: schema, configurations: [configuration])
            container.mainContext.insert(FlowPlanModel(plan: FlowPlan(
                id: UUID(), title: "Legacy plan", journeyName: "Work", isSoftMode: false,
                targetMinutes: 25, launchWithSurge: true, pinned: false, archived: false,
                launchCount: 0, lastLaunchedAt: nil, createdAt: .now, updatedAt: .now
            )))
            try container.mainContext.save()
        }

        let schema = Schema(versionedSchema: ScyraSchemaV3.self)
        let configuration = ModelConfiguration("Scyra", schema: schema, url: storeURL)
        let upgraded = try ModelContainer(
            for: schema,
            migrationPlan: ScyraMigrationPlan.self,
            configurations: [configuration]
        )
        let repository = SwiftDataFlowRepository(container: upgraded)
        #expect(try repository.fetchActiveFlowPlans().map(\.title) == ["Legacy plan"])
        let arc = try repository.createArcPlan(input: makeArcInput(), at: .now)
        #expect(try repository.fetchArcPlan(id: arc.id)?.steps.count == 3)
    }
}

@MainActor
struct HorizonArcPlanFlowIntegrationTests {
    @Test func plannedArcLaunchPrefillsAndCommitAdvancesBeforeRewardDismissal() throws {
        let repository = InMemoryFlowRepository()
        let clock = ArcPlanTestClock(Date(timeIntervalSince1970: 230_000))
        let plan = try repository.createArcPlan(input: makeArcInput(), at: clock.value)
        let flow = FlowViewModel(repository: repository, now: { clock.value })

        #expect(flow.prepareFromArcPlan(plan))
        #expect(flow.title == "Clear")
        #expect(flow.journeyName == "Focus")
        #expect(flow.plannedArcTitle == plan.title)
        #expect(flow.plannedArcStepIndex == 0)
        #expect(flow.plannedArcTotalSteps == 3)
        #expect(flow.activeArc?.sessionCount == 0)

        flow.complete(.continueArc)
        #expect(flow.reward != nil)
        #expect(flow.reward?.arcMultiplierUsed == ArcRuntimeState.baseMultiplier)
        #expect(flow.plannedArcStepIndex == 0)
        #expect(repository.fetchActivePlannedArcRun()?.currentStepIndex == 1)

        #expect(!flow.finishReward())
        #expect(flow.title == "Build")
        #expect(flow.journeyName == "Work")
        #expect(flow.plannedArcStepIndex == 1)
        #expect(flow.surgePlannedMinutes == 30)
        #expect(repository.fetchActiveFlow()?.title == "Build")
    }

    @Test func relaunchAfterCommitRestoresDurablyAdvancedStep() throws {
        let repository = InMemoryFlowRepository()
        let clock = ArcPlanTestClock(Date(timeIntervalSince1970: 240_000))
        let plan = try repository.createArcPlan(input: makeArcInput(), at: clock.value)
        let original = FlowViewModel(repository: repository, now: { clock.value })
        #expect(original.prepareFromArcPlan(plan))
        original.complete(.continueArc)

        let restored = FlowViewModel(repository: repository, now: { clock.value })
        #expect(restored.title == "Build")
        #expect(restored.plannedArcStepIndex == 1)
        #expect(restored.activeArc?.sessionCount == 1)
    }

    @Test func finalPlannedStepCompletesRunAndLeavesBlankArcContinuation() throws {
        let repository = InMemoryFlowRepository()
        let clock = ArcPlanTestClock(Date(timeIntervalSince1970: 250_000))
        let plan = try repository.createArcPlan(input: makeArcInput(), at: clock.value)
        let flow = FlowViewModel(repository: repository, now: { clock.value })
        #expect(flow.prepareFromArcPlan(plan))

        for expectedIndex in 1...2 {
            flow.complete(.continueArc)
            #expect(!flow.finishReward())
            #expect(flow.plannedArcStepIndex == expectedIndex)
        }
        flow.complete(.continueArc)
        #expect(repository.fetchActivePlannedArcRun() == nil)
        #expect(!flow.finishReward())
        #expect(flow.title.isEmpty)
        #expect(flow.plannedArcTitle == nil)
        #expect(flow.activeArc != nil)
    }

    @Test func completingArcClearsPlannedRunAndMeaningfulFlowBlocksReplacement() throws {
        let repository = InMemoryFlowRepository()
        let clock = ArcPlanTestClock(Date(timeIntervalSince1970: 260_000))
        let first = try repository.createArcPlan(input: makeArcInput(title: "First"), at: clock.value)
        let second = try repository.createArcPlan(input: makeArcInput(title: "Second"), at: clock.value)
        let flow = FlowViewModel(repository: repository, now: { clock.value })
        #expect(flow.prepareFromArcPlan(first))
        flow.enterFlowMode()
        #expect(!flow.prepareFromArcPlan(second))
        #expect(repository.fetchActivePlannedArcRun()?.arcPlanID == first.id)

        clock.value = clock.value.addingTimeInterval(60)
        flow.exitFlowMode()
        flow.complete(.completeArc)
        #expect(repository.fetchActivePlannedArcRun() == nil)
        #expect(flow.activeArc == nil)
    }
}

@MainActor
private func makeArcInput(
    title: String = "Launch Arc",
    recurrence: ArcPlanRecurrence = .oneTime,
    days: Set<Int> = []
) -> ArcPlanInput {
    let definitions: [(String, String, Int?, Bool)] = [
        ("Clear", "Focus", 10, false),
        ("Build", "Work", 30, true),
        ("Review", "Planning", 20, false)
    ]
    return ArcPlanInput(
        title: title,
        steps: definitions.map { title, journey, minutes, surge in
            ArcPlanStepInput(
                sourceFlowPlanID: UUID(), titleSnapshot: title,
                journeyNameSnapshot: journey, isSoftModeSnapshot: false,
                targetMinutesSnapshot: minutes, launchWithSurgeSnapshot: surge,
                linkState: .linked
            )
        },
        isInStudio: false,
        recurrence: recurrence,
        recurrenceDays: days
    )
}

@MainActor
private final class ArcPlanTestClock {
    var value: Date
    init(_ value: Date) { self.value = value }
}
