package com.partyrockers.appsonos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.partyrockers.appsonos.data.Playlist
import com.partyrockers.appsonos.ui.components.TrackRow
import com.partyrockers.appsonos.viewmodel.PlayerViewModel

/**
 * Playlists: crear, ver las importadas (.m3u / .wpl) y reordenar
 * las rolas de una lista ya creada (flechas arriba / abajo).
 */
@Composable
fun PlaylistsScreen(viewModel: PlayerViewModel) {
    var openPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    if (openPlaylist == null) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Crear playlist")
                    }
                }
            }
            if (viewModel.playlists.isEmpty()) {
                item {
                    Text(
                        text = "Aún no tienes playlists",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(viewModel.playlists, key = { it.id }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openPlaylist = playlist }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.QueueMusic, contentDescription = "Playlist ${playlist.name}")
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(text = playlist.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${playlist.source} · ${playlist.trackIds.size} rolas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Borrar playlist ${playlist.name}")
                    }
                }
            }
        }
    } else {
        val playlist = viewModel.playlists.find { it.id == openPlaylist!!.id } ?: openPlaylist!!
        val tracks = viewModel.tracksOf(playlist)
        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(onClick = { openPlaylist = null }) {
                Text("← Todas las playlists")
            }
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = "${playlist.source} · ${tracks.size} rolas. Usa las flechas para cambiar el orden.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (tracks.isEmpty()) {
                    item {
                        Text(
                            text = "Aún sin rolas en esta playlist",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRow(
                        track = track,
                        onClick = { viewModel.playTrack(track) },
                        trailing = {
                            IconButton(
                                onClick = {
                                    viewModel.moveTrackInPlaylist(playlist.id, index, index - 1)
                                },
                            ) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir ${track.title}")
                            }
                            IconButton(
                                onClick = {
                                    viewModel.moveTrackInPlaylist(playlist.id, index, index + 1)
                                },
                            ) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = "Bajar ${track.title}")
                            }
                            IconButton(
                                onClick = {
                                    viewModel.removeTrackFromPlaylist(playlist.id, track.id)
                                },
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Quitar ${track.title}")
                            }
                        },
                    )
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Crear playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nombre de la playlist") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createPlaylist(newName)
                        newName = ""
                        showCreate = false
                    },
                ) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Cancelar") }
            },
        )
    }
}
