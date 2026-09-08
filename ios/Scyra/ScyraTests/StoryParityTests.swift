import Foundation
import Testing
@testable import Scyra

@MainActor
struct StoryCalendarParityTests {
    @Test func weekStartsMondayInTheDeviceTimeZone() throws {
        let timeZone = try #require(TimeZone(identifier: "America/Chicago"))
        let calendar = StoryCalendarRules.appCalendar(timeZone: timeZone)
        let sunday = try #require(calendar.date(from: DateComponents(
            year: 2026, month: 8, day: 23, hour: 12
        )))
        let start = StoryCalendarRules.start(of: .week, containing: sunday, calendar: calendar)
        let components = calendar.dateComponents([.year, .month, .day, .weekday], from: start)

        #expect(components.year == 2026)
        #expect(components.month == 8)
        #expect(components.day == 17)
        #expect(components.weekday == 2)
    }

    @Test func dayWindowUsesCalendarBoundariesAcrossDaylightSaving() throws {
        let timeZone = try #require(TimeZone(identifier: "America/Chicago"))
        let calendar = StoryCalendarRules.appCalendar(timeZone: timeZone)
        let noonOnSpringForward = try #require(calendar.date(from: DateComponents(
            year: 2026, month: 3, day: 8, hour: 12
        )))
        let window = StoryCalendarRules.window(for: noonOnSpringForward, period: .day, calendar: calendar)

        #expect(window.end.timeIntervalSince(window.start) == 23 * 60 * 60)
        #expect(window.contains(window.start))
        #expect(!window.contains(window.end))
    }
}

@MainActor
struct StoryViewModelParityTests {
    @Test func storyUsesCreatedAtHalfOpenWindowAndJourneyFilters() throws {
        let context = try makeContext()
        let repository = InMemoryFlowRepository()
        let now = try context.date(2026, 8, 19, 12)
        let monday = try context.date(2026, 8, 17, 0)
        let nextMonday = try context.date(2026, 8, 24, 0)

        repository.commit(session: makeSession(title: "Boundary", journey: "Work", createdAt: monday), activeArc: nil, recentlyEndedArc: nil)
        repository.commit(session: makeSession(title: "Rest", journey: "Life", createdAt: try context.date(2026, 8, 18, 8)), activeArc: nil, recentlyEndedArc: nil)
        repository.commit(session: makeSession(title: "Excluded", journey: "Work", createdAt: nextMonday), activeArc: nil, recentlyEndedArc: nil)

        let viewModel = StoryViewModel(repository: repository, now: { now }, calendar: context.calendar)
        #expect(viewModel.visibleSessions.map(\.title) == ["Rest", "Boundary"])

        viewModel.toggleJourney("Work")
        #expect(viewModel.visibleSessions.map(\.title) == ["Boundary"])
        #expect(viewModel.totalDurationMs == 10 * 60_000)
    }

    @Test func arcGroupingUsesWholeArcTotalsAndReportsFlowsOutsideView() throws {
        let context = try makeContext()
        let repository = InMemoryFlowRepository()
        let now = try context.date(2026, 8, 19, 12)
        let arcID = UUID()

        repository.commit(
            session: makeSession(
                title: "Earlier link",
                journey: "Scyra",
                createdAt: try context.date(2026, 8, 10, 9),
                durationMs: 20 * 60_000,
                score: 30,
                arcID: arcID,
                arcIndex: 1,
                multiplier: 1
            ),
            activeArc: nil,
            recentlyEndedArc: nil
        )
        repository.commit(
            session: makeSession(
                title: "Current link",
                journey: "Scyra",
                createdAt: try context.date(2026, 8, 18, 9),
                durationMs: 30 * 60_000,
                score: 55,
                arcID: arcID,
                arcIndex: 2,
                multiplier: 1.4
            ),
            activeArc: nil,
            recentlyEndedArc: nil
        )

        let viewModel = StoryViewModel(repository: repository, now: { now }, calendar: context.calendar)
        let item = try #require(viewModel.chronicleItems.first)
        guard case .arc(let group) = item else {
            Issue.record("Expected an Arc group")
            return
        }

        #expect(group.visibleFlows.map(\.title) == ["Current link"])
        #expect(group.hiddenFlowCount == 1)
        #expect(group.totalFlowCount == 2)
        #expect(group.totalDurationMs == 50 * 60_000)
        #expect(group.totalScore == 85)
        #expect(abs((group.peakMultiplier ?? 0) - 1.4) < 0.000_001)
    }

    @Test func sagaRankingMatchesAndroidScoreThenDurationOrder() throws {
        let context = try makeContext()
        let repository = InMemoryFlowRepository()
        let now = try context.date(2026, 8, 19, 12)
        let date = try context.date(2026, 8, 18, 9)

        repository.commit(session: makeSession(title: "A", journey: "Lower", createdAt: date, score: 10), activeArc: nil, recentlyEndedArc: nil)
        repository.commit(session: makeSession(title: "B", journey: "Higher", createdAt: date, score: 20), activeArc: nil, recentlyEndedArc: nil)

        let viewModel = StoryViewModel(repository: repository, now: { now }, calendar: context.calendar)
        #expect(viewModel.sagas.map(\.journeyName) == ["Higher", "Lower"])
    }

    private func makeContext() throws -> TestCalendarContext {
        let timeZone = try #require(TimeZone(identifier: "America/Chicago"))
        return TestCalendarContext(calendar: StoryCalendarRules.appCalendar(timeZone: timeZone))
    }

    private func makeSession(
        title: String,
        journey: String,
        createdAt: Date,
        durationMs: Int64 = 10 * 60_000,
        score: Int = 15,
        arcID: UUID? = nil,
        arcIndex: Int? = nil,
        multiplier: Double? = nil
    ) -> FlowSession {
        FlowSession(
            id: UUID(),
            flowInstanceID: UUID(),
            title: title,
            description: "",
            journeyName: journey,
            startTime: createdAt.addingTimeInterval(-Double(durationMs) / 1_000),
            endTime: createdAt,
            durationMs: durationMs,
            surgePlannedMs: nil,
            surgePoints: 0,
            scyraPoints: score,
            isSoftMode: false,
            arcID: arcID,
            arcIndex: arcIndex,
            arcMultiplierUsed: multiplier,
            arcBonusPoints: 0,
            createdAt: createdAt
        )
    }
}

private struct TestCalendarContext {
    let calendar: Calendar

    func date(_ year: Int, _ month: Int, _ day: Int, _ hour: Int) throws -> Date {
        try #require(calendar.date(from: DateComponents(
            year: year,
            month: month,
            day: day,
            hour: hour
        )))
    }
}
