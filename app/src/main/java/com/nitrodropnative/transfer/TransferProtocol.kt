package com.nitrodropnative.transfer

import com.nitrodropnative.core.constants.AppConstants
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

object TransferProtocol {
    private const val MAX_HEADER_BYTES = 64 * 1024

    fun writeMetadata(out: DataOutputStream, metadata: TransferMetadata) {
        val magic = AppConstants.PROTOCOL_MAGIC.toByteArray(StandardCharsets.UTF_8)
        val json = metadata.toJson().toByteArray(StandardCharsets.UTF_8)
        require(json.size <= MAX_HEADER_BYTES) { "Metadata too large" }
        out.writeInt(magic.size)
        out.write(magic)
        out.writeInt(json.size)
        out.write(json)
        out.flush()
    }

    fun readMetadata(input: DataInputStream): TransferMetadata {
        val magicLength = input.readInt()
        require(magicLength in 1..32) { "Invalid protocol magic length" }
        val magicBytes = ByteArray(magicLength)
        input.readFully(magicBytes)
        val magic = magicBytes.toString(StandardCharsets.UTF_8)
        require(magic == AppConstants.PROTOCOL_MAGIC) { "Unsupported NitroDrop protocol" }

        val jsonLength = input.readInt()
        require(jsonLength in 1..MAX_HEADER_BYTES) { "Invalid metadata length" }
        val jsonBytes = ByteArray(jsonLength)
        input.readFully(jsonBytes)
        return TransferMetadata.fromJson(jsonBytes.toString(StandardCharsets.UTF_8))
    }

    fun writeResumeOffset(out: DataOutputStream, offset: Long) {
        out.writeLong(offset)
        out.flush()
    }

    fun readResumeOffset(input: DataInputStream): Long = input.readLong()

    fun writeChecksum(out: DataOutputStream, checksum: String) {
        val bytes = checksum.toByteArray(StandardCharsets.UTF_8)
        out.writeInt(bytes.size)
        out.write(bytes)
        out.flush()
    }

    fun readChecksum(input: DataInputStream): String {
        val length = input.readInt()
        require(length in 0..256) { "Invalid checksum length" }
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return bytes.toString(StandardCharsets.UTF_8)
    }
}
