import SwiftUI

struct ScyraFloatingActionDock<Content: View>: View {
    private let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        HStack(spacing: ScyraSpacing.sm) {
            content
        }
        .accessibilityElement(children: .contain)
    }
}

#Preview("Floating action dock") {
    ZStack(alignment: .bottomLeading) {
        ScyraColors.background.ignoresSafeArea()
        ScyraFloatingActionDock {
            ScyraButton("Pulse", systemImage: "brain.head.profile", variant: .secondary) {}
            ScyraButton("Flow", systemImage: "sparkles") {}
        }
        .padding(ScyraSpacing.screenPadding)
    }
}
