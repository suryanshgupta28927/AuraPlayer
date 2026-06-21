package com.aura.player.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aura.player.data.Song
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Connects the Compose UI to the PlaybackService's MediaSession through a
 * MediaController, and republishes player state as StateFlow so screens
 * can collect it reactively. This is the single playback control surface
 * used by the whole app (mini player, full player, notification all stay
 * in sync because they all drive the same underlying session).
 */
class PlayerController(private val context: Context) {

    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private var songLookup: Map<String, Song> = emptyMap()

    fun connect(onReady: () -> Unit = {}) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            controller = future.get()
            attachListener()
            onReady()
        }, MoreExecutors.directExecutor())
    }

    private fun attachListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId
                _currentSong.value = id?.let { songLookup[it] }
                _durationMs.value = controller?.duration?.coerceAtLeast(0) ?: 0L
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _durationMs.value = controller?.duration?.coerceAtLeast(0) ?: 0L
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }

    /** Call periodically (e.g. every 500ms from a UI LaunchedEffect loop) to update the scrubber. */
    fun pollPosition() {
        controller?.let {
            _positionMs.value = it.currentPosition.coerceAtLeast(0)
            _durationMs.value = it.duration.coerceAtLeast(0)
        }
    }

    /** Loads a list of songs as the playback queue and starts playing at startIndex. */
    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        songLookup = songs.associateBy { it.id }
        _queue.value = songs

        val mediaItems = songs.map { song -> song.toMediaItem() }
        controller?.apply {
            setMediaItems(mediaItems, startIndex, 0L)
            prepare()
            play()
        }
        _currentSong.value = songs.getOrNull(startIndex)
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun skipToNext() = controller?.seekToNext()

    fun skipToPrevious() = controller?.seekToPrevious()

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun release() {
        controller?.release()
        controller = null
    }

    private fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .apply { artUri?.let { setArtworkUri(it) } }
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }
}
