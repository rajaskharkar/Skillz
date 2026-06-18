package com.kingkharnivore.skillz.ui.screen.shell.rooms.focus

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import java.util.UUID

class FocusExerciseVoiceGuide(
    context: Context,
    private val onReadyChanged: (Boolean) -> Unit = {},
    private val onError: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var isReady: Boolean = false
    private var isInitializing: Boolean = false

    init {
        initialize()
    }

    override fun onInit(status: Int) {
        isInitializing = false

        if (status != TextToSpeech.SUCCESS) {
            isReady = false
            postReady(false)
            postError(
                "Voice guidance is not available yet. Check your device Text-to-speech settings or use text-only guidance."
            )
            return
        }

        val languageResult = tts?.setLanguage(Locale.US)

        val languageReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                languageResult != TextToSpeech.LANG_NOT_SUPPORTED

        if (!languageReady) {
            isReady = false
            postReady(false)
            postError(
                "English voice guidance is unavailable on this device. You can still follow the text prompts."
            )
            return
        }

        chooseGentlestAvailableVoice()
        tts?.setSpeechRate(0.68f)
        tts?.setPitch(0.94f)

        isReady = true
        postReady(true)
    }

    fun retry() {
        stop()
        shutdownInternal()
        initialize()
    }

    fun speak(text: String, flush: Boolean = true): Boolean {
        if (!isReady || tts == null) {
            postError(
                "Voice guidance is not ready. Check Text-to-speech settings or follow the text prompts."
            )
            return false
        }

        val queueMode = if (flush) {
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }

        val params = Bundle()
        val utteranceId = UUID.randomUUID().toString()

        val softenedText = text.toGentleTtsText()

        val result = tts?.speak(
            softenedText,
            queueMode,
            params,
            utteranceId
        )

        return if (result == TextToSpeech.ERROR) {
            postError("Voice guidance could not play this step.")
            false
        } else {
            true
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        stop()
        shutdownInternal()
    }

    private fun initialize() {
        if (isInitializing) return

        isInitializing = true
        isReady = false
        postReady(false)

        try {
            tts = TextToSpeech(appContext, this)
        } catch (_: Throwable) {
            isInitializing = false
            isReady = false
            postReady(false)
            postError(
                "Voice guidance could not start. Check your device Text-to-speech settings or use text-only guidance."
            )
        }
    }

    private fun shutdownInternal() {
        tts?.shutdown()
        tts = null
        isReady = false
        isInitializing = false
        postReady(false)
    }

    private fun chooseGentlestAvailableVoice() {
        val engine = tts ?: return
        val voices = engine.voices ?: return

        val installedVoices = voices.filter { voice ->
            voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
        }

        if (installedVoices.isEmpty()) return

        /**
         * Android/Google TTS voice names vary by device.
         * These are common Google UK English voice-name patterns.
         *
         * Android does not expose "female" reliably, so this is a best-effort
         * preference list. If none exist on the device, we fall back safely.
         */
        val preferredUkVoiceNameFragments = listOf(
            "en-gb-x-gba", // often available on Google TTS
            "en-gb-x-gbb",
            "en-gb-x-rjs",
            "en-gb",
            "gb"
        )

        val ukEnglishVoices = installedVoices.filter { voice ->
            voice.locale.language == Locale.ENGLISH.language &&
                    voice.locale.country.equals("GB", ignoreCase = true)
        }

        val preferredUkVoice = preferredUkVoiceNameFragments
            .asSequence()
            .mapNotNull { fragment ->
                ukEnglishVoices.firstOrNull { voice ->
                    voice.name.contains(fragment, ignoreCase = true)
                }
            }
            .firstOrNull()

        val fallbackUkVoice = ukEnglishVoices
            .sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenBy { it.latency }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenBy { it.name.lowercase() }
            )
            .firstOrNull()

        val fallbackAnyEnglishVoice = installedVoices
            .filter { voice -> voice.locale.language == Locale.ENGLISH.language }
            .sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenBy { it.latency }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenBy { it.name.lowercase() }
            )
            .firstOrNull()

        val selectedVoice = preferredUkVoice
            ?: fallbackUkVoice
            ?: fallbackAnyEnglishVoice
            ?: return

        engine.setVoice(selectedVoice)
    }

    private fun String.toGentleTtsText(): String {
        return this
            .replace("Welcome to", "Let's begin")
            .replace("We will", "We'll")
            .replace("Do not", "Don't")
            .replace("You are here.", "You're here.")
            .replace(". ", ".  ")
            .replace("? ", "?  ")
            .replace("! ", "!  ")
            .trim()
    }

    private fun postReady(ready: Boolean) {
        mainHandler.post {
            onReadyChanged(ready)
        }
    }

    private fun postError(message: String) {
        mainHandler.post {
            onError(message)
        }
    }
}