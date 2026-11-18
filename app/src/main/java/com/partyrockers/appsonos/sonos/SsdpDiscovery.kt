package com.partyrockers.appsonos.sonos

import android.content.Context
import android.net.wifi.WifiManager
import com.partyrockers.appsonos.data.SonosSpeaker
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URI

/**
 * Descubrimiento Sonos por SSDP. Debe ejecutarse desde Dispatchers.IO.
 * Android requiere un MulticastLock para recibir respuestas multicast/SSDP.
 */
object SsdpDiscovery {

    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val SEARCH_TARGET = "urn:schemas-upnp-org:device:ZonePlayer:1"

    fun discover(context: Context, timeoutMs: Int = 4_000): List<SonosSpeaker> {
        val appContext = context.applicationContext
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val multicastLock = wifi.createMulticastLock("AppSonos-SSDP").apply {
            setReferenceCounted(false)
            acquire()
        }

        try {
            val found = linkedMapOf<String, SonosSpeaker>()
            val request = (
                "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: $SEARCH_TARGET\r\n" +
                    "\r\n"
                ).toByteArray(Charsets.UTF_8)

            DatagramSocket().use { socket ->
                socket.soTimeout = 500
                socket.broadcast = true
                val group = InetAddress.getByName(SSDP_ADDRESS)
                socket.send(DatagramPacket(request, request.size, group, SSDP_PORT))

                val deadline = System.currentTimeMillis() + timeoutMs
                val buffer = ByteArray(8192)
                while (System.currentTimeMillis() < deadline) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: java.net.SocketTimeoutException) {
                        continue
                    }

                    val response = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    if (!response.startsWith("HTTP/1.1 200", ignoreCase = true)) continue
                    val location = response.lineSequence()
                        .firstOrNull { it.trim().startsWith("LOCATION:", ignoreCase = true) }
                        ?.substringAfter(":", "")
                        ?.trim()
                        ?: continue

                    val speaker = describe(location) ?: continue
                    found[speaker.uuid] = speaker
                }
            }
            return found.values.toList()
        } finally {
            if (multicastLock.isHeld) multicastLock.release()
        }
    }

    /** Descarga la descripción UPnP y extrae nombre, modelo, UUID e IP. */
    fun describe(locationUrl: String): SonosSpeaker? {
        return try {
            val xml = SoapClient.getText(locationUrl)
            val uuid = tag(xml, "UDN")?.removePrefix("uuid:") ?: return null
            val name = tag(xml, "friendlyName") ?: return null
            val model = tag(xml, "modelNumber") ?: "Sonos"
            val ip = URI(locationUrl).host ?: return null
            SonosSpeaker(uuid = uuid, name = name, model = model, ip = ip)
        } catch (_: Exception) {
            null
        }
    }

    /** Extrae el contenido de una etiqueta XML sencilla. */
    fun tag(xml: String, name: String): String? {
        val regex = Regex("<(?:[A-Za-z0-9_-]+:)?$name(?:\\s[^>]*)?>(.*?)</(?:[A-Za-z0-9_-]+:)?$name>", RegexOption.DOT_MATCHES_ALL)
        return regex.find(xml)?.groupValues?.getOrNull(1)?.trim()
    }
}
