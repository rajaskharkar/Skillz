import Combine
import SwiftUI

struct FlowView: View {
    @ObservedObject var viewModel: FlowViewModel
    @ObservedObject var preferences: AppPreferencesModel
    let onBackToStory: () -> Void
    let onOpenShell: () -> Void

    @State private var surgeMinutesInput = ""
    @State private var showResetConfirmation = false
    @State private var showSoftArcConfirmation = false
    @State private var page: FlowPage = .flow
    @State private var chronicleBlocksPager = false
    @State private var showChronicleDraftPrompt = false
    @State private var pendingEndAction: FlowEndAction?

    private let ticker = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack(spacing: 0) {
            ScyraActionScreenHeader(title: FlowStrings.title) {
                viewModel.discardDraftIfIdle()
                onBackToStory()
            }

            FlowChroniclePagerSelector(
                selection: $page,
                canLeaveChronicle: !chronicleBlocksPager
            )

            switch page {
            case .flow:
                flowPage
            case .chronicle:
                ChronicleTextEditorView(
                    snapshot: viewModel.chronicle,
                    onDraftChanged: viewModel.updateChronicleDraft,
                    onAdd: { _ = viewModel.addChronicleText() },
                    onUpdate: viewModel.updateChronicleMoment,
                    onDelete: viewModel.deleteChronicleMoment,
                    onMove: viewModel.moveChronicleMoment,
                    isImportingMedia: viewModel.isImportingChronicleMedia,
                    onImportMedia: viewModel.importChronicleMedia,
                    onStageMedia: viewModel.stageChronicleMedia,
                    onReplaceMedia: viewModel.replaceChronicleMedia,
                    onAddVoice: viewModel.addChronicleVoice,
                    onUpdateTranscript: viewModel.updateChronicleTranscript,
                    onEditingChanged: { chronicleBlocksPager = $0 }
                )
            }
        }
        .onReceive(ticker) { date in
            viewModel.refreshElapsed(at: date)
            if viewModel.recentlyResumedArcMessage != nil {
                Task { @MainActor in
                    try? await Task.sleep(for: .seconds(3))
                    viewModel.consumeRecentlyResumedArcMessage()
                }
            }
        }
        .alert(FlowStrings.resetTitle, isPresented: $showResetConfirmation) {
            Button(FlowStrings.cancel, role: .cancel) {}
            Button(FlowStrings.resetConfirmation, role: .destructive) {
                viewModel.resetTimer()
            }
        } message: {
            let minutes = max(1, Int(viewModel.elapsedMs / ScoreCalculator.millisPerMinute))
            Text("You've already focused for \(minutes) \(minutes == 1 ? "minute" : "minutes"). Are you sure you want to reset and lose this progress?")
        }
        .alert("Enter soft?", isPresented: $showSoftArcConfirmation) {
            Button("Keep multiplier", role: .cancel) {}
            Button("Enter soft") { viewModel.setMode(.soft) }
        } message: {
            Text("Your Arc multiplier will be reset when you enter Soft Mode.")
        }
        .alert(ChronicleStrings.unfinishedMoment, isPresented: $showChronicleDraftPrompt) {
            Button(ChronicleStrings.addMoment) {
                if viewModel.addChronicleText() { completePendingAction() }
            }
            Button(ChronicleStrings.discard, role: .destructive) {
                viewModel.discardChronicleDraftText()
                completePendingAction()
            }
            Button(ChronicleStrings.cancel, role: .cancel) { pendingEndAction = nil }
        }
        .alert(
            "Continue this Idea too?",
            isPresented: Binding(
                get: { viewModel.pendingIdeaContinuation != nil },
                set: { _ in }
            )
        ) {
            Button("Continue Arc Only") {
                viewModel.resolveIdeaContinuation(includeIdea: false)
            }
            Button("Continue Arc + Idea") {
                viewModel.resolveIdeaContinuation(includeIdea: true)
            }
        } message: {
            Text("Your next Flow will continue the Arc. Do you also want it to be part of this Pulse’s Idea journey?")
        }
        .sheet(
            isPresented: Binding(
                get: { viewModel.reward != nil },
                set: { _ in }
            )
        ) {
            if let reward = viewModel.reward {
                FlowRewardSheet(
                    reward: reward,
                    calmMode: preferences.calmMode,
                    onDone: {
                        if viewModel.finishReward() { onBackToStory() }
                    },
                    onEnterShell: reward.hasShellReward ? {
                        if viewModel.enterShellFromReward() { onOpenShell() }
                    } : nil
                )
                .interactiveDismissDisabled()
            }
        }
    }

    private var flowPage: some View {
        ScrollView {
            VStack(spacing: 14) {
                focusCard
                modeCard
                journeyCard
                timerCard

                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.error)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .accessibilityLabel("Error: \(errorMessage)")
                }

                completionControls
            }
            .padding(.horizontal, ScyraSpacing.md)
            .padding(.vertical, 12)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(ScyraColors.background)
    }

    private var focusCard: some View {
        FlowRitualCard(rotation: -0.20, cornerRadius: 30) {
            VStack(spacing: 10) {
                if viewModel.originPulseID != nil, let originTitle = viewModel.originPulseTitle {
                    VStack(spacing: 2) {
                        Text("From Pulse")
                            .font(ScyraTypography.label)
                            .fontWeight(.semibold)
                            .foregroundStyle(ScyraColors.primary)
                        Text(originTitle)
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(10)
                    .background(ScyraColors.primaryContainer)
                    .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous))
                    .accessibilityElement(children: .combine)
                }

                Text(FlowStrings.focus)
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.textMuted)
                    .tracking(0.6)

                TextField(
                    FlowStrings.titlePlaceholder,
                    text: Binding(get: { viewModel.title }, set: viewModel.updateTitle)
                )
                .font(.title2.weight(.regular))
                .foregroundStyle(ScyraColors.textPrimary)
                .multilineTextAlignment(.center)
                .textInputAutocapitalization(.sentences)
                .submitLabel(.done)
                .padding(.horizontal, ScyraSpacing.md)
                .frame(minHeight: 56)
                .background(ScyraColors.surfaceVariant.opacity(0.50))
                .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
                .accessibilityLabel("Flow title")
            }
        }
    }

    private var modeCard: some View {
        FlowRitualCard(rotation: 0.08, cornerRadius: 26) {
            VStack(spacing: 10) {
                Text(FlowStrings.mode)
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.textMuted)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if viewModel.isModeLocked {
                    FlowModeOption(
                        title: viewModel.mode == .soft ? FlowStrings.softMode : FlowStrings.flowMode,
                        subtitle: viewModel.mode == .soft ? FlowStrings.softModeSubtitle : FlowStrings.flowModeSubtitle,
                        iconAsset: viewModel.mode == .soft ? "materialSpa" : "materialAutoAwesome",
                        mode: viewModel.mode,
                        selected: true,
                        disabled: true,
                        action: {}
                    )
                } else {
                    HStack(spacing: 10) {
                        FlowModeOption(
                            title: FlowStrings.flowMode,
                            subtitle: FlowStrings.flowModeSubtitle,
                            iconAsset: "materialAutoAwesome",
                            mode: .flow,
                            selected: viewModel.mode == .flow,
                            disabled: false
                        ) {
                            viewModel.setMode(.flow)
                        }

                        FlowModeOption(
                            title: FlowStrings.softMode,
                            subtitle: FlowStrings.softModeSubtitle,
                            iconAsset: "materialSpa",
                            mode: .soft,
                            selected: viewModel.mode == .soft,
                            disabled: false
                        ) {
                            if viewModel.activeArc != nil, viewModel.mode == .flow {
                                showSoftArcConfirmation = true
                            } else {
                                viewModel.setMode(.soft)
                            }
                        }
                    }
                }

                if viewModel.isModeLocked {
                    Text(FlowStrings.modeLocked)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textMuted)
                }
            }
        }
    }

    private var journeyCard: some View {
        FlowRitualCard(rotation: 0.12, cornerRadius: 26) {
            VStack(spacing: 10) {
                Text(FlowStrings.journeys)
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.textMuted)
                    .tracking(0.6)
                    .frame(maxWidth: .infinity)

                if !viewModel.journeys.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: ScyraSpacing.sm) {
                            ForEach(viewModel.journeys.prefix(8)) { journey in
                                ScyraChip(
                                    journey.name,
                                    isSelected: journey.name.caseInsensitiveCompare(viewModel.journeyName) == .orderedSame
                                ) {
                                    viewModel.selectJourney(journey)
                                }
                                .accessibilityLabel("Select journey \(journey.name)")
                            }
                        }
                        .padding(.horizontal, 2)
                    }
                }

                TextField(
                    FlowStrings.journeyPlaceholder,
                    text: Binding(get: { viewModel.journeyName }, set: viewModel.updateJourneyName)
                )
                .font(ScyraTypography.body)
                .font(.title2.weight(.regular))
                .multilineTextAlignment(.center)
                .textInputAutocapitalization(.words)
                .submitLabel(.done)
                .padding(.horizontal, ScyraSpacing.md)
                .frame(minHeight: 52)
                .background(ScyraColors.surfaceVariant.opacity(0.48))
                .clipShape(Capsule())
                .accessibilityLabel("Journey name")
            }
        }
    }

    private var timerCard: some View {
        FlowRitualCard(rotation: -0.08, cornerRadius: 32, transparent: true, showBorder: false) {
            VStack(spacing: 12) {
                if let message = viewModel.recentlyResumedArcMessage {
                    Text(message)
                        .font(ScyraTypography.label)
                        .foregroundStyle(ScyraColors.primary)
                        .frame(maxWidth: .infinity)
                        .padding(10)
                        .background(ScyraColors.primaryContainer)
                        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card))
                }

                if let title = viewModel.plannedArcTitle,
                   let index = viewModel.plannedArcStepIndex,
                   let total = viewModel.plannedArcTotalSteps {
                    PlannedArcStatusPill(title: title, stepIndex: index, totalSteps: total)
                }

                if let arc = viewModel.activeArc {
                    ArcStatusPill(
                        arc: arc,
                        nextIndex: viewModel.arcNextIndex,
                        graceRemainingMs: viewModel.arcGraceRemainingMs(),
                        pauseRemainingMs: viewModel.arcPauseRemainingMs(),
                        calmMode: preferences.calmMode
                    )
                }

                if viewModel.movementBonusEligibleAtStart {
                    MovementBonusActivePill()
                }

                Text(FlowStrings.inFlow)
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.textPrimary)

                if !preferences.calmMode || !viewModel.isRunning {
                    Text(FlowDurationFormatter.stopwatch(viewModel.elapsedMs))
                        .font(.system(.largeTitle, design: .monospaced).weight(.semibold))
                        .foregroundStyle(ScyraColors.textPrimary)
                        .contentTransition(.numericText())
                        .accessibilityLabel("Elapsed time: \(FlowDurationFormatter.stopwatch(viewModel.elapsedMs))")

                    Button(FlowStrings.reset) {
                        if viewModel.elapsedMs >= 2 * ScoreCalculator.millisPerMinute {
                            showResetConfirmation = true
                        } else {
                            viewModel.resetTimer()
                        }
                    }
                    .buttonStyle(.bordered)
                    .tint(ScyraColors.primary)
                    .disabled(viewModel.elapsedMs == 0 || viewModel.isRunning)
                } else {
                    Text("Calm Mode")
                        .font(ScyraTypography.label)
                        .foregroundStyle(ScyraColors.textMuted)
                        .frame(maxWidth: .infinity)
                        .accessibilityLabel("Calm Mode. Elapsed timer hidden while Flow is running.")
                }

                FlowWideButton(
                    title: flowModeActionTitle,
                    systemImage: viewModel.isInFlowMode ? "pause.fill" : "play.fill",
                    variant: .primary,
                    disabled: false,
                    action: viewModel.toggleFlowMode
                )

                if viewModel.mode == .flow {
                    surgeControl
                } else {
                    VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
                        Text(FlowStrings.softFlowLabel)
                            .font(ScyraTypography.label)
                            .foregroundStyle(ScyraColors.secondaryGold)
                        Text(FlowStrings.softFlowBody)
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(ScyraColors.secondaryContainer)
                    .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card))
                }

                if viewModel.isInFlowMode {
                    Text(FlowStrings.inFlowHelper)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textSecondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private var surgeControl: some View {
        HStack(spacing: ScyraSpacing.sm) {
            ScyraCanonicalIcon(systemName: "bolt.fill")
                .foregroundStyle(ScyraColors.rewardSurge)
                .accessibilityHidden(true)

            if let minutes = viewModel.surgePlannedMinutes {
                Text(FlowStrings.plannedSurge(minutes: minutes))
                    .font(ScyraTypography.label)
                Spacer()
                Button(FlowStrings.turnOff) {
                    viewModel.clearSurge()
                    surgeMinutesInput = ""
                }
                .font(ScyraTypography.label)
                .disabled(viewModel.isSurgeLocked || viewModel.isInFlowMode)
            } else {
                Text(FlowStrings.surge)
                    .font(ScyraTypography.label)
                Spacer()
                TextField("0", text: $surgeMinutesInput)
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.center)
                    .frame(width: 52, height: 36)
                    .background(ScyraColors.surface.opacity(0.7))
                    .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.capsule))
                    .onChange(of: surgeMinutesInput) { _, newValue in
                        surgeMinutesInput = String(newValue.filter(\.isNumber).prefix(3))
                    }
                    .accessibilityLabel("Surge Time in minutes")
                Text("min")
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textMuted)
                Button(FlowStrings.set) {
                    if let minutes = Int(surgeMinutesInput), minutes > 0 {
                        viewModel.setSurge(minutes: minutes)
                    }
                }
                .font(ScyraTypography.label)
                .disabled(
                    Int(surgeMinutesInput).map { $0 <= 0 } ?? true
                        || viewModel.isSurgeLocked
                        || viewModel.isInFlowMode
                )
            }
        }
        .padding(.horizontal, 12)
        .frame(minHeight: 48)
        .background((preferences.calmMode ? ScyraColors.surface : ScyraColors.rewardSurge).opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card))
        .accessibilityElement(children: .contain)
    }

    @ViewBuilder
    private var completionControls: some View {
        switch viewModel.completionControls {
        case .standaloneSoft:
            FlowWideButton(
                title: viewModel.isSaving ? FlowStrings.saving : FlowStrings.saveSoftFlow,
                variant: .primary,
                disabled: !viewModel.canComplete
            ) {
                requestCompletion(.saveFlow)
            }

        case .arcActions, .regularActions:
            HStack(spacing: 10) {
                FlowWideButton(
                    title: FlowStrings.continueArc,
                    variant: .secondary,
                    disabled: !viewModel.canContinueArc
                ) {
                    requestCompletion(.continueArc)
                }

                FlowWideButton(
                    title: viewModel.isSaving
                        ? FlowStrings.saving
                        : (viewModel.activeArc == nil ? FlowStrings.completeFlow : FlowStrings.completeArc),
                    variant: .primary,
                    disabled: !viewModel.canComplete
                ) {
                    requestCompletion(viewModel.activeArc == nil ? .saveFlow : .completeArc)
                }
            }
        }
    }

    private var flowModeActionTitle: String {
        if viewModel.isInFlowMode {
            return viewModel.isSoftMode ? FlowStrings.exitSoftFlow : FlowStrings.exitFlow
        }
        return viewModel.isSoftMode ? FlowStrings.beginSoftFlow : FlowStrings.enterFlow
    }

    private func requestCompletion(_ action: FlowEndAction) {
        if viewModel.chronicle.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            viewModel.complete(action)
        } else {
            pendingEndAction = action
            showChronicleDraftPrompt = true
        }
    }

    private func completePendingAction() {
        guard let action = pendingEndAction else { return }
        pendingEndAction = nil
        viewModel.complete(action)
    }
}

private enum FlowPage: Hashable {
    case flow
    case chronicle
}

private struct FlowChroniclePagerSelector: View {
    @Binding var selection: FlowPage
    let canLeaveChronicle: Bool

    var body: some View {
        HStack(spacing: 3) {
            segment(.flow, title: FlowStrings.title, iconAsset: "materialAutoAwesome", enabled: selection != .chronicle || canLeaveChronicle)
            segment(.chronicle, title: ChronicleStrings.title, iconAsset: "materialAutoStories", enabled: true)
        }
        .padding(3)
        .frame(minWidth: 280, maxWidth: 360)
        .background(ScyraColors.primary.opacity(0.08), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
    }

    private func segment(_ page: FlowPage, title: String, iconAsset: String, enabled: Bool) -> some View {
        let selected = selection == page
        return Button {
            withAnimation(.easeInOut(duration: 0.18)) { selection = page }
        } label: {
            HStack(spacing: 7) {
                ScyraMaterialIcon(
                    assetName: iconAsset,
                    size: 20,
                    color: selected ? ScyraColors.onPrimary : ScyraColors.textPrimary
                )
                Text(title)
            }
                .font(.subheadline.weight(.medium))
                .tracking(0.6)
                .foregroundStyle(selected ? ScyraColors.onPrimary : ScyraColors.textPrimary)
                .frame(maxWidth: .infinity, minHeight: 48)
                .padding(.horizontal, 10)
                .background(selected ? ScyraColors.primary : Color.clear, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel(title)
        .accessibilityValue(selected ? "Selected" : "Not selected")
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

private struct FlowRitualCard<Content: View>: View {
    let rotation: Double
    let cornerRadius: CGFloat
    let transparent: Bool
    let showBorder: Bool
    private let content: Content

    init(
        rotation: Double,
        cornerRadius: CGFloat,
        transparent: Bool = false,
        showBorder: Bool = true,
        @ViewBuilder content: () -> Content
    ) {
        self.rotation = rotation
        self.cornerRadius = cornerRadius
        self.transparent = transparent
        self.showBorder = showBorder
        self.content = content()
    }

    var body: some View {
        content
            .padding(16)
            .frame(maxWidth: .infinity)
            .background(transparent ? Color.clear : ScyraColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(showBorder ? ScyraColors.textPrimary.opacity(0.06) : Color.clear, lineWidth: 1)
            )
            .shadow(color: transparent ? .clear : Color.black.opacity(0.11), radius: 2, y: 1)
            .rotationEffect(.degrees(rotation))
    }
}

private struct FlowModeOption: View {
    let title: String
    let subtitle: String
    let iconAsset: String
    let mode: FlowMode
    let selected: Bool
    let disabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                ScyraMaterialIcon(assetName: iconAsset, size: 24, color: contentColor)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.subheadline.weight(.semibold))
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(contentColor.opacity(0.78))
                        .lineLimit(2)
                }
                Spacer(minLength: 0)
            }
            .foregroundStyle(contentColor)
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, minHeight: 68)
            .background(containerColor, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .opacity(disabled && !selected ? 0.5 : 1)
        .accessibilityValue(selected ? "Selected" : "Not selected")
    }

    private var containerColor: Color {
        guard selected else { return ScyraColors.surfaceVariant }
        return mode == .soft ? ScyraColors.secondaryGold : ScyraColors.primary
    }

    private var contentColor: Color {
        guard selected else { return ScyraColors.textPrimary }
        return mode == .soft ? ScyraColors.onSecondary : ScyraColors.onPrimary
    }
}

struct FlowWideButton: View {
    let title: String
    var systemImage: String? = nil
    let variant: ScyraButtonVariant
    let disabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: ScyraSpacing.sm) {
                if let systemImage {
                    ScyraCanonicalIcon(systemName: systemImage).accessibilityHidden(true)
                }
                Text(title)
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)
            }
            .font(ScyraTypography.button)
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity, minHeight: 52)
            .padding(.horizontal, ScyraSpacing.sm)
            .background(background)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(variant == .secondary ? ScyraColors.primary.opacity(0.35) : .clear, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .opacity(disabled ? 0.5 : 1)
    }

    private var foreground: Color {
        variant == .primary ? ScyraColors.onPrimary : ScyraColors.primary
    }

    private var background: Color {
        variant == .primary ? ScyraColors.primary : ScyraColors.primaryContainer
    }
}

private struct PlannedArcStatusPill: View {
    let title: String
    let stepIndex: Int
    let totalSteps: Int

    var body: some View {
        HStack(spacing: ScyraSpacing.sm) {
            ScyraCanonicalIcon(systemName: "point.topleft.down.to.point.bottomright.curvepath")
                .foregroundStyle(ScyraColors.primary)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(ScyraTypography.label)
                Text("Step \(stepIndex + 1) of \(totalSteps)")
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .frame(minHeight: 48)
        .background(ScyraColors.primary.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.capsule))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(title). Step \(stepIndex + 1) of \(totalSteps).")
        .accessibilityIdentifier("planned-arc-status")
    }
}

private struct ArcStatusPill: View {
    let arc: ArcRuntimeState
    let nextIndex: Int?
    let graceRemainingMs: Int64?
    let pauseRemainingMs: Int64?
    let calmMode: Bool

    var body: some View {
        HStack(spacing: ScyraSpacing.sm) {
            ScyraCanonicalIcon(systemName: calmMode ? "circle" : "flame.fill")
                .foregroundStyle(calmMode ? ScyraColors.textMuted : ScyraColors.rewardArc)
                .accessibilityHidden(true)
            Text(calmMode ? "Arc active" : FlowStrings.arcMultiplier(arc.multiplier))
            if !calmMode, let nextIndex { Text("• \(FlowStrings.arcFlowIndex(nextIndex))") }
            Spacer(minLength: 0)
            if let remaining = pauseRemainingMs ?? graceRemainingMs {
                Text(FlowDurationFormatter.countdown(remaining))
                    .monospacedDigit()
            }
        }
        .font(ScyraTypography.label)
        .foregroundStyle(ScyraColors.textPrimary)
        .padding(.horizontal, 12)
        .frame(minHeight: 44)
        .background((calmMode ? ScyraColors.surface : ScyraColors.rewardArc).opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.capsule))
        .accessibilityElement(children: .combine)
    }
}

private struct FlowRewardSheet: View {
    let reward: FlowReward
    let calmMode: Bool
    let onDone: () -> Void
    let onEnterShell: (() -> Void)?

    @State private var showsArcSummary: Bool

    init(
        reward: FlowReward,
        calmMode: Bool,
        onDone: @escaping () -> Void,
        onEnterShell: (() -> Void)?
    ) {
        self.reward = reward
        self.calmMode = calmMode
        self.onDone = onDone
        self.onEnterShell = onEnterShell
        _showsArcSummary = State(initialValue: reward.isArcOnlySummary)
    }

    private var advancesToArcSummary: Bool {
        reward.arcSummary != nil && !showsArcSummary && !reward.isArcOnlySummary
    }

    private var visibleCards: [RewardRevealCard] {
        if showsArcSummary, let summary = reward.arcSummary {
            return RewardRevealMapper.arcCards(for: summary, calmMode: calmMode)
        }
        if reward.isSoftSession { return RewardRevealMapper.softCards(for: reward) }
        return RewardRevealMapper.sessionCards(for: reward, calmMode: calmMode)
    }

    private var stageTitle: String {
        if showsArcSummary { return FlowStrings.arcReward }
        return reward.isSoftSession ? FlowStrings.softRecorded : FlowStrings.youDidIt
    }

    var body: some View {
        VStack(spacing: 0) {
            Text(stageTitle)
                .font(.title2.weight(.semibold))
                .foregroundStyle(ScyraColors.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, ScyraSpacing.lg)
                .padding(.top, ScyraSpacing.lg)
                .accessibilityAddTraits(.isHeader)
                .accessibilityIdentifier("reward-stage-title")

            RewardRevealDeckView(
                cards: visibleCards,
                onDone: {
                    if advancesToArcSummary {
                        withAnimation(.easeInOut(duration: 0.2)) { showsArcSummary = true }
                    } else {
                        onDone()
                    }
                },
                onEnterShell: onEnterShell,
                primaryTitle: advancesToArcSummary ? FlowStrings.next : FlowStrings.done,
                primaryIdentifier: advancesToArcSummary ? "reward-next-arc" : "reward-done"
            )
            .id(showsArcSummary ? "arc-reward-stage" : "flow-reward-stage")
            .transition(.opacity)
        }
        .background(ScyraColors.background.ignoresSafeArea())
        .presentationDetents([.large])
    }
}

enum FlowDurationFormatter {
    static func stopwatch(_ durationMs: Int64) -> String {
        let totalSeconds = max(0, durationMs) / 1_000
        let hours = totalSeconds / 3_600
        let minutes = (totalSeconds % 3_600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 { return String(format: "%lld:%02lld:%02lld", hours, minutes, seconds) }
        return String(format: "%02lld:%02lld", minutes, seconds)
    }

    static func countdown(_ durationMs: Int64) -> String {
        let totalSeconds = max(0, durationMs) / 1_000
        return String(format: "%lld:%02lld", totalSeconds / 60, totalSeconds % 60)
    }

    static func compact(_ durationMs: Int64) -> String {
        let totalMinutes = max(0, durationMs) / ScoreCalculator.millisPerMinute
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        return hours > 0 ? "\(hours)h \(minutes)m" : "\(minutes)m"
    }
}

#Preview("Flow") {
    FlowView(
        viewModel: FlowViewModel(repository: InMemoryFlowRepository()),
        preferences: AppPreferencesModel(store: InMemoryAppPreferencesStore()),
        onBackToStory: {},
        onOpenShell: {}
    )
}
