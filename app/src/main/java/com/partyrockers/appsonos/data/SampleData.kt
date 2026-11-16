package com.partyrockers.appsonos.data

/**
 * Datos de ejemplo para el demo v1.
 * Los títulos y artistas son genéricos y de ejemplo; no son rolas reales
 * y no traen archivos de audio ni letras reales.
 * En v2 la biblioteca se llenará leyendo el almacenamiento (MediaStore).
 */
object SampleData {

    val tracks: List<Track> = listOf(
        Track(
            id = "t1", title = "Ritmo de Barrio", artist = "Los del Norte",
            album = "Calle y Sonido", durationSeconds = 214, kbps = 320,
            hasLyrics = true,
            lyricsLines = listOf(
                "Línea de ejemplo uno, para probar la letra en Home.",
                "Línea de ejemplo dos, solo es un demo.",
                "Línea de ejemplo tres, la letra real va aquí en v2.",
            ),
            addedDaysAgo = 1, folder = "Música/Rap",
        ),
        Track(
            id = "t2", title = "Noche en Monterrey", artist = "DJ Ejemplo",
            album = "Noche en Monterrey", durationSeconds = 187, kbps = 256,
            hasLyrics = false, addedDaysAgo = 2, folder = "Música/Rap",
        ),
        Track(
            id = "t3", title = "Amanecer Prendido", artist = "Los del Norte",
            album = "Calle y Sonido", durationSeconds = 243, kbps = 320,
            hasLyrics = true,
            lyricsLines = listOf(
                "Ejemplo de letra sincronizada en la portada.",
                "Segunda línea de ejemplo.",
            ),
            addedDaysAgo = 0, folder = "Música/Rap",
        ),
        Track(
            id = "t4", title = "Bajo Que Retumba", artist = "Sonido Pesado",
            album = "Bajo Pesado", durationSeconds = 201, kbps = 192,
            hasLyrics = false, addedDaysAgo = 5, folder = "Música/Fiesta",
        ),
        Track(
            id = "t5", title = "Calle Sin Miedo", artist = "MC Ejemplo",
            album = "Versos de Calle", durationSeconds = 176, kbps = 320,
            hasLyrics = false, addedDaysAgo = 12, folder = "Música/Rap",
        ),
        Track(
            id = "t6", title = "Fiesta Hasta Tarde", artist = "DJ Ejemplo",
            album = "Fiesta", durationSeconds = 229, kbps = 256,
            hasLyrics = false, addedDaysAgo = 20, folder = "Música/Fiesta",
        ),
        Track(
            id = "t7", title = "Vuelvo a Empezar", artist = "Sonido Pesado",
            album = "Bajo Pesado", durationSeconds = 198, kbps = 320,
            hasLyrics = true,
            lyricsLines = listOf("Línea de ejemplo para la portada de Home."),
            addedDaysAgo = 3, folder = "Música/Fiesta",
        ),
        Track(
            id = "t8", title = "Ritmo y Tambor", artist = "MC Ejemplo",
            album = "Versos de Calle", durationSeconds = 221, kbps = 128,
            hasLyrics = false, addedDaysAgo = 40, folder = "Música/Rap",
        ),
        Track(
            id = "t9", title = "Ciudad de Noche", artist = "Los del Norte",
            album = "Calle y Sonido", durationSeconds = 205, kbps = 320,
            hasLyrics = false, addedDaysAgo = 8, folder = "Música/Rap",
        ),
        Track(
            id = "t10", title = "Prende la Bocina", artist = "DJ Ejemplo",
            album = "Fiesta", durationSeconds = 192, kbps = 256,
            hasLyrics = false, addedDaysAgo = 1, folder = "Música/Fiesta",
        ),
        Track(
            id = "t11", title = "Camino Largo", artist = "Sonido Pesado",
            album = "Bajo Pesado", durationSeconds = 254, kbps = 320,
            hasLyrics = false, addedDaysAgo = 60, folder = "Música/Fiesta",
        ),
        Track(
            id = "t12", title = "Sin Frenos", artist = "MC Ejemplo",
            album = "Versos de Calle", durationSeconds = 183, kbps = 192,
            hasLyrics = false, addedDaysAgo = 15, folder = "Música/Rap",
        ),
    )

    val folders: List<MusicFolder> = listOf(
        MusicFolder("Música/Rap", "Rap", 7),
        MusicFolder("Música/Fiesta", "Fiesta", 5),
        MusicFolder("Música/Descargas", "Descargas", 0),
    )

    val playlists: List<Playlist> = listOf(
        Playlist("p1", "Para Entrenar", "App Sonos", listOf("t1", "t3", "t4", "t10")),
        Playlist("p2", "Fiesta en Casa", "App Sonos", listOf("t4", "t6", "t10", "t11")),
        Playlist("p3", "Mis favoritas WMP", "Importada (.wpl)", listOf("t2", "t5", "t8")),
        Playlist("p4", "Lista vieja", "Importada (.m3u)", listOf("t9", "t12", "t1")),
    )

    val speakers: List<SonosSpeaker> = listOf(
        SonosSpeaker("RINCON_EJEMPLO_SALA", "Sala", "SP100", "192.168.0.101", isCoordinator = true),
        SonosSpeaker("RINCON_EJEMPLO_COCINA", "Cocina", "ZP100", "192.168.0.102"),
        SonosSpeaker("RINCON_EJEMPLO_RECAMARA", "Recámara", "SP100", "192.168.0.103"),
    )

    fun trackById(id: String): Track? = tracks.find { it.id == id }
}
