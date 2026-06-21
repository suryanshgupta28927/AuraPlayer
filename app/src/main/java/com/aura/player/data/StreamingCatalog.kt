package com.aura.player.data

import android.net.Uri

/**
 * Demo streaming catalog using royalty-free tracks so the streaming path
 * is testable out of the box. Replace with your own backend / API call
 * (Retrofit, Firebase, etc.) to power real streaming search.
 */
object StreamingCatalog {

    fun sampleTracks(): List<Song> = listOf(
        Song(
            id = "stream_1",
            title = "Midnight Drive",
            artist = "Aura Sessions",
            album = "Neon Nights",
            durationMs = 0L, // unknown until loaded
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
            artUri = null,
            isStreaming = true
        ),
        Song(
            id = "stream_2",
            title = "Glass City",
            artist = "Aura Sessions",
            album = "Neon Nights",
            durationMs = 0L,
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
            artUri = null,
            isStreaming = true
        ),
        Song(
            id = "stream_3",
            title = "Violet Hour",
            artist = "Aura Sessions",
            album = "Neon Nights",
            durationMs = 0L,
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
            artUri = null,
            isStreaming = true
        ),
        Song(
            id = "stream_4",
            title = "Static Bloom",
            artist = "Aura Sessions",
            album = "Neon Nights",
            durationMs = 0L,
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
            artUri = null,
            isStreaming = true
        )
    )

    /** Simple title/artist text search across the demo catalog. */
    fun search(query: String): List<Song> {
        if (query.isBlank()) return sampleTracks()
        val q = query.trim().lowercase()
        return sampleTracks().filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
        }
    }
}
