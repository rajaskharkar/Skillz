package com.kingkharnivore.skillz.data.repository.shell

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.PulseDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.data.model.dao.ChronicleDao
import com.kingkharnivore.skillz.data.model.dao.ChronicleSummary
import com.kingkharnivore.skillz.data.model.dao.shell.IdeaGroveDao
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentType
import com.kingkharnivore.skillz.data.model.entity.ChronicleOwnerType
import com.kingkharnivore.skillz.data.model.entity.PulseEntity
import com.kingkharnivore.skillz.data.model.entity.PulseCreationEntity
import com.kingkharnivore.skillz.data.model.entity.PulseFlowLinkEntity
import com.kingkharnivore.skillz.data.model.entity.PulseGroveStatusValues
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.PulseRepository
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveFlowUiModel
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemType
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemUiModel
import com.kingkharnivore.skillz.model.state.ideagrove.PulseLaunchContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class IdeaGroveRepository @Inject constructor(
    private val database: SkillzDatabase,
    private val ideaGroveDao: IdeaGroveDao,
    private val pulseDao: PulseDao,
    private val pulseRepository: PulseRepository,
    private val sessionDao: SessionDao,
    private val tagDao: TagDao,
    private val chronicleDao: ChronicleDao
) {
    fun observeIdeaGroveItems(): Flow<List<IdeaGroveItemUiModel>> = combine(
        pulseDao.getAllPulses(),
        ideaGroveDao.observePulseFlowLinks(),
        sessionDao.getAllSessions(),
        tagDao.getAllTags(),
        chronicleDao.observeSummaries()
    ) { pulses, links, sessions, tags, texts ->
        mapItems(pulses, links, sessions, tags, texts)
    }

    /** A new Idea starts as an alive Pulse; its first recorded Flow grows its journey. */
    suspend fun createIdea(
        title: String,
        journeyName: String,
        creationKey: String = UUID.randomUUID().toString()
    ): Long = database.withTransaction {
        require(creationKey.isNotBlank())
        pulseDao.findCreatedPulse(creationKey)?.let { return@withTransaction it }
        require(title.isNotBlank())
        val tagId = journeyName.trim().takeIf(String::isNotEmpty)?.let { name ->
            tagDao.getTagByName(name)?.id ?: tagDao.insertTag(TagEntity(name = name))
        }
        val id = pulseRepository.addPulse(title.trim(), "", tagId, null, null, null)
        pulseDao.insertCreation(PulseCreationEntity(creationKey, id, System.currentTimeMillis()))
        id
    }

    suspend fun markPulseAsInsight(pulseId: Long) = database.withTransaction {
        if (ideaGroveDao.getLinkedFlowCount(pulseId) > 0) return@withTransaction
        ideaGroveDao.updatePulseGroveStatus(
            pulseId = pulseId,
            status = PulseGroveStatusValues.INSIGHT,
            changedAt = System.currentTimeMillis()
        )
    }

    suspend fun markPulseCompleted(pulseId: Long) = database.withTransaction {
        if (ideaGroveDao.getLinkedFlowCount(pulseId) <= 0) return@withTransaction
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

    suspend fun repairCompletedPulsesWithoutFlows() {
        ideaGroveDao.repairCompletedPulsesWithoutFlows(
            completedStatus = PulseGroveStatusValues.COMPLETED,
            aliveStatus = PulseGroveStatusValues.ALIVE,
            changedAt = System.currentTimeMillis()
        )
    }

    suspend fun getPulseLaunchContext(pulseId: Long): PulseLaunchContext? {
        val pulse = pulseDao.getPulseById(pulseId) ?: return null
        val journeyName = pulse.tagId?.let { tagId ->
            tagDao.getAllTagsSnapshot().firstOrNull { it.id == tagId }?.name
        }
        return PulseLaunchContext(
            pulseId = pulse.id,
            title = pulse.title,
            description = chronicleDao.find(
                ChronicleOwnerType.PULSE,
                pulse.id.toString()
            )?.let { chronicle ->
                chronicleDao.moments(chronicle.id)
                    .firstOrNull { moment ->
                        moment.type == ChronicleMomentType.TEXT &&
                            !moment.text.isNullOrBlank()
                    }
                    ?.text
            } ?: pulse.description,
            journeyName = journeyName
        )
    }

    private fun mapItems(
        pulses: List<PulseEntity>,
        links: List<PulseFlowLinkEntity>,
        sessions: List<SessionEntity>,
        tags: List<TagEntity>,
        texts: List<ChronicleSummary>
    ): List<IdeaGroveItemUiModel> {
        val tagsById = tags.associateBy { it.id }
        val sessionsById = sessions.associateBy { it.id }
        val linksByPulse = links.groupBy { it.pulseId }
        val previews = texts.associate { "${it.ownerType}/${it.ownerKey}" to it.excerpt.orEmpty() }

        return pulses.map { pulse ->
            val flows = linksByPulse[pulse.id]
                .orEmpty()
                .mapNotNull { link ->
                    val session = sessionsById[link.sessionId] ?: return@mapNotNull null
                    IdeaGroveFlowUiModel(
                        sessionId = session.id,
                        title = session.title,
                        description = previews["SESSION/${session.id}"] ?: session.description,
                        journeyName = tagsById[session.tagId]?.name,
                        durationMs = session.durationMs,
                        startTime = session.startTime,
                        endTime = session.endTime
                    )
                }
                .sortedByDescending { it.endTime ?: it.startTime }

            val flowCount = flows.size
            val effectiveStatus = when {
                pulse.groveStatus == PulseGroveStatusValues.COMPLETED && flowCount == 0 -> PulseGroveStatusValues.ALIVE
                // Old racing status updates must not hide a recorded journey as an Insight.
                pulse.groveStatus == PulseGroveStatusValues.INSIGHT && flowCount > 0 -> PulseGroveStatusValues.ALIVE
                else -> pulse.groveStatus
            }
            val type = when {
                effectiveStatus == PulseGroveStatusValues.INSIGHT -> IdeaGroveItemType.INSIGHT
                effectiveStatus == PulseGroveStatusValues.COMPLETED ->
                    IdeaGroveItemType.COMPLETED_IDEA
                flowCount > 0 -> IdeaGroveItemType.IDEA
                else -> IdeaGroveItemType.RAW_PULSE
            }

            IdeaGroveItemUiModel(
                pulseId = pulse.id,
                type = type,
                title = pulse.title,
                description = previews["PULSE/${pulse.id}"] ?: pulse.description,
                journeyName = pulse.tagId?.let { tagsById[it]?.name },
                createdAt = pulse.createdAt,
                updatedAt = pulse.updatedAt,
                groveStatus = effectiveStatus,
                groveStatusChangedAt = pulse.groveStatusChangedAt,
                flowCount = flowCount,
                totalFlowDurationMs = flows.sumOf { it.durationMs },
                lastWorkedAt = flows.maxOfOrNull { it.endTime ?: it.startTime },
                flows = flows,
                wasCapturedDuringFlow = pulse.parentSessionId != null
                        || pulse.parentFlowInstanceId != null
            )
        }
    }
}
