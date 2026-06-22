import SwiftUI

struct PlaceholderRouteView: View {
    let route: AppRoute

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [ScyraColor.backgroundTop, ScyraColor.backgroundBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: ScyraSpacing.md) {
                if let systemImage = route.display.systemImage {
                    Image(systemName: systemImage)
                        .font(.system(size: 32, weight: .semibold))
                        .foregroundStyle(ScyraColor.primaryTeal)
                        .accessibilityHidden(true)
                }

                Text(route.display.title)
                    .font(ScyraTypography.screenTitle)
                    .foregroundStyle(ScyraColor.textPrimary)

                Text("This screen is planned for future Scyra parity work.")
                    .font(ScyraTypography.body)
                    .foregroundStyle(ScyraColor.textSecondary)
                    .multilineTextAlignment(.center)
            }
            .padding(ScyraSpacing.xl)
            .frame(maxWidth: .infinity)
            .background(ScyraColor.cardBackground)
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
