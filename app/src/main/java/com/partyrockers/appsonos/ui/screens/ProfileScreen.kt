package com.partyrockers.appsonos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.partyrockers.appsonos.viewmodel.PlayerViewModel

/**
 * Perfil: solo "Mi sistema" con las bocinas encontradas de verdad
 * en la WiFi, y los ajustes del equipo, que aún no están disponibles.
 * Sin cuenta inventada, sin bocinas inventadas y sin valores inventados.
 */
@Composable
fun ProfileScreen(viewModel: PlayerViewModel, onClose: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Perfil",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text("Cerrar") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Mi sistema", style = MaterialTheme.typography.titleMedium)
        if (viewModel.speakers.isEmpty()) {
            Text(
                text = "No se encontró ninguna bocina",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        for (speaker in viewModel.speakers) {
            SettingRow(
                icon = Icons.Filled.Wifi,
                title = "${speaker.name} · ${speaker.model}",
                subtitle = speaker.ip + if (speaker == viewModel.currentSpeaker) " · en uso" else "",
                onClick = { viewModel.selectSpeaker(speaker) },
            )
        }
        TextButton(onClick = { viewModel.discoverSpeakers() }) {
            Text("Buscar bocinas en la WiFi")
        }
        Text(
            text = viewModel.sonosStatus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Ajustes del equipo", style = MaterialTheme.typography.titleMedium)
        SettingRow(
            icon = Icons.Filled.Wifi,
            title = "WiFi",
            subtitle = "Aún no disponible",
            onClick = {},
            showChevron = false,
        )
        SettingRow(
            icon = Icons.Filled.Timer,
            title = "Retraso de audio en grupo",
            subtitle = "Aún no disponible",
            onClick = {},
            showChevron = false,
        )
        SettingRow(
            icon = Icons.Filled.Input,
            title = "Entrada de línea",
            subtitle = "Aún no disponible",
            onClick = {},
            showChevron = false,
        )
        SettingRow(
            icon = Icons.Filled.Lightbulb,
            title = "LED de la bocina",
            subtitle = "Aún no disponible",
            onClick = {},
            showChevron = false,
        )
        SettingRow(
            icon = Icons.Filled.TouchApp,
            title = "Botones táctiles",
            subtitle = "Aún no disponible",
            onClick = {},
            showChevron = false,
        )
        SettingRow(
            icon = Icons.Filled.SystemUpdate,
            title = "Actualizaciones",
            subtitle = "Aún no disponible",
            onClick = {},
            showChevron = false,
        )
        SettingRow(
            icon = Icons.Filled.Alarm,
            title = "Alarmas",
            subtitle = "Aún no disponible",
            onClick = {},
            showChevron = false,
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = title)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showChevron) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Abrir $title")
        }
    }
}
