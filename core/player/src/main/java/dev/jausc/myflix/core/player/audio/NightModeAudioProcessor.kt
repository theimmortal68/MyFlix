package dev.jausc.myflix.core.player.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * An [AudioProcessor] that implements dynamic range compression (DRC) for "night mode" listening.
 *
 * This processor reduces the volume difference between loud and quiet sounds:
 * - Boosts quiet dialogue so it's easier to hear
 * - Compresses loud sounds (explosions, music) so they're not jarring
 *
 * Perfect for late-night viewing when you don't want to disturb others.
 *
 * The compression uses a soft-knee compressor algorithm with:
 * - Configurable threshold, ratio, attack/release times
 * - Makeup gain to compensate for overall volume reduction
 * - Smooth envelope following to avoid pumping artifacts
 */
@OptIn(UnstableApi::class)
class NightModeAudioProcessor : AudioProcessor {

    private var enabled: Boolean = false
    private var pendingEnabled: Boolean = false

    // Compression parameters
    private var thresholdDb: Float = DEFAULT_THRESHOLD_DB
    private var ratio: Float = DEFAULT_RATIO
    private var attackMs: Float = DEFAULT_ATTACK_MS
    private var releaseMs: Float = DEFAULT_RELEASE_MS
    private var makeupGainDb: Float = DEFAULT_MAKEUP_GAIN_DB

    // Derived values (calculated on configure)
    private var attackCoeff: Float = 0f
    private var releaseCoeff: Float = 0f
    private var threshold: Float = 0f
    private var makeupGain: Float = 1f

    // Envelope follower state (per channel)
    private var envelopeLevel: FloatArray = FloatArray(0)

    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat: AudioFormat = AudioFormat.NOT_SET

    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER

    private var inputEnded: Boolean = false

    /**
     * Enables or disables night mode compression.
     * Change takes effect on next flush (seek/track change).
     */
    fun setEnabled(enabled: Boolean) {
        pendingEnabled = enabled
        Log.d(TAG, "Night mode pending: $enabled")
    }

    /**
     * Returns whether night mode is currently enabled.
     */
    fun isEnabled(): Boolean = enabled

    /**
     * Returns whether night mode is pending to be enabled.
     */
    fun isPendingEnabled(): Boolean = pendingEnabled

    /**
     * Sets compression parameters. Call before playback starts.
     *
     * @param thresholdDb dB level above which compression starts (default: -24dB)
     * @param ratio compression ratio, e.g., 4.0 = 4:1 (default: 4.0)
     * @param attackMs attack time in milliseconds (default: 5ms)
     * @param releaseMs release time in milliseconds (default: 100ms)
     * @param makeupGainDb gain boost to apply after compression (default: 6dB)
     */
    fun setCompressionParams(
        thresholdDb: Float = DEFAULT_THRESHOLD_DB,
        ratio: Float = DEFAULT_RATIO,
        attackMs: Float = DEFAULT_ATTACK_MS,
        releaseMs: Float = DEFAULT_RELEASE_MS,
        makeupGainDb: Float = DEFAULT_MAKEUP_GAIN_DB,
    ) {
        this.thresholdDb = thresholdDb
        this.ratio = ratio
        this.attackMs = attackMs
        this.releaseMs = releaseMs
        this.makeupGainDb = makeupGainDb
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding == C.ENCODING_INVALID) {
            return AudioFormat.NOT_SET
        }

        // Only support PCM formats for processing
        if (!isPcmEncoding(inputAudioFormat.encoding)) {
            Log.d(TAG, "Night mode: Non-PCM encoding ${inputAudioFormat.encoding}, bypassing")
            this.inputAudioFormat = AudioFormat.NOT_SET
            this.outputAudioFormat = AudioFormat.NOT_SET
            return AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat

        // Calculate time constants based on sample rate
        val sampleRate = inputAudioFormat.sampleRate.toFloat()
        attackCoeff = 1f - exp(-1f / (attackMs * sampleRate / 1000f))
        releaseCoeff = 1f - exp(-1f / (releaseMs * sampleRate / 1000f))

        // Convert dB to linear
        threshold = dbToLinear(thresholdDb)
        makeupGain = dbToLinear(makeupGainDb)

        // Initialize envelope followers for each channel
        envelopeLevel = FloatArray(inputAudioFormat.channelCount) { 0f }

        Log.d(
            TAG,
            "Night mode configured: threshold=${thresholdDb}dB, ratio=${ratio}:1, " +
                "attack=${attackMs}ms, release=${releaseMs}ms, makeup=${makeupGainDb}dB",
        )

        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return enabled && outputAudioFormat != AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive() || inputBuffer.remaining() == 0) {
            return
        }

        this.inputBuffer = inputBuffer

        val bytesPerSample = getBytesPerSample(inputAudioFormat.encoding)
        val channelCount = inputAudioFormat.channelCount
        val frameSize = bytesPerSample * channelCount
        val frameCount = inputBuffer.remaining() / frameSize

        // Ensure output buffer is large enough
        val requiredSize = frameCount * frameSize
        if (outputBuffer.capacity() < requiredSize) {
            outputBuffer = ByteBuffer.allocateDirect(requiredSize).order(ByteOrder.nativeOrder())
        }
        outputBuffer.clear()

        // Process based on encoding
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> process16Bit(inputBuffer, outputBuffer, channelCount, frameCount)
            C.ENCODING_PCM_FLOAT -> processFloat(inputBuffer, outputBuffer, channelCount, frameCount)
            else -> {
                // Passthrough for unsupported encodings
                outputBuffer.put(inputBuffer)
            }
        }

        outputBuffer.flip()
        inputBuffer.position(inputBuffer.limit())
    }

    private fun process16Bit(input: ByteBuffer, output: ByteBuffer, channels: Int, frames: Int) {
        val scale = 1f / 32768f // Convert to -1..1 range
        val invScale = 32768f

        for (frame in 0 until frames) {
            for (ch in 0 until channels) {
                // Read sample
                val sample = input.short.toFloat() * scale

                // Process through compressor
                val processed = compressSample(sample, ch)

                // Convert back and clamp
                val outSample = (processed * invScale).toInt().coerceIn(-32768, 32767).toShort()
                output.putShort(outSample)
            }
        }
    }

    private fun processFloat(input: ByteBuffer, output: ByteBuffer, channels: Int, frames: Int) {
        for (frame in 0 until frames) {
            for (ch in 0 until channels) {
                val sample = input.float
                val processed = compressSample(sample, ch)
                output.putFloat(processed.coerceIn(-1f, 1f))
            }
        }
    }

    /**
     * Applies compression to a single sample.
     * Uses a feed-forward compressor with envelope following.
     */
    private fun compressSample(sample: Float, channel: Int): Float {
        val inputLevel = abs(sample)

        // Update envelope follower (peak detection with attack/release)
        val coeff = if (inputLevel > envelopeLevel[channel]) attackCoeff else releaseCoeff
        envelopeLevel[channel] += coeff * (inputLevel - envelopeLevel[channel])

        // Calculate gain reduction
        val gainReduction = if (envelopeLevel[channel] > threshold) {
            // Soft knee compression
            val overDb = linearToDb(envelopeLevel[channel] / threshold)
            val compressedDb = overDb / ratio
            dbToLinear(compressedDb - overDb)
        } else {
            1f
        }

        // Apply gain reduction and makeup gain
        return sample * gainReduction * makeupGain
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

        // Apply pending enabled state
        enabled = pendingEnabled
        Log.d(TAG, "Night mode flushed, enabled=$enabled")

        // Reset envelope followers
        for (i in envelopeLevel.indices) {
            envelopeLevel[i] = 0f
        }
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
        enabled = false
        pendingEnabled = false
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

    private fun dbToLinear(db: Float): Float = 10.0.pow((db / 20f).toDouble()).toFloat()

    private fun linearToDb(linear: Float): Float =
        (20.0 * log10(max(linear, 0.0001f).toDouble())).toFloat()

    companion object {
        private const val TAG = "NightModeAudioProcessor"
        private val EMPTY_BUFFER: ByteBuffer =
            ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())

        // Default compression parameters
        const val DEFAULT_THRESHOLD_DB = -24f
        const val DEFAULT_RATIO = 4f
        const val DEFAULT_ATTACK_MS = 5f
        const val DEFAULT_RELEASE_MS = 100f
        const val DEFAULT_MAKEUP_GAIN_DB = 6f
    }
}
