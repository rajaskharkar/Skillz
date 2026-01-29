package com.kingkharnivore.skillz.viewmodel.atlas

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun tickerFlow(periodMs: Long = 60_000L): Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(periodMs)
    }
}