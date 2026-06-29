import SwiftUI

struct ShellPlaceholderView: View {
    var body: some View {
        PlaceholderRouteView(
            route: .shell,
            message: "Shell will hold Scyra’s creature rooms, rewards, Chest, and badges."
        )
    }
}

#Preview { ShellPlaceholderView() }
