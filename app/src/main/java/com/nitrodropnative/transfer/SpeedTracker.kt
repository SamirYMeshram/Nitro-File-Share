package com.nitrodropnative.transfer

import com.nitrodropnative.core.constants.AppConstants
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class SpeedTracker(
    private val windowMs: Long = AppConstants.SPEED_WINDOW_MS,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private data class Sample(val timeMs: Long, val bytes: Long)

    private val samples = ArrayDeque<Sample>()
    private var startedAtMs = clock()
    private var totalBytes = 0L
    private var peakBytesPerSecond = 0L
    private var lastSpeed = 0L
    private val recentSpeeds = ArrayDeque<Long>()

    @Synchronized
    fun addBytes(bytes: Int) {
        if (bytes <= 0) return
        val now = clock()
        totalBytes += bytes.toLong()
        samples.addLast(Sample(now, bytes.toLong()))
        trim(now)
        lastSpeed = currentSpeedLocked(now)
        peakBytesPerSecond = max(peakBytesPerSecond, lastSpeed)
        recentSpeeds.addLast(lastSpeed)
        while (recentSpeeds.size > 16) recentSpeeds.removeFirst()
    }

    @Synchronized
    fun currentSpeed(): Long {
        val now = clock()
        trim(now)
        lastSpeed = currentSpeedLocked(now)
        peakBytesPerSecond = max(peakBytesPerSecond, lastSpeed)
        return lastSpeed
    }

    @Synchronized
    fun averageSpeed(): Long {
        val elapsedSeconds = max(1.0, (clock() - startedAtMs) / 1000.0)
        return (totalBytes / elapsedSeconds).toLong()
    }

    @Synchronized
    fun peakSpeed(): Long = peakBytesPerSecond

    @Synchronized
    fun eta(totalBytes: Long, transferredBytes: Long): Long {
        val remaining = totalBytes - transferredBytes
        val speed = currentSpeed().takeIf { it > 0 } ?: averageSpeed()
        return if (remaining <= 0) 0 else if (speed <= 0) Long.MAX_VALUE else remaining / speed
    }

    @Synchronized
    fun stabilityPercent(): Int {
        if (recentSpeeds.size < 4) return 100
        val avg = recentSpeeds.average()
        if (avg <= 0.0) return 100
        val meanDeviation = recentSpeeds.map { abs(it - avg) }.average()
        val instability = (meanDeviation / avg).coerceIn(0.0, 1.0)
        return ((1.0 - instability) * 100.0).roundToInt().coerceIn(0, 100)
    }

    @Synchronized
    fun reset() {
        samples.clear()
        recentSpeeds.clear()
        startedAtMs = clock()
        totalBytes = 0L
        peakBytesPerSecond = 0L
        lastSpeed = 0L
    }

    private fun trim(now: Long) {
        while (samples.isNotEmpty() && now - samples.first.timeMs > windowMs) {
            samples.removeFirst()
        }
    }

    private fun currentSpeedLocked(now: Long): Long {
        if (samples.isEmpty()) return 0L
        val bytes = samples.sumOf { it.bytes }
        val elapsed = max(250L, now - samples.first.timeMs)
        return ((bytes * 1000.0) / elapsed).toLong()
    }
}
