import SwiftUI

struct RootNavigationShell<Content: View>: View {
    let selectedRoute: AppRoute
    let onSelectRoute: (AppRoute) -> Void
    let onBackToRoot: () -> Void
    let shellPearlBalance: Int
    let shellNotificationCount: Int
    let onShellNotifications: () -> Void

    private let content: Content

    init(
        selectedRoute: AppRoute,
        onSelectRoute: @escaping (AppRoute) -> Void,
        onBackToRoot: @escaping () -> Void,
        shellPearlBalance: Int = 0,
        shellNotificationCount: Int = 0,
        onShellNotifications: @escaping () -> Void = {},
        @ViewBuilder content: () -> Content
    ) {
        self.selectedRoute = selectedRoute
        self.onSelectRoute = onSelectRoute
        self.onBackToRoot = onBackToRoot
        self.shellPearlBalance = shellPearlBalance
        self.shellNotificationCount = shellNotificationCount
        self.onShellNotifications = onShellNotifications
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 0) {
            if selectedRoute.isShellRoute {
                ScyraShellTopBar(
                    title: selectedRoute == .shell ? "The Shell" : selectedRoute.display.title,
                    pearlBalance: shellPearlBalance,
                    notificationCount: shellNotificationCount,
                    onBack: onBackToRoot,
                    onNotifications: onShellNotifications
                )
            } else if selectedRoute.usesScyraTopBar {
                ScyraTopBar(
                    selectedRoute: selectedRoute,
                    showsBackButton: false,
                    onSelectRoute: onSelectRoute,
                    onBackToRoot: onBackToRoot
                )
            }

            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .simultaneousGesture(homePagerGesture)
        }
        .background(ScyraColors.backgroundBottom.ignoresSafeArea())
    }

    private var homePagerGesture: some Gesture {
        DragGesture(minimumDistance: 36)
            .onEnded { value in
                let horizontal = value.predictedEndTranslation.width
                let vertical = value.predictedEndTranslation.height
                guard abs(horizontal) > abs(vertical) * 1.35,
                      abs(horizontal) > 64,
                      let index = Self.homePages.firstIndex(of: selectedRoute) else { return }
                let destination = horizontal < 0 ? index + 1 : index - 1
                guard Self.homePages.indices.contains(destination) else { return }
                withAnimation(.easeOut(duration: 0.24)) {
                    onSelectRoute(Self.homePages[destination])
                }
            }
    }

    /// Android's horizontal home pager contains these four destinations; Shell is a separate
    /// top-bar action and must not become an extra page in the swipe sequence.
    private static var homePages: [AppRoute] { [.story, .horizon, .notepad, .help] }
}

#Preview("Story root") {
    RootNavigationShell(
        selectedRoute: .story,
        onSelectRoute: { _ in },
        onBackToRoot: {}
    ) {
        StoryView(
            viewModel: StoryViewModel(repository: InMemoryFlowRepository()),
            preferences: AppPreferencesModel(store: InMemoryAppPreferencesStore()),
            onOpenPulse: {},
            onOpenFlow: {},
            onOpenFlowDetail: { _ in },
            onOpenPulseDetail: { _ in },
            onEditPulse: { _ in }
        )
    }
}

#Preview("Flow action screen") {
    RootNavigationShell(
        selectedRoute: .flow,
        onSelectRoute: { _ in },
        onBackToRoot: {}
    ) {
        FlowView(
            viewModel: FlowViewModel(repository: InMemoryFlowRepository()),
            preferences: AppPreferencesModel(store: InMemoryAppPreferencesStore()),
            onBackToStory: {},
            onOpenShell: {}
        )
    }
}
