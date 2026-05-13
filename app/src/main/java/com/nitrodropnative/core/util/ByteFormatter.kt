package com.nitrodropnative.core.util

import java.util.Locale
import kotlin.math.max

object ByteFormatter {
    private val units = arrayOf("B", "KB", "MB", "GB", "TB")

    fun format(bytes: Long): String {
        var value = max(0L, bytes).toDouble()
        var unit = 0
        while (value >= 1024.0 && unit < units.lastIndex) {
            value /= 1024.0
            unit++
        }
        return if (unit == 0) "${bytes} B" else String.format(Locale.US, "%.2f %s", value, units[unit])
    }
}
