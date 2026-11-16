package com.partyrockers.appsonos.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.partyrockers.appsonos.ui.components.TrackRow
import com.partyrockers.appsonos.viewmodel.PlayerViewModel
import com.partyrockers.appsonos.viewmodel.SortOrder

/** Biblioteca: búsqueda, orden por título / artista / duración y Recién agregados. */
@Composable
fun LibraryScreen(viewModel: PlayerViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Buscar canción, artista o álbum") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text = "Ordenar por:", style = MaterialTheme.typography.bodyMedium)
        }
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            FilterChip(
                selected = viewModel.sortOrder == SortOrder.TITLE,
                onClick = { viewModel.updateSortOrder(SortOrder.TITLE) },
                label = { Text("Título") },
            )
            FilterChip(
                selected = viewModel.sortOrder == SortOrder.ARTIST,
                onClick = { viewModel.updateSortOrder(SortOrder.ARTIST) },
                label = { Text("Artista") },
                modifier = Modifier.padding(start = 8.dp),
            )
            FilterChip(
                selected = viewModel.sortOrder == SortOrder.DURATION,
                onClick = { viewModel.updateSortOrder(SortOrder.DURATION) },
                label = { Text("Duración") },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (viewModel.allTracks.isEmpty()) {
            Text(
                text = "No hay música cargada",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (viewModel.recentTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recién agregados",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(viewModel.recentTracks, key = { it.id }) { track ->
                        TrackRow(track = track, onClick = { viewModel.playTrack(track) })
                    }
                }
                item {
                    Text(
                        text = "Todas las rolas (${viewModel.visibleTracks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (viewModel.visibleTracks.isEmpty()) {
                    item {
                        Text(
                            text = "No se encontró nada con esa búsqueda",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    items(viewModel.visibleTracks, key = { it.id }) { track ->
                        TrackRow(track = track, onClick = { viewModel.playTrack(track) })
                    }
                }
            }
        }
    }
}
