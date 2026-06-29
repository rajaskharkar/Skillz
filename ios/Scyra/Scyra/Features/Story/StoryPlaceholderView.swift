import SwiftUI

struct StoryPlaceholderView: View {
    let onSelectRoute: (AppRoute) -> Void

    var body: some View {
        PlaceholderRouteView(
            route: .story,
            message: "Your Scyra Story will open here, matching the Android launch experience."
        )
        .safeAreaInset(edge: .bottom, alignment: .leading, spacing: 0) {
            ScyraFloatingActionDock {
                ScyraButton("Flow", systemImage: "play.circle") {
                    onSelectRoute(.flow)
                }
                .accessibilityLabel("Open Flow")

                ScyraButton("Pulse", systemImage: "waveform", variant: .secondary) {
                    onSelectRoute(.pulse)
                }
                .accessibilityLabel("Open Pulse")
            }
            .padding(.leading, ScyraSpacing.screenPadding)
            .padding(.bottom, ScyraSpacing.md)
        }
    }
}

#Preview { StoryPlaceholderView(onSelectRoute: { _ in }) }
