package com.partyrockers.appsonos

import com.partyrockers.appsonos.data.PlaylistParser
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistParserTest {

    @Test
    fun parseM3u_ignoresCommentsAndBlankLines() {
        val text = "#EXTM3U\n\n/Musica/Rap/rola1.mp3\n#EXTINF:214,Artista - Titulo\n/Musica/Rap/rola2.mp3\n"
        assertEquals(
            listOf("/Musica/Rap/rola1.mp3", "/Musica/Rap/rola2.mp3"),
            PlaylistParser.parseM3u(text),
        )
    }

    @Test
    fun parsePls_ordersByFileNumber() {
        val text = "[playlist]\nFile2=/b.mp3\nFile1=/a.mp3\nNumberOfEntries=2\n"
        assertEquals(listOf("/a.mp3", "/b.mp3"), PlaylistParser.parsePls(text))
    }

    @Test
    fun parseWpl_extractsMediaSrc() {
        val text = """<?wpl version="1.0"?><smil><body><seq><media src="/a.mp3"/><media src="/b.mp3"/></seq></body></smil>"""
        assertEquals(listOf("/a.mp3", "/b.mp3"), PlaylistParser.parseWpl(text))
    }

    @Test
    fun parse_unknownExtension_returnsEmpty() {
        assertEquals(emptyList<String>(), PlaylistParser.parse("lista.txt", "algo"))
    }
}
