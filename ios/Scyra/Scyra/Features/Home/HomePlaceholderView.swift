import SwiftUI

struct HomePlaceholderView: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [ScyraColor.backgroundTop, ScyraColor.backgroundBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: ScyraSpacing.lg) {
                VStack(spacing: ScyraSpacing.sm) {
                    Text("Home")
                        .font(ScyraTypography.screenTitle)
                        .foregroundStyle(ScyraColor.textPrimary)
                        .multilineTextAlignment(.center)

                    Text("Scyra’s iOS shell is ready for future feature destinations.")
                        .font(ScyraTypography.body)
                        .foregroundStyle(ScyraColor.textSecondary)
                        .multilineTextAlignment(.center)
                }

                Text("Feature screens will arrive in safe, scoped phases.")
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColor.textSecondary)
                    .padding(.horizontal, ScyraSpacing.md)
                    .padding(.vertical, ScyraSpacing.sm)
                    .background(ScyraColor.primaryTeal.opacity(0.10))
                    .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.capsule))
            }
            .padding(ScyraSpacing.xl)
            .frame(maxWidth: .infinity)
            .background(ScyraColor.cardBackground)
            .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.largeCard))
            .shadow(color: ScyraColor.primaryTeal.opacity(0.18), radius: 24, x: 0, y: 12)
            .padding(ScyraSpacing.screenPadding)
        }
    }
}

#Preview {
    HomePlaceholderView()
}
