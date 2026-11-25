package com.kingkharnivore.skillz.ui.screen.paths

import android.Manifest
import android.os.Build
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.domain.reminders.ActivityReminder
import com.kingkharnivore.skillz.model.state.paths.PathsPrimaryTab
import com.kingkharnivore.skillz.model.ui.FlowPlanListItemUiModel
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ActivityReminderUiTest {
    @get:Rule val compose = createComposeRule()
    @Before fun permissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            val i = InstrumentationRegistry.getInstrumentation()
            i.uiAutomation.grantRuntimePermission(i.targetContext.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    private fun SemanticsNodeInteraction.activate() = performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() }

    @Test fun createHabitSupportsMultipleMonthlyDatesAndSaves() {
        var saved: ActivityReminder? = null
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent { SkillzTheme(darkTheme = false, dynamicColor = false) {
            ActivityEditor(null, PathsPrimaryTab.HABITS, emptyList(), false, null, {},
                { _, _, _, _, _, _, reminder -> saved = reminder })
        } }
        compose.onNodeWithText("Title").performTextInput("Reading")
        compose.onNodeWithContentDescription("Reminders").activate()
        compose.onNodeWithText("Monthly").activate()
        compose.onNodeWithText("First day").assertIsSelected()
        compose.onNodeWithText("Last day").activate()
        compose.onNodeWithContentDescription("Day 15").activate()
        compose.onNodeWithContentDescription("Day 31").activate()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Monthly").performScrollTo().assertIsSelected()
        compose.onNodeWithContentDescription("Day 15").assertIsSelected()
        compose.onNodeWithContentDescription("Day 31").assertIsSelected()
        compose.onNodeWithContentDescription("Reminders").performScrollTo()
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        java.io.File(instrumentation.targetContext.cacheDir, "reminders-monthly.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Save").activate()
        compose.runOnIdle {
            assertEquals(ActivityReminder(true, ActivityReminder.MONTHLY, monthDays = 1L or 2L or (1L shl 15) or (1L shl 31)), saved)
        }
    }

    @Test fun editPlanRestoresWeeklyChoicesAndCanDisableWithoutLosingThem() {
        val settings = ActivityReminder(true, ActivityReminder.WEEKLY, 17 * 60 + 45, 5)
        val plan = FlowPlanListItemUiModel(1, "Writing", null, "", false, null, false, false, 0, null, kind = "PLAN", reminder = settings)
        var saved: ActivityReminder? = null
        compose.setContent { SkillzTheme(darkTheme = false, dynamicColor = false) {
            ActivityEditor(plan, PathsPrimaryTab.PLANS, emptyList(), false, null, {},
                { _, _, _, _, _, _, reminder -> saved = reminder })
        } }
        compose.onNodeWithText("Weekly").performScrollTo().assertIsSelected()
        compose.onNodeWithContentDescription("Monday").assertIsSelected()
        compose.onNodeWithContentDescription("Wednesday").assertIsSelected()
        compose.onNodeWithContentDescription("Reminders").activate()
        compose.onNodeWithText("Weekly").assertDoesNotExist()
        compose.onNodeWithText("Save").activate()
        compose.runOnIdle { assertEquals(settings.copy(enabled = false), saved) }
    }

    @Test fun dailyReminderUsesTheSelectedTime() {
        val settings = ActivityReminder(enabled = true)
        val plan = FlowPlanListItemUiModel(1, "Writing", null, "", false, null, false, false, 0, null, reminder = settings)
        var saved: ActivityReminder? = null
        compose.setContent { SkillzTheme(darkTheme = false, dynamicColor = false) {
            ActivityEditor(plan, PathsPrimaryTab.PLANS, emptyList(), false, null, {},
                { _, _, _, _, _, _, reminder -> saved = reminder })
        } }
        compose.onNodeWithText("Reminder time:", substring = true).activate()
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.widget.TimePicker::class.java))
            .perform(object : androidx.test.espresso.ViewAction {
                override fun getConstraints() = androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.widget.TimePicker::class.java)
                override fun getDescription() = "Set reminder time to 18:35"
                override fun perform(controller: androidx.test.espresso.UiController, view: android.view.View) {
                    (view as android.widget.TimePicker).apply { hour = 18; minute = 35 }
                    controller.loopMainThreadUntilIdle()
                }
            })
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.button1))
            .perform(androidx.test.espresso.action.ViewActions.click())
        compose.onNodeWithText("Save").activate()
        compose.runOnIdle { assertEquals(settings.copy(minuteOfDay = 18 * 60 + 35), saved) }
    }

    @Test fun weeklyWithoutAnyDayCannotBeSaved() {
        val settings = ActivityReminder(true, ActivityReminder.WEEKLY, weekDays = 1)
        val plan = FlowPlanListItemUiModel(1, "Writing", null, "", false, null, false, false, 0, null, reminder = settings)
        compose.setContent { SkillzTheme(darkTheme = false, dynamicColor = false) {
            ActivityEditor(plan, PathsPrimaryTab.HABITS, emptyList(), false, null, {}, { _, _, _, _, _, _, _ -> })
        } }
        compose.onNodeWithContentDescription("Monday").activate()
        compose.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Friday").activate()
        compose.onNodeWithText("Save").performScrollTo().assertIsEnabled()
    }
    @Test fun horizonHabitCardShowsWeeklyDaysAndTimeAndUpdatesWhenTurnedOff() {
        val reminder = ActivityReminder(true, ActivityReminder.WEEKLY, 17 * 60 + 45, 5)
        val state = androidx.compose.runtime.mutableStateOf(FlowPlanListItemUiModel(1, "Writing", null, "", false, null,
            false, false, 0, null, kind = "HABIT", reminder = reminder))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val time = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 17); set(java.util.Calendar.MINUTE, 45)
        }.time)
        compose.setContent { SkillzTheme(darkTheme = false, dynamicColor = false) {
            ActivityCard(state.value, false, false, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Reminder · Weekly · Mon, Wed · $time").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(reminder = reminder.copy(enabled = false)) }
        compose.onNodeWithText("Reminders off").assertDoesNotExist()
        compose.onNodeWithText("Reminder · Weekly", substring = true).assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(reminder = reminder.copy(frequency = ActivityReminder.DAILY)) }
        compose.onNodeWithText("Reminder · Daily · $time").assertIsDisplayed()
    }

    @Test fun horizonPlanCardShowsAllMonthlyDatesAndPausedStateWhenArchived() {
        val reminder = ActivityReminder(true, ActivityReminder.MONTHLY, monthDays = 1L or 2L or (1L shl 15) or (1L shl 31))
        val plan = FlowPlanListItemUiModel(1, "Writing", null, "", false, null, false, false, 0, null, kind = "PLAN", reminder = reminder)
        val archived = androidx.compose.runtime.mutableStateOf(false)
        compose.setContent { SkillzTheme(darkTheme = false, dynamicColor = false) {
            ActivityCard(plan, false, archived.value, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Reminder · Monthly · First day, 15, 31, Last day ·", substring = true).assertIsDisplayed()
        compose.runOnIdle { archived.value = true }
        compose.onNodeWithText("Reminder paused · Monthly · First day, 15, 31, Last day ·", substring = true).assertIsDisplayed()
    }

    @Test fun createOneTimeHabitPicksDateAndTimeAndRestoresDraft() {
        var saved: ActivityReminder? = null
        val date = java.time.LocalDate.now().plusDays(10)
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent { SkillzTheme(darkTheme = false, dynamicColor = false) {
            ActivityEditor(null, PathsPrimaryTab.HABITS, emptyList(), false, null, {},
                { _, _, _, _, _, _, reminder -> saved = reminder })
        } }
        compose.onNodeWithText("Title").performTextInput("One chapter")
        compose.onNodeWithContentDescription("Reminders").activate()
        compose.onNodeWithText("One time").activate()
        compose.onNodeWithText("Reminder date:", substring = true).activate()
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.widget.DatePicker::class.java))
            .perform(object : androidx.test.espresso.ViewAction {
                override fun getConstraints() = androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.widget.DatePicker::class.java)
                override fun getDescription() = "Choose the one-time date"
                override fun perform(controller: androidx.test.espresso.UiController, view: android.view.View) {
                    (view as android.widget.DatePicker).updateDate(date.year, date.monthValue - 1, date.dayOfMonth)
                    controller.loopMainThreadUntilIdle()
                }
            })
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.button1))
            .perform(androidx.test.espresso.action.ViewActions.click())
        compose.onNodeWithText("Reminder time:", substring = true).activate()
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.widget.TimePicker::class.java))
            .perform(object : androidx.test.espresso.ViewAction {
                override fun getConstraints() = androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.widget.TimePicker::class.java)
                override fun getDescription() = "Choose 18:35"
                override fun perform(controller: androidx.test.espresso.UiController, view: android.view.View) {
                    (view as android.widget.TimePicker).apply { hour = 18; minute = 35 }
                    controller.loopMainThreadUntilIdle()
                }
            })
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.button1))
            .perform(androidx.test.espresso.action.ViewActions.click())
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("One time").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Save").activate()
        compose.runOnIdle {
            assertEquals(ActivityReminder(true, ActivityReminder.ONE_TIME, 18 * 60 + 35, oneTimeDateEpochDay = date.toEpochDay()), saved)
        }
    }

    @Test fun editOneTimePlanRestoresDateAndCanDisableIt() {
        val date = java.time.LocalDate.now().plusDays(3)
        val settings = ActivityReminder(true, ActivityReminder.ONE_TIME, 18 * 60 + 35, oneTimeDateEpochDay = date.toEpochDay())
        val plan = FlowPlanListItemUiModel(1, "Writing", null, "", false, null, false, false, 0, null, kind = "PLAN", reminder = settings)
        var saved: ActivityReminder? = null
        compose.setContent { SkillzTheme(darkTheme = false, dynamicColor = false) {
            ActivityEditor(plan, PathsPrimaryTab.PLANS, emptyList(), false, null, {},
                { _, _, _, _, _, _, reminder -> saved = reminder })
        } }
        compose.onNodeWithText("One time").performScrollTo().assertIsSelected()
        val label = date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
        compose.onNodeWithText("Reminder date: $label").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Reminders").activate()
        compose.onNodeWithText("Save").activate()
        compose.runOnIdle { assertEquals(settings.copy(enabled = false), saved) }
    }

    @Test fun horizonOneTimeCardShowsTheDateAndPastStatusAfterConsumption() {
        val date = java.time.LocalDate.now().plusDays(3)
        val settings = ActivityReminder(true, ActivityReminder.ONE_TIME, oneTimeDateEpochDay = date.toEpochDay())
        val plan = androidx.compose.runtime.mutableStateOf(FlowPlanListItemUiModel(1, "Writing", null, "", false, null,
            false, false, 0, null, kind = "PLAN", reminder = settings))
        compose.setContent { SkillzTheme(darkTheme = false, dynamicColor = false) {
            ActivityCard(plan.value, false, false, {}, {}, {}, {}, {})
        } }
        val label = date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
        compose.onNodeWithText("Reminder · One time · $label ·", substring = true).assertIsDisplayed()
        compose.runOnIdle { plan.value = plan.value.copy(reminder = settings.copy(oneTimeConsumedAt = System.currentTimeMillis())) }
        compose.onNodeWithText("Past reminder · One time · $label ·", substring = true).assertIsDisplayed()
    }

}
