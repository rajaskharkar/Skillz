import SwiftUI

struct FlowDetailView: View {
    let session: FlowSession?
    let chronicle: ChronicleSnapshot?
    let childPulses: [StoryPulseItem]
    let journeySuggestions: [String]
    let onCreatePulse: (UUID, String, String, String?) -> Bool
    let onOpenPulse: (UUID) -> Void
    let onEditPulse: (UUID) -> Void
    let onDeletePulse: (UUID) -> Void
    let onDeleteFlow: (UUID) -> Bool
    let backAccessibilityLabel: String
    let onBackToStory: () -> Void

    @State private var showPulseComposer = false
    @State private var pulseTitle = ""
    @State private var pulseDescription = ""
    @State private var pulseJourney = ""
    @State private var showDeleteConfirmation = false

    var body: some View {
        VStack(spacing: 0) {
            ScyraActionScreenHeader(
                title: "Flow details",
                backAccessibilityLabel: backAccessibilityLabel,
                onBack: onBackToStory
            )

            if let session {
                ScrollView {
                    VStack(spacing: 14) {
                        ScyraCard(style: .elevated) {
                            VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                                ScyraCanonicalLabel(session.isSoftMode ? "Soft flow" : "Flow", systemImage: session.isSoftMode ? "leaf" : "sparkles")
                                    .font(ScyraTypography.label)
                                    .foregroundStyle(session.isSoftMode ? ScyraColors.secondaryGold : ScyraColors.primary)
                                Text(session.title)
                                    .font(ScyraTypography.screenTitle)
                                Text(session.journeyName)
                                    .font(ScyraTypography.body)
                                    .foregroundStyle(ScyraColors.textSecondary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        if let chronicle, !chronicle.moments.isEmpty {
                            ChronicleReadView(snapshot: chronicle)
                        }

                        pulseSection(for: session)

                        Button("Delete flow", systemImage: "trash", role: .destructive) {
                            showDeleteConfirmation = true
                        }
                        .buttonStyle(.bordered)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    }
                    .padding(ScyraSpacing.screenPadding)
                }
                .background(
                    LinearGradient(
                        colors: [ScyraColors.background, ScyraColors.backgroundBottom],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .confirmationDialog("Delete Flow?", isPresented: $showDeleteConfirmation, titleVisibility: .visible) {
                    Button("Delete", role: .destructive) {
                        if onDeleteFlow(session.id) { onBackToStory() }
                    }
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("This permanently removes the Flow. Pulses captured in it remain in your Story.")
                }
            } else {
                ScyraEmptyState(
                    systemImage: "exclamationmark.triangle",
                    title: "Flow not found",
                    message: "This Flow may have been removed."
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(ScyraColors.background)
            }
        }
    }

    private func pulseSection(for session: FlowSession) -> some View {
        ScyraCard(style: .elevated) {
            VStack(alignment: .leading, spacing: ScyraSpacing.md) {
                HStack(alignment: .firstTextBaseline) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("Pulses")
                            .font(ScyraTypography.cardTitle)
                        Text("Moments captured in or added to this Flow")
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                    Spacer()
                    Text("\(childPulses.count)")
                        .font(.system(.headline, design: .rounded).weight(.bold))
                        .accessibilityLabel("\(childPulses.count) pulses")
                }

                if childPulses.isEmpty {
                    Text("No pulses yet.")
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textSecondary)
                } else {
                    ForEach(childPulses) { item in
                        HistoricalPulseRow(
                            item: item,
                            onOpen: { onOpenPulse(item.id) },
                            onEdit: { onEditPulse(item.id) },
                            onDelete: { onDeletePulse(item.id) }
                        )
                    }
                }

                Button(showPulseComposer ? "Hide Pulse Composer" : "Add Pulse") {
                    withAnimation(.easeInOut(duration: 0.2)) { showPulseComposer.toggle() }
                }
                .buttonStyle(.bordered)
                .frame(maxWidth: .infinity)
                .accessibilityLabel("Toggle pulse composer")
                .accessibilityIdentifier("flow-details-toggle-pulse-composer")

                if showPulseComposer {
                    VStack(spacing: ScyraSpacing.sm) {
                        TextField("Pulse title", text: $pulseTitle)
                            .textFieldStyle(.roundedBorder)
                        TextField("Journey (optional)", text: $pulseJourney)
                            .textFieldStyle(.roundedBorder)
                        if !journeySuggestions.isEmpty {
                            Text("Suggestions: \(journeySuggestions.prefix(6).joined(separator: " • "))")
                                .font(ScyraTypography.caption)
                                .foregroundStyle(ScyraColors.textMuted)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        TextField("Pulse description", text: $pulseDescription, axis: .vertical)
                            .lineLimit(3...6)
                            .textFieldStyle(.roundedBorder)

                        ScyraButton("Save Pulse", systemImage: "brain.head.profile") {
                            if onCreatePulse(
                                session.id,
                                pulseTitle.trimmingCharacters(in: .whitespacesAndNewlines),
                                pulseDescription.trimmingCharacters(in: .whitespacesAndNewlines),
                                pulseJourney
                            ) {
                                pulseTitle = ""
                                pulseDescription = ""
                                pulseJourney = ""
                                showPulseComposer = false
                            }
                        }
                        .disabled(
                            pulseTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                                || pulseDescription.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        )
                    }
                }
            }
        }
    }

}

private struct HistoricalPulseRow: View {
    let item: StoryPulseItem
    let onOpen: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    @State private var showDeleteConfirmation = false

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack {
                ScyraCanonicalLabel(item.pulse.title.isEmpty ? "Pulse" : item.pulse.title, systemImage: "brain.head.profile")
                    .font(ScyraTypography.label)
                Spacer()
                Text(item.pulse.createdAt.formatted(date: .omitted, time: .shortened))
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textMuted)
            }
            if let excerpt = item.chronicle.excerpt {
                Text(excerpt)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                    .lineLimit(2)
            }
            HStack {
                Button("Edit", systemImage: "square.and.pencil", action: onEdit)
                Spacer()
                Button("Delete", systemImage: "trash", role: .destructive) {
                    showDeleteConfirmation = true
                }
                Button("Details", systemImage: "chevron.right", action: onOpen)
            }
            .font(ScyraTypography.caption)
        }
        .padding(ScyraSpacing.sm)
        .background(ScyraColors.primary.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .confirmationDialog("Delete entry?", isPresented: $showDeleteConfirmation, titleVisibility: .visible) {
            Button("Delete", role: .destructive, action: onDelete)
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will permanently delete this entry.")
        }
    }
}
