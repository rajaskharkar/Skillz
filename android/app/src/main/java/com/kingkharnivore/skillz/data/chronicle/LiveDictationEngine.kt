package com.kingkharnivore.skillz.data.chronicle

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Local-only streaming speech boundary used by Chronicle. */
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

/** Uses only Android's explicitly on-device recognizer; there is no network recognizer fallback. */
@Singleton
class AndroidOnDeviceDictationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : LiveDictationEngine {
    private var recognizer: SpeechRecognizer? = null
    private var listener: LiveDictationEngine.Listener? = null

    override fun isAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    override fun start(listener: LiveDictationEngine.Listener): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            !SpeechRecognizer.isOnDeviceRecognitionAvailable(context) || recognizer != null) return false
        this.listener = listener
        recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also { speech ->
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onPartialResults(results: Bundle) = deliver(results, final = false)
                override fun onResults(results: Bundle) = deliver(results, final = true)
                override fun onError(error: Int) {
                    this@AndroidOnDeviceDictationEngine.listener?.onError()
                    release()
                }
                override fun onReadyForSpeech(params: Bundle) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle) = Unit
            })
            speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            })
        }
        return true
    }

    override fun finish() { recognizer?.stopListening() }

    override fun cancel() {
        recognizer?.cancel()
        release()
    }

    private fun deliver(results: Bundle, final: Boolean) {
        val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
        if (final) {
            listener?.onFinal(text)
            release()
        } else listener?.onPartial(text)
    }

    private fun release() {
        recognizer?.destroy()
        recognizer = null
        listener = null
    }
}
