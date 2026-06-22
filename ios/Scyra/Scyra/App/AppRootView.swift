import SwiftUI

struct AppRootView: View {
    let container: AppDependencyContainer
    @State private var selectedRoute: AppRoute

    init(container: AppDependencyContainer) {
        self.container = container
        _selectedRoute = State(initialValue: container.appLaunchCoordinator.initialRoute())
    }

    var body: some View {
        // Future: replace placeholder routing with a typed NavigationStack when routes are implemented.
        switch selectedRoute {
        case .home:
            HomePlaceholderView()
        case .flow, .story, .paths, .shell, .notepad, .help:
            PlaceholderRouteView(route: selectedRoute)
        }
    }
}

#Preview {
    AppRootView(container: AppDependencyContainer())
}
