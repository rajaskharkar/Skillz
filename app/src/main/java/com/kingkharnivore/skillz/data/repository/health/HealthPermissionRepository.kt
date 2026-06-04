package com.kingkharnivore.skillz.data.repository.health

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.kingkharnivore.skillz.data.health.HealthConnectAvailability
import com.kingkharnivore.skillz.data.health.HealthConnectClientProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthPermissionRepository @Inject constructor(
    private val clientProvider: HealthConnectClientProvider
) {
    val readStepsPermission: String =
        HealthPermission.getReadPermission(StepsRecord::class)

    fun availability(): HealthConnectAvailability {
        return clientProvider.availability()
    }

    fun rawSdkStatus(): Int {
        return clientProvider.rawSdkStatus()
    }

    fun isHealthConnectAvailable(): Boolean {
        return clientProvider.isAvailable()
    }

    suspend fun isReadStepsGranted(): Boolean {
        val client = clientProvider.clientOrNull() ?: run {
            Log.d(TAG, "isReadStepsGranted=false because Health Connect client is null")
            return false
        }

        return runCatching {
            val granted = client.permissionController.getGrantedPermissions()
            Log.d(TAG, "Granted Health Connect permissions=$granted")
            readStepsPermission in granted
        }.onFailure {
            Log.w(TAG, "Could not read Health Connect granted permissions", it)
        }.getOrDefault(false)
    }

    private companion object {
        const val TAG = "HealthPermissionRepo"
    }
}