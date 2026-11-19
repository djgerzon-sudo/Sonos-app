package com.partyrockers.appsonos.sonos

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Servidor HTTP local mínimo, solo con la biblioteca estándar de Java.
 * Sonos no lee archivos del teléfono directamente: la app le sirve el audio
 * por HTTP y le pasa la URL con SetAVTransportURI.
 * v1 usa este servidor propio en vez de NanoHTTPD para no sumar una
 * dependencia externa; hace lo mismo para archivos mp3/flac/wav.
 *
 * Uso:
 *   val server = LocalHttpServer()
 *   server.start()
 *   val url = server.urlFor(File("/ruta/cancion.mp3")) // http://192.168.0.x:puerto/f0.mp3
 *   server.stop()
 */
class LocalHttpServer {

    private var serverSocket: ServerSocket? = null
    private var running = false
    private val files = mutableMapOf<String, File>()
    private var counter = 0

    val port: Int
        get() = serverSocket?.localPort ?: 0

    fun start(): Int {
        if (running) return port
        val socket = ServerSocket(0)
        serverSocket = socket
        running = true
        thread(name = "app-sonos-http", isDaemon = true) { acceptLoop(socket) }
        return socket.localPort
    }

    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {
            // Cerrar un socket ya cerrado no es un error.
        }
        serverSocket = null
        synchronized(files) { files.clear() }
    }

    /** Registra un archivo y devuelve su URL local. */
    fun urlFor(file: File): String? {
        if (!running) return null
        val key = synchronized(files) {
            // La extensión real importa: Sonos la usa como pista del formato.
            val ext = file.extension.lowercase().ifBlank { "mp3" }
            val name = "f${counter++}.$ext"
            files[name] = file
            name
        }
        val ip = localIpAddress() ?: return null
        return "http://$ip:${port}/$key"
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (running) {
            val client: Socket = try {
                socket.accept()
            } catch (_: Exception) {
                break
            }
            thread(name = "app-sonos-http-client", isDaemon = true) {
                handleClient(client)
            }
        }
    }

    private fun handleClient(client: Socket) {
        try {
            client.use { socket ->
                val input = socket.getInputStream()
                val output = socket.getOutputStream()
                val reader = input.bufferedReader(Charsets.UTF_8)
                val requestLine = reader.readLine() ?: return
                // "GET /f0.mp3 HTTP/1.1"
                val path = requestLine.split(" ").getOrNull(1)?.removePrefix("/") ?: return
                // Leer todas las cabeceras: Sonos pide Range para buscar
                // y para buferizar el inicio sin bajar todo el archivo.
                var rangeHeader: String? = null
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) break
                    if (line.startsWith("Range:", ignoreCase = true)) {
                        rangeHeader = line.substringAfter(":").trim()
                    }
                }
                val file = synchronized(files) { files[path] }
                if (file == null || !file.exists()) {
                    val body = "Not found".toByteArray(Charsets.UTF_8)
                    output.write(
                        "HTTP/1.1 404 Not Found\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                            .toByteArray(Charsets.UTF_8),
                    )
                    output.write(body)
                    return
                }
                val contentType = when (file.extension.lowercase()) {
                    "flac" -> "audio/flac"
                    "wav" -> "audio/wav"
                    "ogg" -> "audio/ogg"
                    "m4a" -> "audio/mp4"
                    else -> "audio/mpeg"
                }
                val fileLength = file.length()
                when (val range = parseRange(rangeHeader, fileLength)) {
                    is RangeResult.Invalid -> {
                        val header = "HTTP/1.1 416 Range Not Satisfiable\r\n" +
                            "Content-Range: bytes */$fileLength\r\nContent-Length: 0\r\n" +
                            "Connection: close\r\n\r\n"
                        output.write(header.toByteArray(Charsets.UTF_8))
                        return
                    }
                    is RangeResult.Range -> {
                        val length = range.end - range.start + 1
                        val header = "HTTP/1.1 206 Partial Content\r\nContent-Type: $contentType\r\n" +
                            "Accept-Ranges: bytes\r\n" +
                            "Content-Range: bytes ${range.start}-${range.end}/$fileLength\r\n" +
                            "Content-Length: $length\r\nConnection: close\r\n\r\n"
                        output.write(header.toByteArray(Charsets.UTF_8))
                        FileInputStream(file).use { stream ->
                            var skipped = 0L
                            while (skipped < range.start) {
                                val justSkipped = stream.skip(range.start - skipped)
                                if (justSkipped <= 0) break
                                skipped += justSkipped
                            }
                            val buffer = ByteArray(8192)
                            var remaining = length
                            while (remaining > 0) {
                                val read = stream.read(
                                    buffer,
                                    0,
                                    minOf(buffer.size.toLong(), remaining).toInt(),
                                )
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                remaining -= read
                            }
                        }
                    }
                    is RangeResult.NoRange -> {
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: $contentType\r\n" +
                            "Accept-Ranges: bytes\r\nContent-Length: $fileLength\r\n" +
                            "Connection: close\r\n\r\n"
                        output.write(header.toByteArray(Charsets.UTF_8))
                        BufferedInputStream(FileInputStream(file)).use { stream ->
                            stream.copyTo(output)
                        }
                    }
                }
                output.flush()
            }
        } catch (_: Exception) {
            // Un cliente que se desconecta a mitad del stream no debe tumbar el servidor.
        }
    }

    private sealed interface RangeResult {
        data object NoRange : RangeResult
        data object Invalid : RangeResult
        data class Range(val start: Long, val end: Long) : RangeResult
    }

    /**
     * Parsea "bytes=INICIO-FIN", "bytes=INICIO-" o "bytes=-ULTIMOS".
     * Sin cabecera devuelve NoRange; si el rango no se puede cumplir, Invalid.
     */
    private fun parseRange(rangeHeader: String?, fileLength: Long): RangeResult {
        if (rangeHeader == null) return RangeResult.NoRange
        val value = rangeHeader.removePrefix("bytes=").trim()
        if (value.startsWith("-")) {
            val suffix = value.removePrefix("-").toLongOrNull() ?: return RangeResult.Invalid
            if (suffix <= 0) return RangeResult.Invalid
            val start = (fileLength - suffix).coerceAtLeast(0)
            return RangeResult.Range(start, fileLength - 1)
        }
        val parts = value.split("-")
        if (parts.size != 2) return RangeResult.Invalid
        val start = parts[0].toLongOrNull() ?: return RangeResult.Invalid
        val end = if (parts[1].isBlank()) {
            fileLength - 1
        } else {
            parts[1].toLongOrNull() ?: return RangeResult.Invalid
        }
        if (start < 0 || start >= fileLength || end < start) return RangeResult.Invalid
        return RangeResult.Range(start, minOf(end, fileLength - 1))
    }

    /** Primera IPv4 local que no sea loopback (la de la WiFi). */
    fun localIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress &&
                        address.isSiteLocalAddress
                    ) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
