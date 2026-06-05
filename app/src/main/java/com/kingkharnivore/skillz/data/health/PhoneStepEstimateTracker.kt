package com.kingkharnivore.skillz.data.health

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PhoneStepEstimateState(
    val isAvailable: Boolean = false,
    val permissionGranted: Boolean = false,
    val isTracking: Boolean = false,
    val estimatedSteps: Long = 0L,
    val estimatedMovementPoints: Long = estimatedSteps / 25L,
    val source: PhoneStepSource = PhoneStepSource.NONE,
    val message: String? = null
)

@Singleton
class PhoneStepEstimateTracker @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val activeSensor: Sensor? = stepCounter ?: stepDetector
    private val source: PhoneStepSource = when {
        stepCounter != null -> PhoneStepSource.TYPE_STEP_COUNTER
        stepDetector != null -> PhoneStepSource.TYPE_STEP_DETECTOR
        else -> PhoneStepSource.NONE
    }

    private var accumulatedBeforePause = 0L
    private var baselineCounter: Float? = null
    private var latestCounter: Float? = null
    private var tracking = false

    private val _state = MutableStateFlow(currentState(isTracking = false, message = availabilityMessage()))
    val state: StateFlow<PhoneStepEstimateState> = _state.asStateFlow()

    fun hasRuntimePermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    fun isSensorAvailable(): Boolean = activeSensor != null

    fun canTrack(): Boolean = isSensorAvailable() && hasRuntimePermission()

    fun reset() {
        stopTracking()
        accumulatedBeforePause = 0L
        baselineCounter = null
        latestCounter = null
        publish(message = availabilityMessage())
    }

    fun startOrResumeTracking() {
        if (tracking) return
        if (!canTrack()) {
            publish(isTracking = false, message = availabilityMessage())
            return
        }
        baselineCounter = null
        latestCounter = null
        val registered = sensorManager.registerListener(this, activeSensor, SensorManager.SENSOR_DELAY_NORMAL)
        tracking = registered
        publish(isTracking = tracking, message = if (registered) null else "Phone step estimate is not available on this device. Watch steps may still sync through Health Connect.")
    }

    fun pauseTracking() {
        if (!tracking) return
        accumulatedBeforePause = estimatedSteps()
        sensorManager.unregisterListener(this)
        tracking = false
        baselineCounter = null
        latestCounter = null
        publish(isTracking = false)
    }

    fun stopTracking(): Long? {
        val finalSteps = if (isSensorAvailable() && hasRuntimePermission()) estimatedSteps() else null
        if (tracking) sensorManager.unregisterListener(this)
        tracking = false
        baselineCounter = null
        latestCounter = null
        publish(isTracking = false)
        return finalSteps
    }

    fun currentEstimatedSteps(): Long? = if (isSensorAvailable() && hasRuntimePermission()) estimatedSteps() else null

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> onStepCounter(event.values.firstOrNull() ?: return)
            Sensor.TYPE_STEP_DETECTOR -> if (tracking) {
                accumulatedBeforePause += 1L
            }
        }
        publish(isTracking = tracking)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    internal fun onStepCounterForTest(counterValue: Float) = onStepCounter(counterValue)
    internal fun onStepDetectorForTest() {
        if (tracking) accumulatedBeforePause += 1L
        publish(isTracking = tracking)
    }

    private fun onStepCounter(counterValue: Float) {
        if (!tracking) return
        val baseline = baselineCounter
        if (baseline == null || counterValue < baseline) {
            baselineCounter = counterValue
            latestCounter = counterValue
            return
        }
        latestCounter = counterValue
    }

    private fun estimatedSteps(): Long {
        val activeDelta = if (source == PhoneStepSource.TYPE_STEP_COUNTER) {
            val baseline = baselineCounter
            val latest = latestCounter
            if (baseline != null && latest != null && latest >= baseline) (latest - baseline).toLong() else 0L
        } else {
            0L
        }
        return (accumulatedBeforePause + activeDelta).coerceAtLeast(0L)
    }

    private fun availabilityMessage(): String? = when {
        !isSensorAvailable() -> "Phone step estimate is not available on this device. Watch steps may still sync through Health Connect."
        !hasRuntimePermission() -> "Movement Bonus can still use Health Connect. Watch steps may sync after the Flow."
        else -> null
    }

    private fun publish(isTracking: Boolean = tracking, message: String? = availabilityMessage()) {
        _state.value = currentState(isTracking = isTracking, message = message)
    }

    private fun currentState(isTracking: Boolean, message: String?): PhoneStepEstimateState {
        val steps = if (isSensorAvailable() && hasRuntimePermission()) estimatedSteps() else 0L
        return PhoneStepEstimateState(
            isAvailable = isSensorAvailable(),
            permissionGranted = hasRuntimePermission(),
            isTracking = isTracking,
            estimatedSteps = steps,
            estimatedMovementPoints = steps / 25L,
            source = source,
            message = message
        )
    }
}
