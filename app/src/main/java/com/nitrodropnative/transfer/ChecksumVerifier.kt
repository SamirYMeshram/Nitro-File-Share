package com.nitrodropnative.transfer

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object ChecksumVerifier {
    fun newSha256(): MessageDigest = MessageDigest.getInstance("SHA-256")

    fun digestHex(digest: MessageDigest): String = digest.digest().joinToString("") { "%02x".format(it) }

    fun sha256(file: File): String = file.inputStream().use { input -> sha256(input) }

    fun sha256(input: InputStream): String {
        val digest = newSha256()
        val buffer = ByteArray(1024 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digestHex(digest)
    }
}
