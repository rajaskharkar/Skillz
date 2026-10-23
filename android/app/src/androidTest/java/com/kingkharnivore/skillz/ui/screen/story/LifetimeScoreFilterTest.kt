package com.kingkharnivore.skillz.ui.screen.story

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.data.health.HealthConnectClientProvider
import com.kingkharnivore.skillz.data.health.HealthConnectMovementDataSource
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.repository.*
import com.kingkharnivore.skillz.data.repository.health.*
import com.kingkharnivore.skillz.utils.health.HealthRefreshUseCase
import com.kingkharnivore.skillz.utils.health.MovementBonusCalculator
import com.kingkharnivore.skillz.utils.time.StoryPeriod
import com.kingkharnivore.skillz.utils.time.TimeWindowUtils
import com.kingkharnivore.skillz.utils.user.UserPrefs
import com.kingkharnivore.skillz.viewmodel.StoryViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LifetimeScoreFilterTest {
    @Test fun scoreFollowsSingleAndMultipleTagsAndResetsWhenClearedAcrossPeriods() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SkillzDatabase::class.java).build()
        var model: StoryViewModel? = null
        try {
            val art = db.tagDao().insertTag(TagEntity(name = "Art"))
            val music = db.tagDao().insertTag(TagEntity(name = "Music"))
            val reading = db.tagDao().insertTag(TagEntity(name = "Reading"))
            val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val old = LocalDate.now().minusYears(2).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            suspend fun save(tag: Long, points: Int, surge: Int, start: Long) = db.sessionDao().insertSession(
                SessionEntity(title = "Practice", description = "", tagId = tag,
                    startTime = start, endTime = start + 60_000, durationMs = 60_000,
                    createdAt = start + 60_000, scyraPoints = points, surgePoints = surge))
            val oldArt = save(art, 100, 10, old)
            save(art, 50, 5, today)
            save(music, 200, 20, today)
            save(reading, 300, 30, old)
            db.objectiveCompletionDao().insertCompletion(ObjectiveCompletionEntity(
                objectiveId = 1, journeyId = art, journeyNameSnapshot = "Art", periodType = "daily",
                objectiveType = "one_time", periodStartMs = old, periodEndMs = old + 86_400_000,
                completedAt = old + 60_000, achievedDurationMs = 60_000, targetDurationMs = 60_000,
                baseRewardPearls = 0, streakBeforeCompletion = 0, streakMultiplier = 1.0,
                finalRewardPearls = 0, badgeKey = "fixture", badgeLabelSnapshot = "Objective",
                pearlsClaimed = true, finalRewardPoints = 10, sourceSessionId = oldArt))
            val chronicle = ChronicleRepository(db, db.chronicleDao())
            val flows = FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao(), chronicle)
            val journeys = JourneyRepository(db.tagDao())
            val health = FlowHealthRepository(db, db.flowHealthDao(), db.sessionDao(), db.pearlLedgerDao())
            val provider = HealthConnectClientProvider(context)
            // Keep this test independent of the device's movement settings and permissions.
            val settings = HealthSettingsRepository(object : DataStore<Preferences> {
                override val data = flowOf(emptyPreferences())
                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                    error("Read-only test settings")
            })
            val vm = withContext(Dispatchers.Main.immediate) {
                StoryViewModel(flows,
                    PulseRepository(db.pulseDao(), db.sessionDao(), db.tagDao(), db, db.chronicleDao(), chronicle),
                    journeys, AliveFlowRepository(db.ongoingSessionDao()), health,
                    HealthRefreshUseCase(settings, HealthPermissionRepository(provider),
                        HealthConnectMovementDataSource(provider), health, MovementBonusCalculator()),
                    UserPrefs(context), ArcMetadataRepository(db.arcMetadataDao()), chronicle,
                    LifetimeAnalyticsRepository(flows, journeys), SavedStateHandle())
            }
            model = vm
            suspend fun expect(period: StoryPeriod, tags: Set<Long>, score: Long, surge: Long, count: Int) {
                val expectedAnchor = TimeWindowUtils.startOfPeriodMs(System.currentTimeMillis(), period)
                val state = withTimeout(10_000) {
                    // Period and anchor are separate inputs; wait for the complete selection.
                    vm.uiState.first { !it.isLoading && it.period == period &&
                        it.anchorDayStartMs == expectedAnchor && it.selectedTagIds == tags }
                }
                assertEquals(score, state.currentScore)
                assertEquals(surge, state.currentSurgeScore)
                assertEquals(count, state.sessions.size)
            }
            vm.onPeriodSelected(StoryPeriod.LIFETIME)
            expect(StoryPeriod.LIFETIME, emptySet(), 660, 65, 4)
            vm.onTagToggled(art)
            expect(StoryPeriod.LIFETIME, setOf(art), 160, 15, 2)
            vm.onTagToggled(music)
            expect(StoryPeriod.LIFETIME, setOf(art, music), 360, 35, 3)
            for (period in listOf(StoryPeriod.DAY, StoryPeriod.WEEK, StoryPeriod.MONTH)) {
                vm.onPeriodSelected(period)
                expect(period, setOf(art, music), 250, 25, 2)
            }
            vm.onPeriodSelected(StoryPeriod.LIFETIME)
            expect(StoryPeriod.LIFETIME, setOf(art, music), 360, 35, 3)
            vm.onTagToggled(art)
            expect(StoryPeriod.LIFETIME, setOf(music), 200, 20, 1)
            vm.onClearAllTags()
            expect(StoryPeriod.LIFETIME, emptySet(), 660, 65, 4)
        } finally {
            model?.viewModelScope?.coroutineContext?.get(Job)?.cancelAndJoin()
            db.close()
        }
    }
}
