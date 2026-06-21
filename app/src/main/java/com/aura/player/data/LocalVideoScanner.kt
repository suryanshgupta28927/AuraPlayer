package com.aura.player.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/**
 * Scans the device's MediaStore for local video files.
 * Requires READ_MEDIA_VIDEO (API 33+) or READ_EXTERNAL_STORAGE permission,
 * which is requested at runtime from the UI layer before calling this.
 */
object LocalVideoScanner {

    fun scan(context: Context): List<Video> {
        val videos = mutableListOf<Video>()

        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection, projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)

                videos += Video(
                    id = id.toString(),
                    title = cursor.getString(titleCol) ?: "Unknown Video",
                    durationMs = cursor.getLong(durationCol),
                    uri = contentUri,
                    thumbnailUri = contentUri,
                    sizeBytes = cursor.getLong(sizeCol),
                    resolution = if (width > 0 && height > 0) "${width}x${height}" else ""
                )
            }
        }
        return videos
    }
}
