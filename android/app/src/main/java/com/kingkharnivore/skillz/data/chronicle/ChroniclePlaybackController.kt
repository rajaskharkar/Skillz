package com.kingkharnivore.skillz.data.chronicle

import android.content.Context
import android.net.Uri
import android.view.TextureView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ChroniclePlaybackState(
    val activeSourceId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasError: Boolean = false,
)

/** A single playback authority shared by every video and Voice row in one Chronicle surface. */
class ChroniclePlaybackController(context: Context) : Player.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player = ExoPlayer.Builder(context.applicationContext).build().apply {
        setAudioAttributes(AudioAttributes.DEFAULT, true)
        addListener(this@ChroniclePlaybackController)
    }
    private val _state = MutableStateFlow(ChroniclePlaybackState())
    val state: StateFlow<ChroniclePlaybackState> = _state.asStateFlow()
    private var ticker: Job? = null
    private var released = false
    private var boundVideoView: TextureView? = null

    fun prepare(sourceId: String, file: File, mimeType: String? = null) {
        if (released || !file.isFile) {
            _state.value = _state.value.copy(hasError = true)
            return
        }
        if (_state.value.activeSourceId == sourceId) return
        setSource(sourceId, file, mimeType, playWhenReady = false)
    }

    fun toggle(sourceId: String, file: File, mimeType: String? = null) {
        if (released || !file.isFile) {
            _state.value = _state.value.copy(hasError = true)
            return
        }
        if (_state.value.activeSourceId == sourceId) {
            if (player.isPlaying) player.pause() else player.play()
            return
        }
        setSource(sourceId, file, mimeType, playWhenReady = true)
    }

    private fun setSource(sourceId: String, file: File, mimeType: String?, playWhenReady: Boolean) {
        val mediaItem = MediaItem.Builder()
            .setMediaId(sourceId)
            .setUri(Uri.fromFile(file))
            .apply { mimeType?.takeIf(String::isNotBlank)?.let(::setMimeType) }
            .build()
        player.setMediaItem(
            mediaItem
        )
        _state.value = ChroniclePlaybackState(activeSourceId = sourceId)
        player.playWhenReady = playWhenReady
        player.prepare()
        ensureTicker()
    }

    fun play(sourceId: String, file: File, mimeType: String? = null) {
        if (_state.value.activeSourceId == sourceId && player.isPlaying) return
        toggle(sourceId, file, mimeType)
    }

    fun pause() {
        if (!released) player.pause()
    }

    fun seekTo(sourceId: String, positionMs: Long) {
        if (!released && _state.value.activeSourceId == sourceId) {
            player.seekTo(positionMs.coerceAtLeast(0L))
            publishState()
        }
    }

    fun stop() {
        if (released) return
        player.stop()
        player.clearMediaItems()
        _state.value = ChroniclePlaybackState()
    }

    fun bindVideo(view: TextureView) {
        if (released || boundVideoView === view) return
        boundVideoView?.let(player::clearVideoTextureView)
        boundVideoView = view
        player.setVideoTextureView(view)
    }

    fun unbindVideo(view: TextureView) {
        if (released || boundVideoView !== view) return
        player.clearVideoTextureView(view)
        boundVideoView = null
    }

    fun release() {
        if (released) return
        released = true
        ticker?.cancel()
        boundVideoView = null
        player.removeListener(this)
        player.release()
        scope.cancel()
        _state.value = ChroniclePlaybackState()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) = publishState()

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            player.pause()
            player.seekTo(0L)
        }
        publishState()
    }

    override fun onPlayerError(error: PlaybackException) {
        _state.value = _state.value.copy(isPlaying = false, hasError = true)
    }

    private fun ensureTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive && !released) {
                publishState()
                delay(200L)
            }
        }
    }

    private fun publishState() {
        if (released) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L } ?: 0L
        _state.value = _state.value.copy(
            activeSourceId = player.currentMediaItem?.mediaId,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            hasError = player.playerError != null,
        )
    }
}
