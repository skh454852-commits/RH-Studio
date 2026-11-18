package com.example.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.SocialPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class AudioPlaybackState(
    val activeTrack: SocialPost? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val hasLocalAudioSource: Boolean = false,
    val statusNotice: String? = null
)

class AudioPlayerManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build()

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) startProgressPolling() else progressJob?.cancel()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val duration = exoPlayer.duration.takeIf { it > 0L }
                    ?: ((_state.value.activeTrack?.durationSeconds ?: 0L) * 1000L)
                _state.value = _state.value.copy(durationMs = duration)
            }
        })
    }

    fun playTrack(post: SocialPost) {
        val file = post.localMediaPath.takeIf { it.isNotBlank() }?.let(::File)
        if (file != null && file.exists()) {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            _state.value = _state.value.copy(
                activeTrack = post,
                isPlaying = true,
                positionMs = 0L,
                durationMs = (post.durationSeconds * 1000L).coerceAtLeast(1000L),
                hasLocalAudioSource = true,
                statusNotice = null
            )
        } else {
            exoPlayer.stop()
            _state.value = _state.value.copy(
                activeTrack = post,
                isPlaying = false,
                positionMs = 0L,
                durationMs = (post.durationSeconds * 1000L).coerceAtLeast(1000L),
                hasLocalAudioSource = false,
                statusNotice = "Audio file is stored on the creator's device; upload/record locally to stream."
            )
        }
    }

    fun togglePlayPause() {
        val current = _state.value
        if (!current.hasLocalAudioSource) return
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        if (!_state.value.hasLocalAudioSource) return
        val clamped = positionMs.coerceIn(0L, _state.value.durationMs.coerceAtLeast(1L))
        exoPlayer.seekTo(clamped)
        _state.value = _state.value.copy(positionMs = clamped)
    }

    fun seekBy(deltaMs: Long) {
        seekTo(_state.value.positionMs + deltaMs)
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        exoPlayer.volume = v
        _state.value = _state.value.copy(volume = v)
    }

    fun stop() {
        progressJob?.cancel()
        exoPlayer.stop()
        _state.value = AudioPlaybackState(volume = _state.value.volume)
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (exoPlayer.isPlaying) {
                val dur = exoPlayer.duration.takeIf { it > 0L } ?: _state.value.durationMs
                _state.value = _state.value.copy(
                    positionMs = exoPlayer.currentPosition,
                    durationMs = dur
                )
                delay(250L)
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        exoPlayer.release()
    }
}
