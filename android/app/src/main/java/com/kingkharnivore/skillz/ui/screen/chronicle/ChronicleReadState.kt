package com.kingkharnivore.skillz.ui.screen.chronicle

import com.kingkharnivore.skillz.data.repository.ChronicleRepository
import com.kingkharnivore.skillz.model.ui.ChronicleMomentUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ChronicleReadState(
    ownerType: String,
    ownerKey: String,
    repository: ChronicleRepository,
    parentScope: CoroutineScope
) {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    val moments: StateFlow<List<ChronicleMomentUi>> = repository.observeContent(ownerType, ownerKey)
        .map { it.moments }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun close() = job.cancel()
}
