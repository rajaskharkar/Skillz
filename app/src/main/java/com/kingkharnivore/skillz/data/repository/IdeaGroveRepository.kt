package com.kingkharnivore.skillz.data.repository

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.IdeaGroveDao
import com.kingkharnivore.skillz.data.model.dao.PulseDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.data.model.entity.PulseEntity
import com.kingkharnivore.skillz.data.model.entity.PulseFlowLinkEntity
import com.kingkharnivore.skillz.data.model.entity.PulseGroveStatusValues
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveFlowUiModel
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemType
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemUiModel
import com.kingkharnivore.skillz.model.state.ideagrove.PulseLaunchContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdeaGroveRepository @Inject constructor(
    private val database: SkillzDatabase,
    private val ideaGroveDao: IdeaGroveDao,
    private val pulseDao: PulseDao,
    private val pulseRepository: PulseRepository,
    private val sessionDao: SessionDao,
    private val tagDao: TagDao
) {
    fun observeIdeaGroveItems(): Flow<List<IdeaGroveItemUiModel>> = combine(
        pulseDao.getAllPulses(),
        ideaGroveDao.observePulseFlowLinks(),
        sessionDao.getAllSessions(),
        tagDao.getAllTags()
    ) { pulses, links, sessions, tags ->
        mapItems(pulses, links, sessions, tags)
    }

    suspend fun markPulseAsInsight(pulseId: Long) {
        if (ideaGroveDao.getLinkedFlowCount(pulseId) > 0) return
        ideaGroveDao.updatePulseGroveStatus(
            pulseId = pulseId,
            status = PulseGroveStatusValues.INSIGHT,
            changedAt = System.currentTimeMillis()
        )
    }

    suspend fun markPulseCompleted(pulseId: Long) {
        if (ideaGroveDao.getLinkedFlowCount(pulseId) <= 0) return
        ideaGroveDao.updatePulseGroveStatus(
            pulseId = pulseId,
            status = PulseGroveStatusValues.COMPLETED,
            changedAt = System.currentTimeMillis()
        )
    }

    suspend fun revivePulse(pulseId: Long) {
        ideaGroveDao.updatePulseGroveStatus(
            pulseId = pulseId,
            status = PulseGroveStatusValues.ALIVE,
            changedAt = System.currentTimeMillis()
        )
    }

    suspend fun deletePulse(pulseId: Long) {
        pulseRepository.deletePulseAndCleanupTag(pulseId)
    }

    suspend fun getPulseLaunchContext(pulseId: Long): PulseLaunchContext? {
        val pulse = pulseDao.getPulseById(pulseId) ?: return null
        val journeyName = pulse.tagId?.let { tagId ->
            tagDao.getAllTagsSnapshot().firstOrNull { it.id == tagId }?.name
        }
        return PulseLaunchContext(
            pulseId = pulse.id,
            title = pulse.title,
            description = pulse.description,
            journeyName = journeyName
        )
    }

    suspend fun linkCompletedFlowToPulse(pulseId: Long, sessionId: Long) {
        runCatching {
            database.withTransaction {
                val pulse = ideaGroveDao.getPulse(pulseId) ?: return@withTransaction
                if (sessionDao.getSessionById(sessionId) == null) return@withTransaction
                val now = System.currentTimeMillis()
                ideaGroveDao.insertPulseFlowLink(
                    PulseFlowLinkEntity(
                        pulseId = pulseId,
                        sessionId = sessionId,
                        linkedAt = now
                    )
                )
                if (pulse.groveStatus == PulseGroveStatusValues.INSIGHT) {
                    ideaGroveDao.updatePulseGroveStatus(
                        pulseId = pulseId,
                        status = PulseGroveStatusValues.ALIVE,
                        changedAt = now
                    )
                }
            }
        }
    }

    private fun mapItems(
        pulses: List<PulseEntity>,
        links: List<PulseFlowLinkEntity>,
        sessions: List<SessionEntity>,
        tags: List<TagEntity>
    ): List<IdeaGroveItemUiModel> {
        val tagsById = tags.associateBy { it.id }
        val sessionsById = sessions.associateBy { it.id }
        val linksByPulse = links.groupBy { it.pulseId }

        return pulses.map { pulse ->
            val flows = linksByPulse[pulse.id]
                .orEmpty()
                .mapNotNull { link ->
                    val session = sessionsById[link.sessionId] ?: return@mapNotNull null
                    IdeaGroveFlowUiModel(
                        sessionId = session.id,
                        title = session.title,
                        description = session.description,
                        journeyName = tagsById[session.tagId]?.name,
                        durationMs = session.durationMs,
                        startTime = session.startTime,
                        endTime = session.endTime
                    )
                }
                .sortedByDescending { it.endTime ?: it.startTime }

            val flowCount = flows.size
            val effectiveStatus = if (
                pulse.groveStatus == PulseGroveStatusValues.COMPLETED && flowCount == 0
            ) {
                PulseGroveStatusValues.ALIVE
            } else {
                pulse.groveStatus
            }
            val type = when {
                effectiveStatus == PulseGroveStatusValues.INSIGHT -> IdeaGroveItemType.INSIGHT
                effectiveStatus == PulseGroveStatusValues.COMPLETED -> IdeaGroveItemType.COMPLETED_IDEA
                flowCount > 0 -> IdeaGroveItemType.IDEA
                else -> IdeaGroveItemType.RAW_PULSE
            }

            IdeaGroveItemUiModel(
                pulseId = pulse.id,
                type = type,
                title = pulse.title.ifBlank { "Untitled Pulse" },
                description = pulse.description,
                journeyName = pulse.tagId?.let { tagsById[it]?.name },
                createdAt = pulse.createdAt,
                updatedAt = pulse.updatedAt,
                groveStatus = effectiveStatus,
                groveStatusChangedAt = pulse.groveStatusChangedAt,
                flowCount = flowCount,
                totalFlowDurationMs = flows.sumOf { it.durationMs },
                lastWorkedAt = flows.maxOfOrNull { it.endTime ?: it.startTime },
                flows = flows,
                wasCapturedDuringFlow = pulse.parentSessionId != null || pulse.parentFlowInstanceId != null
            )
        }
    }
}
