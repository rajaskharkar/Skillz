package com.kingkharnivore.skillz.chronicle

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class DictationBufferTest {
    @Test fun evolvingPartialReplacesRatherThanDuplicates() {
        val buffer = DictationBuffer(TextFieldValue("Before after", TextRange(7)))
        buffer.partial("hello")
        assertEquals("Before hello world after", buffer.partial("hello world").text)
    }

    @Test fun cancelRestoresExactTextAndSelection() {
        val original = TextFieldValue("Before after", TextRange(3, 7))
        val buffer = DictationBuffer(original)
        buffer.partial("temporary words")
        assertEquals(original, buffer.cancel())
    }

    @Test fun finalSpeechRemainsEditableAndKeepsSurroundingText() {
        val buffer = DictationBuffer(TextFieldValue("One three", TextRange(4)))
        assertEquals("One two three", buffer.final("two").text)
    }

    @Test fun tapAndHoldAreMutuallyExclusive() {
        assertEquals(MicrophoneGesture.Dictation, classifyMicrophoneGesture(499, 500))
        assertEquals(MicrophoneGesture.VoiceNote, classifyMicrophoneGesture(500, 500))
    }
}
