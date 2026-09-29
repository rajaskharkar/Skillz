package com.kingkharnivore.skillz.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class LandLocalizationTest {
    private val res = File("app/src/main/res")
    private val languages = listOf("hi", "mr", "es")
    private fun elements(file: File): List<Element> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val nodes = factory.newDocumentBuilder().parse(file).documentElement.childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }
    private fun strings(folder: String): Map<String, Element> =
        File(res, folder).listFiles()!!.filter { it.extension == "xml" }.flatMap(::elements)
            .filter { it.tagName == "string" || it.tagName == "plurals" }.associateBy { it.getAttribute("name") }
    private fun placeholders(text: String) = Regex("%([0-9]+)\\${'$'}[,0-9.\\-]*[a-zA-Z]").findAll(text)
        .map { it.value }.sorted().toList()

    @Test fun everyFeatureResourceHasAllThreeTranslationsAndMatchingArguments() {
        val defaults = strings("values")
        val keys = listOf("land_strings.xml", "land_badge_strings.xml")
            .flatMap { elements(File(res, "values/$it")) }.map { it.getAttribute("name") } +
            javaClass.getResourceAsStream("/land/localization-shared-keys.txt")!!.bufferedReader().readLines()
        languages.forEach { language ->
            val translations = strings("values-$language")
            keys.forEach { key ->
                val actual = translations[key]
                assertNotNull("$language missing $key", actual)
                assertTrue("$language empty $key", actual!!.textContent.isNotBlank())
                assertEquals("$language arguments for $key", placeholders(defaults.getValue(key).textContent), placeholders(actual.textContent))
            }
        }
    }
    @Test fun speciesNamesAreCompleteAndDoNotCollapseDistinctSpecies() {
        listOf("values", "values-hi", "values-mr", "values-es").forEach { folder ->
            val names = elements(File(res,"$folder/land_strings.xml"))
                .filter { it.getAttribute("name").startsWith("land_creature_") && it.getAttribute("name") != "land_creature_description" }
            assertEquals(folder, 114, names.size)
            assertEquals("Duplicate species translations in $folder",114,names.map { it.textContent }.toSet().size)
        }
    }
    @Test fun noDuplicateResourceKeysInAnySupportedLocale() {
        listOf("values", "values-hi", "values-mr", "values-es").forEach { folder ->
            val nodes = File(res,folder).listFiles()!!.filter { it.extension=="xml" }.flatMap(::elements)
                .filter { it.tagName in setOf("string","plurals") }
            val keys=nodes.map { it.tagName to it.getAttribute("name") }
            assertEquals("Duplicate resource in $folder",keys.size,keys.toSet().size)
        }
    }
    @Test fun featureUiDoesNotFallBackToEnglishDisplayNames() {
        val ui=File("app/src/main/java/com/kingkharnivore/skillz/ui/screen")
        listOf("shell/rooms/blue/BlueRealmSelector.kt", "shell/rooms/blue/LandZonePage.kt",
            "shell/rooms/blue/BlueUtils.kt", "shell/rooms/blue/TheBlueDepthRail.kt",
            "shell/rooms/blue/BeyondBlueEncounterSheet.kt", "shell/rooms/stillwater/StillwaterRoomScreen.kt",
            "flow/reward/ArcSummaryContent.kt").forEach { path ->
            assertFalse("English catalog label in $path", File(ui,path).readText().contains(".displayName"))
        }
    }
}
