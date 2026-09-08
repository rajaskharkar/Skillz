import AVFoundation
import Foundation
import ImageIO
import UniformTypeIdentifiers

struct ChronicleMediaSource: Sendable {
    let url: URL
    let contentTypeIdentifier: String
    let deleteWhenFinished: Bool

    init(url: URL, contentTypeIdentifier: String, deleteWhenFinished: Bool = false) {
        self.url = url
        self.contentTypeIdentifier = contentTypeIdentifier
        self.deleteWhenFinished = deleteWhenFinished
    }
}

struct ChronicleMediaImportResult: Equatable, Sendable {
    let momentID: UUID?
    let importedCount: Int
    let failedCount: Int
}

struct ChronicleMediaStageResult: Equatable, Sendable {
    let items: [ChronicleMediaItem]
    let failedCount: Int

    var importedCount: Int { items.count }
}

struct ChronicleStagedVoice: Sendable {
    let url: URL
}

enum ChronicleFileStoreError: Error, Equatable {
    case unsupportedType
    case unavailableSource
    case emptyFile
    case corruptMedia
    case unsafePath
    case incompleteCopy
}

/// Durable, app-owned storage for Chronicle media. SwiftData owns metadata; this
/// actor owns binary import/validation and exposes narrowly scoped cleanup APIs.
actor ChronicleFileStore {
    struct StoredMedia: Sendable {
        let id: UUID
        let localPath: String
        let mimeType: String
        let durationMs: Int64?
        let width: Int?
        let height: Int?
        let createdAt: Date

        func item(position: Int) -> ChronicleMediaItem {
            ChronicleMediaItem(
                id: id,
                position: position,
                localPath: localPath,
                mimeType: mimeType,
                durationMs: durationMs,
                width: width,
                height: height,
                thumbnailPath: nil,
                createdAt: createdAt
            )
        }
    }

    struct StoredVoice: Sendable {
        let localPath: String
        let mimeType: String
        let durationMs: Int64
    }

    nonisolated let baseURL: URL
    private nonisolated var durableRoot: URL { baseURL.appending(path: "chronicle", directoryHint: .isDirectory) }
    private var stagingRoot: URL { baseURL.appending(path: "chronicle_staging", directoryHint: .isDirectory) }

    init(baseURL: URL = ChronicleFileStore.applicationSupportBaseURL()) {
        self.baseURL = baseURL.standardizedFileURL
    }

    nonisolated static func applicationSupportBaseURL() -> URL {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        return support
            .appending(path: "Scyra", directoryHint: .isDirectory)
            .standardizedFileURL
    }

    nonisolated static func temporaryBaseURL() -> URL {
        FileManager.default.temporaryDirectory
            .appending(path: "ScyraChronicleTests-\(UUID().uuidString)", directoryHint: .isDirectory)
            .standardizedFileURL
    }

    func importMedia(chronicleID: UUID, source: ChronicleMediaSource) async throws -> StoredMedia {
        defer {
            if source.deleteWhenFinished { try? FileManager.default.removeItem(at: source.url) }
        }

        guard let contentType = UTType(source.contentTypeIdentifier),
              contentType.conforms(to: .image) || contentType.conforms(to: .movie) else {
            throw ChronicleFileStoreError.unsupportedType
        }
        guard source.url.isFileURL else { throw ChronicleFileStoreError.unavailableSource }

        let operationID = UUID()
        let operationURL = stagingRoot.appending(path: operationID.uuidString, directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: operationURL, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: operationURL) }

        let identity = UUID()
        let sourceExtension = source.url.pathExtension
        let pathExtension = contentType.preferredFilenameExtension
            ?? (sourceExtension.isEmpty ? nil : sourceExtension)
            ?? (contentType.conforms(to: .image) ? "jpg" : "mov")
        let fileName = "\(identity.uuidString).\(pathExtension)"
        let stagedURL = operationURL.appending(path: fileName)

        let accessed = source.url.startAccessingSecurityScopedResource()
        defer { if accessed { source.url.stopAccessingSecurityScopedResource() } }
        guard FileManager.default.fileExists(atPath: source.url.path) else {
            throw ChronicleFileStoreError.unavailableSource
        }
        try FileManager.default.copyItem(at: source.url, to: stagedURL)

        let stagedSize = try fileSize(at: stagedURL)
        guard stagedSize > 0 else { throw ChronicleFileStoreError.emptyFile }
        let metadata = try await mediaMetadata(at: stagedURL, contentType: contentType)

        let chronicleSegment = chronicleID.uuidString
        guard Self.isSafePathSegment(chronicleSegment) else { throw ChronicleFileStoreError.unsafePath }
        let destinationDirectory = durableRoot
            .appending(path: chronicleSegment, directoryHint: .isDirectory)
            .appending(path: "media", directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: destinationDirectory, withIntermediateDirectories: true)
        let destination = destinationDirectory.appending(path: fileName)

        do {
            try FileManager.default.moveItem(at: stagedURL, to: destination)
            guard try fileSize(at: destination) == stagedSize else {
                throw ChronicleFileStoreError.incompleteCopy
            }
        } catch {
            try? FileManager.default.removeItem(at: destination)
            throw error
        }

        let mimeType = contentType.preferredMIMEType
            ?? (contentType.conforms(to: .image) ? "image/jpeg" : "video/quicktime")
        return StoredMedia(
            id: identity,
            localPath: "chronicle/\(chronicleSegment)/media/\(fileName)",
            mimeType: mimeType,
            durationMs: metadata.durationMs,
            width: metadata.width,
            height: metadata.height,
            createdAt: Date()
        )
    }

    func createVoiceStaging() throws -> ChronicleStagedVoice {
        let operationURL = stagingRoot.appending(path: UUID().uuidString, directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: operationURL, withIntermediateDirectories: true)
        return ChronicleStagedVoice(url: operationURL.appending(path: "voice.m4a"))
    }

    func finalizeVoice(chronicleID: UUID, staged: ChronicleStagedVoice) async throws -> StoredVoice {
        guard ownsVoiceStagingURL(staged.url),
              FileManager.default.fileExists(atPath: staged.url.path) else {
            throw ChronicleFileStoreError.unavailableSource
        }
        let operationURL = staged.url.deletingLastPathComponent()
        defer { try? FileManager.default.removeItem(at: operationURL) }

        let stagedSize = try fileSize(at: staged.url)
        guard stagedSize > 0 else { throw ChronicleFileStoreError.emptyFile }
        let seconds: Double
        let tracks: [AVAssetTrack]
        do {
            let asset = AVURLAsset(url: staged.url)
            seconds = try await asset.load(.duration).seconds
            tracks = try await asset.loadTracks(withMediaType: .audio)
        } catch {
            throw ChronicleFileStoreError.corruptMedia
        }
        guard seconds.isFinite, seconds > 0, !tracks.isEmpty else {
            throw ChronicleFileStoreError.corruptMedia
        }

        let chronicleSegment = chronicleID.uuidString
        guard Self.isSafePathSegment(chronicleSegment) else { throw ChronicleFileStoreError.unsafePath }
        let destinationDirectory = durableRoot
            .appending(path: chronicleSegment, directoryHint: .isDirectory)
            .appending(path: "audio", directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: destinationDirectory, withIntermediateDirectories: true)
        let fileName = "\(UUID().uuidString).m4a"
        let destination = destinationDirectory.appending(path: fileName)

        do {
            try FileManager.default.moveItem(at: staged.url, to: destination)
            guard try fileSize(at: destination) == stagedSize else {
                throw ChronicleFileStoreError.incompleteCopy
            }
        } catch {
            try? FileManager.default.removeItem(at: destination)
            throw error
        }

        return StoredVoice(
            localPath: "chronicle/\(chronicleSegment)/audio/\(fileName)",
            mimeType: "audio/mp4",
            durationMs: Int64((seconds * 1_000).rounded())
        )
    }

    func discardVoiceStaging(_ staged: ChronicleStagedVoice) {
        guard ownsVoiceStagingURL(staged.url) else { return }
        try? FileManager.default.removeItem(at: staged.url.deletingLastPathComponent())
    }

    nonisolated func resolve(_ localPath: String) -> URL? {
        guard !localPath.isEmpty, !localPath.hasPrefix("/") else { return nil }
        let candidate = baseURL.appending(path: localPath).standardizedFileURL
        let rootPath = durableRoot.standardizedFileURL.path + "/"
        guard candidate.path.hasPrefix(rootPath) else { return nil }
        return candidate
    }

    nonisolated func deleteIfOwned(_ localPath: String) {
        guard let url = resolve(localPath) else { return }
        try? FileManager.default.removeItem(at: url)
        removeEmptyParentDirectories(startingAt: url.deletingLastPathComponent())
    }

    nonisolated func deleteChronicle(_ chronicleID: UUID) {
        guard Self.isSafePathSegment(chronicleID.uuidString) else { return }
        let url = durableRoot.appending(path: chronicleID.uuidString, directoryHint: .isDirectory)
        try? FileManager.default.removeItem(at: url)
    }

    func reconcile(referencedPaths: Set<String>, olderThan: TimeInterval = 24 * 60 * 60) {
        let cutoff = Date().addingTimeInterval(-olderThan)
        removeStaleFiles(in: stagingRoot, cutoff: cutoff, referencedPaths: [])
        removeStaleFiles(in: durableRoot, cutoff: cutoff, referencedPaths: referencedPaths)
    }

    private func mediaMetadata(at url: URL, contentType: UTType) async throws
        -> (durationMs: Int64?, width: Int?, height: Int?) {
        if contentType.conforms(to: .image) {
            guard let source = CGImageSourceCreateWithURL(url as CFURL, nil),
                  CGImageSourceGetCount(source) > 0,
                  let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
                  let rawWidth = properties[kCGImagePropertyPixelWidth] as? Int,
                  let rawHeight = properties[kCGImagePropertyPixelHeight] as? Int,
                  rawWidth > 0,
                  rawHeight > 0 else {
                throw ChronicleFileStoreError.corruptMedia
            }
            let orientation = properties[kCGImagePropertyOrientation] as? Int ?? 1
            let swapsDimensions = [5, 6, 7, 8].contains(orientation)
            return (nil, swapsDimensions ? rawHeight : rawWidth, swapsDimensions ? rawWidth : rawHeight)
        }

        let asset = AVURLAsset(url: url)
        let duration = try await asset.load(.duration)
        let durationSeconds = duration.seconds
        guard durationSeconds.isFinite, durationSeconds >= 0 else {
            throw ChronicleFileStoreError.corruptMedia
        }
        let tracks = try await asset.loadTracks(withMediaType: .video)
        guard let track = tracks.first else { throw ChronicleFileStoreError.corruptMedia }
        let naturalSize = try await track.load(.naturalSize)
        let transform = try await track.load(.preferredTransform)
        let transformed = CGRect(origin: .zero, size: naturalSize).applying(transform)
        let width = Int(abs(transformed.width).rounded())
        let height = Int(abs(transformed.height).rounded())
        guard width > 0, height > 0 else { throw ChronicleFileStoreError.corruptMedia }
        return (Int64((durationSeconds * 1_000).rounded()), width, height)
    }

    private func fileSize(at url: URL) throws -> Int64 {
        let values = try url.resourceValues(forKeys: [.fileSizeKey])
        return Int64(values.fileSize ?? 0)
    }

    private func ownsVoiceStagingURL(_ url: URL) -> Bool {
        let candidate = url.standardizedFileURL
        let rootPath = stagingRoot.standardizedFileURL.path + "/"
        return candidate.path.hasPrefix(rootPath)
            && candidate.lastPathComponent == "voice.m4a"
            && candidate.deletingLastPathComponent() != stagingRoot.standardizedFileURL
    }

    private func removeStaleFiles(in root: URL, cutoff: Date, referencedPaths: Set<String>) {
        guard let enumerator = FileManager.default.enumerator(
            at: root,
            includingPropertiesForKeys: [.isRegularFileKey, .contentModificationDateKey],
            options: [.skipsHiddenFiles]
        ) else { return }
        var directories: [URL] = []
        for case let url as URL in enumerator {
            let values = try? url.resourceValues(forKeys: [.isRegularFileKey, .contentModificationDateKey])
            if values?.isRegularFile == true {
                let relative = url.path.replacingOccurrences(of: baseURL.path + "/", with: "")
                if (values?.contentModificationDate ?? .distantPast) < cutoff,
                   !referencedPaths.contains(relative) {
                    try? FileManager.default.removeItem(at: url)
                }
            } else {
                directories.append(url)
            }
        }
        directories.reversed().forEach { directory in
            let contents = try? FileManager.default.contentsOfDirectory(atPath: directory.path)
            if contents?.isEmpty == true { try? FileManager.default.removeItem(at: directory) }
        }
    }

    private nonisolated func removeEmptyParentDirectories(startingAt directory: URL) {
        var candidate = directory.standardizedFileURL
        let root = durableRoot.standardizedFileURL
        while candidate.path.hasPrefix(root.path + "/"), candidate != root {
            let contents = try? FileManager.default.contentsOfDirectory(atPath: candidate.path)
            guard contents?.isEmpty == true else { break }
            try? FileManager.default.removeItem(at: candidate)
            candidate.deleteLastPathComponent()
        }
    }

    private nonisolated static func isSafePathSegment(_ value: String) -> Bool {
        !value.isEmpty && value != "." && value != ".." && !value.contains("/") && !value.contains("\\")
    }
}
