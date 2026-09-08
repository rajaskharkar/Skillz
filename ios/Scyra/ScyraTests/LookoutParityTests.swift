import Foundation
import SwiftData
import Testing
@testable import Scyra

@MainActor
struct LookoutDomainParityTests {
    private var newYork: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "America/New_York")!
        return calendar
    }

    @Test func windowsUseLocalMidnightAndCanonicalOneSevenThirtyDayLengths() throws {
        let calendar = newYork
        let start = try #require(calendar.date(from: .init(
            timeZone: calendar.timeZone, year: 2026, month: 3, day: 8, hour: 14
        )))
        let daily = ObjectiveProgressCalculator.initialWindow(startAt: start, period: .daily, calendar: calendar)
        #expect(calendar.component(.hour, from: daily.start) == 0)
        #expect(calendar.dateComponents([.day], from: daily.start, to: daily.end).day == 1)
        #expect(daily.end.timeIntervalSince(daily.start) == 23 * 60 * 60, "DST spring-forward remains midnight-to-midnight")
        #expect(calendar.dateComponents(
            [.day], from: daily.start,
            to: ObjectiveProgressCalculator.initialWindow(startAt: start, period: .weekly, calendar: calendar).end
        ).day == 7)
        #expect(calendar.dateComponents(
            [.day], from: daily.start,
            to: ObjectiveProgressCalculator.initialWindow(startAt: start, period: .monthly, calendar: calendar).end
        ).day == 30)
    }

    @Test func onlyRegularJourneyFlowsCountAndFirstCrossingPreservesOvershoot() throws {
        let calendar = newYork
        let start = try #require(calendar.date(from: .init(
            timeZone: calendar.timeZone, year: 2026, month: 6, day: 1
        )))
        let objective = makeObjective(start: start, targetMinutes: 30)
        let window = ObjectiveProgressCalculator.window(for: objective, at: start, calendar: calendar)
        let flows = [
            makeSourceFlow(minutes: 20, end: start.addingTimeInterval(3_600), soft: false),
            makeSourceFlow(minutes: 200, end: start.addingTimeInterval(3_700), soft: true),
            makeSourceFlow(minutes: 200, end: start.addingTimeInterval(3_800), journey: "Other", soft: false),
            makeSourceFlow(minutes: 15, end: start.addingTimeInterval(3_900), soft: false)
        ]
        let evidence = try #require(ObjectiveProgressCalculator.completionEvidence(
            for: objective, flows: flows, window: window
        ))
        #expect(evidence.achievedDurationMs == 35 * 60_000)
        #expect(evidence.completedAt == flows.last?.endTime)
        let completion = ObjectiveProgressCalculator.makeCompletion(
            objective: objective, window: window, evidence: evidence, streakBefore: 0
        )
        #expect(completion.baseRewardPearls == 35)
        #expect(completion.finalRewardPearls == 35)
    }

    @Test func recurringMultiplierIsUncappedAndSkippedBoundaryBreaksTheStreak() throws {
        let calendar = newYork
        let start = try #require(calendar.date(from: .init(
            timeZone: calendar.timeZone, year: 2026, month: 6, day: 1
        )))
        let objective = makeObjective(start: start, kind: .recurring, targetMinutes: 30)
        let window = ObjectiveProgressCalculator.initialWindow(startAt: start, period: .daily, calendar: calendar)
        let completion = ObjectiveProgressCalculator.makeCompletion(
            objective: objective,
            window: window,
            evidence: .init(achievedDurationMs: 30 * 60_000, completedAt: start.addingTimeInterval(3_600)),
            streakBefore: 20
        )
        #expect(completion.streakMultiplier == 3)
        #expect(completion.finalRewardPearls == 90)
        let next = ObjectiveWindow(
            start: window.end,
            end: try #require(calendar.date(byAdding: .day, value: 1, to: window.end))
        )
        let skip = ObjectiveSkippedCycle(
            id: "skip", objectiveID: objective.id, periodStart: window.start,
            periodEnd: window.end, skippedAt: window.start
        )
        #expect(RecurringObjectiveStatsCalculator.streakBefore(
            objective: objective, completions: [completion], skipped: [skip], periodStart: next.start
        ) == 0)
    }

    private func makeObjective(
        start: Date,
        kind: ObjectiveKind = .oneTime,
        targetMinutes: Int64
    ) -> LookoutObjective {
        .init(
            id: UUID(), journeyID: UUID(), journeyNameSnapshot: "Drums",
            period: .daily, kind: kind, targetDurationMs: targetMinutes * 60_000,
            startAt: start, weeklyBoundaryDay: nil, currentStreak: 0, maxStreak: 0,
            totalCompletions: 0, isArchived: false, createdAt: start, updatedAt: start
        )
    }

    private func makeSourceFlow(
        minutes: Int64,
        end: Date,
        journey: String = "Drums",
        soft: Bool
    ) -> ObjectiveSourceFlow {
        .init(
            id: UUID(), journeyID: nil, journeyName: journey,
            startTime: end.addingTimeInterval(-Double(minutes * 60)), endTime: end,
            durationMs: minutes * 60_000, isSoftMode: soft
        )
    }
}

@MainActor
struct LookoutTransactionParityTests {
    private var localCalendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        return calendar
    }

    @Test func flowCommitAtomicallyMaterializesOneCompletionBadgeAndClaimableReward() throws {
        let repository = SwiftDataFlowRepository(container: try ScyraPersistenceFactory.makeContainer(inMemory: true))
        let start = Date(timeIntervalSince1970: 1_800_000_000)
        _ = try repository.createLookoutObjective(
            .init(
                journeyID: nil, journeyName: "Drums", period: .daily, kind: .oneTime,
                targetDurationMs: 30 * 60_000, startAt: start
            ), at: start, calendar: localCalendar
        )
        let first = makeSession(id: UUID(), journey: "Drums", minutes: 20, end: start.addingTimeInterval(3_600))
        let crossing = makeSession(id: UUID(), journey: "Drums", minutes: 15, end: start.addingTimeInterval(7_200))
        _ = try repository.commit(session: first, activeArc: nil, recentlyEndedArc: nil)
        #expect(try repository.fetchLookoutSnapshot(at: first.endTime, calendar: localCalendar).completions.isEmpty)
        _ = try repository.commit(session: crossing, activeArc: nil, recentlyEndedArc: nil)

        let snapshot = try repository.fetchLookoutSnapshot(at: crossing.endTime, calendar: localCalendar)
        let completion = try #require(snapshot.completions.single)
        #expect(completion.achievedDurationMs == 35 * 60_000)
        #expect(completion.completedAt == crossing.endTime)
        #expect(completion.finalRewardPearls == 35)
        #expect(snapshot.unclaimedPearls == 35)
        #expect(try repository.fetchAchievementDashboard().badges.first {
            $0.badgeID == completion.badgeKey
        }?.count == 1)

        let balanceBeforeClaim = try repository.fetchPearlBalance()
        #expect(try repository.claimLookoutCompletion(id: completion.id, at: crossing.endTime) == 35)
        #expect(try repository.claimLookoutCompletion(id: completion.id, at: crossing.endTime) == 0)
        #expect(try repository.fetchPearlBalance() == balanceBeforeClaim + 35)
        #expect(try repository.fetchLookoutSnapshot(at: crossing.endTime, calendar: localCalendar).unclaimedPearls == 0)

        _ = try repository.commit(session: crossing, activeArc: nil, recentlyEndedArc: nil)
        #expect(try repository.fetchLookoutSnapshot(at: crossing.endTime, calendar: localCalendar).completions.count == 1)
    }

    @Test func objectiveCreationReconcilesExistingFlowButNeverCountsSoftFlow() throws {
        let repository = SwiftDataFlowRepository(container: try ScyraPersistenceFactory.makeContainer(inMemory: true))
        let start = Date(timeIntervalSince1970: 1_800_086_400)
        _ = try repository.commit(
            session: makeSession(id: UUID(), journey: "Writing", minutes: 45, end: start.addingTimeInterval(1_000), soft: true),
            activeArc: nil, recentlyEndedArc: nil
        )
        _ = try repository.createLookoutObjective(
            .init(
                journeyID: nil, journeyName: "Writing", period: .daily, kind: .oneTime,
                targetDurationMs: 30 * 60_000, startAt: start
            ), at: start.addingTimeInterval(2_000), calendar: localCalendar
        )
        #expect(try repository.fetchLookoutSnapshot(at: start.addingTimeInterval(2_000), calendar: localCalendar).completions.isEmpty)
        _ = try repository.commit(
            session: makeSession(id: UUID(), journey: "Writing", minutes: 31, end: start.addingTimeInterval(3_000)),
            activeArc: nil, recentlyEndedArc: nil
        )
        #expect(try repository.fetchLookoutSnapshot(at: start.addingTimeInterval(3_000), calendar: localCalendar).completions.count == 1)
    }

    @Test func v5StoreLightweightMigratesToLookoutV6() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("scyra-lookout-v6-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let storeURL = directory.appendingPathComponent("Scyra.store")
        do {
            let schema = Schema(versionedSchema: ScyraSchemaV5.self)
            let configuration = ModelConfiguration("Scyra", schema: schema, url: storeURL)
            let container = try ModelContainer(for: schema, configurations: [configuration])
            container.mainContext.insert(PearlLedgerModel(entry: .init(
                id: "legacy", delta: 7, reason: "test", sourceType: "test",
                sourceID: nil, createdAt: .now, note: nil
            )))
            try container.mainContext.save()
        }
        let schema = Schema(versionedSchema: ScyraSchemaV6.self)
        let configuration = ModelConfiguration("Scyra", schema: schema, url: storeURL)
        let upgraded = try ModelContainer(
            for: schema, migrationPlan: ScyraMigrationPlan.self, configurations: [configuration]
        )
        let repository = SwiftDataFlowRepository(container: upgraded)
        #expect(try repository.fetchPearlBalance() == 7)
        #expect(try repository.fetchLookoutSnapshot(at: .now, calendar: localCalendar).cards.isEmpty)
    }

    private func makeSession(
        id: UUID,
        journey: String,
        minutes: Int64,
        end: Date,
        soft: Bool = false
    ) -> FlowSession {
        .init(
            id: id, flowInstanceID: id, title: "Flow", description: "",
            journeyName: journey, startTime: end.addingTimeInterval(-Double(minutes * 60)),
            endTime: end, durationMs: minutes * 60_000, surgePlannedMs: nil,
            surgePoints: 0, scyraPoints: Int(minutes), isSoftMode: soft,
            arcID: nil, arcIndex: nil, arcMultiplierUsed: nil, arcBonusPoints: 0,
            createdAt: end
        )
    }
}

private extension Array {
    var single: Element? { count == 1 ? first : nil }
}
