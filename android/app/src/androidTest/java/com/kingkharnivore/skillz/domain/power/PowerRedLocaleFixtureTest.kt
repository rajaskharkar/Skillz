package com.kingkharnivore.skillz.domain.power

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.debug.PowerRedVisualTestActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PowerRedLocaleFixtureTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun fixture(language: String, scenario: String, scale: Float = 1f) =
        ActivityScenario.launch<PowerRedVisualTestActivity>(Intent(context, PowerRedVisualTestActivity::class.java)
            .putExtra("locale", language).putExtra("scenario", scenario).putExtra("fontScale", scale))

    @Test fun catalogDialogUsesEachFixtureLanguageWithoutChangingApplicationLocale() {
        val original = context.resources.configuration.locales.toLanguageTags()
        listOf("en" to "Beyond the Red", "es" to "Más allá de The Red",
            "hi" to "द रेड के परे", "mr" to "द रेडच्या पलीकडे").forEach { (language, title) ->
            fixture(language, "catalog", 1.5f).use {
                compose.onNode(hasText(title) and hasAnyAncestor(hasTestTag("red-catalog-sheet"))).assertIsDisplayed()
                val sheet = compose.onNodeWithTag("red-catalog-sheet").assertIsDisplayed().getUnclippedBoundsInRoot()
                val localized = context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply {
                    setLocales(android.os.LocaleList.forLanguageTags(language))
                })
                listOf(com.kingkharnivore.skillz.R.string.red_triassic, com.kingkharnivore.skillz.R.string.red_jurassic,
                    com.kingkharnivore.skillz.R.string.red_cretaceous).forEach { era ->
                    val bounds = compose.onNode(hasText(localized.getString(era)) and hasAnyAncestor(hasTestTag("red-catalog-sheet")))
                        .assertIsDisplayed().getUnclippedBoundsInRoot()
                    org.junit.Assert.assertTrue("Era chip must fit the sheet", bounds.left >= sheet.left && bounds.right <= sheet.right)
                }
            }
            assertEquals(original, context.resources.configuration.locales.toLanguageTags())
        }
    }

    @Test fun localizedOwnedDetailKeepsLastActionReachableAt150PercentText() {
        listOf("es" to "Intercambiar más allá de The Blue", "hi" to "द ब्लू के पार विनिमय करें",
            "mr" to "द ब्लूच्या पलीकडे देवाणघेवाण करा").forEach { (language, trade) ->
            fixture(language, "owned", 1.5f).use {
                compose.onNodeWithText(trade).performScrollTo().assertIsDisplayed()
            }
        }
    }
    @Test fun powerBadgeActionsAreVisibleOnOpenAndReachableAtLargeText() {
        listOf("en", "es", "hi", "mr").forEach { language ->
            val localized = context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply {
                setLocales(android.os.LocaleList.forLanguageTags(language))
            })
            val label = localized.getString(com.kingkharnivore.skillz.R.string.badge_open_action)
            fixture(language, "power-badge-progress").use {
                compose.onNodeWithText(label).assertIsDisplayed()
            }
            fixture(language, "power-badge-progress", 1.5f).use {
                compose.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            }
        }
    }

}
