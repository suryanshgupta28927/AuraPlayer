package com.aura.player.downloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 🎬 YouTube Downloader — Kotlin port of yt_downloader.py
 *
 * Uses yt-dlp's JSON API (via a public proxy/extractor service) to fetch
 * video metadata + direct download URLs, then delegates actual downloading
 * to Android's DownloadManager (mirrors yt-dlp's two-step: info → download).
 *
 * NOTE: For production, embed yt-dlp binary via NDK or use a trusted
 * backend. This implementation uses the public cobalt.tools API which
 * is free and doesn't require an API key.
 */
object YtDownloaderHelper {

    // ── Data classes ────────────────────────────────────────────────────────

    data class VideoInfo(
        val title: String,
        val uploader: String,
        val durationSec: Long,
        val viewCount: Long,
        val uploadDate: String,
        val availableQualities: List<String>,
        val thumbnailUrl: String?
    )

    data class DownloadResult(
        val success: Boolean,
        val downloadId: Long = -1L,
        val errorMessage: String = ""
    )

    enum class DownloadMode { VIDEO, AUDIO }

    data class QualityOption(val label: String, val value: String)

    val videoQualities = listOf(
        QualityOption("Best (Max)", "best"),
        QualityOption("4K (2160p)", "4k"),
        QualityOption("1080p", "1080"),
        QualityOption("720p", "720"),
        QualityOption("480p", "480"),
        QualityOption("360p", "360")
    )

    val audioQualities = listOf(
        QualityOption("320 kbps", "320"),
        QualityOption("192 kbps", "192"),
        QualityOption("128 kbps", "128")
    )

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Fetch video info without downloading — mirrors get_info() in Python script.
     * Returns null on failure.
     */
    suspend fun getInfo(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val videoId = extractVideoId(url) ?: error("Invalid YouTube URL")
            val apiUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", "AuraPlayer/1.0")

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(response)
            val title = json.optString("title", "Unknown Title")
            val uploader = json.optString("author_name", "Unknown")

            VideoInfo(
                title = title,
                uploader = uploader,
                durationSec = 0L,   // oembed doesn't expose duration
                viewCount = 0L,
                uploadDate = "",
                availableQualities = listOf("1080", "720", "480", "360"),
                thumbnailUrl = json.optString("thumbnail_url").takeIf { it.isNotBlank() }
            )
        }
    }

    /**
     * Download video or audio — mirrors download_video() / download_audio() in Python.
     * Uses Android DownloadManager for background downloading with system notifications.
     */
    suspend fun download(
        context: Context,
        url: String,
        mode: DownloadMode,
        quality: String
    ): DownloadResult = withContext(Dispatchers.IO) {
        runCatching {
            val videoId = extractVideoId(url) ?: return@runCatching DownloadResult(
                success = false, errorMessage = "Invalid YouTube URL"
            )

            // Resolve actual download URL via cobalt.tools (free yt-dlp-backed API)
            val downloadUrl = resolveDownloadUrl(url, mode, quality)
                ?: return@runCatching DownloadResult(
                    success = false, errorMessage = "Could not resolve download URL.\nCheck internet connection."
                )

            // Queue with DownloadManager (mirrors yt-dlp's download step)
            val fileName = when (mode) {
                DownloadMode.VIDEO -> "aura_${videoId}_${quality}p.mp4"
                DownloadMode.AUDIO -> "aura_${videoId}_${quality}kbps.mp3"
            }
            val subDir = when (mode) {
                DownloadMode.VIDEO -> Environment.DIRECTORY_MOVIES
                DownloadMode.AUDIO -> Environment.DIRECTORY_MUSIC
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle(fileName)
                setDescription("Downloading via Aura Player")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(subDir, "AuraPlayer/$fileName")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
                addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AuraPlayer/1.0")
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)
            DownloadResult(success = true, downloadId = downloadId)

        }.getOrElse {
            DownloadResult(success = false, errorMessage = it.message ?: "Unknown error")
        }
    }

    /**
     * Poll DownloadManager for progress (0–100) and status.
     * Returns Pair(progress%, statusString)
     */
    fun queryProgress(context: Context, downloadId: Long): Pair<Int, String> {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = dm.query(query)

        if (!cursor.moveToFirst()) {
            cursor.close()
            return 0 to "Pending..."
        }

        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        cursor.close()

        val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0

        val statusStr = when (status) {
            DownloadManager.STATUS_PENDING   -> "Queued..."
            DownloadManager.STATUS_RUNNING   -> "$progress% — ${formatBytes(downloaded)} / ${formatBytes(total)}"
            DownloadManager.STATUS_PAUSED    -> "Paused ($progress%)"
            DownloadManager.STATUS_SUCCESSFUL -> "✅ Done!"
            DownloadManager.STATUS_FAILED    -> "❌ Failed"
            else -> "Unknown"
        }

        return progress to statusStr
    }

    fun isCompleted(context: Context, downloadId: Long): Boolean {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = dm.query(query)
        if (!cursor.moveToFirst()) { cursor.close(); return false }
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        cursor.close()
        return status == DownloadManager.STATUS_SUCCESSFUL
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun extractVideoId(url: String): String? {
        // Handles youtu.be/ID and youtube.com/watch?v=ID and shorts
        val patterns = listOf(
            Regex("youtu\\.be/([A-Za-z0-9_-]{11})"),
            Regex("[?&]v=([A-Za-z0-9_-]{11})"),
            Regex("shorts/([A-Za-z0-9_-]{11})")
        )
        for (p in patterns) {
            val match = p.find(url)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    /**
     * Resolves a direct streamable/downloadable URL via cobalt.tools API.
     * cobalt is an open-source, free media downloader API backed by yt-dlp.
     * Docs: https://cobalt.tools
     */
    private fun resolveDownloadUrl(url: String, mode: DownloadMode, quality: String): String? {
        return try {
            val apiUrl = "https://api.cobalt.tools/api/json"
            val body = JSONObject().apply {
                put("url", url)
                put("vCodec", "h264")
                put("vQuality", when (quality) {
                    "4k"   -> "2160"
                    "best" -> "max"
                    else   -> quality
                })
                put("aFormat", "mp3")
                put("isAudioOnly", mode == DownloadMode.AUDIO)
                put("disableMetadata", false)
            }.toString()

            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.outputStream.write(body.toByteArray())

            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(response)
            val status = json.optString("status")

            when (status) {
                "stream", "redirect" -> json.optString("url").takeIf { it.isNotBlank() }
                "picker" -> {
                    // Multiple streams — pick first
                    val picker = json.optJSONArray("picker")
                    picker?.getJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }
    }
}
