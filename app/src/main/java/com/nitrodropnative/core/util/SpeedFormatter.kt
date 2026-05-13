package com.nitrodropnative.core.util

import java.util.Locale

object SpeedFormatter {
    fun format(bytesPerSecond: Long): String {
        val mbps = bytesPerSecond / (1024.0 * 1024.0)
        return when {
            mbps >= 100 -> String.format(Locale.US, "%.0f MB/s", mbps)
            mbps >= 10 -> String.format(Locale.US, "%.1f MB/s", mbps)
            mbps >= 1 -> String.format(Locale.US, "%.2f MB/s", mbps)
            else -> "${bytesPerSecond / 1024} KB/s"
        }
    }

    fun mbps(bytesPerSecond: Long): String = String.format(Locale.US, "%.2f", bytesPerSecond / (1024.0 * 1024.0))
}
