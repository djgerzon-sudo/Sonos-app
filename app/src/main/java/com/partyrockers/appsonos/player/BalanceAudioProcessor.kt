package com.partyrockers.appsonos.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Balance real izquierda/derecha para el audio local (Media3/ExoPlayer).
 * balance: -100 (todo a la izquierda) .. 0 (centro) .. +100 (todo a la derecha).
 * Solo procesa PCM estéreo (16-bit o float); cualquier otro formato pasa sin tocar.
 * El valor se puede cambiar en caliente: el procesador lee [balance] en cada buffer,
 * así el slider responde sin reconstruir el reproductor.
 */
class BalanceAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var balance: Int = 0
        set(value) {
            field = value.coerceIn(-100, 100)
        }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.channelCount != 2) return AudioProcessor.AudioFormat.NOT_SET
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    // Media3 1.4.1: BaseAudioProcessor NO implementa queueInput (es abstracto en la
    // interfaz AudioProcessor) y NO existe onQueueInput (eso era ExoPlayer 2.x).
    // La subclase debe implementar queueInput(ByteBuffer) directamente.
    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val b = balance
        val leftGain = if (b <= 0) 1f else 1f - b / 100f
        val rightGain = if (b >= 0) 1f else 1f + b / 100f
        val out = replaceOutputBuffer(remaining)
        if (leftGain == 1f && rightGain == 1f) {
            // Centro: copiar sin tocar.
            out.put(inputBuffer)
            out.flip()
            return
        }
        if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            val output = out.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            while (input.hasRemaining()) {
                output.put((input.get() * leftGain).coerceIn(-1f, 1f))
                if (!input.hasRemaining()) break
                output.put((input.get() * rightGain).coerceIn(-1f, 1f))
            }
            inputBuffer.position(inputBuffer.limit())
            out.position(output.position() * 4)
            out.flip()
        } else {
            val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val output = out.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            while (input.hasRemaining()) {
                output.put(
                    (input.get() * leftGain).roundToInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort(),
                )
                if (!input.hasRemaining()) break
                output.put(
                    (input.get() * rightGain).roundToInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort(),
                )
            }
            inputBuffer.position(inputBuffer.limit())
            out.position(output.position() * 2)
            out.flip()
        }
    }
}
