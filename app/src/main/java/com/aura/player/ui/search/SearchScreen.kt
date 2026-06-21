package com.aura.player.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.player.ui.components.SongRow
import com.aura.player.ui.player.PlayerViewModel
import com.aura.player.ui.theme.*

@Composable
fun SearchScreen(viewModel: PlayerViewModel) {
    var query by remember { mutableStateOf("") }
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val results = viewModel.repository.search(query)

    Column(modifier = Modifier.fillMaxSize().background(AuraBg).padding(20.dp)) {
        Text("Search", color = AuraText, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Songs, artists, albums...", color = AuraMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = AuraMuted) },
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

        Spacer(modifier = Modifier.height(12.dp))

        if (query.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Search your local library and streaming catalog", color = AuraMuted)
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No results for \"$query\"", color = AuraMuted)
            }
        } else {
            LazyColumn {
                items(results, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isCurrent = currentSong?.id == song.id,
                        onClick = {
                            val idx = results.indexOf(song)
                            viewModel.playQueue(results, idx)
                        }
                    )
                }
            }
        }
    }
}
