package com.partyrockers.appsonos.data

/**
 * Una rola de la biblioteca. Solo existe si hay un archivo real.
 * @param localPath ruta del archivo en el teléfono; null si no hay archivo.
 * @param hasLyrics true solo si hay letra disponible (incrustada, .lrc o descargada).
 * @param lyricsLines líneas de la letra, solo cuando hay letra real.
 * @param kbps velocidad a la que se está reproduciendo; 0 si aún no suena nada.
 * @param addedDaysAgo hace cuántos días se agregó al almacenamiento (para "Recién agregados").
 * @param folder carpeta donde vive el archivo, tal como está en el teléfono.
 */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val kbps: Int = 320,
    val hasLyrics: Boolean = false,
    val lyricsLines: List<String> = emptyList(),
    val addedDaysAgo: Int = 30,
    val folder: String = "Música",
    val localPath: String? = null,
) {
    val durationText: String
        get() {
            val m = durationSeconds / 60
            val s = durationSeconds % 60
            return "%d:%02d".format(m, s)
        }
}

data class Playlist(
    val id: String,
    val name: String,
    val source: String, // "App Sonos" o "Importada (.m3u)" / "Importada (.wpl)"
    val trackIds: List<String>,
)

data class MusicFolder(
    val path: String,
    val name: String,
    val trackCount: Int,
)

/** Una bocina Sonos encontrada en la red local. */
data class SonosSpeaker(
    val uuid: String,
    val name: String,
    val model: String, // p. ej. "SP100" o "ZP100"
    val ip: String,
    val isCoordinator: Boolean = false,
)

/** Preset del ecualizador: 12 valores, uno por banda, en dB (-12..+12). */
data class EqPreset(val name: String, val gainsDb: List<Float>) {
    init {
        require(gainsDb.size == 12) { "Un preset de EQ debe tener 12 bandas" }
    }
}

val EQ_BAND_FREQUENCIES = listOf(
    "60", "120", "250", "500", "1k", "2k", "4k", "6k", "8k", "10k", "12k", "16k",
)

val EQ_PRESETS = listOf(
    EqPreset("Plano", List(12) { 0f }),
    EqPreset("Graves", listOf(8f, 7f, 5f, 3f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqPreset("Agudos", listOf(0f, 0f, 0f, 0f, 0f, 1f, 3f, 5f, 6f, 7f, 8f, 8f)),
    EqPreset("Vocal", listOf(-2f, -1f, 0f, 2f, 4f, 5f, 4f, 2f, 0f, 0f, 0f, 0f)),
    EqPreset("Fiesta", listOf(7f, 6f, 4f, 2f, 0f, 0f, 1f, 3f, 5f, 6f, 7f, 6f)),
)
