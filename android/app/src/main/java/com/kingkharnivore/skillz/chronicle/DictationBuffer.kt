package com.kingkharnivore.skillz.chronicle

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Pure state machine which makes evolving recognition hypotheses replaceable and cancel-safe. */
class DictationBuffer(private val original: TextFieldValue) {
    private val insertion = original.selection.min
    private var finalSpeech = ""
    private var partialSpeech = ""

    fun partial(text: String): TextFieldValue {
        partialSpeech = text
        return rendered()
    }

    fun final(text: String): TextFieldValue {
        finalSpeech = joinSpeech(finalSpeech, text)
        partialSpeech = ""
        return rendered()
    }

    fun finish(): TextFieldValue = rendered()

    fun cancel(): TextFieldValue = original

    private fun rendered(): TextFieldValue {
        val speech = joinSpeech(finalSpeech, partialSpeech)
        val before = original.text.substring(0, insertion)
        val after = original.text.substring(original.selection.max)
        val prefix = if (speech.isNotBlank() && before.lastOrNull()?.isWhitespace() == false) " " else ""
        val suffix = if (speech.isNotBlank() && after.firstOrNull()?.isWhitespace() == false) " " else ""
        val inserted = prefix + speech + suffix
        return TextFieldValue(
            text = before + inserted + after,
            selection = TextRange(insertion + inserted.length)
        )
    }

    private fun joinSpeech(left: String, right: String): String =
        listOf(left.trim(), right.trim()).filter(String::isNotEmpty).joinToString(" ")
}

enum class MicrophoneGesture { Dictation, VoiceNote }

fun classifyMicrophoneGesture(heldMillis: Long, longPressMillis: Long): MicrophoneGesture =
    if (heldMillis >= longPressMillis) MicrophoneGesture.VoiceNote else MicrophoneGesture.Dictation
