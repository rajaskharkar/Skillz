package com.kingkharnivore.skillz.utils.arc

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import kotlinx.coroutines.flow.first

class ArcPrefs(private val ds: DataStore<Preferences>) {

    private val K_ARC_ID = longPreferencesKey("arc_id")
    private val K_PENDING = booleanPreferencesKey("arc_pending")
    private val K_MULT = doublePreferencesKey("arc_mult")
    private val K_PROGRESS = longPreferencesKey("arc_progress")
    private val K_LAST_END = longPreferencesKey("arc_last_end")
    private val K_COUNT = intPreferencesKey("arc_count")

    private val K_RECENT_ARC_ID = longPreferencesKey("recent_arc_id")
    private val K_RECENT_PENDING = booleanPreferencesKey("recent_arc_pending")
    private val K_RECENT_MULT = doublePreferencesKey("recent_arc_mult")
    private val K_RECENT_PROGRESS = longPreferencesKey("recent_arc_progress")
    private val K_RECENT_LAST_END = longPreferencesKey("recent_arc_last_end")
    private val K_RECENT_COUNT = intPreferencesKey("recent_arc_count")
    private val K_RECENT_COMPLETED_AT = longPreferencesKey("recent_arc_completed_at")

    suspend fun load(): ArcRuntimeState? {
        val p = ds.data.first()
        val id = p[K_ARC_ID] ?: return null

        return ArcRuntimeState(
            arcId = id,
            isPending = p[K_PENDING] ?: true,
            multiplier = p[K_MULT] ?: ArcRules.START_MULTIPLIER,
            progressMs = 0L, // strict arcs: no carry/banking
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

    /**
     * Clears only the currently active Arc.
     *
     * Important:
     * This intentionally does NOT clear the recently-ended Arc snapshot.
     * That snapshot is what allows Scyra to recover momentum when the user
     * starts another Flow within the grace window.
     */
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

    suspend fun saveRecentlyEnded(
        state: ArcRuntimeState,
        completedAtMs: Long = System.currentTimeMillis()
    ) {
        ds.edit { p ->
            p[K_RECENT_ARC_ID] = state.arcId
            p[K_RECENT_PENDING] = state.isPending
            p[K_RECENT_MULT] = state.multiplier
            p[K_RECENT_PROGRESS] = state.progressMs
            p[K_RECENT_LAST_END] = state.lastSessionEndTimeMs
            p[K_RECENT_COUNT] = state.sessionCountInArc
            p[K_RECENT_COMPLETED_AT] = completedAtMs
        }
    }

    suspend fun loadRecentlyEnded(): ArcRuntimeState? {
        val p = ds.data.first()
        val id = p[K_RECENT_ARC_ID] ?: return null

        return ArcRuntimeState(
            arcId = id,
            isPending = p[K_RECENT_PENDING] ?: true,
            multiplier = p[K_RECENT_MULT] ?: ArcRules.START_MULTIPLIER,
            progressMs = 0L, // strict arcs: no carry/banking
            lastSessionEndTimeMs = p[K_RECENT_LAST_END] ?: p[K_RECENT_COMPLETED_AT] ?: 0L,
            sessionCountInArc = p[K_RECENT_COUNT] ?: 0
        )
    }

    suspend fun clearRecentlyEnded() {
        ds.edit { p ->
            p.remove(K_RECENT_ARC_ID)
            p.remove(K_RECENT_PENDING)
            p.remove(K_RECENT_MULT)
            p.remove(K_RECENT_PROGRESS)
            p.remove(K_RECENT_LAST_END)
            p.remove(K_RECENT_COUNT)
            p.remove(K_RECENT_COMPLETED_AT)
        }
    }
}