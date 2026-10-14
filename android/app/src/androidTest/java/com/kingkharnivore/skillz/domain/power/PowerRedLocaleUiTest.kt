package com.kingkharnivore.skillz.domain.power

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.ui.screen.flow.reward.rememberRewardRevealTextProvider
import com.kingkharnivore.skillz.ui.screen.shell.rooms.voyage.PowerVoyageContent
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.utils.shell.voyage.*
import java.time.YearMonth
import java.util.Locale
import org.junit.Rule
import org.junit.Test

class PowerRedLocaleUiTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun localized(tag: String) = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
        setLocales(LocaleList.forLanguageTags(tag))
    })

    @Test fun rewardCurrenciesUseIndependentPluralsAndUpdateWhenAppLanguageChanges() {
        val language = mutableStateOf("es")
        compose.setContent {
            val local = localized(language.value)
            CompositionLocalProvider(LocalContext provides local, LocalConfiguration provides local.resources.configuration) {
                val text = rememberRewardRevealTextProvider()
                Text(text.powerCurrencies(1, 2))
            }
        }
        compose.onNodeWithText("+1 perla · +2 guijarros").assertIsDisplayed()
        compose.runOnIdle { language.value = "en" }
        compose.onNodeWithText("+1 Pearl · +2 Pebbles").assertIsDisplayed()
    }

    @Test fun voyageMonthUsesAppLanguageEvenWhenDeviceDefaultDiffers() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val local = localized("es")
            compose.setContent {
                CompositionLocalProvider(LocalContext provides local, LocalConfiguration provides local.resources.configuration) {
                    SkillzTheme(dynamicColor = false) {
                        PowerVoyageContent(PowerVoyageStats(mapOf(0 to PowerRangeStats(60_000, 1, .5,
                            listOf(PowerJourneyTrend(1, "Example", 60_000, .5, mapOf(YearMonth.of(2026, 1) to .5)))))))
                    }
                }
            }
            compose.onNodeWithText("Example").performClick()
            compose.onNodeWithText("ene 2026 · 50,0% Power").assertIsDisplayed()
            compose.onNodeWithText("1 sesión Power").assertIsDisplayed()
        } finally { Locale.setDefault(previous) }
    }
    @Test fun powerHourBadgeProgressUsesHoursAndMinutes() {
        val local = localized("en")
        compose.setContent {
            CompositionLocalProvider(LocalContext provides local, LocalConfiguration provides local.resources.configuration) {
                Text(com.kingkharnivore.skillz.ui.screen.shell.inventory.badgeObjectiveProgressText("power_bedrock", 1934, 3000, 1066))
            }
        }
        compose.onNodeWithText("32h 14m / 50h").assertIsDisplayed()
    }
}
