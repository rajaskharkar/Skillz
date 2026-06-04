package com.kingkharnivore.skillz.domain.health

import com.kingkharnivore.skillz.data.health.MovementReadResult

class MovementStepAggregator {
    suspend fun readStepsAcrossActiveIntervals(
        intervals: List<FlowActiveInterval>,
        readInterval: suspend (FlowActiveInterval) -> MovementReadResult
    ): MovementReadResult {
        val normalized = FlowActiveIntervalNormalizer.normalize(intervals)
        if (normalized.isEmpty()) return MovementReadResult.NoData

        var totalSteps = 0L
        var sawSuccess = false
        var sawNoData = false
        normalized.forEach { interval ->
            when (val result = readInterval(interval)) {
                MovementReadResult.HealthConnectUnavailable -> return MovementReadResult.HealthConnectUnavailable
                MovementReadResult.PermissionMissing -> return MovementReadResult.PermissionMissing
                MovementReadResult.NoData -> sawNoData = true
                is MovementReadResult.Error -> return result
                is MovementReadResult.Success -> {
                    sawSuccess = true
                    totalSteps += result.steps
                }
            }
        }
        return if (sawSuccess) {
            MovementReadResult.Success(totalSteps)
        } else if (sawNoData) {
            MovementReadResult.NoData
        } else {
            MovementReadResult.NoData
        }
    }
}
