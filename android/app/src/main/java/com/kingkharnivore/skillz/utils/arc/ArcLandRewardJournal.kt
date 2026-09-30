package com.kingkharnivore.skillz.utils.arc

import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.ArcLandRewardEntity

/** Caller holds the same Room transaction that attaches a Flow to its Arc. */
object ArcLandRewardJournal {
    suspend fun record(db: SkillzDatabase, arcId: Long) {
        if (!com.kingkharnivore.skillz.BuildConfig.LAND_ENABLED) return
        val prior = db.arcLandRewardDao().get(arcId)
        check(prior?.finalizedAt == null) { "A finalized Arc cannot accept another Flow." }
        val sessions = db.sessionDao().getSessionsForArc(arcId)
        val last = sessions.maxByOrNull { it.endTime } ?: return
        db.arcLandRewardDao().save(ArcLandRewardEntity(arcId, sessions.size, last.endTime, last.id))
    }
}
