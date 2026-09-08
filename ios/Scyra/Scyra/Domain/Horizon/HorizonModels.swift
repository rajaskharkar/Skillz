import Foundation

enum HorizonPrimaryTab: String, CaseIterable, Identifiable, Sendable {
    case flows = "Flows"
    case arcs = "Arcs"

    var id: String { rawValue }
}

/// Android currently retains this selection in Paths state even though it does
/// not filter the plan library. Keeping it explicit prevents a future planning
/// lens from being confused with Story's date-window filtering.
enum HorizonTimeLens: String, CaseIterable, Identifiable, Sendable {
    case day = "Day"
    case week = "Week"
    case month = "Month"

    var id: String { rawValue }
}

struct FlowPlan: Identifiable, Equatable, Sendable {
    let id: UUID
    var title: String
    var journeyName: String?
    var isSoftMode: Bool
    var targetMinutes: Int?
    var launchWithSurge: Bool
    var pinned: Bool
    var archived: Bool
    var launchCount: Int
    var lastLaunchedAt: Date?
    let createdAt: Date
    var updatedAt: Date
}

struct FlowPlanDraft: Equatable, Sendable {
    var title = ""
    var journeyName = ""
    var isSoftMode = false
    var targetMinutesText = ""
    var launchWithSurge = false

    func validated() throws -> FlowPlanInput {
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty else { throw FlowPlanValidationError.titleRequired }

        let normalizedJourney = journeyName.trimmingCharacters(in: .whitespacesAndNewlines)
        let minutesText = targetMinutesText.trimmingCharacters(in: .whitespacesAndNewlines)
        let parsedMinutes = minutesText.isEmpty ? nil : Int(minutesText)
        if !minutesText.isEmpty {
            guard let parsedMinutes, parsedMinutes > 0 else {
                throw FlowPlanValidationError.targetMinutesMustBePositive
            }
        }

        let target = isSoftMode ? nil : parsedMinutes
        return FlowPlanInput(
            title: normalizedTitle,
            journeyName: normalizedJourney.isEmpty ? nil : normalizedJourney,
            isSoftMode: isSoftMode,
            targetMinutes: target,
            launchWithSurge: !isSoftMode && target != nil && launchWithSurge
        )
    }
}

struct FlowPlanInput: Equatable, Sendable {
    let title: String
    let journeyName: String?
    let isSoftMode: Bool
    let targetMinutes: Int?
    let launchWithSurge: Bool

    func normalizedForPersistence() -> FlowPlanInput {
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedJourney = journeyName?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .nilIfEmpty
        let normalizedTarget = targetMinutes.flatMap { $0 > 0 ? $0 : nil }
        return FlowPlanInput(
            title: normalizedTitle,
            journeyName: normalizedJourney,
            isSoftMode: isSoftMode,
            targetMinutes: normalizedTarget,
            launchWithSurge: !isSoftMode && normalizedTarget != nil && launchWithSurge
        )
    }
}

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}

enum FlowPlanValidationError: LocalizedError, Equatable {
    case titleRequired
    case targetMinutesMustBePositive

    var errorDescription: String? {
        switch self {
        case .titleRequired:
            "Title is required."
        case .targetMinutesMustBePositive:
            "Target minutes must be greater than 0."
        }
    }
}

enum ArcPlanRecurrence: String, CaseIterable, Identifiable, Codable, Sendable {
    case oneTime = "one_time"
    case daily
    case weekdays
    case weekly
    case custom

    var id: String { rawValue }

    var title: String {
        switch self {
        case .oneTime: "One time"
        case .daily: "Daily"
        case .weekdays: "Weekdays"
        case .weekly: "Weekly"
        case .custom: "Custom"
        }
    }

    var defaultDays: Set<Int> {
        switch self {
        case .weekdays: Set(1...5)
        case .weekly: [1]
        case .oneTime, .daily, .custom: []
        }
    }
}

enum ArcPlanStepLinkState: String, Codable, Sendable {
    case linked
    case customized
    case detached
}

struct ArcPlanStep: Identifiable, Equatable, Sendable {
    let id: UUID
    let orderIndex: Int
    let sourceFlowPlanID: UUID?
    let titleSnapshot: String
    let journeyNameSnapshot: String?
    let isSoftModeSnapshot: Bool
    let targetMinutesSnapshot: Int?
    let launchWithSurgeSnapshot: Bool
    let linkState: ArcPlanStepLinkState
    let createdAt: Date
    let updatedAt: Date
}

struct ArcPlanStepInput: Equatable, Sendable {
    let sourceFlowPlanID: UUID?
    let titleSnapshot: String
    let journeyNameSnapshot: String?
    let isSoftModeSnapshot: Bool
    let targetMinutesSnapshot: Int?
    let launchWithSurgeSnapshot: Bool
    let linkState: ArcPlanStepLinkState

    func normalizedForPersistence() -> ArcPlanStepInput {
        let title = titleSnapshot.trimmingCharacters(in: .whitespacesAndNewlines)
        let journey = journeyNameSnapshot?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .nilIfEmpty
        let target = targetMinutesSnapshot.flatMap { $0 > 0 ? $0 : nil }
        return ArcPlanStepInput(
            sourceFlowPlanID: sourceFlowPlanID,
            titleSnapshot: title,
            journeyNameSnapshot: journey,
            isSoftModeSnapshot: isSoftModeSnapshot,
            targetMinutesSnapshot: target,
            launchWithSurgeSnapshot: !isSoftModeSnapshot && target != nil && launchWithSurgeSnapshot,
            linkState: linkState
        )
    }
}

struct ArcPlanInput: Equatable, Sendable {
    let title: String
    let steps: [ArcPlanStepInput]
    let isInStudio: Bool
    let recurrence: ArcPlanRecurrence
    let recurrenceDays: Set<Int>

    func validated() throws -> ArcPlanInput {
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty else { throw ArcPlanValidationError.titleRequired }
        guard steps.count >= 2 else { throw ArcPlanValidationError.needsTwoFlows }
        guard !steps.contains(where: \.isSoftModeSnapshot) else {
            throw ArcPlanValidationError.softFlowNotAllowed
        }
        let normalizedSteps = steps.map { $0.normalizedForPersistence() }
        guard normalizedSteps.allSatisfy({ !$0.titleSnapshot.isEmpty }) else {
            throw ArcPlanValidationError.flowTitleRequired
        }
        guard recurrence != .custom || !recurrenceDays.isEmpty else {
            throw ArcPlanValidationError.customDayRequired
        }
        return ArcPlanInput(
            title: normalizedTitle,
            steps: normalizedSteps,
            isInStudio: isInStudio,
            recurrence: recurrence,
            recurrenceDays: Set(recurrenceDays.filter { (1...7).contains($0) })
        )
    }
}

struct ArcPlan: Identifiable, Equatable, Sendable {
    let id: UUID
    var title: String
    var isInStudio: Bool
    var archived: Bool
    var launchCount: Int
    var lastLaunchedAt: Date?
    var recurrence: ArcPlanRecurrence
    var recurrenceDays: Set<Int>
    var steps: [ArcPlanStep]
    let createdAt: Date
    var updatedAt: Date

    var totalTargetMinutes: Int? {
        let total = steps.compactMap(\.targetMinutesSnapshot).reduce(0, +)
        return total > 0 ? total : nil
    }

    var hasSurge: Bool { steps.contains(where: \.launchWithSurgeSnapshot) }
    var hasSoftFlow: Bool { steps.contains(where: \.isSoftModeSnapshot) }
}

struct ArcPlanStepDraft: Identifiable, Equatable, Sendable {
    let id: UUID
    let sourceFlowPlanID: UUID?
    var title: String
    var journeyName: String
    var targetMinutesText: String
    var launchWithSurge: Bool
    let isSoftMode: Bool
    let linkState: ArcPlanStepLinkState

    nonisolated init(plan: FlowPlan) {
        id = UUID()
        sourceFlowPlanID = plan.id
        title = plan.title
        journeyName = plan.journeyName ?? ""
        targetMinutesText = plan.targetMinutes.map(String.init) ?? ""
        launchWithSurge = plan.launchWithSurge
        isSoftMode = plan.isSoftMode
        linkState = .linked
    }

    nonisolated init(step: ArcPlanStep) {
        id = step.id
        sourceFlowPlanID = step.sourceFlowPlanID
        title = step.titleSnapshot
        journeyName = step.journeyNameSnapshot ?? ""
        targetMinutesText = step.targetMinutesSnapshot.map(String.init) ?? ""
        launchWithSurge = step.launchWithSurgeSnapshot
        isSoftMode = step.isSoftModeSnapshot
        linkState = step.linkState
    }

    nonisolated init(routeStep: SuggestedRouteStep) {
        id = UUID()
        sourceFlowPlanID = nil
        title = routeStep.title
        journeyName = routeStep.journeyName
        targetMinutesText = routeStep.targetMinutes.map(String.init) ?? ""
        launchWithSurge = routeStep.launchWithSurge
        isSoftMode = false
        linkState = .customized
    }

    func validatedInput() throws -> ArcPlanStepInput {
        let minutesText = targetMinutesText.trimmingCharacters(in: .whitespacesAndNewlines)
        let minutes = minutesText.isEmpty ? nil : Int(minutesText)
        if !minutesText.isEmpty, minutes == nil || minutes ?? 0 <= 0 {
            throw ArcPlanValidationError.targetMinutesMustBePositive
        }
        return ArcPlanStepInput(
            sourceFlowPlanID: sourceFlowPlanID,
            titleSnapshot: title,
            journeyNameSnapshot: journeyName,
            isSoftModeSnapshot: isSoftMode,
            targetMinutesSnapshot: minutes,
            launchWithSurgeSnapshot: launchWithSurge,
            linkState: linkState
        )
    }
}

struct ArcPlanDraft: Equatable, Sendable {
    var title = ""
    var steps: [ArcPlanStepDraft] = []
    var isInStudio = false
    var recurrence: ArcPlanRecurrence = .oneTime
    var recurrenceDays: Set<Int> = []

    init() {}

    init(plan: ArcPlan) {
        title = plan.title
        steps = plan.steps.sorted { $0.orderIndex < $1.orderIndex }.map(ArcPlanStepDraft.init)
        isInStudio = plan.isInStudio
        recurrence = plan.recurrence
        recurrenceDays = plan.recurrenceDays
    }

    init(route: SuggestedRoute, isInStudio: Bool = false) {
        title = route.title
        steps = route.steps.map(ArcPlanStepDraft.init)
        self.isInStudio = isInStudio
    }

    func validatedInput() throws -> ArcPlanInput {
        try ArcPlanInput(
            title: title,
            steps: steps.map { try $0.validatedInput() },
            isInStudio: isInStudio,
            recurrence: recurrence,
            recurrenceDays: recurrenceDays
        ).validated()
    }
}

struct ActivePlannedArcRun: Equatable, Sendable {
    static let singletonID = "active-planned-arc"

    let arcPlanID: UUID
    let arcTitle: String
    var currentStepIndex: Int
    let totalSteps: Int
    var currentStepTitle: String
    var currentJourneyName: String?
    var currentIsSoftMode: Bool
    let startedAt: Date
    var updatedAt: Date
}

struct PlannedArcLaunch: Equatable, Sendable {
    let plan: ArcPlan
    let run: ActivePlannedArcRun
    let step: ArcPlanStep
    let runtime: ArcRuntimeState
}

enum PlannedArcCompletionAction: Sendable {
    case unchanged
    case advance
    case complete
}

enum PlannedArcAdvanceResult: Equatable, Sendable {
    case advanced(ActivePlannedArcRun)
    case completed
    case notPlannedArc
}

enum ArcPlanValidationError: LocalizedError, Equatable {
    case titleRequired
    case needsTwoFlows
    case softFlowNotAllowed
    case targetMinutesMustBePositive
    case customDayRequired
    case flowTitleRequired
    case noSteps
    case notFound

    var errorDescription: String? {
        switch self {
        case .titleRequired: "Arc title is required."
        case .needsTwoFlows: "An Arc needs at least two Flows."
        case .softFlowNotAllowed: "Soft Flows cannot be added to Arcs."
        case .targetMinutesMustBePositive: "Target minutes must be greater than 0."
        case .customDayRequired: "Choose at least one day."
        case .flowTitleRequired: "Flow title is required."
        case .noSteps: "This arc has no steps."
        case .notFound: "Arc not found."
        }
    }
}

struct SuggestedRouteStep: Equatable, Sendable {
    let title: String
    let journeyName: String
    let targetMinutes: Int?
    let launchWithSurge: Bool
}

struct SuggestedRoute: Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let subtitle: String
    let category: String
    let approximateMinutes: Int?
    let steps: [SuggestedRouteStep]
}
