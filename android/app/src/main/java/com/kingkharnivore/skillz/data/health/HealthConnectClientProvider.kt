package com.kingkharnivore.skillz.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectClientProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun availability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED
            else -> HealthConnectAvailability.UNAVAILABLE
        }

    fun isAvailable(): Boolean = availability() == HealthConnectAvailability.AVAILABLE

    fun clientOrNull(): HealthConnectClient? =
        if (isAvailable()) HealthConnectClient.getOrCreate(context) else null
}
