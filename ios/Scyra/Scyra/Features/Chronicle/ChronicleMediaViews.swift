import AVFoundation
import AVKit
import CoreTransferable
import ImageIO
import Photos
import PhotosUI
import SwiftUI
import UniformTypeIdentifiers
import UIKit

private struct ChronicleFileStoreEnvironmentKey: EnvironmentKey {
    static let defaultValue = ChronicleFileStore()
}

extension EnvironmentValues {
    var chronicleFileStore: ChronicleFileStore {
        get { self[ChronicleFileStoreEnvironmentKey.self] }
        set { self[ChronicleFileStoreEnvironmentKey.self] = newValue }
    }
}

struct ChronicleMediaTransfer: Transferable, Sendable {
    let url: URL
    let contentTypeIdentifier: String

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(importedContentType: .image) { received in
            Self(
                url: try ChronicleIncomingMedia.copy(received.file, contentType: .image),
                contentTypeIdentifier: UTType.image.identifier
            )
        }
        FileRepresentation(importedContentType: .movie) { received in
            Self(
                url: try ChronicleIncomingMedia.copy(received.file, contentType: .movie),
                contentTypeIdentifier: UTType.movie.identifier
            )
        }
    }
}

enum ChronicleCameraMode: String, Identifiable {
    case photo
    case video

    var id: String { rawValue }
}

struct ChronicleCameraCaptureView: UIViewControllerRepresentable {
    let mode: ChronicleCameraMode
    let onCapture: (ChronicleMediaSource) -> Void
    let onCancel: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onCapture: onCapture, onCancel: onCancel)
    }

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        picker.sourceType = .camera
        picker.mediaTypes = [mode == .photo ? UTType.image.identifier : UTType.movie.identifier]
        picker.cameraCaptureMode = mode == .photo ? .photo : .video
        picker.videoQuality = .typeHigh
        picker.allowsEditing = false
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    final class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        private let onCapture: (ChronicleMediaSource) -> Void
        private let onCancel: () -> Void

        init(
            onCapture: @escaping (ChronicleMediaSource) -> Void,
            onCancel: @escaping () -> Void
        ) {
            self.onCapture = onCapture
            self.onCancel = onCancel
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            onCancel()
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            do {
                if let image = info[.originalImage] as? UIImage {
                    let url = try ChronicleIncomingMedia.writeJPEG(image)
                    finishCapture(ChronicleMediaSource(
                        url: url,
                        contentTypeIdentifier: UTType.jpeg.identifier,
                        deleteWhenFinished: true
                    ))
                } else if let url = info[.mediaURL] as? URL {
                    let copy = try ChronicleIncomingMedia.copy(url, contentType: .movie)
                    finishCapture(ChronicleMediaSource(
                        url: copy,
                        contentTypeIdentifier: UTType.movie.identifier,
                        deleteWhenFinished: true
                    ))
                } else {
                    onCancel()
                }
            } catch {
                onCancel()
            }
        }

        private func finishCapture(_ source: ChronicleMediaSource) {
            Task {
                await ChroniclePhotoLibraryPublisher.publish(source)
                onCapture(source)
            }
        }
    }
}

private enum ChroniclePhotoLibraryPublisher {
    static func publish(_ source: ChronicleMediaSource) async {
        let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        guard status == .authorized || status == .limited else { return }
        try? await PHPhotoLibrary.shared().performChanges {
            guard let type = UTType(source.contentTypeIdentifier) else { return }
            if type.conforms(to: .image) {
                PHAssetChangeRequest.creationRequestForAssetFromImage(atFileURL: source.url)
            } else if type.conforms(to: .movie) {
                PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: source.url)
            }
        }
    }
}

struct ChronicleMediaMomentView: View {
    let items: [ChronicleMediaItem]
    var compact = false
    var allowsOpening = true

    @State private var selection = 0
    @State private var viewerItem: ChronicleMediaItem?

    var body: some View {
        if items.isEmpty {
            ChronicleMediaUnavailableView()
        } else if compact {
            compactBody
        } else {
            galleryBody
        }
    }

    private var compactBody: some View {
        Group {
            if allowsOpening {
                Button {
                    viewerItem = items.first
                } label: {
                    compactLabel
                }
                .buttonStyle(.plain)
            } else {
                compactLabel
            }
        }
        .fullScreenCover(item: $viewerItem) { item in
            ChronicleMediaViewer(items: items, initialItemID: item.id)
        }
    }

    private var compactLabel: some View {
        HStack(spacing: ScyraSpacing.sm) {
            ChronicleMediaThumbnail(item: items[0])
                .frame(width: 76, height: 62)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            VStack(alignment: .leading, spacing: 3) {
                Text(items[0].mimeType.hasPrefix("video/") ? ChronicleStrings.video : ChronicleStrings.photo)
                    .font(ScyraTypography.label)
                if items.count > 1 {
                    Text("\(items.count) items")
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textSecondary)
                }
            }
            Spacer()
            if allowsOpening { ScyraCanonicalIcon(systemName: "arrow.up.left.and.arrow.down.right") }
        }
        .contentShape(Rectangle())
    }

    private var galleryBody: some View {
        VStack(spacing: ScyraSpacing.sm) {
            TabView(selection: $selection) {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    Button {
                        viewerItem = item
                    } label: {
                        ZStack {
                            ChronicleMediaThumbnail(item: item)
                            if item.mimeType.hasPrefix("video/") {
                                ScyraCanonicalIcon(systemName: "play.circle.fill")
                                    .font(.system(size: 54))
                                    .foregroundStyle(.white)
                                    .shadow(radius: 6)
                            }
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color.black.opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .tag(index)
                    .accessibilityLabel(mediaAccessibilityLabel(item, index: index))
                }
            }
            .tabViewStyle(.page(indexDisplayMode: items.count > 1 ? .automatic : .never))
            .frame(height: 235)

            if items.count > 1 {
                Text("\(selection + 1) of \(items.count)")
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
            }
        }
        .fullScreenCover(item: $viewerItem) { item in
            ChronicleMediaViewer(items: items, initialItemID: item.id)
        }
    }

    private func mediaAccessibilityLabel(_ item: ChronicleMediaItem, index: Int) -> String {
        let kind = item.mimeType.hasPrefix("video/") ? ChronicleStrings.video : ChronicleStrings.photo
        return "\(kind), \(index + 1) of \(items.count), \(ChronicleStrings.openMedia)"
    }
}

struct ChronicleMediaThumbnail: View {
    let item: ChronicleMediaItem

    @Environment(\.chronicleFileStore) private var fileStore
    @State private var image: UIImage?
    @State private var finishedLoading = false

    var body: some View {
        ZStack {
            Color.black.opacity(0.07)
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else if finishedLoading {
                ChronicleMediaUnavailableView()
            } else {
                ProgressView()
            }
        }
        .clipped()
        .task(id: item.localPath) {
            guard let url = fileStore.resolve(item.localPath) else {
                finishedLoading = true
                return
            }
            image = await ChronicleThumbnailLoader.shared.load(
                url: url,
                mimeType: item.mimeType,
                maximumPixelSize: 1_200
            )
            finishedLoading = true
        }
    }
}

private struct ChronicleMediaViewer: View {
    let items: [ChronicleMediaItem]
    let initialItemID: UUID

    @Environment(\.dismiss) private var dismiss
    @State private var selection: Int

    init(items: [ChronicleMediaItem], initialItemID: UUID) {
        self.items = items
        self.initialItemID = initialItemID
        _selection = State(initialValue: items.firstIndex { $0.id == initialItemID } ?? 0)
    }

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            TabView(selection: $selection) {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    ChronicleMediaViewerPage(item: item, active: selection == index)
                        .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: items.count > 1 ? .automatic : .never))

            Button(action: dismiss.callAsFunction) {
                ScyraCanonicalIcon(systemName: "xmark")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
                    .background(.black.opacity(0.55), in: Circle())
            }
            .padding()
            .accessibilityLabel(ChronicleStrings.closeMedia)
        }
    }
}

private struct ChronicleMediaViewerPage: View {
    let item: ChronicleMediaItem
    let active: Bool

    @Environment(\.chronicleFileStore) private var fileStore
    @State private var image: UIImage?
    @State private var player: AVPlayer?
    @State private var unavailable = false

    var body: some View {
        Group {
            if item.mimeType.hasPrefix("video/") {
                if let player {
                    VideoPlayer(player: player)
                } else if unavailable {
                    ChronicleMediaUnavailableView(foreground: .white)
                } else {
                    ProgressView().tint(.white)
                }
            } else if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
            } else if unavailable {
                ChronicleMediaUnavailableView(foreground: .white)
            } else {
                ProgressView().tint(.white)
            }
        }
        .padding(.vertical, 48)
        .task(id: item.localPath) { prepare() }
        .onChange(of: active) { _, isActive in
            if !isActive { player?.pause() }
        }
        .onDisappear { player?.pause() }
    }

    private func prepare() {
        guard let url = fileStore.resolve(item.localPath),
              FileManager.default.fileExists(atPath: url.path) else {
            unavailable = true
            return
        }
        if item.mimeType.hasPrefix("video/") {
            player = AVPlayer(url: url)
        } else {
            image = UIImage(contentsOfFile: url.path)
            unavailable = image == nil
        }
    }
}

private struct ChronicleMediaUnavailableView: View {
    var foreground: Color = ScyraColors.textSecondary

    var body: some View {
        ScyraCanonicalLabel(ChronicleStrings.mediaUnavailable, systemImage: "photo.badge.exclamationmark")
            .font(ScyraTypography.caption)
            .foregroundStyle(foreground)
            .padding()
    }
}

private actor ChronicleThumbnailLoader {
    static let shared = ChronicleThumbnailLoader()

    private var cache: [String: UIImage] = [:]
    private var order: [String] = []

    func load(url: URL, mimeType: String, maximumPixelSize: Int) async -> UIImage? {
        let values = try? url.resourceValues(forKeys: [.contentModificationDateKey, .fileSizeKey])
        let key = "\(url.path)|\(values?.contentModificationDate?.timeIntervalSince1970 ?? 0)|\(values?.fileSize ?? 0)|\(maximumPixelSize)"
        if let cached = cache[key] { return cached }

        let image: UIImage?
        if mimeType.hasPrefix("video/") {
            let generator = AVAssetImageGenerator(asset: AVURLAsset(url: url))
            generator.appliesPreferredTrackTransform = true
            generator.maximumSize = CGSize(width: maximumPixelSize, height: maximumPixelSize)
            image = try? await UIImage(cgImage: generator.image(at: .zero).image)
        } else if let source = CGImageSourceCreateWithURL(url as CFURL, nil),
                  let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                    kCGImageSourceCreateThumbnailFromImageAlways: true,
                    kCGImageSourceCreateThumbnailWithTransform: true,
                    kCGImageSourceThumbnailMaxPixelSize: maximumPixelSize
                  ] as CFDictionary) {
            image = UIImage(cgImage: thumbnail)
        } else {
            image = nil
        }

        if let image {
            cache[key] = image
            order.append(key)
            while order.count > 40, let oldest = order.first {
                order.removeFirst()
                cache.removeValue(forKey: oldest)
            }
        }
        return image
    }
}

private enum ChronicleIncomingMedia {
    nonisolated static func copy(_ source: URL, contentType: UTType) throws -> URL {
        let directory = FileManager.default.temporaryDirectory
            .appending(path: "ScyraChronicleIncoming", directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let pathExtension = contentType.preferredFilenameExtension
            ?? source.pathExtension
        let destination = directory.appending(path: "\(UUID().uuidString).\(pathExtension)")
        try FileManager.default.copyItem(at: source, to: destination)
        return destination
    }

    nonisolated static func writeJPEG(_ image: UIImage) throws -> URL {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let normalized = UIGraphicsImageRenderer(size: image.size, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: image.size))
        }
        guard let data = normalized.jpegData(compressionQuality: 0.94) else {
            throw ChronicleFileStoreError.corruptMedia
        }
        let directory = FileManager.default.temporaryDirectory
            .appending(path: "ScyraChronicleIncoming", directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let destination = directory.appending(path: "\(UUID().uuidString).jpg")
        try data.write(to: destination, options: .atomic)
        return destination
    }
}
