import SwiftUI

struct ScyraPalettePreview: View {
    private let sections: [PaletteSection] = [
        PaletteSection(
            title: "Brand",
            items: [
                PaletteItem(name: "slytherinButNiceTeal", hex: "#3F8F8B", color: ScyraColors.slytherinButNiceTeal),
                PaletteItem(name: "primary / primaryTeal", hex: "#3F8F8B", color: ScyraColors.primary),
                PaletteItem(name: "legacyRavenclawBlue", hex: "#2F4F6F legacy", color: ScyraColors.legacyRavenclawBlue),
                PaletteItem(name: "secondaryGold", hex: "#B8A56A", color: ScyraColors.secondaryGold)
            ]
        ),
        PaletteSection(
            title: "Surfaces",
            items: [
                PaletteItem(name: "background", hex: "#F2EBDD", color: ScyraColors.background),
                PaletteItem(name: "backgroundBottom", hex: "#E4D8BB", color: ScyraColors.backgroundBottom),
                PaletteItem(name: "surface", hex: "#E4D8BB", color: ScyraColors.surface),
                PaletteItem(name: "elevatedSurface", hex: "white 82%", color: ScyraColors.elevatedSurface)
            ]
        ),
        PaletteSection(
            title: "Text",
            items: [
                PaletteItem(name: "textPrimary", hex: "#000000", color: ScyraColors.textPrimary),
                PaletteItem(name: "textSecondary", hex: "black 75%", color: ScyraColors.textSecondary),
                PaletteItem(name: "textMuted", hex: "black 55%", color: ScyraColors.textMuted),
                PaletteItem(name: "textDisabled", hex: "black 35%", color: ScyraColors.textDisabled)
            ]
        ),
        PaletteSection(
            title: "Containers",
            items: [
                PaletteItem(name: "primaryContainer", hex: "#3F8F8B 16%", color: ScyraColors.primaryContainer),
                PaletteItem(name: "secondaryContainer", hex: "#B8A56A 18%", color: ScyraColors.secondaryContainer),
                PaletteItem(name: "border", hex: "#3F8F8B 20%", color: ScyraColors.border),
                PaletteItem(name: "hairline", hex: "#3F8F8B 14%", color: ScyraColors.hairline)
            ]
        ),
        PaletteSection(
            title: "Rewards / Semantic",
            items: [
                PaletteItem(name: "rewardPearl", hex: "#D9C08A", color: ScyraColors.rewardPearl),
                PaletteItem(name: "rewardMovement", hex: "#3F8F8B", color: ScyraColors.rewardMovement),
                PaletteItem(name: "rewardArc", hex: "#8C6AA8", color: ScyraColors.rewardArc),
                PaletteItem(name: "rewardSurge", hex: "#D1B45A", color: ScyraColors.rewardSurge),
                PaletteItem(name: "success", hex: "#2F8F86", color: ScyraColors.success),
                PaletteItem(name: "warning", hex: "#CC8A3E", color: ScyraColors.warning),
                PaletteItem(name: "error", hex: "#7F0909", color: ScyraColors.error)
            ]
        )
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                Text("Scyra Color Palette")
                    .font(ScyraTypography.screenTitle)
                    .foregroundStyle(ScyraColors.textPrimary)

                ForEach(sections) { section in
                    VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                        Text(section.title)
                            .font(ScyraTypography.cardTitle)
                            .foregroundStyle(ScyraColors.primary)

                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 220), spacing: ScyraSpacing.md)], spacing: ScyraSpacing.md) {
                            ForEach(section.items) { item in
                                PaletteSwatch(item: item)
                            }
                        }
                    }
                }
            }
            .padding(ScyraSpacing.lg)
        }
        .background(ScyraColors.background)
    }
}

private struct PaletteSection: Identifiable {
    let id = UUID()
    let title: String
    let items: [PaletteItem]
}

private struct PaletteItem: Identifiable {
    let id = UUID()
    let name: String
    let hex: String
    let color: Color
}

private struct PaletteSwatch: View {
    let item: PaletteItem

    var body: some View {
        HStack(spacing: ScyraSpacing.md) {
            RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous)
                .fill(item.color)
                .frame(width: 56, height: 56)
                .overlay(
                    RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous)
                        .stroke(ScyraColors.border, lineWidth: 1)
                )

            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.textPrimary)
                Text(item.hex)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
            }

            Spacer(minLength: 0)
        }
        .padding(ScyraSpacing.md)
        .background(ScyraColors.elevatedSurface)
        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous))
    }
}

#Preview("Scyra palette") {
    ScyraPalettePreview()
}
