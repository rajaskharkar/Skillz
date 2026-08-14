package com.kingkharnivore.skillz.ui.screen.chronicle

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Platform-independent arbitration for the composer's single Mic gesture. */
internal class MicGestureArbiter {
    enum class Result { NONE, START_DICTATION, START_VOICE, FINISH_VOICE }
    private enum class State { IDLE, PENDING, RECORDING }
    private var state = State.IDLE

    fun pointerDown(): Result {
        if (state == State.IDLE) state = State.PENDING
        return Result.NONE
    }

    fun longPressThreshold(): Result = if (state == State.PENDING) {
        state = State.RECORDING
        Result.START_VOICE
    } else Result.NONE

    fun pointerUp(): Result = when (state) {
        State.PENDING -> Result.START_DICTATION.also { state = State.IDLE }
        State.RECORDING -> Result.FINISH_VOICE.also { state = State.IDLE }
        State.IDLE -> Result.NONE
    }

    fun cancel(): Result {
        state = State.IDLE
        return Result.NONE
    }
}

/** Exact, selection-aware provisional text model. No hypothesis is ever appended twice. */
internal class DictationTextSession(val original: TextFieldValue) {
    private val start = original.selection.min.coerceIn(0, original.text.length)
    private val end = original.selection.max.coerceIn(start, original.text.length)
    private val prefix = original.text.substring(0, start)
    private val suffix = original.text.substring(end)
    private var active = true
    private var hypothesis = ""

    fun partial(value: String): TextFieldValue {
        if (!active) return current()
        hypothesis = value
        return current()
    }

    fun finish(): TextFieldValue {
        active = false
        return current()
    }

    fun cancel(): TextFieldValue {
        active = false
        hypothesis = ""
        return original
    }

    private fun current(): TextFieldValue {
        val text = prefix + hypothesis + suffix
        val cursor = (prefix.length + hypothesis.length).coerceAtMost(text.length)
        return TextFieldValue(text, TextRange(cursor))
    }
}
