package com.aura.player.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for the music library (local + streaming).
 * Lightweight in-memory repository — swap the streaming half for a real
 * API/Retrofit service when you have a backend.
 */
class MusicRepository private constructor() {

    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs

    private val _streamingSongs = MutableStateFlow<List<Song>>(StreamingCatalog.sampleTracks())
    val streamingSongs: StateFlow<List<Song>> = _streamingSongs

    private val _localVideos = MutableStateFlow<List<Video>>(emptyList())
    val localVideos: StateFlow<List<Video>> = _localVideos

    fun loadLocalVideos(context: Context) {
        _localVideos.value = LocalVideoScanner.scan(context)
    }

    /** Playlists keyed by name -> ordered list of song ids. */
    private val _playlists = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val playlists: StateFlow<Map<String, List<String>>> = _playlists

    fun allSongs(): List<Song> = _localSongs.value + _streamingSongs.value

    fun loadLocalLibrary(context: Context) {
        _localSongs.value = LocalMusicScanner.scan(context)
    }

    fun search(query: String): List<Song> {
        if (query.isBlank()) return allSongs()
        val q = query.trim().lowercase()
        return allSongs().filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
    }

    fun createPlaylist(name: String) {
        if (_playlists.value.containsKey(name)) return
        _playlists.value = _playlists.value + (name to emptyList())
    }

    fun addToPlaylist(playlistName: String, songId: String) {
        val current = _playlists.value[playlistName] ?: return
        if (current.contains(songId)) return
        _playlists.value = _playlists.value + (playlistName to (current + songId))
    }

    fun removeFromPlaylist(playlistName: String, songId: String) {
        val current = _playlists.value[playlistName] ?: return
        _playlists.value = _playlists.value + (playlistName to current.filter { it != songId })
    }

    fun songsInPlaylist(playlistName: String): List<Song> {
        val ids = _playlists.value[playlistName] ?: return emptyList()
        val byId = allSongs().associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    companion object {
        @Volatile private var instance: MusicRepository? = null

        fun get(): MusicRepository =
            instance ?: synchronized(this) {
                instance ?: MusicRepository().also { instance = it }
            }
    }
}
