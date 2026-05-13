package com.nitrodropnative.transfer

import com.nitrodropnative.core.constants.AppConstants

class StabilityController {
    fun chunkSizeFor(bytesPerSecond: Long): Int = when {
        bytesPerSecond > 80L * 1024L * 1024L -> AppConstants.LARGE_CHUNK_SIZE
        bytesPerSecond > 30L * 1024L * 1024L -> AppConstants.DEFAULT_CHUNK_SIZE
        else -> AppConstants.SMALL_CHUNK_SIZE
    }

    fun uiIntervalFor(bytesPerSecond: Long, stabilityPercent: Int): Long = when {
        stabilityPercent < 60 -> AppConstants.UI_UPDATE_INTERVAL_SLOW_MS
        bytesPerSecond > 80L * 1024L * 1024L -> AppConstants.UI_UPDATE_INTERVAL_MS
        else -> AppConstants.UI_UPDATE_INTERVAL_MS
    }
}
