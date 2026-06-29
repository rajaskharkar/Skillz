import SwiftUI

struct ScyraTopBar: View {
    let selectedRoute: AppRoute
    let showsBackButton: Bool
    let onSelectRoute: (AppRoute) -> Void
    let onBackToRoot: () -> Void

    init(
        selectedRoute: AppRoute,
        showsBackButton: Bool = false,
        onSelectRoute: @escaping (AppRoute) -> Void,
        onBackToRoot: @escaping () -> Void = {}
    ) {
        self.selectedRoute = selectedRoute
        self.showsBackButton = showsBackButton
        self.onSelectRoute = onSelectRoute
        self.onBackToRoot = onBackToRoot
    }

    var body: some View {
        HStack(spacing: ScyraSpacing.sm) {
            if showsBackButton {
                Button(action: onBackToRoot) {
                    Image(systemName: "chevron.left")
                        .font(ScyraTypography.navigationIcon)
                        .foregroundStyle(ScyraColors.primary)
                        .frame(width: ScyraSpacing.topBarTapTarget, height: ScyraSpacing.topBarTapTarget)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Back to Story")
            }

            Button(action: onBackToRoot) {
                Text("Scyra")
                    .font(ScyraTypography.appTitleResolved)
                    .foregroundStyle(ScyraColors.primary)
                    .frame(minHeight: ScyraSpacing.topBarTapTarget)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Open Story")
            .accessibilityValue(selectedRoute == .story ? "Selected" : "")
            .accessibilityAddTraits(selectedRoute == .story ? [.isSelected] : [])

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

#Preview("Story") {
    ScyraTopBar(selectedRoute: .story, onSelectRoute: { _ in }, onBackToRoot: {})
}

#Preview("Flow with back") {
    ScyraTopBar(selectedRoute: .flow, showsBackButton: true, onSelectRoute: { _ in }, onBackToRoot: {})
}
