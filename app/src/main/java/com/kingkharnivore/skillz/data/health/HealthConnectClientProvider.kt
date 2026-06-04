package com.kingkharnivore.skillz.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectClientProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun rawSdkStatus(): Int {
        val status = HealthConnectClient.getSdkStatus(context)
        Log.d(TAG, "Health Connect raw SDK status=$status")
        return status
    }

    fun availability(): HealthConnectAvailability {
        return when (val status = rawSdkStatus()) {
            HealthConnectClient.SDK_AVAILABLE -> {
                Log.d(TAG, "Health Connect availability=AVAILABLE")
                HealthConnectAvailability.AVAILABLE
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Log.d(TAG, "Health Connect availability=PROVIDER_UPDATE_REQUIRED")
                HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED
            }

            else -> {
                Log.d(TAG, "Health Connect availability=UNAVAILABLE rawStatus=$status")
                HealthConnectAvailability.UNAVAILABLE
            }
        }
    }

    fun isAvailable(): Boolean {
        return availability() == HealthConnectAvailability.AVAILABLE
    }

    fun clientOrNull(): HealthConnectClient? {
        return if (isAvailable()) {
            runCatching {
                HealthConnectClient.getOrCreate(context)
            }.onFailure {
                Log.w(TAG, "HealthConnectClient.getOrCreate failed", it)
            }.getOrNull()
        } else {
            null
        }
    }

    fun requireClient(): HealthConnectClient {
        check(isAvailable()) {
            "Health Connect is not available. status=${rawSdkStatus()}"
        }
        return HealthConnectClient.getOrCreate(context)
    }

    private companion object {
        const val TAG = "HealthConnectProvider"
    }
}