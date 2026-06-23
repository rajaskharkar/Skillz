import SwiftUI

struct ScyraEmptyState: View {
    let systemImage: String?
    let title: String
    let message: String
    let actionTitle: String?
    let action: (() -> Void)?

    init(systemImage: String? = nil, title: String, message: String, actionTitle: String? = nil, action: (() -> Void)? = nil) {
        self.systemImage = systemImage; self.title = title; self.message = message; self.actionTitle = actionTitle; self.action = action
    }

    var body: some View {
        ScyraCard(style: .elevated, padding: ScyraSpacing.xl) {
            VStack(spacing: ScyraSpacing.md) {
                if let systemImage { Image(systemName: systemImage).font(ScyraTypography.rewardNumber).foregroundStyle(ScyraColors.primary).accessibilityHidden(true) }
                Text(title).font(ScyraTypography.screenTitle).foregroundStyle(ScyraColors.textPrimary).multilineTextAlignment(.center)
                Text(message).font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary).multilineTextAlignment(.center)
                if let actionTitle, let action { ScyraButton(actionTitle, action: action).padding(.top, ScyraSpacing.sm) }
            }
            .frame(maxWidth: .infinity)
        }
        .accessibilityElement(children: .combine)
    }
}

#Preview("ScyraEmptyState") { ScyraEmptyState(systemImage: "sparkles", title: "Nothing here yet", message: "Future Scyra work will fill this space.", actionTitle: "Begin") {}.padding().background(ScyraColors.background) }
