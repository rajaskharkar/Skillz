package com.kingkharnivore.skillz.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChronicleOrderingContractTest {
    private fun valid(current: List<String>, proposed: List<String>) =
        current.size == proposed.size && current.toSet() == proposed.toSet()

    @Test fun completePermutationIsAccepted() {
        assertTrue(valid(listOf("text", "media", "voice"), listOf("voice", "text", "media")))
    }

    @Test fun staleOrForeignOrderingIsRejected() {
        assertFalse(valid(listOf("text", "media"), listOf("text")))
        assertFalse(valid(listOf("text", "media"), listOf("text", "foreign")))
    }
}
