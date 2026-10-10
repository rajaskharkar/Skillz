package com.kingkharnivore.skillz.domain.green

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class GreenLocalizationTest {
    private fun strings(folder: String): Map<String,String> = File("app/src/main/res/$folder").listFiles()!!
        .filter { it.extension=="xml" }.flatMap { file ->
            val nodes=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement.childNodes
            (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }.filter { it.tagName=="string" }.map { it.getAttribute("name") to it.textContent }
        }.toMap()
    @Test fun growthPluralResourcesCoverSingularAndPluralInEveryLocale() {
        val arguments=Regex("%[0-9]+\\$[ds]")
        val expected=mutableMapOf<String,List<String>>()
        listOf("values","values-es","values-hi","values-mr").forEach { locale ->
            val nodes=DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File("app/src/main/res/$locale/growth_badges.xml")).getElementsByTagName("plurals")
            assertEquals(5,nodes.length)
            (0 until nodes.length).forEach { index ->
                val plural=nodes.item(index) as Element
                val items=plural.getElementsByTagName("item")
                assertEquals(if(locale=="values-es") 3 else 2,items.length)
                (0 until items.length).forEach { itemIndex ->
                    val item=items.item(itemIndex) as Element
                    val key=plural.getAttribute("name")+":"+item.getAttribute("quantity").let { if(it=="many") "other" else it }
                    val format=arguments.findAll(item.textContent).map { it.value }.sorted().toList()
                    if(locale=="values") expected[key]=format else assertEquals("$locale $key",expected[key],format)
                }
            }
        }
    }
    @Test fun allGreenTranslationsPreservePlaceholdersAndNoLocaleContainsTheRetiredRoom() {
        val english=strings("values")
        val arguments=Regex("%[0-9]+\\$[ds]")
        listOf("values","values-es","values-hi","values-mr").forEach { locale ->
            val translated=strings(locale)
            english.filterKeys { it.startsWith("green_") || it.startsWith("growth_") }.forEach { (key,text) ->
                assertNotNull("$locale $key",translated[key])
                assertEquals("$locale $key",arguments.findAll(text).map { it.value }.sorted().toList(),arguments.findAll(translated.getValue(key)).map { it.value }.sorted().toList())
            }
            assertFalse("Retired room in $locale",translated.values.any { "Stillwater" in it })
        }
    }
}
