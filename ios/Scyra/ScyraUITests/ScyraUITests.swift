//
//  ScyraUITests.swift
//  ScyraUITests
//
//  Created by Rajas Kharkar on 6/20/26.
//

import XCTest

final class ScyraUITests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    @MainActor
    func testStoryShowsScoreAtLaunch() throws {
        let app = XCUIApplication()
        app.launch()

        let score = app.descendants(matching: .any)["story-score-display"]
        XCTAssertTrue(score.waitForExistence(timeout: 5))
        XCTAssertTrue(app.windows.firstMatch.frame.intersects(score.frame), "Score is outside the launch viewport")
        XCTAssertTrue(score.label.contains("points"), "Unexpected score label: \(score.label)")
        XCTAssertTrue(score.label.contains("This week"), "Unexpected score period: \(score.label)")
    }

    @MainActor
    func testCapturePrimaryCanonicalScreens() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.buttons["Open Story"].waitForExistence(timeout: 5))
        app.buttons["Open Story"].tap()
        settleAfterNavigation()
        keepScreenshot(app, named: "01 Story")

        app.buttons["Open Horizon"].tap()
        XCTAssertTrue(app.descendants(matching: .any)["horizon-primary-tab"].waitForExistence(timeout: 5))
        settleAfterNavigation()
        keepScreenshot(app, named: "02 Horizon")

        app.buttons["Open Notepad"].tap()
        XCTAssertTrue(app.descendants(matching: .any)["notepad-editor"].waitForExistence(timeout: 5))
        settleAfterNavigation()
        keepScreenshot(app, named: "03 Notepad")

        app.buttons["Open Help"].tap()
        XCTAssertTrue(app.switches["help-show-score-toggle"].waitForExistence(timeout: 5))
        settleAfterNavigation()
        keepScreenshot(app, named: "04 Help")

        app.buttons["Open Shell"].tap()
        XCTAssertTrue(app.descendants(matching: .any)["shell-pearl-balance"].waitForExistence(timeout: 5))
        settleAfterNavigation()
        keepScreenshot(app, named: "05 Shell")
    }

    @MainActor
    func testShellNavigatesToRealIdeaGrove() throws {
        let app = XCUIApplication()
        app.launch()

        let shell = app.buttons["Open Shell"]
        XCTAssertTrue(shell.waitForExistence(timeout: 5))
        shell.tap()

        XCTAssertTrue(app.descendants(matching: .any)["shell-pearl-balance"].waitForExistence(timeout: 5))

        let grove = app.buttons["Open Idea Grove"]
        XCTAssertTrue(grove.waitForExistence(timeout: 5))
        grove.tap()

        XCTAssertTrue(app.segmentedControls["idea-grove-tabs"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["No ideas yet"].exists || app.buttons.matching(
            NSPredicate(format: "identifier BEGINSWITH %@", "idea-grove-card-")
        ).firstMatch.exists)

        let back = app.buttons["Back to Shell"]
        XCTAssertTrue(back.exists)
        back.tap()
        XCTAssertTrue(app.buttons["Open Idea Grove"].waitForExistence(timeout: 5))
    }

    @MainActor
    private func keepScreenshot(_ app: XCUIApplication, named name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    /// The app deliberately mirrors Android's animated horizontal home pager. Waiting for the
    /// short page transition keeps visual snapshots from recording an in-between frame.
    @MainActor
    private func settleAfterNavigation() {
        Thread.sleep(forTimeInterval: 0.75)
    }

    @MainActor
    func testCaptureCanonicalFlowPulseAndShellRooms() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.buttons["Open Story"].waitForExistence(timeout: 5))
        app.buttons["Open Story"].tap()

        app.buttons["Open Flow"].tap()
        XCTAssertTrue(app.textFields["Flow title"].waitForExistence(timeout: 5))
        settleAfterNavigation()
        keepScreenshot(app, named: "06 Flow entry")
        app.buttons["Back to Story"].tap()

        app.buttons["Open Pulse"].tap()
        XCTAssertTrue(app.textFields["pulse-title-field"].waitForExistence(timeout: 5))
        settleAfterNavigation()
        keepScreenshot(app, named: "07 Pulse entry")
        app.buttons["Back to Story"].tap()

        app.buttons["Open Shell"].tap()
        XCTAssertTrue(app.descendants(matching: .any)["shell-pearl-balance"].waitForExistence(timeout: 5))

        captureShellRoom(app, button: app.buttons["Open Idea Grove"], screenID: "idea-grove-tabs", name: "08 Idea Grove")
        captureShellRoom(app, button: app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Open Chest")).firstMatch, screenID: "shell-chest-screen", name: "09 Chest")
        captureShellRoom(app, button: app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Open Achievements")).firstMatch, screenID: "shell-achievements-screen", name: "10 Badges")
        captureShellRoom(app, button: app.buttons["shell-open-stillwater"], screenID: "stillwater-screen", name: "11 Stillwater")
        captureShellRoom(app, button: app.buttons["shell-open-lookout"], screenID: "lookout-screen", name: "12 Lookout")
        captureShellRoom(app, button: app.buttons["shell-open-voyage-hall"], screenID: "voyage-hall-screen", name: "13 Voyage Hall")
        captureShellRoom(app, button: app.buttons["shell-open-focus-room"], screenID: "focus-room-list", name: "14 Focus Room")
        captureShellRoom(app, button: app.buttons["shell-open-the-blue"], screenID: "the-blue-screen", name: "15 The Blue")
    }

    @MainActor
    private func captureShellRoom(
        _ app: XCUIApplication,
        button: XCUIElement,
        screenID: String,
        name: String
    ) {
        XCTAssertTrue(button.waitForExistence(timeout: 5), "Missing Shell entrance for \(name)")
        XCTAssertTrue(button.isHittable, "Shell entrance is not hittable for \(name)")
        button.tap()
        XCTAssertTrue(app.descendants(matching: .any)[screenID].waitForExistence(timeout: 5))
        settleAfterNavigation()
        keepScreenshot(app, named: name)
        let back = app.buttons["Back to Shell"]
        XCTAssertTrue(back.waitForExistence(timeout: 5))
        back.tap()
        XCTAssertTrue(app.descendants(matching: .any)["shell-pearl-balance"].waitForExistence(timeout: 5))
        settleAfterNavigation()
    }

    @MainActor
    func testShellExposesWorkingChestAndAchievementDestinations() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.buttons["Open Shell"].waitForExistence(timeout: 5))
        app.buttons["Open Shell"].tap()

        let chest = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Open Chest")).firstMatch
        XCTAssertTrue(chest.waitForExistence(timeout: 5))
        chest.tap()
        XCTAssertTrue(app.descendants(matching: .any)["shell-chest-screen"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["The Chest is empty."].exists)
        XCTAssertTrue(app.buttons["Go to The Blue"].exists)

        XCTAssertTrue(app.buttons["Back to Shell"].exists)
        app.buttons["Back to Shell"].tap()

        let achievements = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Open Achievements")).firstMatch
        XCTAssertTrue(achievements.waitForExistence(timeout: 5))
        achievements.tap()
        XCTAssertTrue(app.descendants(matching: .any)["shell-achievements-screen"].waitForExistence(timeout: 5))
        let badgeBookTab = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Badge Book tab")
        ).firstMatch
        XCTAssertTrue(badgeBookTab.exists)
        badgeBookTab.tap()
        XCTAssertTrue(app.textFields["Search badges"].waitForExistence(timeout: 3))

        let withinReachTab = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Within Reach tab")
        ).firstMatch
        XCTAssertTrue(withinReachTab.exists)
        withinReachTab.tap()
        XCTAssertTrue(app.staticTexts["Within Reach"].waitForExistence(timeout: 3))

        let progressTab = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Progress tab")
        ).firstMatch
        XCTAssertTrue(progressTab.exists)
        progressTab.tap()
        XCTAssertTrue(app.staticTexts["Collection Progress"].waitForExistence(timeout: 3))
    }

    @MainActor
    func testCollectionSpeciesRoutesToExactBeyondBlueTarget() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.buttons["Open Shell"].waitForExistence(timeout: 5))
        app.buttons["Open Shell"].tap()
        let achievements = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Open Achievements")
        ).firstMatch
        XCTAssertTrue(achievements.waitForExistence(timeout: 5))
        achievements.tap()

        let progressTab = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Progress tab")
        ).firstMatch
        XCTAssertTrue(progressTab.waitForExistence(timeout: 5))
        progressTab.tap()

        let sunlit = app.descendants(matching: .any)["collection-card-blue_sunlit_reef"]
        for _ in 0..<8 where !sunlit.isHittable { app.swipeUp() }
        XCTAssertTrue(sunlit.waitForExistence(timeout: 5))
        XCTAssertTrue(sunlit.isHittable)
        sunlit.tap()

        XCTAssertTrue(app.descendants(matching: .any)["collection-details-blue_sunlit_reef"].waitForExistence(timeout: 5))
        let clownfish = app.descendants(matching: .any)["collection-species-creature_clownfish"]
        for _ in 0..<6 where !clownfish.isHittable { app.swipeUp() }
        XCTAssertTrue(clownfish.waitForExistence(timeout: 5))
        XCTAssertTrue(clownfish.isHittable)
        clownfish.tap()

        XCTAssertTrue(app.descendants(matching: .any)["the-blue-screen"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.descendants(matching: .any)["beyond-blue-catalog"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Clownfish"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testShellNavigatesToStillwaterWithCanonicalLockedAndDropStates() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.buttons["Open Shell"].waitForExistence(timeout: 5))
        app.buttons["Open Shell"].tap()
        let stillwater = app.buttons["shell-open-stillwater"]
        XCTAssertTrue(stillwater.waitForExistence(timeout: 5))
        stillwater.tap()

        XCTAssertTrue(app.descendants(matching: .any)["stillwater-screen"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.descendants(matching: .any)["stillwater-available-drops"].exists)
        XCTAssertTrue(app.descendants(matching: .any)["stillwater-vessel-fishbowl"].exists)
        for _ in 0..<5 where !app.descendants(matching: .any)["stillwater-vessel-aquarium"].exists { app.swipeUp() }
        XCTAssertTrue(app.descendants(matching: .any)["stillwater-vessel-aquarium"].exists)
        XCTAssertTrue(app.staticTexts["Locked"].exists)
        XCTAssertFalse(app.buttons["stillwater-draw-aquarium"].isEnabled)
        XCTAssertTrue(app.buttons["Back to Shell"].exists)
    }

    @MainActor
    func testShellNavigatesToTheBlueCatalog() throws {
        let app = XCUIApplication()
        app.launch()

        app.buttons["Open Shell"].tap()
        let blue = app.buttons["shell-open-the-blue"]
        XCTAssertTrue(blue.waitForExistence(timeout: 5))
        blue.tap()
        XCTAssertTrue(app.descendants(matching: .any)["the-blue-screen"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testShellNavigatesToLookoutAndPresentsObjectiveEditor() throws {
        let app = XCUIApplication()
        app.launch()

        app.buttons["Open Shell"].tap()
        let lookout = app.buttons["shell-open-lookout"]
        for _ in 0..<5 where !lookout.isHittable { app.swipeUp() }
        XCTAssertTrue(lookout.waitForExistence(timeout: 5))
        XCTAssertTrue(lookout.isHittable)
        lookout.tap()

        XCTAssertTrue(app.descendants(matching: .any)["lookout-screen"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.segmentedControls["lookout-period-picker"].exists)
        XCTAssertTrue(app.staticTexts["No daily objectives"].exists)
        let set = app.buttons["lookout-set-objective"]
        XCTAssertTrue(set.exists)
        set.tap()
        XCTAssertTrue(app.descendants(matching: .any)["lookout-editor"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.textFields["lookout-editor-journey"].exists)
        XCTAssertTrue(app.staticTexts.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Only completed regular Flows in this Journey count.")
        ).firstMatch.exists)
    }

    @MainActor
    func testShellNavigatesToVoyageHallEmptyState() throws {
        let app = XCUIApplication()
        app.launch()

        app.buttons["Open Shell"].tap()
        let voyage = app.buttons["shell-open-voyage-hall"]
        for _ in 0..<7 where !voyage.isHittable { app.swipeUp() }
        XCTAssertTrue(voyage.waitForExistence(timeout: 5))
        XCTAssertTrue(voyage.isHittable)
        voyage.tap()

        XCTAssertTrue(app.descendants(matching: .any)["voyage-hall-screen"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Voyage Hall"].exists)
        XCTAssertTrue(app.staticTexts["No records yet"].exists)
    }

    @MainActor
    func testShellNavigatesToFocusRoomAndStartsGuidedExercise() throws {
        let app = XCUIApplication()
        app.launch()

        app.buttons["Open Shell"].tap()
        let focus = app.buttons["shell-open-focus-room"]
        for _ in 0..<9 where !focus.isHittable { app.swipeUp() }
        XCTAssertTrue(focus.waitForExistence(timeout: 5))
        XCTAssertTrue(focus.isHittable)
        focus.tap()

        XCTAssertTrue(app.descendants(matching: .any)["focus-room-list"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Three-Point Grounding"].exists)
        let select = app.buttons["focus-start-threePointGrounding"]
        XCTAssertTrue(select.exists)
        for _ in 0..<4 where !select.isHittable { app.swipeUp() }
        XCTAssertTrue(select.isHittable)
        select.tap()
        XCTAssertTrue(app.descendants(matching: .any)["focus-player"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["focus-begin"].exists)
        XCTAssertTrue(app.switches["focus-voice-toggle"].exists)
        XCTAssertTrue(app.staticTexts["Ready when you are"].exists)
    }

    @MainActor
    func testFlowEntryShowsCanonicalControls() throws {
        let app = XCUIApplication()
        app.launch()

        let openFlow = app.buttons["Open Flow"]
        XCTAssertTrue(openFlow.waitForExistence(timeout: 5))
        openFlow.tap()

        XCTAssertTrue(app.textFields["Flow title"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.textFields["Journey name"].exists)
        XCTAssertTrue(app.buttons["Enter Flow"].exists)
        XCTAssertTrue(app.buttons["Continue Arc"].exists)

        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "Flow entry"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }

    @MainActor
    func testCompletedFlowAppearsInStory() throws {
        let app = XCUIApplication()
        addUIInterruptionMonitor(withDescription: "Notification authorization") { alert in
            if alert.buttons["Allow"].exists {
                alert.buttons["Allow"].tap()
                return true
            }
            return false
        }
        app.launch()

        let title = "UI parity Flow"
        app.buttons["Open Flow"].tap()

        let titleField = app.textFields["Flow title"]
        XCTAssertTrue(titleField.waitForExistence(timeout: 5))
        titleField.tap()
        titleField.typeText(title + "\n")

        let journeyField = app.textFields["Journey name"]
        journeyField.tap()
        journeyField.typeText("Scyra\n")

        let enterFlow = app.buttons["Enter Flow"]
        XCTAssertTrue(enterFlow.waitForExistence(timeout: 5))
        enterFlow.tap()
        app.tap()
        sleep(2)

        let exitFlow = app.buttons["Exit Flow"]
        XCTAssertTrue(exitFlow.waitForExistence(timeout: 5))
        exitFlow.tap()

        let completeFlow = app.buttons["Complete Flow"]
        for _ in 0..<3 where !completeFlow.isHittable { app.swipeUp() }
        XCTAssertTrue(completeFlow.waitForExistence(timeout: 5))
        completeFlow.tap()

        let done = app.buttons["Done"]
        XCTAssertTrue(done.waitForExistence(timeout: 5))
        done.tap()

        let storyEntry = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@", title)
        ).firstMatch
        XCTAssertTrue(storyEntry.waitForExistence(timeout: 5))
    }

    @MainActor
    func testHistoricalPulseNavigationReturnsToItsFlowContext() throws {
        let app = XCUIApplication()
        addUIInterruptionMonitor(withDescription: "Notification authorization") { alert in
            if alert.buttons["Allow"].exists {
                alert.buttons["Allow"].tap()
                return true
            }
            return false
        }
        app.launch()

        let flowTitle = "Nested Flow \(UUID().uuidString.prefix(6))"
        let pulseTitle = "Nested Pulse \(UUID().uuidString.prefix(6))"
        app.buttons["Open Flow"].tap()
        let titleField = app.textFields["Flow title"]
        XCTAssertTrue(titleField.waitForExistence(timeout: 5))
        titleField.tap()
        titleField.typeText(flowTitle + "\n")
        let journeyField = app.textFields["Journey name"]
        journeyField.tap()
        journeyField.typeText("Navigation\n")
        app.buttons["Enter Flow"].tap()
        app.tap()
        sleep(2)
        app.buttons["Exit Flow"].tap()

        let completeFlow = app.buttons["Complete Flow"]
        for _ in 0..<3 where !completeFlow.isHittable { app.swipeUp() }
        XCTAssertTrue(completeFlow.isHittable)
        completeFlow.tap()
        XCTAssertTrue(app.buttons["Done"].waitForExistence(timeout: 5))
        app.buttons["Done"].tap()

        let flowCard = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@", flowTitle)
        ).firstMatch
        XCTAssertTrue(flowCard.waitForExistence(timeout: 5))
        flowCard.tap()
        XCTAssertTrue(app.staticTexts["Flow details"].waitForExistence(timeout: 5))

        let addPulse = app.buttons["flow-details-toggle-pulse-composer"]
        for _ in 0..<3 where !addPulse.isHittable { app.swipeUp() }
        XCTAssertTrue(addPulse.isHittable)
        addPulse.tap()
        app.textFields["Pulse title"].tap()
        app.textFields["Pulse title"].typeText(pulseTitle + "\n")
        app.textFields["Pulse description"].tap()
        app.textFields["Pulse description"].typeText("A historical moment\n")
        let savePulse = app.buttons["Save Pulse"]
        for _ in 0..<3 where !savePulse.isHittable { app.swipeUp() }
        XCTAssertTrue(savePulse.isHittable)
        savePulse.tap()

        let details = app.buttons["Details"]
        for _ in 0..<3 where !details.isHittable { app.swipeUp() }
        XCTAssertTrue(details.waitForExistence(timeout: 5))
        details.tap()
        XCTAssertTrue(app.staticTexts["Pulse details"].waitForExistence(timeout: 5))

        app.buttons["Edit Pulse"].tap()
        XCTAssertTrue(app.staticTexts["Edit Pulse"].waitForExistence(timeout: 5))
        app.buttons["Cancel"].tap()
        XCTAssertTrue(app.staticTexts["Pulse details"].waitForExistence(timeout: 5))

        app.buttons["Back to Flow details"].tap()
        XCTAssertTrue(app.staticTexts["Flow details"].waitForExistence(timeout: 5))
        app.buttons["Back to Story"].tap()
        XCTAssertTrue(flowCard.waitForExistence(timeout: 5))
    }

    @MainActor
    func testSoftFlowRewardCanEnterTheShell() throws {
        let app = XCUIApplication()
        addUIInterruptionMonitor(withDescription: "Notification authorization") { alert in
            if alert.buttons["Allow"].exists {
                alert.buttons["Allow"].tap()
                return true
            }
            return false
        }
        app.launch()
        app.buttons["Open Flow"].tap()

        let titleField = app.textFields["Flow title"]
        XCTAssertTrue(titleField.waitForExistence(timeout: 5))
        titleField.tap()
        titleField.typeText("Quiet reset\n")
        let journeyField = app.textFields["Journey name"]
        journeyField.tap()
        journeyField.typeText("Recovery\n")

        let softMode = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Soft")
        ).firstMatch
        XCTAssertTrue(softMode.waitForExistence(timeout: 5))
        softMode.tap()
        app.buttons["Begin Soft Flow"].tap()
        app.tap()
        sleep(2)
        app.buttons["Exit Soft Flow"].tap()

        let save = app.buttons["Save Soft Flow"]
        for _ in 0..<3 where !save.isHittable { app.swipeUp() }
        XCTAssertTrue(save.waitForExistence(timeout: 5))
        XCTAssertTrue(save.isHittable)
        save.tap()

        let enterShell = app.buttons["reward-enter-shell"]
        XCTAssertTrue(enterShell.waitForExistence(timeout: 5))
        enterShell.tap()
        XCTAssertTrue(app.descendants(matching: .any)["shell-pearl-balance"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["shell-open-stillwater"].exists)
    }

    @MainActor
    func testCompletedArcRevealsFlowRewardBeforeArcSummary() throws {
        let app = XCUIApplication()
        addUIInterruptionMonitor(withDescription: "Notification authorization") { alert in
            if alert.buttons["Allow"].exists {
                alert.buttons["Allow"].tap()
                return true
            }
            return false
        }
        app.launch()
        app.buttons["Open Flow"].tap()

        let titleField = app.textFields["Flow title"]
        XCTAssertTrue(titleField.waitForExistence(timeout: 5))
        titleField.tap()
        titleField.typeText("Arc opening \(UUID().uuidString.prefix(6))\n")
        let journeyField = app.textFields["Journey name"]
        journeyField.tap()
        journeyField.typeText("Two-stage Arc\n")

        let continueArc = app.buttons["Continue Arc"]
        for _ in 0..<3 where !continueArc.isHittable { app.swipeUp() }
        XCTAssertTrue(continueArc.waitForExistence(timeout: 5))
        continueArc.tap()
        XCTAssertTrue(app.buttons["reward-done"].waitForExistence(timeout: 5))
        app.buttons["reward-done"].tap()

        XCTAssertTrue(titleField.waitForExistence(timeout: 5))
        titleField.tap()
        titleField.typeText("Arc closing \(UUID().uuidString.prefix(6))\n")

        let softMode = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Soft")
        ).firstMatch
        XCTAssertTrue(softMode.waitForExistence(timeout: 5))
        softMode.tap()
        let enterSoft = app.buttons["Enter soft"]
        if enterSoft.waitForExistence(timeout: 1) { enterSoft.tap() }

        app.buttons["Begin Soft Flow"].tap()
        app.tap()
        sleep(2)
        app.buttons["Exit Soft Flow"].tap()

        let completeArc = app.buttons["Complete Arc"]
        for _ in 0..<3 where !completeArc.isHittable { app.swipeUp() }
        XCTAssertTrue(completeArc.waitForExistence(timeout: 5))
        completeArc.tap()

        XCTAssertTrue(app.descendants(matching: .any)["reward-card-stillwaterResult"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.descendants(matching: .any)["reward-stage-title"].label, "Soft Flow recorded")
        keepScreenshot(app, named: "Completed Arc - Flow reward stage")
        let next = app.buttons["reward-next-arc"]
        XCTAssertTrue(next.exists)
        next.tap()
        XCTAssertTrue(app.descendants(matching: .any)["reward-card-arcScore"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.descendants(matching: .any)["reward-stage-title"].label, "Arc Reward")
        XCTAssertTrue(app.buttons["reward-done"].exists)
        keepScreenshot(app, named: "Completed Arc - Arc reward stage")
    }

    @MainActor
    func testPulseWithChronicleAppearsInStory() throws {
        let app = XCUIApplication()
        app.launch()

        let title = "UI parity Pulse \(UUID().uuidString.prefix(6))"
        let moment = "A durable Chronicle moment"

        let openPulse = app.buttons["Open Pulse"]
        XCTAssertTrue(openPulse.waitForExistence(timeout: 5))
        openPulse.tap()

        let titleField = app.textFields["pulse-title-field"]
        XCTAssertTrue(titleField.waitForExistence(timeout: 5))
        titleField.tap()
        titleField.typeText(title)

        let chronicleTab = app.buttons["Chronicle"]
        XCTAssertTrue(chronicleTab.waitForExistence(timeout: 5))
        chronicleTab.tap()

        let draftField = app.textViews["chronicle-draft-field"]
        XCTAssertTrue(draftField.waitForExistence(timeout: 5))
        draftField.tap()
        draftField.typeText(moment)

        let addMoment = app.buttons["chronicle-add-moment"]
        for _ in 0..<3 where !addMoment.isHittable { app.swipeUp() }
        XCTAssertTrue(addMoment.isHittable)
        addMoment.tap()

        let pulseTab = app.buttons["Pulse"]
        XCTAssertTrue(pulseTab.waitForExistence(timeout: 5))
        pulseTab.tap()

        let savePulse = app.buttons["save-pulse"]
        for _ in 0..<3 where !savePulse.isHittable { app.swipeUp() }
        XCTAssertTrue(savePulse.isHittable)
        savePulse.tap()

        let storyEntry = app.descendants(matching: .any).matching(
            NSPredicate(
                format: "identifier BEGINSWITH %@ AND label BEGINSWITH %@",
                "pulse-card-",
                title
            )
        ).firstMatch
        XCTAssertTrue(storyEntry.waitForExistence(timeout: 5))
        XCTAssertTrue(
            storyEntry.label.contains(moment),
            "Story Pulse card did not expose Chronicle text. Label: \(storyEntry.label)"
        )
    }

    @MainActor
    func testChronicleOffersAudioControls() throws {
        let app = XCUIApplication()
        app.launch()

        let openPulse = app.buttons["Open Pulse"]
        XCTAssertTrue(openPulse.waitForExistence(timeout: 5))
        openPulse.tap()

        let chronicleTab = app.buttons["Chronicle"]
        XCTAssertTrue(chronicleTab.waitForExistence(timeout: 5))
        chronicleTab.tap()

        XCTAssertTrue(app.textViews["chronicle-draft-field"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Start dictation"].exists)
        XCTAssertTrue(app.buttons["Start voice recording"].exists)
    }

    @MainActor
    func testHelpExposesMovementBonusWithoutRequestingHealthAtLaunch() throws {
        let app = XCUIApplication()
        app.launch()

        let help = app.buttons["Open Help"]
        XCTAssertTrue(help.waitForExistence(timeout: 5))
        help.tap()

        XCTAssertTrue(app.staticTexts["Movement Bonus"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.alerts.firstMatch.exists)
    }

    @MainActor
    func testHelpScorePreferencePropagatesBackToStory() throws {
        let app = XCUIApplication()
        app.launch()

        app.buttons["Open Help"].tap()
        let toggle = app.switches["help-show-score-toggle"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 5))
        if switchIsOn(toggle) { toggle.tap() }

        app.buttons["Open Story"].tap()
        XCTAssertFalse(app.descendants(matching: .any)["story-score-display"].waitForExistence(timeout: 1))

        app.buttons["Open Help"].tap()
        let restoreToggle = app.switches["help-show-score-toggle"]
        XCTAssertTrue(restoreToggle.waitForExistence(timeout: 5))
        if !switchIsOn(restoreToggle) { restoreToggle.tap() }
    }

    @MainActor
    func testHorizonCreatesAndLaunchesSurgePlanIntoFlow() throws {
        let app = XCUIApplication()
        app.launch()

        let title = "Horizon UI \(UUID().uuidString.prefix(6))"
        let openHorizon = app.buttons["Open Horizon"]
        XCTAssertTrue(openHorizon.waitForExistence(timeout: 5))
        openHorizon.tap()

        let planFlow = app.buttons["Plan Flow"]
        XCTAssertTrue(planFlow.waitForExistence(timeout: 5))
        planFlow.tap()

        let titleField = app.textFields["flow-plan-title"]
        XCTAssertTrue(titleField.waitForExistence(timeout: 5))
        titleField.tap()
        titleField.typeText(title)

        let journeyField = app.textFields["flow-plan-journey"]
        journeyField.tap()
        journeyField.typeText("Craft")

        let surge = app.switches["flow-plan-surge"]
        XCTAssertTrue(surge.exists)
        surge.tap()
        let minutes = app.textFields["flow-plan-minutes"]
        XCTAssertTrue(minutes.waitForExistence(timeout: 5))
        minutes.tap()
        minutes.typeText("25")

        app.buttons["flow-plan-save"].tap()
        let planTitle = app.staticTexts[title]
        XCTAssertTrue(planTitle.waitForExistence(timeout: 5))
        planTitle.tap()

        let flowTitle = app.textFields["Flow title"]
        XCTAssertTrue(flowTitle.waitForExistence(timeout: 5))
        XCTAssertEqual(flowTitle.value as? String, title)
        XCTAssertEqual(app.textFields["Journey name"].value as? String, "Craft")
        XCTAssertTrue(app.staticTexts["Planned: 25 min"].exists)

        app.buttons["Back to Story"].tap()
    }

    @MainActor
    func testHorizonExposesArcStudioAndCanonicalSuggestedScenes() throws {
        let app = XCUIApplication()
        app.launch()

        app.buttons["Open Horizon"].tap()
        let tabs = app.descendants(matching: .any)["horizon-primary-tab"]
        XCTAssertTrue(tabs.waitForExistence(timeout: 5))
        tabs.buttons["Arcs"].tap()

        XCTAssertTrue(app.buttons["horizon-create-arc"].waitForExistence(timeout: 5))
        let suggestion = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Suggested sequence. Deep Work Launch")
        ).firstMatch
        for _ in 0..<4 where !suggestion.isHittable { app.swipeUp() }
        XCTAssertTrue(suggestion.waitForExistence(timeout: 5))
        suggestion.tap()

        XCTAssertTrue(app.buttons["Save as Arc"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Begin Arc"].exists)
        XCTAssertTrue(app.staticTexts["Clear the Desk"].exists)
    }

    @MainActor
    func testHorizonArcDetailTransitionsReliablyIntoEditor() throws {
        let app = XCUIApplication()
        app.launch()

        app.buttons["Open Horizon"].tap()
        let tabs = app.descendants(matching: .any)["horizon-primary-tab"]
        XCTAssertTrue(tabs.waitForExistence(timeout: 5))
        tabs.buttons["Arcs"].tap()

        let suggestion = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Suggested sequence. Deep Work Launch")
        ).firstMatch
        for _ in 0..<5 where !suggestion.isHittable { app.swipeUp() }
        XCTAssertTrue(suggestion.waitForExistence(timeout: 5))
        suggestion.tap()
        XCTAssertTrue(app.buttons["Save as Arc"].waitForExistence(timeout: 5))
        app.buttons["Save as Arc"].tap()

        let openPlan = app.buttons.matching(
            NSPredicate(format: "identifier BEGINSWITH %@", "arc-plan-open-")
        ).firstMatch
        for _ in 0..<5 where !openPlan.isHittable { app.swipeDown() }
        XCTAssertTrue(openPlan.waitForExistence(timeout: 5))
        XCTAssertTrue(openPlan.isHittable)
        openPlan.tap()

        XCTAssertTrue(app.descendants(matching: .any)["arc-plan-detail"].waitForExistence(timeout: 5))
        app.buttons["Edit"].tap()
        XCTAssertTrue(app.descendants(matching: .any)["arc-plan-editor"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.textFields["arc-plan-title"].exists)
    }

    private func switchIsOn(_ element: XCUIElement) -> Bool {
        if let number = element.value as? NSNumber { return number.boolValue }
        guard let value = element.value as? String else { return false }
        return ["1", "on", "true", "selected"].contains(value.lowercased())
    }

    @MainActor
    func testLaunchPerformance() throws {
        // This measures how long it takes to launch your application.
        measure(metrics: [XCTApplicationLaunchMetric()]) {
            XCUIApplication().launch()
        }
    }
}
