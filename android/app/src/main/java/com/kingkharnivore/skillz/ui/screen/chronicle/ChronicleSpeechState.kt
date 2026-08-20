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
    private var terminal: TextFieldValue? = null

    fun partial(value: String): TextFieldValue {
        if (!active) return terminal ?: original
        hypothesis = value
        return current()
    }

    fun finish(): TextFieldValue {
        val value = current()
        active = false
        terminal = value
        return value
    }

    fun cancel(): TextFieldValue {
        active = false
        hypothesis = ""
        terminal = original
        return original
    }

    private fun current(): TextFieldValue {
        val before = if (hypothesis.isNotBlank() && prefix.lastOrNull()?.isLetterOrDigit() == true &&
            hypothesis.firstOrNull()?.isLetterOrDigit() == true) " " else ""
        val after = if (hypothesis.isNotBlank() && suffix.firstOrNull()?.isLetterOrDigit() == true &&
            hypothesis.lastOrNull()?.isLetterOrDigit() == true) " " else ""
        val text = prefix + before + hypothesis + after + suffix
        val cursor = (prefix.length + before.length + hypothesis.length).coerceAtMost(text.length)
        return TextFieldValue(text, TextRange(cursor))
    }
}
