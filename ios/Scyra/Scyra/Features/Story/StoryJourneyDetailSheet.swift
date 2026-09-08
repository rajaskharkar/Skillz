import SwiftUI

struct StoryJourneyDetailSheet: View {
    let saga: StorySaga
    let sessions: [FlowSession]
    let periodTitle: String
    let onOpenFlow: (UUID) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedSessionID: UUID?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: ScyraSpacing.md) {
                    if let selectedSession {
                        sessionDetail(selectedSession)
                    } else {
                        summary
                        sessionList
                    }
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(ScyraColors.background)
            .navigationTitle(saga.journeyName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if selectedSession != nil {
                        Button("Back") { selectedSessionID = nil }
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .accessibilityLabel("Journey details")
    }

    private var selectedSession: FlowSession? {
        selectedSessionID.flatMap { id in sessions.first { $0.id == id } }
    }

    private var summary: some View {
        ScyraCard(style: .elevated) {
            VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                Text(periodTitle)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                HStack {
                    journeyStat("Flows", "\(sessions.count)")
                    Spacer()
                    journeyStat("Total time", FlowDurationFormatter.compact(sessions.reduce(0) { $0 + $1.durationMs }))
                    Spacer()
                    journeyStat("Scyra Score", "\(sessions.reduce(0) { $0 + $1.scyraPoints })")
                }
            }
        }
    }

    @ViewBuilder
    private var sessionList: some View {
        if sessions.isEmpty {
            ScyraEmptyState(
                systemImage: "sparkles",
                title: "No Flows",
                message: "No Flows for this Journey in this view."
            )
        } else {
            LazyVStack(spacing: ScyraSpacing.sm) {
                ForEach(sessions) { session in
                    Button {
                        selectedSessionID = session.id
                    } label: {
                        HStack(spacing: ScyraSpacing.md) {
                            VStack(alignment: .leading, spacing: 5) {
                                Text(session.title)
                                    .font(ScyraTypography.cardTitle)
                                Text("\(storyDuration(session.durationMs)) • \(session.createdAt.formatted(date: .abbreviated, time: .shortened))")
                                    .font(ScyraTypography.caption)
                                    .foregroundStyle(ScyraColors.textSecondary)
                            }
                            Spacer()
                            if !session.isSoftMode {
                                Text("\(session.scyraPoints)")
                                    .font(.system(.headline, design: .rounded).weight(.bold))
                                    .foregroundStyle(ScyraColors.primary)
                            }
                            ScyraCanonicalIcon(systemName: "chevron.right")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(ScyraColors.textMuted)
                        }
                        .padding(ScyraSpacing.md)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(ScyraColors.elevatedSurface)
                        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .accessibilityHint("Show Flow summary")
                }
            }
        }
    }

    private func sessionDetail(_ session: FlowSession) -> some View {
        VStack(spacing: ScyraSpacing.md) {
            ScyraCard(style: .elevated) {
                VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                    ScyraCanonicalLabel(session.isSoftMode ? "Soft Flow" : "Flow", systemImage: session.isSoftMode ? "leaf" : "sparkles")
                        .font(ScyraTypography.label)
                        .foregroundStyle(session.isSoftMode ? ScyraColors.secondaryGold : ScyraColors.primary)
                    Text(session.title)
                        .font(ScyraTypography.screenTitle)
                    Text(session.createdAt.formatted(date: .abbreviated, time: .shortened))
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textSecondary)
                    Divider()
                    Text("Duration: \(storyDuration(session.durationMs))")
                    if !session.isSoftMode {
                        Text("Scyra Score: \(session.scyraPoints)")
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            ScyraButton("Open full details", systemImage: "chevron.right") {
                dismiss()
                onOpenFlow(session.id)
            }
        }
    }

    private func journeyStat(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(label)
                .font(ScyraTypography.caption)
                .foregroundStyle(ScyraColors.textMuted)
            Text(value)
                .font(ScyraTypography.label)
                .foregroundStyle(ScyraColors.textPrimary)
        }
        .accessibilityElement(children: .combine)
    }
}
