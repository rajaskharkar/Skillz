import Foundation
import SwiftData
import Testing
import UIKit
import UniformTypeIdentifiers
@testable import Scyra

@MainActor
struct ChronicleMediaParityTests {
    @Test func swiftDataImportPersistsMetadataAndDeletionCleansTheOwnedFile() async throws {
        let fixture = try makeFixture()
        defer { fixture.cleanup() }
        let owner = ChronicleOwner.activeFlow(UUID())
        let source = try makeJPEG(width: 12, height: 8)
        defer { try? FileManager.default.removeItem(at: source) }

        let result = try await fixture.repository.importChronicleMedia(
            owner: owner,
            sources: [ChronicleMediaSource(
                url: source,
                contentTypeIdentifier: UTType.jpeg.identifier
            )]
        )

        #expect(result.importedCount == 1)
        #expect(result.failedCount == 0)
        let imported = try #require(fixture.repository.fetchChronicle(owner: owner).moments.first)
        #expect(imported.type == .media)
        let item = try #require(imported.mediaItems.first)
        #expect(item.width == 12)
        #expect(item.height == 8)
        #expect(item.mimeType == "image/jpeg")
        let ownedURL = try #require(fixture.fileStore.resolve(item.localPath))
        #expect(FileManager.default.fileExists(atPath: ownedURL.path))

        let recreated = SwiftDataFlowRepository(
            container: fixture.container,
            chronicleFileStore: fixture.fileStore
        )
        #expect(try recreated.fetchChronicle(owner: owner).mediaItems == [item])

        let remaining = try recreated.deleteChronicleMoment(owner: owner, momentID: imported.id)
        #expect(remaining.moments.isEmpty)
        #expect(!FileManager.default.fileExists(atPath: ownedURL.path))
    }

    @Test func multiImportCreatesOneOrderedMomentAndReportsIndividualFailures() async throws {
        let fixture = try makeFixture()
        defer { fixture.cleanup() }
        let valid = try makeJPEG(width: 5, height: 7)
        let corrupt = FileManager.default.temporaryDirectory
            .appending(path: "corrupt-\(UUID().uuidString).jpg")
        try Data("not an image".utf8).write(to: corrupt)
        defer {
            try? FileManager.default.removeItem(at: valid)
            try? FileManager.default.removeItem(at: corrupt)
        }

        let owner = ChronicleOwner.pulseDraft(UUID())
        let result = try await fixture.repository.importChronicleMedia(
            owner: owner,
            sources: [valid, corrupt].map {
                ChronicleMediaSource(url: $0, contentTypeIdentifier: UTType.jpeg.identifier)
            }
        )

        #expect(result.importedCount == 1)
        #expect(result.failedCount == 1)
        let snapshot = try fixture.repository.fetchChronicle(owner: owner)
        #expect(snapshot.moments.count == 1)
        #expect(snapshot.moments[0].type == .media)
        #expect(snapshot.moments[0].mediaItems.map(\.position) == [0])
    }

    @Test func pulseDeletionRemovesItsPromotedChronicleDirectory() async throws {
        let fixture = try makeFixture()
        defer { fixture.cleanup() }
        let creationKey = UUID()
        let source = try makeJPEG(width: 9, height: 9)
        defer { try? FileManager.default.removeItem(at: source) }

        _ = try await fixture.repository.importChronicleMedia(
            owner: .pulseDraft(creationKey),
            sources: [ChronicleMediaSource(url: source, contentTypeIdentifier: UTType.jpeg.identifier)]
        )
        let pulse = try fixture.repository.createPulse(
            creationKey: creationKey,
            title: "",
            journeyName: nil,
            parentFlowInstanceID: nil,
            arcID: nil,
            createdAt: .now
        )
        let item = try #require(
            fixture.repository.fetchChronicle(owner: .pulse(pulse.id)).mediaItems.first
        )
        let ownedURL = try #require(fixture.fileStore.resolve(item.localPath))

        try fixture.repository.deletePulse(id: pulse.id)

        #expect(!FileManager.default.fileExists(atPath: ownedURL.path))
        #expect(try fixture.repository.fetchPulse(id: pulse.id) == nil)
    }

    @Test func mediaEditCanStageAddReorderAndRemoveWithFileCleanup() async throws {
        let fixture = try makeFixture()
        defer { fixture.cleanup() }
        let owner = ChronicleOwner.activeFlow(UUID())
        let firstSource = try makeJPEG(width: 4, height: 4)
        let secondSource = try makeJPEG(width: 6, height: 6)
        let stagedSource = try makeJPEG(width: 8, height: 8)
        defer {
            try? FileManager.default.removeItem(at: firstSource)
            try? FileManager.default.removeItem(at: secondSource)
            try? FileManager.default.removeItem(at: stagedSource)
        }

        let imported = try await fixture.repository.importChronicleMedia(
            owner: owner,
            sources: [firstSource, secondSource].map {
                ChronicleMediaSource(url: $0, contentTypeIdentifier: UTType.jpeg.identifier)
            }
        )
        let momentID = try #require(imported.momentID)
        let originals = try fixture.repository.fetchChronicle(owner: owner).mediaItems
        let removedURL = try #require(fixture.fileStore.resolve(originals[0].localPath))
        let retainedURL = try #require(fixture.fileStore.resolve(originals[1].localPath))

        let staged = try await fixture.repository.stageChronicleMedia(
            owner: owner,
            sources: [ChronicleMediaSource(
                url: stagedSource,
                contentTypeIdentifier: UTType.jpeg.identifier
            )]
        )
        let addition = try #require(staged.items.first)
        let additionURL = try #require(fixture.fileStore.resolve(addition.localPath))

        let updated = try fixture.repository.replaceChronicleMedia(
            owner: owner,
            momentID: momentID,
            items: [addition, originals[1]]
        )

        #expect(updated.mediaItems.map(\.id) == [addition.id, originals[1].id])
        #expect(updated.mediaItems.map(\.position) == [0, 1])
        #expect(!FileManager.default.fileExists(atPath: removedURL.path))
        #expect(FileManager.default.fileExists(atPath: retainedURL.path))
        #expect(FileManager.default.fileExists(atPath: additionURL.path))

        let recreated = SwiftDataFlowRepository(
            container: fixture.container,
            chronicleFileStore: fixture.fileStore
        )
        #expect(try recreated.fetchChronicle(owner: owner).mediaItems == updated.mediaItems)
    }

    @Test func fileResolverRejectsAbsoluteAndTraversalPaths() throws {
        let store = ChronicleFileStore(baseURL: ChronicleFileStore.temporaryBaseURL())
        #expect(store.resolve("/tmp/outside.jpg") == nil)
        #expect(store.resolve("chronicle/../outside.jpg") == nil)
    }

    private func makeFixture() throws -> Fixture {
        let baseURL = ChronicleFileStore.temporaryBaseURL()
        let fileStore = ChronicleFileStore(baseURL: baseURL)
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        return Fixture(
            baseURL: baseURL,
            fileStore: fileStore,
            container: container,
            repository: SwiftDataFlowRepository(
                container: container,
                chronicleFileStore: fileStore
            )
        )
    }

    private func makeJPEG(width: Int, height: Int) throws -> URL {
        let size = CGSize(width: width, height: height)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let image = UIGraphicsImageRenderer(size: size, format: format).image { context in
            UIColor.systemTeal.setFill()
            context.fill(CGRect(origin: .zero, size: size))
        }
        let data = try #require(image.jpegData(compressionQuality: 0.95))
        let url = FileManager.default.temporaryDirectory
            .appending(path: "chronicle-test-\(UUID().uuidString).jpg")
        try data.write(to: url, options: .atomic)
        return url
    }

    private struct Fixture {
        let baseURL: URL
        let fileStore: ChronicleFileStore
        let container: SwiftData.ModelContainer
        let repository: SwiftDataFlowRepository

        func cleanup() {
            try? FileManager.default.removeItem(at: baseURL)
        }
    }
}
