import SwiftUI

struct StoryPlaceholderView: View {
    var body: some View {
        PlaceholderRouteView(
            route: .story,
            message: "Your Scyra Story will open here, matching the Android launch experience."
        )
    }
}

#Preview { StoryPlaceholderView() }
