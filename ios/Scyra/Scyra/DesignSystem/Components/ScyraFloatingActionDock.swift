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
        .padding(ScyraSpacing.sm)
        .background(ScyraColors.elevatedSurface)
        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.largeCard, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ScyraRadius.largeCard, style: .continuous)
                .stroke(ScyraColors.border, lineWidth: 1)
        )
        .shadow(color: ScyraColors.primary.opacity(0.12), radius: 12, x: 0, y: 6)
        .accessibilityElement(children: .contain)
    }
}

#Preview("Floating action dock") {
    ZStack(alignment: .bottomLeading) {
        ScyraColors.background.ignoresSafeArea()
        ScyraFloatingActionDock {
            ScyraButton("Flow", systemImage: "play.circle") {}
            ScyraButton("Pulse", systemImage: "waveform", variant: .secondary) {}
        }
        .padding(ScyraSpacing.screenPadding)
    }
}
