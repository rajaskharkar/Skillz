package com.kingkharnivore.skillz.data.model.entity.health

enum class FlowHealthSyncStatus {
    NOT_ENABLED,
    NOT_ELIGIBLE,
    PENDING,
    NO_REWARD,
    CAPTURED,
    EXPIRED,
    PERMISSION_REVOKED,
    DISABLED_BEFORE_CAPTURE,
    ERROR_RETRYABLE,
    ERROR_FINAL
}
