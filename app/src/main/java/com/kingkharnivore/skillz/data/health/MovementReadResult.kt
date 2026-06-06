package com.kingkharnivore.skillz.data.health

sealed interface MovementReadResult {
    data class Success(val steps: Long) : MovementReadResult
    data object PermissionMissing : MovementReadResult
    data object HealthConnectUnavailable : MovementReadResult
    data object NoData : MovementReadResult
    data class Error(val throwable: Throwable) : MovementReadResult
}
