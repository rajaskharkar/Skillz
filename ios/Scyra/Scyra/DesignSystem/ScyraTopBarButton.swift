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
                    Circle()
                        .fill(buttonBackgroundColor)
                )
                .overlay(
                    Circle()
                        .stroke(isSelected ? ScyraColors.primaryManuscriptBlue.opacity(0.20) : Color.clear, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(route.display.accessibilityLabel)
        .accessibilityValue(isSelected ? "Selected" : "")
        .accessibilityHint(isSelected ? "Current section" : "Selects \(route.display.title)")
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }

    private var iconFrameSize: CGFloat {
        route.display.assetImageName == nil ? 24 : 34
    }

    private var buttonBackgroundColor: Color {
        if route.display.assetImageName != nil {
            return ScyraColors.primaryManuscriptBlue
        }

        return isSelected ? ScyraColors.primaryContainer : Color.clear
    }

    @ViewBuilder
    private func topBarImage(for display: AppRouteDisplay) -> some View {
        if let assetImageName = display.assetImageName {
            Image(assetImageName)
                .renderingMode(.original)
                .resizable()
                .scaledToFit()
                .accessibilityHidden(true)
        } else {
            Image(systemName: display.systemImage ?? "circle")
                .font(ScyraTypography.navigationIcon)
                .foregroundStyle(isSelected ? ScyraColors.primaryManuscriptBlue : ScyraColors.textSecondary)
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
