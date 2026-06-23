import SwiftUI

struct HomePlaceholderView: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [ScyraColors.background, ScyraColors.backgroundBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: ScyraSpacing.lg) {
                VStack(spacing: ScyraSpacing.sm) {
                    Text("Home")
                        .font(ScyraTypography.screenTitle)
                        .foregroundStyle(ScyraColors.textPrimary)
                        .multilineTextAlignment(.center)

                    Text("Scyra’s iOS shell is ready for future feature destinations.")
                        .font(ScyraTypography.body)
                        .foregroundStyle(ScyraColors.textSecondary)
                        .multilineTextAlignment(.center)
                }

                Text("Feature screens will arrive in safe, scoped phases.")
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                    .padding(.horizontal, ScyraSpacing.md)
                    .padding(.vertical, ScyraSpacing.sm)
                    .background(ScyraColors.primaryManuscriptBlue.opacity(0.10))
                    .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.capsule))
            }
            .padding(ScyraSpacing.xl)
            .frame(maxWidth: .infinity)
            .background(ScyraColors.elevatedSurface)
            .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.largeCard))
            .shadow(color: ScyraColors.primaryManuscriptBlue.opacity(0.18), radius: 24, x: 0, y: 12)
            .padding(ScyraSpacing.screenPadding)
        }
    }
}

#Preview {
    HomePlaceholderView()
}
