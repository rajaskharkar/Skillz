import SwiftUI

struct ScyraDialog<Content: View>: View {
    let title: String
    let message: String?
    let primaryTitle: String
    let primaryVariant: ScyraButtonVariant
    let primaryAction: () -> Void
    let secondaryTitle: String?
    let secondaryAction: (() -> Void)?
    private let content: Content

    init(title: String, message: String? = nil, primaryTitle: String, primaryVariant: ScyraButtonVariant = .primary, primaryAction: @escaping () -> Void, secondaryTitle: String? = nil, secondaryAction: (() -> Void)? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.message = message
        self.primaryTitle = primaryTitle
        self.primaryVariant = primaryVariant
        self.primaryAction = primaryAction
        self.secondaryTitle = secondaryTitle
        self.secondaryAction = secondaryAction
        self.content = content()
    }

    var body: some View {
        ScyraCard(style: .elevated, padding: ScyraSpacing.lg) {
            VStack(spacing: ScyraSpacing.md) {
                Text(title)
                    .font(ScyraTypography.cardTitle)
                    .foregroundStyle(ScyraColors.textPrimary)
                    .multilineTextAlignment(.center)

                if let message {
                    Text(message)
                        .font(ScyraTypography.body)
                        .foregroundStyle(ScyraColors.textSecondary)
                        .multilineTextAlignment(.center)
                }

                content

                HStack {
                    if let secondaryTitle, let secondaryAction {
                        ScyraButton(secondaryTitle, variant: .ghost, action: secondaryAction)
                    }
                    ScyraButton(primaryTitle, variant: primaryVariant, action: primaryAction)
                }
            }
            .frame(maxWidth: .infinity)
        }
    }
}

extension ScyraDialog where Content == EmptyView {
    init(title: String, message: String? = nil, primaryTitle: String, primaryVariant: ScyraButtonVariant = .primary, primaryAction: @escaping () -> Void, secondaryTitle: String? = nil, secondaryAction: (() -> Void)? = nil) {
        self.init(title: title, message: message, primaryTitle: primaryTitle, primaryVariant: primaryVariant, primaryAction: primaryAction, secondaryTitle: secondaryTitle, secondaryAction: secondaryAction) { EmptyView() }
    }
}

#Preview("ScyraDialog") {
    ScyraDialog(title: "Complete Flow?", message: "Preview how Scyra dialogs will frame decisions.", primaryTitle: "Continue", primaryAction: {}, secondaryTitle: "Cancel", secondaryAction: {})
        .padding()
        .background(ScyraColors.background)
}
