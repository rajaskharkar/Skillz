package com.kingkharnivore.skillz.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import javax.inject.Inject

class HealthConnectMovementDataSource @Inject constructor(
    private val clientProvider: HealthConnectClientProvider
) {
    suspend fun readStepsBetween(start: Instant, end: Instant): MovementReadResult {
        if (!end.isAfter(start)) return MovementReadResult.NoData
        val client = clientProvider.clientOrNull() ?: return MovementReadResult.HealthConnectUnavailable
        return try {
            val permissions = client.permissionController.getGrantedPermissions()
            if (HealthPermission.getReadPermission(StepsRecord::class) !in permissions) {
                return MovementReadResult.PermissionMissing
            }
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val steps = response[StepsRecord.COUNT_TOTAL] ?: return MovementReadResult.NoData
            MovementReadResult.Success(steps.coerceAtLeast(0L))
        } catch (security: SecurityException) {
            MovementReadResult.PermissionMissing
        } catch (t: Throwable) {
            MovementReadResult.Error(t)
        }
    }
}
