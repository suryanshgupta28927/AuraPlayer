package com.aura.player.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aura.player.data.Video
import com.aura.player.ui.theme.*

@Composable
fun VideoLibraryScreen(
    viewModel: VideoPlayerViewModel,
    hasVideoPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenVideo: (List<Video>, Int) -> Unit
) {
    val videos by viewModel.repository.localVideos.collectAsStateWithLifecycle()

    LaunchedEffect(hasVideoPermission) {
        if (hasVideoPermission) viewModel.loadLocalVideos()
    }

    Column(modifier = Modifier.fillMaxSize().background(AuraBg)) {
        Text(
            text = "Videos",
            color = AuraText,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(20.dp)
        )

        when {
            !hasVideoPermission -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Allow access to your videos to see them here.",
                        color = AuraText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AuraAccent)
                    ) { Text("Grant Permission") }
                }
            }
            videos.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No videos found on this device.", color = AuraMuted)
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(videos, key = { it.id }) { video ->
                        VideoThumbnail(video = video) {
                            val idx = videos.indexOf(video)
                            onOpenVideo(videos, idx)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnail(video: Video, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuraCard)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(AuraSurface),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = video.thumbnailUri,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Icon(
                Icons.Filled.PlayCircle,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = formatDuration(video.durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        Text(
            text = video.title,
            color = AuraText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
