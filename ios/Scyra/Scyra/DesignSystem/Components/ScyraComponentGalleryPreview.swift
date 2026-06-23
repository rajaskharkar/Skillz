import SwiftUI

struct ScyraComponentGalleryPreview: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                ScyraTopBar(selectedRoute: .home) { _ in }

                ScyraSectionHeader(title: "Reusable Scyra Components", subtitle: "Design-system-only preview coverage")

                ScyraCard(style: .elevated) {
                    VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                        Text("Manuscript Card").font(ScyraTypography.cardTitle).foregroundStyle(ScyraColors.textPrimary)
                        Text("Cards provide a quiet surface before product screens arrive.").font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                    }
                }

                VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                    ScyraSectionHeader(title: "Buttons")
                    HStack { ScyraButton("Begin Flow", systemImage: "play.fill") {}; ScyraButton("Review", variant: .secondary) {} }
                    HStack { ScyraButton("Ghost", variant: .ghost) {}; ScyraButton("Remove", variant: .destructive) {} }
                }

                VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                    ScyraSectionHeader(title: "Icon Buttons and Chips")
                    HStack { ScyraIconButton(systemImage: "book", accessibilityLabel: "Story") {}; ScyraIconButton(systemImage: "leaf.fill", accessibilityLabel: "Selected Shell", isSelected: true) {} }
                    HStack { ScyraChip("Flow", systemImage: "timer", isSelected: true); ScyraChip("Story"); ScyraChip("Movement", tint: .reward) }
                }

                HStack { ScyraStatPill(label: "Score", value: "128", systemImage: "sparkles"); ScyraStatPill(label: "Minutes", value: "24", systemImage: "clock", tint: ScyraColors.rewardMovement) }

                ScyraRewardRow(title: "Movement", value: "+12", systemImage: "figure.walk", subtitle: "Android-parity teal remains a reward tint", tint: ScyraColors.rewardMovement)

                ScyraEmptyState(systemImage: "moon.stars", title: "A calm place to begin", message: "Feature screens will use these components without adding product logic here.", actionTitle: "Preview Action") {}

                ScyraDialog(title: "Flow reward preview", message: "Dialog content can host future reward rows and simple actions.", primaryTitle: "Collect", primaryAction: {}, secondaryTitle: "Later", secondaryAction: {}) {
                    ScyraRewardRow(title: "Pearls", value: "+6", systemImage: "circle.hexagongrid", tint: ScyraColors.rewardPearl)
                }
            }
            .padding(ScyraSpacing.lg)
        }
        .background(LinearGradient(colors: [ScyraColors.background, ScyraColors.backgroundBottom], startPoint: .top, endPoint: .bottom))
    }
}

#Preview("Scyra component gallery") { ScyraComponentGalleryPreview() }
