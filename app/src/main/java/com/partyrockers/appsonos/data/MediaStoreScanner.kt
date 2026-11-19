package com.partyrockers.appsonos.data

import android.content.Context
import android.provider.MediaStore
import java.io.File

/**
 * Lee la música REAL del teléfono con MediaStore. No inventa nada:
 * solo devuelve archivos de audio que existen en el almacenamiento,
 * con sus metadatos reales (título, artista, álbum, duración, fecha).
 * Requiere el permiso READ_MEDIA_AUDIO (API 33+) o
 * READ_EXTERNAL_STORAGE (API 32 o menos), pedido en la UI antes de escanear.
 */
object MediaStoreScanner {

    fun scan(context: Context): List<Track> {
        val found = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DATA,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val nowSeconds = System.currentTimeMillis() / 1000
        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val displayIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val relativeIdx = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val durationMs = cursor.getLong(durationIdx)
                if (durationMs <= 0) continue
                val displayName = cursor.getString(displayIdx) ?: continue
                val dataPath = if (dataIdx >= 0) cursor.getString(dataIdx) else null
                val relativePath = if (relativeIdx >= 0) cursor.getString(relativeIdx) ?: "" else ""
                val fullPath = if (!dataPath.isNullOrBlank() && File(dataPath).exists()) {
                    dataPath
                } else {
                    "/storage/emulated/0/$relativePath$displayName"
                }
                val file = File(fullPath)
                if (!file.exists()) continue
                val folder = file.parent ?: "Música"
                val dateAdded = cursor.getLong(dateAddedIdx)
                val addedDaysAgo = if (dateAdded > 0) {
                    ((nowSeconds - dateAdded) / 86400).toInt().coerceAtLeast(0)
                } else {
                    30
                }
                found.add(
                    Track(
                        id = "media:${cursor.getLong(idIdx)}",
                        title = cursor.getString(titleIdx)?.takeIf { it.isNotBlank() }
                            ?: displayName.substringBeforeLast("."),
                        artist = cursor.getString(artistIdx)?.takeIf { it.isNotBlank() }
                            ?: "Desconocido",
                        album = cursor.getString(albumIdx)?.takeIf { it.isNotBlank() }
                            ?: "Desconocido",
                        durationSeconds = (durationMs / 1000).toInt(),
                        kbps = 0, // MediaStore no da el bitrate; 0 = aún sin medir.
                        addedDaysAgo = addedDaysAgo,
                        folder = folder,
                        localPath = fullPath,
                    ),
                )
            }
        }
        return found
    }
}
