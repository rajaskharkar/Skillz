import SwiftUI

struct ScyraTopBarButton: View {
    let route: AppRoute
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            topBarImage(for: route.display)
                .frame(width: iconFrameSize, height: iconFrameSize)
                .frame(width: ScyraSpacing.topBarTapTarget, height: ScyraSpacing.topBarTapTarget)
                .background(
                    Capsule()
                        .fill(buttonBackgroundColor)
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(route.display.accessibilityLabel)
        .accessibilityValue(isSelected ? "Selected" : "")
        .accessibilityHint(isSelected ? "Current section" : "Selects \(route.display.title)")
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }

    private var iconFrameSize: CGFloat {
        route.display.assetImageName == "scyraTurtle" ? 34 : 22
    }

    private var buttonBackgroundColor: Color {
        isSelected && route.display.assetImageName != "scyraTurtle"
            ? ScyraColors.onPrimary.opacity(0.18)
            : Color.clear
    }

    @ViewBuilder
    private func topBarImage(for display: AppRouteDisplay) -> some View {
        if let assetImageName = display.assetImageName {
            if assetImageName == "scyraTurtle" {
                Image(assetImageName)
                    .renderingMode(.original)
                    .resizable()
                    .scaledToFit()
                    .accessibilityHidden(true)
            } else {
                Image(assetImageName)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundStyle(ScyraColors.onPrimary.opacity(isSelected ? 1 : 0.72))
                    .accessibilityHidden(true)
            }
        } else {
            ScyraCanonicalIcon(systemName: display.systemImage ?? "circle")
                .font(ScyraTypography.navigationIcon)
                .foregroundStyle(ScyraColors.onPrimary.opacity(isSelected ? 1 : 0.72))
                .accessibilityHidden(true)
        }
    }
}

#Preview {
    HStack {
        ScyraTopBarButton(route: .story, isSelected: false) {}
        ScyraTopBarButton(route: .shell, isSelected: true) {}
    }
    .padding()
}
