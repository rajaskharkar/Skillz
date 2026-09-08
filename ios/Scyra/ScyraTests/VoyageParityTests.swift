import Foundation
import Testing
@testable import Scyra

@MainActor
struct VoyageParityTests {
    private var calendar: Calendar {
        var value = Calendar(identifier: .gregorian)
        value.timeZone = TimeZone(identifier: "America/New_York")!
        return value
    }

    @Test func regularFlowRecordsExcludeSoftWhileArcRecordsIncludeIt() throws {
        let arc = id(42)
        let sessions = [
            session(1, date: date(2026, 5, 20), minutes: 30, points: 100, arc: arc, multiplier: 1.3),
            session(2, date: date(2026, 5, 21), minutes: 45, points: 200, soft: true, arc: arc, multiplier: 1),
            session(3, date: date(2026, 5, 22), minutes: 60, points: 300, arc: arc, multiplier: 2)
        ]
        let stats = VoyageStatsCalculator.calculate(
            sessions: sessions, now: date(2026, 5, 30), calendar: calendar
        )
        let longestArc = try #require(stats.longestArcByTime)
        #expect(longestArc.flowCount == 3)
        #expect(longestArc.totalDurationMs == Int64(135 * 60_000))
        #expect(longestArc.totalPoints == 600)
        #expect(longestArc.peakMultiplier == 2)
        #expect(stats.bestDayByPoints?.flows.map(\.sessionID) == [id(3)])
        #expect(stats.longestFlow?.sessionID == id(3))
    }

    @Test func streakCollapsesDuplicateDaysAndRemainsActiveThroughYesterday() throws {
        let sessions = [
            session(1, date: date(2026, 5, 27)),
            session(2, date: date(2026, 5, 28)),
            session(3, date: date(2026, 5, 29)),
            session(4, date: date(2026, 5, 29), points: 200)
        ]
        let active = VoyageStatsCalculator.calculate(
            sessions: sessions, now: date(2026, 5, 30), calendar: calendar
        )
        #expect(active.currentDailyStreak?.days == 3)
        #expect(active.longestDailyStreak?.days == 3)
        let broken = VoyageStatsCalculator.calculate(
            sessions: sessions, now: date(2026, 6, 1), calendar: calendar
        )
        #expect(broken.currentDailyStreak == nil)
        #expect(broken.longestDailyStreak?.days == 3)
    }

    @Test func pointsPeriodsUseCompletionDayMondayWeeksAndCalendarMonths() throws {
        let sessions = [
            session(1, date: date(2026, 5, 25), points: 900),
            session(2, date: date(2026, 5, 31), points: 200),
            session(3, date: date(2026, 6, 1), points: 100)
        ]
        let stats = VoyageStatsCalculator.calculate(
            sessions: sessions, now: date(2026, 6, 2), calendar: calendar
        )
        #expect(stats.bestDayByPoints?.startDate == calendar.startOfDay(for: date(2026, 5, 25)))
        #expect(stats.bestWeekByPoints?.points == 1_100)
        #expect(stats.bestWeekByPoints?.startDate == calendar.startOfDay(for: date(2026, 5, 25)))
        #expect(stats.bestWeekByPoints?.endDate == calendar.startOfDay(for: date(2026, 5, 31)))
        #expect(stats.bestMonthByPoints?.points == 1_100)
        #expect(stats.bestMonthByPoints?.startDate == calendar.startOfDay(for: date(2026, 5, 1)))
        #expect(stats.bestMonthByPoints?.endDate == calendar.startOfDay(for: date(2026, 5, 31)))
    }

    @Test func arcAndFlowTiesPreferMostRecentThenLowestStableIdentifier() throws {
        let sameEnd = date(2026, 5, 22)
        let smaller = id(2)
        let larger = id(9)
        let arcSmaller = id(4)
        let arcLarger = id(7)
        let sessions = [
            session(larger, date: sameEnd, minutes: 60, points: 1_000),
            session(smaller, date: sameEnd, minutes: 60, points: 1_000),
            session(10, date: date(2026, 5, 23), minutes: 30, arc: arcLarger),
            session(11, date: date(2026, 5, 23), minutes: 30, arc: arcLarger),
            session(12, date: date(2026, 5, 23), minutes: 30, arc: arcSmaller),
            session(13, date: date(2026, 5, 23), minutes: 30, arc: arcSmaller)
        ]
        let stats = VoyageStatsCalculator.calculate(
            sessions: sessions, now: date(2026, 5, 30), calendar: calendar
        )
        #expect(stats.bestFlowByPoints?.sessionID == smaller)
        #expect(stats.longestArcByTime?.arcID == arcSmaller)
        #expect(stats.mostChainedFlowsInArc?.arcID == arcSmaller)
    }

    @Test func emptyAndSingleArcInputsHaveCanonicalRecordAvailability() throws {
        let empty = VoyageStatsCalculator.calculate(
            sessions: [], now: date(2026, 5, 30), calendar: calendar
        )
        #expect(!empty.hasEligibleFlows)
        #expect(empty.bestFlowByPoints == nil)
        let one = VoyageStatsCalculator.calculate(
            sessions: [session(1, date: date(2026, 5, 29), arc: id(99))],
            now: date(2026, 5, 30), calendar: calendar
        )
        #expect(one.hasEligibleFlows)
        #expect(one.longestArcByTime?.flowCount == 1)
        #expect(one.mostArcsInDay?.count == 1)
    }

    private func date(_ year: Int, _ month: Int, _ day: Int) -> Date {
        calendar.date(from: .init(
            timeZone: calendar.timeZone, year: year, month: month, day: day, hour: 12
        ))!
    }

    private func id(_ value: Int) -> UUID {
        UUID(uuidString: String(format: "00000000-0000-0000-0000-%012d", value))!
    }

    private func session(
        _ value: Int,
        date: Date,
        minutes: Int64 = 30,
        points: Int = 100,
        soft: Bool = false,
        arc: UUID? = nil,
        multiplier: Double? = nil
    ) -> FlowSession {
        session(id(value), date: date, minutes: minutes, points: points, soft: soft, arc: arc, multiplier: multiplier)
    }

    private func session(
        _ identifier: UUID,
        date: Date,
        minutes: Int64 = 30,
        points: Int = 100,
        soft: Bool = false,
        arc: UUID? = nil,
        multiplier: Double? = nil
    ) -> FlowSession {
        .init(
            id: identifier, flowInstanceID: identifier, title: "Flow \(identifier.uuidString.suffix(2))",
            description: "", journeyName: "Scyra",
            startTime: date.addingTimeInterval(-Double(minutes * 60)), endTime: date,
            durationMs: minutes * 60_000, surgePlannedMs: nil, surgePoints: 0,
            scyraPoints: points, isSoftMode: soft, arcID: arc, arcIndex: arc == nil ? nil : 1,
            arcMultiplierUsed: multiplier, arcBonusPoints: 0, createdAt: date
        )
    }
}
