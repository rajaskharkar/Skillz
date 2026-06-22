import SwiftUI

struct RootNavigationShell<Content: View>: View {
    let selectedRoute: AppRoute
    let onSelectRoute: (AppRoute) -> Void
    @ViewBuilder let content: Content

    var body: some View {
        VStack(spacing: 0) {
            ScyraTopBar(
                selectedRoute: selectedRoute,
                onSelectRoute: onSelectRoute
            )

            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(ScyraColor.backgroundBottom)
    }
}

#Preview {
    RootNavigationShell(selectedRoute: .home) { _ in } content: {
        HomePlaceholderView()
    }
}
