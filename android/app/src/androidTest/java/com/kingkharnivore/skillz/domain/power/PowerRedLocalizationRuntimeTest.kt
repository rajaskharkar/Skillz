package com.kingkharnivore.skillz.domain.power

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.achievement.RedBadgeCatalog
import com.kingkharnivore.skillz.utils.shell.RedCreatureCatalog
import org.junit.Assert.*
import org.junit.Test

class PowerRedLocalizationRuntimeTest {
    @Test fun allLanguagesResolveEveryDinosaurBadgeAndCountWithoutFallbackOrFormatErrors() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        listOf("en", "es", "hi", "mr").forEach { language ->
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags(language))
            })
            val resources = localized.resources
            val english = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags("en"))
            })
            if (language != "en") listOf(
                R.string.shell_badge_flow_10_title,R.string.shell_badge_flow_30_title,
                R.string.shell_badge_flow_60_title,R.string.shell_badge_flow_120_title,
                R.string.shell_badge_flow_10_description,R.string.shell_badge_flow_30_description,
                R.string.shell_badge_flow_60_description,R.string.shell_badge_flow_120_description
            ).forEach { id -> assertNotEquals("$language/$id",english.getString(id),localized.getString(id)) }
            assertFalse(localized.getString(R.string.badge_one_time_a11y,
                localized.getString(R.string.red_first_footprint_title),localized.getString(R.string.badge_earned),0,"","").contains("%"))
            com.kingkharnivore.skillz.domain.achievement.BadgeBookCollections.collections.forEach { collection ->
                val title=localized.getString(collection.titleRes)
                assertTrue(title.isNotBlank())
                if(language != "en") assertNotEquals(english.getString(collection.titleRes),title)
                assertFalse(localized.getString(R.string.book_collection_award_title,title).contains("%"))
                assertFalse(localized.getString(R.string.book_collection_award_description,title).contains("%"))
            }
            assertFalse(resources.getQuantityString(R.plurals.book_member_progress,4,2,4).contains("%"))
            assertFalse(localized.getString(R.string.chest_environment_selected,localized.getString(R.string.red_jurassic)).contains("%"))
            assertFalse(localized.getString(R.string.chest_creature_environment,localized.getString(R.string.red_title),localized.getString(R.string.red_jurassic)).contains("%"))
            val names = RedCreatureCatalog.entries.map { localized.getString(it.nameRes) }
            assertEquals(language, 69, names.toSet().size)
            RedBadgeCatalog.specs.forEach { badge ->
                assertTrue(localized.getString(badge.titleRes).isNotBlank())
                assertTrue(localized.getString(badge.descriptionRes).isNotBlank())
            }
            listOf(0, 1, 2, 99, 12450, 1_000_000).forEach { count ->
                listOf(R.plurals.red_balance, R.plurals.red_price, R.plurals.red_grow, R.plurals.red_release,
                    R.plurals.red_lifetime, R.plurals.power_pebbles_earned, R.plurals.power_pearls_earned,
                    R.plurals.power_voyage_count, R.plurals.power_days).forEach { id ->
                    assertFalse("$language/$id/$count", resources.getQuantityString(id, count, count).contains("%"))
                }
                assertFalse(resources.getQuantityString(R.plurals.red_scene_description, count, localized.getString(R.string.red_triassic), count).contains("%"))
            }
            if (language == "es") {
                assertEquals("1 guijarro", resources.getQuantityString(R.plurals.red_balance, 1, 1))
                assertEquals("2 guijarros", resources.getQuantityString(R.plurals.red_balance, 2, 2))
                assertEquals("12.450 guijarros", resources.getQuantityString(R.plurals.red_balance, 12450, 12450))
                assertEquals("1 sesión Power", resources.getQuantityString(R.plurals.power_voyage_count, 1, 1))
            }
            if (language == "mr") {
                assertTrue(resources.getQuantityString(R.plurals.red_balance, 1, 1).endsWith("खडा"))
                assertTrue(resources.getQuantityString(R.plurals.red_balance, 2, 2).endsWith("खडे"))
            }
        }
    }
}
