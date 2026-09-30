package com.kingkharnivore.skillz.domain.land

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.achievement.LandBadgeCatalog
import com.kingkharnivore.skillz.utils.shell.LandCreatureCatalog
import org.junit.Assert.*
import org.junit.Test

class LandLocalizationRuntimeTest {
    @Test fun packagedResourcesResolveEverySpeciesAndBadgeInAllLocales() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val expected=listOf("en" to "Tiger", "hi" to "बाघ", "mr" to "वाघ", "es" to "Tigre")
        expected.forEach { (tag,tiger) ->
            val localized=context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags(tag))
            })
            assertEquals(tiger,localized.getString(R.string.land_creature_tiger))
            val names=LandCreatureCatalog.all.map { localized.getString(it.titleRes) }
            assertEquals(114,names.toSet().size)
            LandBadgeCatalog.specs.forEach {
                assertTrue(localized.getString(it.titleRes).isNotBlank())
                assertTrue(localized.getString(it.descriptionRes).isNotBlank())
            }
            listOf(0,1,2,99,10000).forEach { count ->
                assertFalse(localized.getString(R.string.land_animal_a11y,tiger,count,99).contains("%"))
                assertFalse(localized.getString(R.string.land_arc_requirement,count).contains("%"))
                assertFalse(localized.getString(R.string.badge_species_mastery_title,tiger).contains("%"))
                assertFalse(localized.resources.getQuantityString(R.plurals.badges_summary,count,count,count,count).contains("%"))
            }
        }
    }
    @Test fun masteryTemplatesKeepSpeciesNamesUninflectedForEveryLandSpecies() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        listOf("en", "hi", "mr", "es").forEach { tag ->
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags(tag))
            })
            LandCreatureCatalog.all.forEach { creature ->
                val name = localized.getString(creature.titleRes)
                listOf(R.string.mastery_first_species_support, R.string.mastery_second_species_support,
                    R.string.mastery_third_species_support).forEach { resource ->
                    assertTrue(localized.getString(resource, name).contains(name))
                }
                assertTrue(localized.getString(R.string.mastery_many_species_support, 10, name).contains(name))
            }
        }
    }

}
