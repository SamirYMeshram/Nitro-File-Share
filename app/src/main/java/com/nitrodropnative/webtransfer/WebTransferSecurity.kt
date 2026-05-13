package com.nitrodropnative.webtransfer

import java.security.SecureRandom

object WebTransferSecurity {
    private const val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    private val random = SecureRandom()

    fun newToken(length: Int = 32): String = buildString(length) {
        repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) }
    }

    fun newPassword(): String = (100 + random.nextInt(900)).toString()

    fun safeFileName(name: String): String {
        val cleaned = name
            .replace("\\", "_")
            .replace("/", "_")
            .replace(Regex("[\\r\\n\\t]"), "_")
            .replace(Regex("[^A-Za-z0-9 ._()\u002D]"), "_")
            .trim()
        return cleaned.take(180).ifBlank { "nitrodrop_file" }
    }

    fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}
