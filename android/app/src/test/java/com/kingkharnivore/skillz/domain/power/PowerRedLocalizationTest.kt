package com.kingkharnivore.skillz.domain.power

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class PowerRedLocalizationTest {
    private fun resources(language: String): Map<String, Element> {
        val directory = if (language == "en") "values" else "values-$language"
        val root = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }.newDocumentBuilder().parse(File("app/src/main/res/$directory/strings_power_red.xml")).documentElement
        return (0 until root.childNodes.length).mapNotNull { root.childNodes.item(it) as? Element }
            .associateBy { it.getAttribute("name") }
    }
    private fun forms(element: Element): List<String> = if (element.tagName == "string") listOf(element.textContent)
        else (0 until element.childNodes.length).mapNotNull { element.childNodes.item(it) as? Element }.map { it.textContent }
    private fun arguments(text: String) = Regex("%([0-9]+)\\$[,0-9.]*([dsf])").findAll(text)
        .map { it.groupValues[1] to it.groupValues[2] }.sortedBy { it.first }.toList()

    @Test fun everySupportedLanguageHasCompleteResourcesAndCompatibleFormats() {
        val defaults = resources("en")
        listOf("es", "hi", "mr").forEach { language ->
            val actual = resources(language)
            assertEquals(language, defaults.keys, actual.keys)
            defaults.forEach { (key, expected) ->
                val translated = actual.getValue(key)
                assertEquals("$language/$key type", expected.tagName, translated.tagName)
                forms(translated).forEach { text ->
                    assertTrue("$language/$key empty", text.isNotBlank())
                    assertEquals("$language/$key arguments", arguments(forms(expected).first()), arguments(text))
                }
                if (translated.tagName == "plurals") {
                    val quantities = (0 until translated.childNodes.length).mapNotNull { translated.childNodes.item(it) as? Element }.map { it.getAttribute("quantity") }
                    assertTrue("$language/$key required quantities", quantities.containsAll(if (language == "es") listOf("one", "many", "other") else listOf("one", "other")))
                }
            }
        }
    }

    @Test fun all69NamesStayDistinctAndLocalScriptMatchesBadgeReferences() {
        val defaults = resources("en")
        val speciesKeys = defaults.keys.filter { it.startsWith("red_triassic_") || it.startsWith("red_jurassic_") || it.startsWith("red_cretaceous_") }.filterNot { it.endsWith("_age") || it.endsWith("_world") }
        assertEquals(69, speciesKeys.size)
        listOf("en", "es", "hi", "mr").forEach { language ->
            val localized = resources(language)
            assertEquals(language, 69, speciesKeys.map { localized.getValue(it).textContent }.toSet().size)
            if (language in listOf("hi", "mr")) {
                speciesKeys.forEach { key ->
                    val latinName = defaults.getValue(key).textContent
                    val localName = localized.getValue(key).textContent
                    assertTrue(localName, localName.any { it in '\u0900'..'\u097f' })
                    defaults.filter { it.key.endsWith("_description") && it.value.textContent.contains(latinName) }.keys.forEach { badge ->
                        assertTrue("$language/$badge missing $localName", localized.getValue(badge).textContent.contains(localName))
                    }
                }
                localized.forEach { (key, element) ->
                    assertFalse("English product prose in $language/$key", Regex("\\b(Power|POWER|Journey|Journeys|Red|Beyond)\\b").containsMatchIn(element.textContent))
                }
            }
        }
    }

    @Test fun currencyGroupingAndSpanishDecimalAreLocalized() {
        assertTrue(String.format(Locale.forLanguageTag("es"), forms(resources("es").getValue("red_price")).last(), 12450).contains("12.450"))
        listOf("power_helper", "power_mode_subtitle", "power_reward_time").forEach { key ->
            assertTrue(key, resources("es").getValue(key).textContent.contains("1,5"))
        }
    }
}
