import Combine
import Foundation

enum AppLanguage: String, CaseIterable, Identifiable, Sendable {
    case system
    case english = "en"
    case spanish = "es"
    case hindi = "hi"
    case marathi = "mr"

    var id: String { rawValue }

    var tag: String? {
        self == .system ? nil : rawValue
    }

    var locale: Locale {
        tag.map(Locale.init(identifier:)) ?? .autoupdatingCurrent
    }

    var displayName: String {
        switch self {
        case .system: "System default"
        case .english: "English"
        case .spanish: "Español"
        case .hindi: "हिन्दी"
        case .marathi: "मराठी"
        }
    }

    init(tag: String?) {
        self = tag.flatMap(Self.init(rawValue:)) ?? .system
    }
}

struct AppPreferenceSnapshot: Equatable, Sendable {
    var showScoreUI: Bool = true
    var calmMode: Bool = false
    var language: AppLanguage = .system
}

protocol AppPreferencesStoring: Sendable {
    func load() -> AppPreferenceSnapshot
    func save(_ snapshot: AppPreferenceSnapshot)
}

struct UserDefaultsAppPreferencesStore: AppPreferencesStoring, @unchecked Sendable {
    private enum Key {
        static let showScoreUI = "show_score_ui"
        static let calmMode = "calm_mode"
        static let languageTag = "app_language_tag"
    }

    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load() -> AppPreferenceSnapshot {
        let showScore = defaults.object(forKey: Key.showScoreUI) as? Bool ?? true
        let calm = defaults.object(forKey: Key.calmMode) as? Bool ?? false
        return AppPreferenceSnapshot(
            showScoreUI: showScore,
            calmMode: calm,
            language: AppLanguage(tag: defaults.string(forKey: Key.languageTag))
        )
    }

    func save(_ snapshot: AppPreferenceSnapshot) {
        defaults.set(snapshot.showScoreUI, forKey: Key.showScoreUI)
        defaults.set(snapshot.calmMode, forKey: Key.calmMode)
        if let tag = snapshot.language.tag {
            defaults.set(tag, forKey: Key.languageTag)
        } else {
            defaults.removeObject(forKey: Key.languageTag)
        }
    }
}

struct InMemoryAppPreferencesStore: AppPreferencesStoring, @unchecked Sendable {
    private final class Storage: @unchecked Sendable {
        let lock = NSLock()
        var snapshot: AppPreferenceSnapshot

        init(snapshot: AppPreferenceSnapshot) {
            self.snapshot = snapshot
        }
    }

    private let storage: Storage

    init(snapshot: AppPreferenceSnapshot = AppPreferenceSnapshot()) {
        storage = Storage(snapshot: snapshot)
    }

    func load() -> AppPreferenceSnapshot {
        storage.lock.withLock { storage.snapshot }
    }

    func save(_ snapshot: AppPreferenceSnapshot) {
        storage.lock.withLock { storage.snapshot = snapshot }
    }
}

@MainActor
final class AppPreferencesModel: ObservableObject {
    @Published private(set) var snapshot: AppPreferenceSnapshot

    private let store: any AppPreferencesStoring

    convenience init() {
        self.init(store: UserDefaultsAppPreferencesStore())
    }

    init(store: any AppPreferencesStoring) {
        self.store = store
        self.snapshot = store.load()
    }

    var showScoreUI: Bool { snapshot.showScoreUI }
    var calmMode: Bool { snapshot.calmMode }
    var language: AppLanguage { snapshot.language }

    func setShowScoreUI(_ enabled: Bool) {
        guard snapshot.showScoreUI != enabled else { return }
        snapshot.showScoreUI = enabled
        store.save(snapshot)
    }

    func setCalmMode(_ enabled: Bool) {
        guard snapshot.calmMode != enabled else { return }
        snapshot.calmMode = enabled
        store.save(snapshot)
    }

    func setLanguage(_ language: AppLanguage) {
        guard snapshot.language != language else { return }
        snapshot.language = language
        store.save(snapshot)
    }
}
