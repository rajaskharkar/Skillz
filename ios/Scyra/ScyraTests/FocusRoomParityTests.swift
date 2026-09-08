import Foundation
import Testing
@testable import Scyra

@MainActor
struct FocusRoomParityTests {
    @Test func originalFiveCatalogMatchesCanonicalOrderCopyAndDurations() throws {
        let exercises = FocusRoomCatalog.exercises
        #expect(exercises.map(\.id) == [
            .threePointGrounding, .boxBreathing, .miniBodyScan,
            .fourSevenEightBreathing, .fiveSensesReset
        ])
        #expect(exercises.map(\.title) == [
            "Three-Point Grounding", "Box Breathing", "Mini Body Scan",
            "4-7-8 Breathing", "Five Senses Reset"
        ])
        #expect(exercises.map(\.purpose) == ["Return", "Steady", "Soften", "Deepen", "Reset"])
        #expect(exercises.map(\.durationSeconds) == [48, 60, 90, 79, 98])
        #expect(exercises[1].steps.filter(\.type.isBreathPhase).count == 12)
        #expect(exercises[3].steps.filter(\.type.isBreathPhase).map(\.durationSeconds) == [4, 7, 8, 4, 7, 8, 4, 7, 8])
    }

    @Test func playerDerivesStepsFromWallClockAndCompletesAtCanonicalDuration() throws {
        let time = MutableDate(Date(timeIntervalSince1970: 1_000))
        let voice = RecordingVoiceGuide()
        let model = FocusRoomViewModel(voiceGuide: voice, now: { time.value })
        let exercise = try #require(FocusRoomCatalog.exercise(id: .threePointGrounding))

        model.select(exercise)
        model.start()
        #expect(model.isPlaying)
        #expect(voice.spoken == [exercise.steps[0].spokenText])

        time.value = time.value.addingTimeInterval(6)
        model.synchronize(at: time.value)
        #expect(model.currentStepIndex == 1)
        #expect(model.elapsedInCurrentStep == 0)
        #expect(voice.spoken.last == exercise.steps[1].spokenText)

        time.value = Date(timeIntervalSince1970: 1_048)
        model.synchronize(at: time.value)
        #expect(model.totalElapsedSeconds == 48)
        #expect(model.isCompleted)
        #expect(!model.isPlaying)
        #expect(voice.stopCount > 0)
    }

    @Test func pauseFreezesElapsedTimeAndResumeContinuesWithoutSkipping() throws {
        let time = MutableDate(Date(timeIntervalSince1970: 2_000))
        let voice = RecordingVoiceGuide()
        let model = FocusRoomViewModel(voiceGuide: voice, now: { time.value })
        let exercise = try #require(FocusRoomCatalog.exercise(id: .boxBreathing))
        model.select(exercise)
        model.start()

        time.value = Date(timeIntervalSince1970: 2_010)
        model.pause()
        #expect(model.totalElapsedSeconds == 10)
        #expect(!model.isPlaying)
        time.value = Date(timeIntervalSince1970: 2_040)
        model.synchronize(at: time.value)
        #expect(model.totalElapsedSeconds == 10)

        model.resume()
        time.value = Date(timeIntervalSince1970: 2_043)
        model.synchronize(at: time.value)
        #expect(model.totalElapsedSeconds == 13)
        #expect(model.currentStep?.displayText == "Hold")
    }

    @Test func voiceCanBeDisabledWithoutAffectingExerciseProgress() throws {
        let time = MutableDate(Date(timeIntervalSince1970: 3_000))
        let voice = RecordingVoiceGuide()
        let model = FocusRoomViewModel(voiceGuide: voice, now: { time.value })
        model.voiceEnabled = false
        model.select(try #require(FocusRoomCatalog.exercise(id: .miniBodyScan)))
        model.start()
        time.value = Date(timeIntervalSince1970: 3_020)
        model.synchronize(at: time.value)
        #expect(voice.spoken.isEmpty)
        #expect(model.totalElapsedSeconds == 20)
        #expect(model.isPlaying)
    }
}

@MainActor
private final class RecordingVoiceGuide: FocusExerciseVoiceGuiding {
    var isAvailable = true
    var spoken: [String] = []
    var stopCount = 0

    func speak(_ text: String) { spoken.append(text) }
    func stop() { stopCount += 1 }
}

private final class MutableDate {
    var value: Date
    init(_ value: Date) { self.value = value }
}
