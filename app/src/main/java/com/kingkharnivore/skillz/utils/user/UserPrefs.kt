package com.kingkharnivore.skillz.utils.user

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ✅ This creates `context.userPrefsDataStore`
private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_SHOW_SCORE_UI = booleanPreferencesKey("show_score_ui") // default false
        val KEY_CALM_MODE = booleanPreferencesKey("calm_mode")         // default false
    }

    val showScoreUi: Flow<Boolean> =
        context.userPrefsDataStore.data.map { prefs ->
            prefs[KEY_SHOW_SCORE_UI] ?: false
        }

    val calmMode: Flow<Boolean> =
        context.userPrefsDataStore.data.map { prefs ->
            prefs[KEY_CALM_MODE] ?: false
        }

    suspend fun setShowScoreUi(enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[KEY_SHOW_SCORE_UI] = enabled
        }
    }

    suspend fun setCalmMode(enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[KEY_CALM_MODE] = enabled
        }
    }
}