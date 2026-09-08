import Foundation
import SwiftData

@MainActor
protocol LookoutRepository: AnyObject {
    func fetchLookoutSnapshot(at date: Date, calendar: Calendar) throws -> LookoutSnapshot
    @discardableResult func createLookoutObjective(
        _ request: LookoutObjectiveRequest,
        at date: Date,
        calendar: Calendar
    ) throws -> LookoutObjective
    func archiveLookoutObjective(id: UUID, at date: Date, calendar: Calendar) throws
    func skipLookoutCycle(objectiveID: UUID, at date: Date, calendar: Calendar) throws
    @discardableResult func claimLookoutCompletion(id: UUID, at date: Date) throws -> Int
    @discardableResult func claimLookoutAchievement(badgeID: String, at date: Date) throws -> Int
    @discardableResult func claimAllLookoutRewards(at date: Date) throws -> Int
    @discardableResult func reconcileLookout(at date: Date, calendar: Calendar) throws -> Int
}

extension LookoutRepository {
    func fetchLookoutSnapshot() throws -> LookoutSnapshot {
        try fetchLookoutSnapshot(at: Date(), calendar: .current)
    }

    @discardableResult func createLookoutObjective(
        _ request: LookoutObjectiveRequest
    ) throws -> LookoutObjective {
        try createLookoutObjective(request, at: Date(), calendar: .current)
    }

    func archiveLookoutObjective(id: UUID) throws {
        try archiveLookoutObjective(id: id, at: Date(), calendar: .current)
    }

    func skipLookoutCycle(objectiveID: UUID) throws {
        try skipLookoutCycle(objectiveID: objectiveID, at: Date(), calendar: .current)
    }

    @discardableResult func claimLookoutCompletion(id: UUID) throws -> Int {
        try claimLookoutCompletion(id: id, at: Date())
    }

    @discardableResult func claimLookoutAchievement(badgeID: String) throws -> Int {
        try claimLookoutAchievement(badgeID: badgeID, at: Date())
    }

    @discardableResult func claimAllLookoutRewards() throws -> Int {
        try claimAllLookoutRewards(at: Date())
    }

    @discardableResult func reconcileLookout() throws -> Int {
        try reconcileLookout(at: Date(), calendar: .current)
    }
}

extension SwiftDataFlowRepository {
    func fetchLookoutSnapshot(at date: Date, calendar: Calendar) throws -> LookoutSnapshot {
        let objectives = try context.fetch(FetchDescriptor<LookoutObjectiveModel>())
            .filter { !$0.isArchived }.map(Self.lookoutObjective)
        let completions = try context.fetch(FetchDescriptor<LookoutCompletionModel>()).map(Self.lookoutCompletion)
        let skipped = try context.fetch(FetchDescriptor<LookoutSkippedCycleModel>()).map(Self.lookoutSkippedCycle)
        let flows = try lookoutSourceFlows(through: date)
        return LookoutProjection.snapshot(
            objectives: objectives, completions: completions, skipped: skipped,
            flows: flows, at: date, calendar: calendar
        )
    }

    @discardableResult
    func createLookoutObjective(
        _ request: LookoutObjectiveRequest,
        at date: Date,
        calendar: Calendar
    ) throws -> LookoutObjective {
        do {
            let name = request.journeyName.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !name.isEmpty else { throw LookoutError.journeyRequired }
            guard request.targetDurationMs >= ObjectiveProgressCalculator.millisPerMinute else {
                throw LookoutError.targetRequired
            }
            let start = calendar.startOfDay(for: request.startAt)
            guard start >= calendar.startOfDay(for: date) else { throw LookoutError.startDateInPast }

            let journeys = try context.fetch(FetchDescriptor<JourneyModel>())
            let journey = request.journeyID.flatMap { id in journeys.first { $0.id == id } }
                ?? journeys.first { $0.name.caseInsensitiveCompare(name) == .orderedSame }
                ?? JourneyModel(name: name, createdAt: date)
            if journey.modelContext == nil { context.insert(journey) }

            let models = try context.fetch(FetchDescriptor<LookoutObjectiveModel>())
            let hasDuplicate = models.contains { model in
                guard !model.isArchived, model.journeyID == journey.id,
                      model.periodRawValue == request.period.rawValue else { return false }
                let objective = Self.lookoutObjective(model)
                return objective.kind == .recurring || ObjectiveProgressCalculator.window(
                    for: objective, at: date, calendar: calendar
                ).end > date
            }
            guard !hasDuplicate else { throw LookoutError.duplicateObjective }

            let objective = LookoutObjective(
                id: UUID(), journeyID: journey.id, journeyNameSnapshot: journey.name,
                period: request.period, kind: request.kind,
                targetDurationMs: request.targetDurationMs, startAt: start,
                weeklyBoundaryDay: request.period == .weekly ? calendar.component(.weekday, from: start) : nil,
                currentStreak: 0, maxStreak: 0, totalCompletions: 0,
                isArchived: false, createdAt: date, updatedAt: date
            )
            let model = LookoutObjectiveModel(objective: objective)
            context.insert(model)
            _ = try reconcileLookoutObjective(model, asOf: date, calendar: calendar, extraFlow: nil)
            try context.save()
            return Self.lookoutObjective(model)
        } catch {
            context.rollback()
            throw error
        }
    }

    func archiveLookoutObjective(id: UUID, at date: Date, calendar: Calendar) throws {
        do {
            guard let model = try context.fetch(FetchDescriptor<LookoutObjectiveModel>()).first(where: { $0.id == id }) else {
                throw LookoutError.objectiveMissing
            }
            guard !model.isArchived else { return }
            _ = try reconcileLookoutObjective(model, asOf: date, calendar: calendar, extraFlow: nil)
            model.isArchived = true
            model.updatedAt = max(date, model.createdAt)
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func skipLookoutCycle(objectiveID: UUID, at date: Date, calendar: Calendar) throws {
        do {
            guard let model = try context.fetch(FetchDescriptor<LookoutObjectiveModel>())
                .first(where: { $0.id == objectiveID && !$0.isArchived }) else { throw LookoutError.objectiveMissing }
            let objective = Self.lookoutObjective(model)
            let window = ObjectiveProgressCalculator.window(for: objective, at: date, calendar: calendar)
            let id = Self.lookoutCycleKey(objectiveID: objectiveID, window: window)
            if try !context.fetch(FetchDescriptor<LookoutSkippedCycleModel>()).contains(where: { $0.id == id }) {
                context.insert(LookoutSkippedCycleModel(cycle: .init(
                    id: id, objectiveID: objectiveID, periodStart: window.start,
                    periodEnd: window.end, skippedAt: date
                )))
            }
            try rebuildLookoutStats(model, asOf: date, calendar: calendar)
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    @discardableResult
    func claimLookoutCompletion(id: UUID, at date: Date) throws -> Int {
        try claimLookoutModels(
            try context.fetch(FetchDescriptor<LookoutCompletionModel>()).filter { $0.id == id && !$0.pearlsClaimed },
            sourceType: "objective_completion", sourceID: id.uuidString, at: date
        )
    }

    @discardableResult
    func claimLookoutAchievement(badgeID: String, at date: Date) throws -> Int {
        try claimLookoutModels(
            try context.fetch(FetchDescriptor<LookoutCompletionModel>()).filter { $0.badgeKey == badgeID && !$0.pearlsClaimed },
            sourceType: "objective_achievement", sourceID: badgeID, at: date
        )
    }

    @discardableResult
    func claimAllLookoutRewards(at date: Date) throws -> Int {
        try claimLookoutModels(
            try context.fetch(FetchDescriptor<LookoutCompletionModel>()).filter { !$0.pearlsClaimed },
            sourceType: "objective_claim_all", sourceID: nil, at: date
        )
    }

    @discardableResult
    func reconcileLookout(at date: Date, calendar: Calendar) throws -> Int {
        do {
            let processed = Set(try context.fetch(FetchDescriptor<LookoutProcessedSessionModel>()).map(\.sessionID))
            let sessions = try context.fetch(FetchDescriptor<FlowSessionModel>())
                .filter { !$0.isSoftMode && !processed.contains($0.id) && $0.endTime <= date }
                .sorted {
                    if $0.endTime != $1.endTime { return $0.endTime < $1.endTime }
                    return $0.id.uuidString < $1.id.uuidString
                }
            var count = 0
            for session in sessions {
                count += try processLookoutSessionForCommit(Self.lookoutFlowSession(session), calendar: calendar)
            }
            try context.save()
            return count
        } catch {
            context.rollback()
            throw error
        }
    }

    /// Called by the Flow repository before its single commit save. This method
    /// intentionally does not save or rollback so session, Arc, Shell rewards,
    /// objective evidence, and badges share one transaction boundary.
    @discardableResult
    func processLookoutSessionForCommit(_ session: FlowSession, calendar: Calendar = .current) throws -> Int {
        guard !session.isSoftMode else { return 0 }
        if try context.fetch(FetchDescriptor<LookoutProcessedSessionModel>()).contains(where: { $0.sessionID == session.id }) {
            return 0
        }
        let models = try context.fetch(FetchDescriptor<LookoutObjectiveModel>()).filter {
            !$0.isArchived && $0.journeyNameSnapshot.caseInsensitiveCompare(session.journeyName) == .orderedSame
        }
        var inserted = 0
        for model in models {
            inserted += try reconcileLookoutObjective(model, asOf: session.endTime, calendar: calendar, extraFlow: session) ? 1 : 0
        }
        context.insert(LookoutProcessedSessionModel(sessionID: session.id, processedAt: session.createdAt))
        return inserted
    }

    private func reconcileLookoutObjective(
        _ model: LookoutObjectiveModel,
        asOf date: Date,
        calendar: Calendar,
        extraFlow: FlowSession?
    ) throws -> Bool {
        let objective = Self.lookoutObjective(model)
        let window = ObjectiveProgressCalculator.window(for: objective, at: date, calendar: calendar)
        guard window.contains(date) || (objective.kind == .oneTime && date >= window.start) else { return false }
        let cycleKey = LookoutCompletionModel.makeCycleKey(objectiveID: objective.id, start: window.start, end: window.end)
        guard try !context.fetch(FetchDescriptor<LookoutCompletionModel>()).contains(where: { $0.cycleKey == cycleKey }) else {
            return false
        }
        guard try !context.fetch(FetchDescriptor<LookoutSkippedCycleModel>()).contains(where: {
            $0.objectiveID == objective.id && $0.periodStart == window.start && $0.periodEnd == window.end
        }) else { return false }

        var flows = try lookoutSourceFlows(through: date)
        if let extraFlow, !flows.contains(where: { $0.id == extraFlow.id }) {
            flows.append(Self.lookoutSourceFlow(extraFlow, journeyID: objective.journeyID))
        }
        guard let evidence = ObjectiveProgressCalculator.completionEvidence(
            for: objective, flows: flows, window: window
        ) else { return false }
        let existing = try context.fetch(FetchDescriptor<LookoutCompletionModel>()).map(Self.lookoutCompletion)
        let skipped = try context.fetch(FetchDescriptor<LookoutSkippedCycleModel>()).map(Self.lookoutSkippedCycle)
        let streak = RecurringObjectiveStatsCalculator.streakBefore(
            objective: objective, completions: existing, skipped: skipped, periodStart: window.start
        )
        let completion = ObjectiveProgressCalculator.makeCompletion(
            objective: objective, window: window, evidence: evidence, streakBefore: streak
        )
        context.insert(LookoutCompletionModel(completion: completion))
        try incrementLookoutBadge(completion, at: evidence.completedAt)
        try rebuildLookoutStats(model, completions: existing + [completion], skipped: skipped, asOf: date, calendar: calendar)
        return true
    }

    private func rebuildLookoutStats(
        _ model: LookoutObjectiveModel,
        completions: [LookoutCompletion]? = nil,
        skipped: [ObjectiveSkippedCycle]? = nil,
        asOf date: Date,
        calendar: Calendar
    ) throws {
        let objective = Self.lookoutObjective(model)
        let stats = RecurringObjectiveStatsCalculator.derive(
            objective: objective,
            completions: try completions ?? context.fetch(FetchDescriptor<LookoutCompletionModel>()).map(Self.lookoutCompletion),
            skipped: try skipped ?? context.fetch(FetchDescriptor<LookoutSkippedCycleModel>()).map(Self.lookoutSkippedCycle),
            asOf: date,
            calendar: calendar
        )
        model.currentStreak = stats.currentStreak
        model.maxStreak = stats.maxStreak
        model.totalCompletions = stats.totalCompletions
        model.updatedAt = max(date, model.createdAt)
    }

    private func incrementLookoutBadge(_ completion: LookoutCompletion, at date: Date) throws {
        let badges = try context.fetch(FetchDescriptor<ShellBadgeModel>())
        if let badge = badges.first(where: { $0.badgeID == completion.badgeKey }) {
            badge.count += 1
            badge.lastEarnedAt = max(badge.lastEarnedAt, date)
            badge.isNew = true
        } else {
            context.insert(ShellBadgeModel(badgeID: completion.badgeKey, count: 1, earnedAt: date, isNew: true))
        }
    }

    private func claimLookoutModels(
        _ models: [LookoutCompletionModel],
        sourceType: String,
        sourceID: String?,
        at date: Date
    ) throws -> Int {
        guard !models.isEmpty else { return 0 }
        do {
            let total = models.reduce(0) { $0 + $1.finalRewardPearls }
            for model in models {
                model.pearlsClaimed = true
                model.pearlsClaimedAt = date
            }
            if total > 0 {
                let identity = models.map { $0.id.uuidString }.sorted().joined(separator: ",")
                context.insert(PearlLedgerModel(entry: .init(
                    id: "lookout-claim:\(identity)", delta: total,
                    reason: "objective_completion_claim", sourceType: sourceType,
                    sourceID: sourceID, createdAt: date,
                    note: "\(models.count) Objective reward(s)"
                )))
            }
            try context.save()
            return total
        } catch {
            context.rollback()
            throw error
        }
    }

    private func lookoutSourceFlows(through date: Date) throws -> [ObjectiveSourceFlow] {
        try context.fetch(FetchDescriptor<FlowSessionModel>()).filter { $0.endTime <= date }.map { model in
            .init(
                id: model.id, journeyID: model.journey?.id,
                journeyName: model.journeyNameSnapshot, startTime: model.startTime,
                endTime: model.endTime, durationMs: model.durationMs, isSoftMode: model.isSoftMode
            )
        }
    }

    private static func lookoutSourceFlow(_ session: FlowSession, journeyID: UUID?) -> ObjectiveSourceFlow {
        .init(id: session.id, journeyID: journeyID, journeyName: session.journeyName,
              startTime: session.startTime, endTime: session.endTime,
              durationMs: session.durationMs, isSoftMode: session.isSoftMode)
    }

    private static func lookoutFlowSession(_ model: FlowSessionModel) -> FlowSession {
        .init(
            id: model.id, flowInstanceID: model.flowInstanceID, title: model.title,
            description: model.flowDescription, journeyName: model.journeyNameSnapshot,
            startTime: model.startTime, endTime: model.endTime, durationMs: model.durationMs,
            surgePlannedMs: model.surgePlannedMs, surgePoints: model.surgePoints,
            scyraPoints: model.scyraPoints, isSoftMode: model.isSoftMode,
            arcID: model.arcID, arcIndex: model.arcIndex,
            arcMultiplierUsed: model.arcMultiplierUsed, arcBonusPoints: model.arcBonusPoints,
            createdAt: model.createdAt
        )
    }

    private static func lookoutObjective(_ model: LookoutObjectiveModel) -> LookoutObjective {
        .init(
            id: model.id, journeyID: model.journeyID, journeyNameSnapshot: model.journeyNameSnapshot,
            period: ObjectivePeriod(rawValue: model.periodRawValue) ?? .daily,
            kind: ObjectiveKind(rawValue: model.kindRawValue) ?? .oneTime,
            targetDurationMs: model.targetDurationMs, startAt: model.startAt,
            weeklyBoundaryDay: model.weeklyBoundaryDay, currentStreak: model.currentStreak,
            maxStreak: model.maxStreak, totalCompletions: model.totalCompletions,
            isArchived: model.isArchived, createdAt: model.createdAt, updatedAt: model.updatedAt
        )
    }

    private static func lookoutCompletion(_ model: LookoutCompletionModel) -> LookoutCompletion {
        .init(
            id: model.id, objectiveID: model.objectiveID, journeyID: model.journeyID,
            journeyNameSnapshot: model.journeyNameSnapshot,
            period: ObjectivePeriod(rawValue: model.periodRawValue) ?? .daily,
            kind: ObjectiveKind(rawValue: model.kindRawValue) ?? .oneTime,
            periodStart: model.periodStart, periodEnd: model.periodEnd,
            completedAt: model.completedAt, achievedDurationMs: model.achievedDurationMs,
            targetDurationMs: model.targetDurationMs, baseRewardPearls: model.baseRewardPearls,
            streakBeforeCompletion: model.streakBeforeCompletion,
            streakMultiplier: model.streakMultiplier, finalRewardPearls: model.finalRewardPearls,
            badgeKey: model.badgeKey, badgeLabelSnapshot: model.badgeLabelSnapshot,
            pearlsClaimed: model.pearlsClaimed, pearlsClaimedAt: model.pearlsClaimedAt
        )
    }

    private static func lookoutSkippedCycle(_ model: LookoutSkippedCycleModel) -> ObjectiveSkippedCycle {
        .init(id: model.id, objectiveID: model.objectiveID, periodStart: model.periodStart,
              periodEnd: model.periodEnd, skippedAt: model.skippedAt)
    }

    private static func lookoutCycleKey(objectiveID: UUID, window: ObjectiveWindow) -> String {
        LookoutCompletionModel.makeCycleKey(objectiveID: objectiveID, start: window.start, end: window.end)
    }
}

enum LookoutProjection {
    static func snapshot(
        objectives: [LookoutObjective],
        completions: [LookoutCompletion],
        skipped: [ObjectiveSkippedCycle],
        flows: [ObjectiveSourceFlow],
        at date: Date,
        calendar: Calendar
    ) -> LookoutSnapshot {
        let cards = objectives.compactMap { objective -> ObjectiveCardModel? in
            let window = ObjectiveProgressCalculator.window(for: objective, at: date, calendar: calendar)
            if skipped.contains(where: {
                $0.objectiveID == objective.id && $0.periodStart == window.start && $0.periodEnd == window.end
            }) { return nil }
            let completion = completions.first {
                $0.objectiveID == objective.id && $0.periodStart == window.start && $0.periodEnd == window.end
            }
            let progress = completion?.achievedDurationMs ?? ObjectiveProgressCalculator.progress(
                for: objective, flows: flows, window: window
            )
            let state: ObjectiveCardState
            if completion != nil, date < window.end { state = .completed }
            else if date < window.start { state = .upcoming }
            else if date < window.end { state = .inProgress }
            else { return nil }
            let stats = RecurringObjectiveStatsCalculator.derive(
                objective: objective, completions: completions, skipped: skipped,
                asOf: date, calendar: calendar
            )
            return .init(
                objective: objective, window: window, state: state,
                progressDurationMs: progress,
                progressPercent: completion == nil
                    ? ObjectiveProgressCalculator.percent(progress: progress, target: objective.targetDurationMs) : 100,
                completion: completion,
                effectiveCurrentStreak: objective.kind == .recurring ? stats.currentStreak : 0
            )
        }
        let orderedCards = cards.sorted {
            if $0.objective.period != $1.objective.period {
                return ObjectivePeriod.allCases.firstIndex(of: $0.objective.period)! < ObjectivePeriod.allCases.firstIndex(of: $1.objective.period)!
            }
            if $0.state != $1.state {
                let order: [ObjectiveCardState] = [.inProgress, .completed, .upcoming]
                return order.firstIndex(of: $0.state)! < order.firstIndex(of: $1.state)!
            }
            if $0.progressPercent != $1.progressPercent { return $0.progressPercent > $1.progressPercent }
            return $0.objective.createdAt < $1.objective.createdAt
        }
        let unclaimed = completions.filter { !$0.pearlsClaimed }
        return .init(
            cards: orderedCards,
            completions: completions.sorted { $0.completedAt > $1.completedAt },
            unclaimedPearls: unclaimed.reduce(0) { $0 + $1.finalRewardPearls },
            unclaimedCompletionCount: unclaimed.count
        )
    }
}

extension InMemoryFlowRepository {
    func fetchLookoutSnapshot(at date: Date, calendar: Calendar) -> LookoutSnapshot {
        LookoutProjection.snapshot(
            objectives: lookoutObjectives.filter { !$0.isArchived },
            completions: lookoutCompletions,
            skipped: lookoutSkippedCycles,
            flows: lookoutSourceFlowsInMemory(through: date),
            at: date,
            calendar: calendar
        )
    }

    @discardableResult
    func createLookoutObjective(
        _ request: LookoutObjectiveRequest,
        at date: Date,
        calendar: Calendar
    ) throws -> LookoutObjective {
        let name = request.journeyName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { throw LookoutError.journeyRequired }
        guard request.targetDurationMs >= ObjectiveProgressCalculator.millisPerMinute else {
            throw LookoutError.targetRequired
        }
        let start = calendar.startOfDay(for: request.startAt)
        guard start >= calendar.startOfDay(for: date) else { throw LookoutError.startDateInPast }
        let duplicate = lookoutObjectives.contains { objective in
            guard !objective.isArchived,
                  objective.journeyNameSnapshot.caseInsensitiveCompare(name) == .orderedSame,
                  objective.period == request.period else { return false }
            return objective.kind == .recurring || ObjectiveProgressCalculator.window(
                for: objective, at: date, calendar: calendar
            ).end > date
        }
        guard !duplicate else { throw LookoutError.duplicateObjective }
        let objective = LookoutObjective(
            id: UUID(), journeyID: request.journeyID ?? UUID(), journeyNameSnapshot: name,
            period: request.period, kind: request.kind,
            targetDurationMs: request.targetDurationMs, startAt: start,
            weeklyBoundaryDay: request.period == .weekly ? calendar.component(.weekday, from: start) : nil,
            currentStreak: 0, maxStreak: 0, totalCompletions: 0,
            isArchived: false, createdAt: date, updatedAt: date
        )
        lookoutObjectives.append(objective)
        _ = reconcileLookoutObjectiveInMemory(id: objective.id, asOf: date, calendar: calendar)
        return lookoutObjectives.first { $0.id == objective.id } ?? objective
    }

    func archiveLookoutObjective(id: UUID, at date: Date, calendar: Calendar) throws {
        guard let index = lookoutObjectives.firstIndex(where: { $0.id == id }) else {
            throw LookoutError.objectiveMissing
        }
        guard !lookoutObjectives[index].isArchived else { return }
        _ = reconcileLookoutObjectiveInMemory(id: id, asOf: date, calendar: calendar)
        guard let refreshed = lookoutObjectives.firstIndex(where: { $0.id == id }) else { return }
        lookoutObjectives[refreshed].isArchived = true
        lookoutObjectives[refreshed].updatedAt = max(date, lookoutObjectives[refreshed].createdAt)
    }

    func skipLookoutCycle(objectiveID: UUID, at date: Date, calendar: Calendar) throws {
        guard let index = lookoutObjectives.firstIndex(where: { $0.id == objectiveID && !$0.isArchived }) else {
            throw LookoutError.objectiveMissing
        }
        let window = ObjectiveProgressCalculator.window(for: lookoutObjectives[index], at: date, calendar: calendar)
        let id = "\(objectiveID.uuidString):\(window.key)"
        if !lookoutSkippedCycles.contains(where: { $0.id == id }) {
            lookoutSkippedCycles.append(.init(
                id: id, objectiveID: objectiveID, periodStart: window.start,
                periodEnd: window.end, skippedAt: date
            ))
        }
        rebuildLookoutStatsInMemory(objectiveID: objectiveID, asOf: date, calendar: calendar)
    }

    @discardableResult
    func claimLookoutCompletion(id: UUID, at date: Date) -> Int {
        claimLookoutInMemory(
            matching: { $0.id == id }, sourceType: "objective_completion",
            sourceID: id.uuidString, at: date
        )
    }

    @discardableResult
    func claimLookoutAchievement(badgeID: String, at date: Date) -> Int {
        claimLookoutInMemory(
            matching: { $0.badgeKey == badgeID }, sourceType: "objective_achievement",
            sourceID: badgeID, at: date
        )
    }

    @discardableResult
    func claimAllLookoutRewards(at date: Date) -> Int {
        claimLookoutInMemory(matching: { _ in true }, sourceType: "objective_claim_all", sourceID: nil, at: date)
    }

    @discardableResult
    func reconcileLookout(at date: Date, calendar: Calendar) -> Int {
        let pending = sessions.filter {
            !$0.isSoftMode && !lookoutProcessedSessionIDs.contains($0.id) && $0.endTime <= date
        }.sorted {
            if $0.endTime != $1.endTime { return $0.endTime < $1.endTime }
            return $0.id.uuidString < $1.id.uuidString
        }
        return pending.reduce(0) { $0 + processLookoutSessionForCommitInMemory($1, calendar: calendar) }
    }

    @discardableResult
    func processLookoutSessionForCommitInMemory(_ session: FlowSession, calendar: Calendar = .current) -> Int {
        guard !session.isSoftMode, !lookoutProcessedSessionIDs.contains(session.id) else { return 0 }
        let matchingIDs = lookoutObjectives.filter {
            !$0.isArchived && $0.journeyNameSnapshot.caseInsensitiveCompare(session.journeyName) == .orderedSame
        }.map(\.id)
        let inserted = matchingIDs.reduce(0) {
            $0 + (reconcileLookoutObjectiveInMemory(id: $1, asOf: session.endTime, calendar: calendar) ? 1 : 0)
        }
        lookoutProcessedSessionIDs.insert(session.id)
        return inserted
    }

    private func reconcileLookoutObjectiveInMemory(id: UUID, asOf date: Date, calendar: Calendar) -> Bool {
        guard let index = lookoutObjectives.firstIndex(where: { $0.id == id && !$0.isArchived }) else { return false }
        let objective = lookoutObjectives[index]
        let window = ObjectiveProgressCalculator.window(for: objective, at: date, calendar: calendar)
        guard window.contains(date) || (objective.kind == .oneTime && date >= window.start) else { return false }
        guard !lookoutCompletions.contains(where: {
            $0.objectiveID == id && $0.periodStart == window.start && $0.periodEnd == window.end
        }), !lookoutSkippedCycles.contains(where: {
            $0.objectiveID == id && $0.periodStart == window.start && $0.periodEnd == window.end
        }) else { return false }
        guard let evidence = ObjectiveProgressCalculator.completionEvidence(
            for: objective,
            flows: lookoutSourceFlowsInMemory(through: date),
            window: window
        ) else { return false }
        let streak = RecurringObjectiveStatsCalculator.streakBefore(
            objective: objective, completions: lookoutCompletions,
            skipped: lookoutSkippedCycles, periodStart: window.start
        )
        let completion = ObjectiveProgressCalculator.makeCompletion(
            objective: objective, window: window, evidence: evidence, streakBefore: streak
        )
        lookoutCompletions.append(completion)
        if let badge = shellBadges[completion.badgeKey] {
            shellBadges[completion.badgeKey] = .init(
                badgeID: completion.badgeKey, count: badge.count + 1,
                firstEarnedAt: badge.firstEarnedAt,
                lastEarnedAt: max(badge.lastEarnedAt, completion.completedAt),
                isNew: true
            )
        } else {
            shellBadges[completion.badgeKey] = .init(
                badgeID: completion.badgeKey, count: 1,
                firstEarnedAt: completion.completedAt, lastEarnedAt: completion.completedAt,
                isNew: true
            )
        }
        rebuildLookoutStatsInMemory(objectiveID: id, asOf: date, calendar: calendar)
        return true
    }

    private func rebuildLookoutStatsInMemory(objectiveID: UUID, asOf date: Date, calendar: Calendar) {
        guard let index = lookoutObjectives.firstIndex(where: { $0.id == objectiveID }) else { return }
        let stats = RecurringObjectiveStatsCalculator.derive(
            objective: lookoutObjectives[index], completions: lookoutCompletions,
            skipped: lookoutSkippedCycles, asOf: date, calendar: calendar
        )
        lookoutObjectives[index].currentStreak = stats.currentStreak
        lookoutObjectives[index].maxStreak = stats.maxStreak
        lookoutObjectives[index].totalCompletions = stats.totalCompletions
        lookoutObjectives[index].updatedAt = max(date, lookoutObjectives[index].createdAt)
    }

    private func claimLookoutInMemory(
        matching: (LookoutCompletion) -> Bool,
        sourceType: String,
        sourceID: String?,
        at date: Date
    ) -> Int {
        let indices = lookoutCompletions.indices.filter {
            !lookoutCompletions[$0].pearlsClaimed && matching(lookoutCompletions[$0])
        }
        guard !indices.isEmpty else { return 0 }
        let total = indices.reduce(0) { $0 + lookoutCompletions[$1].finalRewardPearls }
        for index in indices {
            lookoutCompletions[index].pearlsClaimed = true
            lookoutCompletions[index].pearlsClaimedAt = date
        }
        if total > 0 {
            let identity = indices.map { lookoutCompletions[$0].id.uuidString }.sorted().joined(separator: ",")
            pearlLedger.append(.init(
                id: "lookout-claim:\(identity)", delta: total,
                reason: "objective_completion_claim", sourceType: sourceType,
                sourceID: sourceID, createdAt: date, note: "\(indices.count) Objective reward(s)"
            ))
        }
        return total
    }

    private func lookoutSourceFlowsInMemory(through date: Date) -> [ObjectiveSourceFlow] {
        sessions.filter { $0.endTime <= date }.map {
            .init(
                id: $0.id, journeyID: nil, journeyName: $0.journeyName,
                startTime: $0.startTime, endTime: $0.endTime,
                durationMs: $0.durationMs, isSoftMode: $0.isSoftMode
            )
        }
    }
}
