package com.kingkharnivore.skillz.di

import android.content.Context
import androidx.room.Room
import com.kingkharnivore.skillz.data.model.OngoingSessionDao
import com.kingkharnivore.skillz.data.model.SessionDao
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.TagDao
import com.kingkharnivore.skillz.data.repository.FocusSessionRepository
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
}