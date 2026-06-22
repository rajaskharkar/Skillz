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
                Text(route.title)
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
        }
    }
}

private extension AppRoute {
    var title: String {
        switch self {
        case .home: "Home"
        case .flow: "Flow"
        case .story: "Story"
        case .paths: "Paths"
        case .shell: "Shell"
        case .notepad: "Notepad"
        case .help: "Help"
        }
    }
}

#Preview {
    PlaceholderRouteView(route: .flow)
}
