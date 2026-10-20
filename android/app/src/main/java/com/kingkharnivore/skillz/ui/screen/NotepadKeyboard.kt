package com.kingkharnivore.skillz.ui.screen

import android.os.Build
import android.text.TextUtils
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.SurroundingText
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.core.view.inputmethod.EditorInfoCompat
import com.mohamedrejeb.richeditor.model.RichTextState

/** The rich editor uses spaces between styled paragraphs. Expose those as newlines to the IME
 * so its normal sentence capitalization works, without changing text or selection offsets. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NotepadKeyboard(state: RichTextState, content: @Composable () -> Unit) {
    val interceptor = remember(state) {
        PlatformTextInputInterceptor { request, next ->
            next.startInputMethod(PlatformTextInputMethodRequest { info ->
                val connection = NotepadInputConnection(request.createInputConnection(info), state)
                EditorInfoCompat.setInitialSurroundingText(info, state.keyboardText())
                info.initialCapsMode = connection.getCursorCapsMode(info.inputType)
                connection
            })
        }
    }
    InterceptPlatformTextInput(interceptor, content)
}

private fun RichTextState.keyboardText(): String {
    val text = annotatedString.text.toCharArray()
    annotatedString.paragraphStyles.forEach { paragraph ->
        val separator = paragraph.start - 1
        if (separator >= 0 && text.getOrNull(separator) == ' ') text[separator] = '\n'
    }
    return text.concatToString()
}

internal class NotepadInputConnection(
    target: InputConnection,
    private val state: RichTextState,
) : InputConnectionWrapper(target, false) {
    private fun withLineBreaks(text: CharSequence?, offset: Int): CharSequence? {
        if (text == null) return null
        // During an IME batch the editor can briefly lag behind the input connection.
        // Only map offsets when both represent the same text.
        val source = state.annotatedString.text
        if (offset < 0 || !source.regionMatches(offset, text.toString(), 0, text.length)) return text
        return state.keyboardText().subSequence(offset, offset + text.length)
    }

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        val text = super.getTextBeforeCursor(n, flags)
        return withLineBreaks(text, state.selection.min - (text?.length ?: 0))
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? =
        withLineBreaks(super.getTextAfterCursor(n, flags), state.selection.max)

    override fun getSelectedText(flags: Int): CharSequence? =
        withLineBreaks(super.getSelectedText(flags), state.selection.min)

    override fun getCursorCapsMode(reqModes: Int): Int {
        val before = getTextBeforeCursor(state.annotatedString.length, 0)
            ?: return super.getCursorCapsMode(reqModes)
        return TextUtils.getCapsMode(before, before.length, reqModes)
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        val extracted = super.getExtractedText(request, flags) ?: return null
        return ExtractedText().apply {
            text = withLineBreaks(extracted.text, extracted.startOffset + extracted.partialStartOffset.coerceAtLeast(0))
            startOffset = extracted.startOffset
            partialStartOffset = extracted.partialStartOffset
            partialEndOffset = extracted.partialEndOffset
            selectionStart = extracted.selectionStart
            selectionEnd = extracted.selectionEnd
            this.flags = extracted.flags
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getSurroundingText(beforeLength: Int, afterLength: Int, flags: Int): SurroundingText? {
        val surrounding = super.getSurroundingText(beforeLength, afterLength, flags) ?: return null
        val offset = if (surrounding.offset >= 0) surrounding.offset
            else state.selection.min - surrounding.selectionStart
        return SurroundingText(
            withLineBreaks(surrounding.text, offset) ?: surrounding.text,
            surrounding.selectionStart,
            surrounding.selectionEnd,
            surrounding.offset,
        )
    }
}
