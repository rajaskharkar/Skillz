import SwiftUI

struct ScyraActionScreenHeader: View {
    let title: String
    var backAccessibilityLabel = "Back to Story"
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: ScyraSpacing.sm) {
            Button(action: onBack) {
                ScyraCanonicalIcon(systemName: "chevron.left")
                    .font(ScyraTypography.navigationIcon)
                    .foregroundStyle(ScyraColors.primary)
                    .frame(width: ScyraSpacing.topBarTapTarget, height: ScyraSpacing.topBarTapTarget)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(backAccessibilityLabel)

            Text(title)
                .font(.system(.title3, design: .default).weight(.semibold))
                .foregroundStyle(ScyraColors.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.80)

            Spacer(minLength: ScyraSpacing.sm)
        }
        .padding(.horizontal, 4)
        .frame(height: 64)
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
