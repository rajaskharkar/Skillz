package com.kingkharnivore.skillz.data.repository.shell

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import com.kingkharnivore.skillz.domain.achievement.*

/** Called inside score-changing transactions, so a saved milestone cannot be lost on exit. */
internal object LifetimePointsBadgeRecorder {
    suspend fun reconcile(
        db: SkillzDatabase,
        now: Long = System.currentTimeMillis(),
        mode: AchievementReconciliationMode = AchievementReconciliationMode.RUNTIME
    ) = db.withTransaction {
        val total = db.sessionDao().lifetimeScyraPoints()
        val badges = db.userBadgeDao()
        for (spec in LifetimePointsBadges.specs) {
            if (total < spec.target || badges.get(spec.id) != null) continue
            badges.upsert(UserBadgeEntity(
                badgeId = spec.id, count = 1, firstEarnedAt = now, lastEarnedAt = now,
                isNew = AchievementReconciliationPolicy.isNew(mode),
                viewedAt = AchievementReconciliationPolicy.initialViewedAt(mode, now),
                // Historical totals prove qualification, but not when a threshold was crossed.
                timestampConfidence = if (mode == AchievementReconciliationMode.RUNTIME)
                    AchievementTimestampConfidence.EXACT else AchievementTimestampConfidence.UNKNOWN
            ))
        }
    }
}
