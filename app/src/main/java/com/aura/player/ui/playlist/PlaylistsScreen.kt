package com.aura.player.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.player.ui.player.PlayerViewModel
import com.aura.player.ui.theme.*

@Composable
fun PlaylistsScreen(viewModel: PlayerViewModel, onOpenPlaylist: (String) -> Unit) {
    val playlists by viewModel.repository.playlists.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(AuraBg).padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Playlists", color = AuraText, style = MaterialTheme.typography.headlineLarge)
            IconButton(
                onClick = { showDialog = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AuraAccent)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New playlist", tint = androidx.compose.ui.graphics.Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No playlists yet. Tap + to create one.", color = AuraMuted)
            }
        } else {
            LazyColumn {
                items(playlists.keys.toList()) { name ->
                    val count = playlists[name]?.size ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenPlaylist(name) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AuraCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = AuraAccent)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(name, color = AuraText, style = MaterialTheme.typography.titleMedium)
                            Text("$count songs", color = AuraMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = AuraSurface,
            title = { Text("New Playlist", color = AuraText) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Playlist name", color = AuraMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraAccent,
                        unfocusedBorderColor = AuraCard,
                        focusedTextColor = AuraText,
                        unfocusedTextColor = AuraText
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.repository.createPlaylist(newName.trim())
                        newName = ""
                        showDialog = false
                    }
                }) { Text("Create", color = AuraAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel", color = AuraMuted) }
            }
        )
    }
}
