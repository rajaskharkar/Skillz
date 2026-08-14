package com.kingkharnivore.skillz.ui.screen.chronicle

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class ChronicleSpeechStateTest {
    @Test fun tapStartsOnlyDictation() {
        val subject = MicGestureArbiter()
        assertEquals(MicGestureArbiter.Result.NONE, subject.pointerDown())
        assertEquals(MicGestureArbiter.Result.START_DICTATION, subject.pointerUp())
    }

    @Test fun holdStartsAndFinishesOnlyVoice() {
        val subject = MicGestureArbiter()
        subject.pointerDown()
        assertEquals(MicGestureArbiter.Result.START_VOICE, subject.longPressThreshold())
        assertEquals(MicGestureArbiter.Result.FINISH_VOICE, subject.pointerUp())
    }

    @Test fun partialReplacesSelectionWithoutDuplicationAndCancelIsExact() {
        val original = TextFieldValue("Today was productive.", TextRange(10, 10))
        val subject = DictationTextSession(original)
        subject.partial("very")
        assertEquals("Today was veryproductive.", subject.partial("very").text)
        assertEquals(original, subject.cancel())
    }

    @Test fun selectedTextIsReplacedByLatestPartial() {
        val subject = DictationTextSession(TextFieldValue("one old three", TextRange(4, 7)))
        assertEquals("one new three", subject.partial("new").text)
        assertEquals("one newest three", subject.partial("newest").text)
    }
}
