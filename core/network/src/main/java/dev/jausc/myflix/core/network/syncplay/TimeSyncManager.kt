package dev.jausc.myflix.core.network.syncplay

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages time synchronization between client and Jellyfin server.
 * Uses NTP-style algorithm selecting measurement with minimum delay for accuracy.
 *
 * Modes:
 * - Greedy: First 3 measurements at 1-second intervals
 * - Low-profile: After initial sync, measure every 60 seconds
 */
class TimeSyncManager(
    private val syncTimeProvider: suspend () -> UtcTimeResponse,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val measurements = mutableListOf<TimeSyncMeasurement>()
    private val maxMeasurements = 8

    private var _timeOffset: Long = 0L
    private var _roundTripTime: Long = 0L
    private var _measurementCount: Int = 0

    private val _serverTimeOffset = MutableStateFlow(0L)
    val serverTimeOffset: StateFlow<Long> = _serverTimeOffset.asStateFlow()

    private val _averagePing = MutableStateFlow(0L)
    val averagePing: StateFlow<Long> = _averagePing.asStateFlow()

    val timeOffset: Long get() = _timeOffset
    val roundTripTime: Long get() = _roundTripTime
    val measurementCount: Int get() = _measurementCount
    val isGreedyMode: Boolean get() = _measurementCount < GREEDY_PING_COUNT

    private var syncJob: Job? = null
    private var isSyncing = false

    private data class TimeSyncMeasurement(
        val offset: Long,
        val roundTripTime: Long,
        val delay: Long,
        val timestamp: Long = System.currentTimeMillis(),
    )

    companion object {
        private const val TAG = "TimeSyncManager"
        private const val GREEDY_INTERVAL_MS = 1000L
        private const val LOW_PROFILE_INTERVAL_MS = 60_000L
        private const val GREEDY_PING_COUNT = 3
        private const val MAX_RTT_MS = 5000L
    }

    /**
     * Start periodic time synchronization.
     * Initial greedy mode for quick sync, then low-profile maintenance.
     */
    fun startSync() {
        if (isSyncing) return
        isSyncing = true
        _measurementCount = 0

        syncJob = scope.launch {
            while (isActive && isSyncing) {
                performSyncMeasurement()
                _measurementCount++

                val interval = if (_measurementCount < GREEDY_PING_COUNT) {
                    GREEDY_INTERVAL_MS
                } else {
                    LOW_PROFILE_INTERVAL_MS
                }
                delay(interval)
            }
        }
        Log.d(TAG, "Started time synchronization")
    }

    /**
     * Stop periodic time synchronization.
     */
    fun stopSync() {
        isSyncing = false
        syncJob?.cancel()
        syncJob = null
        measurements.clear()
        _measurementCount = 0
        Log.d(TAG, "Stopped time synchronization")
    }

    /**
     * Perform a single NTP-style time sync measurement.
     *
     * Algorithm:
     * t0 = client send time
     * t1 = server receive time
     * t2 = server send time
     * t3 = client receive time
     *
     * offset = ((t1 - t0) + (t2 - t3)) / 2
     * roundTrip = (t3 - t0) - (t2 - t1)
     */
    private suspend fun performSyncMeasurement() {
        try {
            val t0 = System.currentTimeMillis()

            val response = withContext(Dispatchers.IO) {
                syncTimeProvider()
            }

            val t3 = System.currentTimeMillis()

            val t1 = parseIsoTimestamp(response.requestReceptionTime)
            val t2 = parseIsoTimestamp(response.responseTransmissionTime)

            val offset = ((t1 - t0) + (t2 - t3)) / 2
            val rtt = (t3 - t0) - (t2 - t1)
            val networkDelay = (t3 - t0) / 2

            if (rtt > MAX_RTT_MS || rtt < 0) {
                Log.w(TAG, "Discarding measurement with RTT=${rtt}ms")
                return
            }

            synchronized(measurements) {
                measurements.add(TimeSyncMeasurement(offset, rtt, networkDelay))

                while (measurements.size > maxMeasurements) {
                    measurements.removeAt(0)
                }

                if (measurements.isNotEmpty()) {
                    val best = measurements.minByOrNull { it.delay }!!
                    _timeOffset = best.offset
                    _roundTripTime = best.roundTripTime
                    _serverTimeOffset.value = _timeOffset
                    _averagePing.value = _roundTripTime
                }
            }

            Log.v(TAG, "offset=${_timeOffset}ms, RTT=${_roundTripTime}ms, measurements=${measurements.size}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync time: ${e.message}")
        }
    }

    /**
     * Force an immediate sync measurement.
     */
    suspend fun syncNow() {
        performSyncMeasurement()
    }

    /**
     * Convert server time to local time.
     */
    fun toLocalTime(serverMs: Long): Long = serverMs - _timeOffset

    /**
     * Convert local time to server time.
     */
    fun toServerTime(localMs: Long): Long = localMs + _timeOffset

    /**
     * Get current server time based on local clock and offset.
     */
    fun getServerTimeNow(): Long = System.currentTimeMillis() + _timeOffset

    /**
     * Parse ISO 8601 timestamp to milliseconds.
     */
    private fun parseIsoTimestamp(iso: String): Long {
        return try {
            val instant = java.time.Instant.parse(iso)
            instant.toEpochMilli()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse timestamp: $iso")
            System.currentTimeMillis()
        }
    }
}
