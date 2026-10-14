package com.kingkharnivore.skillz.domain.power

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kingkharnivore.skillz.ui.screen.shell.rooms.red.rememberRedSceneClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RedSceneClockTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sceneClockStopsWhenCoveredOrBackgroundedAndResumesWithoutJumping() {
        val enabled = mutableStateOf(true)
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle = registry
        }
        owner.registry.currentState = Lifecycle.State.RESUMED
        lateinit var clock: State<Float>
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                clock = rememberRedSceneClock(null, enabled.value)
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        val running = compose.runOnIdle { clock.value }
        assertTrue(running > .5f)
        compose.runOnIdle { enabled.value = false }
        compose.mainClock.advanceTimeByFrame()
        val covered = compose.runOnIdle { clock.value }
        compose.mainClock.advanceTimeBy(2_000)
        compose.runOnIdle { assertEquals(covered, clock.value, .001f); enabled.value = true }
        compose.mainClock.advanceTimeBy(1_000)
        val resumed = compose.runOnIdle { clock.value }
        assertTrue(resumed > covered + .5f && resumed < covered + 1.1f)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        val stopped = compose.runOnIdle { clock.value }
        compose.mainClock.advanceTimeBy(2_000)
        compose.runOnIdle { assertEquals(stopped, clock.value, .001f); owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle { assertTrue(clock.value > stopped + .5f && clock.value < stopped + 1.1f) }
    }
}
