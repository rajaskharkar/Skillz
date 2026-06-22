import Foundation

/// Owns startup route decisions so the app entry point and root view stay small.
struct AppLaunchCoordinator {
    // Future: load settings, restore active Flow, process notification/deep-link
    // launch state, trigger foreground movement refresh, and choose the initial route.
    func initialRoute() -> AppRoute {
        .home
    }
}
