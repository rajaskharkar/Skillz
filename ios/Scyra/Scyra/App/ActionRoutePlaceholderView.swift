import SwiftUI

struct ActionRoutePlaceholderView: View {
    let route: AppRoute
    let message: String
    let onBackToRoot: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScyraActionScreenHeader(title: route.display.title, onBack: onBackToRoot)

            PlaceholderRouteView(route: route, message: message)
        }
    }
}

#Preview("Flow detail placeholder") {
    ActionRoutePlaceholderView(
        route: .flowDetail(id: "preview-flow"),
        message: "A completed Flow detail view will live here.",
        onBackToRoot: {}
    )
}
