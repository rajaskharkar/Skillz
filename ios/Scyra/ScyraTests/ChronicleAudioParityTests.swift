import AVFoundation
import Foundation
import SwiftData
import Testing
@testable import Scyra

@MainActor
struct ChronicleAudioParityTests {
    @Test func dictationSessionReplacesSelectionWithoutDuplicatingHypotheses() {
        var session = ChronicleDictationTextSession(
            original: "Plan old thought today",
            selection: NSRange(location: 5, length: 11)
        )

        #expect(session.partial("a") == "Plan a today")
        #expect(session.partial("a clearer thought") == "Plan a clearer thought today")
        #expect(session.current == "Plan a clearer thought today")
        #expect(session.original == "Plan old thought today")
    }

    @Test func voiceCommitPersistsMetadataAndManualTranscriptSurvivesRetranscription() async throws {
        let baseURL = ChronicleFileStore.temporaryBaseURL()
        defer { try? FileManager.default.removeItem(at: baseURL) }
        let fileStore = ChronicleFileStore(baseURL: baseURL)
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container, chronicleFileStore: fileStore)
        let owner = ChronicleOwner.activeFlow(UUID())
        let staged = try await makeStagedVoice(fileStore: fileStore)

        var snapshot = try await repository.addChronicleVoice(owner: owner, staged: staged)
        let voice = try #require(snapshot.moments.first)
        #expect(voice.type == .voice)
        #expect(voice.mimeType == "audio/mp4")
        #expect((voice.durationMs ?? 0) > 0)
        let ownedURL = try #require(voice.audioPath.flatMap(fileStore.resolve))
        #expect(FileManager.default.fileExists(atPath: ownedURL.path))

        snapshot = try repository.updateChronicleTranscript(
            owner: owner,
            momentID: voice.id,
            transcript: "original words",
            manuallyEdited: false
        )
        #expect(snapshot.moments[0].originalTranscript == "original words")
        #expect(snapshot.moments[0].transcript == "original words")

        _ = try repository.updateChronicleTranscript(
            owner: owner,
            momentID: voice.id,
            transcript: "carefully edited",
            manuallyEdited: true
        )
        snapshot = try repository.updateChronicleTranscript(
            owner: owner,
            momentID: voice.id,
            transcript: "new recognition",
            manuallyEdited: false
        )
        #expect(snapshot.moments[0].originalTranscript == "new recognition")
        #expect(snapshot.moments[0].transcript == "carefully edited")
        #expect(snapshot.moments[0].transcriptEdited)

        _ = try repository.deleteChronicleMoment(owner: owner, momentID: voice.id)
        #expect(!FileManager.default.fileExists(atPath: ownedURL.path))
    }

    @Test func invalidVoiceStagingIsRejectedAndCleaned() async throws {
        let baseURL = ChronicleFileStore.temporaryBaseURL()
        defer { try? FileManager.default.removeItem(at: baseURL) }
        let fileStore = ChronicleFileStore(baseURL: baseURL)
        let staged = try await fileStore.createVoiceStaging()
        try Data("not audio".utf8).write(to: staged.url)

        await #expect(throws: ChronicleFileStoreError.self) {
            try await fileStore.finalizeVoice(chronicleID: UUID(), staged: staged)
        }
        #expect(!FileManager.default.fileExists(atPath: staged.url.deletingLastPathComponent().path))
    }

    private func makeStagedVoice(fileStore: ChronicleFileStore) async throws -> ChronicleStagedVoice {
        let staged = try await fileStore.createVoiceStaging()
        let settings: [String: Any] = [
            AVFormatIDKey: kAudioFormatMPEG4AAC,
            AVSampleRateKey: 44_100,
            AVNumberOfChannelsKey: 1,
            AVEncoderBitRateKey: 128_000,
        ]
        do {
            let file = try AVAudioFile(forWriting: staged.url, settings: settings)
            let format = file.processingFormat
            let frames = AVAudioFrameCount(format.sampleRate)
            let buffer = try #require(AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frames))
            buffer.frameLength = frames
            if let channels = buffer.floatChannelData {
                for channel in 0..<Int(format.channelCount) {
                    channels[channel].initialize(repeating: 0, count: Int(frames))
                }
            }
            try file.write(from: buffer)
        }
        return staged
    }
}
