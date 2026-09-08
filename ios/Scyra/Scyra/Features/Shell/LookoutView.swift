import SwiftUI

struct LookoutView: View {
    @ObservedObject var viewModel: LookoutViewModel
    let onLaunchFlow: (String) -> Void

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                header
                if let message = viewModel.message {
                    ScyraCanonicalLabel(message, systemImage: "checkmark.circle.fill")
                        .font(ScyraTypography.body).foregroundStyle(ScyraColors.primary)
                        .accessibilityIdentifier("lookout-message")
                }
                if viewModel.showsAchievements { achievements }
                else { objectives }
            }
            .padding(ScyraSpacing.screenPadding)
        }
        .background(background)
        .onAppear(perform: viewModel.refresh)
        .sheet(isPresented: $viewModel.showsEditor) { editor }
        .confirmationDialog(
            removalTitle,
            isPresented: Binding(
                get: { viewModel.objectivePendingRemoval != nil },
                set: { if !$0 { viewModel.cancelRemoval() } }
            ),
            titleVisibility: .visible
        ) {
            if viewModel.objectivePendingRemoval?.objective.kind == .recurring {
                Button("Skip this cycle") { viewModel.skipPendingCycle() }
                Button("Stop recurring objective", role: .destructive) { viewModel.archivePendingObjective() }
            } else {
                Button("Delete objective", role: .destructive) { viewModel.archivePendingObjective() }
            }
            Button("Cancel", role: .cancel, action: viewModel.cancelRemoval)
        }
        .alert(
            "The Lookout",
            isPresented: Binding(
                get: { viewModel.errorMessage != nil },
                set: { if !$0 { viewModel.dismissError() } }
            )
        ) {
            Button("OK", action: viewModel.dismissError)
        } message: {
            Text(viewModel.errorMessage ?? "Your objective could not be changed.")
        }
        .accessibilityIdentifier("lookout-screen")
    }

    private var header: some View {
        ScyraCard(style: .elevated, padding: ScyraSpacing.xl) {
            VStack(alignment: .leading, spacing: ScyraSpacing.md) {
                HStack(spacing: ScyraSpacing.md) {
                    ScyraCanonicalIcon(systemName: viewModel.showsAchievements ? "medal.fill" : "eye.fill")
                        .font(.system(size: 30, weight: .semibold))
                        .foregroundStyle(ScyraColors.primary)
                        .frame(width: 54, height: 54)
                        .background(ScyraColors.primaryContainer)
                        .clipShape(Circle())
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(viewModel.showsAchievements ? "Objective achievements" : "The Lookout")
                            .font(ScyraTypography.screenTitle)
                        Text(viewModel.showsAchievements
                             ? "Every completed objective and the Pearls it earned."
                             : "Set objectives for the Journeys you want to move forward.")
                            .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                    }
                }
                if viewModel.showsAchievements {
                    ScyraButton("Back to objectives", systemImage: "arrow.left", variant: .secondary) {
                        viewModel.showsAchievements = false
                    }
                } else {
                    HStack {
                        ScyraButton("Completed", systemImage: "medal", variant: .secondary) {
                            viewModel.showsAchievements = true
                        }
                        Spacer()
                        ScyraButton("Set", systemImage: "plus") { viewModel.openEditor() }
                            .accessibilityIdentifier("lookout-set-objective")
                    }
                    rewardStatus
                }
            }
        }
    }

    private var rewardStatus: some View {
        HStack(spacing: ScyraSpacing.md) {
            ScyraCanonicalIcon(systemName: viewModel.snapshot.unclaimedPearls > 0 ? "circle.hexagongrid.fill" : "checkmark.circle.fill")
                .foregroundStyle(viewModel.snapshot.unclaimedPearls > 0 ? ScyraColors.secondaryGold : ScyraColors.primary)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(viewModel.snapshot.unclaimedPearls > 0
                     ? "\(viewModel.snapshot.unclaimedPearls) Pearls unclaimed"
                     : "All Pearls claimed")
                    .font(ScyraTypography.label).monospacedDigit()
                Text(viewModel.snapshot.unclaimedPearls > 0
                     ? "From \(viewModel.snapshot.unclaimedCompletionCount) completed objective(s)"
                     : "No objective rewards waiting")
                    .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
            }
            Spacer()
            if viewModel.snapshot.unclaimedPearls > 0 {
                ScyraButton("Claim", variant: .secondary, action: viewModel.claimAll)
                    .accessibilityLabel("Claim \(viewModel.snapshot.unclaimedPearls) Pearls")
                    .accessibilityIdentifier("lookout-claim-all")
            }
        }
    }

    private var objectives: some View {
        Group {
            Picker("Objective period", selection: $viewModel.selectedPeriod) {
                ForEach(ObjectivePeriod.allCases, id: \.self) { Text($0.title).tag($0) }
            }
            .pickerStyle(.segmented)
            .accessibilityIdentifier("lookout-period-picker")

            if viewModel.visibleCards.isEmpty {
                ScyraEmptyState(
                    systemImage: "scope",
                    title: "No \(viewModel.selectedPeriod.title.lowercased()) objectives",
                    message: "Set an objective, then complete regular Flows in its Journey. Soft Flows do not count."
                )
            } else {
                ForEach(viewModel.visibleCards) { card in objectiveCard(card) }
            }
        }
    }

    private func objectiveCard(_ card: ObjectiveCardModel) -> some View {
        ScyraCard(style: card.state == .completed ? .elevated : .plain, padding: ScyraSpacing.lg) {
            VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(card.objective.journeyNameSnapshot).font(ScyraTypography.cardTitle)
                        Text("\(card.objective.period.title) · \(card.objective.kind.title)")
                            .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                    }
                    Spacer()
                    Button(role: .destructive) { viewModel.requestRemoval(card) } label: {
                        ScyraCanonicalIcon(systemName: "trash").frame(width: 44, height: 44)
                    }
                    .accessibilityLabel(card.objective.kind == .recurring ? "Change recurring objective" : "Delete objective")
                }
                ProgressView(value: Double(card.progressPercent), total: 100)
                    .tint(card.state == .completed ? ScyraColors.secondaryGold : ScyraColors.primary)
                    .accessibilityLabel("\(card.progressPercent) percent complete")
                Text("\(viewModel.duration(card.progressDurationMs)) of \(viewModel.duration(card.objective.targetDurationMs))")
                    .font(ScyraTypography.label).monospacedDigit()
                HStack {
                    Text(status(card)).font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                    Spacer()
                    ScyraCanonicalLabel(
                        "\(card.completion?.finalRewardPearls ?? max(1, Int(card.objective.targetDurationMs / ObjectiveProgressCalculator.millisPerMinute)))",
                        systemImage: "circle.hexagongrid.fill"
                    )
                    .font(ScyraTypography.label).foregroundStyle(ScyraColors.secondaryGold)
                    .accessibilityLabel("Estimated reward in Pearls")
                }
                if card.objective.kind == .recurring {
                    Text("Current streak \(card.effectiveCurrentStreak) · Best \(card.objective.maxStreak) · \(card.objective.totalCompletions) completions")
                        .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textMuted)
                    if card.effectiveCurrentStreak > 0, card.completion == nil {
                        Text("+\(card.effectiveCurrentStreak * 10)% streak bonus")
                            .font(ScyraTypography.caption).foregroundStyle(ScyraColors.primary)
                    }
                }
                if let completion = card.completion, !completion.pearlsClaimed {
                    ScyraButton("Claim \(completion.finalRewardPearls) Pearls", systemImage: "sparkles") {
                        viewModel.claim(completionID: completion.id)
                    }
                } else if card.state != .completed {
                    ScyraButton("Begin Flow", systemImage: "play.fill", variant: .secondary) {
                        onLaunchFlow(card.objective.journeyNameSnapshot)
                    }
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("lookout-objective-\(card.objective.id.uuidString)")
    }

    private var achievements: some View {
        Group {
            if viewModel.completionGroups.isEmpty {
                ScyraEmptyState(
                    systemImage: "medal",
                    title: "No completed objectives yet",
                    message: "Complete an objective to begin this history."
                )
            } else {
                ForEach(viewModel.completionGroups) { group in
                    ScyraSectionHeader(title: group.journeyName, subtitle: "Completed objective history")
                    ForEach(group.rows) { row in
                        ScyraCard {
                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text("\(row.period.title) Objectives").font(ScyraTypography.cardTitle)
                                    Text("\(row.completionCount) completions · \(row.earnedPearls) Pearls earned")
                                        .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                                    Text("Last completed \(row.lastCompletedAt.formatted(date: .abbreviated, time: .omitted))")
                                        .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textMuted)
                                }
                                Spacer()
                                if row.unclaimedPearls > 0 {
                                    ScyraButton("Claim \(row.unclaimedPearls)", variant: .secondary) {
                                        viewModel.claimAchievement(row.id)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private var editor: some View {
        NavigationStack {
            Form {
                Section("Journey") {
                    TextField("Journey name", text: $viewModel.editorJourney)
                        .textInputAutocapitalization(.words)
                        .accessibilityIdentifier("lookout-editor-journey")
                    if !viewModel.journeys.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack {
                                ForEach(viewModel.journeys) { journey in
                                    ScyraChip(journey.name, isSelected: viewModel.editorJourney == journey.name) {
                                        viewModel.editorJourney = journey.name
                                    }
                                }
                            }
                        }
                    }
                }
                Section("Objective") {
                    Picker("Period", selection: $viewModel.selectedPeriod) {
                        ForEach(ObjectivePeriod.allCases, id: \.self) { Text($0.title).tag($0) }
                    }
                    Picker("Type", selection: $viewModel.editorKind) {
                        ForEach(ObjectiveKind.allCases, id: \.self) { Text($0.title).tag($0) }
                    }
                    DatePicker("Start date", selection: $viewModel.editorStartDate, displayedComponents: .date)
                    HStack {
                        TextField("Hours", text: $viewModel.editorHours).keyboardType(.numberPad)
                        Text("hours").foregroundStyle(ScyraColors.textSecondary)
                        TextField("Minutes", text: $viewModel.editorMinutes).keyboardType(.numberPad)
                        Text("minutes").foregroundStyle(ScyraColors.textSecondary)
                    }
                }
                Section {
                    Text("Only completed regular Flows in this Journey count. Rewards become claimable after the target is reached; overshoot minutes and recurring streak bonuses are preserved.")
                        .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                }
            }
            .navigationTitle("Set objective")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { viewModel.showsEditor = false } }
                ToolbarItem(placement: .confirmationAction) { Button("Set") { viewModel.saveObjective() } }
            }
        }
        .accessibilityIdentifier("lookout-editor")
    }

    private func status(_ card: ObjectiveCardModel) -> String {
        switch card.state {
        case .upcoming: "Starts in \(viewModel.remaining(until: card.window.start))"
        case .inProgress: "\(viewModel.remaining(until: card.window.end)) left"
        case .completed:
            card.objective.kind == .recurring
                ? "Completed · next cycle \(card.window.end.formatted(.dateTime.weekday(.wide)))"
                : "Completed"
        }
    }

    private var removalTitle: String {
        guard let card = viewModel.objectivePendingRemoval else { return "Remove objective?" }
        return card.objective.kind == .recurring
            ? "Skip this cycle or stop the recurring objective for \(card.objective.journeyNameSnapshot)?"
            : "Delete the objective for \(card.objective.journeyNameSnapshot)?"
    }

    private var background: some View {
        LinearGradient(
            colors: [ScyraColors.background, ScyraColors.backgroundBottom],
            startPoint: .top, endPoint: .bottom
        ).ignoresSafeArea()
    }
}

#Preview {
    LookoutView(viewModel: LookoutViewModel(repository: InMemoryFlowRepository()), onLaunchFlow: { _ in })
}
