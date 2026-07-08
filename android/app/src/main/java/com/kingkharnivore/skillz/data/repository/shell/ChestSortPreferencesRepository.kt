package com.kingkharnivore.skillz.data.repository.shell

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kingkharnivore.skillz.utils.shell.ChestSortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChestSortPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val chestSortOptionKey = stringPreferencesKey("chest_sort_option")

    val selectedSortOption: Flow<ChestSortOption> = dataStore.data.map { preferences ->
        ChestSortOption.fromKey(preferences[chestSortOptionKey])
    }

    suspend fun setSelectedSortOption(option: ChestSortOption) {
        dataStore.edit { preferences ->
            preferences[chestSortOptionKey] = option.key
        }
    }
}
