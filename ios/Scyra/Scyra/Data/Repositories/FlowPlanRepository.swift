import Foundation
import SwiftData

@MainActor
protocol FlowPlanRepository: AnyObject {
    func fetchActiveFlowPlans() throws -> [FlowPlan]
    func fetchArchivedFlowPlans() throws -> [FlowPlan]
    func fetchFlowPlan(id: UUID) throws -> FlowPlan?
    func createFlowPlan(input: FlowPlanInput, at date: Date) throws -> FlowPlan
    func updateFlowPlan(id: UUID, input: FlowPlanInput, at date: Date) throws
    func setFlowPlanPinned(id: UUID, pinned: Bool, at date: Date) throws
    func setFlowPlanArchived(id: UUID, archived: Bool, at date: Date) throws
    func markFlowPlanLaunched(id: UUID, at date: Date) throws
    func deleteFlowPlan(id: UUID) throws
}

enum FlowPlanRepositoryError: LocalizedError {
    case notFound

    var errorDescription: String? { "Planned flow not found." }
}

extension SwiftDataFlowRepository {
    func fetchActiveFlowPlans() throws -> [FlowPlan] {
        try context.fetch(FetchDescriptor<FlowPlanModel>())
            .filter { !$0.archived }
            .map(Self.flowPlan(from:))
            .sorted(by: Self.activeFlowPlanSort)
    }

    func fetchArchivedFlowPlans() throws -> [FlowPlan] {
        try context.fetch(FetchDescriptor<FlowPlanModel>())
            .filter(\.archived)
            .map(Self.flowPlan(from:))
            .sorted(by: Self.archivedFlowPlanSort)
    }

    func fetchFlowPlan(id: UUID) throws -> FlowPlan? {
        try flowPlanModel(id: id).map(Self.flowPlan(from:))
    }

    func createFlowPlan(input: FlowPlanInput, at date: Date) throws -> FlowPlan {
        let input = input.normalizedForPersistence()
        let plan = FlowPlan(
            id: UUID(),
            title: input.title,
            journeyName: input.journeyName,
            isSoftMode: input.isSoftMode,
            targetMinutes: input.targetMinutes,
            launchWithSurge: input.launchWithSurge,
            pinned: false,
            archived: false,
            launchCount: 0,
            lastLaunchedAt: nil,
            createdAt: date,
            updatedAt: date
        )
        do {
            context.insert(FlowPlanModel(plan: plan))
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
        return plan
    }

    func updateFlowPlan(id: UUID, input: FlowPlanInput, at date: Date) throws {
        guard let model = try flowPlanModel(id: id) else { throw FlowPlanRepositoryError.notFound }
        try saveOrRollback {
            model.update(input: input.normalizedForPersistence(), at: date)
        }
    }

    func setFlowPlanPinned(id: UUID, pinned: Bool, at date: Date) throws {
        guard let model = try flowPlanModel(id: id) else { throw FlowPlanRepositoryError.notFound }
        try saveOrRollback {
            model.pinned = pinned
            model.updatedAt = date
        }
    }

    func setFlowPlanArchived(id: UUID, archived: Bool, at date: Date) throws {
        guard let model = try flowPlanModel(id: id) else { throw FlowPlanRepositoryError.notFound }
        try saveOrRollback {
            model.archived = archived
            model.updatedAt = date
        }
    }

    func markFlowPlanLaunched(id: UUID, at date: Date) throws {
        guard let model = try flowPlanModel(id: id) else { throw FlowPlanRepositoryError.notFound }
        try saveOrRollback {
            model.launchCount += 1
            model.lastLaunchedAt = date
            model.updatedAt = date
        }
    }

    func deleteFlowPlan(id: UUID) throws {
        guard let model = try flowPlanModel(id: id) else { return }
        try saveOrRollback {
            try detachArcPlanSteps(sourceFlowPlanID: id, at: Date())
            context.delete(model)
        }
    }

    private func flowPlanModel(id: UUID) throws -> FlowPlanModel? {
        let descriptor = FetchDescriptor<FlowPlanModel>(predicate: #Predicate { $0.id == id })
        return try context.fetch(descriptor).first
    }

    private func saveOrRollback(_ mutation: () throws -> Void) throws {
        do {
            try mutation()
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    private static func flowPlan(from model: FlowPlanModel) -> FlowPlan {
        FlowPlan(
            id: model.id,
            title: model.title,
            journeyName: model.journeyName,
            isSoftMode: model.isSoftMode,
            targetMinutes: model.targetMinutes,
            launchWithSurge: model.launchWithSurge,
            pinned: model.pinned,
            archived: model.archived,
            launchCount: model.launchCount,
            lastLaunchedAt: model.lastLaunchedAt,
            createdAt: model.createdAt,
            updatedAt: model.updatedAt
        )
    }
}

extension InMemoryFlowRepository {
    func fetchActiveFlowPlans() -> [FlowPlan] {
        flowPlans.filter { !$0.archived }.sorted(by: Self.activeFlowPlanSort)
    }

    func fetchArchivedFlowPlans() -> [FlowPlan] {
        flowPlans.filter(\.archived).sorted(by: Self.archivedFlowPlanSort)
    }

    func fetchFlowPlan(id: UUID) -> FlowPlan? {
        flowPlans.first { $0.id == id }
    }

    func createFlowPlan(input: FlowPlanInput, at date: Date) -> FlowPlan {
        let input = input.normalizedForPersistence()
        let plan = FlowPlan(
            id: UUID(),
            title: input.title,
            journeyName: input.journeyName,
            isSoftMode: input.isSoftMode,
            targetMinutes: input.targetMinutes,
            launchWithSurge: input.launchWithSurge,
            pinned: false,
            archived: false,
            launchCount: 0,
            lastLaunchedAt: nil,
            createdAt: date,
            updatedAt: date
        )
        flowPlans.append(plan)
        return plan
    }

    func updateFlowPlan(id: UUID, input: FlowPlanInput, at date: Date) throws {
        guard let index = flowPlans.firstIndex(where: { $0.id == id }) else {
            throw FlowPlanRepositoryError.notFound
        }
        let input = input.normalizedForPersistence()
        flowPlans[index].title = input.title
        flowPlans[index].journeyName = input.journeyName
        flowPlans[index].isSoftMode = input.isSoftMode
        flowPlans[index].targetMinutes = input.targetMinutes
        flowPlans[index].launchWithSurge = input.launchWithSurge
        flowPlans[index].updatedAt = date
    }

    func setFlowPlanPinned(id: UUID, pinned: Bool, at date: Date) throws {
        guard let index = flowPlans.firstIndex(where: { $0.id == id }) else {
            throw FlowPlanRepositoryError.notFound
        }
        flowPlans[index].pinned = pinned
        flowPlans[index].updatedAt = date
    }

    func setFlowPlanArchived(id: UUID, archived: Bool, at date: Date) throws {
        guard let index = flowPlans.firstIndex(where: { $0.id == id }) else {
            throw FlowPlanRepositoryError.notFound
        }
        flowPlans[index].archived = archived
        flowPlans[index].updatedAt = date
    }

    func markFlowPlanLaunched(id: UUID, at date: Date) throws {
        guard let index = flowPlans.firstIndex(where: { $0.id == id }) else {
            throw FlowPlanRepositoryError.notFound
        }
        flowPlans[index].launchCount += 1
        flowPlans[index].lastLaunchedAt = date
        flowPlans[index].updatedAt = date
    }

    func deleteFlowPlan(id: UUID) {
        detachArcPlanSteps(sourceFlowPlanID: id, at: Date())
        flowPlans.removeAll { $0.id == id }
    }
}

private extension FlowPlanRepository {
    static func activeFlowPlanSort(_ lhs: FlowPlan, _ rhs: FlowPlan) -> Bool {
        if lhs.pinned != rhs.pinned { return lhs.pinned && !rhs.pinned }
        if lhs.updatedAt != rhs.updatedAt { return lhs.updatedAt > rhs.updatedAt }
        return lhs.title.localizedCaseInsensitiveCompare(rhs.title) == .orderedAscending
    }

    static func archivedFlowPlanSort(_ lhs: FlowPlan, _ rhs: FlowPlan) -> Bool {
        if lhs.updatedAt != rhs.updatedAt { return lhs.updatedAt > rhs.updatedAt }
        return lhs.title.localizedCaseInsensitiveCompare(rhs.title) == .orderedAscending
    }
}
