package com.kingkharnivore.skillz.data.repository.shell

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementBackfillWorkerTest {
    @Test fun liveLevel99AchievementRemainsNewAndUnviewed() {
        assertTrue(AchievementReconciliationPolicy.isNew(AchievementReconciliationMode.RUNTIME))
        assertNull(AchievementReconciliationPolicy.initialViewedAt(AchievementReconciliationMode.RUNTIME, 123L))
    }

    @Test fun backfilledAchievementIsNotNewAndIsAlreadyAcknowledged() {
        assertFalse(AchievementReconciliationPolicy.isNew(AchievementReconciliationMode.HISTORICAL_IMPORT))
        assertEquals(
            123L,
            AchievementReconciliationPolicy.initialViewedAt(AchievementReconciliationMode.HISTORICAL_IMPORT, 123L)
        )
    }

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
