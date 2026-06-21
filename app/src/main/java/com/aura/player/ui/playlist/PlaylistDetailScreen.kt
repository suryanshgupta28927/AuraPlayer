package com.aura.player.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.player.ui.components.SongRow
import com.aura.player.ui.player.PlayerViewModel
import com.aura.player.ui.theme.AuraBg
import com.aura.player.ui.theme.AuraMuted
import com.aura.player.ui.theme.AuraText

@Composable
fun PlaylistDetailScreen(viewModel: PlayerViewModel, playlistName: String, onBack: () -> Unit) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val songs = viewModel.repository.songsInPlaylist(playlistName)

    Column(modifier = Modifier.fillMaxSize().background(AuraBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AuraText)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(playlistName, color = AuraText, style = MaterialTheme.typography.headlineMedium)
        }

        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs added yet.", color = AuraMuted)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp)) {
                items(songs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isCurrent = currentSong?.id == song.id,
                        onClick = {
                            val idx = songs.indexOf(song)
                            viewModel.playQueue(songs, idx)
                        },
                        onAddToPlaylist = {
                            viewModel.repository.removeFromPlaylist(playlistName, song.id)
                        }
                    )
                }
            }
        }
    }
}
