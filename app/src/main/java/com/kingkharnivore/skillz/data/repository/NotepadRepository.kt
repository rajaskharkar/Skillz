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

    // ✅ Brand-y default only if key is missing (fresh install / never saved)
    private val DEFAULT_WELCOME_HTML: String =
        """
        <p><b>Hi! Welcome to Scyra.</b> ✨</p>
        <p>This is your <b>Flow Log</b> — a place to capture thoughts, intentions, and tiny wins while you Flow.</p>
        <p>Start anywhere. Write freely. Scyra will remember.</p>
        """.trimIndent()

    val notepadTextFlow: Flow<String> =
        dataStore.data
            .map { prefs ->
                // If key missing -> default welcome text
                // If user saved empty string -> keep empty string
                if (prefs.contains(NOTEPAD_TEXT)) prefs[NOTEPAD_TEXT] ?: ""
                else DEFAULT_WELCOME_HTML
            }
            .distinctUntilChanged()

    val notepadDocFontFlow: Flow<Int> =
        dataStore.data
            .map { prefs -> prefs[NOTEPAD_DOC_FONT] ?: 0 }
            .distinctUntilChanged()

    suspend fun saveNotepadText(text: String) {
        dataStore.edit { prefs ->
            prefs[NOTEPAD_TEXT] = text
        }
    }

    suspend fun saveNotepadDocFont(font: Int) {
        dataStore.edit { prefs ->
            prefs[NOTEPAD_DOC_FONT] = font.coerceIn(0, 2)
        }
    }
}