import SwiftUI

struct AppRootView: View {
    let container: AppDependencyContainer
    @State private var selectedRoute: AppRoute

    init(container: AppDependencyContainer) {
        self.container = container
        _selectedRoute = State(initialValue: container.appLaunchCoordinator.initialRoute())
    }

    var body: some View {
        RootNavigationShell(
            selectedRoute: selectedRoute,
            onSelectRoute: { selectedRoute = $0 }
        ) {
            routeContent
        }
    }

    @ViewBuilder
    private var routeContent: some View {
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
