package com.kingkharnivore.skillz.domain.achievement

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.LocaleList
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.shell.inventory.*
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class LifetimePointsBadgeUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext.let { base ->
        base.createConfigurationContext(Configuration(base.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags("en"))
        })
    }
    private fun dashboard(points: Long) = BadgeDashboardCalculator.calculate(emptyList(), emptyList(),
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), lifetimeScyraPoints = points)
    private fun capture(name: String) {
        val dir = File(context.getExternalFilesDir(null), "lifetime-badges").apply { mkdirs() }
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun milestoneArtworkShowsEveryLabelInBothThemesAndAtLargeFontScale() {
        val dark = mutableStateOf(false)
        val fontScale = mutableFloatStateOf(1f)
        val badges = dashboard(1_000_000_000).badges.associateBy { it.badgeId }
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration,
                LocalDensity provides Density(density, fontScale.floatValue)) {
                SkillzTheme(darkTheme = dark.value, dynamicColor = false) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LifetimePointsBadges.specs.chunked(4).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { BadgeMedallion(badges.getValue(it.id), BadgeMedallionSize.Small) }
                                }
                            }
                        }
                    }
                }
            }
        }
        listOf(false, true).forEach { theme ->
            compose.runOnIdle { dark.value = theme; fontScale.floatValue = if (theme) 2f else 1f }
            LifetimePointsBadges.specs.forEach {
                compose.onNodeWithText(it.label, useUnmergedTree = true).assertIsDisplayed()
                val target = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(it.target)
                compose.onNode(hasContentDescription(context.getString(R.string.lifetime_points_badge_title, target), substring = true) and hasText(it.label)).assertExists()
            }
            capture(if (theme) "milestones-dark-large-text" else "milestones-light")
        }
    }

    @Test fun badgeBookSearchFindsMilestoneAndTrackingUsesPoints() {
        var tracked: String? = null
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration) {
                SkillzTheme(dynamicColor = false) {
                    BadgesScreen(ShellUiState(badgeDashboard = dashboard(1500)), { _, _ -> }, {}, {},
                        { tracked = it }, {}, {}, {}, {}, {}, {}, {}, {},
                        initialTab = BadgesTab.BADGE_BOOK, initialBookSection = BadgeBookSection.ALL_BADGES)
                }
            }
        }
        compose.onNodeWithText(context.getString(R.string.badges_search)).performScrollTo()
            .performTextInput("10,000 Lifetime Scyra Points")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNode(hasText("10,000 Lifetime Scyra Points") and !hasSetTextAction()).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1,500 / 10,000 Lifetime Scyra Points").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Track").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("lifetime_points_10000", tracked) }
        capture("badge-book")
    }
}
