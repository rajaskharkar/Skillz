package com.kingkharnivore.skillz.domain.anchor

import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedAnchorAppCatalogTest {
    @Test
    fun commonDistractionCatalogIncludesRequestedReliableSelections() {
        val packages = CuratedAnchorAppCatalog.apps.map { it.packageName }.toSet()

        assertTrue("Instagram should be selectable from Common Distractions", "com.instagram.android" in packages)
        assertTrue("WhatsApp should be selectable from Common Distractions", "com.whatsapp" in packages)
        assertTrue("Reddit should be selectable from Common Distractions", "com.reddit.frontpage" in packages)
    }
}
