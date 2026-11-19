package com.partyrockers.appsonos.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.partyrockers.appsonos.data.EqPreset
import com.partyrockers.appsonos.data.MediaStoreScanner
import com.partyrockers.appsonos.data.MusicFolder
import com.partyrockers.appsonos.data.Playlist
import com.partyrockers.appsonos.data.SonosSpeaker
import com.partyrockers.appsonos.data.Track
import com.partyrockers.appsonos.player.LocalPlayer
import com.partyrockers.appsonos.sonos.LocalHttpServer
import com.partyrockers.appsonos.sonos.SonosController
import com.partyrockers.appsonos.sonos.SoapClient
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOrder { TITLE, ARTIST, DURATION }

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val localPlayer = LocalPlayer(application)
    private val sonos = SonosController(application)
    private val httpServer = LocalHttpServer()

    // --- Biblioteca y cola ---
    // Vacías al inicio: nada inventado. Se llenan solo con archivos reales.
    var allTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set
    var folders by mutableStateOf<List<MusicFolder>>(emptyList())
        private set

    // --- Rola actual ---
    // Null cuando no hay nada cargado: la UI muestra "No hay música cargada".
    var currentTrack by mutableStateOf<Track?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var queue by mutableStateOf<List<Track>>(emptyList())
        private set

    // --- Volumen ---
    var volume by mutableIntStateOf(55) // 0..100
        private set
    var volumeX2 by mutableStateOf(false)
        private set

    /** Volumen efectivo que sale a la bocina (0..200 con x2). */
    val effectiveVolume: Int
        get() = (volume * if (volumeX2) 2 else 1).coerceIn(0, 200)

    // --- Bocinas ---
    // Vacías al inicio: solo aparecen las que encuentre de verdad
    // "Buscar bocinas en la WiFi". Null = aún sin bocina.
    var speakers by mutableStateOf<List<SonosSpeaker>>(emptyList())
        private set
    var currentSpeaker by mutableStateOf<SonosSpeaker?>(null)
        private set
    var sonosConnected by mutableStateOf(false)
        private set
    var sonosStatus by mutableStateOf("Aún sin bocina. Toca \"Buscar bocinas en la WiFi\".")
        private set

    // --- Escaneo de música real del teléfono ---
    private val prefs = application.getSharedPreferences("app_sonos_prefs", Context.MODE_PRIVATE)

    /** Rolas escaneadas (todas, antes de aplicar carpetas excluidas). */
    private var scannedTracks by mutableStateOf<List<Track>>(emptyList())

    /** Rutas de carpetas que el usuario excluyó. Se guardan en SharedPreferences. */
    var excludedFolders by mutableStateOf(
        prefs.getStringSet("excluded_folders", emptySet())?.toSet() ?: emptySet(),
    )
        private set

    var scanState by mutableStateOf("Aún sin escanear. Toca \"Escanear almacenamiento\".")
        private set
    var isScanning by mutableStateOf(false)
        private set

    // --- Prueba manual de conexión con la bocina ---
    var connectionTestResult by mutableStateOf("")
        private set
    var isTestingConnection by mutableStateOf(false)
        private set

    // --- EQ y efectos (apagados por defecto; compresor y reverb pesan más) ---
    var eqGains by mutableStateOf<List<Float>>(List(12) { 0f })
        private set
    var eqPresetName by mutableStateOf("Plano")
        private set
    var compressorOn by mutableStateOf(false)
        private set
    var reverbOn by mutableStateOf(false)
        private set
    var reverbLevel by mutableFloatStateOf(0.3f)
        private set

    // --- Biblioteca: búsqueda y orden ---
    var searchQuery by mutableStateOf("")
        private set
    var sortOrder by mutableStateOf(SortOrder.TITLE)
        private set

    val visibleTracks: List<Track>
        get() {
            val filtered = if (searchQuery.isBlank()) {
                allTracks
            } else {
                allTracks.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        it.album.contains(searchQuery, ignoreCase = true)
                }
            }
            return when (sortOrder) {
                SortOrder.TITLE -> filtered.sortedBy { it.title.lowercase() }
                SortOrder.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
                SortOrder.DURATION -> filtered.sortedBy { it.durationSeconds }
            }
        }

    /** Rolas agregadas hace 7 días o menos. */
    val recentTracks: List<Track>
        get() = allTracks.filter { it.addedDaysAgo <= 7 }.sortedBy { it.addedDaysAgo }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun updateSortOrder(order: SortOrder) {
        sortOrder = order
    }

    // --- Reproducción ---

    fun playTrack(track: Track) {
        currentTrack = track
        isPlaying = true
        // La cola es la biblioteca visible: así Siguiente/Anterior avanzan.
        if (queue.none { it.id == track.id }) {
            queue = visibleTracks
        }
        // 1) Suena en el teléfono si hay archivo local.
        if (track.localPath != null) {
            localPlayer.playLocalFile(track.localPath)
        }
        // 2) Se manda a Sonos por HTTP si hay bocina conectada.
        viewModelScope.launch {
            val path = track.localPath
            if (sonosConnected && path != null) {
                httpServer.start()
                val url = httpServer.urlFor(File(path))
                if (url != null) sonos.playTrack(track, url)
            }
        }
    }

    fun togglePlayPause() {
        val track = currentTrack ?: return
        if (isPlaying) {
            isPlaying = false
            localPlayer.pause()
            viewModelScope.launch { sonos.pause() }
        } else {
            isPlaying = true
            if (track.localPath != null) localPlayer.resume()
            viewModelScope.launch { sonos.play() }
        }
    }

    fun nextTrack() {
        if (queue.isEmpty()) return
        val current = currentTrack
        if (current == null) {
            queue.firstOrNull()?.let { playTrack(it) }
            return
        }
        val index = queue.indexOf(current)
        val next = queue.getOrNull(index + 1) ?: queue.firstOrNull() ?: return
        playTrack(next)
        viewModelScope.launch { sonos.next() }
    }

    fun previousTrack() {
        if (queue.isEmpty()) return
        val current = currentTrack
        if (current == null) {
            queue.firstOrNull()?.let { playTrack(it) }
            return
        }
        val index = queue.indexOf(current)
        val prev = if (index > 0) queue[index - 1] else queue.lastOrNull() ?: return
        playTrack(prev)
        viewModelScope.launch { sonos.previous() }
    }

    fun updateVolume(value: Int) {
        volume = value.coerceIn(0, 100)
        // El volumen que se manda a Sonos es el efectivo, sin pasar de 100.
        val toSonos = effectiveVolume.coerceIn(0, 100)
        viewModelScope.launch { sonos.setVolume(toSonos) }
    }

    fun updateVolumeX2(on: Boolean) {
        volumeX2 = on
        val toSonos = effectiveVolume.coerceIn(0, 100)
        viewModelScope.launch { sonos.setVolume(toSonos) }
    }

    // --- EQ y efectos ---

    fun applyEqPreset(preset: EqPreset) {
        eqPresetName = preset.name
        eqGains = preset.gainsDb
    }

    fun setEqBand(index: Int, gainDb: Float) {
        if (index !in eqGains.indices) return
        eqPresetName = "Personalizado"
        eqGains = eqGains.toMutableList().also { it[index] = gainDb.coerceIn(-12f, 12f) }
    }

    fun setCompressor(on: Boolean) {
        compressorOn = on
    }

    fun setReverb(on: Boolean) {
        reverbOn = on
    }

    fun updateReverbLevel(level: Float) {
        reverbLevel = level.coerceIn(0f, 1f)
    }

    // --- Bocinas Sonos ---

    fun selectSpeaker(speaker: SonosSpeaker) {
        currentSpeaker = speaker
        sonos.selectSpeaker(speaker)
        sonosConnected = true
        sonosStatus = "Conectada a ${speaker.name} (${speaker.ip})"
    }

    fun discoverSpeakers() {
        sonosStatus = "Buscando bocinas en la WiFi…"
        viewModelScope.launch {
            try {
                val found = sonos.discoverSpeakers()
                if (found.isNotEmpty()) {
                    speakers = found
                    currentSpeaker = found[0]
                    sonos.selectSpeaker(found[0])
                    sonosConnected = true
                    sonosStatus = "Conexión verificada: ${found.size} bocina(s) Sonos"
                } else {
                    speakers = emptyList()
                    currentSpeaker = null
                    sonos.selectSpeaker(null)
                    sonosConnected = false
                    sonosStatus = "No se encontró una bocina Sonos accesible por SSDP y SOAP"
                }
            } catch (error: Exception) {
                speakers = emptyList()
                currentSpeaker = null
                sonos.selectSpeaker(null)
                sonosConnected = false
                sonosStatus = "Falló la búsqueda: ${error.message ?: "error de red"}"
            }
        }
    }

    // --- Escaneo de música y carpetas ---

    fun isFolderExcluded(path: String): Boolean = path in excludedFolders

    fun setFolderExcluded(path: String, excluded: Boolean) {
        excludedFolders = if (excluded) {
            excludedFolders + path
        } else {
            excludedFolders - path
        }
        prefs.edit().putStringSet("excluded_folders", excludedFolders).apply()
        applyFolderFilter()
    }

    /** Biblioteca y carpetas según las carpetas que el usuario dejó incluidas. */
    private fun applyFolderFilter() {
        allTracks = scannedTracks.filter { it.folder !in excludedFolders }
        folders = scannedTracks
            .groupBy { it.folder }
            .map { (path, tracks) ->
                MusicFolder(
                    path = path,
                    name = path.substringAfterLast("/").ifBlank { path },
                    trackCount = tracks.size,
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    /** Lee la música real del teléfono. Se llama solo tras dar el permiso. */
    fun scanLibrary() {
        if (isScanning) return
        isScanning = true
        scanState = "Escaneando…"
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) {
                MediaStoreScanner.scan(getApplication())
            }
            scannedTracks = found
            applyFolderFilter()
            scanState = if (found.isEmpty()) {
                "No se encontró música en el almacenamiento"
            } else {
                "Se encontraron ${found.size} rolas en ${folders.size} carpetas"
            }
            isScanning = false
        }
    }

    fun onScanPermissionDenied() {
        scanState = "Sin permiso no puedo leer tu música. Toca Escanear de nuevo para permitirlo."
    }

    // --- Prueba manual de conexión teléfono-bocina ---

    fun testConnection() {
        val speaker = currentSpeaker
        if (speaker == null) {
            connectionTestResult = "Primero busca bocinas en la WiFi"
            return
        }
        if (isTestingConnection) return
        isTestingConnection = true
        connectionTestResult = "Probando conexión…"
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    SoapClient.getText("http://${speaker.ip}:1400/xml/device_description.xml")
                    SoapClient.getZoneGroupState(speaker.ip)
                    "Conexión OK: ${speaker.model} responde en ${speaker.ip}"
                } catch (_: Exception) {
                    "Sin respuesta de ${speaker.ip}: revisa que la bocina esté encendida y en la misma WiFi"
                }
            }
            connectionTestResult = result
            isTestingConnection = false
        }
    }

    // --- Playlists ---

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val newPlaylist = Playlist(
            id = "p${System.currentTimeMillis()}",
            name = trimmed,
            source = "App Sonos",
            trackIds = emptyList(),
        )
        playlists = playlists + newPlaylist
    }

    fun deletePlaylist(playlistId: String) {
        playlists = playlists.filterNot { it.id == playlistId }
    }

    fun addFolderToPlaylist(folderPath: String, playlistId: String) {
        val folderTracks = allTracks.filter { it.folder == folderPath }.map { it.id }
        playlists = playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(trackIds = playlist.trackIds + folderTracks)
            } else {
                playlist
            }
        }
    }

    fun addTrackToPlaylist(trackId: String, playlistId: String) {
        playlists = playlists.map { playlist ->
            if (playlist.id == playlistId && trackId !in playlist.trackIds) {
                playlist.copy(trackIds = playlist.trackIds + trackId)
            } else {
                playlist
            }
        }
    }

    fun moveTrackInPlaylist(playlistId: String, fromIndex: Int, toIndex: Int) {
        playlists = playlists.map { playlist ->
            if (playlist.id != playlistId) return@map playlist
            val ids = playlist.trackIds.toMutableList()
            if (fromIndex !in ids.indices || toIndex !in ids.indices) return@map playlist
            val moved = ids.removeAt(fromIndex)
            ids.add(toIndex, moved)
            playlist.copy(trackIds = ids)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlists = playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(trackIds = playlist.trackIds.filterNot { it == trackId })
            } else {
                playlist
            }
        }
    }

    fun tracksOf(playlist: Playlist): List<Track> =
        playlist.trackIds.mapNotNull { id -> allTracks.find { it.id == id } }

    override fun onCleared() {
        localPlayer.release()
        httpServer.stop()
        super.onCleared()
    }
}
