import Foundation

struct VoyageFlowSummary: Identifiable, Equatable, Sendable {
    var id: UUID { sessionID }
    let sessionID: UUID
    let title: String
    let journeyName: String?
    let durationMs: Int64
    let points: Int
    let completedAt: Date
    let arcID: UUID?
    let arcIndex: Int?
    let arcMultiplierUsed: Double?
}

struct VoyageArcSummary: Identifiable, Equatable, Sendable {
    var id: UUID { arcID }
    let arcID: UUID
    let flowCount: Int
    let regularFlowCount: Int
    let softFlowCount: Int
    let totalDurationMs: Int64
    let totalPoints: Int
    let totalArcBonusPoints: Int
    let peakMultiplier: Double?
    let latestFlowEnd: Date
    let flows: [VoyageFlowSummary]
}

struct VoyageStreakRecord: Equatable, Sendable {
    let days: Int
    let startDate: Date
    let endDate: Date
}

struct VoyageArcRecord: Identifiable, Equatable, Sendable {
    var id: UUID { arcID }
    let arcID: UUID
    let totalDurationMs: Int64
    let flowCount: Int
    let totalPoints: Int
    let peakMultiplier: Double?
    let latestFlowEnd: Date
    let flows: [VoyageFlowSummary]
}

struct VoyageMultiplierRecord: Equatable, Sendable {
    let arcID: UUID
    let multiplier: Double
    let flowCount: Int
    let reachedAt: Date
    let reachedInSessionID: UUID
    let totalDurationMs: Int64
    let totalPoints: Int
    let flows: [VoyageFlowSummary]
}

struct VoyagePeriodPointsRecord: Equatable, Sendable {
    let points: Int
    let totalDurationMs: Int64
    let flowCount: Int
    let startDate: Date
    let endDate: Date
    let flows: [VoyageFlowSummary]
}

struct VoyagePeriodDurationRecord: Equatable, Sendable {
    let durationMs: Int64
    let points: Int
    let flowCount: Int
    let startDate: Date
    let endDate: Date
    let flows: [VoyageFlowSummary]
}

struct VoyagePeriodCountRecord: Equatable, Sendable {
    let count: Int
    let totalDurationMs: Int64
    let points: Int
    let startDate: Date
    let endDate: Date
    let flows: [VoyageFlowSummary]
    let arcs: [VoyageArcSummary]
}

struct VoyageHallStats: Equatable, Sendable {
    let currentDailyStreak: VoyageStreakRecord?
    let longestDailyStreak: VoyageStreakRecord?
    let longestArcByTime: VoyageArcRecord?
    let highestArcMultiplier: VoyageMultiplierRecord?
    let mostChainedFlowsInArc: VoyageArcRecord?
    let bestDayByPoints: VoyagePeriodPointsRecord?
    let bestWeekByPoints: VoyagePeriodPointsRecord?
    let bestMonthByPoints: VoyagePeriodPointsRecord?
    let bestFlowByPoints: VoyageFlowSummary?
    let longestFlow: VoyageFlowSummary?
    let mostFlowsInDay: VoyagePeriodCountRecord?
    let mostTimeInDay: VoyagePeriodDurationRecord?
    let mostTimeInWeek: VoyagePeriodDurationRecord?
    let mostTimeInMonth: VoyagePeriodDurationRecord?
    let mostArcsInDay: VoyagePeriodCountRecord?
    let mostArcsInWeek: VoyagePeriodCountRecord?
    let hasEligibleFlows: Bool
}

enum VoyageStatsCalculator {
    static func calculate(
        sessions: [FlowSession],
        now: Date,
        calendar: Calendar = .current
    ) -> VoyageHallStats {
        let completed = sessions.filter { $0.endTime.timeIntervalSince1970 > 0 && $0.durationMs > 0 }
        let regular = completed.filter { !$0.isSoftMode }
        let regularSummaries = regular.map { summary($0) }
        let regularDates = Set(regular.map { calendar.startOfDay(for: $0.endTime) })
        let arcSummaries = Dictionary(grouping: completed.filter { $0.arcID != nil }, by: { $0.arcID! })
            .map { arcSummary(id: $0.key, flows: $0.value) }

        return VoyageHallStats(
            currentDailyStreak: currentStreak(dates: regularDates, now: now, calendar: calendar),
            longestDailyStreak: longestStreak(dates: regularDates, calendar: calendar),
            longestArcByTime: bestArc(arcSummaries, metric: \.totalDurationMs),
            highestArcMultiplier: highestMultiplier(arcSummaries),
            mostChainedFlowsInArc: bestArc(arcSummaries, metric: { Int64($0.flowCount) }),
            bestDayByPoints: bestPoints(groups: grouped(regularSummaries, calendar: calendar, period: .day)),
            bestWeekByPoints: bestPoints(groups: grouped(regularSummaries, calendar: calendar, period: .week)),
            bestMonthByPoints: bestPoints(groups: grouped(regularSummaries, calendar: calendar, period: .month)),
            bestFlowByPoints: bestFlow(regularSummaries, metric: { Int64($0.points) }),
            longestFlow: bestFlow(regularSummaries, metric: \.durationMs),
            mostFlowsInDay: bestCount(groups: grouped(regularSummaries, calendar: calendar, period: .day)),
            mostTimeInDay: bestDuration(groups: grouped(regularSummaries, calendar: calendar, period: .day)),
            mostTimeInWeek: bestDuration(groups: grouped(regularSummaries, calendar: calendar, period: .week)),
            mostTimeInMonth: bestDuration(groups: grouped(regularSummaries, calendar: calendar, period: .month)),
            mostArcsInDay: bestArcCount(groups: groupedArcs(arcSummaries, calendar: calendar, period: .day)),
            mostArcsInWeek: bestArcCount(groups: groupedArcs(arcSummaries, calendar: calendar, period: .week)),
            hasEligibleFlows: !regular.isEmpty || !arcSummaries.isEmpty
        )
    }

    private static func summary(_ flow: FlowSession) -> VoyageFlowSummary {
        .init(
            sessionID: flow.id, title: flow.title,
            journeyName: flow.journeyName.isEmpty ? nil : flow.journeyName,
            durationMs: flow.durationMs, points: flow.scyraPoints,
            completedAt: flow.endTime, arcID: flow.arcID, arcIndex: flow.arcIndex,
            arcMultiplierUsed: flow.arcMultiplierUsed
        )
    }

    private static func arcSummary(id: UUID, flows: [FlowSession]) -> VoyageArcSummary {
        let orderedSessions = flows.sorted {
            if $0.endTime != $1.endTime { return $0.endTime < $1.endTime }
            return $0.id.uuidString < $1.id.uuidString
        }
        let summaries = orderedSessions.map { summary($0) }
        return .init(
            arcID: id, flowCount: flows.count,
            regularFlowCount: flows.filter { !$0.isSoftMode }.count,
            softFlowCount: flows.filter(\.isSoftMode).count,
            totalDurationMs: summaries.reduce(0) { $0 + $1.durationMs },
            totalPoints: summaries.reduce(0) { $0 + $1.points },
            totalArcBonusPoints: flows.reduce(0) { $0 + $1.arcBonusPoints },
            peakMultiplier: summaries.compactMap(\.arcMultiplierUsed).max() ?? 1,
            latestFlowEnd: summaries.map(\.completedAt).max() ?? .distantPast,
            flows: summaries
        )
    }

    private static func currentStreak(
        dates: Set<Date>, now: Date, calendar: Calendar
    ) -> VoyageStreakRecord? {
        guard let latest = dates.max() else { return nil }
        let today = calendar.startOfDay(for: now)
        let yesterday = calendar.date(byAdding: .day, value: -1, to: today)
        guard latest == today || latest == yesterday else { return nil }
        var cursor = latest
        var count = 0
        while dates.contains(cursor) {
            count += 1
            guard let prior = calendar.date(byAdding: .day, value: -1, to: cursor) else { break }
            cursor = prior
        }
        let start = calendar.date(byAdding: .day, value: 1, to: cursor) ?? latest
        return .init(days: count, startDate: start, endDate: latest)
    }

    private static func longestStreak(dates: Set<Date>, calendar: Calendar) -> VoyageStreakRecord? {
        let ordered = dates.sorted()
        guard var runStart = ordered.first, var previous = ordered.first else { return nil }
        var bestStart = runStart
        var bestEnd = previous
        for date in ordered.dropFirst() {
            if calendar.date(byAdding: .day, value: 1, to: previous) == date {
                previous = date
            } else {
                if streakIsBetter(start: runStart, end: previous, than: bestStart, bestEnd, calendar: calendar) {
                    bestStart = runStart; bestEnd = previous
                }
                runStart = date; previous = date
            }
        }
        if streakIsBetter(start: runStart, end: previous, than: bestStart, bestEnd, calendar: calendar) {
            bestStart = runStart; bestEnd = previous
        }
        let days = (calendar.dateComponents([.day], from: bestStart, to: bestEnd).day ?? 0) + 1
        return .init(days: days, startDate: bestStart, endDate: bestEnd)
    }

    private static func streakIsBetter(
        start: Date, end: Date, than bestStart: Date, _ bestEnd: Date, calendar: Calendar
    ) -> Bool {
        let count = calendar.dateComponents([.day], from: start, to: end).day ?? 0
        let best = calendar.dateComponents([.day], from: bestStart, to: bestEnd).day ?? 0
        return count > best || (count == best && end > bestEnd)
    }

    private static func bestArc(
        _ arcs: [VoyageArcSummary], metric: (VoyageArcSummary) -> Int64
    ) -> VoyageArcRecord? {
        arcs.sorted {
            if metric($0) != metric($1) { return metric($0) > metric($1) }
            if $0.latestFlowEnd != $1.latestFlowEnd { return $0.latestFlowEnd > $1.latestFlowEnd }
            return $0.arcID.uuidString < $1.arcID.uuidString
        }.first.map {
            .init(
                arcID: $0.arcID, totalDurationMs: $0.totalDurationMs,
                flowCount: $0.flowCount, totalPoints: $0.totalPoints,
                peakMultiplier: $0.peakMultiplier, latestFlowEnd: $0.latestFlowEnd,
                flows: $0.flows
            )
        }
    }

    private static func highestMultiplier(_ arcs: [VoyageArcSummary]) -> VoyageMultiplierRecord? {
        let candidates = arcs.flatMap { arc in
            arc.flows.compactMap { flow -> (VoyageArcSummary, VoyageFlowSummary, Double)? in
                flow.arcMultiplierUsed.map { (arc, flow, $0) }
            }
        }.sorted {
            if $0.2 != $1.2 { return $0.2 > $1.2 }
            if $0.1.completedAt != $1.1.completedAt { return $0.1.completedAt > $1.1.completedAt }
            return $0.1.sessionID.uuidString < $1.1.sessionID.uuidString
        }
        guard let value = candidates.first else { return nil }
        return .init(
            arcID: value.0.arcID, multiplier: value.2, flowCount: value.0.flowCount,
            reachedAt: value.1.completedAt, reachedInSessionID: value.1.sessionID,
            totalDurationMs: value.0.totalDurationMs, totalPoints: value.0.totalPoints,
            flows: value.0.flows
        )
    }

    private static func bestFlow(
        _ flows: [VoyageFlowSummary], metric: (VoyageFlowSummary) -> Int64
    ) -> VoyageFlowSummary? {
        flows.sorted {
            if metric($0) != metric($1) { return metric($0) > metric($1) }
            if $0.completedAt != $1.completedAt { return $0.completedAt > $1.completedAt }
            return $0.sessionID.uuidString < $1.sessionID.uuidString
        }.first
    }

    private enum PeriodKind { case day, week, month }
    private struct Period: Hashable { let start: Date; let end: Date }

    private static func period(for date: Date, calendar: Calendar, kind: PeriodKind) -> Period {
        let day = calendar.startOfDay(for: date)
        switch kind {
        case .day: return .init(start: day, end: day)
        case .week:
            let daysAfterMonday = (calendar.component(.weekday, from: day) + 5) % 7
            let start = calendar.date(byAdding: .day, value: -daysAfterMonday, to: day) ?? day
            return .init(start: start, end: calendar.date(byAdding: .day, value: 6, to: start) ?? start)
        case .month:
            let interval = calendar.dateInterval(of: .month, for: day)
            let start = interval?.start ?? day
            let end = calendar.date(byAdding: .day, value: -1, to: interval?.end ?? day) ?? day
            return .init(start: start, end: end)
        }
    }

    private static func grouped(
        _ flows: [VoyageFlowSummary], calendar: Calendar, period kind: PeriodKind
    ) -> [Period: [VoyageFlowSummary]] {
        Dictionary(grouping: flows, by: { period(for: $0.completedAt, calendar: calendar, kind: kind) })
    }

    private static func groupedArcs(
        _ arcs: [VoyageArcSummary], calendar: Calendar, period kind: PeriodKind
    ) -> [Period: [VoyageArcSummary]] {
        Dictionary(grouping: arcs, by: { period(for: $0.latestFlowEnd, calendar: calendar, kind: kind) })
    }

    private static func ordered(_ flows: [VoyageFlowSummary]) -> [VoyageFlowSummary] {
        flows.sorted {
            if $0.completedAt != $1.completedAt { return $0.completedAt < $1.completedAt }
            return $0.sessionID.uuidString < $1.sessionID.uuidString
        }
    }

    private static func bestPoints(groups: [Period: [VoyageFlowSummary]]) -> VoyagePeriodPointsRecord? {
        groups.map { period, flows in
            let flows = ordered(flows)
            return VoyagePeriodPointsRecord(
                points: flows.reduce(0) { $0 + $1.points },
                totalDurationMs: flows.reduce(Int64(0)) { $0 + $1.durationMs },
                flowCount: flows.count, startDate: period.start, endDate: period.end, flows: flows
            )
        }.sorted {
            if $0.points != $1.points { return $0.points > $1.points }
            if $0.endDate != $1.endDate { return $0.endDate > $1.endDate }
            return $0.startDate > $1.startDate
        }.first
    }

    private static func bestDuration(groups: [Period: [VoyageFlowSummary]]) -> VoyagePeriodDurationRecord? {
        groups.map { period, flows in
            let flows = ordered(flows)
            return VoyagePeriodDurationRecord(
                durationMs: flows.reduce(Int64(0)) { $0 + $1.durationMs },
                points: flows.reduce(0) { $0 + $1.points }, flowCount: flows.count,
                startDate: period.start, endDate: period.end, flows: flows
            )
        }.sorted {
            if $0.durationMs != $1.durationMs { return $0.durationMs > $1.durationMs }
            if $0.endDate != $1.endDate { return $0.endDate > $1.endDate }
            return $0.startDate > $1.startDate
        }.first
    }

    private static func bestCount(groups: [Period: [VoyageFlowSummary]]) -> VoyagePeriodCountRecord? {
        groups.map { period, flows in
            let flows = ordered(flows)
            return VoyagePeriodCountRecord(
                count: flows.count, totalDurationMs: flows.reduce(Int64(0)) { $0 + $1.durationMs },
                points: flows.reduce(0) { $0 + $1.points }, startDate: period.start,
                endDate: period.end, flows: flows, arcs: []
            )
        }.sorted {
            if $0.count != $1.count { return $0.count > $1.count }
            if $0.endDate != $1.endDate { return $0.endDate > $1.endDate }
            return $0.startDate > $1.startDate
        }.first
    }

    private static func bestArcCount(groups: [Period: [VoyageArcSummary]]) -> VoyagePeriodCountRecord? {
        groups.map { period, arcs in
            let arcs = arcs.sorted {
                if $0.latestFlowEnd != $1.latestFlowEnd { return $0.latestFlowEnd < $1.latestFlowEnd }
                return $0.arcID.uuidString < $1.arcID.uuidString
            }
            return VoyagePeriodCountRecord(
                count: arcs.count, totalDurationMs: arcs.reduce(Int64(0)) { $0 + $1.totalDurationMs },
                points: arcs.reduce(0) { $0 + $1.totalPoints }, startDate: period.start,
                endDate: period.end, flows: [], arcs: arcs
            )
        }.sorted {
            if $0.count != $1.count { return $0.count > $1.count }
            if $0.endDate != $1.endDate { return $0.endDate > $1.endDate }
            return $0.startDate > $1.startDate
        }.first
    }
}
