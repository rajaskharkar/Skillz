import AVFoundation
import Combine
import Foundation

@MainActor
protocol FocusExerciseVoiceGuiding: AnyObject {
    var isAvailable: Bool { get }
    func speak(_ text: String)
    func stop()
}

@MainActor
final class FocusExerciseVoiceGuide: NSObject, FocusExerciseVoiceGuiding {
    private let synthesizer = AVSpeechSynthesizer()

    var isAvailable: Bool {
        AVSpeechSynthesisVoice(language: "en-GB") != nil || AVSpeechSynthesisVoice(language: "en-US") != nil
    }

    func speak(_ text: String) {
        guard isAvailable else { return }
        synthesizer.stopSpeaking(at: .immediate)
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "en-GB")
            ?? AVSpeechSynthesisVoice(language: "en-US")
        utterance.rate = 0.38
        utterance.pitchMultiplier = 0.94
        synthesizer.speak(utterance)
    }

    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
    }
}

@MainActor
final class FocusRoomViewModel: ObservableObject {
    @Published private(set) var selectedExercise: FocusGuidedExercise?
    @Published private(set) var currentStepIndex = 0
    @Published private(set) var elapsedInCurrentStep = 0
    @Published private(set) var totalElapsedSeconds = 0
    @Published private(set) var hasStarted = false
    @Published private(set) var isPlaying = false
    @Published private(set) var isCompleted = false
    @Published var voiceEnabled = true {
        didSet {
            if !voiceEnabled {
                voiceGuide.stop()
            } else if isPlaying {
                speakCurrentStep()
            }
        }
    }

    var voiceReady: Bool { voiceGuide.isAvailable }
    var voiceMessage: String? {
        voiceReady ? nil : "English voice guidance is unavailable on this device. You can still follow the text prompts."
    }
    var currentStep: FocusGuidedExerciseStep? { selectedExercise?.steps[safe: currentStepIndex] }
    var progress: Double {
        guard let duration = selectedExercise?.durationSeconds, duration > 0 else { return 0 }
        return min(1, Double(totalElapsedSeconds) / Double(duration))
    }
    var remainingSeconds: Int {
        max(0, (selectedExercise?.durationSeconds ?? 0) - totalElapsedSeconds)
    }

    private let voiceGuide: FocusExerciseVoiceGuiding
    private let now: () -> Date
    private var runStartedAt: Date?
    private var elapsedBeforeRun = 0
    private var ticker: Task<Void, Never>?

    init(
        voiceGuide: FocusExerciseVoiceGuiding? = nil,
        now: @escaping () -> Date = Date.init
    ) {
        self.voiceGuide = voiceGuide ?? FocusExerciseVoiceGuide()
        self.now = now
    }

    deinit { ticker?.cancel() }

    func select(_ exercise: FocusGuidedExercise) {
        stopTickerAndVoice()
        selectedExercise = exercise
        resetProgress(started: false)
    }

    func start() {
        guard selectedExercise != nil else { return }
        resetProgress(started: true)
        isPlaying = true
        runStartedAt = now()
        speakCurrentStep()
        startTicker()
    }

    func pause() {
        guard isPlaying else { return }
        synchronize(at: now())
        elapsedBeforeRun = totalElapsedSeconds
        runStartedAt = nil
        isPlaying = false
        ticker?.cancel()
        ticker = nil
        voiceGuide.stop()
    }

    func resume() {
        guard selectedExercise != nil, hasStarted, !isCompleted, !isPlaying else { return }
        isPlaying = true
        runStartedAt = now()
        speakCurrentStep()
        startTicker()
    }

    func restart() {
        guard selectedExercise != nil else { return }
        start()
    }

    func end() {
        stopTickerAndVoice()
        selectedExercise = nil
        resetProgress(started: false)
    }

    func pauseForBackground() {
        pause()
    }

    /// Reconciles the player from wall-clock time, so delayed timers and device sleep never
    /// make a running exercise take longer than its canonical duration.
    func synchronize(at date: Date) {
        guard isPlaying, let exercise = selectedExercise, let runStartedAt else { return }
        let runElapsed = max(0, Int(date.timeIntervalSince(runStartedAt)))
        let elapsed = min(exercise.durationSeconds, elapsedBeforeRun + runElapsed)
        let oldStepIndex = currentStepIndex
        apply(totalElapsed: elapsed, exercise: exercise)
        if isCompleted {
            stopTickerAndVoice()
        } else if currentStepIndex != oldStepIndex {
            speakCurrentStep()
        }
    }

    private func resetProgress(started: Bool) {
        currentStepIndex = 0
        elapsedInCurrentStep = 0
        totalElapsedSeconds = 0
        elapsedBeforeRun = 0
        runStartedAt = nil
        hasStarted = started
        isPlaying = false
        isCompleted = false
    }

    private func apply(totalElapsed: Int, exercise: FocusGuidedExercise) {
        totalElapsedSeconds = totalElapsed
        if totalElapsed >= exercise.durationSeconds {
            currentStepIndex = max(0, exercise.steps.count - 1)
            elapsedInCurrentStep = exercise.steps.last?.durationSeconds ?? 0
            isPlaying = false
            isCompleted = true
            return
        }

        var prior = 0
        for (index, step) in exercise.steps.enumerated() {
            let boundary = prior + step.durationSeconds
            if totalElapsed < boundary {
                currentStepIndex = index
                elapsedInCurrentStep = totalElapsed - prior
                return
            }
            prior = boundary
        }
    }

    private func startTicker() {
        ticker?.cancel()
        ticker = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(for: .milliseconds(250))
                guard !Task.isCancelled, let self else { return }
                self.synchronize(at: self.now())
                if !self.isPlaying { return }
            }
        }
    }

    private func speakCurrentStep() {
        guard voiceEnabled, voiceReady, let currentStep else { return }
        voiceGuide.speak(currentStep.spokenText)
    }

    private func stopTickerAndVoice() {
        ticker?.cancel()
        ticker = nil
        voiceGuide.stop()
    }
}

private extension Collection {
    subscript(safe index: Index) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
