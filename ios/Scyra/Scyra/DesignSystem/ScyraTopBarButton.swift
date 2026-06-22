import SwiftUI

struct ScyraTopBarButton: View {
    let route: AppRoute
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            topBarImage(for: route.display)
                .frame(width: 24, height: 24)
                .frame(width: ScyraSpacing.topBarTapTarget, height: ScyraSpacing.topBarTapTarget)
                .background(
                    Circle()
                        .fill(isSelected ? ScyraColor.topBarSelectedBackground : Color.clear)
                )
                .overlay(
                    Circle()
                        .stroke(isSelected ? ScyraColor.primaryTeal.opacity(0.20) : Color.clear, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(route.display.accessibilityLabel)
        .accessibilityValue(isSelected ? "Selected" : "")
        .accessibilityHint(isSelected ? "Current section" : "Selects \(route.display.title)")
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }

    @ViewBuilder
    private func topBarImage(for display: AppRouteDisplay) -> some View {
        if let assetImageName = display.assetImageName {
            Image(assetImageName)
                .resizable()
                .scaledToFit()
                .accessibilityHidden(true)
        } else {
            Image(systemName: display.systemImage ?? "circle")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(isSelected ? ScyraColor.primaryTeal : ScyraColor.textSecondary)
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
