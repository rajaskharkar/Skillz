import AVFoundation
import Combine
import Foundation
import Speech

struct ChronicleDictationTextSession: Equatable {
    let original: String
    let selection: NSRange
    private(set) var hypothesis = ""

    init(original: String, selection: NSRange? = nil) {
        self.original = original
        let length = (original as NSString).length
        let requested = selection ?? NSRange(location: length, length: 0)
        let start = min(max(0, requested.location), length)
        let end = min(max(start, requested.location + requested.length), length)
        self.selection = NSRange(location: start, length: end - start)
    }

    mutating func partial(_ value: String) -> String {
        hypothesis = value
        return current
    }

    var current: String {
        let source = original as NSString
        let prefix = source.substring(to: selection.location)
        let suffix = source.substring(from: selection.location + selection.length)
        let before = needsSpace(between: prefix.last, and: hypothesis.first) ? " " : ""
        let after = needsSpace(between: hypothesis.last, and: suffix.first) ? " " : ""
        return prefix + before + hypothesis + after + suffix
    }

    private func needsSpace(between lhs: Character?, and rhs: Character?) -> Bool {
        guard !hypothesis.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let lhs, let rhs else { return false }
        return lhs.isLetterOrNumber && rhs.isLetterOrNumber
    }
}

private extension String {
    var first: Character? { isEmpty ? nil : self[startIndex] }
    var last: Character? { isEmpty ? nil : self[index(before: endIndex)] }
}

@MainActor
final class ChronicleAudioCaptureController: NSObject, ObservableObject, @preconcurrency AVAudioRecorderDelegate {
    @Published private(set) var isRecording = false
    @Published private(set) var isFinishing = false
    @Published private(set) var elapsedMs: Int64 = 0
    @Published private(set) var amplitudes: [Double] = []
    @Published private(set) var microphoneDenied = false
    @Published private(set) var failed = false

    private var recorder: AVAudioRecorder?
    private var staged: ChronicleStagedVoice?
    private var fileStore: ChronicleFileStore?
    private var startedAt: Date?
    private var meterTask: Task<Void, Never>?
    private var interruptionObserver: NSObjectProtocol?
    private var resetObserver: NSObjectProtocol?
    private var operationID = UUID()

    override init() {
        super.init()
        let center = NotificationCenter.default
        interruptionObserver = center.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            let type = (notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt)
                .flatMap(AVAudioSession.InterruptionType.init(rawValue:))
            guard type == .began else { return }
            Task { @MainActor [weak self] in await self?.discard(markFailed: true) }
        }
        resetObserver = center.addObserver(
            forName: AVAudioSession.mediaServicesWereResetNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in await self?.discard(markFailed: true) }
        }
    }

    deinit {
        meterTask?.cancel()
        if let interruptionObserver { NotificationCenter.default.removeObserver(interruptionObserver) }
        if let resetObserver { NotificationCenter.default.removeObserver(resetObserver) }
    }

    func start(fileStore: ChronicleFileStore) async {
        guard !isRecording, !isFinishing else { return }
        let operation = UUID()
        operationID = operation
        failed = false
        microphoneDenied = false
        guard await Self.requestMicrophonePermission() else {
            guard operationID == operation else { return }
            microphoneDenied = true
            return
        }
        guard operationID == operation else { return }

        do {
            let staged = try await fileStore.createVoiceStaging()
            guard operationID == operation else {
                await fileStore.discardVoiceStaging(staged)
                return
            }
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .spokenAudio, options: [.defaultToSpeaker])
            try session.setActive(true)
            let recorder = try AVAudioRecorder(
                url: staged.url,
                settings: [
                    AVFormatIDKey: kAudioFormatMPEG4AAC,
                    AVSampleRateKey: 44_100,
                    AVNumberOfChannelsKey: 1,
                    AVEncoderBitRateKey: 128_000,
                    AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue,
                ]
            )
            recorder.delegate = self
            recorder.isMeteringEnabled = true
            guard recorder.prepareToRecord(), recorder.record() else {
                await fileStore.discardVoiceStaging(staged)
                throw ChronicleAudioError.captureUnavailable
            }
            guard operationID == operation else {
                recorder.stop()
                await fileStore.discardVoiceStaging(staged)
                deactivateAudioSession()
                return
            }
            self.fileStore = fileStore
            self.staged = staged
            self.recorder = recorder
            startedAt = Date()
            elapsedMs = 0
            amplitudes = []
            isRecording = true
            startMetering()
        } catch {
            guard operationID == operation else { return }
            failed = true
            deactivateAudioSession()
        }
    }

    func finish() async -> ChronicleStagedVoice? {
        guard isRecording, let recorder, let staged, let startedAt else { return nil }
        let operation = operationID
        isFinishing = true
        let elapsed = Date().timeIntervalSince(startedAt)
        if elapsed < 0.9 {
            try? await Task.sleep(for: .seconds(0.9 - elapsed))
        }
        guard operationID == operation,
              self.recorder === recorder,
              self.staged?.url == staged.url else {
            isFinishing = false
            return nil
        }
        let duration = recorder.currentTime
        recorder.stop()
        meterTask?.cancel()
        meterTask = nil
        self.recorder = nil
        self.staged = nil
        self.startedAt = nil
        operationID = UUID()
        isRecording = false
        isFinishing = false
        elapsedMs = Int64(max(duration, 0.9) * 1_000)
        deactivateAudioSession()
        return staged
    }

    func discard(markFailed: Bool = false) async {
        operationID = UUID()
        meterTask?.cancel()
        meterTask = nil
        recorder?.stop()
        recorder = nil
        if let staged, let fileStore { await fileStore.discardVoiceStaging(staged) }
        staged = nil
        startedAt = nil
        isRecording = false
        isFinishing = false
        elapsedMs = 0
        amplitudes = []
        if markFailed { failed = true }
        deactivateAudioSession()
    }

    func audioRecorderEncodeErrorDidOccur(_ recorder: AVAudioRecorder, error: Error?) {
        Task { @MainActor [weak self] in await self?.discard(markFailed: true) }
    }

    private func startMetering() {
        meterTask?.cancel()
        meterTask = Task { @MainActor [weak self] in
            while !Task.isCancelled, let self, self.isRecording, let recorder = self.recorder {
                recorder.updateMeters()
                let normalized = min(1, max(0, pow(10, Double(recorder.peakPower(forChannel: 0)) / 20)))
                self.amplitudes = Array((self.amplitudes + [normalized]).suffix(48))
                self.elapsedMs = Int64(max(0, Date().timeIntervalSince(self.startedAt ?? Date())) * 1_000)
                try? await Task.sleep(for: .milliseconds(100))
            }
        }
    }

    private func deactivateAudioSession() {
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    static func requestMicrophonePermission() async -> Bool {
        switch AVAudioApplication.shared.recordPermission {
        case .granted:
            true
        case .denied:
            false
        case .undetermined:
            await withCheckedContinuation { continuation in
                AVAudioApplication.requestRecordPermission { continuation.resume(returning: $0) }
            }
        @unknown default:
            false
        }
    }
}

enum ChronicleAudioError: Error {
    case captureUnavailable
    case recognitionUnavailable
    case noSpeech
}

@MainActor
final class ChronicleSpeechController: ObservableObject {
    @Published private(set) var isDictating = false
    @Published private(set) var latestDictation = ""
    @Published private(set) var unavailable = false
    @Published private(set) var microphoneDenied = false

    private let audioEngine = AVAudioEngine()
    private var liveRequest: SFSpeechAudioBufferRecognitionRequest?
    private var liveTask: SFSpeechRecognitionTask?
    private var liveTapInstalled = false
    private var liveOperation = UUID()
    private var liveCompletion: ((Result<String, Error>) -> Void)?
    private var fileTasks: [UUID: SFSpeechRecognitionTask] = [:]
    private var fileTokens: [UUID: UUID] = [:]
    private let notificationCenter: NotificationCenter
    private var interruptionObserver: NSObjectProtocol?
    private var resetObserver: NSObjectProtocol?

    init(notificationCenter: NotificationCenter = .default) {
        self.notificationCenter = notificationCenter
        interruptionObserver = notificationCenter.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            let type = (notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt)
                .flatMap(AVAudioSession.InterruptionType.init(rawValue:))
            guard type == .began else { return }
            Task { @MainActor [weak self] in self?.interruptLiveDictation() }
        }
        resetObserver = notificationCenter.addObserver(
            forName: AVAudioSession.mediaServicesWereResetNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in self?.interruptLiveDictation() }
        }
    }

    deinit {
        if let interruptionObserver { notificationCenter.removeObserver(interruptionObserver) }
        if let resetObserver { notificationCenter.removeObserver(resetObserver) }
    }

    var transcriptionSupported: Bool {
        SFSpeechRecognizer(locale: .current) != nil
    }

    func startDictation(
        onPartial: @escaping (String) -> Void,
        onCompletion: @escaping (Result<String, Error>) -> Void
    ) async {
        guard !isDictating, liveCompletion == nil else { return }
        let operation = UUID()
        liveOperation = operation
        liveCompletion = onCompletion
        unavailable = false
        microphoneDenied = false
        let speechAuthorized = await Self.requestSpeechPermission()
        guard liveOperation == operation else { return }
        guard speechAuthorized,
              let recognizer = SFSpeechRecognizer(locale: .current), recognizer.isAvailable else {
            unavailable = true
            completeLive(.failure(ChronicleAudioError.recognitionUnavailable))
            return
        }
        let microphoneAuthorized = await ChronicleAudioCaptureController.requestMicrophonePermission()
        guard liveOperation == operation else { return }
        guard microphoneAuthorized else {
            microphoneDenied = true
            completeLive(.failure(ChronicleAudioError.captureUnavailable))
            return
        }

        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .measurement, options: [.duckOthers])
            try session.setActive(true)
            let input = audioEngine.inputNode
            let format = input.outputFormat(forBus: 0)
            guard format.sampleRate > 0, format.channelCount > 0 else {
                throw ChronicleAudioError.captureUnavailable
            }
            let request = SFSpeechAudioBufferRecognitionRequest()
            request.shouldReportPartialResults = true
            request.requiresOnDeviceRecognition = recognizer.supportsOnDeviceRecognition
            input.installTap(onBus: 0, bufferSize: 1_024, format: format) { buffer, _ in
                request.append(buffer)
            }
            liveTapInstalled = true
            audioEngine.prepare()
            try audioEngine.start()

            liveRequest = request
            latestDictation = ""
            isDictating = true
            liveTask = recognizer.recognitionTask(with: request) { [weak self] result, error in
                Task { @MainActor [weak self] in
                    guard let self, self.liveOperation == operation, self.isDictating else { return }
                    if let result {
                        let text = result.bestTranscription.formattedString
                        self.latestDictation = text
                        onPartial(text)
                        if result.isFinal { self.completeLive(.success(text)) }
                    } else if let error {
                        if !self.latestDictation.isEmpty {
                            self.completeLive(.success(self.latestDictation))
                        } else {
                            self.completeLive(.failure(error))
                        }
                    }
                }
            }
        } catch {
            guard liveOperation == operation else { return }
            unavailable = true
            completeLive(.failure(error))
        }
    }

    func finishDictation() {
        guard isDictating else { return }
        liveRequest?.endAudio()
        if audioEngine.isRunning { audioEngine.stop() }
        removeLiveTap()
        liveTask?.finish()
        let operation = liveOperation
        Task { @MainActor [weak self] in
            try? await Task.sleep(for: .milliseconds(600))
            guard let self, self.isDictating, self.liveOperation == operation else { return }
            if self.latestDictation.isEmpty {
                self.completeLive(.failure(ChronicleAudioError.noSpeech))
            } else {
                self.completeLive(.success(self.latestDictation))
            }
        }
    }

    func cancelDictation() {
        let completion = liveCompletion
        stopLiveAudio()
        completion?(.failure(CancellationError()))
    }

    func startTranscription(
        id: UUID,
        url: URL,
        onPartial: @escaping (String) -> Void,
        onCompletion: @escaping (Result<String, Error>) -> Void
    ) async {
        cancelTranscription(id: id)
        let token = UUID()
        fileTokens[id] = token
        let speechAuthorized = await Self.requestSpeechPermission()
        guard fileTokens[id] == token else { return }
        guard speechAuthorized,
              let recognizer = SFSpeechRecognizer(locale: .current), recognizer.isAvailable else {
            fileTokens.removeValue(forKey: id)
            onCompletion(.failure(ChronicleAudioError.recognitionUnavailable))
            return
        }
        beginFileRecognition(
            id: id,
            token: token,
            url: url,
            recognizer: recognizer,
            preferOnDevice: recognizer.supportsOnDeviceRecognition,
            onPartial: onPartial,
            onCompletion: onCompletion
        )
    }

    func cancelTranscription(id: UUID) {
        fileTokens.removeValue(forKey: id)
        fileTasks.removeValue(forKey: id)?.cancel()
    }

    func cancelAll() {
        cancelDictation()
        for id in Array(fileTasks.keys) { cancelTranscription(id: id) }
    }

    private func interruptLiveDictation() {
        guard liveCompletion != nil || isDictating else { return }
        unavailable = true
        completeLive(.failure(ChronicleAudioError.captureUnavailable))
    }

    private func beginFileRecognition(
        id: UUID,
        token: UUID,
        url: URL,
        recognizer: SFSpeechRecognizer,
        preferOnDevice: Bool,
        onPartial: @escaping (String) -> Void,
        onCompletion: @escaping (Result<String, Error>) -> Void
    ) {
        fileTokens[id] = token
        let request = SFSpeechURLRecognitionRequest(url: url)
        request.shouldReportPartialResults = true
        request.requiresOnDeviceRecognition = preferOnDevice
        fileTasks[id] = recognizer.recognitionTask(with: request) { [weak self] result, error in
            Task { @MainActor [weak self] in
                guard let self, self.fileTokens[id] == token else { return }
                if let result {
                    let text = result.bestTranscription.formattedString
                    if !text.isEmpty { onPartial(text) }
                    if result.isFinal {
                        self.fileTokens.removeValue(forKey: id)
                        self.fileTasks.removeValue(forKey: id)
                        onCompletion(text.isEmpty ? .failure(ChronicleAudioError.noSpeech) : .success(text))
                    }
                } else if let error {
                    self.fileTasks.removeValue(forKey: id)
                    if preferOnDevice {
                        self.beginFileRecognition(
                            id: id,
                            token: token,
                            url: url,
                            recognizer: recognizer,
                            preferOnDevice: false,
                            onPartial: onPartial,
                            onCompletion: onCompletion
                        )
                    } else {
                        self.fileTokens.removeValue(forKey: id)
                        onCompletion(.failure(error))
                    }
                }
            }
        }
    }

    private func completeLive(_ result: Result<String, Error>) {
        let completion = liveCompletion
        stopLiveAudio()
        completion?(result)
    }

    private func stopLiveAudio() {
        liveOperation = UUID()
        if audioEngine.isRunning { audioEngine.stop() }
        removeLiveTap()
        liveRequest?.endAudio()
        liveTask?.cancel()
        liveRequest = nil
        liveTask = nil
        liveCompletion = nil
        isDictating = false
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    private func removeLiveTap() {
        guard liveTapInstalled else { return }
        audioEngine.inputNode.removeTap(onBus: 0)
        liveTapInstalled = false
    }

    static func requestSpeechPermission() async -> Bool {
        switch SFSpeechRecognizer.authorizationStatus() {
        case .authorized:
            true
        case .notDetermined:
            await withCheckedContinuation { continuation in
                SFSpeechRecognizer.requestAuthorization { status in
                    continuation.resume(returning: status == .authorized)
                }
            }
        case .denied, .restricted:
            false
        @unknown default:
            false
        }
    }
}

@MainActor
final class ChronicleAudioPlaybackController: ObservableObject {
    @Published private(set) var activeSourceID: UUID?
    @Published private(set) var isPlaying = false
    @Published private(set) var positionMs: Int64 = 0
    @Published private(set) var durationMs: Int64 = 0
    @Published private(set) var hasError = false

    private var player: AVPlayer?
    private var timeObserver: Any?
    private var endObserver: NSObjectProtocol?
    private var interruptionObserver: NSObjectProtocol?

    init() {
        interruptionObserver = NotificationCenter.default.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            let type = (notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt)
                .flatMap(AVAudioSession.InterruptionType.init(rawValue:))
            if type == .began { Task { @MainActor [weak self] in self?.pause() } }
        }
    }

    deinit {
        if let timeObserver { player?.removeTimeObserver(timeObserver) }
        if let endObserver { NotificationCenter.default.removeObserver(endObserver) }
        if let interruptionObserver { NotificationCenter.default.removeObserver(interruptionObserver) }
    }

    func toggle(sourceID: UUID, url: URL, fallbackDurationMs: Int64) {
        if activeSourceID == sourceID {
            isPlaying ? pause() : play()
        } else {
            prepare(sourceID: sourceID, url: url, fallbackDurationMs: fallbackDurationMs)
            play()
        }
    }

    func seek(sourceID: UUID, url: URL, fallbackDurationMs: Int64, to milliseconds: Int64) {
        if activeSourceID != sourceID {
            prepare(sourceID: sourceID, url: url, fallbackDurationMs: fallbackDurationMs)
        }
        let bounded = min(max(0, milliseconds), max(0, durationMs))
        player?.seek(to: CMTime(value: bounded, timescale: 1_000), toleranceBefore: .zero, toleranceAfter: .zero)
        positionMs = bounded
    }

    func pause() {
        player?.pause()
        isPlaying = false
    }

    func stop() {
        pause()
        player?.seek(to: .zero)
        positionMs = 0
    }

    private func prepare(sourceID: UUID, url: URL, fallbackDurationMs: Int64) {
        tearDownPlayer()
        guard FileManager.default.fileExists(atPath: url.path) else {
            activeSourceID = sourceID
            hasError = true
            durationMs = fallbackDurationMs
            return
        }
        let player = AVPlayer(url: url)
        self.player = player
        activeSourceID = sourceID
        positionMs = 0
        durationMs = fallbackDurationMs
        hasError = false
        timeObserver = player.addPeriodicTimeObserver(
            forInterval: CMTime(value: 200, timescale: 1_000),
            queue: .main
        ) { [weak self, weak player] time in
            Task { @MainActor [weak self, weak player] in
                guard let self, let player else { return }
                self.positionMs = Int64(max(0, time.seconds) * 1_000)
                if let seconds = player.currentItem?.duration.seconds,
                   seconds.isFinite, seconds > 0 {
                    self.durationMs = Int64(seconds * 1_000)
                }
                if player.currentItem?.status == .failed {
                    self.hasError = true
                    self.isPlaying = false
                }
            }
        }
        endObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: player.currentItem,
            queue: .main
        ) { [weak self, weak player] _ in
            player?.seek(to: .zero)
            Task { @MainActor [weak self] in
                self?.positionMs = 0
                self?.isPlaying = false
            }
        }
    }

    private func play() {
        guard let player, !hasError else { return }
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio)
            try AVAudioSession.sharedInstance().setActive(true)
            player.play()
            isPlaying = true
        } catch {
            hasError = true
            isPlaying = false
        }
    }

    private func tearDownPlayer() {
        if let timeObserver { player?.removeTimeObserver(timeObserver) }
        timeObserver = nil
        if let endObserver { NotificationCenter.default.removeObserver(endObserver) }
        endObserver = nil
        player?.pause()
        player = nil
        isPlaying = false
    }
}

extension Character {
    fileprivate var isLetterOrNumber: Bool {
        unicodeScalars.allSatisfy { CharacterSet.alphanumerics.contains($0) }
    }
}
