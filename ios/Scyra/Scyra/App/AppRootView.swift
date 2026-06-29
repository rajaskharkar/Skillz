import SwiftUI

struct AppRootView: View {
    let container: AppDependencyContainer
    @StateObject private var navigationModel: AppNavigationModel

    init(container: AppDependencyContainer) {
        self.container = container
        _navigationModel = StateObject(
            wrappedValue: AppNavigationModel(
                initialRoute: container.appLaunchCoordinator.initialRoute()
            )
        )
    }

    var body: some View {
        RootNavigationShell(
            selectedRoute: navigationModel.selectedRoute,
            onSelectRoute: navigationModel.selectTopLevel,
            onBackToRoot: navigationModel.backToStoryRoot
        ) {
            content(for: navigationModel.selectedRoute)
        }
    }

    @ViewBuilder
    private func content(for route: AppRoute) -> some View {
        switch route {
        case .story:
            StoryPlaceholderView(
                onOpenPulse: navigationModel.openPulse,
                onOpenFlow: navigationModel.openFlow
            )
        case .horizon:
            HorizonPlaceholderView()
        case .shell:
            ShellPlaceholderView()
        case .notepad:
            NotepadPlaceholderView()
        case .help:
            HelpPlaceholderView()
        case .flow:
            FlowPlaceholderView(onBackToRoot: navigationModel.backToStoryRoot)
        case .pulse:
            PulsePlaceholderView(onBackToRoot: navigationModel.backToStoryRoot)
        case .flowDetail(_):
            ActionRoutePlaceholderView(
                route: route,
                message: "A completed Flow detail view will live here.",
                onBackToRoot: navigationModel.backToStoryRoot
            )
        case .flowEdit(_):
            ActionRoutePlaceholderView(
                route: route,
                message: "Flow editing will live here.",
                onBackToRoot: navigationModel.backToStoryRoot
            )
        case .pulseDetail(_):
            ActionRoutePlaceholderView(
                route: route,
                message: "A Pulse detail view will live here.",
                onBackToRoot: navigationModel.backToStoryRoot
            )
        case .pulseEdit(_):
            ActionRoutePlaceholderView(
                route: route,
                message: "Pulse editing will live here.",
                onBackToRoot: navigationModel.backToStoryRoot
            )
        case .shellRoom(_):
            PlaceholderRouteView(
                route: route,
                message: "This Shell room will be implemented in a future phase."
            )
        }
    }
}

#Preview {
    AppRootView(container: AppDependencyContainer())
}
