import SwiftUI

struct ScyraEmptyState: View {
    let systemImage: String?
    let assetImageName: String?
    let title: String
    let message: String
    let actionTitle: String?
    let action: (() -> Void)?

    init(systemImage: String? = nil, assetImageName: String? = nil, title: String, message: String, actionTitle: String? = nil, action: (() -> Void)? = nil) {
        self.systemImage = systemImage
        self.assetImageName = assetImageName
        self.title = title
        self.message = message
        self.actionTitle = actionTitle
        self.action = action
    }

    var body: some View {
        VStack(spacing: 14) {
            if systemImage != nil || assetImageName != nil {
                Group {
                    if let assetImageName {
                        ScyraMaterialIcon(assetName: assetImageName, size: 28, color: ScyraColors.primary)
                    } else if let systemImage {
                        ScyraCanonicalIcon(systemName: systemImage)
                            .font(.system(size: 28))
                            .foregroundStyle(ScyraColors.primary)
                            .accessibilityHidden(true)
                    }
                }
                .padding(14)
                .background(ScyraColors.primary.opacity(0.12), in: Circle())
            }
            Text(title)
                .font(.title3.weight(.semibold))
                .foregroundStyle(ScyraColors.textPrimary)
                .multilineTextAlignment(.center)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(ScyraColors.textSecondary)
                .multilineTextAlignment(.center)
            if let actionTitle, let action {
                ScyraButton(actionTitle, action: action)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 28)
        .frame(maxWidth: .infinity)
        .background(ScyraColors.surface, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
        .shadow(color: .black.opacity(0.08), radius: 2, y: 1)
        .accessibilityElement(children: .combine)
    }
}

#Preview("ScyraEmptyState") { ScyraEmptyState(systemImage: "sparkles", title: "Nothing here yet", message: "Future Scyra work will fill this space.", actionTitle: "Begin") {}.padding().background(ScyraColors.background) }
