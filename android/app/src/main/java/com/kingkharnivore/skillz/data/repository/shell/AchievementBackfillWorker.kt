package com.kingkharnivore.skillz.data.repository.shell

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface AchievementBackfillEntryPoint {
    fun shellRepository(): ShellRepository
}

class AchievementBackfillWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = EntryPointAccessors.fromApplication(
            applicationContext,
            AchievementBackfillEntryPoint::class.java
        ).shellRepository()
        return runCatching { repository.backfillAchievements(BACKFILL_VERSION) }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        const val BACKFILL_VERSION = 2
        const val UNIQUE_WORK_NAME = "scyra_achievement_backfill_v2"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<AchievementBackfillWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
