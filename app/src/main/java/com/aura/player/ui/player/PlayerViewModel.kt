package com.aura.player.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.player.data.MusicRepository
import com.aura.player.data.Song
import com.aura.player.service.PlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = PlayerController(application)
    val repository = MusicRepository.get()

    val isPlaying: StateFlow<Boolean> = controller.isPlaying
    val currentSong: StateFlow<Song?> = controller.currentSong
    val positionMs: StateFlow<Long> = controller.positionMs
    val durationMs: StateFlow<Long> = controller.durationMs
    val queue: StateFlow<List<Song>> = controller.queue
    val shuffleEnabled: StateFlow<Boolean> = controller.shuffleEnabled
    val repeatMode: StateFlow<Int> = controller.repeatMode

    init {
        controller.connect {
            startPositionPolling()
        }
        repository.loadLocalLibrary(application)
    }

    private fun startPositionPolling() {
        viewModelScope.launch {
            while (true) {
                controller.pollPosition()
                delay(500)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int) = controller.playQueue(songs, startIndex)
    fun togglePlayPause() = controller.togglePlayPause()
    fun skipToNext() = controller.skipToNext()
    fun skipToPrevious() = controller.skipToPrevious()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)
    fun toggleShuffle() = controller.toggleShuffle()
    fun cycleRepeatMode() = controller.cycleRepeatMode()
    fun refreshLocalLibrary() = repository.loadLocalLibrary(getApplication())

    override fun onCleared() {
        controller.release()
        super.onCleared()
    }
}
