package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity

data class ObjectiveBadgePresentationMetadata(
    val badgeKey: String,
    val journeyId: Long,
    val journeyNameSnapshot: String,
    val periodType: String
)

fun objectiveBadgePresentationMetadata(
    completions: List<ObjectiveCompletionEntity>
): Map<String, ObjectiveBadgePresentationMetadata> = completions
    .asSequence()
    .filter { ObjectiveBadgeIdentity.fromBadgeId(it.badgeKey) != null }
    .sortedWith(compareBy<ObjectiveCompletionEntity> { it.completedAt }.thenBy { it.id })
    .distinctBy { it.badgeKey }
    .associate { completion ->
        completion.badgeKey to ObjectiveBadgePresentationMetadata(
            completion.badgeKey,
            completion.journeyId,
            completion.journeyNameSnapshot,
            completion.periodType
        )
    }
