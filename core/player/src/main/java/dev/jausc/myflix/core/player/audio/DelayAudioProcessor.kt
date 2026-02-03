package dev.jausc.myflix.core.player.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * An [AudioProcessor] that delays audio by a configurable amount.
 *
 * Positive delay values delay the audio (audio plays later than video).
 * Negative delay values advance the audio (audio plays earlier / drops initial samples).
 *
 * This is useful for correcting audio/video sync issues, particularly with
 * DTS or other formats that may have processing latency.
 *
 * Range: -500ms to +500ms in 10ms increments recommended.
 */
@OptIn(UnstableApi::class)
class DelayAudioProcessor : AudioProcessor {

    private var pendingDelayMs: Long = 0L
    private var activeDelayMs: Long = 0L

    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat: AudioFormat = AudioFormat.NOT_SET

    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var delayBuffer: ByteBuffer = EMPTY_BUFFER

    private var delayBufferSize: Int = 0
    private var delayBufferWritePos: Int = 0
    private var delayBufferReadPos: Int = 0
    private var delayBufferFilled: Int = 0

    private var inputEnded: Boolean = false
    private var samplesToSkip: Int = 0

    /**
     * Sets the delay in milliseconds.
     *
     * @param delayMs Positive to delay audio (audio behind video),
     *                negative to advance audio (audio ahead of video).
     *                Typical range: -500 to +500 ms.
     */
    fun setDelayMs(delayMs: Long) {
        pendingDelayMs = delayMs.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
        Log.d(TAG, "Audio delay pending: ${pendingDelayMs}ms")
    }

    /**
     * Gets the currently pending delay in milliseconds.
     */
    fun getPendingDelayMs(): Long = pendingDelayMs

    /**
     * Gets the currently active delay in milliseconds.
     */
    fun getActiveDelayMs(): Long = activeDelayMs

    /**
     * Returns whether delay is currently being applied.
     */
    fun isDelayActive(): Boolean = activeDelayMs != 0L

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding == C.ENCODING_INVALID) {
            return AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        // Bypass completely if no delay would be applied
        if (pendingDelayMs == 0L && activeDelayMs == 0L) return false
        return outputAudioFormat != AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive() || inputBuffer.remaining() == 0) {
            return
        }

        this.inputBuffer = inputBuffer

        // Handle negative delay (skip samples from start)
        if (samplesToSkip > 0) {
            val bytesToSkip = min(samplesToSkip, inputBuffer.remaining())
            inputBuffer.position(inputBuffer.position() + bytesToSkip)
            samplesToSkip -= bytesToSkip
            if (inputBuffer.remaining() == 0) {
                return
            }
        }

        // If no positive delay, pass through directly
        if (activeDelayMs <= 0) {
            outputBuffer = inputBuffer.slice().order(ByteOrder.nativeOrder())
            inputBuffer.position(inputBuffer.limit())
            return
        }

        // Process through delay buffer for positive delay
        processWithDelay(inputBuffer)
    }

    private fun processWithDelay(input: ByteBuffer) {
        val inputBytes = input.remaining()

        // Ensure output buffer is large enough
        if (outputBuffer.capacity() < inputBytes) {
            outputBuffer = ByteBuffer.allocateDirect(inputBytes).order(ByteOrder.nativeOrder())
        }
        outputBuffer.clear()

        var bytesWritten = 0

        while (input.hasRemaining()) {
            // Write input to delay buffer
            if (delayBufferFilled < delayBufferSize) {
                // Still filling the delay buffer - output silence
                val bytesToBuffer = min(input.remaining(), delayBufferSize - delayBufferFilled)
                for (i in 0 until bytesToBuffer) {
                    delayBuffer.put(delayBufferWritePos, input.get())
                    delayBufferWritePos = (delayBufferWritePos + 1) % delayBufferSize
                }
                delayBufferFilled += bytesToBuffer

                // Output silence while filling
                for (i in 0 until bytesToBuffer) {
                    outputBuffer.put(0)
                    bytesWritten++
                }
            } else {
                // Delay buffer is full - read old data and write new data
                val byte = input.get()
                val delayedByte = delayBuffer.get(delayBufferReadPos)

                delayBuffer.put(delayBufferWritePos, byte)
                delayBufferWritePos = (delayBufferWritePos + 1) % delayBufferSize
                delayBufferReadPos = (delayBufferReadPos + 1) % delayBufferSize

                outputBuffer.put(delayedByte)
                bytesWritten++
            }
        }

        outputBuffer.flip()
    }

    override fun queueEndOfStream() {
        inputEnded = true

        // Flush remaining delay buffer
        if (activeDelayMs > 0 && delayBufferFilled > 0) {
            if (outputBuffer.capacity() < delayBufferFilled) {
                outputBuffer =
                    ByteBuffer.allocateDirect(delayBufferFilled).order(ByteOrder.nativeOrder())
            }
            outputBuffer.clear()

            for (i in 0 until delayBufferFilled) {
                outputBuffer.put(delayBuffer.get(delayBufferReadPos))
                delayBufferReadPos = (delayBufferReadPos + 1) % delayBufferSize
            }
            delayBufferFilled = 0
            outputBuffer.flip()
        }
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === EMPTY_BUFFER && delayBufferFilled == 0
    }

    override fun flush() {
        inputBuffer = EMPTY_BUFFER
        outputBuffer = EMPTY_BUFFER
        inputEnded = false

        // Apply pending delay change
        activeDelayMs = pendingDelayMs
        Log.d(TAG, "Audio delay flushed, active: ${activeDelayMs}ms")

        if (activeDelayMs > 0 && inputAudioFormat != AudioFormat.NOT_SET) {
            // Calculate delay buffer size in bytes
            val bytesPerSecond = inputAudioFormat.sampleRate *
                inputAudioFormat.channelCount *
                getBytesPerSample(inputAudioFormat.encoding)
            delayBufferSize = (bytesPerSecond * activeDelayMs / 1000).toInt()

            if (delayBuffer.capacity() < delayBufferSize) {
                delayBuffer =
                    ByteBuffer.allocateDirect(delayBufferSize).order(ByteOrder.nativeOrder())
            }
            delayBuffer.clear()
            for (i in 0 until delayBufferSize) {
                delayBuffer.put(0)
            }
            delayBufferWritePos = 0
            delayBufferReadPos = 0
            delayBufferFilled = 0
            samplesToSkip = 0
        } else if (activeDelayMs < 0 && inputAudioFormat != AudioFormat.NOT_SET) {
            // For negative delay, calculate samples to skip
            val bytesPerSecond = inputAudioFormat.sampleRate *
                inputAudioFormat.channelCount *
                getBytesPerSample(inputAudioFormat.encoding)
            samplesToSkip = (bytesPerSecond * -activeDelayMs / 1000).toInt()
            delayBufferSize = 0
            delayBufferFilled = 0
        } else {
            delayBufferSize = 0
            delayBufferFilled = 0
            samplesToSkip = 0
        }
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
        delayBuffer = EMPTY_BUFFER
        activeDelayMs = 0
        pendingDelayMs = 0
    }

    private fun getBytesPerSample(encoding: Int): Int {
        return when (encoding) {
            C.ENCODING_PCM_8BIT -> 1
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG_ENDIAN -> 2
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN -> 4
            C.ENCODING_PCM_FLOAT -> 4
            else -> 2 // Default to 16-bit
        }
    }

    companion object {
        private const val TAG = "DelayAudioProcessor"
        private val EMPTY_BUFFER: ByteBuffer =
            ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())

        /** Minimum delay in milliseconds */
        const val MIN_DELAY_MS = -500L

        /** Maximum delay in milliseconds */
        const val MAX_DELAY_MS = 500L

        /** Recommended increment for delay adjustments */
        const val DELAY_INCREMENT_MS = 10L
    }
}
