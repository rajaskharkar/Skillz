import Foundation

enum StoryPeriod: String, CaseIterable, Identifiable, Sendable {
    case day = "Day"
    case week = "Week"
    case month = "Month"

    var id: Self { self }
}

struct StoryTimeWindow: Equatable, Sendable {
    let start: Date
    let end: Date

    func contains(_ date: Date) -> Bool {
        date >= start && date < end
    }
}

enum StoryCalendarRules {
    static func appCalendar(timeZone: TimeZone = .current) -> Calendar {
        var calendar = Calendar(identifier: .iso8601)
        calendar.locale = .current
        calendar.timeZone = timeZone
        calendar.firstWeekday = 2
        calendar.minimumDaysInFirstWeek = 4
        return calendar
    }

    static func start(
        of period: StoryPeriod,
        containing date: Date,
        calendar: Calendar = appCalendar()
    ) -> Date {
        window(for: date, period: period, calendar: calendar).start
    }

    static func window(
        for anchor: Date,
        period: StoryPeriod,
        calendar: Calendar = appCalendar()
    ) -> StoryTimeWindow {
        let component: Calendar.Component = switch period {
        case .day: .day
        case .week: .weekOfYear
        case .month: .month
        }

        guard let interval = calendar.dateInterval(of: component, for: anchor) else {
            let start = calendar.startOfDay(for: anchor)
            return StoryTimeWindow(start: start, end: start.addingTimeInterval(86_400))
        }
        return StoryTimeWindow(start: interval.start, end: interval.end)
    }

    static func shifted(
        _ anchor: Date,
        period: StoryPeriod,
        direction: Int,
        calendar: Calendar = appCalendar()
    ) -> Date {
        let component: Calendar.Component = switch period {
        case .day: .day
        case .week: .weekOfYear
        case .month: .month
        }
        let shifted = calendar.date(byAdding: component, value: direction, to: anchor) ?? anchor
        return start(of: period, containing: shifted, calendar: calendar)
    }
}

struct StorySaga: Identifiable, Equatable, Sendable {
    var id: String { journeyName }
    let journeyName: String
    let totalScore: Int
    let totalDurationMs: Int64
    let flowCount: Int
}

struct ArcMetadata: Equatable, Sendable {
    static let titleLimit = 60
    static let summaryLimit = 500
    static let reflectionLimit = 250

    let arcID: UUID
    let title: String?
    let summary: String?
    let outcome: String?
    let highlight: String?
    let nextStep: String?

    init(
        arcID: UUID,
        title: String? = nil,
        summary: String? = nil,
        outcome: String? = nil,
        highlight: String? = nil,
        nextStep: String? = nil
    ) {
        self.arcID = arcID
        self.title = title
        self.summary = summary
        self.outcome = outcome
        self.highlight = highlight
        self.nextStep = nextStep
    }

    static func normalized(
        arcID: UUID,
        title: String,
        summary: String,
        outcome: String,
        highlight: String,
        nextStep: String
    ) -> Self {
        Self(
            arcID: arcID,
            title: normalize(title),
            summary: normalize(summary),
            outcome: normalize(outcome),
            highlight: normalize(highlight),
            nextStep: normalize(nextStep)
        )
    }

    var isEmpty: Bool {
        title == nil && summary == nil && outcome == nil && highlight == nil && nextStep == nil
    }

    var hasReflection: Bool {
        outcome != nil || highlight != nil || nextStep != nil
    }

    private static func normalize(_ value: String) -> String? {
        let normalized = value.trimmingCharacters(in: .whitespacesAndNewlines)
        return normalized.isEmpty ? nil : normalized
    }
}

struct StoryArcGroup: Identifiable, Equatable, Sendable {
    let id: UUID
    let visibleFlows: [FlowSession]
    let hiddenFlowCount: Int
    let totalFlowCount: Int
    let totalDurationMs: Int64
    let totalScore: Int
    let peakMultiplier: Double?
    let mostRecentAt: Date
    let childPulsesByFlowID: [UUID: [StoryPulseItem]]
    let chroniclesByFlowID: [UUID: ChronicleSnapshot]
    let movementByFlowID: [UUID: FlowHealthSnapshot]
    let metadata: ArcMetadata?
}

struct StoryPulseItem: Identifiable, Equatable, Sendable {
    var id: UUID { pulse.id }
    let pulse: Pulse
    let chronicle: ChronicleSnapshot
}

enum StoryChronicleItem: Identifiable, Equatable, Sendable {
    case flow(FlowSession, childPulses: [StoryPulseItem])
    case pulse(StoryPulseItem)
    case arc(StoryArcGroup)

    var id: String {
        switch self {
        case .flow(let flow, _): "flow-\(flow.id.uuidString)"
        case .pulse(let pulse): "pulse-\(pulse.id.uuidString)"
        case .arc(let arc): "arc-\(arc.id.uuidString)"
        }
    }

    var mostRecentAt: Date {
        switch self {
        case .flow(let flow, _): flow.createdAt
        case .pulse(let pulse): pulse.pulse.createdAt
        case .arc(let arc): arc.mostRecentAt
        }
    }
}
