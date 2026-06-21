package com.aura.player.ui.video

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.aura.player.data.MusicRepository
import com.aura.player.data.Video
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Owns a dedicated ExoPlayer for local video playback. Kept separate from
 * the audio PlayerViewModel/PlaybackService since video playback is
 * in-app (full-screen activity UI), not a background media-session
 * notification use case like audio.
 */
class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MusicRepository.get()

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _currentVideo = MutableStateFlow<Video?>(null)
    val currentVideo: StateFlow<Video?> = _currentVideo

    private var queue: List<Video> = emptyList()
    private var currentIndex: Int = 0

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                _durationMs.value = player.duration.coerceAtLeast(0)
            }
        })
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                _positionMs.value = player.currentPosition.coerceAtLeast(0)
                _durationMs.value = player.duration.coerceAtLeast(0)
                delay(500)
            }
        }
    }

    fun playQueue(videos: List<Video>, startIndex: Int) {
        if (videos.isEmpty()) return
        queue = videos
        currentIndex = startIndex
        _currentVideo.value = videos[startIndex]
        player.setMediaItem(MediaItem.fromUri(videos[startIndex].uri))
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun playNext() {
        if (queue.isEmpty()) return
        currentIndex = (currentIndex + 1) % queue.size
        playQueue(queue, currentIndex)
    }

    fun playPrevious() {
        if (queue.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) queue.size - 1 else currentIndex - 1
        playQueue(queue, currentIndex)
    }

    fun loadLocalVideos() = repository.loadLocalVideos(getApplication())

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
