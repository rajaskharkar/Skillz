import Foundation

enum AppLocalization {
    static func string(_ key: String, language: AppLanguage, fallback: String) -> String {
        guard let tag = language.tag,
              let path = Bundle.main.path(forResource: tag, ofType: "lproj"),
              let bundle = Bundle(path: path) else {
            return Bundle.main.localizedString(forKey: key, value: fallback, table: nil)
        }
        return bundle.localizedString(forKey: key, value: fallback, table: nil)
    }
}
