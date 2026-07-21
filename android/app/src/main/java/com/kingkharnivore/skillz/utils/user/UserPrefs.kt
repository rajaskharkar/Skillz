package com.kingkharnivore.skillz.utils.user

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.utils.shell.ChestSortOption
import com.kingkharnivore.skillz.utils.shell.ChestFilterOption
import com.kingkharnivore.skillz.domain.achievement.BadgeSort
import com.kingkharnivore.skillz.domain.achievement.BadgeUiCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_SHOW_SCORE_UI = booleanPreferencesKey("show_score_ui")
        val KEY_CALM_MODE = booleanPreferencesKey("calm_mode")
        val KEY_APP_LANGUAGE_TAG = stringPreferencesKey("app_language_tag")
        val KEY_CHEST_SORT_OPTION = stringPreferencesKey("chest_sort_option")
        val KEY_BADGE_CATEGORY = stringPreferencesKey("badge_category")
        val KEY_BADGE_SORT = stringPreferencesKey("badge_sort")
        val KEY_BACKFILL_ACKNOWLEDGED = intPreferencesKey("achievement_backfill_acknowledged")
        val KEY_CHEST_FILTER = stringPreferencesKey("chest_filter")
    }

    val showScoreUi: Flow<Boolean> =
        context.userPrefsDataStore.data.map { prefs ->
            prefs[KEY_SHOW_SCORE_UI] ?: BuildConfig.SHOW_SCORE
        }

    val calmMode: Flow<Boolean> =
        context.userPrefsDataStore.data.map { prefs ->
            prefs[KEY_CALM_MODE] ?: false
        }

    val appLanguageTag: Flow<String?> =
        context.userPrefsDataStore.data.map { prefs ->
            prefs[KEY_APP_LANGUAGE_TAG]
        }

    val chestSortOption: Flow<ChestSortOption> =
        context.userPrefsDataStore.data.map { prefs ->
            ChestSortOption.fromKey(prefs[KEY_CHEST_SORT_OPTION])
        }
    val badgeCategory: Flow<BadgeUiCategory> = context.userPrefsDataStore.data.map { prefs ->
        prefs[KEY_BADGE_CATEGORY]?.let { runCatching { BadgeUiCategory.valueOf(it) }.getOrNull() } ?: BadgeUiCategory.ALL
    }
    val badgeSort: Flow<BadgeSort> = context.userPrefsDataStore.data.map { prefs ->
        prefs[KEY_BADGE_SORT]?.let { runCatching { BadgeSort.valueOf(it) }.getOrNull() } ?: BadgeSort.RECOMMENDED
    }
    val acknowledgedBackfillVersion: Flow<Int> = context.userPrefsDataStore.data.map { it[KEY_BACKFILL_ACKNOWLEDGED] ?: 0 }
    val chestFilter: Flow<ChestFilterOption> = context.userPrefsDataStore.data.map { ChestFilterOption.fromKey(it[KEY_CHEST_FILTER]) }

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

    suspend fun setAppLanguageTag(tag: String?) {
        context.userPrefsDataStore.edit { prefs ->
            if (tag == null) {
                prefs.remove(KEY_APP_LANGUAGE_TAG)
            } else {
                prefs[KEY_APP_LANGUAGE_TAG] = tag
            }
        }
    }

    suspend fun setChestSortOption(option: ChestSortOption) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[KEY_CHEST_SORT_OPTION] = option.key
        }
    }
    suspend fun setBadgeCategory(value: BadgeUiCategory) { context.userPrefsDataStore.edit { it[KEY_BADGE_CATEGORY] = value.name } }
    suspend fun setBadgeSort(value: BadgeSort) { context.userPrefsDataStore.edit { it[KEY_BADGE_SORT] = value.name } }
    suspend fun acknowledgeBackfill(version: Int) { context.userPrefsDataStore.edit { it[KEY_BACKFILL_ACKNOWLEDGED] = version } }
    suspend fun setChestFilter(value: ChestFilterOption) { context.userPrefsDataStore.edit { it[KEY_CHEST_FILTER] = value.key } }
}
