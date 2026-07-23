package com.kingkharnivore.skillz.data.repository.shell

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import java.io.IOException

enum class AchievementBackfillFailureType(val retryable: Boolean) {
    TRANSIENT_DATABASE(true),
    DATABASE_LOCKED(true),
    STORAGE_UNAVAILABLE(true),
    SCHEMA_MISMATCH(false),
    PROGRAMMING_ERROR(false),
    UNKNOWN(false)
}

object AchievementBackfillFailureClassifier {
    fun classify(failure: Throwable): AchievementBackfillFailureType {
        val causes = generateSequence(failure) { it.cause }.toList()
        val names = causes.map { it::class.java.simpleName }
        val messages = causes.mapNotNull { it.message?.lowercase() }
        return when {
            names.any { it.contains("DatabaseLocked") || it.contains("LockedException") } ->
                AchievementBackfillFailureType.DATABASE_LOCKED
            messages.any { "no such table" in it || "migration" in it || "schema" in it } ->
                AchievementBackfillFailureType.SCHEMA_MISMATCH
            causes.any { it is IOException } || names.any { it.contains("CantOpenDatabase") } ->
                AchievementBackfillFailureType.STORAGE_UNAVAILABLE
            names.any { it.contains("SQLite") } -> AchievementBackfillFailureType.TRANSIENT_DATABASE
            causes.any { it is IllegalArgumentException || it is IllegalStateException } ->
                AchievementBackfillFailureType.PROGRAMMING_ERROR
            else -> AchievementBackfillFailureType.UNKNOWN
        }
    }
}

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
            .fold(onSuccess = { Result.success() }, onFailure = { failure ->
                val type = AchievementBackfillFailureClassifier.classify(failure)
                val output = workDataOf("failureType" to type.name)
                if (type.retryable && runAttemptCount + 1 < MAX_ATTEMPTS) Result.retry()
                else Result.failure(output)
            })
    }

    companion object {
        const val BACKFILL_VERSION = 2
        const val UNIQUE_WORK_NAME = "scyra_achievement_backfill_v2"
        const val MAX_ATTEMPTS = 3

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
