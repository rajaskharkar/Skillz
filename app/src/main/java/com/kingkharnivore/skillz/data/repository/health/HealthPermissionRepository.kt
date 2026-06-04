package com.kingkharnivore.skillz.data.repository.health

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.kingkharnivore.skillz.data.health.HealthConnectClientProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthPermissionRepository @Inject constructor(
    private val clientProvider: HealthConnectClientProvider
) {
    val readStepsPermission: String = HealthPermission.getReadPermission(StepsRecord::class)

    fun isHealthConnectAvailable(): Boolean = clientProvider.isAvailable()

    suspend fun isReadStepsGranted(): Boolean {
        val client = clientProvider.clientOrNull() ?: return false
        return readStepsPermission in client.permissionController.getGrantedPermissions()
    }
}
