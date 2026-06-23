import SwiftUI

struct ScyraTopBar: View {
    let selectedRoute: AppRoute
    let onSelectRoute: (AppRoute) -> Void

    var body: some View {
        HStack(spacing: ScyraSpacing.sm) {
            Button {
                onSelectRoute(.home)
            } label: {
                Text("Scyra")
                    .font(ScyraTypography.topBarTitle)
                    .foregroundStyle(ScyraColors.primaryManuscriptBlue)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Open Home")
            .accessibilityValue(selectedRoute == .home ? "Selected" : "")
            .accessibilityAddTraits(selectedRoute == .home ? [.isSelected] : [])

            Spacer(minLength: ScyraSpacing.sm)

            ForEach(AppRoute.rootTopBarActions, id: \.self) { route in
                ScyraTopBarButton(
                    route: route,
                    isSelected: selectedRoute == route,
                    action: { onSelectRoute(route) }
                )
            }
        }
        .padding(.horizontal, ScyraSpacing.md)
        .padding(.vertical, ScyraSpacing.sm)
        .background(ScyraColors.elevatedSurface)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(ScyraColors.hairline)
                .frame(height: 1)
        }
    }
}

#Preview {
    ScyraTopBar(selectedRoute: .shell) { _ in }
}
