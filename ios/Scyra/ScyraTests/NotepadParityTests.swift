import Testing
import UIKit
@testable import Scyra

@MainActor
struct NotepadParityTests {
    @Test func firstLaunchDocumentMatchesAndroidWelcomeCopyAndTypography() {
        let document = NotepadWelcomeDocument.make()

        #expect(document.string == NotepadWelcomeDocument.plainText)
        #expect(document.string.contains("This is your SkratchPad — a place to plan your next Flow,"))
        #expect(document.string.hasSuffix("This is your time."))

        let source = document.string as NSString
        let headingFont = document.attribute(
            .font,
            at: source.range(of: "SkratchPad").location,
            effectiveRange: nil
        ) as? UIFont
        let greetingFont = document.attribute(
            .font,
            at: source.range(of: "Hi! Welcome to Scyra!").location,
            effectiveRange: nil
        ) as? UIFont

        #expect(headingFont?.fontDescriptor.symbolicTraits.contains(.traitMonoSpace) == true)
        #expect(greetingFont?.fontName == ScyraTypography.FontName.appTitle)
    }
}
