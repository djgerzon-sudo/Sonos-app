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
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.partyrockers.appsonos.viewmodel.PlayerViewModel

/**
 * Perfil: cuenta (ejemplo, sin llamada real a Sonos), Mi sistema y
 * ajustes del equipo como en la app original: WiFi, retraso de audio
 * en grupo, entrada de línea y su nivel, LED, botones táctiles,
 * actualizaciones y alarmas. Todo es placeholder en v1: no se borra
 * ni se cambia nada del sistema real hasta probar con las bocinas.
 */
@Composable
fun ProfileScreen(viewModel: PlayerViewModel, onClose: () -> Unit) {
    val scrollState = rememberScrollState()
    var signedIn by remember { mutableStateOf(false) }
    var groupDelayMs by remember { mutableFloatStateOf(75f) }
    var lineInLevel by remember { mutableFloatStateOf(5f) }
    var ledOn by remember { mutableStateOf(true) }
    var touchButtonsOn by remember { mutableStateOf(true) }
    var showSignOut by remember { mutableStateOf(false) }
    var showBackendNote by remember { mutableStateOf(false) }

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

        Text(text = "Cuenta", style = MaterialTheme.typography.titleMedium)
        if (signedIn) {
            Text(text = "Usuario de ejemplo", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "cuenta@ejemplo.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { showSignOut = true }) {
                Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión")
                Text("Cerrar sesión")
            }
        } else {
            Text(
                text = "No has iniciado sesión (ejemplo).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { signedIn = true }) {
                Icon(Icons.Filled.Login, contentDescription = "Iniciar sesión")
                Text("Iniciar sesión")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Mi sistema", style = MaterialTheme.typography.titleMedium)
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
            subtitle = "Red de la casa (ejemplo)",
            onClick = { showBackendNote = true },
        )
        Text(
            text = "Retraso de audio en grupo: ${groupDelayMs.toInt()} ms",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Timer, contentDescription = "Retraso de audio")
            Slider(
                value = groupDelayMs,
                onValueChange = { groupDelayMs = it },
                valueRange = 0f..150f,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        SettingRow(
            icon = Icons.Filled.Input,
            title = "Entrada de línea",
            subtitle = "Nivel: ${lineInLevel.toInt()} de 10",
            onClick = { showBackendNote = true },
        )
        Slider(
            value = lineInLevel,
            onValueChange = { lineInLevel = it },
            valueRange = 0f..10f,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lightbulb, contentDescription = "LED")
            Text(text = "LED de la bocina", modifier = Modifier.weight(1f).padding(start = 8.dp))
            Switch(checked = ledOn, onCheckedChange = { ledOn = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TouchApp, contentDescription = "Botones táctiles")
            Text(text = "Botones táctiles", modifier = Modifier.weight(1f).padding(start = 8.dp))
            Switch(checked = touchButtonsOn, onCheckedChange = { touchButtonsOn = it })
        }
        SettingRow(
            icon = Icons.Filled.SystemUpdate,
            title = "Actualizaciones",
            subtitle = "Buscar actualización del sistema (ejemplo)",
            onClick = { showBackendNote = true },
        )
        SettingRow(
            icon = Icons.Filled.Alarm,
            title = "Alarmas",
            subtitle = "Sin alarmas (ejemplo)",
            onClick = { showBackendNote = true },
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Backend y APK",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = "Todavía no hay APK desde este demo. El backend UPnP/SOAP y la APK se compilan con GitHub Actions (ver README del proyecto). No borres la app de Sonos hasta probar esta app con tus SP100/ZP100 reales.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (showSignOut) {
        AlertDialog(
            onDismissRequest = { showSignOut = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que quieres cerrar la sesión de ejemplo?") },
            confirmButton = {
                TextButton(onClick = { signedIn = false; showSignOut = false }) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOut = false }) { Text("Cancelar") }
            },
        )
    }
    if (showBackendNote) {
        AlertDialog(
            onDismissRequest = { showBackendNote = false },
            title = { Text("Ajuste del equipo") },
            text = { Text("Este ajuste es de ejemplo en v1. Se conecta al equipo real cuando el backend UPnP esté probado con las bocinas.") },
            confirmButton = {
                TextButton(onClick = { showBackendNote = false }) { Text("Entendido") }
            },
            dismissButton = {},
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
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
        Icon(Icons.Filled.ChevronRight, contentDescription = "Abrir $title")
    }
}
