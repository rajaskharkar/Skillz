import SwiftUI

struct StoryPlaceholderView: View {
    let onOpenPulse: () -> Void
    let onOpenFlow: () -> Void

    var body: some View {
        PlaceholderRouteView(
            route: .story,
            message: "Your Scyra Story will open here, matching the Android launch experience."
        )
        .safeAreaInset(edge: .bottom, alignment: .trailing, spacing: 0) {
            ScyraFloatingActionDock {
                ScyraButton("Pulse", systemImage: "brain.head.profile", variant: .secondary) {
                    onOpenPulse()
                }
                .accessibilityLabel("Open Pulse")

                ScyraButton("Flow", systemImage: "sparkles") {
                    onOpenFlow()
                }
                .accessibilityLabel("Open Flow")
            }
            .padding(.trailing, ScyraSpacing.screenPadding)
            .padding(.bottom, ScyraSpacing.md)
        }
    }
}

#Preview { StoryPlaceholderView(onOpenPulse: {}, onOpenFlow: {}) }
