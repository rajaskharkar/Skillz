import SwiftUI

struct IdeaGroveView: View {
    @ObservedObject var viewModel: IdeaGroveViewModel
    let onStartFlow: (PulseLaunchContext) -> Void
    let onOpenCurrentFlow: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: ScyraSpacing.md) {
                Text(IdeaGroveStrings.title)
                    .font(ScyraTypography.screenTitle)
                    .foregroundStyle(ScyraColors.textPrimary)

                Picker("Idea Grove section", selection: $viewModel.selectedCompletedTab) {
                    Text(IdeaGroveStrings.alive).tag(false)
                    Text(IdeaGroveStrings.completed).tag(true)
                }
                .pickerStyle(.segmented)
                .accessibilityIdentifier("idea-grove-tabs")
            }
            .padding(.horizontal, ScyraSpacing.screenPadding)
            .padding(.top, ScyraSpacing.md)

            ScrollView {
                LazyVStack(spacing: 12) {
                    summaryCard

                    if !viewModel.selectedCompletedTab {
                        sortControl
                    }

                    if let notice = viewModel.notice {
                        Button(action: viewModel.clearNotice) {
                            Text(notice)
                                .font(ScyraTypography.label)
                                .foregroundStyle(ScyraColors.primary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(12)
                                .background(ScyraColors.primaryContainer)
                                .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card))
                        }
                        .buttonStyle(.plain)
                        .accessibilityHint("Dismiss")
                    }

                    if let error = viewModel.errorMessage {
                        ScyraEmptyState(
                            systemImage: "exclamationmark.triangle",
                            title: "Idea Grove unavailable",
                            message: error,
                            actionTitle: "Try again",
                            action: { viewModel.refresh() }
                        )
                    } else if displayedItems.isEmpty {
                        emptyState
                    } else {
                        ForEach(displayedItems) { item in
                            IdeaGroveCard(
                                item: item,
                                isExpanded: viewModel.expandedPulseID == item.id,
                                onToggle: { viewModel.toggleExpanded(item.id) },
                                onFlow: { startFlow(item.id) },
                                onMarkInsight: { viewModel.markAsInsight(item.id) },
                                onMarkCompleted: { viewModel.markCompleted(item.id) },
                                onRevive: { viewModel.revive(item.id) },
                                onDelete: { viewModel.requestDelete(item.id) }
                            )
                        }
                    }
                }
                .padding(ScyraSpacing.screenPadding)
                .padding(.bottom, ScyraSpacing.xl)
            }
        }
        .background(
            LinearGradient(
                colors: [
                    ScyraColors.primary,
                    ScyraColors.primary.opacity(0.72),
                    ScyraColors.background
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .onAppear { viewModel.refresh() }
        .alert(
            IdeaGroveStrings.activeFlow,
            isPresented: $viewModel.showsActiveFlowConflict
        ) {
            Button(IdeaGroveStrings.cancel, role: .cancel) {
                viewModel.dismissActiveFlowConflict()
            }
            Button(IdeaGroveStrings.viewFlow) {
                viewModel.dismissActiveFlowConflict()
                onOpenCurrentFlow()
            }
        }
        .alert(
            deleteTitle,
            isPresented: Binding(
                get: { viewModel.pendingDelete != nil },
                set: { if !$0 { viewModel.cancelDelete() } }
            )
        ) {
            Button(IdeaGroveStrings.cancel, role: .cancel, action: viewModel.cancelDelete)
            Button(IdeaGroveStrings.deletePulse, role: .destructive, action: viewModel.confirmDelete)
        } message: {
            Text(IdeaGroveStrings.deleteBody)
        }
    }

    private var displayedItems: [IdeaGroveItem] {
        viewModel.selectedCompletedTab ? viewModel.completedItems : viewModel.aliveItems
    }

    private var summaryCard: some View {
        HStack(spacing: ScyraSpacing.sm) {
            ScyraCanonicalIcon(systemName: "leaf")
                .foregroundStyle(ScyraColors.primary)
                .accessibilityHidden(true)
            Text(summary)
                .font(ScyraTypography.label)
                .foregroundStyle(ScyraColors.textPrimary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(ScyraColors.primaryContainer)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .accessibilityElement(children: .combine)
    }

    private var sortControl: some View {
        HStack {
            Menu {
                ForEach(IdeaGroveSort.allCases) { sort in
                    Button(sort.rawValue) { viewModel.sort = sort }
                }
            } label: {
                ScyraCanonicalLabel("Sort: \(viewModel.sort.rawValue)", systemImage: "arrow.up.arrow.down")
                    .font(ScyraTypography.label)
                    .padding(.horizontal, 14)
                    .frame(minHeight: 44)
                    .background(ScyraColors.elevatedSurface)
                    .clipShape(Capsule())
                    .overlay(Capsule().stroke(ScyraColors.border, lineWidth: 1))
            }
            .accessibilityLabel("Sorting Alive ideas by \(viewModel.sort.rawValue)")
            Spacer()
        }
    }

    private var emptyState: some View {
        let hasCompleted = !viewModel.completedItems.isEmpty
        let title = viewModel.selectedCompletedTab
            ? IdeaGroveStrings.noCompletedIdeas
            : (hasCompleted ? IdeaGroveStrings.noAliveIdeas : IdeaGroveStrings.noIdeas)
        let body = viewModel.selectedCompletedTab
            ? IdeaGroveStrings.completedEmptyBody
            : (hasCompleted ? IdeaGroveStrings.aliveEmptyBody : IdeaGroveStrings.createPulseHint)
        return ScyraEmptyState(systemImage: "leaf", title: title, message: body)
    }

    private var summary: String {
        let count = viewModel.selectedCompletedTab
            ? viewModel.completedPulseFlowCount
            : viewModel.totalPulseFlowCount
        let duration = viewModel.selectedCompletedTab
            ? viewModel.completedPulseFlowDurationMs
            : viewModel.totalPulseFlowDurationMs
        if count > 0 {
            return "\(IdeaGroveDurationFormatter.compact(duration)) spent across \(count) Pulse Flows"
        }
        if viewModel.selectedCompletedTab, !viewModel.completedItems.isEmpty {
            return IdeaGroveStrings.completedInsightsSummary
        }
        return viewModel.selectedCompletedTab
            ? IdeaGroveStrings.noCompletedPulseFlows
            : IdeaGroveStrings.noPulseFlows
    }

    private var deleteTitle: String {
        let title = viewModel.pendingDelete?.title.trimmingCharacters(in: .whitespacesAndNewlines)
        let displayTitle = title.flatMap { $0.isEmpty ? nil : $0 } ?? IdeaGroveStrings.untitled
        return "Delete “\(displayTitle)”?"
    }

    private func startFlow(_ pulseID: UUID) {
        if case .launch(let context) = viewModel.requestFlow(from: pulseID) {
            onStartFlow(context)
        }
    }
}

private struct IdeaGroveCard: View {
    let item: IdeaGroveItem
    let isExpanded: Bool
    let onToggle: () -> Void
    let onFlow: () -> Void
    let onMarkInsight: () -> Void
    let onMarkCompleted: () -> Void
    let onRevive: () -> Void
    let onDelete: () -> Void
    @State private var showsAllFlows = false

    var body: some View {
        ScyraCard(style: .elevated, padding: 18) {
            VStack(alignment: .leading, spacing: 10) {
                Button(action: onToggle) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .firstTextBaseline) {
                            Text(displayTitle)
                                .font(ScyraTypography.cardTitle)
                                .foregroundStyle(ScyraColors.textPrimary)
                            Spacer()
                            ScyraCanonicalIcon(systemName: "chevron.down")
                                .rotationEffect(.degrees(isExpanded ? 180 : 0))
                                .foregroundStyle(ScyraColors.textMuted)
                        }
                        if let journey = item.journeyName, !journey.isEmpty {
                            Text(journey)
                                .font(ScyraTypography.body)
                                .foregroundStyle(ScyraColors.primary)
                        }
                        Text(primarySummary)
                            .font(ScyraTypography.label)
                            .foregroundStyle(ScyraColors.textPrimary)
                        Text(dateSummary)
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(cardAccessibilityLabel)
                .accessibilityValue(isExpanded ? "Expanded" : "Collapsed")

                if isExpanded {
                    if !item.description.isEmpty {
                        Text(item.description)
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.textPrimary)
                    }

                    ViewThatFits(in: .horizontal) {
                        HStack(spacing: ScyraSpacing.sm) { actionButtons }
                        VStack(alignment: .leading, spacing: ScyraSpacing.sm) { actionButtons }
                    }

                    if item.type == .rawPulse {
                        Text(IdeaGroveStrings.noFlowsFromPulse).font(ScyraTypography.body)
                        Text(IdeaGroveStrings.startFlowHint)
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                    } else if item.type == .insight {
                        Text(IdeaGroveStrings.insightBody).font(ScyraTypography.body)
                    } else if !item.flows.isEmpty {
                        flowHistory
                    }
                }
            }
        }
        .accessibilityIdentifier("idea-grove-card-\(item.id.uuidString)")
    }

    @ViewBuilder
    private var actionButtons: some View {
        switch item.type {
        case .rawPulse:
            ScyraButton(IdeaGroveStrings.flow, systemImage: "sparkles", action: onFlow)
                .accessibilityLabel("Start Flow from \(displayTitle)")
            ScyraButton(IdeaGroveStrings.markInsight, variant: .secondary, action: onMarkInsight)
        case .idea:
            ScyraButton(IdeaGroveStrings.flow, systemImage: "sparkles", action: onFlow)
                .accessibilityLabel("Start Flow from \(displayTitle)")
            ScyraButton(IdeaGroveStrings.markCompleted, variant: .secondary, action: onMarkCompleted)
        case .insight, .completedIdea:
            ScyraButton(IdeaGroveStrings.revive, systemImage: "leaf", action: onRevive)
        }
        ScyraButton(IdeaGroveStrings.deletePulse, systemImage: "trash", variant: .destructive, action: onDelete)
            .accessibilityLabel("Delete Pulse \(displayTitle)")
    }

    private var flowHistory: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(IdeaGroveStrings.flowHistory)
                .font(ScyraTypography.label)
                .fontWeight(.bold)
            ForEach(visibleFlows) { flow in
                VStack(alignment: .leading, spacing: 3) {
                    Text(flow.title)
                        .font(ScyraTypography.label)
                    Text([flow.journeyName, IdeaGroveDurationFormatter.compact(flow.durationMs), shortDate(flow.endTime)]
                        .compactMap { $0 }
                        .joined(separator: " · "))
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textSecondary)
                    if !flow.description.isEmpty {
                        Text(flow.description)
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                            .lineLimit(3)
                    }
                }
            }
            if item.flows.count > 5 {
                Button(showsAllFlows ? IdeaGroveStrings.showLess : IdeaGroveStrings.showMore) {
                    showsAllFlows.toggle()
                }
                .buttonStyle(.bordered)
                .tint(ScyraColors.primary)
            }
        }
    }

    private var visibleFlows: [IdeaGroveFlow] {
        showsAllFlows ? item.flows : Array(item.flows.prefix(5))
    }

    private var displayTitle: String {
        item.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? IdeaGroveStrings.untitled
            : item.title
    }

    private var primarySummary: String {
        switch item.type {
        case .rawPulse: IdeaGroveStrings.noFlowsYet
        case .insight: IdeaGroveStrings.insight
        case .idea, .completedIdea:
            "\(item.flowCount) \(item.flowCount == 1 ? "Flow" : "Flows") · \(IdeaGroveDurationFormatter.compact(item.totalFlowDurationMs))"
        }
    }

    private var dateSummary: String {
        switch item.type {
        case .rawPulse: "Created \(shortDate(item.createdAt))"
        case .idea: "Last worked \(shortDate(item.lastWorkedAt ?? item.updatedAt))"
        case .insight, .completedIdea: "Completed \(shortDate(item.groveStatusChangedAt ?? item.updatedAt))"
        }
    }

    private var cardAccessibilityLabel: String {
        let journey = item.journeyName.map { "Journey \($0)." } ?? ""
        let status: String = switch item.type {
        case .rawPulse: "No Flows yet."
        case .insight: "Marked as Insight."
        case .idea, .completedIdea:
            "\(item.flowCount) \(item.flowCount == 1 ? "Flow" : "Flows"), \(IdeaGroveDurationFormatter.spoken(item.totalFlowDurationMs)) spent."
        }
        return "\(displayTitle). \(journey) \(status) \(dateSummary)."
    }

    private func shortDate(_ date: Date) -> String {
        date.formatted(.dateTime.month(.abbreviated).day())
    }
}

#Preview {
    IdeaGroveView(
        viewModel: IdeaGroveViewModel(repository: InMemoryFlowRepository()),
        onStartFlow: { _ in },
        onOpenCurrentFlow: {}
    )
}
