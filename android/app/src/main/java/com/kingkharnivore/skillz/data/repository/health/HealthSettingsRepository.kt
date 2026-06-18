package com.kingkharnivore.skillz.data.repository.health

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class HealthSettings(val movementBonusEnabled: Boolean)

@Singleton
class HealthSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val movementBonusEnabledKey = booleanPreferencesKey("movement_bonus_enabled")

    val settings: Flow<HealthSettings> = dataStore.data.map { prefs ->
        HealthSettings(movementBonusEnabled = prefs[movementBonusEnabledKey] ?: false)
    }

    suspend fun setMovementBonusEnabled(enabled: Boolean) {
        dataStore.edit { it[movementBonusEnabledKey] = enabled }
    }
}
