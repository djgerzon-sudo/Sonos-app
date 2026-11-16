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
            val name = "f${counter++}.mp3"
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
                val requestLine = input.bufferedReader(Charsets.UTF_8).readLine() ?: return
                // "GET /f0.mp3 HTTP/1.1"
                val path = requestLine.split(" ").getOrNull(1)?.removePrefix("/") ?: return
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
                    else -> "audio/mpeg"
                }
                val header = "HTTP/1.1 200 OK\r\nContent-Type: $contentType\r\n" +
                    "Content-Length: ${file.length()}\r\nConnection: close\r\n\r\n"
                output.write(header.toByteArray(Charsets.UTF_8))
                BufferedInputStream(FileInputStream(file)).use { stream ->
                    stream.copyTo(output)
                }
                output.flush()
            }
        } catch (_: Exception) {
            // Un cliente que se desconecta a mitad del stream no debe tumbar el servidor.
        }
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
