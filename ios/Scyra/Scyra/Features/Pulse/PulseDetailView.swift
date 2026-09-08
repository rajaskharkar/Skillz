import SwiftUI

struct PulseDetailView: View {
    let pulse: Pulse?
    let chronicle: ChronicleSnapshot?
    let onEdit: (String) -> Void
    let backAccessibilityLabel: String
    let onBackToStory: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScyraActionScreenHeader(
                title: "Pulse details",
                backAccessibilityLabel: backAccessibilityLabel,
                onBack: onBackToStory
            )

            if let pulse {
                ScrollView {
                    VStack(spacing: 14) {
                        ScyraCard(style: .elevated) {
                            VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                                ScyraCanonicalLabel(pulse.journeyName ?? "Pulse", systemImage: "brain.head.profile")
                                    .font(ScyraTypography.label)
                                    .foregroundStyle(ScyraColors.primary)
                                if !pulse.title.isEmpty {
                                    Text(pulse.title).font(ScyraTypography.screenTitle)
                                }
                                Text(pulse.createdAt.formatted(date: .abbreviated, time: .shortened))
                                    .font(ScyraTypography.caption)
                                    .foregroundStyle(ScyraColors.textSecondary)
                                if pulse.parentSessionID != nil {
                                    ScyraCanonicalLabel("Attached to a Flow", systemImage: "link")
                                        .font(ScyraTypography.caption)
                                        .foregroundStyle(ScyraColors.textSecondary)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        ChronicleReadView(
                            snapshot: chronicle ?? .empty(owner: .pulse(pulse.id))
                        )

                        Button("Edit Pulse", systemImage: "square.and.pencil") {
                            onEdit(pulse.id.uuidString)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(ScyraColors.primary)
                        .frame(maxWidth: .infinity)
                    }
                    .padding(ScyraSpacing.screenPadding)
                }
                .background(ScyraColors.background)
            } else {
                ScyraEmptyState(
                    systemImage: "exclamationmark.triangle",
                    title: "Pulse not found",
                    message: "This Pulse may have been removed."
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(ScyraColors.background)
            }
        }
    }
}
