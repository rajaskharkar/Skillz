package com.kingkharnivore.skillz.data.chronicle

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteOrder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface TranscriptionEngine {
    fun isSupported(): Boolean
    suspend fun transcribe(file: File, onPartial: (String) -> Unit): String
}

/**
 * Local-only ML Kit transcription. Compressed input is decoded in bounded chunks and streamed as
 * real-time 16 kHz mono PCM, which is the provider's required custom-audio contract.
 */
@Singleton
class MlKitTranscriptionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : TranscriptionEngine {
    override fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    override suspend fun transcribe(file: File, onPartial: (String) -> Unit): String {
        check(isSupported()) { "On-device transcription is unavailable" }
        check(file.isFile && file.length() > 0L) { "Voice source is unavailable" }
        val mlKitResult = runCatching { transcribeWithMlKit(file, onPartial) }
        mlKitResult.getOrNull()?.let { return it }
        val mlKitFailure = mlKitResult.exceptionOrNull()
        if (mlKitFailure is CancellationException) throw mlKitFailure
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                return transcribeWithAndroid(file, onPartial)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (platformFailure: Exception) {
                mlKitFailure?.let(platformFailure::addSuppressed)
                throw platformFailure
            }
        }
        throw mlKitFailure ?: IOException("Transcription is unavailable")
    }

    private suspend fun transcribeWithMlKit(file: File, onPartial: (String) -> Unit): String {
        val recognizer = SpeechRecognition.getClient(
            speechRecognizerOptions {
                locale = Locale.getDefault()
                preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
            }
        )
        try {
            prepareModel(recognizer)
            val pipe = ParcelFileDescriptor.createPipe()
            var committed = ""
            try {
                coroutineScope {
                    val writer = launch(Dispatchers.IO) {
                        ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                            LocalAudioPcmStreamer.stream(file, output)
                        }
                    }
                    try {
                        val request = speechRecognizerRequest { audioSource = AudioSource.fromPfd(pipe[0]) }
                        recognizer.startRecognition(request).collect { response ->
                            when (response) {
                                is SpeechRecognizerResponse.PartialTextResponse -> {
                                    onPartial(joinSpeech(committed, response.text))
                                }
                                is SpeechRecognizerResponse.FinalTextResponse -> {
                                    committed = joinSpeech(committed, response.text)
                                    onPartial(committed)
                                }
                                is SpeechRecognizerResponse.CompletedResponse -> Unit
                                is SpeechRecognizerResponse.ErrorResponse -> throw response.e
                            }
                        }
                        writer.join()
                    } finally {
                        writer.cancel()
                    }
                }
            } finally {
                pipe.forEach { runCatching { it.close() } }
            }
            return committed.trim().takeIf(String::isNotEmpty)
                ?: throw IOException("No speech was recognized")
        } finally {
            runCatching { recognizer.stopRecognition() }
            recognizer.close()
        }
    }

    private suspend fun prepareModel(
        recognizer: com.google.mlkit.genai.speechrecognition.SpeechRecognizer,
    ) {
        when (recognizer.checkStatus()) {
            FeatureStatus.AVAILABLE -> return
            FeatureStatus.DOWNLOADABLE,
            FeatureStatus.DOWNLOADING -> {
                when (val terminal = recognizer.download().first { status ->
                    status is DownloadStatus.DownloadCompleted ||
                        status is DownloadStatus.DownloadFailed
                }) {
                    DownloadStatus.DownloadCompleted -> Unit
                    is DownloadStatus.DownloadFailed -> throw terminal.e
                    else -> error("Unexpected speech model download state")
                }
                if (recognizer.checkStatus() != FeatureStatus.AVAILABLE) {
                    throw IOException("Speech model download did not become available")
                }
            }
            else -> throw UnsupportedOperationException("On-device speech model is unavailable")
        }
    }

    private suspend fun transcribeWithAndroid(file: File, onPartial: (String) -> Unit): String = coroutineScope {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        check(AndroidSpeechRecognizer.isRecognitionAvailable(context)) {
            "Android speech recognition is unavailable"
        }
        val recognizer = withContext(Dispatchers.Main.immediate) {
            if (AndroidSpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                AndroidSpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                AndroidSpeechRecognizer.createSpeechRecognizer(context)
            }
        }
        val pipe = ParcelFileDescriptor.createPipe()
        val completion = CompletableDeferred<String>()
        var committed = ""
        var latest = ""
        var writer: Job? = null

        fun resultText(results: Bundle): String =
            results.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()

        fun finishWith(value: String) {
            val text = value.trim()
            if (text.isNotEmpty()) completion.complete(text)
            else completion.completeExceptionally(IOException("No speech was recognized"))
        }

        try {
            withContext(Dispatchers.Main.immediate) {
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onPartialResults(results: Bundle) {
                        val partial = resultText(results)
                        if (partial.isNotEmpty()) {
                            latest = joinSpeech(committed, partial)
                            onPartial(latest)
                        }
                    }

                    override fun onSegmentResults(results: Bundle) {
                        val segment = resultText(results)
                        if (segment.isNotEmpty()) {
                            committed = mergeSpeech(committed, segment)
                            latest = committed
                            onPartial(committed)
                        }
                    }

                    override fun onResults(results: Bundle) {
                        val finalText = resultText(results)
                        finishWith(if (finalText.isEmpty()) latest else mergeSpeech(committed, finalText))
                    }

                    override fun onEndOfSegmentedSession() = finishWith(committed.ifEmpty { latest })

                    override fun onError(error: Int) {
                        if (committed.isNotBlank() || latest.isNotBlank()) {
                            finishWith(committed.ifEmpty { latest })
                        } else {
                            completion.completeExceptionally(IOException("Android transcription failed: $error"))
                        }
                    }

                    override fun onReadyForSpeech(params: Bundle) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onEvent(eventType: Int, params: Bundle) = Unit
                })
                recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,
                        AndroidSpeechRecognizer.isOnDeviceRecognitionAvailable(context))
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pipe[0])
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, 16_000)
                    putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE)
                })
            }
            writer = launch(Dispatchers.IO) {
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                        LocalAudioPcmStreamer.stream(file, output)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    completion.completeExceptionally(failure)
                }
            }
            completion.await()
        } finally {
            writer?.cancel()
            pipe.forEach { runCatching { it.close() } }
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                runCatching { recognizer.cancel() }
                recognizer.destroy()
            }
        }
    }

    private fun joinSpeech(prefix: String, text: String): String = when {
        prefix.isBlank() -> text.trim()
        text.isBlank() -> prefix.trim()
        else -> "${prefix.trimEnd()} ${text.trimStart()}"
    }

    private fun mergeSpeech(prefix: String, text: String): String = when {
        prefix.isBlank() -> text.trim()
        text.isBlank() -> prefix.trim()
        text.startsWith(prefix, ignoreCase = true) -> text.trim()
        prefix.endsWith(text, ignoreCase = true) -> prefix.trim()
        else -> joinSpeech(prefix, text)
    }
}

private object LocalAudioPcmStreamer {
    private const val TARGET_SAMPLE_RATE = 16_000
    private const val CODEC_TIMEOUT_US = 10_000L

    suspend fun stream(source: File, output: OutputStream) = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(source.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("No audio track")
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw IOException("Missing audio type")
            var sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            while (!outputEnded) {
                currentCoroutineContext().ensureActive()
                if (!inputEnded) {
                    val inputIndex = decoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val input = decoder.getInputBuffer(inputIndex) ?: throw IOException("Decoder input unavailable")
                        val size = extractor.readSampleData(input, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                when (val outputIndex = decoder.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = decoder.outputFormat
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        if (info.size > 0) {
                            val buffer = decoder.getOutputBuffer(outputIndex) ?: throw IOException("Decoder output unavailable")
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            val shorts = buffer.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val pcm = ShortArray(shorts.remaining())
                            shorts.get(pcm)
                            val converted = downmixAndResample(pcm, channelCount, sampleRate)
                            output.write(converted)
                            output.flush()
                            val outputFrames = converted.size / 2
                            delay((outputFrames * 1_000L / TARGET_SAMPLE_RATE).coerceAtLeast(1L))
                        }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
        } finally {
            runCatching { decoder?.stop() }
            decoder?.release()
            extractor.release()
        }
    }

    private fun downmixAndResample(input: ShortArray, channels: Int, sampleRate: Int): ByteArray {
        require(channels > 0 && sampleRate > 0)
        val inputFrames = input.size / channels
        val outputFrames = (inputFrames.toLong() * TARGET_SAMPLE_RATE / sampleRate).toInt()
        val output = ByteArray(outputFrames * 2)
        for (outIndex in 0 until outputFrames) {
            val sourceFrame = (outIndex.toLong() * sampleRate / TARGET_SAMPLE_RATE)
                .toInt().coerceIn(0, (inputFrames - 1).coerceAtLeast(0))
            var sum = 0
            for (channel in 0 until channels) sum += input[sourceFrame * channels + channel].toInt()
            val sample = (sum / channels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output[outIndex * 2] = (sample and 0xff).toByte()
            output[outIndex * 2 + 1] = (sample shr 8 and 0xff).toByte()
        }
        return output
    }
}
