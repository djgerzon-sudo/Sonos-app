package com.partyrockers.appsonos.sonos

import com.partyrockers.appsonos.data.SonosSpeaker
import com.partyrockers.appsonos.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Capa de control de Sonos para la app.
 * Regla importante de Sonos: los comandos de transporte (play, pausa,
 * siguiente, cola) van al COORDINADOR del grupo, no a cualquier bocina.
 * Si la bocina no es coordinadora, se resuelve el coordinador con
 * ZoneGroupTopology.GetZoneGroupState y se le mandan los comandos a él.
 *
 * Todas las funciones suspenden y hacen red en Dispatchers.IO.
 */
class SonosController(private val appContext: android.content.Context) {

    var currentSpeaker: SonosSpeaker? = null
        private set

    fun selectSpeaker(speaker: SonosSpeaker) {
        currentSpeaker = speaker
    }

    suspend fun discoverSpeakers(timeoutMs: Int = 3_000): List<SonosSpeaker> =
        withContext(Dispatchers.IO) {
            SsdpDiscovery.discover(getApplicationContext(), timeoutMs).filter { speaker ->
                // Encontrar la descripción UPnP no basta: exigimos respuesta SOAP real.
                runCatching { SoapClient.getZoneGroupState(speaker.ip) }.isSuccess
            }
        }

    /**
     * Resuelve la IP del coordinador del grupo al que pertenece [speaker].
     * El XML de GetZoneGroupState trae grupos con coordinator="RINCON_...".
     * Si no se puede resolver, se devuelve la misma bocina (grupos de una sola
     * bocina: la bocina es su propio coordinador).
     */
    suspend fun resolveCoordinatorIp(speaker: SonosSpeaker): String =
        withContext(Dispatchers.IO) {
            try {
                val state = SoapClient.getZoneGroupState(speaker.ip)
                // Des-escape en orden: &amp; SIEMPRE al final, o un "&amp;lt;"
                // se des-escaparía dos veces y corrompería el XML.
                val unescaped = state
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
                // <ZoneGroup Coordinator="RINCON_xxx" ...><ZoneGroupMember UUID="RINCON_yyy" .../>
                val groupRegex = Regex(
                    """<ZoneGroup\s+Coordinator="(RINCON_[^"]+)"[^>]*>(.*?)</ZoneGroup>""",
                    RegexOption.DOT_MATCHES_ALL,
                )
                for (match in groupRegex.findAll(unescaped)) {
                    val coordinatorUuid = match.groupValues[1]
                    val membersXml = match.groupValues[2]
                    if (membersXml.contains(speaker.uuid, ignoreCase = true) ||
                        coordinatorUuid.equals(speaker.uuid, ignoreCase = true)
                    ) {
                        // El coordinador es una bocina del grupo: su IP se saca de
                        // su miembro, que trae Location="http://ip:1400/...".
                        val locationRegex = Regex(
                            """UUID="$coordinatorUuid"[^>]*Location="http://([^:/]+)""",
                        )
                        val ip = locationRegex.find(membersXml)?.groupValues?.getOrNull(1)
                        if (ip != null) return@withContext ip
                        // Si no hay Location, el coordinador es la bocina misma.
                        if (coordinatorUuid.equals(speaker.uuid, ignoreCase = true)) {
                            return@withContext speaker.ip
                        }
                    }
                }
                speaker.ip
            } catch (_: Exception) {
                speaker.ip
            }
        }

    private suspend fun coordinatorIp(): String? {
        val speaker = currentSpeaker ?: return null
        return resolveCoordinatorIp(speaker)
    }

    suspend fun play(): Boolean = runOnCoordinator { SoapClient.play(it) }

    suspend fun pause(): Boolean = runOnCoordinator { SoapClient.pause(it) }

    suspend fun next(): Boolean = runOnCoordinator { SoapClient.next(it) }

    suspend fun previous(): Boolean = runOnCoordinator { SoapClient.previous(it) }

    /** Volumen 0..100. El "Volumen x2" de la UI se aplica antes de llamar aquí. */
    suspend fun setVolume(volume: Int): Boolean {
        val speaker = currentSpeaker ?: return false
        return try {
            withContext(Dispatchers.IO) { SoapClient.setVolume(speaker.ip, volume) }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Manda una rola a la bocina: sirve el archivo local con [serverUrl]
     * y le dice a Sonos que lo toque con SetAVTransportURI + Play.
     * [track] se recibe para la API de v1 (la URL ya lleva el archivo);
     * en v2 se usará para los metadatos DIDL-Lite.
     */
    suspend fun playTrack(track: Track, serverUrl: String): Boolean {
        val ip = coordinatorIp() ?: return false
        return try {
            withContext(Dispatchers.IO) {
                SoapClient.setAvTransportUri(ip, serverUrl)
                SoapClient.play(ip)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Ejecuta [action] contra el coordinador. Devuelve false si falla o no hay bocina. */
    private suspend fun runOnCoordinator(action: (String) -> String): Boolean {
        val ip = coordinatorIp() ?: return false
        return try {
            withContext(Dispatchers.IO) { action(ip) }
            true
        } catch (_: Exception) {
            false
        }
    }
}
