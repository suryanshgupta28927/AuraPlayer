package com.aura.player.data

import android.net.Uri

/**
 * Represents a single playable video sourced from local device storage.
 */
data class Video(
    val id: String,
    val title: String,
    val durationMs: Long,
    val uri: Uri,
    val thumbnailUri: Uri? = null,
    val sizeBytes: Long = 0L,
    val resolution: String = ""
)
