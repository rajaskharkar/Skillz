package com.kingkharnivore.skillz.ui.screen.story

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.domain.analytics.*
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.model.state.FlowListUiState
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.ui.screen.story.data.lifetimeDataItems
import com.kingkharnivore.skillz.ui.screen.story.header.StoryHeaderScrollableWithStickyTabs
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.utils.time.StoryPeriod
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class LifetimeDataUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val stats = LifetimeAnalytics.calculate(
        (1L..8L).map { id ->
            val start = LocalDate.of(2026, 9, 1).atStartOfDay(ZoneId.of("America/Chicago")).toInstant().toEpochMilli() + id * 86_400_000L
            FlowObservation(id, id, FlowMode.entries[(id % 3).toInt()], start, start + id * 3_600_000,
                start + id * 3_600_000, id * 3_600_000)
        },
        listOf("Art", "Building", "Cycling", "Design", "Learning", "Music", "Reading", "Writing")
            .mapIndexed { i, name -> i + 1L to name }.toMap(),
        LocalDate.of(2026, 10, 21), ZoneId.of("America/Chicago"), Locale.US
    )
    private val session = FlowListItemUiModel(sessionId = 77, title = "An early reading Flow",
        description = "A saved Chronicle from the beginning of this Journey.", tagId = 1,
        tagName = "Art", journeyColor = Color.Blue, durationMs = 3_600_000,
        createdAt = 1, score = 25, isSurge = false, surgePoints = 0,
        chronicleTexts = listOf("A saved Chronicle from the beginning of this Journey."))

    @Test fun lifetimeAddsDataAndJourneyBarsOpenExpandableFullHistory() {
        var opened: Long? = null
        compose.setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                var period by remember { mutableStateOf(StoryPeriod.WEEK) }
                var sheetOpen by remember { mutableStateOf(false) }
                Surface {
                    StoryHeaderScrollableWithStickyTabs(
                        uiState = FlowListUiState(isLoading = false, period = period,
                            lifetimeStats = stats, currentScore = 12345, hasAnyRecordedFlows = true,
                            hasAnyRecordedArtifacts = true, viewJourneysTitle = "Art",
                            isViewJourneysOpen = sheetOpen, viewJourneysSessions = listOf(session)),
                        listState = rememberLazyListState(), onTagToggled = {}, onClearAllTags = {},
                        onPeriodSelected = { period = it }, onPrev = {}, onNext = {}, onToday = {},
                        onOpenViewJourneys = { opened = it; sheetOpen = true }, onCloseViewJourneys = { sheetOpen = false },
                        onSessionClick = {}, onDeleteSession = {}, onDeletePulse = {},
                        onCreatePulseForSession = { _, _, _, _ -> }, onAddSessionClick = {}, onEditArc = {},
                        createChronicleEditor = { _, _ -> error("Editor not opened") }, onSaveEntry = { _, _, _, _ -> }
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Data").assertDoesNotExist()
        compose.onNodeWithText("Lifetime").performScrollTo().performClick()
        compose.onNode(hasContentDescription("Lifetime Scyra Score", substring = true)).assertExists()
        compose.onNodeWithContentDescription("Data").performClick()
        compose.onNodeWithTag("story-list").performScrollToNode(hasText("Your lifetime in Flow"))
        screenshot("overview-light")
        compose.onNodeWithTag("story-list").performScrollToNode(hasText("How you Flow"))
        screenshot("modes-light")
        compose.onNodeWithTag("story-list").performScrollToNode(hasTestTag("lifetime-journey-chart"))
        compose.onNodeWithTag("lifetime-journey-chart").performScrollToIndex(7)
        compose.onNodeWithTag("lifetime-journey-8").assertIsDisplayed()
        compose.onNodeWithTag("lifetime-journey-chart").performScrollToIndex(0)
        screenshot("journeys-light")
        compose.onNodeWithTag("lifetime-journey-1").performClick()
        compose.runOnIdle { assertEquals(1L, opened) }
        compose.onNodeWithText("An early reading Flow").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Edit flow").assertExists()
        compose.onNodeWithText("A saved Chronicle from the beginning of this Journey.").assertExists()
        screenshot("journey-expanded")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("story-list").performScrollToIndex(0)
        compose.onNodeWithText("Day", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Data").assertDoesNotExist()
        compose.onNodeWithText("Your lifetime in Flow").assertDoesNotExist()
    }

    @Test fun darkLargeTextChartsAndRhythmRemainReadable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                SkillzTheme(darkTheme = true, dynamicColor = false) {
                    Surface {
                        LazyColumn(Modifier.fillMaxSize().testTag("data-list")) { lifetimeDataItems(stats) {} }
                    }
                }
            }
        }
        screenshot("overview-dark-large")
        compose.onNodeWithTag("data-list").performScrollToNode(hasText("How you Flow"))
        screenshot("modes-dark-large")
        compose.onNodeWithTag("data-list").performScrollToNode(hasText("When you make time"))
        compose.onNodeWithText("Active hours").performScrollTo().performClick()
        compose.onNodeWithText("Active hours").assertIsSelected()
        compose.onNodeWithTag("data-list").performScrollToIndex(5)
        screenshot("rhythm-dark-large")
    }

    @Test fun emptyHistoryExplainsHowToBegin() {
        compose.setContent {
            SkillzTheme { LazyColumn { lifetimeDataItems(LifetimeStats()) {} } }
        }
        compose.onNodeWithText("Record a Flow to start seeing your lifetime stats.").assertExists()
        compose.onNodeWithText("Average daily hours").assertDoesNotExist()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val file = File(context.getExternalFilesDir(null), "lifetime/$name.png")
        file.parentFile!!.mkdirs()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
