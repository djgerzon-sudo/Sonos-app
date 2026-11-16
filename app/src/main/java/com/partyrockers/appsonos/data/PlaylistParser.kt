package com.partyrockers.appsonos.data

/**
 * Lee playlists de otros programas, sin depender de ellos.
 * Formatos que entiende v1:
 *  - .m3u / .m3u8: una ruta o URL por línea, líneas que empiezan con # se ignoran.
 *  - .pls: formato INI con File1=..., Title1=..., Length1=...
 *  - .wpl: XML de Windows Media Player con <media src="..." />.
 * Devuelve solo las rutas / títulos encontrados, en el orden del archivo.
 * No toca la red ni el almacenamiento: el que llama le pasa el texto ya leído.
 */
object PlaylistParser {

    fun parseM3u(text: String): List<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toList()

    fun parsePls(text: String): List<String> {
        val entries = mutableListOf<Pair<Int, String>>()
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("File", ignoreCase = true)) continue
            val eq = trimmed.indexOf('=')
            if (eq < 0) continue
            val key = trimmed.substring(0, eq)
            val number = key.removePrefix("File").removePrefix("file").toIntOrNull() ?: continue
            val value = trimmed.substring(eq + 1).trim()
            if (value.isNotEmpty()) entries.add(number to value)
        }
        return entries.sortedBy { it.first }.map { it.second }
    }

    fun parseWpl(text: String): List<String> {
        // <media src="ruta" /> — se extrae con una expresión simple, sin parser XML.
        val regex = Regex("""<media[^>]*\ssrc\s*=\s*"([^"]+)"""")
        return regex.findAll(text).map { it.groupValues[1] }.toList()
    }

    /** Detecta el formato por la extensión del archivo y lo parsea. */
    fun parse(fileName: String, text: String): List<String> =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "m3u", "m3u8" -> parseM3u(text)
            "pls" -> parsePls(text)
            "wpl" -> parseWpl(text)
            else -> emptyList()
        }
}
