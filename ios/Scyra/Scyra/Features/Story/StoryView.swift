import Foundation
import SwiftUI

struct StoryView: View {
    @ObservedObject var viewModel: StoryViewModel
    @ObservedObject var preferences: AppPreferencesModel
    let onOpenPulse: () -> Void
    let onOpenFlow: () -> Void
    let onOpenFlowDetail: (String) -> Void
    let onOpenPulseDetail: (String) -> Void
    let onEditPulse: (String) -> Void

    @State private var selectedTab: StoryTab = .chronicles
    @State private var expandedArcIDs = Set<UUID>()
    @State private var expandedPulseIDs = Set<UUID>()
    @State private var activeSheet: StorySheet?

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                journeyFilters
                periodNavigator

                if viewModel.totalDurationMs > 0 {
                    timeSummary
                }

                if preferences.showScoreUI {
                    scoreDisplay
                }

                Divider()

                tabPicker

                if let error = viewModel.errorMessage {
                    ScyraCard {
                        Text(error)
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.error)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                } else {
                    tabContent
                }
            }
            .padding(.horizontal, ScyraSpacing.screenPadding)
            .padding(.top, ScyraSpacing.sm)
            .padding(.bottom, 104)
        }
        .background(
            ScyraColors.background
        )
        .safeAreaInset(edge: .bottom, alignment: .trailing, spacing: 0) {
            ScyraFloatingActionDock {
                StoryFloatingActionButton("Pulse", iconAsset: "materialPsychologyAlt") {
                    onOpenPulse()
                }
                .accessibilityLabel("Open Pulse")

                StoryFloatingActionButton("Flow", iconAsset: "materialAutoAwesome") {
                    onOpenFlow()
                }
                .accessibilityLabel("Open Flow")
            }
            .padding(.trailing, ScyraSpacing.screenPadding)
            .padding(.bottom, ScyraSpacing.md)
        }
        .onAppear(perform: viewModel.refresh)
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .journey(let saga):
                StoryJourneyDetailSheet(
                    saga: saga,
                    sessions: viewModel.sessions(forJourney: saga.journeyName),
                    periodTitle: periodTitle,
                    onOpenFlow: { onOpenFlowDetail($0.uuidString) }
                )
            case .arc(let arcID, let metadata):
                ArcMetadataEditorView(
                    arcID: arcID,
                    metadata: metadata,
                    onSave: viewModel.saveArcMetadata
                )
            }
        }
    }

    @ViewBuilder
    private var journeyFilters: some View {
        if !viewModel.journeys.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text(viewModel.selectedJourneys.isEmpty
                    ? StoryStrings.observeJourneys
                    : "Observe journeys (\(viewModel.selectedJourneys.count) selected):")
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: ScyraSpacing.sm) {
                        ScyraChip(StoryStrings.all, isSelected: viewModel.selectedJourneys.isEmpty, tint: .secondary) {
                            viewModel.clearJourneyFilters()
                        }
                        .accessibilityLabel("All journeys")

                        ForEach(viewModel.journeys, id: \.self) { journey in
                            ScyraChip(journey, isSelected: viewModel.selectedJourneys.contains(journey), tint: .secondary) {
                                viewModel.toggleJourney(journey)
                            }
                            .accessibilityLabel("Filter by journey \(journey)")
                        }
                    }
                    .padding(.horizontal, 1)
                }
            }
            .accessibilityElement(children: .contain)
        }
    }

    private var periodNavigator: some View {
        VStack(spacing: 10) {
                HStack(spacing: ScyraSpacing.sm) {
                    ForEach(StoryPeriod.allCases) { period in
                        ScyraChip(period.rawValue, isSelected: viewModel.period == period, tint: .secondary) {
                            viewModel.selectPeriod(period)
                        }
                        .accessibilityValue(viewModel.period == period ? "Selected" : "Not selected")
                    }
                    Spacer(minLength: 0)
                }

                HStack {
                    Button(action: viewModel.goPrevious) {
                        ScyraCanonicalIcon(systemName: "chevron.left")
                            .frame(width: 44, height: 44)
                    }
                    .disabled(!viewModel.canGoPrevious)
                    .accessibilityLabel("Previous")

                    VStack(spacing: 2) {
                        Text(periodTitle)
                            .font(.headline.weight(.semibold))
                        Text(periodSubtitle)
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                    .multilineTextAlignment(.center)
                    .accessibilityElement(children: .combine)

                    Button(action: viewModel.goNext) {
                        ScyraCanonicalIcon(systemName: "chevron.right")
                            .frame(width: 44, height: 44)
                    }
                    .disabled(!viewModel.canGoNext)
                    .accessibilityLabel("Next")
                }

                if !viewModel.isCurrentPeriod {
                    Button(currentPeriodButtonTitle, action: viewModel.goCurrent)
                        .buttonStyle(.borderedProminent)
                        .tint(ScyraColors.secondaryGold)
                }
        }
    }

    private var timeSummary: some View {
        Group {
            if viewModel.selectedJourneys.isEmpty {
                HStack(spacing: ScyraSpacing.sm) {
                    Text(StoryStrings.timeInView)
                        .font(.caption2)
                        .foregroundStyle(ScyraColors.textMuted)
                    Text(FlowDurationFormatter.compact(viewModel.totalDurationMs))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(ScyraColors.textPrimary)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(ScyraColors.surface.opacity(0.65), in: Capsule())
                .frame(maxWidth: .infinity)
            } else {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(StoryStrings.totalTime)
                            .font(.caption2)
                        Text(StoryStrings.selectedJourneyTime)
                            .font(.caption)
                            .opacity(0.75)
                    }
                    Spacer()
                    Text(FlowDurationFormatter.compact(viewModel.totalDurationMs))
                        .font(.title3.weight(.semibold))
                }
                .foregroundStyle(ScyraColors.onSecondary)
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .background(ScyraColors.secondaryGold, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
        }
        .accessibilityElement(children: .combine)
    }

    private var scoreDisplay: some View {
        VStack(spacing: 6) {
            Text("\(viewModel.currentScore)")
                .font(.system(size: 70))
                .foregroundStyle(ScyraColors.textPrimary)

            if !preferences.calmMode, viewModel.currentSurgeScore > 0 {
                Text(StoryStrings.surgeBonus(viewModel.currentSurgeScore))
                    .font(ScyraTypography.cardTitle)
                    .foregroundStyle(ScyraColors.rewardSurge)
            }

            Text(StoryStrings.scorePeriodLabel(for: viewModel.period))
                .font(ScyraTypography.handwrittenLabelResolved)
                .foregroundStyle(ScyraColors.textMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(80)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(StoryStrings.scoreAccessibilityLabel(
            score: viewModel.currentScore,
            surgeScore: preferences.calmMode ? 0 : viewModel.currentSurgeScore,
            period: viewModel.period
        ))
        .accessibilityIdentifier("story-score-display")
        .accessibilityAddTraits(.isHeader)
    }

    private var tabPicker: some View {
        HStack(spacing: 0) {
            storyTabButton(.sagas, image: "materialMenuBook", label: StoryStrings.sagas)
            storyTabButton(.chronicles, image: "materialTimeline", label: StoryStrings.chronicles)
        }
        .padding(3)
        .frame(height: 40)
        .background(ScyraColors.surfaceVariant, in: Capsule())
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
    }

    private func storyTabButton(_ tab: StoryTab, image: String, label: String) -> some View {
        let selected = selectedTab == tab
        return Button {
            withAnimation(.easeInOut(duration: 0.18)) { selectedTab = tab }
        } label: {
            ScyraMaterialIcon(
                assetName: image,
                size: 18,
                color: selected ? ScyraColors.secondaryGold : ScyraColors.textMuted
            )
                .frame(width: 54)
                .frame(maxHeight: .infinity)
                .background(selected ? ScyraColors.surface : Color.clear, in: Capsule())
                .shadow(color: selected ? Color.black.opacity(0.10) : .clear, radius: 1, y: 1)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityValue(selected ? "Selected" : "Not selected")
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    @ViewBuilder
    private var tabContent: some View {
        switch selectedTab {
        case .sagas:
            sagasContent
        case .chronicles:
            chroniclesContent
        }
    }

    @ViewBuilder
    private var sagasContent: some View {
        VStack(spacing: 12) {
            if !viewModel.visiblePulses.isEmpty {
                StorySagaPulseSection(pulses: viewModel.visiblePulses)
            }

            if viewModel.sagas.isEmpty {
                if viewModel.visiblePulses.isEmpty {
                    StoryEmptySagasState()
                }
            } else {
                StorySagasCard(
                    period: viewModel.period,
                    isCurrentPeriod: viewModel.isCurrentPeriod,
                    sagas: viewModel.sagas,
                    onOpenJourney: { activeSheet = .journey($0) }
                )
            }
        }
    }

    @ViewBuilder
    private var chroniclesContent: some View {
        if viewModel.chronicleItems.isEmpty {
            emptyState
        } else {
            ForEach(viewModel.chronicleItems) { item in
                switch item {
                case .flow(let flow, let childPulses):
                    VStack(spacing: ScyraSpacing.sm) {
                        StoryFlowCard(
                            flow: flow,
                            chronicle: viewModel.chronicleForSession(id: flow.id),
                            movement: viewModel.movementForSession(id: flow.id),
                            showScoreUI: preferences.showScoreUI,
                            calmMode: preferences.calmMode,
                            onDelete: { viewModel.deleteSession(id: flow.id) }
                        ) {
                            onOpenFlowDetail(flow.id.uuidString)
                        }
                        ForEach(childPulses) { pulse in
                            StoryPulseCard(
                                item: pulse,
                                isExpanded: expandedPulseIDs.contains(pulse.id),
                                nested: true,
                                parentFlow: flow,
                                onToggle: { togglePulse(pulse.id) },
                                onOpen: { onOpenPulseDetail(pulse.id.uuidString) },
                                onEdit: { onEditPulse(pulse.id.uuidString) },
                                onDelete: { viewModel.deletePulse(id: pulse.id) }
                            )
                        }
                    }
                case .pulse(let pulse):
                    StoryPulseCard(
                        item: pulse,
                        isExpanded: expandedPulseIDs.contains(pulse.id),
                        nested: false,
                        parentFlow: nil,
                        onToggle: { togglePulse(pulse.id) },
                        onOpen: { onOpenPulseDetail(pulse.id.uuidString) },
                        onEdit: { onEditPulse(pulse.id.uuidString) },
                        onDelete: { viewModel.deletePulse(id: pulse.id) }
                    )
                case .arc(let group):
                    StoryArcCard(
                        group: group,
                        isExpanded: expandedArcIDs.contains(group.id),
                        showScoreUI: preferences.showScoreUI,
                        calmMode: preferences.calmMode,
                        onToggle: { toggleArc(group.id) },
                        onOpenFlow: { onOpenFlowDetail($0.id.uuidString) },
                        onDeleteFlow: { viewModel.deleteSession(id: $0.id) },
                        onOpenPulse: { onOpenPulseDetail($0.id.uuidString) },
                        onEditPulse: { onEditPulse($0.id.uuidString) },
                        onDeletePulse: { viewModel.deletePulse(id: $0.id) },
                        onEditDetails: {
                            activeSheet = .arc(
                                id: group.id,
                                metadata: group.metadata ?? ArcMetadata(arcID: group.id)
                            )
                        }
                    )
                }
            }
        }
    }

    @ViewBuilder
    private var emptyState: some View {
        if viewModel.allSessions.isEmpty, viewModel.allPulses.isEmpty, viewModel.isCurrentPeriod {
            VStack(spacing: 14) {
                Text(StoryStrings.firstFlow)
                    .font(.subheadline)
                    .foregroundStyle(ScyraColors.textSecondary)
                    .accessibilityAddTraits(.isHeader)

                Button(action: onOpenFlow) {
                    Text(StoryStrings.startStory)
                        .font(.body.weight(.semibold))
                        .foregroundStyle(ScyraColors.onPrimary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(ScyraColors.primary, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Start your Story")
            }
            .padding(.horizontal, 2)
            .padding(.vertical, 6)
            .accessibilityElement(children: .contain)
            .accessibilityLabel(StoryStrings.firstFlow)
        } else {
            VStack(spacing: 8) {
                ScyraMaterialIcon(assetName: "materialTimeline", size: 24, color: ScyraColors.textMuted)
                Text(viewModel.isCurrentPeriod ? StoryStrings.noFlowsCurrent : StoryStrings.noFlowsPast)
                    .font(.body.weight(.semibold))
                    .accessibilityAddTraits(.isHeader)
                Text(emptySubtitle)
                    .font(.subheadline)
                    .foregroundStyle(ScyraColors.textSecondary)
                    .multilineTextAlignment(.center)
                if !viewModel.isCurrentPeriod {
                    Button(StoryStrings.goToday, action: viewModel.goCurrent)
                }
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 22)
            .frame(maxWidth: .infinity)
            .background(ScyraColors.surfaceVariant.opacity(0.35), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .accessibilityElement(children: .combine)
        }
    }

    private var emptySubtitle: String {
        if viewModel.allSessions.isEmpty, viewModel.allPulses.isEmpty, viewModel.isCurrentPeriod {
            return StoryStrings.firstFlow
        }
        return switch (viewModel.period, viewModel.isCurrentPeriod) {
        case (.day, true): "No Flows have been recorded today."
        case (.day, false): "No Flows were recorded for this day."
        case (.week, true): "No Flows have been recorded this week."
        case (.week, false): "No Flows were recorded for this week."
        case (.month, true): "No Flows have been recorded this month."
        case (.month, false): "No Flows were recorded for this month."
        }
    }

    private var periodTitle: String {
        let start = viewModel.window.start
        return switch viewModel.period {
        case .day:
            start.formatted(.dateTime.weekday(.abbreviated).month(.abbreviated).day())
        case .week:
            "Week of \(start.formatted(.dateTime.month(.abbreviated).day()))"
        case .month:
            start.formatted(.dateTime.month(.wide).year())
        }
    }

    private var periodSubtitle: String {
        switch viewModel.period {
        case .day:
            "Sessions in this day"
        case .week:
            "\(viewModel.window.start.formatted(.dateTime.month(.abbreviated).day())) – \(viewModel.window.end.addingTimeInterval(-1).formatted(.dateTime.month(.abbreviated).day()))"
        case .month:
            "Sessions in this month"
        }
    }

    private var currentPeriodButtonTitle: String {
        switch viewModel.period {
        case .day: "Back to Today"
        case .week: "Back to This Week"
        case .month: "Back to This Month"
        }
    }

    private func toggleArc(_ id: UUID) {
        if expandedArcIDs.contains(id) { expandedArcIDs.remove(id) }
        else { expandedArcIDs.insert(id) }
    }

    private func togglePulse(_ id: UUID) {
        if expandedPulseIDs.contains(id) { expandedPulseIDs.remove(id) }
        else { expandedPulseIDs.insert(id) }
    }
}

private enum StoryTab: Hashable {
    case sagas
    case chronicles
}

private struct StoryFloatingActionButton: View {
    let title: String
    let iconAsset: String
    let action: () -> Void

    init(_ title: String, iconAsset: String, action: @escaping () -> Void) {
        self.title = title
        self.iconAsset = iconAsset
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                ScyraMaterialIcon(assetName: iconAsset, size: 20, color: ScyraColors.onPrimary)
                Text(title)
            }
                .font(.subheadline.weight(.medium))
                .foregroundStyle(ScyraColors.onPrimary)
                .padding(.horizontal, 20)
                .frame(height: 56)
                .background(ScyraColors.primary, in: Capsule())
                .shadow(color: Color.black.opacity(0.22), radius: 3, y: 2)
        }
        .buttonStyle(.plain)
    }
}

private struct StorySagaPulseSection: View {
    let pulses: [Pulse]
    @State private var isExpanded = false

    var body: some View {
        Button {
            withAnimation(.easeInOut(duration: 0.2)) { isExpanded.toggle() }
        } label: {
            VStack(spacing: 10) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Pulses")
                            .font(.subheadline.weight(.semibold))
                        Text("Moments captured in this chapter")
                            .font(.caption)
                            .foregroundStyle(ScyraColors.textPrimary.opacity(0.72))
                    }
                    Spacer()
                    HStack(spacing: 2) {
                        Text("\(pulses.count)")
                            .font(.body.weight(.semibold))
                        ScyraCanonicalIcon(systemName: isExpanded ? "chevron.down" : "chevron.right")
                    }
                }

                if isExpanded {
                    VStack(spacing: 8) {
                        ForEach(pulses.sorted { $0.createdAt > $1.createdAt }) { pulse in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(pulse.title.isEmpty ? "Pulse" : pulse.title)
                                    .font(.subheadline.weight(.semibold))
                                if !pulse.description.isEmpty {
                                    Text(pulse.description)
                                        .font(.caption)
                                        .lineLimit(2)
                                }
                                Text(tagName(for: pulse))
                                    .font(.caption2)
                                    .foregroundStyle(ScyraColors.onPrimary.opacity(0.82))
                            }
                            .foregroundStyle(ScyraColors.onPrimary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(14)
                            .background(ScyraColors.primary, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                        }
                    }
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity)
            .background(ScyraColors.primary.opacity(0.10), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .contentShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(isExpanded ? "Collapse pulses" : "Expand pulses")
        .accessibilityValue("Pulses, \(pulses.count) pulses")
    }

    private func tagName(for pulse: Pulse) -> String {
        guard let journeyName = pulse.journeyName, !journeyName.isEmpty else { return "Untagged" }
        return journeyName
    }
}

private struct StorySagasCard: View {
    let period: StoryPeriod
    let isCurrentPeriod: Bool
    let sagas: [StorySaga]
    let onOpenJourney: (StorySaga) -> Void

    @State private var isExpanded = true

    private var totalFlows: Int { sagas.reduce(0) { $0 + $1.flowCount } }
    private var totalDuration: Int64 { sagas.reduce(0) { $0 + $1.totalDurationMs } }
    private var totalScore: Int { sagas.reduce(0) { $0 + $1.totalScore } }

    var body: some View {
        VStack(spacing: 12) {
            HStack(alignment: .top, spacing: 8) {
                StorySagaHeader(
                    period: period,
                    isCurrentPeriod: isCurrentPeriod,
                    totalFlows: totalFlows,
                    totalDuration: totalDuration,
                    totalScore: totalScore
                )
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) { isExpanded.toggle() }
                } label: {
                    ScyraCanonicalIcon(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundStyle(ScyraColors.textPrimary)
                        .frame(width: 40, height: 40)
                        .background(ScyraColors.surfaceVariant, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(isExpanded ? "Collapse" : "Expand")
                .accessibilityValue(isExpanded ? "Expanded" : "Collapsed")
            }

            if isExpanded {
                VStack(spacing: 10) {
                    ForEach(Array(sagas.enumerated()), id: \.element.id) { index, saga in
                        StorySagaJourneyRow(rank: index + 1, saga: saga) {
                            onOpenJourney(saga)
                        }
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity)
        .background(ScyraColors.surface, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(ScyraColors.textPrimary.opacity(0.07), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.08), radius: 2, y: 1)
    }
}

private struct StorySagaHeader: View {
    let period: StoryPeriod
    let isCurrentPeriod: Bool
    let totalFlows: Int
    let totalDuration: Int64
    let totalScore: Int

    var body: some View {
        VStack(spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 8) {
                        Text("📜").font(.body)
                        Text("Your Saga").font(.body.weight(.semibold))
                    }
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(ScyraColors.textPrimary.opacity(0.65))
                }
                Spacer()
                Text(period.rawValue)
                    .font(.caption2)
                    .foregroundStyle(ScyraColors.textPrimary.opacity(0.9))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(ScyraColors.surfaceVariant, in: Capsule())
            }

            HStack {
                StorySagaHeaderStat(label: "FLOWS", value: "\(totalFlows)")
                Spacer()
                StorySagaHeaderStat(label: "DURATION", value: storyDuration(totalDuration))
                Spacer()
                StorySagaHeaderStat(label: "SCORE", value: "🔥 \(totalScore)", alignment: .trailing)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(ScyraColors.surfaceVariant, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Your Saga. \(subtitle). \(period.rawValue). Flows: \(totalFlows). Duration: \(storyDuration(totalDuration)). Score: \(totalScore).")
    }

    private var subtitle: String {
        switch (period, isCurrentPeriod) {
        case (.day, true): "Record for today"
        case (.day, false): "Record for this day"
        case (.week, true): "Record for this week"
        case (.week, false): "Record for the week"
        case (.month, true): "Record for this month"
        case (.month, false): "Record for the month"
        }
    }
}

private struct StorySagaHeaderStat: View {
    let label: String
    let value: String
    var alignment: HorizontalAlignment = .leading

    var body: some View {
        VStack(alignment: alignment, spacing: 2) {
            Text(label)
                .font(.caption2)
                .foregroundStyle(ScyraColors.textPrimary.opacity(0.70))
            Text(value)
                .font(.caption.weight(.semibold))
                .foregroundStyle(ScyraColors.textPrimary)
        }
    }
}

private struct StorySagaJourneyRow: View {
    let rank: Int
    let saga: StorySaga
    let onOpen: () -> Void

    private var accentOpacity: Double {
        switch rank {
        case 1: 0.28
        case 2: 0.22
        case 3: 0.18
        default: 0.14
        }
    }

    var body: some View {
        Button(action: onOpen) {
            HStack(spacing: 12) {
                Capsule()
                    .fill(ScyraColors.secondaryGold.opacity(accentOpacity))
                    .frame(width: 6, height: 44)

                Text("#\(rank)")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(ScyraColors.secondaryGold)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(ScyraColors.secondaryGold.opacity(0.14), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(ScyraColors.secondaryGold.opacity(0.22), lineWidth: 1)
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text(saga.journeyName)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(ScyraColors.textPrimary)
                        .lineLimit(1)
                    Text("\(saga.flowCount) \(saga.flowCount == 1 ? "flow" : "flows") • \(storyDuration(saga.totalDurationMs))")
                        .font(.caption2)
                        .foregroundStyle(ScyraColors.textPrimary.opacity(0.78))
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
                VStack(alignment: .trailing, spacing: 8) {
                    HStack(spacing: 6) {
                        Text("🔥")
                        Text("\(saga.totalScore)")
                            .font(.subheadline.weight(.semibold))
                    }
                    .foregroundStyle(ScyraColors.textPrimary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(ScyraColors.secondaryGold.opacity(0.12), in: Capsule())
                    .overlay(Capsule().stroke(ScyraColors.secondaryGold.opacity(0.18), lineWidth: 1))
                    ScyraCanonicalIcon(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(ScyraColors.textPrimary.opacity(0.45))
                }
            }
            .padding(.leading, 12)
            .padding(.trailing, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(ScyraColors.surfaceVariant, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(ScyraColors.textPrimary.opacity(0.06), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(saga.journeyName). Rank \(rank). \(saga.flowCount) flows, \(storyDuration(saga.totalDurationMs)). Score: \(saga.totalScore).")
        .accessibilityHint("Open journey details")
    }
}

private struct StoryEmptySagasState: View {
    var body: some View {
        VStack(spacing: 8) {
            ScyraMaterialIcon(assetName: "materialMenuBook", size: 24, color: ScyraColors.textMuted)
            Text("No Sagas in this view")
                .font(.body.weight(.semibold))
            Text("There isn’t enough recorded Flow data to build saga stats here yet.")
                .font(.subheadline)
                .foregroundStyle(ScyraColors.textSecondary)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 22)
        .frame(maxWidth: .infinity)
        .background(ScyraColors.surfaceVariant.opacity(0.35), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("No Sagas in this view. There isn’t enough recorded Flow data to build saga stats here yet.")
    }
}

private enum StorySheet: Identifiable {
    case journey(StorySaga)
    case arc(id: UUID, metadata: ArcMetadata)

    var id: String {
        switch self {
        case .journey(let saga): "journey-\(saga.id)"
        case .arc(let id, _): "arc-\(id.uuidString)"
        }
    }
}

private struct StoryFlowCard: View {
    let flow: FlowSession
    var chronicle: ChronicleSnapshot? = nil
    var movement: FlowHealthSnapshot? = nil
    let showScoreUI: Bool
    let calmMode: Bool
    let onDelete: () -> Void
    let onOpen: () -> Void

    @State private var showDeleteConfirmation = false

    var body: some View {
        Button(action: onOpen) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        ScyraCanonicalLabel(flow.journeyName, systemImage: flow.isSoftMode ? "leaf" : "sparkles")
                            .font(ScyraTypography.label)
                            .foregroundStyle(flow.isSoftMode ? ScyraColors.secondaryGold : journeyColor(flow.journeyName))
                        Text(flow.title)
                            .font(ScyraTypography.cardTitle)
                            .foregroundStyle(ScyraColors.textPrimary)
                            .multilineTextAlignment(.leading)
                    }
                    Spacer()
                    if !flow.isSoftMode, showScoreUI, !calmMode, flow.surgePoints > 0 {
                        Text("⚡ +\(flow.surgePoints)")
                            .font(.system(.headline, design: .rounded).weight(.heavy))
                            .foregroundStyle(ScyraColors.rewardSurge)
                    }
                    ScyraCanonicalIcon(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(ScyraColors.textMuted)
                }

                Divider()

                if let excerpt = chronicle?.excerpt {
                    Text(excerpt)
                        .font(ScyraTypography.body)
                        .foregroundStyle(ScyraColors.textSecondary)
                        .lineLimit(3)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                if let mediaItems = chronicle?.mediaItems, !mediaItems.isEmpty {
                    ChronicleMediaMomentView(
                        items: mediaItems,
                        compact: true,
                        allowsOpening: false
                    )
                }

                Text("Duration: \(storyDuration(flow.durationMs))")
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                Text(flow.createdAt.formatted(date: .omitted, time: .shortened))
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textMuted)

                if !flow.isSoftMode, showScoreUI, !calmMode {
                    HStack(spacing: ScyraSpacing.md) {
                        Text("Scyra Score: \(flow.scyraPoints)")
                        if let multiplier = flow.arcMultiplierUsed {
                            Text("×\(multiplier.formatted(.number.precision(.fractionLength(1))))")
                        }
                    }
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.textSecondary)
                }

                if let movement {
                    FlowMovementLine(snapshot: movement, showsSyncStatus: false)
                }
            }
            .padding(ScyraSpacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(flow.isSoftMode ? ScyraColors.secondaryContainer : journeyColor(flow.journeyName).opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(ScyraColors.hairline, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button("Flow details", systemImage: "square.and.pencil", action: onOpen)
            Button("Delete flow", systemImage: "trash", role: .destructive) {
                showDeleteConfirmation = true
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(flowAccessibilityLabel)
        .accessibilityHint("Open flow details")
        .accessibilityAction(named: "Delete flow") { showDeleteConfirmation = true }
        .confirmationDialog("Delete Flow?", isPresented: $showDeleteConfirmation, titleVisibility: .visible) {
            Button("Delete", role: .destructive, action: onDelete)
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This permanently removes the Flow. Pulses captured in it remain in your Story.")
        }
    }

    private var flowAccessibilityLabel: String {
        var parts = [flow.title, flow.isSoftMode ? "Soft flow" : "Flow", flow.journeyName, storyDuration(flow.durationMs)]
        if !flow.isSoftMode, showScoreUI, !calmMode { parts.append("Scyra Score \(flow.scyraPoints)") }
        if let movement, movement.rawMovementPoints > 0 {
            parts.append("Movement plus \(movement.rawMovementPoints)")
        }
        return parts.joined(separator: ", ")
    }
}

private struct StoryArcCard: View {
    let group: StoryArcGroup
    let isExpanded: Bool
    let showScoreUI: Bool
    let calmMode: Bool
    let onToggle: () -> Void
    let onOpenFlow: (FlowSession) -> Void
    let onDeleteFlow: (FlowSession) -> Void
    let onOpenPulse: (Pulse) -> Void
    let onEditPulse: (Pulse) -> Void
    let onDeletePulse: (Pulse) -> Void
    let onEditDetails: () -> Void

    @State private var expandedPulseIDs = Set<UUID>()

    var body: some View {
        VStack(spacing: 10) {
            Button(action: onToggle) {
                VStack(alignment: .leading, spacing: 12) {
                    if let title = group.metadata?.title {
                        Text(title)
                            .font(ScyraTypography.cardTitle)
                            .foregroundStyle(ScyraColors.textPrimary)
                            .lineLimit(isExpanded ? 2 : 1)
                    }
                    if let summary = group.metadata?.summary {
                        Text(summary)
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.textSecondary)
                            .lineLimit(isExpanded ? 6 : 2)
                    }
                    HStack {
                        ScyraCanonicalLabel("\(group.totalFlowCount) \(group.totalFlowCount == 1 ? "Flow" : "Flows")", systemImage: "chart.line.uptrend.xyaxis")
                            .font(ScyraTypography.cardTitle)
                            .foregroundStyle(ScyraColors.rewardArc)
                        Spacer()
                        Text(isExpanded ? StoryStrings.hideFlows : StoryStrings.showFlows)
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                        ScyraCanonicalIcon(systemName: "chevron.down")
                            .rotationEffect(.degrees(isExpanded ? 180 : 0))
                    }

                    HStack {
                        Text("Total time · \(storyDuration(group.totalDurationMs))")
                        if showScoreUI, !calmMode {
                            Spacer()
                            Text("\(StoryStrings.arcScore) \(group.totalScore)")
                            if let peak = group.peakMultiplier {
                                Text("\(StoryStrings.peak) ×\(peak.formatted(.number.precision(.fractionLength(1))))")
                            }
                        }
                    }
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                }
                .padding(ScyraSpacing.md)
                .background(ScyraColors.elevatedSurface)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            }
            .buttonStyle(.plain)
            .accessibilityLabel(arcAccessibilityLabel)
            .accessibilityValue(isExpanded ? "Expanded" : "Collapsed")

            HStack {
                Spacer()
                Button("Edit Arc details", systemImage: "square.and.pencil", action: onEditDetails)
                    .font(ScyraTypography.label)
            }
            .padding(.horizontal, ScyraSpacing.sm)

            if isExpanded {
                if let metadata = group.metadata, metadata.hasReflection {
                    VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                        Text("Arc reflection")
                            .font(ScyraTypography.cardTitle)
                        if let outcome = metadata.outcome {
                            ArcReflectionField(label: "Outcome", value: outcome)
                        }
                        if let highlight = metadata.highlight {
                            ArcReflectionField(label: "Highlight", value: highlight)
                        }
                        if let nextStep = metadata.nextStep {
                            ArcReflectionField(label: "Next step", value: nextStep)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, ScyraSpacing.sm)
                }

                ForEach(group.visibleFlows) { flow in
                    StoryFlowCard(
                        flow: flow,
                        chronicle: group.chroniclesByFlowID[flow.id],
                        movement: group.movementByFlowID[flow.id],
                        showScoreUI: showScoreUI,
                        calmMode: calmMode,
                        onDelete: { onDeleteFlow(flow) }
                    ) { onOpenFlow(flow) }
                    ForEach(group.childPulsesByFlowID[flow.id, default: []]) { pulse in
                        StoryPulseCard(
                            item: pulse,
                            isExpanded: expandedPulseIDs.contains(pulse.id),
                            nested: true,
                            parentFlow: flow,
                            onToggle: {
                                if expandedPulseIDs.contains(pulse.id) { expandedPulseIDs.remove(pulse.id) }
                                else { expandedPulseIDs.insert(pulse.id) }
                            },
                            onOpen: { onOpenPulse(pulse.pulse) },
                            onEdit: { onEditPulse(pulse.pulse) },
                            onDelete: { onDeletePulse(pulse.pulse) }
                        )
                    }
                }
                if group.hiddenFlowCount > 0 {
                    Text("\(group.hiddenFlowCount) \(group.hiddenFlowCount == 1 ? "Flow is" : "Flows are") outside this view")
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textMuted)
                }
            }
        }
        .padding(10)
        .background(ScyraColors.rewardArc.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .animation(.easeInOut(duration: 0.2), value: isExpanded)
    }

    private var arcAccessibilityLabel: String {
        var parts = ["\(group.totalFlowCount) Flows", "total time \(storyDuration(group.totalDurationMs))"]
        if showScoreUI, !calmMode { parts.append("Arc Score \(group.totalScore)") }
        return parts.joined(separator: ", ")
    }
}

private struct ArcReflectionField: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(ScyraTypography.caption)
                .foregroundStyle(ScyraColors.textMuted)
            Text(value)
                .font(ScyraTypography.body)
                .foregroundStyle(ScyraColors.textPrimary)
        }
    }
}

private struct StoryPulseCard: View {
    let item: StoryPulseItem
    let isExpanded: Bool
    let nested: Bool
    let parentFlow: FlowSession?
    let onToggle: () -> Void
    let onOpen: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    @State private var showDeleteConfirmation = false

    private var pulse: Pulse { item.pulse }

    var body: some View {
        VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 8) {
                        ScyraCanonicalLabel(pulse.journeyName ?? "Pulse", systemImage: "brain.head.profile")
                            .font(ScyraTypography.label)
                        if let parentFlow, !nested {
                            Text(parentContext(parentFlow))
                                .font(ScyraTypography.caption)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 5)
                                .background(metaBackground)
                                .clipShape(Capsule())
                        }
                        if !pulse.title.isEmpty {
                            Text(pulse.title)
                                .font(ScyraTypography.cardTitle)
                                .multilineTextAlignment(.leading)
                        }
                    }
                    Spacer()
                    ScyraCanonicalIcon(systemName: "chevron.down")
                        .rotationEffect(.degrees(isExpanded ? 180 : 0))
                }

                Divider().overlay(contentColor.opacity(0.18))

                let displayedText = item.chronicle.textMoments.joined(separator: "\n\n").isEmpty
                    ? pulse.description
                    : item.chronicle.textMoments.joined(separator: "\n\n")
                if !displayedText.isEmpty {
                    Text(displayedText)
                        .font(ScyraTypography.body)
                        .lineLimit(isExpanded ? nil : 2)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                if !item.chronicle.mediaItems.isEmpty {
                    ChronicleMediaMomentView(
                        items: item.chronicle.mediaItems,
                        compact: true,
                        allowsOpening: false
                    )
                }

                Text(pulse.createdAt.formatted(.dateTime.month(.abbreviated).day().hour().minute()))
                    .font(ScyraTypography.caption)
                    .opacity(0.78)

                if isExpanded {
                    HStack {
                        Button("Edit", systemImage: "square.and.pencil", action: onEdit)
                        Spacer()
                        Button("Delete", systemImage: "trash", role: .destructive) {
                            showDeleteConfirmation = true
                        }
                        Button("Details", systemImage: "chevron.right", action: onOpen)
                    }
                    .font(ScyraTypography.label)
                }
        }
        .foregroundStyle(contentColor)
        .padding(ScyraSpacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(containerColor)
        .clipShape(RoundedRectangle(cornerRadius: nested ? 16 : 20, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: nested ? 16 : 20, style: .continuous))
        .onTapGesture(perform: onToggle)
        .contextMenu {
            Button("Edit", systemImage: "square.and.pencil", action: onEdit)
            Button("Delete", systemImage: "trash", role: .destructive) {
                showDeleteConfirmation = true
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("pulse-card-\(pulse.id.uuidString)")
        .accessibilityLabel(accessibilityLabel)
        .accessibilityValue(isExpanded ? "Expanded" : "Collapsed")
        .accessibilityAction(named: isExpanded ? "Collapse pulse" : "Expand pulse", onToggle)
        .accessibilityAction(named: "Open pulse details", onOpen)
        .confirmationDialog("Delete entry?", isPresented: $showDeleteConfirmation, titleVisibility: .visible) {
            Button("Delete", role: .destructive, action: onDelete)
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will permanently delete this entry.")
        }
    }

    private var containerColor: Color {
        nested ? ScyraColors.primary.opacity(0.12) : ScyraColors.primary
    }

    private var contentColor: Color {
        nested ? ScyraColors.textPrimary : .white
    }

    private var metaBackground: Color {
        nested ? ScyraColors.primary.opacity(0.18) : Color.white.opacity(0.14)
    }

    private var accessibilityLabel: String {
        let mediaSummary = item.chronicle.mediaItems.isEmpty
            ? nil
            : "\(item.chronicle.mediaItems.count) media items"
        return [pulse.title.isEmpty ? "Pulse" : pulse.title, pulse.journeyName, item.chronicle.excerpt, mediaSummary]
            .compactMap { $0 }
            .joined(separator: ", ")
    }

    private func parentContext(_ flow: FlowSession) -> String {
        "Captured during: \(flow.journeyName) • \(flow.title)"
    }
}

private func journeyColor(_ name: String) -> Color {
    let palette: [Color] = [
        ScyraColors.primary,
        ScyraColors.legacyRavenclawBlue,
        ScyraColors.secondaryGold,
        ScyraColors.rewardArc,
        ScyraColors.warning
    ]
    let stableIndex = name.unicodeScalars.reduce(0) { ($0 + Int($1.value)) % palette.count }
    return palette[stableIndex]
}

func storyDuration(_ durationMs: Int64) -> String {
    let totalSeconds = max(0, durationMs / 1_000)
    let hours = totalSeconds / 3_600
    let minutes = (totalSeconds % 3_600) / 60
    let seconds = totalSeconds % 60
    if hours > 0 { return "\(hours) hr \(String(format: "%02lld", minutes)) min \(String(format: "%02lld", seconds)) sec" }
    if minutes > 0 { return "\(minutes) min \(String(format: "%02lld", seconds)) sec" }
    return "\(seconds) sec"
}

#Preview("Story") {
    StoryView(
        viewModel: StoryViewModel(repository: InMemoryFlowRepository()),
        preferences: AppPreferencesModel(store: InMemoryAppPreferencesStore()),
        onOpenPulse: {},
        onOpenFlow: {},
        onOpenFlowDetail: { _ in },
        onOpenPulseDetail: { _ in },
        onEditPulse: { _ in }
    )
}
