import SwiftUI

struct PlaceholderRouteView: View {
    let route: AppRoute

    var body: some View {
        ZStack {
            LinearGradient(colors: [ScyraColors.background, ScyraColors.backgroundBottom], startPoint: .top, endPoint: .bottom).ignoresSafeArea()

            ScyraEmptyState(
                systemImage: route.display.systemImage,
                title: route.display.title,
                message: "This screen is planned for future Scyra parity work."
            )
            .padding(ScyraSpacing.screenPadding)
            .accessibilityLabel(route.display.accessibilityLabel)
        }
    }
}

#Preview { PlaceholderRouteView(route: .flow) }
