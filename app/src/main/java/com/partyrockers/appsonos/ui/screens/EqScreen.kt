package com.partyrockers.appsonos.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.partyrockers.appsonos.data.EQ_BAND_FREQUENCIES
import com.partyrockers.appsonos.data.EQ_PRESETS
import com.partyrockers.appsonos.viewmodel.PlayerViewModel

/**
 * EQ de 12 bandas, presets, Volumen x2 y efectos.
 * Compresor y reverb van apagados por defecto: son los que más pesan,
 * porque el efecto se tiene que aplicar antes de mandar el audio a Sonos.
 */
@Composable
fun EqScreen(viewModel: PlayerViewModel) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
    ) {
        Text(text = "Ecualizador", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Preset: ${viewModel.eqPresetName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            for (preset in EQ_PRESETS) {
                FilterChip(
                    selected = viewModel.eqPresetName == preset.name,
                    onClick = { viewModel.applyEqPreset(preset) },
                    label = { Text(preset.name) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        for (index in viewModel.eqGains.indices) {
            val freq = EQ_BAND_FREQUENCIES.getOrNull(index) ?: "$index"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = freq,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Slider(
                    value = viewModel.eqGains[index],
                    onValueChange = { viewModel.setEqBand(index, it) },
                    valueRange = -12f..12f,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${viewModel.eqGains[index].toInt()} dB",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Balance", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Izquierda y derecha. Sí funciona: en la bocina y en el teléfono.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Izq",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp),
            )
            Slider(
                value = viewModel.balance.toFloat(),
                onValueChange = { viewModel.previewBalance(it.toInt()) },
                onValueChangeFinished = { viewModel.setBalance(viewModel.balance) },
                valueRange = -100f..100f,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Der",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = when {
                viewModel.balance == 0 -> "Centro"
                viewModel.balance < 0 -> "Izquierda ${-viewModel.balance}"
                else -> "Derecha ${viewModel.balance}"
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Volumen", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Volumen x2",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = viewModel.volumeX2,
                onCheckedChange = { viewModel.updateVolumeX2(it) },
            )
        }
        Text(
            text = "Salida efectiva: ${viewModel.effectiveVolume}% (el volumen de la bocina no pasa de 100%)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Efectos (pesan más)", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Compresor", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Iguala el volumen entre rolas. Gasta más batería.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = viewModel.compressorOn,
                onCheckedChange = { viewModel.setCompressor(it) },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Reverb", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Efecto de eco. Es el más pesado de todos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = viewModel.reverbOn,
                onCheckedChange = { viewModel.setReverb(it) },
            )
        }
        if (viewModel.reverbOn) {
            Text(text = "Nivel de reverb: ${(viewModel.reverbLevel * 100).toInt()}%")
            Slider(
                value = viewModel.reverbLevel,
                onValueChange = { viewModel.updateReverbLevel(it) },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "El EQ, el compresor y el reverb aún no se aplican al audio (ni en la bocina ni en el teléfono): por ahora solo se guardan los valores. El balance sí funciona.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
