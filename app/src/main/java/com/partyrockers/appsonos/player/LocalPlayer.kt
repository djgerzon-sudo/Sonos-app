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

    private val balanceProcessor = BalanceAudioProcessor()

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setAudioProcessors(balanceProcessor)
        .build()

    var isPlaying: Boolean = false
        private set

    /** True si hay una rola cargada (aunque esté en pausa o parada). */
    val hasMedia: Boolean
        get() = player.mediaItemCount > 0

    /** Balance -100 (izquierda) .. 0 (centro) .. +100 (derecha). Se aplica en caliente. */
    fun setBalance(balance: Int) {
        balanceProcessor.balance = balance
    }

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
