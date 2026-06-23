import SwiftUI

struct ScyraRewardRow: View {
    let title: String
    let value: String
    let systemImage: String?
    let assetImageName: String?
    let subtitle: String?
    let tint: Color

    init(title: String, value: String, systemImage: String, subtitle: String? = nil, tint: Color = ScyraColors.primaryManuscriptBlue) {
        self.title = title; self.value = value; self.systemImage = systemImage; self.assetImageName = nil; self.subtitle = subtitle; self.tint = tint
    }

    init(title: String, value: String, assetImageName: String, subtitle: String? = nil, tint: Color = ScyraColors.primaryManuscriptBlue) {
        self.title = title; self.value = value; self.systemImage = nil; self.assetImageName = assetImageName; self.subtitle = subtitle; self.tint = tint
    }

    var body: some View {
        HStack(spacing: ScyraSpacing.md) {
            icon.frame(width: 36, height: 36).background(Circle().fill(tint.opacity(0.14)))
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(ScyraTypography.label).foregroundStyle(ScyraColors.textPrimary)
                if let subtitle { Text(subtitle).font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary) }
            }
            Spacer(minLength: ScyraSpacing.sm)
            Text(value).font(ScyraTypography.rewardNumber).foregroundStyle(tint)
        }
        .padding(ScyraSpacing.md)
        .background(ScyraColors.elevatedSurface)
        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous).stroke(ScyraColors.hairline, lineWidth: 1))
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private var icon: some View {
        if let assetImageName { Image(assetImageName).renderingMode(.original).resizable().scaledToFit().padding(6).accessibilityHidden(true) }
        else { Image(systemName: systemImage ?? "sparkles").foregroundStyle(tint).accessibilityHidden(true) }
    }
}

#Preview("ScyraRewardRow") { VStack { ScyraRewardRow(title: "Movement", value: "+12", systemImage: "figure.walk", subtitle: "Gentle progress", tint: ScyraColors.rewardMovement); ScyraRewardRow(title: "Pearls", value: "+6", systemImage: "circle.hexagongrid", tint: ScyraColors.rewardPearl) }.padding().background(ScyraColors.background) }
