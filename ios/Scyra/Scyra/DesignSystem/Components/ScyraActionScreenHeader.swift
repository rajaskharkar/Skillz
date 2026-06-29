import SwiftUI

struct ScyraActionScreenHeader: View {
    let title: String
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: ScyraSpacing.sm) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(ScyraTypography.navigationIcon)
                    .foregroundStyle(ScyraColors.primary)
                    .frame(width: ScyraSpacing.topBarTapTarget, height: ScyraSpacing.topBarTapTarget)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Back to Story")

            Text(title)
                .font(ScyraTypography.screenTitle)
                .foregroundStyle(ScyraColors.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.80)

            Spacer(minLength: ScyraSpacing.sm)
        }
        .padding(.horizontal, ScyraSpacing.screenPadding)
        .padding(.vertical, ScyraSpacing.sm)
        .background(ScyraColors.background)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(ScyraColors.hairline)
                .frame(height: 1)
        }
    }
}

#Preview("Flow action header") {
    ScyraActionScreenHeader(title: "Flow", onBack: {})
}
