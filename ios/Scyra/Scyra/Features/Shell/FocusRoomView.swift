import SwiftUI

struct FocusRoomView: View {
    @ObservedObject var viewModel: FocusRoomViewModel
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ZStack {
            Group {
                if let exercise = viewModel.selectedExercise {
                    if viewModel.isCompleted {
                        completion(exercise)
                    } else {
                        player(exercise)
                    }
                } else {
                    exerciseList
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(roomBackground)
        .onChange(of: scenePhase) { _, phase in
            if phase != .active { viewModel.pauseForBackground() }
        }
        .onDisappear(perform: viewModel.end)
    }

    private var exerciseList: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
                    Text("Focus Room").font(ScyraTypography.screenTitle)
                    Text("Let your attention settle.")
                        .font(ScyraTypography.body)
                        .foregroundStyle(ScyraColors.textSecondary)
                }
                .accessibilityElement(children: .combine)

                hero

                ForEach(FocusRoomCatalog.exercises) { exercise in
                    exerciseCard(exercise)
                }

                ScyraCard {
                    VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                        Text("No points. No pressure.").font(ScyraTypography.cardTitle)
                        Text("Focus Room is here to help you settle. These exercises do not affect Scyra Points, Pearls, creatures, Stillwater, or stats.")
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                }

                if let message = viewModel.voiceMessage {
                    ScyraCard {
                        VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
                            Text("Voice guidance unavailable")
                                .font(ScyraTypography.cardTitle)
                                .foregroundStyle(ScyraColors.error)
                            Text("\(message) You can still follow every exercise on screen.")
                                .font(ScyraTypography.caption)
                                .foregroundStyle(ScyraColors.textSecondary)
                        }
                    }
                }
            }
            .padding(ScyraSpacing.screenPadding)
        }
        .accessibilityLabel("Focus Room. Let your attention settle.")
        .accessibilityIdentifier("focus-room-list")
    }

    private var hero: some View {
        ZStack(alignment: .leading) {
            ScyraColors.primary
            HStack {
                VStack(alignment: .leading, spacing: ScyraSpacing.md) {
                    Text("Let your attention settle.")
                        .font(ScyraTypography.cardTitle)
                        .foregroundStyle(.white)
                    Text("Choose a short guided exercise. Scyra will talk you through breath, body, or attention.")
                        .font(ScyraTypography.body)
                        .foregroundStyle(.white.opacity(0.86))
                    Text("Guided mindfulness only")
                        .font(ScyraTypography.caption)
                        .foregroundStyle(.white)
                        .padding(.horizontal, ScyraSpacing.md)
                        .padding(.vertical, ScyraSpacing.sm)
                        .background(.white.opacity(0.16))
                        .clipShape(Capsule())
                }
                Spacer(minLength: 0)
                FocusAmbientVisual(color: .white)
                    .frame(width: 112, height: 112)
                    .accessibilityLabel("A calm glowing shell ripple.")
            }
            .padding(ScyraSpacing.xl)
        }
        .frame(minHeight: 210)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }

    private func exerciseCard(_ exercise: FocusGuidedExercise) -> some View {
        ScyraCard(style: .elevated, padding: ScyraSpacing.lg) {
            VStack(alignment: .leading, spacing: ScyraSpacing.md) {
                HStack(spacing: ScyraSpacing.md) {
                    exerciseGlyph(exercise.id)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(exercise.title).font(ScyraTypography.cardTitle)
                        Text("\(formatDuration(exercise.durationSeconds)) · Guided")
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.primary)
                    }
                }
                Text(exercise.description).font(ScyraTypography.body)
                Text(exercise.bestFor)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                Text(exercise.purpose)
                    .font(ScyraTypography.label)
                    .padding(.horizontal, ScyraSpacing.md)
                    .padding(.vertical, ScyraSpacing.xs)
                    .background(ScyraColors.primaryContainer)
                    .clipShape(Capsule())
                ScyraButton("Start") { viewModel.select(exercise) }
                    .frame(maxWidth: .infinity)
                    .accessibilityIdentifier("focus-start-\(exercise.id.rawValue)")
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("\(exercise.title). \(formatDuration(exercise.durationSeconds)). Guided. \(exercise.description) \(exercise.bestFor)")
    }

    private func player(_ exercise: FocusGuidedExercise) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
                        Text(exercise.title).font(ScyraTypography.screenTitle)
                        Text("Step \(viewModel.currentStepIndex + 1) of \(exercise.steps.count) · \(formatDuration(exercise.durationSeconds))")
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                    Spacer()
                    Button("End", action: viewModel.end)
                        .foregroundStyle(ScyraColors.error)
                        .accessibilityIdentifier("focus-end")
                }

                if viewModel.hasStarted {
                    promptCard(exercise)
                    voiceCard
                    playerControls
                } else {
                    readyCard(exercise)
                    voiceCard
                    ScyraButton("Back to Focus Room", variant: .ghost, action: viewModel.end)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(ScyraSpacing.screenPadding)
        }
        .accessibilityLabel("\(exercise.title). Guided exercise player.")
        .accessibilityIdentifier("focus-player")
    }

    private func readyCard(_ exercise: FocusGuidedExercise) -> some View {
        VStack(spacing: ScyraSpacing.lg) {
            FocusStepVisual(step: .init(spokenText: "", durationSeconds: 1))
                .frame(height: 170)
            Text("Ready when you are")
                .font(ScyraTypography.screenTitle)
                .multilineTextAlignment(.center)
            Text(exercise.description)
                .font(ScyraTypography.body)
                .multilineTextAlignment(.center)
                .opacity(0.88)
            Text(voiceStatus)
                .font(ScyraTypography.caption)
                .padding(.horizontal, ScyraSpacing.md)
                .padding(.vertical, ScyraSpacing.sm)
                .background(.white.opacity(0.14))
                .clipShape(Capsule())
            ScyraButton("Start guided exercise", variant: .secondary, action: viewModel.start)
                .frame(maxWidth: .infinity)
                .accessibilityIdentifier("focus-begin")
        }
        .foregroundStyle(.white)
        .padding(ScyraSpacing.xl)
        .frame(maxWidth: .infinity)
        .background(ScyraColors.primary)
        .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
    }

    @ViewBuilder
    private func promptCard(_ exercise: FocusGuidedExercise) -> some View {
        if let step = viewModel.currentStep {
            VStack(spacing: ScyraSpacing.lg) {
                FocusStepVisual(step: step)
                    .frame(height: 210)
                Text(step.displayText)
                    .font(ScyraTypography.screenTitle)
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.updatesFrequently)
                    .accessibilityIdentifier("focus-current-prompt")
                if exercise.isBreathingExercise && step.type.isBreathPhase {
                    Text("\(max(0, step.durationSeconds - viewModel.elapsedInCurrentStep))  sec in this phase")
                        .font(ScyraTypography.label)
                        .monospacedDigit()
                        .padding(.horizontal, ScyraSpacing.md)
                        .padding(.vertical, ScyraSpacing.sm)
                        .background(.white.opacity(0.16))
                        .clipShape(Capsule())
                        .accessibilityLabel("\(max(0, step.durationSeconds - viewModel.elapsedInCurrentStep)) seconds remain in this phase")
                }
                Text(supportingText(exercise.id, step.type))
                    .font(ScyraTypography.body)
                    .multilineTextAlignment(.center)
                    .opacity(0.84)
                ProgressView(value: viewModel.progress)
                    .tint(.white)
                HStack {
                    Text(formatClock(viewModel.totalElapsedSeconds))
                    Spacer()
                    Text("-\(formatClock(viewModel.remainingSeconds))")
                }
                .font(ScyraTypography.caption)
                .monospacedDigit()
                .opacity(0.82)
                Text("Step \(viewModel.currentStepIndex + 1) of \(exercise.steps.count)")
                    .font(ScyraTypography.caption)
                    .opacity(0.7)
            }
            .foregroundStyle(.white)
            .padding(ScyraSpacing.xl)
            .frame(maxWidth: .infinity)
            .background(ScyraColors.primary)
            .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
        }
    }

    private var voiceCard: some View {
        ScyraCard {
            VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                Toggle("Voice guidance", isOn: $viewModel.voiceEnabled)
                    .font(ScyraTypography.cardTitle)
                    .tint(ScyraColors.primary)
                    .accessibilityIdentifier("focus-voice-toggle")
                Text(voiceStatus)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                if viewModel.voiceEnabled, let message = viewModel.voiceMessage {
                    Text(message).font(ScyraTypography.caption).foregroundStyle(ScyraColors.error)
                }
            }
        }
    }

    private var playerControls: some View {
        VStack(spacing: ScyraSpacing.md) {
            ScyraButton(viewModel.isPlaying ? "Pause" : "Resume", systemImage: viewModel.isPlaying ? "pause.fill" : "play.fill") {
                viewModel.isPlaying ? viewModel.pause() : viewModel.resume()
            }
            .frame(maxWidth: .infinity)
            .accessibilityIdentifier("focus-pause-resume")
            HStack(spacing: ScyraSpacing.md) {
                ScyraButton("Restart", systemImage: "arrow.counterclockwise", variant: .secondary, action: viewModel.restart)
                    .frame(maxWidth: .infinity)
                ScyraButton("End", systemImage: "xmark", variant: .destructive, action: viewModel.end)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private func completion(_ exercise: FocusGuidedExercise) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                Text("Complete")
                    .font(ScyraTypography.label)
                    .padding(.horizontal, ScyraSpacing.md)
                    .padding(.vertical, ScyraSpacing.sm)
                    .background(.white.opacity(0.16))
                    .clipShape(Capsule())
                Text("Exercise complete").font(ScyraTypography.screenTitle)
                Text("Let the stillness come with you.").font(ScyraTypography.body).opacity(0.88)
                VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
                    Text("You completed").font(ScyraTypography.caption).opacity(0.78)
                    Text(exercise.title).font(ScyraTypography.cardTitle)
                    Text(formatDuration(exercise.durationSeconds)).font(ScyraTypography.caption).opacity(0.76)
                }
                .padding(ScyraSpacing.lg)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(.white.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                Text("No points. No pressure. Just a moment returned to yourself.")
                    .font(ScyraTypography.body).opacity(0.82)
                ScyraButton("Return to Focus Room", variant: .secondary, action: viewModel.end)
                    .frame(maxWidth: .infinity)
                    .accessibilityIdentifier("focus-return")
                ScyraButton("Replay exercise", variant: .ghost, action: viewModel.restart)
                    .frame(maxWidth: .infinity)
                    .accessibilityIdentifier("focus-replay")
            }
            .foregroundStyle(.white)
            .padding(ScyraSpacing.xl)
            .background(ScyraColors.primary)
            .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
            .padding(ScyraSpacing.screenPadding)
        }
        .accessibilityIdentifier("focus-completion")
    }

    private func exerciseGlyph(_ id: FocusExerciseID) -> some View {
        ScyraCanonicalIcon(systemName: glyphName(id))
            .font(.system(size: 24, weight: .semibold))
            .foregroundStyle(ScyraColors.primary)
            .frame(width: 52, height: 52)
            .background(ScyraColors.primaryContainer)
            .clipShape(Circle())
            .accessibilityHidden(true)
    }

    private func glyphName(_ id: FocusExerciseID) -> String {
        switch id {
        case .threePointGrounding: "triangle.fill"
        case .boxBreathing: "square"
        case .miniBodyScan: "figure.mind.and.body"
        case .fourSevenEightBreathing: "circle.dotted"
        case .fiveSensesReset: "sparkles"
        }
    }

    private var voiceStatus: String {
        switch (viewModel.voiceEnabled, viewModel.voiceReady) {
        case (true, true): "On. Scyra will gently guide each step."
        case (true, false): "On, but a device voice is unavailable. Follow the text prompts on screen."
        case (false, _): "Off. You can follow the text prompts on screen."
        }
    }

    private func supportingText(_ id: FocusExerciseID, _ type: FocusExerciseStepType) -> String {
        switch type {
        case .breathIn: "Let the breath arrive."
        case .hold: "Hold gently. Do not force."
        case .breathOut: "Let the breath leave slowly."
        default:
            switch id {
            case .threePointGrounding: "Return through one simple point at a time."
            case .miniBodyScan: "Notice the body without needing to change everything."
            case .fiveSensesReset: "Let the senses bring you back to the room."
            default: "Stay gentle. Return when ready."
            }
        }
    }

    private func formatDuration(_ total: Int) -> String {
        let minutes = total / 60
        let seconds = total % 60
        if minutes > 0, seconds > 0 { return "\(minutes)m \(seconds)s" }
        if minutes > 0 { return "\(minutes)m" }
        return "\(seconds)s"
    }

    private func formatClock(_ total: Int) -> String {
        String(format: "%d:%02d", total / 60, total % 60)
    }

    private var roomBackground: some View {
        LinearGradient(
            colors: [ScyraColors.background, ScyraColors.primaryContainer, ScyraColors.background],
            startPoint: .top,
            endPoint: .bottom
        ).ignoresSafeArea()
    }
}

private struct FocusAmbientVisual: View {
    let color: Color

    var body: some View {
        ZStack {
            Circle().fill(color.opacity(0.12))
            Circle().stroke(color.opacity(0.32), lineWidth: 5).padding(16)
            Circle().fill(color.opacity(0.18)).padding(34)
            Circle().fill(color.opacity(0.42)).padding(48)
        }
        .accessibilityHidden(true)
    }
}

private struct FocusStepVisual: View {
    let step: FocusGuidedExerciseStep
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            Circle().fill(Color.white.opacity(0.12))
            Circle().stroke(Color.white.opacity(0.42), lineWidth: 5).padding(24)
            Circle().fill(Color.white.opacity(0.18)).padding(48)
            Circle().fill(Color.white.opacity(0.38)).padding(70)
        }
        .frame(width: 180, height: 180)
        .scaleEffect(scale)
        .animation(
            reduceMotion ? nil : .easeInOut(duration: Double(max(1, step.durationSeconds))),
            value: step.visualState
        )
        .accessibilityLabel(step.type.isBreathPhase ? "Breathing visual for \(step.displayText)." : "Calm stillness visual.")
    }

    private var scale: CGFloat {
        switch step.visualState {
        case .expand, .full: 1.18
        case .contract, .small: 0.78
        case .grounding: 1.03
        case .body: 1.06
        case .still: 1
        }
    }
}

#Preview {
    FocusRoomView(viewModel: FocusRoomViewModel())
}
