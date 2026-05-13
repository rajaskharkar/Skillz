package com.kingkharnivore.skillz.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.SkillzMigrations
import com.kingkharnivore.skillz.data.model.dao.ActiveArcRunDao
import com.kingkharnivore.skillz.data.model.dao.ArcPlanDao
import com.kingkharnivore.skillz.data.model.dao.FlowPlanDao
import com.kingkharnivore.skillz.data.model.dao.OngoingSessionDao
import com.kingkharnivore.skillz.data.model.dao.PulseDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.utils.arc.ArcPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SkillzDatabase {
        return Room.databaseBuilder(
            context,
            SkillzDatabase::class.java,
            "skillz_db"
        ).addMigrations(*SkillzMigrations.ALL_MIGRATIONS).build()
    }

    @Provides
    fun provideTagDao(db: SkillzDatabase): TagDao = db.tagDao()

    @Provides
    fun provideSessionDao(db: SkillzDatabase): SessionDao = db.sessionDao()

    @Provides
    fun providePulseDao(db: SkillzDatabase): PulseDao = db.pulseDao()

    @Provides
    fun provideOngoingSessionDao(db: SkillzDatabase): OngoingSessionDao =
        db.ongoingSessionDao()

    @Provides
    fun provideFlowPlanDao(db: SkillzDatabase): FlowPlanDao = db.flowPlanDao()

    @Provides
    fun provideArcPlanDao(db: SkillzDatabase): ArcPlanDao = db.arcPlanDao()

    @Provides
    fun provideActiveArcRunDao(db: SkillzDatabase): ActiveArcRunDao = db.activeArcRunDao()

    @Provides
    @Singleton
    fun provideArcPrefs(ds: DataStore<Preferences>): ArcPrefs = ArcPrefs(ds)

    private val Context.skillzDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "skillz_prefs"
    )

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.skillzDataStore
}
