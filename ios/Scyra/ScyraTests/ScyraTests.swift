import Testing
@testable import Scyra

struct AppFoundationTests {
    @Test func initialRouteIsHome() {
        let coordinator = AppLaunchCoordinator()

        #expect(coordinator.initialRoute() == .home)
    }

    @Test func appRouteSupportsEqualityAndHashing() {
        #expect(AppRoute.flow == AppRoute.flow)
        #expect(AppRoute.flow != AppRoute.story)
        #expect(Set([AppRoute.home, AppRoute.home, AppRoute.help]).count == 2)
    }

    @Test func dependencyContainerExposesInjectedLaunchCoordinator() {
        let coordinator = AppLaunchCoordinator()
        let container = AppDependencyContainer(appLaunchCoordinator: coordinator)

        #expect(container.appLaunchCoordinator.initialRoute() == .home)
    }

    @Test func rootTopBarActionOrderMatchesScyraNavigation() {
        #expect(AppRoute.rootTopBarActions == [.story, .paths, .shell, .notepad, .help])
    }

    @Test func rootTopBarActionsAreMarkedAsRootActions() {
        #expect(AppRoute.rootTopBarActions.allSatisfy { $0.display.isRootTopBarAction })
    }

    @Test func nonTopBarRoutesAreNotMarkedAsRootActions() {
        #expect(AppRoute.home.display.isRootTopBarAction == false)
        #expect(AppRoute.flow.display.isRootTopBarAction == false)
    }

    @Test func routeDisplayTitlesAreStable() {
        #expect(AppRoute.home.display.title == "Home")
        #expect(AppRoute.story.display.title == "Story")
        #expect(AppRoute.paths.display.title == "Paths")
        #expect(AppRoute.shell.display.title == "Shell")
        #expect(AppRoute.notepad.display.title == "Notepad")
        #expect(AppRoute.help.display.title == "Help")
    }
}
