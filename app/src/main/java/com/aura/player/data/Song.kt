package com.aura.player.data

import android.net.Uri

/**
 * Represents a single playable track, whether sourced from local device
 * storage or a remote stream URL.
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val artUri: Uri? = null,
    val isStreaming: Boolean = false
)
