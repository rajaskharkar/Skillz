package com.kingkharnivore.skillz.model.state.flow

data class StopwatchState(
    val isRunning: Boolean = false,
    val elapsedMs: Long = 0L
)