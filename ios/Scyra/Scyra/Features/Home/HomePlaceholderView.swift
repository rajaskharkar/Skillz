import SwiftUI

struct HomePlaceholderView: View {
    var body: some View {
        ZStack {
            LinearGradient(colors: [ScyraColors.background, ScyraColors.backgroundBottom], startPoint: .top, endPoint: .bottom).ignoresSafeArea()

            ScyraCard(style: .elevated, padding: ScyraSpacing.xl) {
                VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                    ScyraSectionHeader(title: "Home", subtitle: "Scyra’s iOS shell is ready for future feature destinations.")
                    ScyraChip("Feature screens will arrive in safe, scoped phases.", systemImage: "sparkles", tint: .primary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(ScyraSpacing.screenPadding)
        }
    }
}

#Preview { HomePlaceholderView() }
