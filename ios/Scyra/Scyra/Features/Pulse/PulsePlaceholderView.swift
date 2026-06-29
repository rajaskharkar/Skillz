import SwiftUI

struct PulsePlaceholderView: View {
    let onBackToRoot: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScyraActionScreenHeader(title: "Pulse", onBack: onBackToRoot)

            PlaceholderRouteView(
                route: .pulse,
                message: "Pulse will capture thoughts and ideas without turning them into rewards."
            )
        }
    }
}

#Preview { PulsePlaceholderView(onBackToRoot: {}) }
