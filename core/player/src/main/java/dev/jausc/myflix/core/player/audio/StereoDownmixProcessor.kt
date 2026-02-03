package dev.jausc.myflix.core.player.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * An [AudioProcessor] that downmixes multi-channel audio to stereo.
 *
 * Uses ITU-R BS.775-1 downmix coefficients:
 * - Center channel: -3dB to both L/R (coefficient = 1/√2 ≈ 0.707)
 * - Surround channels: -3dB to respective L/R (coefficient = 1/√2 ≈ 0.707)
 * - LFE: excluded by default (can be enabled with attenuation)
 *
 * Supports common multi-channel layouts:
 * - 5.1 (6 channels): L, R, C, LFE, Ls, Rs
 * - 7.1 (8 channels): L, R, C, LFE, Ls, Rs, Lrs, Rrs
 *
 * This is useful when:
 * - Playing surround content through stereo speakers/headphones
 * - The audio receiver doesn't support surround formats
 * - You want consistent stereo output regardless of source format
 */
@OptIn(UnstableApi::class)
class StereoDownmixProcessor : AudioProcessor {

    private var enabled: Boolean = false
    private var pendingEnabled: Boolean = false
    private var includeLfe: Boolean = false
    private var pendingIncludeLfe: Boolean = false

    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat: AudioFormat = AudioFormat.NOT_SET

    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER

    private var inputEnded: Boolean = false

    /**
     * Enables or disables stereo downmix.
     * Change takes effect on next flush (seek/track change).
     */
    fun setEnabled(enabled: Boolean) {
        pendingEnabled = enabled
        Log.d(TAG, "Stereo downmix pending: $enabled")
    }

    /**
     * Returns whether stereo downmix is currently enabled.
     */
    fun isEnabled(): Boolean = enabled

    /**
     * Returns whether stereo downmix is pending to be enabled.
     */
    fun isPendingEnabled(): Boolean = pendingEnabled

    /**
     * Sets whether to include LFE channel in downmix.
     * When enabled, LFE is mixed at -10dB to both channels.
     * Change takes effect on next flush.
     */
    fun setIncludeLfe(include: Boolean) {
        pendingIncludeLfe = include
        Log.d(TAG, "Stereo downmix include LFE pending: $include")
    }

    /**
     * Returns whether LFE is included in downmix.
     */
    fun isLfeIncluded(): Boolean = includeLfe

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding == C.ENCODING_INVALID) {
            return AudioFormat.NOT_SET
        }

        // Only support PCM formats for processing
        if (!isPcmEncoding(inputAudioFormat.encoding)) {
            Log.d(TAG, "Stereo downmix: Non-PCM encoding ${inputAudioFormat.encoding}, bypassing")
            this.inputAudioFormat = AudioFormat.NOT_SET
            this.outputAudioFormat = AudioFormat.NOT_SET
            return AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat

        // Only downmix if we have more than 2 channels
        if (inputAudioFormat.channelCount <= 2) {
            Log.d(TAG, "Stereo downmix: Already stereo or mono (${inputAudioFormat.channelCount} ch), bypassing")
            this.outputAudioFormat = inputAudioFormat
            return inputAudioFormat
        }

        // Output format is stereo with same sample rate and encoding
        this.outputAudioFormat = AudioFormat(
            inputAudioFormat.sampleRate,
            2, // Stereo output
            inputAudioFormat.encoding,
        )

        Log.d(
            TAG,
            "Stereo downmix configured: ${inputAudioFormat.channelCount}ch -> 2ch, " +
                "sampleRate=${inputAudioFormat.sampleRate}, encoding=${inputAudioFormat.encoding}",
        )

        return outputAudioFormat
    }

    override fun isActive(): Boolean {
        // Active when enabled, format is set, and we actually have multi-channel input
        return enabled &&
            outputAudioFormat != AudioFormat.NOT_SET &&
            inputAudioFormat.channelCount > 2
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive() || inputBuffer.remaining() == 0) {
            return
        }

        this.inputBuffer = inputBuffer

        val bytesPerSample = getBytesPerSample(inputAudioFormat.encoding)
        val inputChannels = inputAudioFormat.channelCount
        val inputFrameSize = bytesPerSample * inputChannels
        val outputFrameSize = bytesPerSample * 2 // Stereo
        val frameCount = inputBuffer.remaining() / inputFrameSize

        // Ensure output buffer is large enough
        val requiredSize = frameCount * outputFrameSize
        if (outputBuffer.capacity() < requiredSize) {
            outputBuffer = ByteBuffer.allocateDirect(requiredSize).order(ByteOrder.nativeOrder())
        }
        outputBuffer.clear()

        // Process based on encoding
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> process16Bit(inputBuffer, outputBuffer, inputChannels, frameCount)
            C.ENCODING_PCM_FLOAT -> processFloat(inputBuffer, outputBuffer, inputChannels, frameCount)
            else -> {
                // For unsupported encodings, pass through (shouldn't happen due to configure check)
                Log.w(TAG, "Unsupported encoding for downmix: ${inputAudioFormat.encoding}")
            }
        }

        outputBuffer.flip()
        inputBuffer.position(inputBuffer.limit())
    }

    private fun process16Bit(input: ByteBuffer, output: ByteBuffer, channels: Int, frames: Int) {
        val scale = 1f / 32768f
        val invScale = 32768f

        for (frame in 0 until frames) {
            // Read all channels for this frame
            val samples = FloatArray(channels)
            for (ch in 0 until channels) {
                samples[ch] = input.short.toFloat() * scale
            }

            // Downmix to stereo
            val (left, right) = downmixFrame(samples, channels)

            // Clamp and write output
            val leftOut = (left * invScale).toInt().coerceIn(-32768, 32767).toShort()
            val rightOut = (right * invScale).toInt().coerceIn(-32768, 32767).toShort()
            output.putShort(leftOut)
            output.putShort(rightOut)
        }
    }

    private fun processFloat(input: ByteBuffer, output: ByteBuffer, channels: Int, frames: Int) {
        for (frame in 0 until frames) {
            // Read all channels for this frame
            val samples = FloatArray(channels)
            for (ch in 0 until channels) {
                samples[ch] = input.float
            }

            // Downmix to stereo
            val (left, right) = downmixFrame(samples, channels)

            // Clamp and write output
            output.putFloat(left.coerceIn(-1f, 1f))
            output.putFloat(right.coerceIn(-1f, 1f))
        }
    }

    /**
     * Downmix a single frame of multi-channel audio to stereo.
     * Uses ITU-R BS.775-1 coefficients.
     *
     * Standard channel order (SMPTE/ITU):
     * - 5.1: L(0), R(1), C(2), LFE(3), Ls(4), Rs(5)
     * - 7.1: L(0), R(1), C(2), LFE(3), Ls(4), Rs(5), Lrs(6), Rrs(7)
     */
    private fun downmixFrame(samples: FloatArray, channels: Int): Pair<Float, Float> {
        var left = 0f
        var right = 0f

        when (channels) {
            6 -> {
                // 5.1: L, R, C, LFE, Ls, Rs
                left = samples[0] + COEFF_CENTER * samples[2] + COEFF_SURROUND * samples[4]
                right = samples[1] + COEFF_CENTER * samples[2] + COEFF_SURROUND * samples[5]
                if (includeLfe) {
                    left += COEFF_LFE * samples[3]
                    right += COEFF_LFE * samples[3]
                }
            }
            8 -> {
                // 7.1: L, R, C, LFE, Ls, Rs, Lrs, Rrs
                left = samples[0] + COEFF_CENTER * samples[2] +
                    COEFF_SURROUND * samples[4] + COEFF_SURROUND * samples[6]
                right = samples[1] + COEFF_CENTER * samples[2] +
                    COEFF_SURROUND * samples[5] + COEFF_SURROUND * samples[7]
                if (includeLfe) {
                    left += COEFF_LFE * samples[3]
                    right += COEFF_LFE * samples[3]
                }
            }
            4 -> {
                // Quad: L, R, Ls, Rs (no center or LFE)
                left = samples[0] + COEFF_SURROUND * samples[2]
                right = samples[1] + COEFF_SURROUND * samples[3]
            }
            3 -> {
                // 3.0: L, R, C
                left = samples[0] + COEFF_CENTER * samples[2]
                right = samples[1] + COEFF_CENTER * samples[2]
            }
            else -> {
                // Generic fallback: mix all channels equally
                val gain = 1f / sqrt(channels.toFloat())
                for (i in 0 until channels) {
                    if (i % 2 == 0) {
                        left += samples[i] * gain
                    } else {
                        right += samples[i] * gain
                    }
                }
            }
        }

        // Apply normalization to prevent clipping
        // For 5.1 worst case: 1 + 0.707 + 0.707 = 2.414, so normalize by ~0.414
        left *= NORMALIZATION_GAIN
        right *= NORMALIZATION_GAIN

        return Pair(left, right)
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === EMPTY_BUFFER
    }

    override fun flush() {
        inputBuffer = EMPTY_BUFFER
        outputBuffer = EMPTY_BUFFER
        inputEnded = false

        // Apply pending state
        enabled = pendingEnabled
        includeLfe = pendingIncludeLfe
        Log.d(TAG, "Stereo downmix flushed, enabled=$enabled, includeLfe=$includeLfe")
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
        enabled = false
        pendingEnabled = false
        includeLfe = false
        pendingIncludeLfe = false
    }

    private fun isPcmEncoding(encoding: Int): Boolean {
        return encoding == C.ENCODING_PCM_8BIT ||
            encoding == C.ENCODING_PCM_16BIT ||
            encoding == C.ENCODING_PCM_16BIT_BIG_ENDIAN ||
            encoding == C.ENCODING_PCM_24BIT ||
            encoding == C.ENCODING_PCM_24BIT_BIG_ENDIAN ||
            encoding == C.ENCODING_PCM_32BIT ||
            encoding == C.ENCODING_PCM_32BIT_BIG_ENDIAN ||
            encoding == C.ENCODING_PCM_FLOAT
    }

    private fun getBytesPerSample(encoding: Int): Int {
        return when (encoding) {
            C.ENCODING_PCM_8BIT -> 1
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG_ENDIAN -> 2
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN -> 4
            C.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }
    }

    companion object {
        private const val TAG = "StereoDownmixProcessor"
        private val EMPTY_BUFFER: ByteBuffer =
            ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())

        // ITU-R BS.775-1 coefficients
        /** Center channel coefficient: -3dB = 1/√2 ≈ 0.707 */
        private const val COEFF_CENTER = 0.7071067811865476f

        /** Surround channel coefficient: -3dB = 1/√2 ≈ 0.707 */
        private const val COEFF_SURROUND = 0.7071067811865476f

        /** LFE coefficient: -10dB ≈ 0.316 */
        private const val COEFF_LFE = 0.31622776601683794f

        /** Normalization gain to prevent clipping (1 / 2.414 ≈ 0.414) */
        private const val NORMALIZATION_GAIN = 0.7071067811865476f
    }
}
