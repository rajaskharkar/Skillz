import SwiftUI

struct PlaceholderRouteView: View {
    let route: AppRoute

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [ScyraColors.background, ScyraColors.backgroundBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: ScyraSpacing.md) {
                if let systemImage = route.display.systemImage {
                    Image(systemName: systemImage)
                        .font(ScyraTypography.rewardNumber)
                        .foregroundStyle(ScyraColors.primaryTeal)
                        .accessibilityHidden(true)
                }

                Text(route.display.title)
                    .font(ScyraTypography.screenTitle)
                    .foregroundStyle(ScyraColors.textPrimary)

                Text("This screen is planned for future Scyra parity work.")
                    .font(ScyraTypography.body)
                    .foregroundStyle(ScyraColors.textSecondary)
                    .multilineTextAlignment(.center)
            }
            .padding(ScyraSpacing.xl)
            .frame(maxWidth: .infinity)
            .background(ScyraColors.elevatedSurface)
            .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card))
            .padding(ScyraSpacing.screenPadding)
            .accessibilityElement(children: .combine)
            .accessibilityLabel(route.display.accessibilityLabel)
        }
    }
}

#Preview {
    PlaceholderRouteView(route: .flow)
}
