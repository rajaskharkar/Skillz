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
        HStack(spacing: 4) {
            if showsBackButton {
                Button(action: onBackToRoot) {
                    ScyraCanonicalIcon(systemName: "chevron.left")
                        .font(ScyraTypography.navigationIcon)
                        .foregroundStyle(ScyraColors.onPrimary)
                        .frame(width: ScyraSpacing.topBarTapTarget, height: ScyraSpacing.topBarTapTarget)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Back to Story")
            }

            Text("Scyra")
                .font(ScyraTypography.appTitleResolved)
                .foregroundStyle(ScyraColors.onPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .accessibilityAddTraits(.isHeader)

            Spacer(minLength: 0)

            ForEach(AppRoute.rootTopBarActions, id: \.self) { route in
                ScyraTopBarButton(
                    route: route,
                    isSelected: selectedRoute.matchesTopBarAction(route),
                    action: { onSelectRoute(route) }
                )
            }
        }
        .padding(.leading, ScyraSpacing.md)
        .padding(.trailing, 6)
        .frame(height: 64)
        .background(ScyraColors.primary)
    }
}

#Preview("Story") {
    ScyraTopBar(selectedRoute: .story, onSelectRoute: { _ in }, onBackToRoot: {})
}

#Preview("Horizon") {
    ScyraTopBar(selectedRoute: .horizon, onSelectRoute: { _ in }, onBackToRoot: {})
}
