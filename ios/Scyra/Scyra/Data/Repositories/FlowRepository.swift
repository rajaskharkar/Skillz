import Foundation
import SwiftData

@MainActor
protocol FlowRepository: AnyObject {
    func fetchJourneys() throws -> [Journey]
    func fetchActiveFlow() throws -> ActiveFlowSnapshot?
    func saveActiveFlow(_ snapshot: ActiveFlowSnapshot) throws
    func clearActiveFlow() throws
    func fetchActiveArc() throws -> ArcRuntimeState?
    func fetchRecentlyEndedArc() throws -> ArcRuntimeState?
    func saveArcState(active: ArcRuntimeState?, recentlyEnded: ArcRuntimeState?) throws
    func commit(
        session: FlowSession,
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?,
        movement: FlowMovementCompletion?,
        originPulseID: UUID?,
        plannedArcAction: PlannedArcCompletionAction
    ) throws -> FlowSession
    func fetchAllSessions() throws -> [FlowSession]
    func fetchSessions(arcID: UUID) throws -> [FlowSession]
    func deleteSession(id: UUID, detachedAt: Date) throws
    func fetchAllArcMetadata() throws -> [UUID: ArcMetadata]
    func saveArcMetadata(_ metadata: ArcMetadata, updatedAt: Date) throws
    func clearArcMetadata(arcID: UUID) throws
    func fetchMovementSnapshot(sessionID: UUID) throws -> FlowHealthSnapshot?
    func fetchAllMovementSnapshots() throws -> [FlowHealthSnapshot]
    func fetchMovementRewardBreakdown(sessionID: UUID) throws -> FlowRewardBreakdown?
    func saveMovementSnapshot(_ snapshot: FlowHealthSnapshot) throws
    func hasRefreshableMovementSnapshots(now: Date) throws -> Bool
    func fetchRefreshableMovementSnapshots(now: Date) throws -> [FlowHealthSnapshot]
    func markRefreshableMovementSnapshotsDisabled(now: Date) throws
    func applyMovementRefresh(snapshot: FlowHealthSnapshot, breakdown: FlowRewardBreakdown) throws
}

extension FlowRepository {
    func commit(
        session: FlowSession,
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?,
        movement: FlowMovementCompletion?,
        originPulseID: UUID?
    ) throws -> FlowSession {
        try commit(
            session: session,
            activeArc: activeArc,
            recentlyEndedArc: recentlyEndedArc,
            movement: movement,
            originPulseID: originPulseID,
            plannedArcAction: .unchanged
        )
    }

    func commit(
        session: FlowSession,
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?
    ) throws -> FlowSession {
        try commit(
            session: session,
            activeArc: activeArc,
            recentlyEndedArc: recentlyEndedArc,
            movement: nil,
            originPulseID: nil,
            plannedArcAction: .unchanged
        )
    }

    func commit(
        session: FlowSession,
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?,
        movement: FlowMovementCompletion?
    ) throws -> FlowSession {
        try commit(
            session: session,
            activeArc: activeArc,
            recentlyEndedArc: recentlyEndedArc,
            movement: movement,
            originPulseID: nil,
            plannedArcAction: .unchanged
        )
    }
}

@MainActor
final class SwiftDataFlowRepository: ScyraRepository {
    // ModelContext does not own the container. Retain both for the lifetime of
    // the repository; otherwise the first deferred fetch can hit a dead store.
    let container: ModelContainer
    let context: ModelContext
    let chronicleFileStore: ChronicleFileStore

    init(container: ModelContainer, chronicleFileStore: ChronicleFileStore = ChronicleFileStore()) {
        self.container = container
        self.context = container.mainContext
        self.chronicleFileStore = chronicleFileStore
        self.context.autosaveEnabled = false
    }

    func fetchJourneys() throws -> [Journey] {
        let models = try context.fetch(FetchDescriptor<JourneyModel>())
        return models
            .map { model in
                let lastFlowUse = model.flows.map(\.endTime).max()
                let lastPulseUse = model.pulses.map(\.createdAt).max()
                return Journey(
                    id: model.id,
                    name: model.name,
                    createdAt: model.createdAt,
                    lastUsedAt: [lastFlowUse, lastPulseUse].compactMap { $0 }.max()
                )
            }
            .filter { $0.lastUsedAt != nil }
            .sorted { lhs, rhs in
                if lhs.lastUsedAt != rhs.lastUsedAt {
                    return (lhs.lastUsedAt ?? .distantPast) > (rhs.lastUsedAt ?? .distantPast)
                }
                if lhs.createdAt != rhs.createdAt { return lhs.createdAt > rhs.createdAt }
                return lhs.name.localizedCaseInsensitiveCompare(rhs.name) == .orderedAscending
            }
    }

    func fetchActiveFlow() throws -> ActiveFlowSnapshot? {
        try activeFlowModel().map(Self.snapshot(from:))
    }

    func saveActiveFlow(_ snapshot: ActiveFlowSnapshot) throws {
        if let model = try activeFlowModel() {
            model.update(from: snapshot)
        } else {
            context.insert(ActiveFlowModel(snapshot: snapshot))
        }
        try context.save()
    }

    func clearActiveFlow() throws {
        if let model = try activeFlowModel() { context.delete(model) }
        try context.save()
    }

    func fetchActiveArc() throws -> ArcRuntimeState? {
        try arcModel(slot: ArcPersistenceSlot.active).map(Self.arc(from:))
    }

    func fetchRecentlyEndedArc() throws -> ArcRuntimeState? {
        try arcModel(slot: ArcPersistenceSlot.recentlyEnded).map(Self.arc(from:))
    }

    func saveArcState(active: ArcRuntimeState?, recentlyEnded: ArcRuntimeState?) throws {
        do {
            try replaceArc(slot: ArcPersistenceSlot.active, with: active)
            try replaceArc(slot: ArcPersistenceSlot.recentlyEnded, with: recentlyEnded)
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func commit(
        session: FlowSession,
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?,
        movement: FlowMovementCompletion?,
        originPulseID: UUID?,
        plannedArcAction: PlannedArcCompletionAction
    ) throws -> FlowSession {
        do {
            let flowInstanceID = session.flowInstanceID
            let existingDescriptor = FetchDescriptor<FlowSessionModel>(
                predicate: #Predicate { $0.flowInstanceID == flowInstanceID }
            )

            let committed: FlowSession
            let isNewCommit: Bool
            if let existing = try context.fetch(existingDescriptor).first {
                committed = Self.session(from: existing)
                isNewCommit = false
            } else {
                let journey = try journey(named: session.journeyName, createdAt: session.createdAt)
                let model = FlowSessionModel(session: session, journey: journey)
                context.insert(model)
                committed = session
                isNewCommit = true
            }

            if let activeFlow = try activeFlowModel() { context.delete(activeFlow) }
            try promoteChronicle(
                from: .activeFlow(session.flowInstanceID),
                to: .session(committed.id),
                now: session.createdAt
            )
            try attachLivePulses(to: committed)
            if isNewCommit {
                try replaceArc(slot: ArcPersistenceSlot.active, with: activeArc)
                try replaceArc(slot: ArcPersistenceSlot.recentlyEnded, with: recentlyEndedArc)
                _ = try applyPlannedArcCompletionForCommit(plannedArcAction, at: session.createdAt)
                if let originPulseID {
                    try insertPulseFlowLinkForCommit(
                        pulseID: originPulseID,
                        sessionID: committed.id,
                        linkedAt: session.createdAt
                    )
                }
            }
            if isNewCommit, let movement {
                try upsertMovement(movement.rebased(to: committed.id))
            }
            if isNewCommit {
                try insertShellRewardsForCommit(session: committed)
                try processLookoutSessionForCommit(committed)
            }
            try context.save()
            return committed
        } catch {
            context.rollback()
            throw error
        }
    }

    func fetchSessions(arcID: UUID) throws -> [FlowSession] {
        var descriptor = FetchDescriptor<FlowSessionModel>(
            predicate: #Predicate { $0.arcID == arcID }
        )
        descriptor.sortBy = [SortDescriptor(\FlowSessionModel.arcIndex)]
        return try context.fetch(descriptor).map(Self.session(from:))
    }

    func fetchAllSessions() throws -> [FlowSession] {
        var descriptor = FetchDescriptor<FlowSessionModel>()
        descriptor.sortBy = [SortDescriptor(\FlowSessionModel.createdAt, order: .reverse)]
        return try context.fetch(descriptor).map(Self.session(from:))
    }

    func deleteSession(id: UUID, detachedAt: Date) throws {
        guard let session = try flowSessionModel(id: id) else { return }
        let journey = session.journey
        let arcID = session.arcID
        let ownerIdentity = ChronicleOwner.session(id).identity
        let chronicle = try context.fetch(FetchDescriptor<ChronicleModel>())
            .first { $0.ownerIdentity == ownerIdentity }
        let chronicleID = chronicle?.id

        do {
            if let chronicle { context.delete(chronicle) }

            for pulse in try context.fetch(FetchDescriptor<PulseModel>()) where pulse.parentSessionID == id {
                pulse.parentSessionID = nil
                pulse.updatedAt = detachedAt
            }
            try context.fetch(FetchDescriptor<PulseFlowLinkModel>())
                .filter { $0.sessionID == id }
                .forEach(context.delete)
            try context.fetch(FetchDescriptor<FlowHealthSnapshotModel>())
                .filter { $0.sessionID == id }
                .forEach(context.delete)
            try context.fetch(FetchDescriptor<FlowRewardBreakdownModel>())
                .filter { $0.sessionID == id }
                .forEach(context.delete)

            context.delete(session)

            if let arcID {
                let hasAnotherArcFlow = try context.fetch(FetchDescriptor<FlowSessionModel>())
                    .contains { $0.id != id && $0.arcID == arcID }
                if !hasAnotherArcFlow, let metadata = try arcMetadataModel(arcID: arcID) {
                    context.delete(metadata)
                }
            }

            if let journey,
               !journey.flows.contains(where: { $0.id != id }),
               journey.pulses.isEmpty {
                context.delete(journey)
            }

            try context.save()
        } catch {
            context.rollback()
            throw error
        }

        if let chronicleID { chronicleFileStore.deleteChronicle(chronicleID) }
    }

    func fetchAllArcMetadata() throws -> [UUID: ArcMetadata] {
        Dictionary(uniqueKeysWithValues: try context.fetch(FetchDescriptor<ArcMetadataModel>()).map {
            ($0.arcID, Self.arcMetadata(from: $0))
        })
    }

    func saveArcMetadata(_ metadata: ArcMetadata, updatedAt: Date) throws {
        guard !metadata.isEmpty else {
            try clearArcMetadata(arcID: metadata.arcID)
            return
        }
        do {
            if let model = try arcMetadataModel(arcID: metadata.arcID) {
                model.update(from: metadata, at: updatedAt)
            } else {
                context.insert(ArcMetadataModel(metadata: metadata, createdAt: updatedAt, updatedAt: updatedAt))
            }
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func clearArcMetadata(arcID: UUID) throws {
        do {
            if let model = try arcMetadataModel(arcID: arcID) { context.delete(model) }
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func fetchMovementSnapshot(sessionID: UUID) throws -> FlowHealthSnapshot? {
        try movementSnapshotModel(sessionID: sessionID).map(Self.movementSnapshot(from:))
    }

    func fetchAllMovementSnapshots() throws -> [FlowHealthSnapshot] {
        try context.fetch(FetchDescriptor<FlowHealthSnapshotModel>()).map(Self.movementSnapshot(from:))
    }

    func fetchMovementRewardBreakdown(sessionID: UUID) throws -> FlowRewardBreakdown? {
        try movementBreakdownModel(sessionID: sessionID).map(Self.movementBreakdown(from:))
    }

    func saveMovementSnapshot(_ snapshot: FlowHealthSnapshot) throws {
        if let model = try movementSnapshotModel(sessionID: snapshot.sessionID) {
            model.update(from: snapshot)
        } else {
            context.insert(FlowHealthSnapshotModel(snapshot: snapshot))
        }
        try context.save()
    }

    func hasRefreshableMovementSnapshots(now: Date) throws -> Bool {
        try expireMovementSnapshots(now: now)
        return try fetchAllMovementSnapshots().contains { snapshot in
            snapshot.status.isRefreshable
                && snapshot.checkCount < MovementRefreshPolicy.maxCheckCount
                && (snapshot.expiresAt.map { $0 > now } ?? false)
        }
    }

    func fetchRefreshableMovementSnapshots(now: Date) throws -> [FlowHealthSnapshot] {
        try expireMovementSnapshots(now: now)
        return try fetchAllMovementSnapshots()
            .filter { snapshot in
                snapshot.status.isRefreshable
                    && snapshot.checkCount < MovementRefreshPolicy.maxCheckCount
                    && (snapshot.expiresAt.map { $0 > now } ?? false)
                    && (snapshot.lastCheckedAt.map {
                        $0 <= now.addingTimeInterval(-MovementRefreshPolicy.throttle)
                    } ?? true)
            }
            .sorted { ($0.lastCheckedAt ?? .distantPast) < ($1.lastCheckedAt ?? .distantPast) }
            .prefix(MovementRefreshPolicy.batchLimit)
            .map { $0 }
    }

    func markRefreshableMovementSnapshotsDisabled(now: Date) throws {
        for model in try context.fetch(FetchDescriptor<FlowHealthSnapshotModel>()) {
            if (FlowHealthSyncStatus(rawValue: model.statusRawValue) ?? .errorFinal).isRefreshable,
               model.checkCount < MovementRefreshPolicy.maxCheckCount {
                model.statusRawValue = FlowHealthSyncStatus.disabledBeforeCapture.rawValue
                model.lastCheckedAt = now
            }
        }
        try context.save()
    }

    func applyMovementRefresh(snapshot: FlowHealthSnapshot, breakdown: FlowRewardBreakdown) throws {
        do {
            guard let session = try flowSessionModel(id: snapshot.sessionID),
                  let oldBreakdown = try fetchMovementRewardBreakdown(sessionID: snapshot.sessionID) else { return }
            if let model = try movementSnapshotModel(sessionID: snapshot.sessionID) {
                model.update(from: snapshot)
            } else {
                context.insert(FlowHealthSnapshotModel(snapshot: snapshot))
            }
            if let model = try movementBreakdownModel(sessionID: breakdown.sessionID) {
                model.update(from: breakdown)
            } else {
                context.insert(FlowRewardBreakdownModel(breakdown: breakdown))
            }
            session.scyraPoints = Int(breakdown.finalScyraPoints)
            session.arcBonusPoints = Int(breakdown.arcBonusPoints)
            try insertMovementPearlDeltaForRefresh(
                sessionID: snapshot.sessionID,
                oldBreakdown: oldBreakdown,
                newBreakdown: breakdown
            )
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    private func activeFlowModel() throws -> ActiveFlowModel? {
        let id = ActiveFlowSnapshot.singletonID
        var descriptor = FetchDescriptor<ActiveFlowModel>(predicate: #Predicate { $0.id == id })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func flowSessionModel(id: UUID) throws -> FlowSessionModel? {
        var descriptor = FetchDescriptor<FlowSessionModel>(predicate: #Predicate { $0.id == id })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func movementSnapshotModel(sessionID: UUID) throws -> FlowHealthSnapshotModel? {
        var descriptor = FetchDescriptor<FlowHealthSnapshotModel>(
            predicate: #Predicate { $0.sessionID == sessionID }
        )
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func arcMetadataModel(arcID: UUID) throws -> ArcMetadataModel? {
        var descriptor = FetchDescriptor<ArcMetadataModel>(predicate: #Predicate { $0.arcID == arcID })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func movementBreakdownModel(sessionID: UUID) throws -> FlowRewardBreakdownModel? {
        var descriptor = FetchDescriptor<FlowRewardBreakdownModel>(
            predicate: #Predicate { $0.sessionID == sessionID }
        )
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    private func upsertMovement(_ movement: FlowMovementCompletion) throws {
        if let snapshot = try movementSnapshotModel(sessionID: movement.snapshot.sessionID) {
            snapshot.update(from: movement.snapshot)
        } else {
            context.insert(FlowHealthSnapshotModel(snapshot: movement.snapshot))
        }
        if let breakdown = try movementBreakdownModel(sessionID: movement.rewardBreakdown.sessionID) {
            breakdown.update(from: movement.rewardBreakdown)
        } else {
            context.insert(FlowRewardBreakdownModel(breakdown: movement.rewardBreakdown))
        }
    }

    private func expireMovementSnapshots(now: Date) throws {
        var changed = false
        for model in try context.fetch(FetchDescriptor<FlowHealthSnapshotModel>()) {
            let status = FlowHealthSyncStatus(rawValue: model.statusRawValue) ?? .errorFinal
            if status.isRefreshable, let expiresAt = model.expiresAt, expiresAt <= now {
                model.statusRawValue = FlowHealthSyncStatus.expired.rawValue
                changed = true
            }
        }
        if changed { try context.save() }
    }

    private func arcModel(slot: String) throws -> ArcStateModel? {
        // There are exactly two named Arc slots. Filtering this bounded set in
        // memory avoids a SwiftData predicate runtime trap seen on iOS 26.
        try context.fetch(FetchDescriptor<ArcStateModel>()).first { $0.slot == slot }
    }

    private func replaceArc(slot: String, with state: ArcRuntimeState?) throws {
        if let model = try arcModel(slot: slot) {
            if let state { model.update(from: state) } else { context.delete(model) }
        } else if let state {
            context.insert(ArcStateModel(slot: slot, state: state))
        }
    }

    func journey(named name: String, createdAt: Date) throws -> JourneyModel {
        let descriptor = FetchDescriptor<JourneyModel>(predicate: #Predicate { $0.name == name })
        if let existing = try context.fetch(descriptor).first { return existing }
        let journey = JourneyModel(name: name, createdAt: createdAt)
        context.insert(journey)
        return journey
    }

    private static func snapshot(from model: ActiveFlowModel) -> ActiveFlowSnapshot {
        ActiveFlowSnapshot(
            flowInstanceID: model.flowInstanceID,
            title: model.title,
            journeyName: model.journeyName,
            mode: FlowMode(rawValue: model.modeRawValue) ?? .flow,
            isInFlowMode: model.isInFlowMode,
            isRunning: model.isRunning,
            accumulatedDurationMs: model.accumulatedDurationMs,
            segmentStartedAt: model.segmentStartedAt,
            firstStartedAt: model.firstStartedAt,
            surgePlannedMs: model.surgePlannedMs,
            healthEnabledAtStart: model.healthEnabledAtStart,
            healthAccessRequestedAtStart: model.healthAccessRequestedAtStart,
            movementBonusEligibleAtStart: model.movementBonusEligibleAtStart,
            originPulseID: model.originPulseID,
            originPulseTitle: model.originPulseTitleSnapshot,
            originPulseJourneyName: model.originPulseJourneyNameSnapshot,
            activeIntervals: model.activeIntervalsData.flatMap {
                try? JSONDecoder().decode([FlowActiveInterval].self, from: $0)
            } ?? [],
            createdAt: model.createdAt
        )
    }

    private static func arc(from model: ArcStateModel) -> ArcRuntimeState {
        ArcRuntimeState(
            id: model.arcID,
            isPending: model.isPending,
            multiplier: model.multiplier,
            progressMs: model.progressMs,
            lastSessionEndTime: model.lastSessionEndTime,
            sessionCount: model.sessionCount,
            pauseUsedMs: model.pauseUsedMs,
            pauseStartedAt: model.pauseStartedAt
        )
    }

    private static func arcMetadata(from model: ArcMetadataModel) -> ArcMetadata {
        ArcMetadata(
            arcID: model.arcID,
            title: model.title,
            summary: model.summary,
            outcome: model.outcome,
            highlight: model.highlight,
            nextStep: model.nextStep
        )
    }

    private static func session(from model: FlowSessionModel) -> FlowSession {
        FlowSession(
            id: model.id,
            flowInstanceID: model.flowInstanceID,
            title: model.title,
            description: model.flowDescription,
            journeyName: model.journey?.name ?? model.journeyNameSnapshot,
            startTime: model.startTime,
            endTime: model.endTime,
            durationMs: model.durationMs,
            surgePlannedMs: model.surgePlannedMs,
            surgePoints: model.surgePoints,
            scyraPoints: model.scyraPoints,
            isSoftMode: model.isSoftMode,
            arcID: model.arcID,
            arcIndex: model.arcIndex,
            arcMultiplierUsed: model.arcMultiplierUsed,
            arcBonusPoints: model.arcBonusPoints,
            createdAt: model.createdAt
        )
    }

    private static func movementSnapshot(from model: FlowHealthSnapshotModel) -> FlowHealthSnapshot {
        FlowHealthSnapshot(
            sessionID: model.sessionID,
            healthEnabledAtStart: model.healthEnabledAtStart,
            accessRequestedAtStart: model.accessRequestedAtStart,
            status: FlowHealthSyncStatus(rawValue: model.statusRawValue) ?? .errorFinal,
            steps: model.steps,
            rawMovementPoints: model.rawMovementPoints,
            finalMovementScyraContribution: model.finalMovementScyraContribution,
            finalMovementPearlContribution: model.finalMovementPearlContribution,
            firstCheckedAt: model.firstCheckedAt,
            lastCheckedAt: model.lastCheckedAt,
            capturedAt: model.capturedAt,
            expiresAt: model.expiresAt,
            checkCount: model.checkCount,
            flowStartTime: model.flowStartTime,
            flowEndTime: model.flowEndTime,
            activeIntervals: model.activeIntervalsData.flatMap {
                try? JSONDecoder().decode([FlowActiveInterval].self, from: $0)
            } ?? [],
            sourceLabel: model.sourceLabel,
            updatedAfterSync: model.updatedAfterSync
        )
    }

    private static func movementBreakdown(from model: FlowRewardBreakdownModel) -> FlowRewardBreakdown {
        FlowRewardBreakdown(
            sessionID: model.sessionID,
            nonMovementPreMultiplierPoints: model.nonMovementPreMultiplierPoints,
            pulseBonusPoints: model.pulseBonusPoints,
            surgeBonusPoints: model.surgeBonusPoints,
            otherPreMultiplierBonusPoints: model.otherPreMultiplierBonusPoints,
            movementPoints: model.movementPoints,
            preMultiplierTotal: model.preMultiplierTotal,
            arcMultiplier: model.arcMultiplier,
            streakMultiplier: model.streakMultiplier,
            otherMultiplier: model.otherMultiplier,
            arcBonusPoints: model.arcBonusPoints,
            finalScyraPoints: model.finalScyraPoints,
            pearlsEarned: model.pearlsEarned,
            pearlEligible: model.pearlEligible
        )
    }
}

enum MovementRefreshPolicy {
    static let refreshWindow: TimeInterval = 72 * 60 * 60
    static let throttle: TimeInterval = 30 * 60
    static let maxCheckCount = 10
    static let batchLimit = 8
}

@MainActor
final class InMemoryFlowRepository: ScyraRepository {
    let chronicleFileStore: ChronicleFileStore
    var activeFlow: ActiveFlowSnapshot?
    var activeArc: ArcRuntimeState?
    var recentlyEndedArc: ArcRuntimeState?
    var sessions: [FlowSession] = []
    var arcMetadataByID: [UUID: ArcMetadata] = [:]
    var chronicles: [ChronicleOwner: ChronicleSnapshot] = [:]
    var finalizedChronicleOwners: Set<ChronicleOwner> = []
    var pulses: [Pulse] = []
    var pulseCreationReceipts: [UUID: UUID] = [:]
    var pulseDraft: PulseDraft?
    var pulseFlowLinks: [PulseFlowLink] = []
    var movementSnapshots: [UUID: FlowHealthSnapshot] = [:]
    var movementBreakdowns: [UUID: FlowRewardBreakdown] = [:]
    var pearlLedger: [PearlLedgerEntry] = []
    var stillwaterLedger: [StillwaterLedgerEntry] = []
    var shellRewardEvents: [ShellRewardEvent] = []
    var shellFindGrants: [ShellFindGrant] = []
    var shellBadges: [String: ShellBadge] = [:]
    var shellFindInstances: [ShellFindInstance] = []
    var shellFindStacks: [String: ShellFindStack] = [:]
    var shellPlacements: [ShellPlacement] = []
    var shellFindUpgrades: [ShellFindUpgrade] = []
    var shellBadgePins: [String: Int] = [:]
    var shellBadgeCountFloors: [String: Int] = [:]
    var shellCollectionBackfills: [Int: (Int, Int)] = [:]
    var stillwaterPerspective: StillwaterPerspective = .overview
    var shellCreatureStatuses: [String: CreatureStatus] = [:]
    var creatureDiscoveries: [String: CreatureDiscoveryEvidence] = [:]
    var creatureMasteries: [CreatureMasteryEvidence] = []
    var collectionCompletions: [CollectionCompletionEvidence] = []
    var trackedAchievements: Set<String> = []
    var masteryCelebrations: [MasteryCelebration] = []
    var creatureActionReceipts: [String: CreatureGrowthResult] = [:]
    var lookoutObjectives: [LookoutObjective] = []
    var lookoutCompletions: [LookoutCompletion] = []
    var lookoutSkippedCycles: [ObjectiveSkippedCycle] = []
    var lookoutProcessedSessionIDs: Set<UUID> = []
    var flowPlans: [FlowPlan] = []
    var arcPlans: [ArcPlan] = []
    var activePlannedArcRun: ActivePlannedArcRun?

    init(chronicleFileStore: ChronicleFileStore = ChronicleFileStore(baseURL: ChronicleFileStore.temporaryBaseURL())) {
        self.chronicleFileStore = chronicleFileStore
    }

    func fetchJourneys() -> [Journey] {
        let flowUses = sessions.map { ($0.journeyName, $0.createdAt, $0.endTime) }
        let pulseUses = pulses.compactMap { pulse -> (String, Date, Date)? in
            pulse.journeyName.map { ($0, pulse.createdAt, pulse.createdAt) }
        }
        return Dictionary(grouping: flowUses + pulseUses, by: \.0)
            .map { name, uses in
                Journey(
                    id: UUID(),
                    name: name,
                    createdAt: uses.map(\.1).min() ?? .now,
                    lastUsedAt: uses.map(\.2).max()
                )
            }
            .sorted { ($0.lastUsedAt ?? .distantPast) > ($1.lastUsedAt ?? .distantPast) }
    }

    func fetchActiveFlow() -> ActiveFlowSnapshot? { activeFlow }
    func saveActiveFlow(_ snapshot: ActiveFlowSnapshot) { activeFlow = snapshot }
    func clearActiveFlow() { activeFlow = nil }
    func fetchActiveArc() -> ArcRuntimeState? { activeArc }
    func fetchRecentlyEndedArc() -> ArcRuntimeState? { recentlyEndedArc }

    func saveArcState(active: ArcRuntimeState?, recentlyEnded: ArcRuntimeState?) {
        activeArc = active
        recentlyEndedArc = recentlyEnded
    }

    func commit(
        session: FlowSession,
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?,
        movement: FlowMovementCompletion?,
        originPulseID: UUID?,
        plannedArcAction: PlannedArcCompletionAction
    ) -> FlowSession {
        let existing = sessions.first { $0.flowInstanceID == session.flowInstanceID }
        let committed = existing ?? session
        if existing == nil {
            sessions.append(session)
        }
        promoteChronicleInMemory(
            from: .activeFlow(session.flowInstanceID),
            to: .session(committed.id)
        )
        for index in pulses.indices where pulses[index].parentFlowInstanceID == session.flowInstanceID {
            pulses[index].parentSessionID = committed.id
            pulses[index].parentFlowInstanceID = nil
            if let arcID = committed.arcID { pulses[index].arcID = arcID }
            pulses[index].updatedAt = committed.createdAt
        }
        activeFlow = nil
        if existing == nil {
            self.activeArc = activeArc
            self.recentlyEndedArc = recentlyEndedArc
            _ = applyPlannedArcCompletionForCommit(plannedArcAction, at: session.createdAt)
        }
        if existing == nil, let movement {
            let committedMovement = movement.rebased(to: committed.id)
            movementSnapshots[committed.id] = committedMovement.snapshot
            movementBreakdowns[committed.id] = committedMovement.rewardBreakdown
        }
        if existing == nil, let originPulseID {
            linkCompletedFlow(pulseID: originPulseID, sessionID: committed.id, linkedAt: session.createdAt)
        }
        if existing == nil {
            insertShellRewardsForCommitInMemory(session: committed)
            _ = processLookoutSessionForCommitInMemory(committed)
        }
        return committed
    }

    func commit(
        session: FlowSession,
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?,
        movement: FlowMovementCompletion?,
        originPulseID: UUID?
    ) -> FlowSession {
        commit(
            session: session,
            activeArc: activeArc,
            recentlyEndedArc: recentlyEndedArc,
            movement: movement,
            originPulseID: originPulseID,
            plannedArcAction: .unchanged
        )
    }

    func commit(
        session: FlowSession,
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?
    ) -> FlowSession {
        commit(
            session: session,
            activeArc: activeArc,
            recentlyEndedArc: recentlyEndedArc,
            movement: nil,
            originPulseID: nil,
            plannedArcAction: .unchanged
        )
    }

    func commit(
        session: FlowSession,
        activeArc: ArcRuntimeState?,
        recentlyEndedArc: ArcRuntimeState?,
        movement: FlowMovementCompletion?
    ) -> FlowSession {
        commit(
            session: session,
            activeArc: activeArc,
            recentlyEndedArc: recentlyEndedArc,
            movement: movement,
            originPulseID: nil,
            plannedArcAction: .unchanged
        )
    }

    func fetchSessions(arcID: UUID) -> [FlowSession] {
        sessions.filter { $0.arcID == arcID }.sorted { ($0.arcIndex ?? 0) < ($1.arcIndex ?? 0) }
    }

    func fetchAllSessions() -> [FlowSession] {
        sessions.sorted { $0.createdAt > $1.createdAt }
    }

    func deleteSession(id: UUID, detachedAt: Date) {
        guard let session = sessions.first(where: { $0.id == id }) else { return }
        sessions.removeAll { $0.id == id }
        pulses = pulses.map { pulse in
            guard pulse.parentSessionID == id else { return pulse }
            var detached = pulse
            detached.parentSessionID = nil
            detached.updatedAt = detachedAt
            return detached
        }
        pulseFlowLinks.removeAll { $0.sessionID == id }
        movementSnapshots.removeValue(forKey: id)
        movementBreakdowns.removeValue(forKey: id)
        if let chronicleID = chronicles.removeValue(forKey: .session(id))?.chronicleID {
            chronicleFileStore.deleteChronicle(chronicleID)
        }
        if let arcID = session.arcID, !sessions.contains(where: { $0.arcID == arcID }) {
            arcMetadataByID.removeValue(forKey: arcID)
        }
    }

    func fetchAllArcMetadata() -> [UUID: ArcMetadata] { arcMetadataByID }

    func saveArcMetadata(_ metadata: ArcMetadata, updatedAt: Date) {
        if metadata.isEmpty { arcMetadataByID.removeValue(forKey: metadata.arcID) }
        else { arcMetadataByID[metadata.arcID] = metadata }
    }

    func clearArcMetadata(arcID: UUID) {
        arcMetadataByID.removeValue(forKey: arcID)
    }

    func fetchMovementSnapshot(sessionID: UUID) -> FlowHealthSnapshot? { movementSnapshots[sessionID] }
    func fetchAllMovementSnapshots() -> [FlowHealthSnapshot] { Array(movementSnapshots.values) }
    func fetchMovementRewardBreakdown(sessionID: UUID) -> FlowRewardBreakdown? { movementBreakdowns[sessionID] }
    func saveMovementSnapshot(_ snapshot: FlowHealthSnapshot) { movementSnapshots[snapshot.sessionID] = snapshot }

    func hasRefreshableMovementSnapshots(now: Date) -> Bool {
        expireMovementSnapshots(now: now)
        return movementSnapshots.values.contains {
            $0.status.isRefreshable
                && $0.checkCount < MovementRefreshPolicy.maxCheckCount
                && ($0.expiresAt.map { $0 > now } ?? false)
        }
    }

    func fetchRefreshableMovementSnapshots(now: Date) -> [FlowHealthSnapshot] {
        expireMovementSnapshots(now: now)
        return movementSnapshots.values
            .filter {
                $0.status.isRefreshable
                    && $0.checkCount < MovementRefreshPolicy.maxCheckCount
                    && ($0.expiresAt.map { $0 > now } ?? false)
                    && ($0.lastCheckedAt.map {
                        $0 <= now.addingTimeInterval(-MovementRefreshPolicy.throttle)
                    } ?? true)
            }
            .sorted { ($0.lastCheckedAt ?? .distantPast) < ($1.lastCheckedAt ?? .distantPast) }
            .prefix(MovementRefreshPolicy.batchLimit)
            .map { $0 }
    }

    func markRefreshableMovementSnapshotsDisabled(now: Date) {
        for (id, var snapshot) in movementSnapshots where snapshot.status.isRefreshable {
            snapshot.status = .disabledBeforeCapture
            snapshot.lastCheckedAt = now
            movementSnapshots[id] = snapshot
        }
    }

    func applyMovementRefresh(snapshot: FlowHealthSnapshot, breakdown: FlowRewardBreakdown) {
        guard let oldBreakdown = movementBreakdowns[breakdown.sessionID] else { return }
        movementSnapshots[snapshot.sessionID] = snapshot
        movementBreakdowns[breakdown.sessionID] = breakdown
        guard let index = sessions.firstIndex(where: { $0.id == snapshot.sessionID }) else { return }
        sessions[index] = sessions[index].updatingRewards(
            scyraPoints: Int(breakdown.finalScyraPoints),
            arcBonusPoints: Int(breakdown.arcBonusPoints)
        )
        insertMovementPearlDeltaInMemory(
            sessionID: snapshot.sessionID,
            oldBreakdown: oldBreakdown,
            newBreakdown: breakdown
        )
    }

    private func expireMovementSnapshots(now: Date) {
        for (id, var snapshot) in movementSnapshots {
            if snapshot.status.isRefreshable, let expiresAt = snapshot.expiresAt, expiresAt <= now {
                snapshot.status = .expired
                movementSnapshots[id] = snapshot
            }
        }
    }
}
