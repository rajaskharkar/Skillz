package com.kingkharnivore.skillz.utils.arc

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import kotlinx.coroutines.flow.first

class ArcPrefs(private val ds: DataStore<Preferences>) {

    private val K_ARC_ID = longPreferencesKey("arc_id")
    private val K_PENDING = booleanPreferencesKey("arc_pending")
    private val K_MULT = doublePreferencesKey("arc_mult")
    private val K_PROGRESS = longPreferencesKey("arc_progress")
    private val K_LAST_END = longPreferencesKey("arc_last_end")
    private val K_COUNT = intPreferencesKey("arc_count")

    suspend fun load(): ArcRuntimeState? {
        val p = ds.data.first()
        val id = p[K_ARC_ID] ?: return null
        return ArcRuntimeState(
            arcId = id,
            isPending = p[K_PENDING] ?: true,
            multiplier = p[K_MULT] ?: ArcRules.START_MULTIPLIER,
            progressMs = 0L, // ✅ strict arcs: no carry/banking
            lastSessionEndTimeMs = p[K_LAST_END] ?: 0L,
            sessionCountInArc = p[K_COUNT] ?: 0
        )
    }

    suspend fun save(state: ArcRuntimeState) {
        ds.edit { p ->
            p[K_ARC_ID] = state.arcId
            p[K_PENDING] = state.isPending
            p[K_MULT] = state.multiplier
            p[K_PROGRESS] = state.progressMs
            p[K_LAST_END] = state.lastSessionEndTimeMs
            p[K_COUNT] = state.sessionCountInArc
        }
    }

    suspend fun clear() {
        ds.edit { p ->
            p.remove(K_ARC_ID)
            p.remove(K_PENDING)
            p.remove(K_MULT)
            p.remove(K_PROGRESS)
            p.remove(K_LAST_END)
            p.remove(K_COUNT)
        }
    }
}