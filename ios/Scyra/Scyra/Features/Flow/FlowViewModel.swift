import Combine
import Foundation

@MainActor
final class FlowViewModel: ObservableObject {
    @Published private(set) var title = ""
    @Published private(set) var journeyName = ""
    @Published private(set) var mode: FlowMode = .flow
    @Published private(set) var isInFlowMode = false
    @Published private(set) var isRunning = false
    @Published private(set) var elapsedMs: Int64 = 0
    @Published private(set) var surgePlannedMs: Int64?
    @Published private(set) var activeArc: ArcRuntimeState?
    @Published private(set) var journeys: [Journey] = []
    @Published private(set) var reward: FlowReward?
    @Published private(set) var isSaving = false
    @Published private(set) var errorMessage: String?
    @Published private(set) var recentlyResumedArcMessage: String?
    @Published private(set) var awaitingNextFlow = false
    @Published private(set) var chronicle: ChronicleSnapshot
    @Published private(set) var isImportingChronicleMedia = false
    @Published private(set) var movementBonusEligibleAtStart = false
    @Published private(set) var originPulseID: UUID?
    @Published private(set) var originPulseTitle: String?
    @Published private(set) var originPulseJourneyName: String?
    @Published private(set) var pendingIdeaContinuation: PendingIdeaContinuation?
    @Published private(set) var plannedArcTitle: String?
    @Published private(set) var plannedArcStepIndex: Int?
    @Published private(set) var plannedArcTotalSteps: Int?

    private let repository: any ScyraRepository
    private let movementController: MovementController
    private let reminderScheduler: any FlowReminderScheduling
    private let surgeHaptics: any SurgeHapticsProviding
    private let now: () -> Date
    private var recentlyEndedArc: ArcRuntimeState?
    private var flowInstanceID: UUID
    private var accumulatedDurationMs: Int64 = 0
    private var segmentStartedAt: Date?
    private var firstStartedAt: Date?
    private var draftCreatedAt: Date
    private var healthEnabledAtStart = false
    private var healthAccessRequestedAtStart = false
    private var activeIntervals: [FlowActiveInterval] = []
    private var surgeRuntime: SurgeRuntimeState?
    private var activePlannedArcRun: ActivePlannedArcRun?

    init(
        repository: any ScyraRepository,
        movementController: MovementController? = nil,
        reminderScheduler: (any FlowReminderScheduling)? = nil,
        surgeHaptics: (any SurgeHapticsProviding)? = nil,
        now: @escaping () -> Date = Date.init,
        startupError: String? = nil
    ) {
        let initialFlowInstanceID = UUID()
        self.repository = repository
        self.movementController = movementController ?? MovementController.disabled(repository: repository)
        self.reminderScheduler = reminderScheduler ?? NoOpFlowReminderScheduler()
        self.surgeHaptics = surgeHaptics ?? NoOpSurgeHaptics()
        self.now = now
        self.flowInstanceID = initialFlowInstanceID
        self.chronicle = .empty(owner: .activeFlow(initialFlowInstanceID))
        self.draftCreatedAt = now()
        self.errorMessage = startupError
        restore()
    }

    var isModeLocked: Bool { elapsedMs > 0 }
    var isSurgeLocked: Bool { elapsedMs > 0 }
    var isSoftMode: Bool { mode == .soft }
    var arcNextIndex: Int? { activeArc.map { $0.sessionCount + 1 } }
    var completionControls: FlowCompletionControls {
        .resolve(isSoftMode: isSoftMode, isArcLinked: activeArc != nil)
    }
    var canContinueArc: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !journeyName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !isSaving
    }
    var canComplete: Bool {
        canContinueArc && elapsedMs > 0 && !isInFlowMode
    }
    var surgePlannedMinutes: Int? {
        surgePlannedMs.map { Int($0 / ScoreCalculator.millisPerMinute) }
    }
    var chronicleOwner: ChronicleOwner { .activeFlow(flowInstanceID) }
    var activePulseContext: (flowInstanceID: UUID, arcID: UUID?)? {
        isInFlowMode ? (flowInstanceID, activeArc?.id) : nil
    }
    var hasMeaningfulActiveFlow: Bool {
        isRunning || isInFlowMode || elapsedMs > 0 || segmentStartedAt != nil || firstStartedAt != nil
    }

    func arcGraceRemainingMs(at date: Date? = nil) -> Int64? {
        guard let activeArc, !isRunning, elapsedMs == 0 else { return nil }
        guard activeArc.sessionCount > 0 else { return ArcRules.graceWindowMs }
        let elapsed = Int64((date ?? now()).timeIntervalSince(activeArc.lastSessionEndTime) * 1_000)
        return max(0, ArcRules.graceWindowMs - elapsed)
    }

    func arcPauseRemainingMs(at date: Date? = nil) -> Int64? {
        guard let activeArc,
              !isRunning,
              elapsedMs > 0,
              let pauseStartedAt = activeArc.pauseStartedAt else { return nil }
        let activePause = max(0, Int64((date ?? now()).timeIntervalSince(pauseStartedAt) * 1_000))
        return max(0, ArcRules.pauseBudget(for: activeArc) - activeArc.pauseUsedMs - activePause)
    }

    func updateTitle(_ value: String) {
        guard !isSaving else { return }
        title = String(value.prefix(60))
        persistDraft()
    }

    func updateJourneyName(_ value: String) {
        guard !isSaving else { return }
        journeyName = value
        persistDraft()
    }

    func selectJourney(_ journey: Journey) {
        updateJourneyName(journey.name)
    }

    func setMode(_ newMode: FlowMode) {
        guard !isSaving, !isModeLocked else { return }
        mode = newMode
        if newMode == .soft {
            surgePlannedMs = nil
            surgeRuntime = nil
            surgeHaptics.cancel()
            activeArc = activeArc?.resettingMultiplierForSoftFlow()
            persistArcs()
        }
        persistDraft()
    }

    func setSurge(minutes: Int) {
        guard !isSaving, mode == .flow, !isSurgeLocked, !isInFlowMode else { return }
        surgePlannedMs = Int64(max(1, minutes)) * ScoreCalculator.millisPerMinute
        surgeRuntime = surgePlannedMs.map { SurgeRuntimeState(plannedMs: $0) }
        surgeHaptics.play(.armed)
        persistDraft()
    }

    func clearSurge() {
        guard !isSaving, !isSurgeLocked, !isInFlowMode else { return }
        surgePlannedMs = nil
        surgeRuntime = nil
        surgeHaptics.cancel()
        persistDraft()
    }

    func toggleFlowMode() {
        guard !isSaving else { return }
        isInFlowMode ? exitFlowMode() : enterFlowMode()
    }

    func enterFlowMode() {
        guard !isSaving else { return }
        if !isRunning, !startOrResumeTimer() { return }
        isInFlowMode = true
        persistDraft()
        activateReminder(requestAuthorization: true)
    }

    func exitFlowMode() {
        guard !isSaving else { return }
        if isRunning { pauseTimer() }
        isInFlowMode = false
        persistDraft()
        deactivatePlatformSession()
    }

    func resetTimer() {
        guard !isSaving, !isRunning else { return }
        accumulatedDurationMs = 0
        segmentStartedAt = nil
        firstStartedAt = nil
        activeIntervals = []
        elapsedMs = 0
        surgeRuntime = surgePlannedMs.map { SurgeRuntimeState(plannedMs: $0) }
        surgeHaptics.cancel()
        persistDraft()
    }

    func refreshElapsed(at date: Date? = nil) {
        guard !isSaving else { return }
        let current = date ?? now()
        if isRunning, let segmentStartedAt {
            let segmentMs = max(0, Int64(current.timeIntervalSince(segmentStartedAt) * 1_000))
            elapsedMs = max(0, accumulatedDurationMs + segmentMs)
            evaluateSurgeHaptics()
            return
        }

        guard let activeArc else { return }
        if elapsedMs == 0,
           activeArc.sessionCount > 0,
           !ArcContinuationResolver.isWithinContinuationWindow(activeArc, flowStartTime: current) {
            concludeExpiredArc()
        } else if elapsedMs > 0, arcPauseRemainingMs(at: current) == 0 {
            concludeExpiredArc()
        }
    }

    func resumeFromBackground() {
        refreshElapsed()
        if let surgeRuntime {
            self.surgeRuntime = SurgeRuntimeEvaluator.silentCatchUp(
                runtime: surgeRuntime,
                elapsedMs: elapsedMs
            )
        }
        if isInFlowMode, isRunning { activateReminder(requestAuthorization: false) }
    }

    func discardDraftIfIdle() {
        let isPulseOriginDraft = originPulseID != nil
        guard elapsedMs == 0,
              !isRunning,
              !isInFlowMode,
              activeArc == nil || isPulseOriginDraft else { return }
        do {
            try repository.discardChronicle(owner: chronicleOwner)
            try repository.clearActiveFlow()
            startFreshDraft(keepingJourney: false)
        } catch {
            showPersistenceError(error)
        }
    }

    @discardableResult
    func prepareFromPulse(_ context: PulseLaunchContext) -> Bool {
        guard !hasMeaningfulActiveFlow, !isSaving else { return false }
        do {
            try repository.discardChronicle(owner: chronicleOwner)
            try repository.clearActiveFlow()
            startFreshDraft(keepingJourney: false)
            title = String(context.title.prefix(60))
            journeyName = context.journeyName ?? ""
            originPulseID = context.pulseID
            originPulseTitle = context.title
            originPulseJourneyName = context.journeyName
            try repository.saveActiveFlow(activeFlowSnapshot(at: now()))
            return true
        } catch {
            showPersistenceError(error)
            return false
        }
    }

    /// Prefills a new Flow from a Lookout objective without weakening the
    /// canonical active-Flow conflict rule used by every other launch origin.
    @discardableResult
    func prepareForJourney(_ journey: String) -> Bool {
        guard !hasMeaningfulActiveFlow, !isSaving else { return false }
        do {
            try repository.discardChronicle(owner: chronicleOwner)
            try repository.clearActiveFlow()
            startFreshDraft(keepingJourney: false)
            journeyName = journey
            try repository.saveActiveFlow(activeFlowSnapshot(at: now()))
            return true
        } catch {
            showPersistenceError(error)
            return false
        }
    }

    /// Replaces an idle draft with a durable planned-Flow prefill. A running,
    /// paused, or otherwise meaningful Flow always wins and must be surfaced to
    /// the user instead of being overwritten by the plan launch.
    @discardableResult
    func prepareFromPlan(_ plan: FlowPlan) -> Bool {
        guard !hasMeaningfulActiveFlow, !isSaving else { return false }
        do {
            try repository.discardChronicle(owner: chronicleOwner)
            try repository.clearActiveFlow()
            startFreshDraft(keepingJourney: false)
            title = String(plan.title.prefix(60))
            journeyName = plan.journeyName ?? ""
            mode = plan.isSoftMode ? .soft : .flow
            if !plan.isSoftMode,
               plan.launchWithSurge,
               let targetMinutes = plan.targetMinutes,
               targetMinutes > 0 {
                surgePlannedMs = Int64(targetMinutes) * ScoreCalculator.millisPerMinute
                surgeRuntime = surgePlannedMs.map { SurgeRuntimeState(plannedMs: $0) }
            }
            try repository.saveActiveFlow(activeFlowSnapshot(at: now()))
            return true
        } catch {
            showPersistenceError(error)
            return false
        }
    }

    /// Begins or resumes an Android-canonical planned Arc. The active run and
    /// pre-created Arc runtime are durable before its first Flow is presented.
    @discardableResult
    func prepareFromArcPlan(_ plan: ArcPlan, restart: Bool = false) -> Bool {
        guard !hasMeaningfulActiveFlow, !isSaving else { return false }
        do {
            try repository.discardChronicle(owner: chronicleOwner)
            try repository.clearActiveFlow()
            startFreshDraft(keepingJourney: false)
            let launch = try repository.beginPlannedArc(id: plan.id, restart: restart, at: now())
            activeArc = launch.runtime
            recentlyEndedArc = nil
            activePlannedArcRun = launch.run
            applyPlannedStep(launch.step, run: launch.run)
            try repository.saveActiveFlow(activeFlowSnapshot(at: now()))
            return true
        } catch {
            showPersistenceError(error)
            return false
        }
    }

    func complete(_ action: FlowEndAction) {
        guard !isSaving else { return }
        refreshElapsed()

        guard chronicle.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            errorMessage = ChronicleStrings.unfinishedMoment
            return
        }

        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedJourney = journeyName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedTitle.isEmpty, !trimmedJourney.isEmpty else {
            errorMessage = FlowStrings.titleAndJourneyRequired
            return
        }
        guard action == .continueArc || elapsedMs > 0 else {
            errorMessage = FlowStrings.startTimerBeforeSaving
            return
        }

        isSaving = true
        errorMessage = nil
        let endTime = now()
        let duration = max(0, elapsedMs)
        let intervals = currentActiveIntervals(at: endTime)
        let startTime = intervals.first?.start
            ?? firstStartedAt
            ?? endTime.addingTimeInterval(-Double(duration) / 1_000)

        if movementBonusEligibleAtStart, !isSoftMode, !intervals.isEmpty {
            Task { [weak self] in
                guard let self else { return }
                let movementRead = await movementController.readForCompletion(
                    eligible: movementBonusEligibleAtStart,
                    isSoftFlow: isSoftMode,
                    intervals: intervals
                )
                finishCompletion(
                    action,
                    title: trimmedTitle,
                    journey: trimmedJourney,
                    endTime: endTime,
                    startTime: startTime,
                    duration: duration,
                    intervals: intervals,
                    movementRead: movementRead
                )
            }
        } else {
            finishCompletion(
                action,
                title: trimmedTitle,
                journey: trimmedJourney,
                endTime: endTime,
                startTime: startTime,
                duration: duration,
                intervals: intervals,
                movementRead: .notEligible
            )
        }
    }

    private func finishCompletion(
        _ action: FlowEndAction,
        title: String,
        journey: String,
        endTime: Date,
        startTime: Date,
        duration: Int64,
        intervals: [FlowActiveInterval],
        movementRead: CompletionMovementRead
    ) {
        let breakdown = ScoreCalculator.breakdown(durationMs: duration)
        let baseScyra = isSoftMode ? 0 : breakdown.totalPoints
        let surgePoints = isSoftMode
            ? 0
            : ScoreCalculator.surgePoints(plannedMs: surgePlannedMs, actualDurationMs: duration)
        let beforeArc = baseScyra + Int(movementRead.movementPoints)

        var sessionArcID: UUID?
        var arcIndex: Int?
        var arcMultiplierUsed: Double?
        var arcBonusPoints = 0
        var finalScyra = beforeArc
        var finalWithoutMovement = baseScyra
        var nextArc = activeArc
        var arcDidLevelUp = false

        if nextArc == nil, action == .continueArc, !isSoftMode {
            let newArcID = UUID()
            sessionArcID = newArcID
            arcIndex = 1
            arcMultiplierUsed = ArcRuntimeState.baseMultiplier
            nextArc = ArcRuntimeState(
                id: newArcID,
                isPending: true,
                multiplier: ArcRules.startMultiplier,
                progressMs: 0,
                lastSessionEndTime: endTime,
                sessionCount: 1,
                pauseUsedMs: 0,
                pauseStartedAt: nil
            )
        } else if var arc = nextArc {
            sessionArcID = arc.id
            arcIndex = arc.sessionCount + 1

            if isSoftMode || arc.sessionCount == 0 {
                arcMultiplierUsed = ArcRuntimeState.baseMultiplier
                arc.multiplier = ArcRuntimeState.baseMultiplier
            } else {
                let withoutMovement = ScoreCalculator.arcMath(
                    beforeArcPoints: baseScyra,
                    chainBase: arc.multiplier,
                    durationMs: duration
                )
                let math = ScoreCalculator.arcMath(
                    beforeArcPoints: beforeArc,
                    chainBase: arc.multiplier,
                    durationMs: duration
                )
                arcMultiplierUsed = math.arcMultiplierUsed
                arcBonusPoints = math.arcBonusPoints
                finalScyra = math.finalPoints
                finalWithoutMovement = withoutMovement.finalPoints
                arc.multiplier = math.nextChainBase
                arcDidLevelUp = math.didLevelUp
            }

            if isSoftMode {
                arc = arc.afterCompletedSoftFlow(at: endTime)
            } else {
                arc.sessionCount += 1
                arc.isPending = arc.sessionCount < 2
                arc.progressMs = 0
                arc.lastSessionEndTime = endTime
                arc.pauseStartedAt = nil
            }
            nextArc = arc
        }

        let resolvedAction: FlowEndAction = isSoftMode && activeArc == nil ? .saveFlow : action
        let plannedArcAction: PlannedArcCompletionAction = if activePlannedArcRun != nil {
            switch resolvedAction {
            case .continueArc: .advance
            case .completeArc, .saveFlow: .complete
            }
        } else {
            .unchanged
        }
        let persistedActiveArc: ArcRuntimeState?
        let persistedRecentlyEndedArc: ArcRuntimeState?
        switch resolvedAction {
        case .continueArc:
            persistedActiveArc = nextArc
            persistedRecentlyEndedArc = nil
        case .completeArc:
            persistedActiveArc = nil
            persistedRecentlyEndedArc = nextArc
        case .saveFlow:
            if !isSoftMode, let nextArc {
                persistedActiveArc = nil
                persistedRecentlyEndedArc = nextArc
            } else {
                persistedActiveArc = nil
                persistedRecentlyEndedArc = nil
            }
        }

        let session = FlowSession(
            id: UUID(),
            flowInstanceID: flowInstanceID,
            title: title,
            description: "",
            journeyName: journey,
            startTime: startTime,
            endTime: endTime,
            durationMs: duration,
            surgePlannedMs: surgePlannedMs,
            surgePoints: surgePoints,
            scyraPoints: finalScyra,
            isSoftMode: isSoftMode,
            arcID: sessionArcID,
            arcIndex: arcIndex,
            arcMultiplierUsed: arcMultiplierUsed,
            arcBonusPoints: arcBonusPoints,
            createdAt: endTime
        )

        let movementCompletion: FlowMovementCompletion? = {
            guard healthEnabledAtStart || movementBonusEligibleAtStart else { return nil }
            let snapshot = FlowHealthSnapshot(
                sessionID: session.id,
                healthEnabledAtStart: healthEnabledAtStart,
                accessRequestedAtStart: healthAccessRequestedAtStart,
                status: movementBonusEligibleAtStart ? movementRead.status : .notEligible,
                steps: movementRead.steps,
                rawMovementPoints: movementRead.movementPoints,
                finalMovementScyraContribution: Int64(max(0, finalScyra - finalWithoutMovement)),
                finalMovementPearlContribution: isSoftMode ? 0 : Int64(max(0, finalScyra - finalWithoutMovement)),
                firstCheckedAt: movementRead.checkedAt,
                lastCheckedAt: movementRead.checkedAt,
                capturedAt: movementRead.movementPoints > 0 ? endTime : nil,
                expiresAt: endTime.addingTimeInterval(MovementRefreshPolicy.refreshWindow),
                checkCount: movementRead.checkedAt == nil ? 0 : 1,
                flowStartTime: startTime,
                flowEndTime: endTime,
                activeIntervals: intervals,
                sourceLabel: "Apple Health",
                updatedAfterSync: false
            )
            let rewardBreakdown = FlowRewardBreakdown(
                sessionID: session.id,
                nonMovementPreMultiplierPoints: Int64(baseScyra),
                pulseBonusPoints: 0,
                surgeBonusPoints: 0,
                otherPreMultiplierBonusPoints: 0,
                movementPoints: movementRead.movementPoints,
                preMultiplierTotal: Int64(beforeArc),
                arcMultiplier: arcMultiplierUsed ?? 1,
                streakMultiplier: 1,
                otherMultiplier: 1,
                arcBonusPoints: Int64(arcBonusPoints),
                finalScyraPoints: Int64(finalScyra),
                pearlsEarned: isSoftMode ? 0 : Int64(finalScyra),
                pearlEligible: !isSoftMode
            )
            return FlowMovementCompletion(snapshot: snapshot, rewardBreakdown: rewardBreakdown)
        }()

        do {
            let committed = try repository.commit(
                session: session,
                activeArc: persistedActiveArc,
                recentlyEndedArc: persistedRecentlyEndedArc,
                movement: movementCompletion,
                originPulseID: originPulseID,
                plannedArcAction: plannedArcAction
            )
            activeArc = persistedActiveArc
            recentlyEndedArc = persistedRecentlyEndedArc
            activePlannedArcRun = try repository.fetchActivePlannedArcRun()
            let shellReward = (try? repository.fetchSessionShellReward(sessionID: committed.id)) ?? .empty
            let summary = resolvedAction == .completeArc
                ? try? buildArcSummary(arcID: committed.arcID)
                : nil
            reward = FlowReward(
                id: committed.id,
                minutes: breakdown.minutes,
                baseScyraPoints: baseScyra,
                tenMinuteBonuses: breakdown.tenMinuteBonuses,
                thirtyMinuteBonuses: breakdown.thirtyMinuteBonuses,
                sixtyMinuteBonuses: breakdown.sixtyMinuteBonuses,
                finalScyraPoints: finalScyra,
                surgePoints: surgePoints,
                movementSteps: movementRead.steps,
                movementPoints: movementRead.movementPoints,
                arcIndex: arcIndex,
                arcMultiplierUsed: arcMultiplierUsed,
                arcBonusPoints: arcBonusPoints,
                arcNextMultiplier: persistedActiveArc?.multiplier,
                arcDidLevelUp: arcDidLevelUp,
                isSoftSession: isSoftMode,
                arcSummary: summary,
                shellReward: shellReward
            )
            if !isSoftMode, let planned = surgePlannedMs {
                surgeHaptics.play(duration <= planned ? .completedSuccess : .completedFailure)
            }
            awaitingNextFlow = resolvedAction == .continueArc
            deactivatePlatformSession(cancelHaptics: false)
            clearTimerState()
            if let refreshedJourneys = try? repository.fetchJourneys() {
                journeys = refreshedJourneys
            }
        } catch {
            showPersistenceError(error)
        }
        isSaving = false
    }

    /// Returns true when the terminal reward should navigate back to Story.
    func finishReward() -> Bool {
        let completedReward = reward
        reward = nil
        // Android presents expiry/pause-limit Arc summaries over the current
        // Flow screen. Dismissing one must preserve any paused Flow draft.
        if completedReward?.isArcOnlySummary == true { return false }
        if awaitingNextFlow {
            prepareNextFlowAfterContinuation(promptForIdea: true)
            return false
        }
        // Android destroys the completed Flow destination on terminal exit.
        // This view model is app-scoped, so reset its in-memory draft explicitly
        // without creating a new durable idle draft.
        startFreshDraft(keepingJourney: false)
        return true
    }

    /// Mirrors Android's reward-dialog Shell entry. Terminal rewards are
    /// consumed before routing, while Continue Arc prepares and persists the
    /// next Flow without carrying the optional originating Idea.
    @discardableResult
    func enterShellFromReward() -> Bool {
        guard let completedReward = reward,
              completedReward.hasShellReward,
              !completedReward.isArcOnlySummary else { return false }
        reward = nil
        if awaitingNextFlow {
            prepareNextFlowAfterContinuation(promptForIdea: false)
        } else {
            startFreshDraft(keepingJourney: false)
        }
        return true
    }

    func resolveIdeaContinuation(includeIdea: Bool) {
        guard let pendingIdeaContinuation else { return }
        if includeIdea {
            originPulseID = pendingIdeaContinuation.pulseID
            originPulseTitle = pendingIdeaContinuation.title
            originPulseJourneyName = pendingIdeaContinuation.journeyName
        }
        self.pendingIdeaContinuation = nil
        persistDraft()
    }

    func consumeRecentlyResumedArcMessage() {
        recentlyResumedArcMessage = nil
    }

    private func prepareNextFlowAfterContinuation(promptForIdea: Bool) {
        awaitingNextFlow = false
        let continuation = promptForIdea ? originPulseID.map {
            PendingIdeaContinuation(
                pulseID: $0,
                title: originPulseTitle ?? title,
                journeyName: originPulseJourneyName ?? (journeyName.isEmpty ? nil : journeyName)
            )
        } : nil
        startFreshDraft(keepingJourney: true)
        if let run = activePlannedArcRun,
           let plan = try? repository.fetchArcPlan(id: run.arcPlanID),
           let nextStep = plan.steps.first(where: { $0.orderIndex == run.currentStepIndex }) {
            applyPlannedStep(nextStep, run: run)
        }
        pendingIdeaContinuation = continuation
        persistDraft()
    }

    func updateChronicleDraft(_ text: String) {
        performChronicleMutation {
            try repository.setChronicleDraft(owner: chronicleOwner, text: text)
        }
    }

    @discardableResult
    func addChronicleText() -> Bool {
        guard !chronicle.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return false
        }
        return performChronicleMutation {
            try repository.addChronicleText(owner: chronicleOwner, text: chronicle.draftText)
        }
    }

    func discardChronicleDraftText() {
        performChronicleMutation {
            try repository.setChronicleDraft(owner: chronicleOwner, text: "")
        }
    }

    func updateChronicleMoment(id: UUID, text: String) {
        performChronicleMutation {
            try repository.updateChronicleText(owner: chronicleOwner, momentID: id, text: text)
        }
    }

    func importChronicleMedia(_ sources: [ChronicleMediaSource]) async -> ChronicleMediaImportResult? {
        guard !isSaving, !isImportingChronicleMedia else { return nil }
        isImportingChronicleMedia = true
        defer { isImportingChronicleMedia = false }
        do {
            let result = try await repository.importChronicleMedia(owner: chronicleOwner, sources: sources)
            chronicle = try repository.fetchChronicle(owner: chronicleOwner)
            errorMessage = nil
            return result
        } catch {
            showPersistenceError(error)
            return nil
        }
    }

    func stageChronicleMedia(_ sources: [ChronicleMediaSource]) async -> ChronicleMediaStageResult? {
        guard !isSaving, !isImportingChronicleMedia else { return nil }
        isImportingChronicleMedia = true
        defer { isImportingChronicleMedia = false }
        do {
            let result = try await repository.stageChronicleMedia(owner: chronicleOwner, sources: sources)
            errorMessage = nil
            return result
        } catch {
            showPersistenceError(error)
            return nil
        }
    }

    func replaceChronicleMedia(momentID: UUID, items: [ChronicleMediaItem]) -> Bool {
        performChronicleMutation {
            try repository.replaceChronicleMedia(owner: chronicleOwner, momentID: momentID, items: items)
        }
    }

    func addChronicleVoice(_ staged: ChronicleStagedVoice) async -> Bool {
        guard !isSaving else { return false }
        do {
            chronicle = try await repository.addChronicleVoice(owner: chronicleOwner, staged: staged)
            errorMessage = nil
            return true
        } catch {
            showPersistenceError(error)
            return false
        }
    }

    func updateChronicleTranscript(id: UUID, transcript: String?, manuallyEdited: Bool) -> Bool {
        performChronicleMutation {
            try repository.updateChronicleTranscript(
                owner: chronicleOwner,
                momentID: id,
                transcript: transcript,
                manuallyEdited: manuallyEdited
            )
        }
    }

    func deleteChronicleMoment(id: UUID) {
        performChronicleMutation {
            try repository.deleteChronicleMoment(owner: chronicleOwner, momentID: id)
        }
    }

    func moveChronicleMoment(id: UUID, offset: Int) {
        let ids = chronicle.moments.map(\.id)
        guard let source = ids.firstIndex(of: id) else { return }
        let destination = source + offset
        guard ids.indices.contains(destination) else { return }
        var reordered = ids
        reordered.swapAt(source, destination)
        performChronicleMutation {
            try repository.reorderChronicleMoments(owner: chronicleOwner, orderedIDs: reordered)
        }
    }

    private func restore() {
        do {
            activeArc = try repository.fetchActiveArc()
            recentlyEndedArc = try repository.fetchRecentlyEndedArc()
            activePlannedArcRun = try repository.fetchActivePlannedArcRun()
            journeys = try repository.fetchJourneys()

            guard let snapshot = try repository.fetchActiveFlow() else {
                if let run = activePlannedArcRun,
                   let plan = try repository.fetchArcPlan(id: run.arcPlanID),
                   let step = plan.steps.first(where: { $0.orderIndex == run.currentStepIndex }) {
                    applyPlannedStep(step, run: run)
                    try repository.saveActiveFlow(activeFlowSnapshot(at: now()))
                }
                chronicle = try repository.fetchChronicle(owner: chronicleOwner)
                return
            }
            if snapshot.isAbandonedPulseOriginDraft {
                try repository.discardChronicle(owner: .activeFlow(snapshot.flowInstanceID))
                try repository.clearActiveFlow()
                chronicle = try repository.fetchChronicle(owner: chronicleOwner)
                return
            }
            flowInstanceID = snapshot.flowInstanceID
            title = snapshot.title
            journeyName = snapshot.journeyName
            mode = snapshot.mode
            isInFlowMode = snapshot.isInFlowMode
            isRunning = snapshot.isRunning
            accumulatedDurationMs = snapshot.accumulatedDurationMs
            segmentStartedAt = snapshot.segmentStartedAt
            firstStartedAt = snapshot.firstStartedAt
            surgePlannedMs = snapshot.surgePlannedMs
            healthEnabledAtStart = snapshot.healthEnabledAtStart
            healthAccessRequestedAtStart = snapshot.healthAccessRequestedAtStart
            movementBonusEligibleAtStart = snapshot.movementBonusEligibleAtStart
            originPulseID = snapshot.originPulseID
            originPulseTitle = snapshot.originPulseTitle
            originPulseJourneyName = snapshot.originPulseJourneyName
            syncPlannedArcUI()
            activeIntervals = snapshot.activeIntervals
            draftCreatedAt = snapshot.createdAt
            elapsedMs = snapshot.elapsedMs(at: now())
            if let planned = surgePlannedMs {
                surgeRuntime = SurgeRuntimeEvaluator.silentCatchUp(
                    runtime: SurgeRuntimeState(plannedMs: planned),
                    elapsedMs: elapsedMs
                )
            }
            chronicle = try repository.fetchChronicle(owner: chronicleOwner)
            refreshElapsed()
            if isInFlowMode, isRunning { activateReminder(requestAuthorization: false) }
        } catch {
            showPersistenceError(error)
        }
    }

    @discardableResult
    private func startOrResumeTimer() -> Bool {
        guard !isRunning else { return true }
        let start = now()
        let isFirstStart = accumulatedDurationMs == 0 && firstStartedAt == nil

        if isFirstStart {
            let resolved = ArcContinuationResolver.resolve(
                activeArc: activeArc,
                recentlyEndedArc: recentlyEndedArc,
                flowStartTime: start
            )
            let resumedRecentlyEndedArc = activeArc == nil && resolved != nil
            activeArc = isSoftMode ? resolved?.resettingMultiplierForSoftFlow() : resolved
            recentlyEndedArc = nil
            if resumedRecentlyEndedArc {
                recentlyResumedArcMessage = FlowStrings.arcResumed
            }
            persistArcs()
            firstStartedAt = start
            healthEnabledAtStart = movementController.isEnabled
            healthAccessRequestedAtStart = movementController.accessWasRequested
            movementBonusEligibleAtStart = movementController.eligibility(isSoftFlow: isSoftMode)
            activeIntervals = []
        }

        guard accountForArcPause(at: start) else { return false }

        segmentStartedAt = start
        isRunning = true
        if isFirstStart, let planned = surgePlannedMs {
            surgeRuntime = SurgeRuntimeEvaluator.silentCatchUp(
                runtime: surgeRuntime ?? SurgeRuntimeState(plannedMs: planned),
                elapsedMs: 0
            )
            surgeHaptics.play(.started)
        }
        persistDraft()
        return true
    }

    @discardableResult
    private func accountForArcPause(at start: Date) -> Bool {
        if var arc = activeArc, let pauseStartedAt = arc.pauseStartedAt {
            let pausedMs = max(0, Int64(start.timeIntervalSince(pauseStartedAt) * 1_000))
            arc.pauseUsedMs += pausedMs
            if arc.pauseUsedMs >= ArcRules.pauseBudget(for: arc) {
                return concludeExpiredArc()
            } else {
                arc.pauseStartedAt = nil
                activeArc = arc
            }
            persistArcs()
        }
        return true
    }

    private func pauseTimer() {
        guard isRunning else { return }
        let pauseTime = now()
        if let segmentStartedAt {
            accumulatedDurationMs += max(0, Int64(pauseTime.timeIntervalSince(segmentStartedAt) * 1_000))
            if pauseTime > segmentStartedAt {
                activeIntervals.append(FlowActiveInterval(start: segmentStartedAt, end: pauseTime))
                activeIntervals = FlowActiveIntervalNormalizer.normalize(activeIntervals)
            }
        }
        self.segmentStartedAt = nil
        elapsedMs = accumulatedDurationMs
        isRunning = false

        if var arc = activeArc, arc.pauseStartedAt == nil {
            arc.pauseStartedAt = pauseTime
            activeArc = arc
            persistArcs()
        }
        persistDraft()
    }

    private func persistDraft() {
        do {
            try repository.saveActiveFlow(activeFlowSnapshot(at: now()))
        } catch {
            showPersistenceError(error)
        }
    }

    private func evaluateSurgeHaptics() {
        guard isRunning, mode == .flow, let planned = surgePlannedMs else { return }
        let current = surgeRuntime ?? SurgeRuntimeState(plannedMs: planned)
        let evaluated = SurgeRuntimeEvaluator.evaluate(runtime: current, elapsedMs: elapsedMs)
        surgeRuntime = evaluated.runtime
        evaluated.events.forEach(surgeHaptics.play)
    }

    private func activateReminder(requestAuthorization: Bool) {
        let context = FlowReminderContext(
            flowInstanceID: flowInstanceID,
            title: title,
            journeyName: journeyName,
            elapsedMs: elapsedMs
        )
        Task {
            if requestAuthorization {
                await reminderScheduler.activate(context)
            } else {
                await reminderScheduler.restore(context)
            }
        }
    }

    private func deactivatePlatformSession(cancelHaptics: Bool = true) {
        if cancelHaptics { surgeHaptics.cancel() }
        Task { await reminderScheduler.deactivate() }
    }

    private func activeFlowSnapshot(at date: Date) -> ActiveFlowSnapshot {
        ActiveFlowSnapshot(
            flowInstanceID: flowInstanceID,
            title: title,
            journeyName: journeyName,
            mode: mode,
            isInFlowMode: isInFlowMode,
            isRunning: isRunning,
            accumulatedDurationMs: accumulatedDurationMs,
            segmentStartedAt: segmentStartedAt,
            firstStartedAt: firstStartedAt,
            surgePlannedMs: surgePlannedMs,
            healthEnabledAtStart: healthEnabledAtStart,
            healthAccessRequestedAtStart: healthAccessRequestedAtStart,
            movementBonusEligibleAtStart: movementBonusEligibleAtStart,
            originPulseID: originPulseID,
            originPulseTitle: originPulseTitle,
            originPulseJourneyName: originPulseJourneyName,
            activeIntervals: currentActiveIntervals(at: date),
            createdAt: draftCreatedAt
        )
    }

    private func persistArcs() {
        do {
            try repository.saveArcState(active: activeArc, recentlyEnded: recentlyEndedArc)
        } catch {
            showPersistenceError(error)
        }
    }

    @discardableResult
    private func concludeExpiredArc() -> Bool {
        guard let arc = activeArc else { return true }
        do {
            let summary = try ArcConclusionPolicy.conclude(
                arcID: arc.id,
                fetchSessions: { try repository.fetchSessions(arcID: arc.id) },
                fetchShellSummary: { try repository.fetchArcShellRewardSummary(arcID: arc.id) },
                persistConclusion: {
                    try repository.saveArcState(active: nil, recentlyEnded: recentlyEndedArc)
                }
            )
            activeArc = nil
            if let summary {
                reward = .arcOnly(arcID: arc.id, summary: summary)
            }
            errorMessage = nil
            return true
        } catch {
            // Keep the in-memory Arc linked so the next foreground/timer tick
            // can safely retry after a transient persistence failure.
            showPersistenceError(error)
            return false
        }
    }

    private func clearTimerState() {
        accumulatedDurationMs = 0
        segmentStartedAt = nil
        firstStartedAt = nil
        activeIntervals = []
        healthEnabledAtStart = false
        healthAccessRequestedAtStart = false
        movementBonusEligibleAtStart = false
        elapsedMs = 0
        isRunning = false
        isInFlowMode = false
        surgeRuntime = nil
    }

    private func currentActiveIntervals(at date: Date) -> [FlowActiveInterval] {
        var intervals = activeIntervals
        if isRunning, let segmentStartedAt, date > segmentStartedAt {
            intervals.append(FlowActiveInterval(start: segmentStartedAt, end: date))
        }
        return FlowActiveIntervalNormalizer.normalize(intervals)
    }

    private func startFreshDraft(keepingJourney: Bool) {
        let retainedJourney = keepingJourney ? journeyName : ""
        flowInstanceID = UUID()
        chronicle = .empty(owner: .activeFlow(flowInstanceID))
        title = ""
        journeyName = retainedJourney
        mode = .flow
        surgePlannedMs = nil
        surgeRuntime = nil
        originPulseID = nil
        originPulseTitle = nil
        originPulseJourneyName = nil
        plannedArcTitle = nil
        plannedArcStepIndex = nil
        plannedArcTotalSteps = nil
        draftCreatedAt = now()
        clearTimerState()
    }

    private func applyPlannedStep(_ step: ArcPlanStep, run: ActivePlannedArcRun) {
        title = String(step.titleSnapshot.prefix(60))
        journeyName = step.journeyNameSnapshot ?? ""
        mode = step.isSoftModeSnapshot ? .soft : .flow
        let plannedSurge = !step.isSoftModeSnapshot
            && step.launchWithSurgeSnapshot
            && (step.targetMinutesSnapshot ?? 0) > 0
        surgePlannedMs = plannedSurge
            ? Int64(step.targetMinutesSnapshot ?? 0) * ScoreCalculator.millisPerMinute
            : nil
        surgeRuntime = surgePlannedMs.map { SurgeRuntimeState(plannedMs: $0) }
        plannedArcTitle = run.arcTitle
        plannedArcStepIndex = run.currentStepIndex
        plannedArcTotalSteps = run.totalSteps
    }

    private func syncPlannedArcUI() {
        plannedArcTitle = activePlannedArcRun?.arcTitle
        plannedArcStepIndex = activePlannedArcRun?.currentStepIndex
        plannedArcTotalSteps = activePlannedArcRun?.totalSteps
    }

    private func buildArcSummary(arcID: UUID?) throws -> ArcSummary? {
        guard let arcID else { return nil }
        let sessions = try repository.fetchSessions(arcID: arcID)
        guard !sessions.isEmpty else { return nil }
        return ArcSummary(
            totalSessions: sessions.count,
            totalDurationMs: sessions.reduce(0) { $0 + $1.durationMs },
            totalFinalPoints: sessions.reduce(0) { $0 + $1.scyraPoints },
            totalArcBonusPoints: sessions.reduce(0) { $0 + $1.arcBonusPoints },
            peakMultiplier: sessions.compactMap(\.arcMultiplierUsed).max() ?? 1,
            shellSummary: try repository.fetchArcShellRewardSummary(arcID: arcID)
        )
    }

    private func showPersistenceError(_ error: Error) {
        errorMessage = FlowStrings.persistenceFailure
        #if DEBUG
        print("Scyra Flow persistence error: \(error)")
        #endif
    }

    @discardableResult
    private func performChronicleMutation(
        _ mutation: () throws -> ChronicleSnapshot
    ) -> Bool {
        guard !isSaving else { return false }
        do {
            chronicle = try mutation()
            errorMessage = nil
            return true
        } catch {
            errorMessage = ChronicleStrings.saveError
            #if DEBUG
            print("Scyra Chronicle persistence error: \(error)")
            #endif
            return false
        }
    }
}
