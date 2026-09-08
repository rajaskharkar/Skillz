import SwiftUI

struct ScyraShellTopBar: View {
    let title: String
    let pearlBalance: Int
    let notificationCount: Int
    let onBack: () -> Void
    let onNotifications: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Button(action: onBack) {
                ScyraCanonicalIcon(systemName: "arrow.left")
                    .font(ScyraTypography.navigationIcon)
                    .foregroundStyle(ScyraColors.primary)
                    .frame(width: ScyraSpacing.topBarTapTarget, height: ScyraSpacing.topBarTapTarget)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Return from The Shell")
            .accessibilityIdentifier("Back to Shell")

            Text(title)
                .font(.headline.weight(.semibold))
                .foregroundStyle(ScyraColors.textPrimary)
                .lineLimit(1)

            Spacer(minLength: 4)

            HStack(spacing: 8) {
                ShellPearlMiniIcon()
                    .frame(width: 18, height: 18)
                    .accessibilityHidden(true)
                Text("\(pearlBalance) Pearls")
                    .font(.body)
                    .foregroundStyle(ScyraColors.textPrimary)
                    .monospacedDigit()
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(ScyraColors.surface, in: Capsule())
            .overlay(Capsule().stroke(ScyraColors.secondaryGold.opacity(0.45), lineWidth: 1))
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Pearl balance inside The Shell: \(pearlBalance)")

            Button(action: onNotifications) {
                ZStack(alignment: .topTrailing) {
                    ScyraCanonicalIcon(systemName: "bell")
                        .font(ScyraTypography.navigationIcon)
                        .foregroundStyle(ScyraColors.primary)
                        .frame(width: ScyraSpacing.topBarTapTarget, height: ScyraSpacing.topBarTapTarget)

                    if notificationCount > 0 {
                        Text("\(min(notificationCount, 9))")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(ScyraColors.onSecondary)
                            .frame(width: 18, height: 18)
                            .background(ScyraColors.secondaryGold, in: Circle())
                            .offset(x: -2, y: 2)
                    }
                }
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Shell notifications, \(notificationCount) new")
        }
        .padding(.horizontal, 4)
        .frame(height: 64)
        .background(ScyraColors.surface)
    }
}

struct ShellPearlMiniIcon: View {
    var body: some View {
        Canvas { context, size in
            let dimension = min(size.width, size.height)
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let pearl = Path(ellipseIn: CGRect(
                x: center.x - dimension * 0.36,
                y: center.y - dimension * 0.36,
                width: dimension * 0.72,
                height: dimension * 0.72
            ))
            context.fill(pearl, with: .color(ScyraColors.onPrimary))

            let glintCenter = CGPoint(x: size.width * 0.60, y: size.height * 0.38)
            let glint = Path(ellipseIn: CGRect(
                x: glintCenter.x - dimension * 0.16,
                y: glintCenter.y - dimension * 0.16,
                width: dimension * 0.32,
                height: dimension * 0.32
            ))
            context.fill(glint, with: .color(ScyraColors.primary.opacity(0.42)))
        }
    }
}
