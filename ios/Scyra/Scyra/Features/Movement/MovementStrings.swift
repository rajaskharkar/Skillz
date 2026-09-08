import Foundation

enum MovementStrings {
    static let title = String(localized: "Movement Bonus")
    static let unavailableHeadline = String(localized: "Apple Health is unavailable.")
    static let healthUnavailable = String(localized: "Movement Bonus needs Apple Health to read steps during your Flows.")
    static let enableHeadline = String(localized: "Enable Health to earn Movement Points during your Flows.")
    static let enableBody = String(localized: "Scyra reads your step count through Apple Health and gives you +1 Movement Point for every 100 steps during eligible Flows. Movement Points are added to your total Scyra Points.")
    static let activeHeadline = String(localized: "Movement Bonus is active.")
    static let activeBody = String(localized: "Steps taken during eligible Flows can earn Movement Points. Movement Points are added to your Scyra Points and can also generate Pearls.")
    static let offHeadline = String(localized: "Movement Bonus is off.")
    static let offBody = String(localized: "Turn it on to earn Movement Points from steps during eligible Flows.")
    static let privacy = String(localized: "If you enable Movement Bonus, Scyra may read step count data from Apple Health during eligible Flows. Scyra uses this only to calculate Movement Points, Scyra Points, Pearls, and related reward displays. Scyra does not sell this data or use it for advertising.")
    static let connect = String(localized: "Connect Apple Health")
    static let permissionNotGranted = String(localized: "Health access was not granted. You can review Scyra’s access in the Health app.")
    static let persistenceFailure = String(localized: "Movement Bonus could not be updated. Try again.")
    static let activePill = String(localized: "Movement Bonus active · Every 100 steps earns +1 point")
    static let activePillAccessibility = String(localized: "Movement Bonus active. Every 100 steps earns 1 Movement Point.")
    static let updatedAfterSync = String(localized: "Movement Points added after sync.")
    static let disableTitle = String(localized: "Disable Health?")
    static let disableBody = String(localized: "Some recent Flows may still be waiting for step data or improved Apple Health sync.\n\nIf you disable Health now, Scyra will stop checking those Flows and any pending Movement Points may not be awarded.\n\nAlready-awarded Movement Points will stay.\n\nYou can turn Health back on later.")
    static let keepOn = String(localized: "Keep Health On")
    static let disableAnyway = String(localized: "Disable Anyway")

    static func steps(_ value: Int64) -> String {
        value == 1 ? String(localized: "1 step") : String(localized: "\(value) steps")
    }

    static func points(_ value: Int64) -> String {
        value == 1 ? String(localized: "1 Movement Point") : String(localized: "\(value) Movement Points")
    }
}
