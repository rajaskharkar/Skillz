package com.kingkharnivore.skillz.data.repository.anchor

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kingkharnivore.skillz.data.model.dao.anchor.AnchoredAppDao
import com.kingkharnivore.skillz.data.model.entity.anchor.AnchoredAppEntity
import com.kingkharnivore.skillz.domain.anchor.AnchorSettings
import com.kingkharnivore.skillz.domain.anchor.AnchorableApp
import com.kingkharnivore.skillz.domain.anchor.AnchoredApp
import com.kingkharnivore.skillz.domain.anchor.NeverAnchorPolicy
import com.kingkharnivore.skillz.domain.anchor.PhoneDownMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AnchorRepository {
    val settings: Flow<AnchorSettings>
    val anchoredApps: Flow<List<AnchoredApp>>

    suspend fun setEnabled(enabled: Boolean)
    suspend fun addAnchoredApp(app: AnchorableApp)
    suspend fun removeAnchoredApp(packageName: String)
    suspend fun getAnchoredPackageSet(): Set<String>
}

@Singleton
class DefaultAnchorRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val anchoredAppDao: AnchoredAppDao,
    private val neverAnchorPolicy: NeverAnchorPolicy
) : AnchorRepository {
    private object Keys {
        val ENABLED = booleanPreferencesKey("anchor_enabled")
        val PHONE_DOWN_MODE = stringPreferencesKey("anchor_phone_down_mode")
    }

    override val settings: Flow<AnchorSettings> = dataStore.data.map { prefs ->
        AnchorSettings(
            enabled = prefs[Keys.ENABLED] ?: false,
            phoneDownMode = runCatching {
                PhoneDownMode.valueOf(prefs[Keys.PHONE_DOWN_MODE] ?: PhoneDownMode.OFF.name)
            }.getOrDefault(PhoneDownMode.OFF)
        )
    }

    override val anchoredApps: Flow<List<AnchoredApp>> =
        anchoredAppDao.observeAnchoredApps().map { apps -> apps.map { it.toDomain() } }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ENABLED] = enabled }
    }

    override suspend fun addAnchoredApp(app: AnchorableApp) {
        val pkg = app.packageName.trim()
        if (pkg.isBlank() || neverAnchorPolicy.isNeverAnchored(pkg)) return
        anchoredAppDao.upsert(
            AnchoredAppEntity(
                packageName = pkg,
                displayName = app.displayName.ifBlank { pkg },
                iconCacheKey = app.iconCacheKey,
                addedAt = System.currentTimeMillis(),
                lastSeenAt = app.lastUsedAt
            )
        )
    }

    override suspend fun removeAnchoredApp(packageName: String) {
        anchoredAppDao.remove(packageName)
    }

    override suspend fun getAnchoredPackageSet(): Set<String> =
        anchoredAppDao.getAnchoredApps()
            .map { it.packageName }
            .filterNot { neverAnchorPolicy.isNeverAnchored(it) }
            .toSet()
}

private fun AnchoredAppEntity.toDomain(): AnchoredApp = AnchoredApp(
    packageName = packageName,
    displayName = displayName,
    iconCacheKey = iconCacheKey,
    addedAt = addedAt,
    lastSeenAt = lastSeenAt
)
