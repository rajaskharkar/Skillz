package com.kingkharnivore.skillz.ui.screen.paths

import com.kingkharnivore.skillz.data.model.entity.*
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.model.state.ideagrove.*
import com.kingkharnivore.skillz.model.state.paths.*
import org.junit.Assert.*
import org.junit.Test

class IdeaMemoryTimelineTest {
    private fun idea(id: Long, times: List<Long>, completed: Boolean = false) = IdeaGroveItemUiModel(
        id, if (completed) IdeaGroveItemType.COMPLETED_IDEA else IdeaGroveItemType.IDEA,
        "Idea $id", "Original spark", "Design", 1, 1,
        if (completed) PulseGroveStatusValues.COMPLETED else PulseGroveStatusValues.ALIVE,
        if (completed) 999 else null, times.size, times.size * 60_000L, times.maxOrNull(),
        times.mapIndexed { index, time -> IdeaGroveFlowUiModel(index.toLong(), "Step $index", "Notes", "Design", 60_000, time - 1, time) }, false
    )

    @Test fun memoriesMixKindsByMostRecentFlowAndIncludeCompletedIdeasButExcludeUnworkedSparks() {
        val receipt = HorizonMemoryEntity("habit-flow", 7, HorizonKind.HABIT, "Habit", null,
            "Habit Flow", "", FlowMode.FLOW, 60_000, 1, 0, null, null, null, 200)
        val habit = MemoryGroupUi(7, HorizonKind.HABIT, "Habit", listOf(MemoryFlowUi(receipt, null, "")))
        val completed = idea(7, listOf(100), completed = true)
        val active = idea(8, listOf(300, 50))
        val timeline = memoryTimeline(listOf(habit), listOf(completed, active, idea(9, emptyList())))
        assertEquals(listOf("IDEA:8", "HABIT:7", "IDEA:7"), timeline.map { it.key })
        assertSame(active, (timeline.first() as MemoryTimelineItem.Idea).idea)
        assertEquals(2, (timeline.first() as MemoryTimelineItem.Idea).idea.flows.size)
        assertSame(completed, (timeline.last() as MemoryTimelineItem.Idea).idea)
    }
}
