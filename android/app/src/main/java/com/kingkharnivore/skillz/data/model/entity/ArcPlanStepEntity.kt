package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "arc_plan_steps",
    foreignKeys = [
        ForeignKey(
            entity = ArcPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["arcPlanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FlowPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceFlowPlanId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagIdSnapshot"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("arcPlanId"),
        Index("sourceFlowPlanId"),
        Index("tagIdSnapshot")
    ]
)
data class ArcPlanStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val arcPlanId: Long,
    val orderIndex: Int,
    val sourceFlowPlanId: Long? = null,
    val titleSnapshot: String,
    val tagIdSnapshot: Long? = null,
    val isSoftModeSnapshot: Boolean = false,
    val targetMinutesSnapshot: Int? = null,
    val launchWithSurgeSnapshot: Boolean = false,
    val linkState: String = LINK_STATE_LINKED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val LINK_STATE_LINKED = "linked"
        const val LINK_STATE_CUSTOMIZED = "customized"
        const val LINK_STATE_DETACHED = "detached"
    }
}