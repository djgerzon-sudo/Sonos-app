package com.partyrockers.appsonos

import com.partyrockers.appsonos.data.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackTest {

    @Test
    fun durationText_formatsMinutesAndSeconds() {
        val track = Track(id = "x", title = "T", artist = "A", album = "B", durationSeconds = 214)
        assertEquals("3:34", track.durationText)
    }

    @Test
    fun durationText_padsSingleDigitSeconds() {
        val track = Track(id = "x", title = "T", artist = "A", album = "B", durationSeconds = 187)
        assertEquals("3:07", track.durationText)
    }
}
