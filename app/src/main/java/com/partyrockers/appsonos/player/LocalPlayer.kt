package com.partyrockers.appsonos.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * Reproductor local del teléfono (Media3 / ExoPlayer).
 * Solo suena cuando la rola tiene un archivo local ([localPath]).
 * Sin archivo no hay audio: la UI no simula reproducción.
 *
 * El balance L/R se aplica con [BalanceAudioProcessor]. En Media3 1.4.1
 * ExoPlayer.Builder NO tiene setAudioProcessors: los procesadores van por
 * DefaultAudioSink.Builder.setAudioProcessors, y el sink se inyecta con una
 * DefaultRenderersFactory que sobrescribe buildAudioSink.
 */
class LocalPlayer(context: Context) {

    private val balanceProcessor = BalanceAudioProcessor()

    private val renderersFactory = object : DefaultRenderersFactory(context.applicationContext) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink? {
            return DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf<AudioProcessor>(balanceProcessor))
                .build()
        }
    }

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setRenderersFactory(renderersFactory)
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
