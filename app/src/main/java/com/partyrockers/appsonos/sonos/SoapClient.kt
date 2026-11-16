package com.partyrockers.appsonos.sonos

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente UPnP/SOAP mínimo para bocinas Sonos (puerto 1400).
 * Las bocinas viejas SP100 / ZP100 hablan este protocolo en la red local,
 * sin cuenta en la nube: todo es HTTP POST con un sobre SOAP.
 *
 * Servicios que usa v1:
 *  - urn:schemas-upnp-org:service:AVTransport:1
 *      Play, Pause, Stop, Next, Previous, SetAVTransportURI
 *  - urn:schemas-upnp-org:service:RenderingControl:1
 *      SetVolume, GetVolume
 *  - urn:schemas-upnp-org:service:ZoneGroupTopology:1
 *      GetZoneGroupState (para encontrar al coordinador del grupo)
 *
 * Todas las llamadas son bloqueantes: llamarlas desde Dispatchers.IO.
 */
object SoapClient {

    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 5_000

    private fun post(ip: String, servicePath: String, serviceType: String, action: String, bodyArgs: String): String {
        val url = URL("http://$ip:1400$servicePath")
        val envelope = """<?xml version="1.0" encoding="utf-8"?>""" +
            """<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" """ +
            """s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">""" +
            """<s:Body><u:$action xmlns:u="$serviceType">$bodyArgs</u:$action></s:Body></s:Envelope>"""
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
            connection.setRequestProperty("SOAPAction", "\"$serviceType#$action\"")
            connection.setRequestProperty("Connection", "close")
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(envelope) }
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val text = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
            } ?: ""
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("SOAP $action falló: HTTP ${connection.responseCode}: $text")
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun avTransport(ip: String, action: String, args: String): String =
        post(ip, "/MediaRenderer/AVTransport/Control", "urn:schemas-upnp-org:service:AVTransport:1", action, args)

    private fun renderingControl(ip: String, action: String, args: String): String =
        post(ip, "/MediaRenderer/RenderingControl/Control", "urn:schemas-upnp-org:service:RenderingControl:1", action, args)

    fun play(ip: String): String =
        avTransport(ip, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")

    fun pause(ip: String): String =
        avTransport(ip, "Pause", "<InstanceID>0</InstanceID>")

    fun stop(ip: String): String =
        avTransport(ip, "Stop", "<InstanceID>0</InstanceID>")

    fun next(ip: String): String =
        avTransport(ip, "Next", "<InstanceID>0</InstanceID>")

    fun previous(ip: String): String =
        avTransport(ip, "Previous", "<InstanceID>0</InstanceID>")

    /**
     * Le dice a la bocina que toque una URL de audio (p. ej. la que sirve
     * LocalHttpServer con un archivo del teléfono).
     * [metadata] es DIDL-Lite; vacío está permitido para un mp3 directo.
     */
    fun setAvTransportUri(ip: String, uri: String, metadata: String = ""): String {
        val escapedUri = escapeXml(uri)
        val escapedMeta = escapeXml(metadata)
        return avTransport(
            ip, "SetAVTransportURI",
            "<InstanceID>0</InstanceID><CurrentURI>$escapedUri</CurrentURI>" +
                "<CurrentURIMetaData>$escapedMeta</CurrentURIMetaData>",
        )
    }

    /** Volumen 0..100 en el canal principal ("Master"). */
    fun setVolume(ip: String, volume: Int): String {
        val v = volume.coerceIn(0, 100)
        return renderingControl(
            ip, "SetVolume",
            "<InstanceID>0</InstanceID><Channel>Master</Channel><DesiredVolume>$v</DesiredVolume>",
        )
    }

    fun getZoneGroupState(ip: String): String =
        post(
            ip,
            "/ZoneGroupTopology/Control",
            "urn:schemas-upnp-org:service:ZoneGroupTopology:1",
            "GetZoneGroupState",
            "<InstanceID>0</InstanceID>",
        )

    /** Lee la descripción del dispositivo (nombre, modelo, UDN) por HTTP GET. */
    fun getText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Connection", "close")
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("GET $url falló: HTTP ${connection.responseCode}")
            }
            return connection.inputStream.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            }
        } finally {
            connection.disconnect()
        }
    }

    fun escapeXml(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
