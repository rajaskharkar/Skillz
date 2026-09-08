import Combine
import Foundation
import HealthKit

@MainActor
protocol MovementDataSource: AnyObject {
    var isHealthDataAvailable: Bool { get }
    func requestAccess() async throws
    func readSteps(between start: Date, and end: Date) async -> MovementReadResult
}

@MainActor
final class HealthKitMovementDataSource: MovementDataSource {
    private let store: HKHealthStore
    private let stepType = HKQuantityType(.stepCount)

    init(store: HKHealthStore = HKHealthStore()) {
        self.store = store
    }

    var isHealthDataAvailable: Bool { HKHealthStore.isHealthDataAvailable() }

    func requestAccess() async throws {
        guard isHealthDataAvailable else { throw MovementServiceError.unavailable }
        try await store.requestAuthorization(toShare: [], read: [stepType])
    }

    func readSteps(between start: Date, and end: Date) async -> MovementReadResult {
        guard end > start else { return .noData }
        guard isHealthDataAvailable else { return .unavailable }
        let predicate = HKQuery.predicateForSamples(
            withStart: start,
            end: end,
            options: [.strictStartDate, .strictEndDate]
        )
        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(
                quantityType: stepType,
                quantitySamplePredicate: predicate,
                options: .cumulativeSum
            ) { _, statistics, error in
                if let healthError = error as? HKError,
                   healthError.code == .errorAuthorizationDenied {
                    continuation.resume(returning: .permissionMissing)
                } else if error != nil {
                    continuation.resume(returning: .failure)
                } else if let sum = statistics?.sumQuantity() {
                    let steps = Int64(max(0, sum.doubleValue(for: .count())))
                    continuation.resume(returning: .success(steps: steps))
                } else {
                    continuation.resume(returning: .noData)
                }
            }
            store.execute(query)
        }
    }
}

@MainActor
final class UnavailableMovementDataSource: MovementDataSource {
    var isHealthDataAvailable: Bool { false }
    func requestAccess() async throws { throw MovementServiceError.unavailable }
    func readSteps(between start: Date, and end: Date) async -> MovementReadResult { .unavailable }
}

enum MovementServiceError: Error {
    case unavailable
    case persistence
}

protocol MovementSettingsPersisting: AnyObject {
    var isEnabled: Bool { get set }
    var accessWasRequested: Bool { get set }
}

final class MovementSettingsStore: MovementSettingsPersisting {
    private enum Key {
        static let enabled = "movement_bonus_enabled"
        static let accessRequested = "movement_health_access_requested"
    }

    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    var isEnabled: Bool {
        get { defaults.bool(forKey: Key.enabled) }
        set { defaults.set(newValue, forKey: Key.enabled) }
    }

    var accessWasRequested: Bool {
        get { defaults.bool(forKey: Key.accessRequested) }
        set { defaults.set(newValue, forKey: Key.accessRequested) }
    }
}

final class InMemoryMovementSettingsStore: MovementSettingsPersisting {
    var isEnabled: Bool
    var accessWasRequested: Bool

    init(isEnabled: Bool = false, accessWasRequested: Bool = false) {
        self.isEnabled = isEnabled
        self.accessWasRequested = accessWasRequested
    }
}

@MainActor
final class MovementController: ObservableObject {
    @Published private(set) var isEnabled: Bool
    @Published private(set) var accessWasRequested: Bool
    @Published private(set) var isBusy = false
    @Published private(set) var errorMessage: String?
    @Published var showDisableWarning = false

    private let settings: any MovementSettingsPersisting
    private let dataSource: any MovementDataSource
    private let repository: any ScyraRepository
    private let now: () -> Date
    private var isRefreshing = false

    init(
        settings: any MovementSettingsPersisting,
        dataSource: any MovementDataSource,
        repository: any ScyraRepository,
        now: @escaping () -> Date = Date.init
    ) {
        self.settings = settings
        self.dataSource = dataSource
        self.repository = repository
        self.now = now
        self.isEnabled = settings.isEnabled
        self.accessWasRequested = settings.accessWasRequested
    }

    var isAvailable: Bool { dataSource.isHealthDataAvailable }

    static func disabled(repository: any ScyraRepository) -> MovementController {
        MovementController(
            settings: InMemoryMovementSettingsStore(),
            dataSource: UnavailableMovementDataSource(),
            repository: repository
        )
    }

    func enable() async {
        guard !isBusy else { return }
        guard isAvailable else {
            errorMessage = MovementStrings.healthUnavailable
            return
        }
        isBusy = true
        defer { isBusy = false }
        do {
            try await dataSource.requestAccess()
            settings.accessWasRequested = true
            settings.isEnabled = true
            accessWasRequested = true
            isEnabled = true
            errorMessage = nil
            await refreshForeground()
        } catch {
            errorMessage = MovementStrings.permissionNotGranted
        }
    }

    func requestDisable() {
        do {
            if try repository.hasRefreshableMovementSnapshots(now: now()) {
                showDisableWarning = true
            } else {
                disableImmediately()
            }
        } catch {
            errorMessage = MovementStrings.persistenceFailure
        }
    }

    func keepEnabled() {
        showDisableWarning = false
    }

    func disableAnyway() {
        do {
            try repository.markRefreshableMovementSnapshotsDisabled(now: now())
            disableImmediately()
        } catch {
            errorMessage = MovementStrings.persistenceFailure
        }
    }

    func eligibility(isSoftFlow: Bool) -> Bool {
        MovementBonusEligibilityPolicy.isEligible(MovementBonusEligibilityInput(
            movementBonusEnabled: isEnabled,
            healthDataAvailable: isAvailable,
            healthAccessRequested: accessWasRequested,
            isRegularPointEligibleFlow: !isSoftFlow,
            isSoftFlow: isSoftFlow
        ))
    }

    func readForCompletion(
        eligible: Bool,
        isSoftFlow: Bool,
        intervals: [FlowActiveInterval]
    ) async -> CompletionMovementRead {
        guard eligible, !isSoftFlow, !intervals.isEmpty else { return .notEligible }
        let checkedAt = now()
        switch await readSteps(across: intervals) {
        case .success(let steps):
            let points = MovementBonusCalculator.points(for: steps)
            return CompletionMovementRead(
                steps: steps,
                movementPoints: points,
                status: points > 0 ? .captured : .noReward,
                checkedAt: checkedAt
            )
        case .noData:
            return CompletionMovementRead(steps: nil, movementPoints: 0, status: .pending, checkedAt: checkedAt)
        case .permissionMissing:
            return CompletionMovementRead(steps: nil, movementPoints: 0, status: .permissionRevoked, checkedAt: checkedAt)
        case .unavailable, .failure:
            return CompletionMovementRead(steps: nil, movementPoints: 0, status: .errorRetryable, checkedAt: checkedAt)
        }
    }

    func refreshForeground() async {
        guard isEnabled, accessWasRequested, isAvailable, !isRefreshing else { return }
        isRefreshing = true
        defer { isRefreshing = false }
        do {
            let refreshable = try repository.fetchRefreshableMovementSnapshots(now: now())
            for snapshot in refreshable {
                await refresh(snapshot)
            }
            errorMessage = nil
        } catch {
            errorMessage = MovementStrings.persistenceFailure
        }
    }

    private func refresh(_ snapshot: FlowHealthSnapshot) async {
        let intervals = snapshot.activeIntervals.isEmpty
            ? [FlowActiveInterval(start: snapshot.flowStartTime, end: snapshot.flowEndTime)]
            : snapshot.activeIntervals
        let checkedAt = now()
        switch await readSteps(across: intervals) {
        case .success(let steps):
            apply(steps: steps, to: snapshot, checkedAt: checkedAt)
        case .noData:
            update(snapshot, status: .pending, checkedAt: checkedAt)
        case .permissionMissing:
            update(snapshot, status: .permissionRevoked, checkedAt: checkedAt)
        case .unavailable, .failure:
            update(snapshot, status: .errorRetryable, checkedAt: checkedAt)
        }
    }

    private func readSteps(across intervals: [FlowActiveInterval]) async -> MovementReadResult {
        await MovementStepAggregator.read(intervals: intervals) { [dataSource] interval in
            await dataSource.readSteps(between: interval.start, and: interval.end)
        }
    }

    private func apply(steps: Int64, to snapshot: FlowHealthSnapshot, checkedAt: Date) {
        do {
            guard let current = try repository.fetchMovementRewardBreakdown(sessionID: snapshot.sessionID) else {
                return
            }
            let result = DelayedMovementRewardPolicy.calculate(
                steps: steps,
                context: StoredMovementRewardContext(
                    sessionID: snapshot.sessionID,
                    nonMovementPreMultiplierPoints: current.nonMovementPreMultiplierPoints,
                    pulseBonusPoints: current.pulseBonusPoints,
                    surgeBonusPoints: current.surgeBonusPoints,
                    otherPreMultiplierBonusPoints: current.otherPreMultiplierBonusPoints,
                    existingMovementPoints: current.movementPoints,
                    oldFinalScyraPoints: current.finalScyraPoints,
                    arcMultiplier: current.arcMultiplier,
                    streakMultiplier: current.streakMultiplier,
                    otherMultiplier: current.otherMultiplier,
                    pearlEligible: current.pearlEligible
                )
            )
            var updatedSnapshot = snapshot
            updatedSnapshot.status = result.newRawMovementPoints > 0 ? .captured : .noReward
            updatedSnapshot.steps = max(snapshot.steps ?? 0, steps)
            updatedSnapshot.rawMovementPoints = result.newRawMovementPoints
            updatedSnapshot.finalMovementScyraContribution = max(
                0,
                result.newFinalScyraPoints - current.nonMovementPreMultiplierPoints
            )
            updatedSnapshot.finalMovementPearlContribution = current.pearlEligible ? result.pearlsEarned : 0
            updatedSnapshot.firstCheckedAt = snapshot.firstCheckedAt ?? checkedAt
            updatedSnapshot.lastCheckedAt = checkedAt
            updatedSnapshot.capturedAt = result.newRawMovementPoints > 0 ? checkedAt : snapshot.capturedAt
            updatedSnapshot.checkCount += 1
            updatedSnapshot.updatedAfterSync = result.deltaScyraPoints > 0 || snapshot.updatedAfterSync

            let updatedBreakdown = FlowRewardBreakdown(
                sessionID: current.sessionID,
                nonMovementPreMultiplierPoints: current.nonMovementPreMultiplierPoints,
                pulseBonusPoints: current.pulseBonusPoints,
                surgeBonusPoints: current.surgeBonusPoints,
                otherPreMultiplierBonusPoints: current.otherPreMultiplierBonusPoints,
                movementPoints: result.newRawMovementPoints,
                preMultiplierTotal: result.newPreMultiplierTotal,
                arcMultiplier: current.arcMultiplier,
                streakMultiplier: current.streakMultiplier,
                otherMultiplier: current.otherMultiplier,
                arcBonusPoints: result.newArcBonusPoints,
                finalScyraPoints: result.newFinalScyraPoints,
                pearlsEarned: result.pearlsEarned,
                pearlEligible: current.pearlEligible
            )
            try repository.applyMovementRefresh(snapshot: updatedSnapshot, breakdown: updatedBreakdown)
        } catch {
            errorMessage = MovementStrings.persistenceFailure
        }
    }

    private func update(_ snapshot: FlowHealthSnapshot, status: FlowHealthSyncStatus, checkedAt: Date) {
        do {
            var updated = snapshot
            updated.status = status
            updated.firstCheckedAt = snapshot.firstCheckedAt ?? checkedAt
            updated.lastCheckedAt = checkedAt
            updated.checkCount += 1
            try repository.saveMovementSnapshot(updated)
        } catch {
            errorMessage = MovementStrings.persistenceFailure
        }
    }

    private func disableImmediately() {
        settings.isEnabled = false
        isEnabled = false
        showDisableWarning = false
        errorMessage = nil
    }
}
