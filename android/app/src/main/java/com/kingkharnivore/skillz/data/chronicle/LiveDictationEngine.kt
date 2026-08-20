package com.kingkharnivore.skillz.data.chronicle

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface LiveDictationEngine {
    interface Listener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError()
    }

    fun isAvailable(): Boolean
    fun start(listener: Listener): Boolean
    fun finish()
    fun cancel()
}

/**
 * Uses Android's on-device recognizer when it can actually start. If the installed on-device
 * language model is missing or broken, the request is retried once with the system speech
 * service instead of leaving Mic unusable.
 */
@Singleton
class AndroidOnDeviceDictationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : LiveDictationEngine {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listener: LiveDictationEngine.Listener? = null
    private var attemptId = 0L
    private var usingOnDeviceRecognizer = false
    private var retriedWithSystemRecognizer = false
    private var finishRequested = false
    private var committedText = ""
    private var lastText = ""

    override fun isAvailable(): Boolean =
        isOnDeviceRecognizerAvailable() || SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(listener: LiveDictationEngine.Listener): Boolean {
        if (this.listener != null || recognizer != null || !isAvailable()) return false
        this.listener = listener
        retriedWithSystemRecognizer = false
        finishRequested = false
        committedText = ""
        lastText = ""
        val started = startAttempt(preferOnDevice = isOnDeviceRecognizerAvailable())
        if (!started) release()
        return started
    }

    override fun finish() {
        finishRequested = true
        val activeRecognizer = recognizer
        if (activeRecognizer == null) {
            completeFinish()
            return
        }
        val finishingAttempt = attemptId
        runCatching { activeRecognizer.stopListening() }
            .onFailure { completeFinish() }
            .onSuccess {
                mainHandler.postDelayed({
                    if (finishRequested && attemptId == finishingAttempt) completeFinish()
                }, FINISH_TIMEOUT_MS)
            }
    }

    override fun cancel() {
        runCatching { recognizer?.cancel() }
        release()
    }

    private fun startAttempt(preferOnDevice: Boolean): Boolean {
        val speech = createRecognizer(preferOnDevice)
            ?: if (preferOnDevice) return startAttempt(preferOnDevice = false) else return false
        val currentAttempt = ++attemptId
        usingOnDeviceRecognizer = preferOnDevice
        recognizer = speech
        val started = runCatching {
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onPartialResults(results: Bundle) {
                    if (currentAttempt == attemptId) deliverPartial(results)
                }

                override fun onResults(results: Bundle) {
                    if (currentAttempt == attemptId) handleSegmentResults(currentAttempt, results)
                }

                override fun onError(error: Int) {
                    if (currentAttempt == attemptId) handleError(currentAttempt, error)
                }

                override fun onReadyForSpeech(params: Bundle) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle) = Unit
            })
            speech.startListening(recognitionIntent(preferOffline = preferOnDevice))
        }.isSuccess
        if (!started) {
            destroyCurrentRecognizer()
            return if (preferOnDevice) startAttempt(preferOnDevice = false) else false
        }
        return true
    }

    private fun handleSegmentResults(currentAttempt: Long, results: Bundle) {
        if (currentAttempt != attemptId) return
        extractText(results)?.let { segment ->
            committedText = joinSpeech(committedText, segment)
            lastText = committedText
            listener?.onPartial(lastText)
        }
        if (finishRequested) completeFinish()
        else restartListening(usingOnDeviceRecognizer, SEGMENT_RESTART_DELAY_MS)
    }

    private fun handleError(currentAttempt: Long, error: Int) {
        if (currentAttempt != attemptId) return
        if (finishRequested) {
            completeFinish()
            return
        }
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
            error == SpeechRecognizer.ERROR_NO_MATCH) {
            restartListening(usingOnDeviceRecognizer, SILENCE_RESTART_DELAY_MS)
            return
        }
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            restartListening(usingOnDeviceRecognizer, BUSY_RESTART_DELAY_MS)
            return
        }
        val activeListener = listener
        val canRetry = usingOnDeviceRecognizer && !retriedWithSystemRecognizer &&
            SpeechRecognizer.isRecognitionAvailable(context)
        if (canRetry && activeListener != null) {
            retriedWithSystemRecognizer = true
            destroyCurrentRecognizer()
            mainHandler.post {
                if (listener === activeListener && recognizer == null && !finishRequested &&
                    !startAttempt(preferOnDevice = false)) {
                    activeListener.onError()
                    release()
                }
            }
        } else {
            activeListener?.onError()
            release()
        }
    }

    private fun deliverPartial(results: Bundle) {
        val segment = extractText(results) ?: return
        lastText = joinSpeech(committedText, segment)
        listener?.onPartial(lastText)
    }

    private fun restartListening(preferOnDevice: Boolean, delayMs: Long) {
        val activeListener = listener ?: return
        destroyCurrentRecognizer()
        mainHandler.postDelayed({
            if (listener === activeListener && recognizer == null && !finishRequested &&
                !startAttempt(preferOnDevice)) {
                activeListener.onError()
                release()
            }
        }, delayMs)
    }

    private fun extractText(results: Bundle): String? =
        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    private fun joinSpeech(prefix: String, text: String): String = when {
        prefix.isBlank() -> text.trim()
        text.isBlank() -> prefix.trim()
        else -> "${prefix.trimEnd()} ${text.trimStart()}"
    }

    private fun completeFinish() {
        val activeListener = listener
        val text = lastText
        try {
            activeListener?.onFinal(text)
        } finally {
            release()
        }
    }

    private fun createRecognizer(onDevice: Boolean): SpeechRecognizer? = runCatching {
        if (onDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }.getOrNull()

    private fun recognitionIntent(preferOffline: Boolean) =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, SILENCE_WINDOW_MS)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_WINDOW_MS)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                SILENCE_WINDOW_MS)
        }

    private fun isOnDeviceRecognizerAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    private fun destroyCurrentRecognizer() {
        ++attemptId
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun release() {
        destroyCurrentRecognizer()
        listener = null
        usingOnDeviceRecognizer = false
        retriedWithSystemRecognizer = false
        finishRequested = false
        committedText = ""
        lastText = ""
    }

    private companion object {
        const val FINISH_TIMEOUT_MS = 2_000L
        const val SEGMENT_RESTART_DELAY_MS = 120L
        const val SILENCE_RESTART_DELAY_MS = 180L
        const val BUSY_RESTART_DELAY_MS = 500L
        const val SILENCE_WINDOW_MS = 30_000L
    }
}

/** Kept as the repository-facing compatibility boundary while live Mic uses Android speech. */
@Singleton
class MlKitLiveDictationEngine @Inject constructor(
    private val platform: AndroidOnDeviceDictationEngine,
) : LiveDictationEngine {
    override fun isAvailable(): Boolean = platform.isAvailable()
    override fun start(listener: LiveDictationEngine.Listener): Boolean = platform.start(listener)
    override fun finish() = platform.finish()
    override fun cancel() = platform.cancel()
}
