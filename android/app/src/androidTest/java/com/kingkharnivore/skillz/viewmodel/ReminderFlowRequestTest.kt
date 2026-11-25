package com.kingkharnivore.skillz.viewmodel

import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test

class ReminderFlowRequestTest {
    @Test fun acceptedDestinationAndSourceSurviveProcessRecreationAndRejectRepeatedTaps() {
        val saved = SavedStateHandle()
        val request = ReminderFlowRequest(saved)
        assertTrue(request.request(17))
        request.accept("source-flow")
        assertFalse(request.request(17))
        assertFalse(request.request(18))
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(saved.savedStateProvider().saveState())
            parcel.setDataPosition(0)
            val restored = ReminderFlowRequest(SavedStateHandle.createHandle(parcel.readBundle(javaClass.classLoader), null))
            assertEquals(17L, restored.activityId.value)
            assertEquals("source-flow", restored.sourceFlowId)
            assertTrue(restored.accepted.value)
            restored.clear()
            assertNull(restored.activityId.value)
            assertNull(restored.sourceFlowId)
            assertFalse(restored.accepted.value)
            assertTrue(restored.request(18))
        } finally { parcel.recycle() }
    }

    @Test fun plannedReminderDetourResumesTheWaitingStepWithoutSkippingIt() {
        assertTrue(isPlannedRunAlreadyAdvanced(1, null,
            com.kingkharnivore.skillz.utils.arc.ArcPrefs.PlannedFlowHandoff.REMINDER_DETOUR))
    }
}
