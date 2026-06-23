import SwiftUI

enum ScyraChipTint { case primary, secondary, reward }

struct ScyraChip: View {
    let title: String
    let systemImage: String?
    let isSelected: Bool
    let tint: ScyraChipTint
    let isDisabled: Bool
    let action: (() -> Void)?

    init(_ title: String, systemImage: String? = nil, isSelected: Bool = false, tint: ScyraChipTint = .primary, isDisabled: Bool = false, action: (() -> Void)? = nil) {
        self.title = title; self.systemImage = systemImage; self.isSelected = isSelected; self.tint = tint; self.isDisabled = isDisabled; self.action = action
    }

    var body: some View {
        Group { if let action { Button(action: action) { content }.buttonStyle(.plain).disabled(isDisabled) } else { content } }
            .opacity(isDisabled ? 0.5 : 1)
    }

    private var content: some View {
        HStack(spacing: ScyraSpacing.xs) {
            if let systemImage {
                Image(systemName: systemImage)
                    .accessibilityHidden(true)
            }
            Text(title)
        }
            .font(ScyraTypography.label)
            .foregroundStyle(isSelected ? selectedForeground : baseColor)
            .padding(.horizontal, ScyraSpacing.md)
            .padding(.vertical, ScyraSpacing.sm)
            .frame(minHeight: action == nil ? 0 : ScyraSpacing.topBarTapTarget)
            .background(isSelected ? baseColor : baseColor.opacity(0.10))
            .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.capsule, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: ScyraRadius.capsule, style: .continuous).stroke(baseColor.opacity(0.25), lineWidth: 1))
    }

    private var baseColor: Color { tint == .secondary ? ScyraColors.secondaryGold : (tint == .reward ? ScyraColors.rewardMovement : ScyraColors.primary) }
    private var selectedForeground: Color { tint == .secondary ? ScyraColors.textPrimary : .white }
}

#Preview("ScyraChip") { HStack { ScyraChip("Flow", isSelected: true); ScyraChip("Story", systemImage: "book"); ScyraChip("Movement", tint: .reward) }.padding().background(ScyraColors.background) }
