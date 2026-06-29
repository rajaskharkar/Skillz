import SwiftUI

struct PlaceholderRouteView: View {
    let route: AppRoute
    let message: String

    var body: some View {
        ZStack {
            LinearGradient(colors: [ScyraColors.background, ScyraColors.backgroundBottom], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()

            ScyraEmptyState(
                systemImage: route.display.systemImage,
                title: route.display.title,
                message: message
            )
            .padding(ScyraSpacing.screenPadding)
            .accessibilityLabel(route.display.accessibilityLabel)
        }
    }
}

#Preview("Story") {
    PlaceholderRouteView(
        route: .story,
        message: "Your Scyra Story will open here, matching the Android launch experience."
    )
}
