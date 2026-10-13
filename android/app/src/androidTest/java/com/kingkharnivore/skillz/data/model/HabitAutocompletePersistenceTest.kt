package com.kingkharnivore.skillz.data.model

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.data.model.entity.*
import com.kingkharnivore.skillz.data.repository.ChronicleRepository
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.model.state.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HabitAutocompletePersistenceTest {
    @Test fun editedOrResetHabitSelectionStaysDetachedAfterDraftRestoreAndSave() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SkillzDatabase::class.java).build()
        try {
            val ready = db.tagDao().insertTag(TagEntity(name = "ready"))
            val otherTag = db.tagDao().insertTag(TagEntity(name = "reading"))
            val habit = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title = "Brush teeth", tagId = ready))
            val chronicles = ChronicleRepository(db, db.chronicleDao())
            val flows = FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao(), chronicles)
            val selected = FlowUiState().withActivityTitle(ActivityTitleSuggestion(habit, "Brush teeth", "ready"))
            val previous = flows.addSessionAndPromoteChronicle("previous-habit", SessionEntity(
                title = selected.title, description = "", tagId = ready, originPlanId = habit,
                startTime = 1, endTime = 60_001, durationMs = 60_000
            ))
            val previousMemory = db.horizonMemoryDao().getBySessionId(previous)
            val abandoned = listOf(
                selected.withEditedTitle("Read a book"),
                selected.withEditedTagName("reading"),
                selected.withEditedTitle("Read a book").withEditedTagName("reading"),
                selected.withResetStopwatch(),
                selected.withEditedTitle("Read a book").withEditedTitle("Brush teeth"),
                selected.withEditedTagName("reading").withEditedTagName("ready")
            )
            abandoned.forEachIndexed { index, state ->
                val key = "abandoned-$index"
                db.ongoingSessionDao().upsert(OngoingSessionEntity(
                    flowInstanceId = key, title = state.title, description = "", tagName = state.tagName,
                    originPlanId = state.originPlanId, isInFlowMode = false, isRunning = false,
                    baseStartTimeMs = null, accumulatedBeforeStartMs = 60_000
                ))
                val restored = db.ongoingSessionDao().getOngoingSession().first()!!
                assertNull(restored.originPlanId)
                val id = flows.addSessionAndPromoteChronicle(key, SessionEntity(
                    title = restored.title, description = "", tagId = if (restored.tagName == "ready") ready else otherTag,
                    originPlanId = restored.originPlanId, startTime = 60_001, endTime = 120_001, durationMs = 60_000
                ))
                assertNotNull(db.sessionDao().getSessionById(id))
                assertNull(db.horizonMemoryDao().getBySessionId(id))
            }
            assertEquals(listOf(previousMemory), db.horizonMemoryDao().observeAll().first())
            assertEquals(7, db.sessionDao().getAllSessions().first().size)
        } finally {
            db.close()
        }
    }

    @Test fun autocompleteAndCompletionPreserveExistingDataAcrossDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "habit-preservation-${java.util.UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, SkillzDatabase::class.java, name)
            .addMigrations(*com.kingkharnivore.skillz.data.model.migration.SkillzDatabaseMigrations.ALL_MIGRATIONS)
            .build()
        var db = open()
        fun repository(): FlowRepository {
            val chronicles = ChronicleRepository(db, db.chronicleDao())
            return FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao(), chronicles)
        }
        try {
            val tagId = db.tagDao().insertTag(TagEntity(name = "ready"))
            val habitId = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title = "Brush teeth", tagId = tagId))
            db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title = "Archived habit", archived = true, tagId = tagId))
            db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title = "Finished plan", kind = HorizonKind.PLAN, completedAt = 42, tagId = tagId))
            val oldFlow = SessionEntity(title = "Previous brushing", description = "Keep this description", tagId = tagId,
                startTime = 1, endTime = 60_001, durationMs = 60_000, originPlanId = habitId, arcId = 8, arcIndex = 1)
            ChronicleRepository(db, db.chronicleDao()).addText("ACTIVE_FLOW", "previous", "Keep this chronicle")
            val oldId = repository().addSessionAndPromoteChronicle("previous", oldFlow)
            db.pulseDao().insertPulse(PulseEntity(title = "Previous idea", description = "Keep this idea", tagId = tagId, parentSessionId = oldId))
            db.arcMetadataDao().upsert(ArcMetadataEntity(8, "Previous arc", "Keep this summary", null, null, null, 1, 1))
            db.stillwaterLedgerDao().insert(com.kingkharnivore.skillz.data.model.entity.shell.StillwaterLedgerEntity("previous-reward", 250, "soft_flow", oldId.toString(), 1))
            val before = snapshot(db)

            db.close()
            db = open()
            assertEquals("Reopening must preserve every stored row", before, snapshot(db))
            val typed = FlowUiState(title = "BRU")
            val selected = typed.withActivityTitle(activityTitleSuggestions(
                db.flowPlanDao().getActiveFlowPlans().first(), db.tagDao().getAllTagsSnapshot(), typed
            ).single())
            assertEquals("Autocomplete must be read-only", before, snapshot(db))
            db.ongoingSessionDao().upsert(OngoingSessionEntity(
                flowInstanceId = "new-brushing", title = selected.title, description = "", tagName = selected.tagName,
                originPlanId = selected.originPlanId, isInFlowMode = false, isRunning = false,
                baseStartTimeMs = null, accumulatedBeforeStartMs = 60_000
            ))
            val newFlow = oldFlow.copy(title = selected.title, description = "", originPlanId = selected.originPlanId,
                startTime = 60_001, endTime = 120_001, arcId = null, arcIndex = null)
            val newId = repository().addSessionAndPromoteChronicle("new-brushing", newFlow)
            assertEquals(newId, repository().addSessionAndPromoteChronicle("new-brushing", newFlow))
            db.close()
            db = open()
            val after = snapshot(db)
            before.forEach { (table, rows) ->
                val remaining = after.getValue(table).toMutableList()
                rows.forEach { row -> assertTrue("Existing row changed or lost in $table", remaining.remove(row)) }
            }
            assertEquals(2, db.horizonMemoryDao().observeAll().first().size)
            assertEquals(habitId, db.ongoingSessionDao().getOngoingSession().first()!!.originPlanId)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    private fun snapshot(db: SkillzDatabase): Map<String, List<List<String?>>> {
        val sqlite = db.openHelper.readableDatabase
        val tables = sqlite.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('room_master_table','android_metadata')").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        return tables.associateWith { table ->
            sqlite.query("SELECT * FROM \"$table\"").use { cursor ->
                buildList { while (cursor.moveToNext()) add(List(cursor.columnCount) { column ->
                    when (cursor.getType(column)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> null
                        android.database.Cursor.FIELD_TYPE_BLOB -> android.util.Base64.encodeToString(cursor.getBlob(column), android.util.Base64.NO_WRAP)
                        else -> cursor.getString(column)
                    }
                }) }.sortedBy { it.toString() }
            }
        }
    }

    @Test fun selectedHabitSurvivesDraftRestoreAndRecordsStandaloneAndArcFlowsOnce() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SkillzDatabase::class.java).build()
        try {
            val tagId = db.tagDao().insertTag(TagEntity(name = "ready"))
            val habitId = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title = "Brush teeth", tagId = tagId))
            val typed = FlowUiState(title = "BRU")
            val selected = typed.withActivityTitle(activityTitleSuggestions(
                db.flowPlanDao().getActiveFlowPlans().first(), db.tagDao().getAllTagsSnapshot(), typed
            ).single())
            db.ongoingSessionDao().upsert(OngoingSessionEntity(
                flowInstanceId = "brush", title = selected.title, description = "", tagName = selected.tagName,
                originPlanId = selected.originPlanId, isInFlowMode = false, isRunning = false,
                baseStartTimeMs = null, accumulatedBeforeStartMs = 0
            ))
            val restored = db.ongoingSessionDao().getOngoingSession().first()!!
            assertEquals("Brush teeth", restored.title)
            assertEquals("ready", restored.tagName)
            assertEquals(habitId, restored.originPlanId)
            assertTrue(db.horizonMemoryDao().observeAll().first().isEmpty())

            val chronicles = ChronicleRepository(db, db.chronicleDao())
            val flows = FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao(), chronicles)
            // Both FlowViewModel save branches use this transaction with the selected originPlanId.
            for ((index, mode) in FlowMode.entries.withIndex()) {
                val session = SessionEntity(
                    title = restored.title, description = "", tagId = tagId,
                    originPlanId = restored.originPlanId, startTime = 1, endTime = 60_001,
                    durationMs = 60_000, mode = mode,
                    arcId = if (index == 0) null else 9L,
                    arcIndex = if (index == 0) null else index
                )
                val key = "brush-$mode"
                val id = flows.addSessionAndPromoteChronicle(key, session)
                assertEquals(id, flows.addSessionAndPromoteChronicle(key, session))
                val memory = db.horizonMemoryDao().getByFlowInstanceId(key)!!
                assertEquals(habitId, memory.sourcePlanId)
                assertEquals("ready", memory.tagName)
                assertEquals(session.arcId, memory.arcId)
            }
            assertEquals(FlowMode.entries.size, db.horizonMemoryDao().observeAll().first().size)
            assertNull(db.flowPlanDao().getFlowPlanById(habitId)!!.completedAt)
        } finally {
            db.close()
        }
    }
}
