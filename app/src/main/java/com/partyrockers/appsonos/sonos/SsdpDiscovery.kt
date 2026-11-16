package com.partyrockers.appsonos.sonos

import com.partyrockers.appsonos.data.SonosSpeaker
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

/**
 * Descubrimiento de bocinas Sonos por SSDP en la red local.
 * Envía M-SEARCH a 239.255.255.250:1900 buscando ZonePlayer,
 * lee las respuestas (cabecera LOCATION) y descarga la descripción
 * de cada bocina para sacar su nombre, modelo y UUID.
 * Llamar desde Dispatchers.IO: bloquea hasta [timeoutMs].
 */
object SsdpDiscovery {

    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val SEARCH_TARGET = "urn:schemas-upnp-org:device:ZonePlayer:1"

    fun discover(timeoutMs: Int = 3_000): List<SonosSpeaker> {
        val found = linkedMapOf<String, SonosSpeaker>()
        val request = (
            "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n" +
                "MAN: \"ns=01\"\r\n" +
                "MX: 2\r\n" +
                "ST: $SEARCH_TARGET\r\n" +
                "\r\n"
            ).toByteArray(Charsets.UTF_8)
        val group = InetAddress.getByName(SSDP_ADDRESS)
        MulticastSocket(SSDP_PORT).use { socket ->
            socket.soTimeout = timeoutMs
            socket.send(DatagramPacket(request, request.size, group, SSDP_PORT))
            val buffer = ByteArray(4096)
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (_: java.net.SocketTimeoutException) {
                    break
                }
                val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val location = text.lineSequence()
                    .firstOrNull { it.trim().startsWith("LOCATION:", ignoreCase = true) }
                    ?.substringAfter(":", "")
                    ?.trim()
                    ?: continue
                val speaker = describe(location) ?: continue
                found[speaker.uuid] = speaker
            }
        }
        return found.values.toList()
    }

    /** Descarga http://ip:1400/xml/device_description.xml y extrae los datos. */
    fun describe(locationUrl: String): SonosSpeaker? {
        return try {
            val xml = SoapClient.getText(locationUrl)
            val uuid = tag(xml, "UDN")?.removePrefix("uuid:") ?: return null
            val name = tag(xml, "friendlyName") ?: return null
            val model = tag(xml, "modelNumber") ?: "Sonos"
            // La IP viene de la URL de descripción, p. ej. http://192.168.0.101:1400/...
            val ip = locationUrl.removePrefix("http://")
                .substringBefore(":")
                .substringBefore("/")
            SonosSpeaker(uuid = uuid, name = name, model = model, ip = ip)
        } catch (_: Exception) {
            null
        }
    }

    /** Extrae <tag>valor</tag> de un XML simple. Devuelve null si no está. */
    fun tag(xml: String, name: String): String? {
        val regex = Regex("<$name>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL)
        return regex.find(xml)?.groupValues?.getOrNull(1)?.trim()
    }
}
