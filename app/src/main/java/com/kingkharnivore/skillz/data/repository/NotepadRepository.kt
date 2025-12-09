package com.kingkharnivore.skillz.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotepadRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private companion object {
        val NOTEPAD_TEXT = stringPreferencesKey("notepad_text")
    }

    val notepadTextFlow: Flow<String> =
        dataStore.data.map { prefs -> prefs[NOTEPAD_TEXT] ?: "" }

    suspend fun saveNotepadText(text: String) {
        dataStore.edit { prefs ->
            prefs[NOTEPAD_TEXT] = text
        }
    }
}

