package com.kingkharnivore.skillz.domain.power

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.ui.screen.shell.inventory.*
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.graphics.luminance
import org.junit.Rule
import org.junit.Test

class RedBadgeUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext.let { base ->
        base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocales(LocaleList.forLanguageTags("en")) })
    }
    private fun badges() = BadgeDashboardCalculator.calculate(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList()).badges.associateBy { it.badgeId }

    @Test fun everyNamedRedBadgeExposesTrackingAndTheRedAction() {
        val all = badges()
        val specs = RedBadgeCatalog.specs.filter { it.id.startsWith("red_") }
        val selected = mutableStateOf(specs.first().id)
        var tracked = ""
        var destination: BadgeActionDestination? = null
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration) {
                SkillzTheme(dynamicColor=false) {
                    val badge = all.getValue(selected.value)
                    BadgeDetailsSheet(badge, {}, {}, { tracked=badge.badgeId }, { destination=badge.action })
                }
            }
        }
        specs.forEach { spec ->
            compose.runOnIdle { selected.value=spec.id }
            compose.onNodeWithText(context.getString(spec.titleRes)).assertIsDisplayed()
            compose.onNodeWithText("Track").performScrollTo().performClick()
            compose.onNodeWithText("Open in The Red").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(spec.id,tracked); assertEquals(all.getValue(spec.id).action,destination) }
        }
    }

    @Test fun powerRecommendationKeepsHourUnitsAndRedProgressExplainsItsRoster() {
        val mode=mutableStateOf("power_bedrock")
        val all=badges()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration) {
                val badge=all.getValue(mode.value)
                Text(recommendationText(if (mode.value=="power_bedrock") badge.copy(currentProgress=1934) else badge.copy(currentProgress=11)))
            }
        }
        compose.onNodeWithText("32h 14m / 50h").assertIsDisplayed()
        compose.runOnIdle { mode.value="red_dawn" }
        compose.onNodeWithText("11 / 14 dinosaurs acquired").assertIsDisplayed()
    }
    @Test fun chestRedFilterShowsDinosaursAndExcludesBlueCreatures() {
        val red = com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity(
            "red", "triassic_saturnalia",1,"red_purchase",null,null,null,false,true)
        val blue = red.copy(instanceId="blue",findId="focus_minnow",sourceType="flow")
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration) {
                SkillzTheme(dynamicColor=false) {
                    ShellChestScreen(com.kingkharnivore.skillz.viewmodel.shell.ShellUiState(
                        finds=listOf(red,blue),chestFilter=com.kingkharnivore.skillz.utils.shell.ChestFilterOption.Red),
                        {_,_->},{_,_->},{},{},{})
                }
            }
        }
        compose.onNodeWithContentDescription("Saturnalia", substring=true).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(com.kingkharnivore.skillz.R.string.shell_find_minnow_title), substring=true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Saturnalia", substring=true).performClick()
        compose.onNode(hasText("Saturnalia") and hasAnyAncestor(isDialog())).assertIsDisplayed()
    }

    @Test fun oneTimeMedallionsHideCountsAndUseTintInBothThemes() {
        val all=badges()
        val selected=mutableStateOf("red_first_footprint")
        val dark=mutableStateOf(false)
        var background=androidx.compose.ui.graphics.Color.Unspecified
        var surface=androidx.compose.ui.graphics.Color.Unspecified
        var accent=androidx.compose.ui.graphics.Color.Unspecified
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration) {
                SkillzTheme(darkTheme=dark.value,dynamicColor=false) {
                    // Historical duplicate rows must not make one-time awards look repeatable.
                    val badge=all.getValue(selected.value).copy(everEarned=true,lifetimeCount=5)
                    background=badgeMedallionBackground(badge)
                    accent=badgeMedallionAccent(badge,background)
                    surface=androidx.compose.material3.MaterialTheme.colorScheme.surface
                    BadgeMedallion(badge)
                }
            }
        }
        listOf(false,true).forEach { theme ->
            compose.runOnIdle { dark.value=theme }
            listOf("red_first_footprint","mastery_first").forEach { id ->
                compose.runOnIdle { selected.value=id }
                compose.onNodeWithText("×5",useUnmergedTree=true).assertDoesNotExist()
                compose.onNodeWithContentDescription("Completed 5 times",substring=true).assertDoesNotExist()
                compose.runOnIdle {
                    assertNotEquals(surface,background)
                    val a=accent.luminance(); val b=background.luminance()
                    assertTrue((maxOf(a,b)+.05f)/(minOf(a,b)+.05f)>=3f)
                }
            }
            compose.runOnIdle { selected.value="badge_flow_10_min" }
            compose.onNodeWithText("×5",useUnmergedTree=true).assertIsDisplayed()
            compose.onNodeWithContentDescription("Completed 5 times",substring=true).assertIsDisplayed()
            compose.runOnIdle { assertEquals(surface,background) }
        }
    }

    @Test fun oneTimeDetailsNeverShowExactCountButRepeatableDetailsDo() {
        val all=badges()
        val selected=mutableStateOf("mastery_first")
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration) {
                SkillzTheme(dynamicColor=false) {
                    BadgeDetailsSheet(all.getValue(selected.value).copy(everEarned=true,lifetimeCount=5),{},{},{},{})
                }
            }
        }
        compose.onNodeWithText("Exact count: 5").assertDoesNotExist()
        compose.runOnIdle { selected.value="badge_flow_10_min" }
        compose.onNodeWithText("Exact count: 5").assertIsDisplayed()
    }

}
