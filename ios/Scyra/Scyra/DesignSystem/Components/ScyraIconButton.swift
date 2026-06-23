import SwiftUI

struct ScyraIconButton: View {
    let systemImage: String?
    let assetImageName: String?
    let accessibilityLabel: String
    let isSelected: Bool
    let action: () -> Void

    init(systemImage: String, accessibilityLabel: String, isSelected: Bool = false, action: @escaping () -> Void) {
        self.systemImage = systemImage
        self.assetImageName = nil
        self.accessibilityLabel = accessibilityLabel
        self.isSelected = isSelected
        self.action = action
    }

    init(assetImageName: String, accessibilityLabel: String, isSelected: Bool = false, action: @escaping () -> Void) {
        self.systemImage = nil
        self.assetImageName = assetImageName
        self.accessibilityLabel = accessibilityLabel
        self.isSelected = isSelected
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            icon
                .frame(width: 28, height: 28)
                .frame(width: ScyraSpacing.topBarTapTarget, height: ScyraSpacing.topBarTapTarget)
                .background(Circle().fill(isSelected ? ScyraColors.primaryContainer : Color.clear))
                .overlay(Circle().stroke(isSelected ? ScyraColors.border : Color.clear, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityValue(isSelected ? "Selected" : "")
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }

    @ViewBuilder private var icon: some View {
        if let assetImageName {
            Image(assetImageName).renderingMode(.original).resizable().scaledToFit().accessibilityHidden(true)
        } else {
            Image(systemName: systemImage ?? "circle")
                .font(ScyraTypography.navigationIcon)
                .foregroundStyle(isSelected ? ScyraColors.primaryManuscriptBlue : ScyraColors.textSecondary)
                .accessibilityHidden(true)
        }
    }
}

#Preview("ScyraIconButton") {
    HStack { ScyraIconButton(systemImage: "book", accessibilityLabel: "Story") {}; ScyraIconButton(systemImage: "leaf.fill", accessibilityLabel: "Selected Shell", isSelected: true) {} }
        .padding().background(ScyraColors.background)
}
