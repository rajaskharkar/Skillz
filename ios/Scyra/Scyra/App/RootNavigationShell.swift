import SwiftUI

struct RootNavigationShell<Content: View>: View {
    let selectedRoute: AppRoute
    let onSelectRoute: (AppRoute) -> Void
    let onBackToRoot: () -> Void

    private let content: Content

    init(
        selectedRoute: AppRoute,
        onSelectRoute: @escaping (AppRoute) -> Void,
        onBackToRoot: @escaping () -> Void,
        @ViewBuilder content: () -> Content
    ) {
        self.selectedRoute = selectedRoute
        self.onSelectRoute = onSelectRoute
        self.onBackToRoot = onBackToRoot
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 0) {
            ScyraTopBar(
                selectedRoute: selectedRoute,
                showsBackButton: shouldShowBackButton(for: selectedRoute),
                onSelectRoute: onSelectRoute,
                onBackToRoot: onBackToRoot
            )

            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(ScyraColors.backgroundBottom.ignoresSafeArea())
    }

    private func shouldShowBackButton(for route: AppRoute) -> Bool {
        route == .flow || route == .pulse
    }
}

#Preview("Story root") {
    RootNavigationShell(
        selectedRoute: .story,
        onSelectRoute: { _ in },
        onBackToRoot: {}
    ) {
        StoryPlaceholderView(onSelectRoute: { _ in })
    }
}

#Preview("Flow with root back") {
    RootNavigationShell(
        selectedRoute: .flow,
        onSelectRoute: { _ in },
        onBackToRoot: {}
    ) {
        FlowPlaceholderView()
    }
}
