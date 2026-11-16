package com.partyrockers.appsonos.data

/**
 * Listas vacías por diseño: la app no inventa rolas, carpetas,
 * playlists ni bocinas. Todo se llena solo con lo que haya de verdad:
 * la biblioteca con archivos del teléfono (trabajo pendiente) y las
 * bocinas con lo que encuentre "Buscar bocinas en la WiFi".
 */
object SampleData {

    val tracks: List<Track> = emptyList()

    val folders: List<MusicFolder> = emptyList()

    val playlists: List<Playlist> = emptyList()

    val speakers: List<SonosSpeaker> = emptyList()

    fun trackById(id: String): Track? = tracks.find { it.id == id }
}
