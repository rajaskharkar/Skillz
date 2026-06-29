import SwiftUI

struct FlowPlaceholderView: View {
    var body: some View {
        PlaceholderRouteView(
            route: .flow,
            message: "Flow will help you enter protected focus and record completed sessions."
        )
    }
}

#Preview { FlowPlaceholderView() }
