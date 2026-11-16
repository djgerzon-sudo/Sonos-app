package com.partyrockers.appsonos.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/**
 * Reproductor local del teléfono (Media3 / ExoPlayer).
 * Solo suena cuando la rola tiene un archivo local ([localPath]).
 * Sin archivo no hay audio: la UI no simula reproducción.
 */
class LocalPlayer(context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    var isPlaying: Boolean = false
        private set

    fun playLocalFile(localPath: String): Boolean {
        return try {
            player.setMediaItem(MediaItem.fromUri(localPath))
            player.prepare()
            player.play()
            isPlaying = true
            true
        } catch (_: Exception) {
            isPlaying = false
            false
        }
    }

    fun pause() {
        player.pause()
        isPlaying = false
    }

    fun resume() {
        player.play()
        isPlaying = true
    }

    fun stop() {
        player.stop()
        isPlaying = false
    }

    fun setVolume(volume01: Float) {
        player.volume = volume01.coerceIn(0f, 1f)
    }

    fun release() {
        player.release()
    }
}
