package com.partyrockers.appsonos.sonos

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente UPnP/SOAP mínimo para Sonos en la red local (puerto 1400).
 * Todas las llamadas son bloqueantes: ejecutarlas desde Dispatchers.IO.
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
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
            connection.setRequestProperty("SOAPAction", "\"$serviceType#$action\"")
            connection.setRequestProperty("Connection", "close")
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(envelope) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
            } ?: ""
            if (code !in 200..299) {
                throw IllegalStateException("SOAP $action falló: HTTP $code: $response")
            }
            return response
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

    fun setAvTransportUri(ip: String, uri: String, metadata: String = ""): String {
        return avTransport(
            ip, "SetAVTransportURI",
            "<InstanceID>0</InstanceID><CurrentURI>${escapeXml(uri)}</CurrentURI>" +
                "<CurrentURIMetaData>${escapeXml(metadata)}</CurrentURIMetaData>",
        )
    }

    fun setVolume(ip: String, volume: Int): String {
        val v = volume.coerceIn(0, 100)
        return renderingControl(
            ip, "SetVolume",
            "<InstanceID>0</InstanceID><Channel>Master</Channel><DesiredVolume>$v</DesiredVolume>",
        )
    }

    /** GetZoneGroupState no recibe argumentos según la acción UPnP. */
    fun getZoneGroupState(ip: String): String =
        post(
            ip,
            "/ZoneGroupTopology/Control",
            "urn:schemas-upnp-org:service:ZoneGroupTopology:1",
            "GetZoneGroupState",
            "",
        )

    fun getText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Connection", "close")
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("GET descripción falló: HTTP $code")
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
