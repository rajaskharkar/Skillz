package com.kingkharnivore.skillz.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotepadRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private companion object {
        val NOTEPAD_TEXT = stringPreferencesKey("notepad_text")
        val NOTEPAD_DOC_FONT = intPreferencesKey("notepad_doc_font") // 0 default, 1 cursive, 2 mono
    }

    /**
     * Default doc is pure HTML with inline font families.
     * Avoid runtime "if contains" injection in NotepadScreen (it can desync meta indices).
     */
    val DEFAULT_WELCOME_HTML: String =
        """
<h2><span style="font-family: monospace;">SkratchPad</span></h2>

<br/>

<h1><span style="font-family: cursive;">Hi! Welcome to Scyra!</span></h1>

<br/>

<p>
This is your SkratchPad — a place to plan your next Flow,
capture your thoughts, and record the progress you earn.
</p>

<br/>

<p>
Sketch what’s ahead.
Reflect on what’s done.
</p>

<br/>

<p>
Write freely.
Design your focus.
Build your momentum.
This is your time.
</p>
""".trimIndent()

    val notepadTextFlow: Flow<String> =
        dataStore.data
            .map { prefs ->
                if (prefs.contains(NOTEPAD_TEXT)) prefs[NOTEPAD_TEXT] ?: ""
                else DEFAULT_WELCOME_HTML
            }
            .distinctUntilChanged()

    val notepadDocFontFlow: Flow<Int> =
        dataStore.data
            .map { prefs -> prefs[NOTEPAD_DOC_FONT] ?: 0 }
            .distinctUntilChanged()

    suspend fun saveNotepadText(text: String) {
        dataStore.edit { prefs -> prefs[NOTEPAD_TEXT] = text }
    }

    suspend fun saveNotepadDocFont(font: Int) {
        dataStore.edit { prefs -> prefs[NOTEPAD_DOC_FONT] = font.coerceIn(0, 2) }
    }
}