package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.BeamEntity
import com.kingkharnivore.skillz.data.repository.BeamRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BeamListItemUi(
    val id: Long,
    val tagName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long
)

@HiltViewModel
class DailyBeamViewModel @Inject constructor(
    beamRepository: BeamRepository,
    tagRepository: JourneyRepository
) : ViewModel() {

    // map tagId -> name
    private val tagMapFlow = tagRepository.getAllTags()

    val beams: StateFlow<List<BeamListItemUi>> =
        beamRepository.observeAllBeams()
            .combine(tagMapFlow) { beams, tags ->
                val map = tags.associateBy({ it.id }, { it.name })
                beams.map { b ->
                    BeamListItemUi(
                        id = b.id,
                        tagName = map[b.tagId] ?: "Unknown",
                        startTime = b.startTime,
                        endTime = b.endTime,
                        durationMs = b.durationMs
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
