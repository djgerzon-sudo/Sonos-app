package com.partyrockers.appsonos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.partyrockers.appsonos.data.MusicFolder
import com.partyrockers.appsonos.ui.components.TrackRow
import com.partyrockers.appsonos.viewmodel.PlayerViewModel

/**
 * Carpetas: se navega por carpetas tal como están en el teléfono.
 * Desde una carpeta se puede agregar TODA la carpeta a una playlist.
 */
@Composable
fun FoldersScreen(viewModel: PlayerViewModel) {
    var openFolder by remember { mutableStateOf<MusicFolder?>(null) }
    var folderToAdd by remember { mutableStateOf<MusicFolder?>(null) }

    if (openFolder == null) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = "Carpetas",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(viewModel.folders, key = { it.path }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openFolder = folder }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = "Carpeta ${folder.name}")
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(text = folder.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${folder.trackCount} rolas · ${folder.path}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { folderToAdd = folder }) {
                        Icon(Icons.Filled.Add, contentDescription = "Agregar carpeta completa a playlist")
                    }
                }
            }
        }
    } else {
        val folder = openFolder!!
        val tracks = viewModel.allTracks.filter { it.folder == folder.path }
        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(onClick = { openFolder = null }) {
                Text("← Todas las carpetas")
            }
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            TextButton(onClick = { folderToAdd = folder }) {
                Text("Agregar carpeta completa a playlist")
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(track = track, onClick = { viewModel.playTrack(track) })
                }
            }
        }
    }

    val target = folderToAdd
    if (target != null) {
        PlaylistPickerDialog(
            folderPath = target.path,
            viewModel = viewModel,
            onDone = { folderToAdd = null },
        )
    }
}

@Composable
private fun PlaylistPickerDialog(
    folderPath: String,
    viewModel: PlayerViewModel,
    onDone: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Elige playlist") },
        text = {
            Column {
                for (playlist in viewModel.playlists) {
                    TextButton(
                        onClick = {
                            viewModel.addFolderToPlaylist(folderPath, playlist.id)
                            onDone()
                        },
                    ) {
                        Text(playlist.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDone) { Text("Cancelar") }
        },
    )
}
