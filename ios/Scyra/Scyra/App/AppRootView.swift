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
            onSelectRoute: selectRoute,
            onBackToRoot: selectStoryRoot
        ) {
            content(for: selectedRoute)
        }
    }

    private func selectRoute(_ route: AppRoute) {
        selectedRoute = route
    }

    private func selectStoryRoot() {
        selectedRoute = .story
    }

    @ViewBuilder
    private func content(for route: AppRoute) -> some View {
        switch route {
        case .story:
            StoryPlaceholderView()
        case .flow:
            FlowPlaceholderView()
        case .pulse:
            PulsePlaceholderView()
        case .shell:
            ShellPlaceholderView()
        }
    }
}

#Preview {
    AppRootView(container: AppDependencyContainer())
}
