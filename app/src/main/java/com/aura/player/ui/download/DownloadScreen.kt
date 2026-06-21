package com.aura.player.ui.download

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aura.player.downloader.YtDownloaderHelper
import com.aura.player.downloader.YtDownloaderHelper.DownloadMode
import com.aura.player.ui.theme.*

@Composable
fun DownloadScreen(viewModel: DownloadViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Header ──────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Download,
                contentDescription = null,
                tint = AuraAccent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Downloader",
                color = AuraText,
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Text(
            "Download YouTube videos & audio",
            color = AuraMuted,
            fontSize = 13.sp
        )

        // ── URL Input ────────────────────────────────────────────────────────
        OutlinedTextField(
            value = state.url,
            onValueChange = viewModel::onUrlChange,
            placeholder = { Text("Paste YouTube URL here...", color = AuraMuted) },
            leadingIcon = {
                Icon(Icons.Filled.Link, contentDescription = null, tint = AuraMuted)
            },
            trailingIcon = {
                if (state.url.isNotBlank()) {
                    IconButton(onClick = { viewModel.onUrlChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = AuraMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AuraAccent,
                unfocusedBorderColor = AuraCard,
                focusedTextColor = AuraText,
                unfocusedTextColor = AuraText,
                cursorColor = AuraAccent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Fetch Info Button ────────────────────────────────────────────────
        Button(
            onClick = viewModel::fetchInfo,
            enabled = state.url.isNotBlank() && !state.isLoadingInfo && !state.isDownloading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuraCard,
                contentColor = AuraText,
                disabledContainerColor = AuraCard.copy(alpha = 0.4f)
            )
        ) {
            if (state.isLoadingInfo) {
                CircularProgressIndicator(color = AuraAccent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Fetching info...")
            } else {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Get Video Info")
            }
        }

        // ── Error: info fetch ────────────────────────────────────────────────
        if (state.infoError != null) {
            ErrorCard(state.infoError!!)
        }

        // ── Video Info Card ──────────────────────────────────────────────────
        if (state.info != null) {
            VideoInfoCard(info = state.info!!)
        }

        // ── Mode Toggle ──────────────────────────────────────────────────────
        ModeSelector(
            selected = state.mode,
            onSelect = viewModel::onModeChange
        )

        // ── Quality Selector ─────────────────────────────────────────────────
        val qualityOptions = if (state.mode == DownloadMode.VIDEO)
            YtDownloaderHelper.videoQualities
        else
            YtDownloaderHelper.audioQualities

        Text("Quality", color = AuraMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        QualitySelector(
            options = qualityOptions,
            selected = state.selectedQuality,
            onSelect = viewModel::onQualityChange
        )

        // ── Download Button ──────────────────────────────────────────────────
        Button(
            onClick = viewModel::startDownload,
            enabled = state.url.isNotBlank() && !state.isDownloading && !state.downloadDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuraAccent,
                contentColor = Color.Black,
                disabledContainerColor = AuraAccent.copy(alpha = 0.3f),
                disabledContentColor = Color.Black.copy(alpha = 0.4f)
            )
        ) {
            Icon(
                if (state.mode == DownloadMode.VIDEO) Icons.Filled.VideoFile else Icons.Filled.AudioFile,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.mode == DownloadMode.VIDEO) "Download MP4" else "Download MP3",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // ── Progress ─────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.isDownloading || state.downloadDone || state.downloadError != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DownloadProgressCard(
                isDownloading = state.isDownloading,
                isDone = state.downloadDone,
                error = state.downloadError,
                progress = state.progress,
                statusText = state.statusText,
                onReset = viewModel::reset
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun VideoInfoCard(info: YtDownloaderHelper.VideoInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AuraCard)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            if (info.thumbnailUrl != null) {
                AsyncImage(
                    model = info.thumbnailUrl,
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AuraSurface)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(info.title, color = AuraText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text("🎤 ${info.uploader}", color = AuraMuted, fontSize = 12.sp)
                if (info.durationSec > 0) {
                    val min = info.durationSec / 60
                    val sec = info.durationSec % 60
                    Text("⏱ ${min}m ${sec}s", color = AuraMuted, fontSize = 12.sp)
                }
                if (info.viewCount > 0) {
                    Text("👁 ${"%,d".format(info.viewCount)} views", color = AuraMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(selected: DownloadMode, onSelect: (DownloadMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuraCard),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DownloadMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(mode) }
                    .background(
                        if (isSelected) AuraAccent.copy(alpha = 0.15f) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (mode == DownloadMode.VIDEO) Icons.Filled.VideoFile else Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (isSelected) AuraAccent else AuraMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (mode == DownloadMode.VIDEO) "Video" else "Audio",
                    color = if (isSelected) AuraAccent else AuraMuted,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun QualitySelector(
    options: List<YtDownloaderHelper.QualityOption>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val isSelected = opt.value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) AuraAccent.copy(alpha = 0.15f) else AuraCard)
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) AuraAccent else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(opt.value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    opt.label,
                    color = if (isSelected) AuraAccent else AuraMuted,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(
    isDownloading: Boolean,
    isDone: Boolean,
    error: String?,
    progress: Int,
    statusText: String,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                error != null -> Color(0xFF3B1A1A)
                isDone        -> Color(0xFF1A3B1A)
                else          -> AuraCard
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                error != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color(0xFFFF6B6B))
                        Spacer(Modifier.width(8.dp))
                        Text("Download Failed", color = Color(0xFFFF6B6B), fontWeight = FontWeight.SemiBold)
                    }
                    Text(error, color = AuraMuted, fontSize = 12.sp)
                }
                isDone -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                        Spacer(Modifier.width(8.dp))
                        Text("Download Complete!", color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                    }
                    Text("File saved to device storage.", color = AuraMuted, fontSize = 12.sp)
                }
                isDownloading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = AuraAccent,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Downloading...", color = AuraText, fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = AuraAccent,
                        trackColor = AuraSurface
                    )
                    Text(statusText, color = AuraMuted, fontSize = 12.sp)
                }
            }

            if (isDone || error != null) {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("New Download", color = AuraAccent)
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1A1A))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFF6B6B))
            Spacer(Modifier.width(8.dp))
            Text(message, color = Color(0xFFFF6B6B), fontSize = 13.sp)
        }
    }
}
