package com.aura.player.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aura.player.data.Song
import com.aura.player.ui.theme.*
import androidx.compose.ui.Modifier as M

@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onCollapse: () -> Unit) {
    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.positionMs.collectAsStateWithLifecycle()
    val duration by viewModel.durationMs.collectAsStateWithLifecycle()
    val shuffleOn by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AuraBg, AuraSurface)))
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse", tint = AuraText)
            }
            Text("NOW PLAYING", color = AuraMuted, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Vinyl-style rotating artwork
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            VinylArt(song = song, isPlaying = isPlaying)
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = song?.title ?: "Nothing playing",
            color = AuraText,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song?.artist ?: "—",
            color = AuraMuted,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Scrubber
        Slider(
            value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
            onValueChange = { frac -> viewModel.seekTo((frac * duration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = AuraAccent,
                activeTrackColor = AuraAccent,
                inactiveTrackColor = AuraCard
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(position), color = AuraMuted, style = MaterialTheme.typography.labelSmall)
            Text(formatMs(duration), color = AuraMuted, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transport controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleOn) AuraAccent else AuraMuted
                )
            }
            IconButton(onClick = { viewModel.skipToPrevious() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = AuraText, modifier = M.size(32.dp))
            }

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AuraAccent, AuraAccent2)))
                    .clickableNoRipple { viewModel.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = M.size(34.dp)
                )
            }

            IconButton(onClick = { viewModel.skipToNext() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = AuraText, modifier = M.size(32.dp))
            }
            IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                Icon(
                    imageVector = if (repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode != 0) AuraAccent else AuraMuted
                )
            }
        }
    }
}

@Composable
private fun VinylArt(song: Song?, isPlaying: Boolean) {
    val infinite = rememberInfiniteTransition(label = "vinyl")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(260.dp)
            .rotate(if (isPlaying) rotation else 0f)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(AuraCard, AuraBg))),
        contentAlignment = Alignment.Center
    ) {
        if (song?.artUri != null) {
            AsyncImage(
                model = song.artUri,
                contentDescription = song.title,
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = AuraMuted,
                modifier = Modifier.size(64.dp)
            )
        }
        // Center hole
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(AuraBg)
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
)
