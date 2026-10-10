package com.kingkharnivore.skillz.domain.power

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.utils.shell.*
import com.kingkharnivore.skillz.utils.user.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.UUID

class ChestPreferencesTest {
    @Test fun legacyFilterAndUnrelatedPreferencesSurviveEditsAndRestart() = runBlocking {
        val file=File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"chest-${UUID.randomUUID()}.preferences_pb")
        var job=SupervisorJob()
        fun open()=PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job), produceFile={file})
        var store=open()
        try {
            store.edit { it[UserPrefs.KEY_CHEST_FILTER]="jurassic"; it[UserPrefs.KEY_APP_LANGUAGE_TAG]="mr" }
            store.edit { updateChestFilterPreferences(it) { filters -> filters.withProgress(ChestFilterOption.NotMastered) } }
            job.cancelAndJoin(); job=SupervisorJob(); store=open()
            val restored=store.data.first()
            assertEquals("jurassic",restored[UserPrefs.KEY_CHEST_ENVIRONMENT])
            assertEquals("not_mastered",restored[UserPrefs.KEY_CHEST_FILTER])
            assertEquals("mr",restored[UserPrefs.KEY_APP_LANGUAGE_TAG])
            coroutineScope {
                awaitAll(async { store.edit { updateChestFilterPreferences(it) { filters -> filters.withEnvironment(ChestFilterOption.Pasture) } } },
                    async { store.edit { updateChestFilterPreferences(it) { filters -> filters.withProgress(ChestFilterOption.Mastered) } } })
            }
            assertEquals("pasture",store.data.first()[UserPrefs.KEY_CHEST_ENVIRONMENT])
            assertEquals("mastered",store.data.first()[UserPrefs.KEY_CHEST_FILTER])
            store.edit { updateChestFilterPreferences(it) { ChestFilters() } }
            assertEquals("all",store.data.first()[UserPrefs.KEY_CHEST_ENVIRONMENT])
            assertEquals("all",store.data.first()[UserPrefs.KEY_CHEST_FILTER])
            assertEquals("mr",store.data.first()[UserPrefs.KEY_APP_LANGUAGE_TAG])
        } finally { job.cancelAndJoin(); file.delete() }
    }
}
