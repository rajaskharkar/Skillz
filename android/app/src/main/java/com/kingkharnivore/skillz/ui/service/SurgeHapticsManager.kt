package com.kingkharnivore.skillz.ui.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SurgeHapticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun playArmed() {
        vibrateWaveform(
            longArrayOf(0, 110, 60, 110),
            intArrayOf(0, 220, 0, 240)
        )
    }

    fun playStarted() {
        vibrateWaveform(
            longArrayOf(0, 120, 80, 140),
            intArrayOf(0, 230, 0, 255)
        )
    }

    fun playMidpoint() {
        vibrateOneShot(220, 255) // long buzz
    }

    fun playFiveMinutesLeft() {
        vibrateWaveform(
            longArrayOf(0, 140, 80, 140),
            intArrayOf(0, 230, 0, 255)
        )
    }

    fun playTwoMinutesLeft() {
        vibrateWaveform(
            longArrayOf(0, 160, 70, 160, 70, 160),
            intArrayOf(0, 220, 0, 240, 0, 255)
        )
    }

    fun playOneMinuteLeft() {
        vibrateWaveform(
            longArrayOf(0, 180, 60, 180, 60, 180),
            intArrayOf(0, 240, 0, 255, 0, 255)
        )
    }

    fun playTenSecondsLeft() {
        vibrateWaveform(
            longArrayOf(0, 120, 50, 120),
            intArrayOf(0, 255, 0, 255)
        )
    }

    fun playCountdownTick(secondsRemaining: Int) {
        val duration = when (secondsRemaining) {
            5 -> 90L
            4 -> 100L
            3 -> 115L
            2 -> 130L
            else -> 150L
        }

        val amplitude = when (secondsRemaining) {
            5 -> 210
            4 -> 225
            3 -> 240
            2 -> 255
            else -> 255
        }

        vibrateOneShot(duration, amplitude)
    }

    fun playTargetReached() {
        cancel()
        vibrateWaveform(
            longArrayOf(0, 300, 120, 300),
            intArrayOf(0, 255, 0, 255)
        )
    }

    fun playCompletedSuccess() {
        vibrateWaveform(
            longArrayOf(0, 160, 60, 120, 60, 160),
            intArrayOf(0, 255, 0, 220, 0, 255)
        )
    }

    fun playCompletedFail() {
        vibrateWaveform(
            longArrayOf(0, 120, 100, 120),
            intArrayOf(0, 160, 0, 160)
        )
    }

    fun cancel() {
        try { vibrator?.cancel() } catch (_: Throwable) {}
    }

    private fun vibrateOneShot(duration: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(
                    VibrationEffect.createOneShot(duration, amplitude.coerceIn(1, 255))
                )
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration)
            }
        } catch (_: Throwable) {}
    }

    private fun vibrateWaveform(
        timings: LongArray,
        amplitudes: IntArray
    ) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(timings.sum())
            }
        } catch (_: Throwable) {}
    }
}