package com.nitrodropnative.transfer

import com.nitrodropnative.core.constants.AppConstants
import java.util.concurrent.ArrayBlockingQueue

class BufferPool(
    private val bufferSize: Int = AppConstants.DEFAULT_CHUNK_SIZE,
    poolSize: Int = 2
) {
    private val pool = ArrayBlockingQueue<ByteArray>(poolSize).apply {
        repeat(poolSize) { offer(ByteArray(bufferSize)) }
    }

    fun acquire(): ByteArray = pool.poll() ?: ByteArray(bufferSize)

    fun release(buffer: ByteArray) {
        if (buffer.size == bufferSize) pool.offer(buffer)
    }
}
