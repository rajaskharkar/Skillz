import SwiftUI

struct RootNavigationShell<Content: View>: View {
    let selectedRoute: AppRoute
    let onSelectRoute: (AppRoute) -> Void

    private let content: Content

    init(
        selectedRoute: AppRoute,
        onSelectRoute: @escaping (AppRoute) -> Void,
        @ViewBuilder content: () -> Content
    ) {
        self.selectedRoute = selectedRoute
        self.onSelectRoute = onSelectRoute
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 0) {
            ScyraTopBar(
                selectedRoute: selectedRoute,
                onSelectRoute: onSelectRoute
            )

            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(ScyraColors.backgroundBottom)
    }
}

#Preview {
    RootNavigationShell(
        selectedRoute: .home,
        onSelectRoute: { _ in }
    ) {
        HomePlaceholderView()
    }
}
