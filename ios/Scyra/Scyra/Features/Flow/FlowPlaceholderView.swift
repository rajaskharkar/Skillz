import SwiftUI

struct FlowPlaceholderView: View {
    let onBackToRoot: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScyraActionScreenHeader(title: "Flow", onBack: onBackToRoot)

            PlaceholderRouteView(
                route: .flow,
                message: "Flow will help you enter protected focus and record completed sessions."
            )
        }
    }
}

#Preview { FlowPlaceholderView(onBackToRoot: {}) }
