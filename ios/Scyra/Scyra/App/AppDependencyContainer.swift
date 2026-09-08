import Foundation

/// App-level composition root. Dependencies stay explicit and injectable so feature
/// state can be tested without global singletons.
@MainActor
struct AppDependencyContainer {
    let appLaunchCoordinator: AppLaunchCoordinator
    let repository: any ScyraRepository
    let chronicleFileStore: ChronicleFileStore
    let movementController: MovementController
    let preferences: AppPreferencesModel
    let flowReminderScheduler: any FlowReminderScheduling
    let surgeHaptics: any SurgeHapticsProviding
    let deepLinkCoordinator: FlowDeepLinkCoordinator
    let persistenceStartupError: String?

    var flowRepository: any FlowRepository { repository }

    init(
        appLaunchCoordinator: AppLaunchCoordinator? = nil,
        repository: (any ScyraRepository)? = nil,
        movementController: MovementController? = nil,
        preferences: AppPreferencesModel? = nil,
        flowReminderScheduler: (any FlowReminderScheduling)? = nil,
        surgeHaptics: (any SurgeHapticsProviding)? = nil,
        deepLinkCoordinator: FlowDeepLinkCoordinator? = nil
    ) {
        self.appLaunchCoordinator = appLaunchCoordinator ?? AppLaunchCoordinator()
        self.preferences = preferences ?? AppPreferencesModel()
        self.flowReminderScheduler = flowReminderScheduler ?? UserNotificationFlowReminderScheduler()
        self.surgeHaptics = surgeHaptics ?? SystemSurgeHaptics()
        self.deepLinkCoordinator = deepLinkCoordinator ?? FlowDeepLinkCoordinator()
        if let repository {
            self.repository = repository
            self.chronicleFileStore = repository.chronicleFileStore
            self.movementController = movementController ?? MovementController(
                settings: MovementSettingsStore(),
                dataSource: HealthKitMovementDataSource(),
                repository: repository
            )
            self.persistenceStartupError = nil
            Task { try? await repository.reconcileChronicleStorage() }
            return
        }

        do {
            let container = try ScyraPersistenceFactory.makeContainer()
            let fileStore = ChronicleFileStore()
            let repository = SwiftDataFlowRepository(
                container: container,
                chronicleFileStore: fileStore
            )
            self.repository = repository
            self.chronicleFileStore = fileStore
            self.movementController = movementController ?? MovementController(
                settings: MovementSettingsStore(),
                dataSource: HealthKitMovementDataSource(),
                repository: repository
            )
            self.persistenceStartupError = nil
            Task { try? await repository.reconcileChronicleStorage() }
        } catch {
            // Keep the UI usable and surface the failure instead of crashing. This
            // fallback intentionally does not pretend that durable storage succeeded.
            let repository = InMemoryFlowRepository()
            self.repository = repository
            self.chronicleFileStore = repository.chronicleFileStore
            self.movementController = movementController ?? MovementController(
                settings: MovementSettingsStore(),
                dataSource: HealthKitMovementDataSource(),
                repository: repository
            )
            self.persistenceStartupError = FlowStrings.persistenceFailure
            #if DEBUG
            print("Scyra persistent store startup error: \(error)")
            #endif
        }
    }
}
