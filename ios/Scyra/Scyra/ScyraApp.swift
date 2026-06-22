import SwiftUI

@main
struct ScyraApp: App {
    private let container = AppDependencyContainer()

    var body: some Scene {
        WindowGroup {
            AppRootView(container: container)
        }
    }
}
