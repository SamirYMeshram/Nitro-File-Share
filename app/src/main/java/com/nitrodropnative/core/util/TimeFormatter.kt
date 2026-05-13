package com.nitrodropnative.core.util

object TimeFormatter {
    fun eta(seconds: Long): String {
        if (seconds < 0 || seconds == Long.MAX_VALUE) return "--"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "%dh %02dm %02ds".format(h, m, s)
            m > 0 -> "%dm %02ds".format(m, s)
            else -> "%ds".format(s)
        }
    }

    fun duration(ms: Long): String = eta(ms / 1000L)
}
