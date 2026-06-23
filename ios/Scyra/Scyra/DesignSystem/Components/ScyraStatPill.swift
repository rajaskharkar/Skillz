import SwiftUI

struct ScyraStatPill: View {
    let label: String
    let value: String
    let systemImage: String?
    let tint: Color

    init(label: String, value: String, systemImage: String? = nil, tint: Color = ScyraColors.primary) {
        self.label = label; self.value = value; self.systemImage = systemImage; self.tint = tint
    }

    var body: some View {
        HStack(spacing: ScyraSpacing.sm) {
            if let systemImage { Image(systemName: systemImage).foregroundStyle(tint).accessibilityHidden(true) }
            VStack(alignment: .leading, spacing: 2) {
                Text(value).font(ScyraTypography.rewardNumber).foregroundStyle(ScyraColors.textPrimary)
                Text(label).font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
            }
        }
        .padding(.horizontal, ScyraSpacing.md).padding(.vertical, ScyraSpacing.sm)
        .background(tint.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.capsule, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: ScyraRadius.capsule, style: .continuous).stroke(tint.opacity(0.22), lineWidth: 1))
        .accessibilityElement(children: .combine)
    }
}

#Preview("ScyraStatPill") { HStack { ScyraStatPill(label: "Score", value: "128", systemImage: "sparkles"); ScyraStatPill(label: "Pearls", value: "+6", systemImage: "circle.hexagongrid", tint: ScyraColors.rewardPearl) }.padding().background(ScyraColors.background) }
