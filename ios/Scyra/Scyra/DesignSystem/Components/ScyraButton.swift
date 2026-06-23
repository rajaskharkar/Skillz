import SwiftUI

enum ScyraButtonVariant {
    case primary
    case secondary
    case ghost
    case destructive
}

struct ScyraButton: View {
    let title: String
    let systemImage: String?
    let variant: ScyraButtonVariant
    let isDisabled: Bool
    let action: () -> Void

    init(_ title: String, systemImage: String? = nil, variant: ScyraButtonVariant = .primary, isDisabled: Bool = false, action: @escaping () -> Void) {
        self.title = title
        self.systemImage = systemImage
        self.variant = variant
        self.isDisabled = isDisabled
        self.action = action
    }

    var body: some View {
        Button(role: variant == .destructive ? .destructive : nil, action: action) {
            HStack(spacing: ScyraSpacing.sm) {
                if let systemImage {
                    Image(systemName: systemImage)
                        .accessibilityHidden(true)
                }
                Text(title)
            }
                .font(ScyraTypography.button)
                .foregroundStyle(foregroundColor)
                .frame(minHeight: ScyraSpacing.topBarTapTarget)
                .padding(.horizontal, ScyraSpacing.lg)
                .background(backgroundColor)
                .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.capsule, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: ScyraRadius.capsule, style: .continuous)
                        .stroke(borderColor, lineWidth: 1)
                )
                .opacity(isDisabled ? 0.55 : 1)
        }
        .buttonStyle(.plain)
        .disabled(isDisabled)
    }

    private var foregroundColor: Color {
        switch variant {
        case .primary, .destructive: return .white
        case .secondary: return ScyraColors.primary
        case .ghost: return ScyraColors.textPrimary
        }
    }

    private var backgroundColor: Color {
        switch variant {
        case .primary: return ScyraColors.primary
        case .secondary: return ScyraColors.primaryContainer
        case .ghost: return .clear
        case .destructive: return ScyraColors.error
        }
    }

    private var borderColor: Color { variant == .ghost ? ScyraColors.border : .clear }
}

#Preview("ScyraButton") {
    VStack(spacing: ScyraSpacing.md) {
        ScyraButton("Begin Flow", systemImage: "play.fill") {}
        ScyraButton("Review", variant: .secondary) {}
        ScyraButton("Maybe Later", variant: .ghost) {}
        ScyraButton("Remove", systemImage: "trash", variant: .destructive) {}
        ScyraButton("Disabled", isDisabled: true) {}
    }.padding().background(ScyraColors.background)
}
