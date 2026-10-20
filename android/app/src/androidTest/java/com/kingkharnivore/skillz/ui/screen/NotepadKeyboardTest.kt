package com.kingkharnivore.skillz.ui.screen

import android.os.Build
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.core.view.inputmethod.EditorInfoCompat
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class)
class NotepadKeyboardTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var request: PlatformTextInputMethodRequest

    private fun openNotepad() {
        compose.setContent {
            val interceptor = remember {
                PlatformTextInputInterceptor { input, next ->
                    request = input
                    next.startInputMethod(input)
                }
            }
            InterceptPlatformTextInput(interceptor) {
                SkillzTheme(dynamicColor = false) {
                    NotepadScreen(text = "<p>first line</p>", onTextChange = {})
                }
            }
        }
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performTextInputSelection(TextRange(10))
        compose.waitUntil(5000) { ::request.isInitialized }
    }

    private fun withConnection(check: (InputConnection, EditorInfo) -> Unit) {
        compose.runOnIdle {
            val info = EditorInfo()
            check(request.createInputConnection(info), info)
        }
    }

    @Test fun enterWithoutPunctuationOffersCapitalizationOnTheNextLine() {
        openNotepad()
        compose.onNode(hasSetTextAction()).performTextInput("\n")
        withConnection { connection, info ->
            assertEquals("first line\n", connection.getTextBeforeCursor(100, 0).toString())
            assertNotEquals(0, connection.getCursorCapsMode(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES))
            assertNotEquals(0, info.inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
            assertNotEquals(0, info.initialCapsMode)
            assertEquals("first line\n", EditorInfoCompat.getInitialTextBeforeCursor(info, 100, 0).toString())
            assertEquals("first line\n", connection.getExtractedText(ExtractedTextRequest(), 0)?.text.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                assertEquals("first line\n", connection.getSurroundingText(100, 100, 0)?.text.toString())
            }
        }
    }

    @Test fun blankAndIndentedLinesOfferCapitalsButMiddleOfWordsDoesNot() {
        openNotepad()
        compose.onNode(hasSetTextAction()).performTextInput("\n\n  ")
        withConnection { connection, _ ->
            assertEquals("first line\n\n  ", connection.getTextBeforeCursor(100, 0).toString())
            assertNotEquals(0, connection.getCursorCapsMode(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES))
        }
        compose.onNode(hasSetTextAction()).performTextInputSelection(TextRange(8))
        withConnection { connection, _ ->
            assertEquals(0, connection.getCursorCapsMode(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES))
        }
    }

    @Test fun lowercaseInputIsRespectedAndCursorStaysAtTheEdit() {
        openNotepad()
        compose.onNode(hasSetTextAction()).performTextInputSelection(TextRange(5))
        compose.onNode(hasSetTextAction()).performTextInput("\n")
        withConnection { connection, _ ->
            assertNotEquals(0, connection.getCursorCapsMode(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES))
            // The user can turn off Shift: input must be delegated without forcing uppercase.
            connection.commitText("lowercase", 1)
        }
        compose.onNode(hasSetTextAction()).assertTextEquals("first lowercase line")
        withConnection { connection, _ ->
            assertEquals("first\nlowercase", connection.getTextBeforeCursor(100, 0).toString())
            assertEquals(" line", connection.getTextAfterCursor(100, 0).toString())
        }
    }
}
