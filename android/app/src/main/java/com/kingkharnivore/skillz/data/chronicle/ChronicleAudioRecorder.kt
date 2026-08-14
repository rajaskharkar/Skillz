package com.kingkharnivore.skillz.data.chronicle

import android.media.MediaRecorder
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Exactly-once local recorder. Room is not touched until [stop] finalizes the file. */
@Singleton
class ChronicleAudioRecorder @Inject constructor(private val fileStore: ChronicleFileStore) {
    data class Recording internal constructor(internal val file: File, internal val recorder: MediaRecorder)

    @Synchronized
    fun start(): Recording {
        check(active == null) { "A Chronicle recording is already active" }
        val file = fileStore.createVoiceStaging()
        @Suppress("DEPRECATION")
        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.path)
            prepare()
            start()
        }
        return Recording(file, recorder).also { active = it }
    }

    fun amplitude(): Int = synchronized(this) { active?.recorder?.maxAmplitude ?: 0 }

    suspend fun stop(chronicleId: String): ChronicleFileStore.StoredFile {
        val recording = synchronized(this) { active?.also { active = null } }
            ?: error("No Chronicle recording is active")
        return try {
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
}
