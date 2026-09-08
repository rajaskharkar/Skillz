import SwiftUI

/// Native SwiftUI rendering of Android's canonical Heart Room orbit. The drawing is code-native,
/// scales with the available width, and retains accessible buttons instead of a flattened image.
struct ShellHeartScene: View {
    let pearlBalance: Int
    let newCreatureCount: Int
    let restingCreatureCount: Int
    let newChestCount: Int
    let newBadgeCount: Int
    let onOpenLookout: () -> Void
    let onOpenVoyageHall: () -> Void
    let onOpenIdeaGrove: () -> Void
    let onOpenFocusRoom: () -> Void
    let onOpenStillwater: () -> Void
    let onOpenTheBlue: () -> Void
    let onOpenChest: () -> Void
    let onOpenBadges: () -> Void

    var body: some View {
        GeometryReader { proxy in
            let width = proxy.size.width
            ZStack {
                ShellInteriorDrawing().accessibilityHidden(true)
                roomNode("The Lookout", icon: "eye.fill", identifier: "shell-open-lookout", dormant: true, action: onOpenLookout)
                    .position(x: width * 0.50, y: 58)
                roomNode("Voyage Hall", icon: "sailboat.fill", identifier: "shell-open-voyage-hall", action: onOpenVoyageHall)
                    .position(x: width * 0.22, y: 142)
                roomNode("Idea Grove", icon: "brain.head.profile", dormant: true, action: onOpenIdeaGrove)
                    .position(x: width * 0.78, y: 142)
                heartCenter
                    .position(x: width * 0.50, y: 252)
                roomNode("Focus Room", icon: "scope", identifier: "shell-open-focus-room", action: onOpenFocusRoom)
                    .position(x: width * 0.22, y: 372)
                roomNode("Stillwater", icon: "drop.fill", identifier: "shell-open-stillwater", action: onOpenStillwater)
                    .position(x: width * 0.78, y: 372)
                roomNode(
                    "The Blue", icon: "camera.filters", identifier: "shell-open-the-blue",
                    hasIndicator: newCreatureCount > 0, action: onOpenTheBlue
                )
                .position(x: width * 0.50, y: 464)
                whisperDock
                    .frame(width: max(220, width - 32))
                    .position(x: width * 0.50, y: 536)
                shortcutDock
                    .frame(width: max(220, width - 32))
                    .position(x: width * 0.50, y: 596)
            }
        }
        .frame(height: 640)
        .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
        .accessibilityElement(children: .contain)
        .accessibilityLabel("The Shell room entrances")
    }

    private var whisperDock: some View {
        Button(action: restingCreatureCount > 0 ? onOpenChest : onOpenFocusRoom) {
            HStack(spacing: 10) {
                ScyraMaterialIcon(assetName: "materialAutoStories", size: 18, color: ScyraColors.primary)
                Text(restingCreatureCount > 0
                    ? "\(restingCreatureCount) creatures are in The Chest."
                    : "Focus has empty nooks.")
                    .lineLimit(1)
            }
            .font(ScyraTypography.caption)
            .foregroundStyle(ScyraColors.textSecondary)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, ScyraSpacing.md)
            .padding(.vertical, ScyraSpacing.sm)
            .background(ScyraColors.elevatedSurface)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(ScyraColors.secondaryGold.opacity(0.35), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityHint(restingCreatureCount > 0 ? "Opens The Chest." : "Opens Focus Room.")
    }

    private var shortcutDock: some View {
        HStack(spacing: ScyraSpacing.xl) {
            shortcut(
                visibleTitle: "The Chest", accessibilityTitle: "Chest",
                icon: "shippingbox.fill", newCount: newChestCount, action: onOpenChest
            )
            shortcut(
                visibleTitle: "Badges", accessibilityTitle: "Achievements",
                icon: "rosette", newCount: newBadgeCount, action: onOpenBadges
            )
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, ScyraSpacing.md)
        .padding(.vertical, ScyraSpacing.sm)
        .background(ScyraColors.elevatedSurface)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 28).stroke(ScyraColors.secondaryGold.opacity(0.30)))
    }

    private func shortcut(
        visibleTitle: String,
        accessibilityTitle: String,
        icon: String,
        newCount: Int,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 4) {
                ZStack(alignment: .topTrailing) {
                    ScyraCanonicalIcon(systemName: icon, size: 22)
                        .foregroundStyle(ScyraColors.primary)
                    if newCount > 0 {
                        Circle().fill(ScyraColors.secondaryGold)
                            .frame(width: 8, height: 8)
                            .offset(x: 5, y: -3)
                    }
                }
                Text(visibleTitle).font(.caption2).foregroundStyle(ScyraColors.textPrimary)
            }
            .frame(minWidth: 92)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Open \(accessibilityTitle)\(newCount > 0 ? ", \(newCount) new" : "")")
    }

    private var heartCenter: some View {
        VStack(spacing: 9) {
            ScyraCanonicalIcon(systemName: "leaf.fill", size: 28)
                .foregroundStyle(ScyraColors.onPrimary)
                .frame(width: 52, height: 52)
                .background(ScyraColors.primary)
                .clipShape(Circle())
                .accessibilityHidden(true)
            Text("The Shell").font(ScyraTypography.cardTitle.weight(.bold))
            HStack(spacing: ScyraSpacing.sm) {
                ShellPearlMiniIcon()
                    .frame(width: 18, height: 18)
                    .accessibilityHidden(true)
                Text("\(pearlBalance) Pearls")
                    .font(ScyraTypography.label)
                    .monospacedDigit()
            }
            .padding(.horizontal, ScyraSpacing.md)
            .padding(.vertical, ScyraSpacing.xs)
            .background(ScyraColors.background)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(ScyraColors.secondaryGold.opacity(0.55), lineWidth: 1))
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Pearl balance inside The Shell: \(pearlBalance)")
            .accessibilityIdentifier("shell-pearl-balance")
        }
        .padding(.horizontal, ScyraSpacing.lg)
        .padding(.vertical, ScyraSpacing.md)
        .frame(width: 214)
        .background(ScyraColors.elevatedSurface)
        .clipShape(RoundedRectangle(cornerRadius: 34, style: .continuous))
        .shadow(color: Color.black.opacity(0.08), radius: 2, y: 1)
    }

    private func roomNode(
        _ title: String,
        icon: String,
        identifier: String? = nil,
        hasIndicator: Bool = false,
        dormant: Bool = false,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            ZStack(alignment: .topTrailing) {
                ShellCardDrawing().accessibilityHidden(true)
                VStack(spacing: 5) {
                    ScyraCanonicalIcon(systemName: icon, size: 19)
                        .foregroundStyle(ScyraColors.onPrimary)
                        .frame(width: 34, height: 34)
                        .background(ScyraColors.primary.opacity(dormant ? 0.64 : 1))
                        .clipShape(Circle())
                    Text(title)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(ScyraColors.textPrimary.opacity(dormant ? 0.76 : 1))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                if hasIndicator {
                    Circle().fill(ScyraColors.secondaryGold)
                        .frame(width: 10, height: 10)
                        .padding(8)
                        .accessibilityHidden(true)
                }
            }
            .frame(width: 108, height: 84)
            .background(ScyraColors.elevatedSurface.opacity(dormant ? 0.82 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: Color.black.opacity(0.08), radius: 2, y: 1)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Open \(title)")
        .accessibilityHint(dormant ? "Opens preview" : "Active room")
        .modifier(OptionalAccessibilityIdentifier(identifier: identifier))
    }
}

private struct OptionalAccessibilityIdentifier: ViewModifier {
    let identifier: String?
    func body(content: Content) -> some View {
        if let identifier { content.accessibilityIdentifier(identifier) } else { content }
    }
}

struct ShellInteriorDrawing: View {
    var body: some View {
        GeometryReader { proxy in
            ZStack {
                RadialGradient(
                    colors: [
                        ScyraColors.secondaryGold.opacity(0.24),
                        ScyraColors.primary.opacity(0.82),
                        ScyraColors.background
                    ],
                    center: .center,
                    startRadius: 0,
                    endRadius: max(proxy.size.width, proxy.size.height) * 0.72
                )

                Canvas { context, size in
                    let width = size.width
                    let height = size.height
                    context.fill(Path(ellipseIn: CGRect(x: -width * 0.12, y: height * 0.02, width: width * 1.24, height: height * 1.10)), with: .color(ScyraColors.primary.opacity(0.24)))
                    context.fill(Path(ellipseIn: CGRect(x: width * 0.04, y: height * 0.07, width: width * 0.92, height: height * 0.86)), with: .color(ScyraColors.surface.opacity(0.16)))
                    context.fill(Path(ellipseIn: CGRect(x: width * 0.13, y: height * 0.12, width: width * 0.74, height: height * 0.68)), with: .color(ScyraColors.secondaryGold.opacity(0.16)))
                    context.fill(Path(ellipseIn: CGRect(x: width * 0.25, y: height * 0.35 - width * 0.25, width: width * 0.50, height: width * 0.50)), with: .color(ScyraColors.primary.opacity(0.18)))

                    var spine = Path()
                    spine.move(to: CGPoint(x: width * 0.5, y: height * 0.1))
                    spine.addCurve(to: CGPoint(x: width * 0.5, y: height * 0.8), control1: CGPoint(x: width * 0.46, y: height * 0.28), control2: CGPoint(x: width * 0.54, y: height * 0.48))
                    context.stroke(spine, with: .color(ScyraColors.secondaryGold.opacity(0.22)), lineWidth: 4.5)

                    for (index, fraction) in ([CGFloat(0.20), 0.34, 0.49, 0.64, 0.78]).enumerated() {
                        let y = height * fraction
                        let inset = width * (0.13 + CGFloat(index) * 0.018)
                        var band = Path()
                        band.move(to: CGPoint(x: inset, y: y))
                        band.addCurve(to: CGPoint(x: width * 0.5, y: y), control1: CGPoint(x: width * 0.30, y: y - height * 0.06), control2: CGPoint(x: width * 0.42, y: y + height * 0.035))
                        band.addCurve(to: CGPoint(x: width - inset, y: y), control1: CGPoint(x: width * 0.58, y: y + height * 0.035), control2: CGPoint(x: width * 0.70, y: y - height * 0.06))
                        context.stroke(band, with: .color(ScyraColors.secondaryGold.opacity(0.12)), lineWidth: 3)
                    }

                    context.stroke(
                        Path(ellipseIn: CGRect(x: -width * 0.08, y: height * 0.02, width: width * 1.16, height: height * 1.04)),
                        with: .color(Color.black.opacity(0.12)),
                        lineWidth: width * 0.08
                    )
                }
            }
        }
    }
}

private struct ShellCardDrawing: View {
    var body: some View {
        Canvas { context, size in
            context.fill(Path(ellipseIn: CGRect(x: size.width * 0.08, y: -size.height * 0.25, width: size.width * 0.84, height: size.height * 1.25)), with: .color(ScyraColors.secondaryGold.opacity(0.08)))
            var line = Path()
            line.move(to: CGPoint(x: size.width * 0.5, y: 0))
            line.addLine(to: CGPoint(x: size.width * 0.5, y: size.height))
            context.stroke(line, with: .color(ScyraColors.textPrimary.opacity(0.10)), lineWidth: 2)
        }
    }
}
