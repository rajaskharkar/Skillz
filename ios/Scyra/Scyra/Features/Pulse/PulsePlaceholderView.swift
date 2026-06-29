import SwiftUI

struct PulsePlaceholderView: View {
    var body: some View {
        PlaceholderRouteView(
            route: .pulse,
            message: "Pulse will capture thoughts and ideas without turning them into rewards."
        )
    }
}

#Preview { PulsePlaceholderView() }
