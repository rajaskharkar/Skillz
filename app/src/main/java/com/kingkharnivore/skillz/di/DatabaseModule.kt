package com.kingkharnivore.skillz.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.kingkharnivore.skillz.data.model.dao.OngoingSessionDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.TagDao
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
        ).build()
    }

    @Provides
    fun provideTagDao(db: SkillzDatabase): TagDao = db.tagDao()

    @Provides
    fun provideSessionDao(db: SkillzDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideOngoingSessionDao(db: SkillzDatabase): OngoingSessionDao =
        db.ongoingSessionDao()

    private val Context.skillzDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "skillz_prefs"
    )

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.skillzDataStore
}