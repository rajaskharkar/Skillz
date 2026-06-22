import Foundation

/// Lightweight dependency container for app-level coordinators and future services.
///
/// Keep dependencies explicit and injectable; do not use global singletons.
struct AppDependencyContainer {
    let appLaunchCoordinator: AppLaunchCoordinator

    init(
        appLaunchCoordinator: AppLaunchCoordinator = AppLaunchCoordinator()
    ) {
        self.appLaunchCoordinator = appLaunchCoordinator
    }
}
