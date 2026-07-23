package com.kingkharnivore.skillz.data.repository.shell

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementBackfillWorkerTest {
    @Test fun transientStorageFailureIsRetryable() {
        assertEquals(
            AchievementBackfillFailureType.STORAGE_UNAVAILABLE,
            AchievementBackfillFailureClassifier.classify(IOException("temporarily unavailable"))
        )
    }

    @Test fun schemaAndProgrammingFailuresAreDeterministic() {
        assertEquals(
            AchievementBackfillFailureType.SCHEMA_MISMATCH,
            AchievementBackfillFailureClassifier.classify(RuntimeException("no such table: achievement_backfill"))
        )
        assertEquals(
            AchievementBackfillFailureType.PROGRAMMING_ERROR,
            AchievementBackfillFailureClassifier.classify(IllegalStateException("invalid catalog"))
        )
    }
}
