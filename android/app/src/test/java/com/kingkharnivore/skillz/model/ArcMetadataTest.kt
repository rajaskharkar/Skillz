package com.kingkharnivore.skillz.model

import org.junit.Assert.*
import org.junit.Test

class ArcMetadataTest {
    @Test fun normalizationTrimsEdgesButPreservesInternalContent() {
        val result = ArcMetadata.normalize(7, "  Morning  Flow  ", "\nline one\nline two\n", " ", "hi", "later")
        assertEquals("Morning  Flow", result.title)
        assertEquals("line one\nline two", result.summary)
        assertNull(result.outcome)
        assertFalse(result.isEmpty)
    }

    @Test fun whitespaceOnlyFormIsEmpty() {
        assertTrue(ArcMetadata.normalize(1, " ", "\n", "\t", "", "  ").isEmpty)
    }

    @Test fun normalizedEquivalentValuesAreEqualForDirtyComparison() {
        val saved = ArcMetadata(1, title = "Morning Flow")
        val edited = ArcMetadata.normalize(1, "  Morning Flow  ", "", "", "", "")
        assertEquals(saved, edited)
    }
}
