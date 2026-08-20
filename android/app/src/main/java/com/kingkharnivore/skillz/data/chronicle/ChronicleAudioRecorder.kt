package com.kingkharnivore.skillz.data.chronicle

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

/** Exactly-once local recorder. Room is not touched until [stop] finalizes the file. */
@Singleton
class ChronicleAudioRecorder @Inject constructor(
    private val fileStore: ChronicleFileStore,
    @ApplicationContext private val context: Context,
) {
    data class Recording internal constructor(
        internal val file: File,
        internal val recorder: MediaRecorder,
        internal val startedAtMs: Long,
    )

    @Synchronized
    fun start(): Recording {
        check(active == null) { "A Chronicle recording is already active" }
        val file = fileStore.createVoiceStaging()
        @Suppress("DEPRECATION")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else MediaRecorder()
        return try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.path)
                prepare()
                start()
            }
            Recording(file, recorder, SystemClock.elapsedRealtime()).also { active = it }
        } catch (failure: Exception) {
            runCatching { recorder.release() }
            file.parentFile?.deleteRecursively()
            throw failure
        }
    }

    fun amplitude(): Int = synchronized(this) { active?.recorder?.maxAmplitude ?: 0 }

    suspend fun stop(chronicleId: String): ChronicleFileStore.StoredFile {
        val recording = synchronized(this) { active?.also { active = null } }
            ?: error("No Chronicle recording is active")
        return try {
            val remainingMs = MIN_RECORDING_DURATION_MS -
                (SystemClock.elapsedRealtime() - recording.startedAtMs)
            if (remainingMs > 0) delay(remainingMs)
            recording.recorder.stop()
            fileStore.finalizeVoice(chronicleId, recording.file)
        } catch (failure: Exception) {
            recording.file.parentFile?.deleteRecursively()
            throw failure
        } finally {
            recording.recorder.release()
        }
    }

    @Synchronized
    fun discard() {
        val recording = active ?: return
        active = null
        runCatching { recording.recorder.stop() }
        recording.recorder.release()
        recording.file.parentFile?.deleteRecursively()
    }

    private var active: Recording? = null

    private companion object {
        const val MIN_RECORDING_DURATION_MS = 900L
    }
}
