package com.nitrodropnative.transfer

import java.io.InputStream

object FileChunker {
    fun read(input: InputStream, buffer: ByteArray): Int = input.read(buffer)
}
