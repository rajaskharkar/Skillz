import SwiftUI

struct HelpPlaceholderView: View {
    var body: some View {
        PlaceholderRouteView(
            route: .help,
            message: "Help will explain Scyra’s systems, rooms, rewards, and focus tools."
        )
    }
}

#Preview { HelpPlaceholderView() }
