package com.aura.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.player.ui.components.SongRow
import com.aura.player.ui.player.PlayerViewModel
import com.aura.player.ui.theme.AuraAccent
import com.aura.player.ui.theme.AuraBg
import com.aura.player.ui.theme.AuraText

@Composable
fun LibraryScreen(viewModel: PlayerViewModel, hasStoragePermission: Boolean, onRequestPermission: () -> Unit) {
    val localSongs by viewModel.repository.localSongs.collectAsStateWithLifecycle()
    val streamingSongs by viewModel.repository.streamingSongs.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Local", "Streaming")

    Column(modifier = Modifier.fillMaxSize().background(AuraBg)) {
        Text(
            text = "Library",
            color = AuraText,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(20.dp)
        )

        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = AuraBg,
            contentColor = AuraAccent
        ) {
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = tabIndex == i,
                    onClick = { tabIndex = i },
                    text = { Text(label) }
                )
            }
        }

        when (tabIndex) {
            0 -> {
                if (!hasStoragePermission) {
                    PermissionPrompt(onRequestPermission)
                } else if (localSongs.isEmpty()) {
                    EmptyState("No local songs found.\nAdd MP3s to your device storage.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        items(localSongs, key = { it.id }) { song ->
                            SongRow(
                                song = song,
                                isCurrent = currentSong?.id == song.id,
                                onClick = {
                                    val idx = localSongs.indexOf(song)
                                    viewModel.playQueue(localSongs, idx)
                                }
                            )
                        }
                    }
                }
            }
            1 -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(streamingSongs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isCurrent = currentSong?.id == song.id,
                            onClick = {
                                val idx = streamingSongs.indexOf(song)
                                viewModel.playQueue(streamingSongs, idx)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Allow access to your music to see local songs here.",
            color = AuraText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = AuraAccent)) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(message, color = AuraText, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
