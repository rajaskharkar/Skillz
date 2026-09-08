import Foundation
import SwiftData

@MainActor
protocol ArcPlanRepository: AnyObject {
    func fetchActiveArcPlans() throws -> [ArcPlan]
    func fetchArcPlan(id: UUID) throws -> ArcPlan?
    func createArcPlan(input: ArcPlanInput, at date: Date) throws -> ArcPlan
    func updateArcPlan(id: UUID, input: ArcPlanInput, at date: Date) throws
    func setArcPlanInStudio(id: UUID, isInStudio: Bool, at date: Date) throws
    func deleteArcPlan(id: UUID) throws
    func fetchActivePlannedArcRun() throws -> ActivePlannedArcRun?
    func beginPlannedArc(id: UUID, restart: Bool, at date: Date) throws -> PlannedArcLaunch
    func advanceActivePlannedArc(at date: Date) throws -> PlannedArcAdvanceResult
    func clearActivePlannedArcRun() throws
}

extension SwiftDataFlowRepository {
    func fetchActiveArcPlans() throws -> [ArcPlan] {
        try context.fetch(FetchDescriptor<ArcPlanModel>())
            .filter { !$0.archived }
            .map(arcPlan(from:))
            .sorted(by: Self.arcPlanSort)
    }

    func fetchArcPlan(id: UUID) throws -> ArcPlan? {
        try arcPlanModel(id: id).map(arcPlan(from:))
    }

    func createArcPlan(input: ArcPlanInput, at date: Date) throws -> ArcPlan {
        let validated = try input.validated()
        do {
            let model = ArcPlanModel(input: validated, now: date)
            context.insert(model)
            try replaceSteps(on: model, with: validated.steps, at: date)
            try context.save()
            return arcPlan(from: model)
        } catch {
            context.rollback()
            throw error
        }
    }

    func updateArcPlan(id: UUID, input: ArcPlanInput, at date: Date) throws {
        let validated = try input.validated()
        guard let model = try arcPlanModel(id: id) else { throw ArcPlanValidationError.notFound }
        do {
            model.update(input: validated, at: date)
            try replaceSteps(on: model, with: validated.steps, at: date)
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func setArcPlanInStudio(id: UUID, isInStudio: Bool, at date: Date) throws {
        guard let model = try arcPlanModel(id: id) else { throw ArcPlanValidationError.notFound }
        do {
            model.isInStudio = isInStudio
            model.updatedAt = date
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func deleteArcPlan(id: UUID) throws {
        guard let model = try arcPlanModel(id: id) else { return }
        do {
            if let run = try activePlannedArcRunModel(), run.arcPlanID == id {
                context.delete(run)
                if let activeArc = try arcStateModel(slot: ArcPersistenceSlot.active) {
                    context.delete(activeArc)
                }
            }
            context.delete(model)
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func fetchActivePlannedArcRun() throws -> ActivePlannedArcRun? {
        try activePlannedArcRunModel().map(Self.activeRun(from:))
    }

    func beginPlannedArc(id: UUID, restart: Bool, at date: Date) throws -> PlannedArcLaunch {
        guard let model = try arcPlanModel(id: id) else { throw ArcPlanValidationError.notFound }
        let plan = arcPlan(from: model)
        guard !plan.steps.isEmpty else { throw ArcPlanValidationError.noSteps }

        if !restart,
           let existing = try activePlannedArcRunModel(),
           existing.arcPlanID == id,
           plan.steps.indices.contains(existing.currentStepIndex) {
            let storedRuntime = try fetchActiveArc()
            let runtime = storedRuntime ?? makePlannedArcRuntime(at: existing.startedAt)
            if storedRuntime == nil {
                do {
                    try upsertActiveArc(runtime)
                    try context.save()
                } catch {
                    context.rollback()
                    throw error
                }
            }
            return PlannedArcLaunch(
                plan: plan,
                run: Self.activeRun(from: existing),
                step: plan.steps[existing.currentStepIndex],
                runtime: runtime
            )
        }

        let first = plan.steps[0]
        let run = ActivePlannedArcRun(
            arcPlanID: plan.id,
            arcTitle: plan.title,
            currentStepIndex: 0,
            totalSteps: plan.steps.count,
            currentStepTitle: first.titleSnapshot,
            currentJourneyName: first.journeyNameSnapshot,
            currentIsSoftMode: first.isSoftModeSnapshot,
            startedAt: date,
            updatedAt: date
        )
        let runtime = makePlannedArcRuntime(at: date)

        do {
            model.launchCount += 1
            model.lastLaunchedAt = date
            model.updatedAt = date
            if let existing = try activePlannedArcRunModel() {
                existing.update(from: run)
            } else {
                context.insert(ActivePlannedArcRunModel(run: run))
            }
            try upsertActiveArc(runtime)
            try context.save()
            return PlannedArcLaunch(plan: arcPlan(from: model), run: run, step: first, runtime: runtime)
        } catch {
            context.rollback()
            throw error
        }
    }

    func advanceActivePlannedArc(at date: Date) throws -> PlannedArcAdvanceResult {
        do {
            let result = try applyPlannedArcCompletionForCommit(.advance, at: date)
            try context.save()
            return result
        } catch {
            context.rollback()
            throw error
        }
    }

    func clearActivePlannedArcRun() throws {
        do {
            if let run = try activePlannedArcRunModel() { context.delete(run) }
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    /// Mutates the current ModelContext without saving so Flow completion can
    /// advance the planned run in the same SwiftData transaction as its session.
    func applyPlannedArcCompletionForCommit(
        _ action: PlannedArcCompletionAction,
        at date: Date
    ) throws -> PlannedArcAdvanceResult {
        guard let runModel = try activePlannedArcRunModel() else { return .notPlannedArc }
        switch action {
        case .unchanged:
            return .notPlannedArc
        case .complete:
            context.delete(runModel)
            return .completed
        case .advance:
            let nextIndex = runModel.currentStepIndex + 1
            guard nextIndex < runModel.totalSteps,
                  let plan = try fetchArcPlan(id: runModel.arcPlanID),
                  let next = plan.steps.first(where: { $0.orderIndex == nextIndex }) else {
                context.delete(runModel)
                return .completed
            }
            runModel.currentStepIndex = nextIndex
            runModel.currentStepTitle = next.titleSnapshot
            runModel.currentJourneyName = next.journeyNameSnapshot
            runModel.currentIsSoftMode = next.isSoftModeSnapshot
            runModel.updatedAt = date
            return .advanced(Self.activeRun(from: runModel))
        }
    }

    func detachArcPlanSteps(sourceFlowPlanID: UUID, at date: Date) throws {
        for step in try context.fetch(FetchDescriptor<ArcPlanStepModel>())
        where step.sourceFlowPlanID == sourceFlowPlanID {
            step.sourceFlowPlanID = nil
            step.linkStateRawValue = ArcPlanStepLinkState.detached.rawValue
            step.updatedAt = date
        }
    }

    private func replaceSteps(
        on plan: ArcPlanModel,
        with inputs: [ArcPlanStepInput],
        at date: Date
    ) throws {
        let existing = try context.fetch(FetchDescriptor<ArcPlanStepModel>())
            .filter { $0.plan?.id == plan.id }
        existing.forEach(context.delete)
        plan.steps.removeAll()
        for (index, input) in inputs.enumerated() {
            let step = ArcPlanStepModel(orderIndex: index, input: input, plan: plan, now: date)
            context.insert(step)
            plan.steps.append(step)
        }
    }

    private func arcPlanModel(id: UUID) throws -> ArcPlanModel? {
        try context.fetch(FetchDescriptor<ArcPlanModel>()).first { $0.id == id }
    }

    private func activePlannedArcRunModel() throws -> ActivePlannedArcRunModel? {
        try context.fetch(FetchDescriptor<ActivePlannedArcRunModel>()).first {
            $0.id == ActivePlannedArcRun.singletonID
        }
    }

    private func arcStateModel(slot: String) throws -> ArcStateModel? {
        try context.fetch(FetchDescriptor<ArcStateModel>()).first { $0.slot == slot }
    }

    private func upsertActiveArc(_ runtime: ArcRuntimeState) throws {
        if let model = try arcStateModel(slot: ArcPersistenceSlot.active) {
            model.update(from: runtime)
        } else {
            context.insert(ArcStateModel(slot: ArcPersistenceSlot.active, state: runtime))
        }
    }

    private func makePlannedArcRuntime(at date: Date) -> ArcRuntimeState {
        ArcRuntimeState(
            id: UUID(),
            isPending: true,
            multiplier: ArcRuntimeState.baseMultiplier,
            progressMs: 0,
            lastSessionEndTime: date,
            sessionCount: 0,
            pauseUsedMs: 0,
            pauseStartedAt: nil
        )
    }

    private func arcPlan(from model: ArcPlanModel) -> ArcPlan {
        ArcPlan(
            id: model.id,
            title: model.title,
            isInStudio: model.isInStudio,
            archived: model.archived,
            launchCount: model.launchCount,
            lastLaunchedAt: model.lastLaunchedAt,
            recurrence: ArcPlanRecurrence(rawValue: model.recurrenceRawValue) ?? .oneTime,
            recurrenceDays: Set(model.recurrenceDaysCSV.split(separator: ",").compactMap { Int($0) }),
            steps: model.steps.map(Self.arcStep(from:)).sorted {
                $0.orderIndex == $1.orderIndex ? $0.id.uuidString < $1.id.uuidString : $0.orderIndex < $1.orderIndex
            },
            createdAt: model.createdAt,
            updatedAt: model.updatedAt
        )
    }

    private static func arcStep(from model: ArcPlanStepModel) -> ArcPlanStep {
        ArcPlanStep(
            id: model.id,
            orderIndex: model.orderIndex,
            sourceFlowPlanID: model.sourceFlowPlanID,
            titleSnapshot: model.titleSnapshot,
            journeyNameSnapshot: model.journeyNameSnapshot,
            isSoftModeSnapshot: model.isSoftModeSnapshot,
            targetMinutesSnapshot: model.targetMinutesSnapshot,
            launchWithSurgeSnapshot: model.launchWithSurgeSnapshot,
            linkState: ArcPlanStepLinkState(rawValue: model.linkStateRawValue) ?? .detached,
            createdAt: model.createdAt,
            updatedAt: model.updatedAt
        )
    }

    private static func activeRun(from model: ActivePlannedArcRunModel) -> ActivePlannedArcRun {
        ActivePlannedArcRun(
            arcPlanID: model.arcPlanID,
            arcTitle: model.arcTitle,
            currentStepIndex: model.currentStepIndex,
            totalSteps: model.totalSteps,
            currentStepTitle: model.currentStepTitle,
            currentJourneyName: model.currentJourneyName,
            currentIsSoftMode: model.currentIsSoftMode,
            startedAt: model.startedAt,
            updatedAt: model.updatedAt
        )
    }

    static func arcPlanSort(_ lhs: ArcPlan, _ rhs: ArcPlan) -> Bool {
        if lhs.updatedAt != rhs.updatedAt { return lhs.updatedAt > rhs.updatedAt }
        return lhs.title.localizedCaseInsensitiveCompare(rhs.title) == .orderedAscending
    }
}

extension InMemoryFlowRepository {
    func fetchActiveArcPlans() -> [ArcPlan] {
        arcPlans.filter { !$0.archived }.sorted(by: Self.arcPlanSort)
    }

    func fetchArcPlan(id: UUID) -> ArcPlan? { arcPlans.first { $0.id == id } }

    func createArcPlan(input: ArcPlanInput, at date: Date) throws -> ArcPlan {
        let validated = try input.validated()
        let plan = ArcPlan(
            id: UUID(),
            title: validated.title,
            isInStudio: validated.isInStudio,
            archived: false,
            launchCount: 0,
            lastLaunchedAt: nil,
            recurrence: validated.recurrence,
            recurrenceDays: validated.recurrenceDays,
            steps: makeSteps(validated.steps, at: date),
            createdAt: date,
            updatedAt: date
        )
        arcPlans.append(plan)
        return plan
    }

    func updateArcPlan(id: UUID, input: ArcPlanInput, at date: Date) throws {
        let validated = try input.validated()
        guard let index = arcPlans.firstIndex(where: { $0.id == id }) else {
            throw ArcPlanValidationError.notFound
        }
        arcPlans[index].title = validated.title
        arcPlans[index].isInStudio = validated.isInStudio
        arcPlans[index].recurrence = validated.recurrence
        arcPlans[index].recurrenceDays = validated.recurrenceDays
        arcPlans[index].steps = makeSteps(validated.steps, at: date)
        arcPlans[index].updatedAt = date
    }

    func setArcPlanInStudio(id: UUID, isInStudio: Bool, at date: Date) throws {
        guard let index = arcPlans.firstIndex(where: { $0.id == id }) else {
            throw ArcPlanValidationError.notFound
        }
        arcPlans[index].isInStudio = isInStudio
        arcPlans[index].updatedAt = date
    }

    func deleteArcPlan(id: UUID) {
        arcPlans.removeAll { $0.id == id }
        if activePlannedArcRun?.arcPlanID == id {
            activePlannedArcRun = nil
            activeArc = nil
        }
    }

    func fetchActivePlannedArcRun() -> ActivePlannedArcRun? { activePlannedArcRun }

    func beginPlannedArc(id: UUID, restart: Bool, at date: Date) throws -> PlannedArcLaunch {
        guard let index = arcPlans.firstIndex(where: { $0.id == id }) else {
            throw ArcPlanValidationError.notFound
        }
        guard !arcPlans[index].steps.isEmpty else { throw ArcPlanValidationError.noSteps }
        if !restart,
           let run = activePlannedArcRun,
           run.arcPlanID == id,
           arcPlans[index].steps.indices.contains(run.currentStepIndex) {
            let runtime = activeArc ?? makeInMemoryPlannedArcRuntime(at: run.startedAt)
            activeArc = runtime
            return PlannedArcLaunch(
                plan: arcPlans[index],
                run: run,
                step: arcPlans[index].steps[run.currentStepIndex],
                runtime: runtime
            )
        }
        let first = arcPlans[index].steps[0]
        let run = ActivePlannedArcRun(
            arcPlanID: id,
            arcTitle: arcPlans[index].title,
            currentStepIndex: 0,
            totalSteps: arcPlans[index].steps.count,
            currentStepTitle: first.titleSnapshot,
            currentJourneyName: first.journeyNameSnapshot,
            currentIsSoftMode: first.isSoftModeSnapshot,
            startedAt: date,
            updatedAt: date
        )
        let runtime = makeInMemoryPlannedArcRuntime(at: date)
        arcPlans[index].launchCount += 1
        arcPlans[index].lastLaunchedAt = date
        arcPlans[index].updatedAt = date
        activePlannedArcRun = run
        activeArc = runtime
        return PlannedArcLaunch(plan: arcPlans[index], run: run, step: first, runtime: runtime)
    }

    func advanceActivePlannedArc(at date: Date) -> PlannedArcAdvanceResult {
        applyPlannedArcCompletionForCommit(.advance, at: date)
    }

    func clearActivePlannedArcRun() { activePlannedArcRun = nil }

    func applyPlannedArcCompletionForCommit(
        _ action: PlannedArcCompletionAction,
        at date: Date
    ) -> PlannedArcAdvanceResult {
        guard var run = activePlannedArcRun else { return .notPlannedArc }
        switch action {
        case .unchanged:
            return .notPlannedArc
        case .complete:
            activePlannedArcRun = nil
            return .completed
        case .advance:
            let nextIndex = run.currentStepIndex + 1
            guard nextIndex < run.totalSteps,
                  let plan = arcPlans.first(where: { $0.id == run.arcPlanID }),
                  let next = plan.steps.first(where: { $0.orderIndex == nextIndex }) else {
                activePlannedArcRun = nil
                return .completed
            }
            run.currentStepIndex = nextIndex
            run.currentStepTitle = next.titleSnapshot
            run.currentJourneyName = next.journeyNameSnapshot
            run.currentIsSoftMode = next.isSoftModeSnapshot
            run.updatedAt = date
            activePlannedArcRun = run
            return .advanced(run)
        }
    }

    func detachArcPlanSteps(sourceFlowPlanID: UUID, at date: Date) {
        for planIndex in arcPlans.indices {
            for stepIndex in arcPlans[planIndex].steps.indices
            where arcPlans[planIndex].steps[stepIndex].sourceFlowPlanID == sourceFlowPlanID {
                let old = arcPlans[planIndex].steps[stepIndex]
                arcPlans[planIndex].steps[stepIndex] = ArcPlanStep(
                    id: old.id,
                    orderIndex: old.orderIndex,
                    sourceFlowPlanID: nil,
                    titleSnapshot: old.titleSnapshot,
                    journeyNameSnapshot: old.journeyNameSnapshot,
                    isSoftModeSnapshot: old.isSoftModeSnapshot,
                    targetMinutesSnapshot: old.targetMinutesSnapshot,
                    launchWithSurgeSnapshot: old.launchWithSurgeSnapshot,
                    linkState: .detached,
                    createdAt: old.createdAt,
                    updatedAt: date
                )
            }
        }
    }

    private func makeSteps(_ inputs: [ArcPlanStepInput], at date: Date) -> [ArcPlanStep] {
        inputs.enumerated().map { index, input in
            let normalized = input.normalizedForPersistence()
            return ArcPlanStep(
                id: UUID(),
                orderIndex: index,
                sourceFlowPlanID: normalized.sourceFlowPlanID,
                titleSnapshot: normalized.titleSnapshot,
                journeyNameSnapshot: normalized.journeyNameSnapshot,
                isSoftModeSnapshot: normalized.isSoftModeSnapshot,
                targetMinutesSnapshot: normalized.targetMinutesSnapshot,
                launchWithSurgeSnapshot: normalized.launchWithSurgeSnapshot,
                linkState: normalized.linkState,
                createdAt: date,
                updatedAt: date
            )
        }
    }

    private func makeInMemoryPlannedArcRuntime(at date: Date) -> ArcRuntimeState {
        ArcRuntimeState(
            id: UUID(),
            isPending: true,
            multiplier: ArcRuntimeState.baseMultiplier,
            progressMs: 0,
            lastSessionEndTime: date,
            sessionCount: 0,
            pauseUsedMs: 0,
            pauseStartedAt: nil
        )
    }

    static func arcPlanSort(_ lhs: ArcPlan, _ rhs: ArcPlan) -> Bool {
        if lhs.updatedAt != rhs.updatedAt { return lhs.updatedAt > rhs.updatedAt }
        return lhs.title.localizedCaseInsensitiveCompare(rhs.title) == .orderedAscending
    }
}
