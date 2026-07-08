package com.kingkharnivore.skillz.data.repository.shell

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kingkharnivore.skillz.utils.shell.ChestSortOption
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals

class ChestSortPreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun defaultSortIsLevelWhenNoSavedPreferenceExists() = runBlocking {
        val repository = repositoryFor(File(temporaryFolder.newFolder(), "empty.preferences_pb"))

        assertEquals(ChestSortOption.Level, repository.selectedSortOption.first())
    }

    @Test
    fun savedSortPreferenceIsLoaded() = runBlocking {
        val file = File(temporaryFolder.newFolder(), "saved.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create { file }
        dataStore.edit { preferences -> preferences[stringPreferencesKey("chest_sort_option")] = "oldest_arrival" }
        val repository = ChestSortPreferencesRepository(dataStore)

        assertEquals(ChestSortOption.OldestArrival, repository.selectedSortOption.first())
    }

    @Test
    fun changingSortOptionSavesNewPreference() = runBlocking {
        val repository = repositoryFor(File(temporaryFolder.newFolder(), "changed.preferences_pb"))

        repository.setSelectedSortOption(ChestSortOption.Recent)

        assertEquals(ChestSortOption.Recent, repository.selectedSortOption.first())
    }

    @Test
    fun invalidSavedSortPreferenceFallsBackToLevel() = runBlocking {
        val file = File(temporaryFolder.newFolder(), "invalid.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create { file }
        dataStore.edit { preferences -> preferences[stringPreferencesKey("chest_sort_option")] = "old_value" }
        val repository = ChestSortPreferencesRepository(dataStore)

        assertEquals(ChestSortOption.Level, repository.selectedSortOption.first())
    }

    private fun repositoryFor(file: File): ChestSortPreferencesRepository =
        ChestSortPreferencesRepository(PreferenceDataStoreFactory.create { file })
}
