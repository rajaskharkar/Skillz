import SwiftUI

struct HorizonPlaceholderView: View {
    var body: some View {
        PlaceholderRouteView(
            route: .horizon,
            message: "Horizon will show your direction, progress, and wider Scyra overview."
        )
    }
}

#Preview { HorizonPlaceholderView() }
